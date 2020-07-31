package ru.ololo.db.etcd

import java.net.InetAddress
import java.nio.charset.StandardCharsets.UTF_8

import cats.syntax.all._
import cats.effect.Async
import io.etcd.jetcd.Watch.Listener
import io.etcd.jetcd.options.WatchOption
import io.etcd.jetcd.watch.WatchResponse
import io.etcd.jetcd.{ByteSequence, Election, Watch}
import ru.ololo.db.cluster.{Follower, Partitions}
import ru.ololo.db.utils.CatsExtensions.CompletableFutureCatsExtensions

class EtcdFollowerImpl[F[_]: Async: Partitions](
  election: Election,
  watch: Watch
) extends Follower[F] {

  def waitForOpportunity(partition: String): F[Unit] = {
    val partitionBytes = ByteSequence.from(partition.getBytes)
    election
      .leader(partitionBytes)
      .async[F]
      .flatMap { leaderKey =>
        val currentValue = leaderKey.getKv.getValue
        Async[F].async { cb =>
          watch.watch(
            partitionBytes,
            WatchOption.newBuilder().build(),
            new Listener {
              def onNext(response: WatchResponse): Unit = {
                val events = response.getEvents
                val lastValue = events.get(events.size()).getPrevKV.getValue
                if (currentValue == lastValue) cb(Right(()))
                else ()
              }
              def onError(throwable: Throwable): Unit = cb(Left(throwable))
              def onCompleted(): Unit = cb(Left(new Exception("Listener completed")))
            }
          )
        }
      }
  }

  def isFollower(key: String): F[Boolean] =
    Partitions[F]
      .getPartition(key)
      .flatMap { partition =>
        election
          .leader(ByteSequence.from(partition.getBytes))
          .async[F]
          .map(_.getKv.getValue.toString(UTF_8) != InetAddress.getLocalHost.getHostAddress)
      }

}
