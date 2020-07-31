package ru.ololo.db.broadcast.impl

import cats.FlatMap
import cats.syntax.all._
import ru.ololo.db.broadcast.Result.{Failure, Success}
import ru.ololo.db.broadcast.{BroadcastClient, Message, Result}
import ru.ololo.db.cluster.Partitions
import ru.ololo.db.transport.Transport
import ru.ololo.db.utils.{Majority, Retry}
import ru.ololo.db.utils.Majority.Syntax
import ru.ololo.db.utils.Retry.Syntax

import scala.util.control.NonFatal
import scala.concurrent.duration._

class BestEffortBroadcastClientImpl[F[_]: Retry: Majority: FlatMap: Partitions: Transport[*, Message, Result]]
    extends BroadcastClient[F] {

  def apply(message: Message): F[Result] =
    Partitions[F]
      .getPartition(message.key)
      .flatMap(partition => Partitions[F].getPartitionReplicas(partition))
      .flatMap { replicas =>
        replicas
          .map { replica =>
            Transport[F, Message, Result]
              .send(replica, message)
              .retry(1 second, _ + (1 second)) {
                case Left(th)          => NonFatal(th)
                case Right(Failure(_)) => true
                case _                 => false
              }
          }
          .majority
          .map(_ => Success())
      }

}
