package ru.ololo.db.storage

trait ReplicationLog[F[_]] {
  def append(row: Row): F[Unit]
}

object ReplicationLog {
  def apply[F[_]](implicit F: ReplicationLog[F]): ReplicationLog[F] = F
}