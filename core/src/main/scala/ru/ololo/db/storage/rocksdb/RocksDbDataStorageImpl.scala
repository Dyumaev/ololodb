package ru.ololo.db.storage.rocksdb

import java.util.Arrays

import cats.syntax.all._
import cats.effect.{Resource, Sync}
import org.rocksdb.{ColumnFamilyHandle, RocksDB, TransactionDB, WriteBatch}
import ru.ololo.db.storage.Row
import ru.ololo.db.storage.DataStorage

import scala.collection.JavaConverters._

class RocksDbDataStorageImpl[F[_]: Sync](
  rocksDB: RocksDB,
  transactionDB: TransactionDB,
  dataColumnFamily: ColumnFamilyHandle,
  versionsColumnFamily: ColumnFamilyHandle
) extends DataStorage[F] { self =>

  def get(key: String): F[Option[Row]] =
    Sync[F].delay {
      val keyBytes = key.getBytes
      val keys = Arrays.asList(keyBytes, keyBytes)
      val handlers = Arrays.asList(dataColumnFamily, versionsColumnFamily)
      rocksDB
        .multiGetAsList(DEFAULT_READ_OPTIONS, handlers, keys)
        .asScala
        .toList match {
        case List(value, version) => Some(Row(key, new String(value), LongSerializer.deserialize(version)))
        case _                    => None
      }
    }

  def put(key: String, value: String): F[Row] =
    Resource
      .fromAutoCloseable(Sync[F].delay(new WriteBatch()))
      .use { batch =>
        Sync[F].delay {
          val keyBytes = key.getBytes

          val walFiles = rocksDB.getSortedWalFiles
          val latestSequenceNumber = rocksDB.getLatestSequenceNumber
          val transactionsLog = rocksDB.getUpdatesSince(latestSequenceNumber)

          if (transactionsLog.isValid) {
            val batch = transactionsLog.getBatch.writeBatch()
            val sequenceNumber = transactionsLog.getBatch.sequenceNumber()
            batch.data()

            batch.iterate(new WriteBatch.Handler {})

          } else ???

          batch.putLogData()

          batch.put(dataColumnFamily, keyBytes, value.getBytes)
          batch.merge(versionsColumnFamily, keyBytes, INCREMENT)
          rocksDB.write(DEFAULT_WRITE_OPTIONS, batch)
        }
      }
      .flatMap { _ =>
        self
          .get(key) // The latest value at the moment
          .map(_.getOrElse(???)) // Impossible situation
      }

  // TODO Ensure transaction rollback
  def cas(key: String, oldValue: String, newValue: String): F[(Row, Boolean)] =
    Resource
      .fromAutoCloseable(Sync[F].delay(transactionDB.beginTransaction(DEFAULT_WRITE_OPTIONS)))
      .use { transaction =>
        Sync[F].delay {
          val keyBytes = key.getBytes
          val currentValue = transaction.getForUpdate(DEFAULT_READ_OPTIONS, dataColumnFamily, keyBytes, false)
          val result =
            if (currentValue == oldValue.getBytes) {
              transaction.put(dataColumnFamily, keyBytes, newValue.getBytes)
              true
            } else false
          transaction.commit()
          result
        }
      }
      .flatMap { result =>
        self
          .get(key) // The latest value at the moment
          .map(_.getOrElse(???) -> result) // Impossible situation
      }

}
