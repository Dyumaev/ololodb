package ru.ololo.db.grpc

import cats.Functor
import cats.syntax.all._
import ru.ololo.db.grpc.stubs.GrpcStub
import ru.ololo.db.transport.{Decoder, Encoder, Transport}

class GrpcInternalTransport[F[_]: Functor, MsgIn, ResOut, MsgOut, ResIn](
  encoder: Encoder[MsgIn, MsgOut],
  decoder: Decoder[ResIn, ResOut],
  grpc: GrpcStub[F, MsgOut, ResIn]
) extends Transport[F, MsgIn, ResOut] {

  def send(destination: String, message: MsgIn): F[ResOut] =
    grpc
      .send(encoder.encode(message))
      .map(response => decoder.decode(response))

}
