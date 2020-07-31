package ru.ololo.db.transport

trait Transport[F[_], Msg, Res] {
  def send(destination: String, message: Msg): F[Res]
}

object Transport {
  def apply[F[_], Msg, Res](implicit F: Transport[F, Msg, Res]): Transport[F, Msg, Res] = F
}
