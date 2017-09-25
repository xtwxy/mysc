package com.wincom.dcim.sharded

import akka.actor.{Actor, ActorRef, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.event.Logging
import com.wincom.dcim.domain.Settings
import com.wincom.dcim.driver.DriverCodecRegistry
import com.wincom.dcim.message.common.Command
import com.wincom.dcim.message.driver._

import scala.collection.convert.ImplicitConversions._

object ShardedDrivers {
  def props = Props(new ShardedDrivers)

  def name = "sharded-drivers"
}

class ShardedDrivers extends Actor {
  val settings = Settings(context.system)
  ShardedDriver.numberOfShards = settings.actor.numberOfShards

  val log = Logging(context.system.eventStream, ShardedDrivers.name)
  val registry: DriverCodecRegistry = (new DriverCodecRegistry(log)).initialize()

  def shardedSignal(): ActorRef = {
    ClusterSharding(context.system).shardRegion(ShardedSignal.shardName)
  }

  ClusterSharding(context.system).start(
    ShardedDriver.shardName,
    ShardedDriver.props(shardedSignal, registry),
    ClusterShardingSettings(context.system),
    ShardedDriver.extractEntityId,
    ShardedDriver.extractShardId
  )

  def shardedDriver: ActorRef = {
    ClusterSharding(context.system).shardRegion(ShardedDriver.shardName)
  }

  override def receive: Receive = {
    case _: GetSupportedModelsCmd =>
      sender() ! SupportedModelsVo(registry.names().toMap)
    case GetModelParamsCmd(modelName) =>
      sender() ! ModelParamsVo(registry.paramOptions(modelName).toSeq)
    case cmd: Command =>
      shardedDriver forward cmd
    case x => log.info("COMMAND: {} {}", this, x)
  }
}