package ru.ololo.db.grpc.stubs

trait GrpcStub[F[_], Rq, Rs] {
  def send(rq: Rq): F[Rs]
}
