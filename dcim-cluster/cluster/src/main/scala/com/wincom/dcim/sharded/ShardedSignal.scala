package com.wincom.dcim.sharded

import akka.actor.{ActorRef, Props, ReceiveTimeout}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.Passivate
import com.wincom.dcim.domain.Signal.{Command, StopSignalCmd}
import com.wincom.dcim.domain.{Settings, Signal}

/**
  * Created by wangxy on 17-8-16.
  */
object ShardedSignal {
  def props(signalId: String, fsuShard: ActorRef) = Props(new ShardedSignal(signalId, fsuShard))
  def name(signalId: String) = s"signal_$signalId"

  val shardName: String = "signal-shards"
  var numberOfShards = 100

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case cmd: Command =>
      (cmd.signalId.toString, cmd)
  }
  val extractShardId: ShardRegion.ExtractShardId = {
    case cmd: Command =>
      (cmd.signalId.hashCode % numberOfShards).toString
  }
}

class ShardedSignal(signalId: String, signalShard: ActorRef) extends Signal(signalId, signalShard) {
  val settings = Settings(context.system)
  context.setReceiveTimeout(settings.passivateTimeout)

  override def unhandled(message: Any): Unit = message match {
    case ReceiveTimeout =>
      context.parent ! Passivate(stopMessage = Signal.StopSignalCmd)
    case StopSignalCmd =>
      context.stop(self)
  }
}
