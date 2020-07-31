package ru.ololo.db.utils

import cats.syntax.all._
import cats.effect.Concurrent
import cats.effect.concurrent.MVar

final class Mutex[F[_]: Concurrent](mvar: MVar[F, Unit]) {
  def acquire: F[Unit] = mvar.take
  def release: F[Unit] = mvar.put(())
}

object Mutex {
  def apply[F[_]: Concurrent](): F[Mutex[F]] = MVar[F].of(()).map(ref => new Mutex(ref))
}
