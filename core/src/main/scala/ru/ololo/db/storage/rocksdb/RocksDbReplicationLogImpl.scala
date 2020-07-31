package ru.ololo.db.storage.rocksdb

import cats.effect.Sync
import org.rocksdb.{ColumnFamilyHandle, RocksDB}
import ru.ololo.db.storage.Row
import ru.ololo.db.storage.ReplicationLog

class RocksDbReplicationLogImpl[F[_]: Sync](
  rocksDB: RocksDB,
  replicationLogColumnFamily: ColumnFamilyHandle
) extends ReplicationLog[F] {

  def append(row: Row): F[Unit] =
    Sync[F].delay {
      val keyBytes = s"${row.key}_${row.version}".getBytes
      rocksDB.put(replicationLogColumnFamily, DEFAULT_WRITE_OPTIONS, keyBytes, row.value.getBytes())
    }

  // TODO Some process traversing log and applying writes

}
