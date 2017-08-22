package com.wincom.dcim.sharded

import akka.actor.{Actor, ActorRef, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.event.Logging
import com.wincom.dcim.domain.Settings
import com.wincom.dcim.domain.Signal.Command

/**
  * Created by wangxy on 17-8-17.
  */
object ShardedSignals {
  def props = Props(new ShardedSignals)

  def name = "sharded-signals"
}

class ShardedSignals extends Actor {
  val settings = Settings(context.system)
  ShardedSignal.numberOfShards = settings.actor.numberOfShards

  val log = Logging(context.system.eventStream, ShardedSignals.name)

  val shardedDriver: () => ActorRef = {
    () => ClusterSharding(context.system).shardRegion(ShardedDriver.shardName)
  }

  ClusterSharding(context.system).start(
    ShardedSignal.shardName,
    ShardedSignal.props(shardedDriver),
    ClusterShardingSettings(context.system),
    ShardedSignal.extractEntityId,
    ShardedSignal.extractShardId)

  def shardedSignal: ActorRef = {
    ClusterSharding(context.system).shardRegion(ShardedSignal.shardName)
  }

  override def receive: Receive = {
    case cmd: Command =>
      log.info("forwarded to: {} {}", shardedDriver, cmd)
      shardedSignal forward cmd
    case x => log.info("COMMAND: {} {}", this, x)
  }
}
