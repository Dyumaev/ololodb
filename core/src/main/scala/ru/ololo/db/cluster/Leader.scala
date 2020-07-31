package ru.ololo.db.cluster

trait Leader[F[_]] {
  type Leadership
  def tryBecome(partition: String): F[Option[Leadership]]
  def tryKeep(leadership: Leadership): F[Unit]
  def cancel(leadership: Leadership): F[Unit]
  def isLeader(key: String): F[Boolean]
}

object Leader {
  def apply[F[_]](implicit F: Leader[F]): Leader[F] = F
}
