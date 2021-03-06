package com.wincom.dcim.sharded

import akka.actor.{ActorRef, Props, ReceiveTimeout}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.Passivate
import com.wincom.dcim.domain.{Driver, Settings}
import com.wincom.dcim.driver.DriverCodecRegistry
import com.wincom.dcim.message.common.Command
import com.wincom.dcim.message.driver.StopDriverCmd

import scala.math.Numeric.IntIsIntegral._

object ShardedDriver {

  def props(shardedSignal: () => ActorRef, registry: DriverCodecRegistry) = Props(new ShardedDriver(shardedSignal, registry))

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

class ShardedDriver(shardedSignal: () => ActorRef, registry: DriverCodecRegistry) extends Driver(shardedSignal, registry) {
  val settings = Settings(context.system)
  context.setReceiveTimeout(settings.actor.passivateTimeout)

  override def unhandled(message: Any): Unit = message match {
    case ReceiveTimeout =>
      context.parent ! Passivate(stopMessage = StopDriverCmd)
    case StopDriverCmd =>
      context.stop(self)
    case x => log.info("unhandled COMMAND: {} {}", this, x)
  }
}