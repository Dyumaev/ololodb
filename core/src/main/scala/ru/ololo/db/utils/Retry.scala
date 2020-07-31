package ru.ololo.db.utils

import cats.effect.{Concurrent, Timer}
import fs2.Stream

import scala.concurrent.duration.FiniteDuration

trait Retry[F[_]] {
  def retry[T](f: F[T])(
    delay: FiniteDuration,
    nextDelay: FiniteDuration => FiniteDuration,
    maxAttempts: Option[Int],
    handler: Either[Throwable, T] => Boolean
  ): F[T]
}

object Retry {

  def apply[F[_]](implicit F: Retry[F]): Retry[F] = F

  class RetryFs2Impl[F[_]](implicit C: Concurrent[F], T: Timer[F]) extends Retry[F] {
    def retry[T](f: F[T])(
      delay: FiniteDuration,
      nextDelay: FiniteDuration => FiniteDuration,
      maxAttempts: Option[Int],
      handler: Either[Throwable, T] => Boolean
    ): F[T] = {
      val delays =
        Stream
          .unfold(delay)(d => Some(d -> nextDelay(d)))
          .covary[F]

      maxAttempts
        .map { attempts =>
          Stream
            .eval(f)
            .attempts(delays)
            .take(attempts)
        }
        .getOrElse {
          Stream
            .eval(f)
            .attempts(delays)
        }
        .takeThrough(result => handler(result))
        .last
        .map(_.get)
        .rethrow
        .compile
        .lastOrError // TODO Better error
    }
  }

  implicit class Syntax[F[_], T](val f: F[T]) extends AnyVal {
    def retry(
      delay: FiniteDuration,
      nextDelay: FiniteDuration => FiniteDuration,
      maxAttempts: Option[Int] = None
    )(handler: Either[Throwable, T] => Boolean)(implicit R: Retry[F]): F[T] =
      R.retry(f)(delay, nextDelay, maxAttempts, handler)
  }

}
