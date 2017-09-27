package com.wincom.dcim.sharded

import akka.actor.{Actor, ActorRef, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.event.Logging
import com.wincom.dcim.domain.Settings
import com.wincom.dcim.message.common.Command

object ShardedDevices {
  def props = Props(new ShardedDevices)

  def name = "sharded-drivers"
}

class ShardedDevices extends Actor {
  val settings = Settings(context.system)
  ShardedDevice.numberOfShards = settings.actor.numberOfShards

  val log = Logging(context.system.eventStream, ShardedDevices.name)

  def shardedSignal(): ActorRef = {
    ClusterSharding(context.system).shardRegion(ShardedSignal.shardName)
  }

  def shardedAlarm(): ActorRef = {
    ClusterSharding(context.system).shardRegion(ShardedAlarm.shardName)
  }

  def shardedDevice(): ActorRef = {
    ClusterSharding(context.system).shardRegion(ShardedDevice.shardName)
  }

  ClusterSharding(context.system).start(
    ShardedDevice.shardName,
    ShardedDevice.props(shardedSignal, shardedAlarm, shardedDevice),
    ClusterShardingSettings(context.system),
    ShardedDevice.extractEntityId,
    ShardedDevice.extractShardId
  )

  override def receive: Receive = {
    case cmd: Command =>
      shardedDevice forward cmd
    case x => log.info("COMMAND: {} {}", this, x)
  }
}
