package com.wincom.dcim.sharded

import akka.actor.{Actor, ActorRef, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.event.Logging
import com.wincom.dcim.domain.Fsu.Command
import com.wincom.dcim.domain.Settings
import com.wincom.dcim.fsu.FsuCodecRegistry

object ShardedFsus {
  def props = Props(new ShardedFsus)

  def name = "sharded-fsus"
}

class ShardedFsus extends Actor {

  val settings = Settings(context.system)
  ShardedFsu.numberOfShards = settings.actor.numberOfShards

  val log = Logging(context.system.eventStream, ShardedFsus.name)
  val registry: FsuCodecRegistry = (new FsuCodecRegistry).initialize()
  ClusterSharding(context.system).start(
    ShardedFsu.shardName,
    ShardedFsu.props(registry),
    ClusterShardingSettings(context.system),
    ShardedFsu.extractEntityId,
    ShardedFsu.extractShardId)

  def shardedFsu: ActorRef = {
    ClusterSharding(context.system).shardRegion(ShardedFsu.shardName)
  }

  override def receive: Receive = {
    case cmd: Command =>
      log.info("forwarded to: {} {}", shardedFsu, cmd)
      shardedFsu forward cmd
    case x => log.info("COMMAND: {} {}", this, x)
  }
}