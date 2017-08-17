package com.wincom.dcim.sharded

import akka.actor.{Actor, ActorRef, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.event.Logging
import com.wincom.dcim.domain.Driver.Command
import com.wincom.dcim.domain.Settings
import com.wincom.dcim.driver.DriverCodecRegistry

object ShardedDrivers {
  def props = Props(new ShardedDrivers)

  def name = "sharded-drivers"
}

class ShardedDrivers extends Actor {
  val settings = Settings(context.system)
  context.setReceiveTimeout(settings.actor.passivateTimeout)
  ShardedDriver.numberOfShards = settings.actor.numberOfShards

  val log = Logging(context.system.eventStream, ShardedDrivers.name)
  val registry: DriverCodecRegistry = (new DriverCodecRegistry).initialize()

  ClusterSharding(context.system).start(
    ShardedDriver.shardName,
    ShardedDriver.props(registry),
    ClusterShardingSettings(context.system),
    ShardedDriver.extractEntityId,
    ShardedDriver.extractShardId
  )

  def shardedDriver: ActorRef = {
    ClusterSharding(context.system).shardRegion(ShardedDriver.shardName)
  }

  override def receive: Receive = {
    case cmd: Command =>
      shardedDriver forward cmd
  }
}