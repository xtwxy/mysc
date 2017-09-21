package com.wincom.dcim.sharded

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import com.wincom.dcim.domain.Settings
import com.wincom.dcim.message.common.Command
import com.wincom.dcim.message.signal._
import com.wincom.dcim.signal.FunctionRegistry

import scala.collection.convert.ImplicitConversions._
/**
  * Created by wangxy on 17-8-17.
  */
object ShardedSignals {
  def props = Props(new ShardedSignals)

  def name = "sharded-signals"
}

class ShardedSignals extends Actor with ActorLogging {
  val settings = Settings(context.system)
  ShardedSignal.numberOfShards = settings.actor.numberOfShards

  val registry: FunctionRegistry = (new FunctionRegistry(log)).initialize()

  ClusterSharding(context.system).start(
    ShardedSignal.shardName,
    ShardedSignal.props(shardedDriver, registry),
    ClusterShardingSettings(context.system),
    ShardedSignal.extractEntityId,
    ShardedSignal.extractShardId)

  def shardedDriver(): ActorRef = {
    ClusterSharding(context.system).shardRegion(ShardedDriver.shardName)
  }

  def shardedSignal: ActorRef = {
    ClusterSharding(context.system).shardRegion(ShardedSignal.shardName)
  }

  override def receive: Receive = {
    case _: GetSupportedFuncsCmd =>
      sender() ! SupportedFuncsVo(registry.names().toSeq)
    case GetFuncParamsCmd(modelName) =>
      sender() ! FuncParamsVo(registry.paramNames(modelName).toSeq)
    case cmd: Command =>
      log.info("forwarded to: {} {}", shardedSignal, cmd)
      shardedSignal forward cmd
    case x => log.info("COMMAND: {} {}", this, x)
  }
}
