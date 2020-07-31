package ru.ololo.db.utils

import java.util.concurrent.CompletableFuture

import cats.effect.Async

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object CatsExtensions {

  implicit class FutureCatsExtensions[T](f: => Future[T]) {
    def async[F[_]](implicit F: Async[F], ctx: ExecutionContext): F[T] =
      F.async { cb =>
        f.onComplete {
          case Success(value) => cb(Right(value))
          case Failure(error) => cb(Left(error))
        }
      }
  }

  implicit class CompletableFutureCatsExtensions[T](completableFuture: => CompletableFuture[T]) {
    def async[F[_]](implicit F: Async[F]): F[T] =
      F.async { cb =>
        completableFuture.whenComplete {
          case (rs, null) => cb(Right(rs))
          case (null, th) => cb(Left(th))
        }
      }
  }

}
