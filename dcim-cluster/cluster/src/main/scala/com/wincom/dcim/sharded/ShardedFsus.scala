package com.wincom.dcim.sharded

import akka.actor.{Actor, ActorRef, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.event.Logging
import com.wincom.dcim.domain.Settings
import com.wincom.dcim.fsu.FsuCodecRegistry
import com.wincom.dcim.message.common.Command
import com.wincom.dcim.message.fsu._

import scala.collection.convert.ImplicitConversions._

object ShardedFsus {
  def props = Props(new ShardedFsus)

  def name = "sharded-fsus"
}

class ShardedFsus extends Actor {

  val settings = Settings(context.system)
  ShardedFsu.numberOfShards = settings.actor.numberOfShards

  val log = Logging(context.system.eventStream, ShardedFsus.name)
  val registry: FsuCodecRegistry = (new FsuCodecRegistry(log)).initialize()

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
    case _: GetSupportedModelsCmd =>
      sender() ! SupportedModelsVo(registry.names().toSeq)
    case GetModelParamsCmd(modelName) =>
      sender() ! ModelParamsVo(registry.paramNames(modelName).toSeq)
    case cmd: Command =>
      shardedFsu forward cmd
    case x => log.info("COMMAND: {} {}", this, x)
  }
}