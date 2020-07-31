package ru.ololo.db.grpc.stubs

import cats.Applicative
import cats.effect.{Concurrent, Sync}
import cats.syntax.all._
import io.grpc.ManagedChannelBuilder
import ru.ololo.db.internal._
import ru.ololo.db.utils.Mutex

import scala.collection.concurrent.TrieMap

class GrpcStubsHolder[F[_]: Concurrent] {

  private val locks = TrieMap[String, Mutex[F]]()
  private val cache = TrieMap[String, InternalGrpc.InternalStub]()

  def forEndpoint(endpoint: String): F[InternalGrpc.InternalStub] =
    cache
      .get(endpoint)
      .map(Applicative[F].pure)
      .getOrElse {
        Mutex[F]().flatMap { mutex =>
          initializeExactlyOneStub(endpoint) {
            locks
              .putIfAbsent(endpoint, mutex)
              .getOrElse(mutex)
          }
        }
      }

  private def initializeExactlyOneStub(endpoint: String)(mutex: Mutex[F]): F[InternalGrpc.InternalStub] =
    mutex.acquire.flatMap { _ =>
      cache
        .get(endpoint)
        .map(stub => mutex.release.map(_ => stub))
        .getOrElse(initializeStub(endpoint))
    }

  private def initializeStub(endpoint: String): F[InternalGrpc.InternalStub] =
    Sync[F].delay {
      val channel = ManagedChannelBuilder.forTarget(endpoint).build()
      InternalGrpc.stub(channel)
    }

}

object GrpcStubsHolder {
  def apply[F[_]](implicit F: GrpcStubsHolder[F]): GrpcStubsHolder[F] = F
}
