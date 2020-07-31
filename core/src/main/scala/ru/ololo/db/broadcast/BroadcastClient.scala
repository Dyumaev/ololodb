package ru.ololo.db.broadcast

trait BroadcastClient[F[_]] {
  def apply(message: Message): F[Result]
}

object BroadcastClient {
  def apply[F[_]](implicit F: BroadcastClient[F]): BroadcastClient[F] = F
}
