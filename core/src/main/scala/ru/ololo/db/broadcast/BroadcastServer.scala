package ru.ololo.db.broadcast

trait BroadcastServer[F[_]] {
  def apply(message: Message): F[Result]
}

object BroadcastServer {
  def apply[F[_]](implicit F: BroadcastServer[F]): BroadcastServer[F] = F
}
