package ru.ololo.db.grpc

import cats.effect.{Effect, IO}
import cats.syntax.all._
import cats.{Applicative, MonadError}
import ru.ololo.db.broadcast.BroadcastClient
import ru.ololo.db.cluster.Cluster
import ru.ololo.db.kv.GetRequest
import ru.ololo.db.storage.DataStorage

import scala.concurrent.Future

class KVServer[F[_]: Effect: Cluster: DataStorage: BroadcastClient] extends KVGrpc.KV {
  def get(request: GetRequest): Future[GetResponse] =
    Effect[F].runAsync {
      Cluster[F]
        .canProcessRequests()
        .flatMap { canProcessRequests =>
          if (canProcessRequests)
            DataStorage[F]
              .get(request.key)
              .flatMap {
                case Some(row) => Applicative[F].pure(GetResponse(row.value))
                case None      => MonadError[F, Throwable].raiseError[GetResponse](new Exception("No data found"))
              }
          else MonadError[F, Throwable].raiseError[GetResponse](new Exception("Can't process requests"))
        }
    } {
      case Right(value) => Future.successful(value)
      case Left(error)  => Future.failed(error)
    }

  def put(request: PutRequest): Future[PutResponse] = ???

  def cas(request: CasRequest): Future[CasResponse] = ???
}
