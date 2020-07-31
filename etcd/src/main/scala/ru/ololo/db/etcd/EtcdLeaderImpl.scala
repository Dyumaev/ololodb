package ru.ololo.db.etcd

import java.net.InetAddress
import java.nio.charset.StandardCharsets.UTF_8

import cats.syntax.all._
import cats.effect.{Concurrent, Timer}
import fs2.Stream
import io.etcd.jetcd.election.LeaderKey
import io.etcd.jetcd.{ByteSequence, Election, Lease}
import ru.ololo.db.cluster.{Heartbeat, Leader, Partitions, Result}
import ru.ololo.db.transport.Transport
import ru.ololo.db.utils.{Majority, Retry}
import ru.ololo.db.utils.Retry.Syntax
import ru.ololo.db.utils.Majority.Syntax
import ru.ololo.db.utils.CatsExtensions.CompletableFutureCatsExtensions

import scala.concurrent.duration._
import scala.util.control.NonFatal

class EtcdLeaderImpl[F[_]: Concurrent: Timer: Retry: Majority: Partitions: Transport[*, Heartbeat, Result]](
  election: Election,
  lease: Lease
) extends Leader[F] {

  type Leadership = LeaderKey

  def tryBecome(partition: String): F[Option[Leadership]] = {
    val electionName = ByteSequence.from(partition.getBytes())
    val proposal = ByteSequence.from(InetAddress.getLocalHost.getHostAddress.getBytes())
    for {
      lease <- lease.grant(30).async[F]
      result <- election.campaign(electionName, lease.getID, proposal).async[F]
    } yield Some(result.getLeader)
  }

  def tryKeep(leadership: Leadership): F[Unit] = {
    val heartbeat =
      for {
        followers <- Partitions[F].getPartitionReplicas(leadership.getName.toString(UTF_8))
        leader = InetAddress.getLocalHost.getHostAddress
        f1 <- Concurrent[F].start(lease.keepAliveOnce(leadership.getLease).async[F])
        f2 <- Concurrent[F].start {
          followers
            .map(follower => Transport[F, Heartbeat, Result].send(follower, Heartbeat(leader, follower)))
            .majority
        }
        _ <- f1.join
        _ <- f2.join
      } yield ()

    val retryHeartbeat =
      heartbeat.retry(1 second, _ + (1 second), Some(2)) {
        case Left(th) => NonFatal(th)
        case _        => false
      }

    (Stream.eval(retryHeartbeat) ++ Stream.sleep(30 seconds)).repeat.compile.drain
  }

  def cancel(leadership: Leadership): F[Unit] =
    election
      .resign(leadership)
      .async[F]
      .map(_ => ())

  def isLeader(key: String): F[Boolean] =
    Partitions[F]
      .getPartition(key)
      .flatMap { partition =>
        election
          .leader(ByteSequence.from(partition.getBytes))
          .async[F]
          .map(_.getKv.getValue.toString(UTF_8) == InetAddress.getLocalHost.getHostAddress)
      }

}
