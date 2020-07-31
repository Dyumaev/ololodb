package ru.ololo.db.storage.rocksdb

import cats.effect._
import cats.syntax.all._
import org.rocksdb._
import ru.ololo.db.old.RocksDbStorage

// It might fail to initialize rocksDb because default CF descriptor is not created
class RocksDbFactory[Init[_], F[_]](implicit Init: Sync[Init], F: Sync[F]) {

  def create(path: String): Init[Resource[Init, RocksDbStorage[F]]] =
    Init.delay(RocksDB.loadLibrary()).map { _ =>
      for {
        options <- new Options()
        rocksDb <- RocksDB.open(options, path)
        dataColumnFamilyOptions <- new ColumnFamilyOptions().optimizeUniversalStyleCompaction().setComparator()
        versionsColumnFamilyOptions <- new ColumnFamilyOptions()
          .optimizeUniversalStyleCompaction()
          .setMergeOperatorName("uint64add")
        dataColumnFamilyDescriptor = new ColumnFamilyDescriptor("data".getBytes, dataColumnFamilyOptions)
        versionsColumnFamilyDescriptor = new ColumnFamilyDescriptor("versions".getBytes, versionsColumnFamilyOptions)
        dataColumnFamily <- rocksDb.createColumnFamily(dataColumnFamilyDescriptor)
        versionsColumnFamily <- rocksDb.createColumnFamily(versionsColumnFamilyDescriptor)
      } yield new RocksDbStorage[F](rocksDb, dataColumnFamily, versionsColumnFamily)
    }

  implicit def toResource[R <: AbstractNativeReference](
    reference: R
  ): Resource[Init, R] = Resource.fromAutoCloseable(Init.delay(reference))

}
