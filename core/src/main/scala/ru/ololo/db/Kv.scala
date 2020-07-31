package ru.ololo.db

import cats.syntax.all._
import cats.MonadError
import cats.effect.Concurrent
import ru.ololo.db.broadcast.{BroadcastClient, Message}
import ru.ololo.db.cluster.Leader
import ru.ololo.db.storage.DataStorage

class Kv[F[_]: Concurrent: Leader: DataStorage: BroadcastClient] {

  def get(key: String): F[Option[String]] =
    Leader[F]
      .isLeader(key)
      .flatMap { canProcessRequests =>
        if (canProcessRequests)
          DataStorage[F]
            .get(key)
            .flatMap {
              case Some(row) =>
                Concurrent[F]
                  .start(BroadcastClient[F].apply(Message(row.key, row.value, row.version))) // read-repair fashion
                  .map(_ => Some(row.value))
              case None => MonadError[F, Throwable].raiseError(new Exception("No data found"))
            }
        else MonadError[F, Throwable].raiseError(new Exception("Can't process requests"))
      }

  def put(key: String, value: String): F[Unit] =
    Leader[F]
      .isLeader(key)
      .flatMap { canProcessRequests =>
        if (canProcessRequests)
          DataStorage[F]
            .put(key, value)
            .flatMap(row => BroadcastClient[F].apply(Message(row.key, row.value, row.version)))
            .map(_ => ())
        else MonadError[F, Throwable].raiseError(new Exception("Can't process requests"))
      }

  def cas(key: String, oldValue: String, newValue: String): F[Boolean] =
    Leader[F]
      .isLeader(key)
      .flatMap { canProcessRequests =>
        if (canProcessRequests)
          DataStorage[F]
            .cas(key, oldValue, newValue)
            .flatMap {
              case (row, replaced) =>
                BroadcastClient[F]
                  .apply(Message(row.key, row.value, row.version))
                  .map(_ => replaced)
            }
        else MonadError[F, Throwable].raiseError(new Exception("Can't process requests"))
      }

}
