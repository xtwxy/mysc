package com.wincom.dcim.sharded

import akka.actor.{ActorRef, Props, ReceiveTimeout}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.Passivate
import com.wincom.dcim.domain.Signal.{Command, StopSignalCmd}
import com.wincom.dcim.domain.{Settings, Signal}
import com.wincom.dcim.signal.FunctionRegistry

import scala.math.Numeric.IntIsIntegral._

/**
  * Created by wangxy on 17-8-16.
  */
object ShardedSignal {
  def props(driverShard: () => ActorRef, registry: FunctionRegistry) = Props(new ShardedSignal(driverShard, registry))

  def name(signalId: String): String = signalId.toString

  val shardName: String = "signal-shards"
  var numberOfShards = 100

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case cmd: Command =>
      (cmd.id.toString, cmd)
  }
  val extractShardId: ShardRegion.ExtractShardId = {
    case cmd: Command =>
      (abs(cmd.id.hashCode) % numberOfShards).toString
  }
}

class ShardedSignal(driverShard: () => ActorRef, registry: FunctionRegistry) extends Signal(driverShard, registry) {
  val settings = Settings(context.system)
  context.setReceiveTimeout(settings.actor.passivateTimeout)

  override def unhandled(message: Any): Unit = message match {
    case ReceiveTimeout =>
      context.parent ! Passivate(stopMessage = Signal.StopSignalCmd)
    case StopSignalCmd =>
      context.stop(self)
  }
}
