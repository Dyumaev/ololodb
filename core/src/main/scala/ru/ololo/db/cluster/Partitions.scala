package ru.ololo.db.cluster

trait Partitions[F[_]] {
  def getPartition(key: String): F[String]
  def getPartitionReplicas(partition: String): F[List[String]]
}

object Partitions {
  def apply[F[_]](implicit F: Partitions[F]): Partitions[F] = F
}
