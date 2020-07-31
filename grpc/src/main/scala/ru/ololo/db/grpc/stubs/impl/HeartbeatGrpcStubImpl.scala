package ru.ololo.db.grpc.stubs.impl

import cats.effect.Async
import cats.syntax.all._
import ru.ololo.db.grpc.stubs.{GrpcStub, GrpcStubsHolder}
import ru.ololo.db.internal._
import ru.ololo.db.utils.CatsExtensions.FutureCatsExtensions

import scala.concurrent.ExecutionContext

class HeartbeatGrpcStubImpl[F[_]: Async: GrpcStubsHolder](
  implicit context: ExecutionContext
) extends GrpcStub[F, HeartbeatRequest, HeartbeatResponse] {

  def send(rq: HeartbeatRequest): F[HeartbeatResponse] =
    GrpcStubsHolder[F]
      .forEndpoint(rq.to)
      .flatMap { stub =>
        stub
          .heartbeat(rq)
          .async[F]
      }

}
