package ru.ololo.db.cluster

trait Follower[F[_]] {
  def waitForOpportunity(partition: String): F[Unit]
  def isFollower(key: String): F[Boolean]
}

object Follower {
  def apply[F[_]](implicit F: Follower[F]): Follower[F] = F
}
