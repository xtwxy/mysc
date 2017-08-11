package com.wincom.dcim.sharded

import akka.actor.{Props, ReceiveTimeout}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.event.Logging
import akka.persistence.{PersistentActor, SnapshotOffer}
import com.wincom.dcim.rest.Settings
import com.wincom.dcim.sharded.FsuActor._

import scala.collection.immutable.Set

object ShardedFsus {
  def props = Props(new ShardedFsus)

  def name = "sharded-fsus"
}

class ShardedFsus extends PersistentActor {

  val settings = Settings(context.system)
  context.setReceiveTimeout(settings.passivateTimeout)
  FsuActor.numberOfShards = settings.numberOfShards

  val log = Logging(context.system.eventStream, "sharded-fsus")

  ClusterSharding(context.system).start(
    ShardedFsus.name,
    Props(new SuppervisedFsu),
    ClusterShardingSettings(context.system),
    FsuActor.extractEntityId,
    FsuActor.extractShardId)
  var config = Set[Int]()
  var isDirty = false

  override def persistenceId: String = s"${self.path.name}"

  def receiveRecover = {
    case cmd@CreateFsu(fsuId, fsuName) =>
      updateState(cmd)
      shardedFsu ! Ping(fsuId)
    case SnapshotOffer(_, FsuIds(ids)) =>
      config = ids
      log.info("RECOVER: {} {}", this, ids)
    case x => log.info("RECOVER: {} {}", this, x)
  }

  def receiveCommand = {
    case cmd@CreateFsu(fsuId, fsuName) =>
      persist(cmd)(updateState)
      isDirty = true
      shardedFsu forward cmd
    case x: ReceiveTimeout =>
      log.info("COMMAND: {} {}", this, x)
      if (isDirty) {
        saveSnapshot(FsuIds(config))
        isDirty = false
      }
      for (fsuId <- (config)) {
        log.info("PING: {} {}", this, fsuId)
        shardedFsu ! Ping(fsuId)
      }
    case x => {
      log.info("{}: forwarding message '{}' to {}", this, x, shardedFsu)
      shardedFsu forward (x)
    }
  }

  def shardedFsu = {
    ClusterSharding(context.system).shardRegion(ShardedFsus.name)
  }

  private def updateState: (Command => Unit) = {
    case CreateFsu(fsuId, fsuName) =>
      if (!config(fsuId)) {
        config += fsuId
      }
  }

  case class FsuIds(ids: Set[Int])
}