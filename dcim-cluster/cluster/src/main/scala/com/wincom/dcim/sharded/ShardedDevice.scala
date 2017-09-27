package com.wincom.dcim.sharded

import akka.actor.{ActorRef, Props}
import akka.cluster.sharding.ShardRegion
import com.wincom.dcim.domain.{Device, Settings}
import com.wincom.dcim.message.common.Command

import scala.math.Numeric.IntIsIntegral._

object ShardedDevice {
  def props(signalShard: () => ActorRef, alarmShard: () => ActorRef, deviceShard: () => ActorRef)  = Props(new ShardedDevice(signalShard, alarmShard, deviceShard))

  def name(driverId: String): String = driverId.toString

  val shardName: String = "driver-shards"
  var numberOfShards = 100

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case cmd: Command =>
      (cmd.entityId.toString, cmd)
  }
  val extractShardId: ShardRegion.ExtractShardId = {
    case cmd: Command =>
      (abs(cmd.entityId.hashCode) % numberOfShards).toString
  }
}

class ShardedDevice(signalShard: () => ActorRef, alarmShard: () => ActorRef, deviceShard: () => ActorRef) extends Device(signalShard, alarmShard, deviceShard) {
  val settings = Settings(context.system)
  context.setReceiveTimeout(settings.actor.passivateTimeout)

  override def unhandled(message: Any): Unit = message match {
    case x => log.info("unhandled COMMAND: {} {}", this, x)
  }
}
