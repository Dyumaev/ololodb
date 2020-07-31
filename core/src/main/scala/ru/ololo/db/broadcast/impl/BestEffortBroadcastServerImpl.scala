package ru.ololo.db.broadcast.impl

import cats.MonadError
import cats.syntax.all._
import ru.ololo.db.broadcast.Result.Success
import ru.ololo.db.broadcast.{BroadcastServer, Message, Result}
import ru.ololo.db.cluster.Follower
import ru.ololo.db.storage.{ReplicationLog, Row}

class BestEffortBroadcastServerImpl[F[_]: MonadError[*, Throwable]: ReplicationLog: Follower]
    extends BroadcastServer[F] {

  def apply(message: Message): F[Result] =
    Follower[F]
      .isFollower(message.key)
      .flatMap { canReplicateRequest =>
        if (canReplicateRequest)
          ReplicationLog[F]
            .append(Row(message.key, message.value, message.version))
            .map(_ => Success())
        else MonadError[F, Throwable].raiseError(new Exception("Can't replicate request"))
      }

}
