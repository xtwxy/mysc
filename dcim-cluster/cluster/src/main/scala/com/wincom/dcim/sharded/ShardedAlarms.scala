package com.wincom.dcim.sharded

import akka.actor._
import akka.cluster.sharding._
import com.wincom.dcim.domain.Settings
import com.wincom.dcim.message.common.Command
import com.wincom.dcim.message.signal.{FuncParamsVo, GetFuncParamsCmd, GetSupportedFuncsCmd, SupportedFuncsVo}
import com.wincom.dcim.signal.FunctionRegistry

import scala.collection.convert.ImplicitConversions._

object ShardedAlarms {
  def props = Props(new ShardedAlarms)

  def name = "sharded-alarms"
}

class ShardedAlarms extends Actor with ActorLogging {
  val settings = Settings(context.system)
  ShardedAlarm.numberOfShards = settings.actor.numberOfShards

  val registry: FunctionRegistry = (new FunctionRegistry(log)).initialize()

  ClusterSharding(context.system).start(
    ShardedAlarm.shardName,
    ShardedAlarm.props(shardedSignal, shardedAlarmRecord, registry),
    ClusterShardingSettings(context.system),
    ShardedAlarm.extractEntityId,
    ShardedAlarm.extractShardId
  )

  def shardedSignal(): ActorRef = {
    ClusterSharding(context.system).shardRegion(ShardedSignal.shardName)
  }

  def shardedAlarmRecord(): ActorRef = {
    ClusterSharding(context.system).shardRegion(ShardedAlarmRecord.shardName)
  }

  def shardedAlarm(): ActorRef = {
    ClusterSharding(context.system).shardRegion(ShardedAlarm.shardName)
  }

  override def receive: Receive = {
    case _: GetSupportedFuncsCmd =>
      sender() ! SupportedFuncsVo(registry.names().toSeq)
    case GetFuncParamsCmd(modelName) =>
      sender() ! FuncParamsVo(registry.paramNames(modelName).toSeq)
    case cmd: Command =>
      log.info("forwarded to: {} {}", shardedAlarm, cmd)
      shardedAlarm forward cmd
    case x => log.info("COMMAND: {} {}", this, x)
  }
}