package ru.ololo.db.utils

import cats.effect.Concurrent
import fs2.Stream
import fs2.concurrent.Queue

trait Majority[F[_]] {
  def majority[T](fs: List[F[T]]): F[List[T]]
}

object Majority {

  def apply[F[_]](implicit F: Majority[F]): Majority[F] = F

  class MajorityFs2Impl[F[_]](implicit C: Concurrent[F]) extends Majority[F] {
    def majority[T](fs: List[F[T]]): F[List[T]] =
      Stream
        .eval(Queue.unbounded[F, T])
        .flatMap { queue =>
          queue.dequeue
            .take(fs.size - (fs.size / 2))
            .concurrently(Stream.emits(fs.map(f => Stream.eval(f).through(queue.enqueue))))
        }
        .compile
        .toList
  }

  implicit class Syntax[F[_], T](val fs: List[F[T]]) extends AnyVal {
    def majority(implicit M: Majority[F]): F[List[T]] = M.majority(fs)
  }

}

//object Majority extends IOApp {
//
//  def run(args: List[String]): IO[ExitCode] =
//    Stream
//      .eval(Queue.unbounded[IO, Int])
//      .flatMap { queue =>
//        val stream: Stream[Pure, Stream[IO, Int]] = Stream(
//          Stream.eval(IO(1)),
//          Stream.eval(IO(2)),
//          Stream.eval(IO.sleep(100 seconds).map { _ =>
//            println("3 ready"); 3
//          }),
//          Stream.eval(IO.sleep(10 seconds).map { _ =>
//            println("4 ready"); 4
//          }),
//          Stream.eval(IO.sleep(100 seconds).map { _ =>
//            println("5 ready"); 5
//          })
//        )
//
//        queue.dequeue
//          .take(3)
//          .concurrently(stream.parJoinUnbounded.through(queue.enqueue))
//      }
//      .compile
//      .toList
//      .map(i => println(i))
//      .as(ExitCode.Success)
//
//}
