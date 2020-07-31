package ru.ololo.db

import fs2.Stream
import cats.syntax.all._
import cats.effect.Effect
import ru.ololo.db.cluster.{Follower, Leader}

class Lifecycle[F[_]: Effect: Leader: Follower] {

  def run(): F[Unit] =
    election("all").repeat.compile.drain

  private def election(partition: String): Stream[F, Unit] = Stream.eval {
    Leader[F]
      .tryBecome(partition)
      .flatMap {
        case Some(leadership) =>
          Leader[F]
            .tryKeep(leadership)
            .handleErrorWith { _ =>
              Leader[F]
                .cancel(leadership)
                .flatMap(_ => Follower[F].waitForOpportunity(partition))
            }
        case None => Follower[F].waitForOpportunity(partition)
      }
  }

}
