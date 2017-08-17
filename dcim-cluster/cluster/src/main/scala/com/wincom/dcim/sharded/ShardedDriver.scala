package com.wincom.dcim.sharded

import akka.actor.{ActorRef, Props, ReceiveTimeout}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.Passivate
import com.wincom.dcim.domain.Driver._
import com.wincom.dcim.domain.{Driver, Settings}
import com.wincom.dcim.driver.DriverCodecRegistry

object ShardedDriver {

  def props(registry: DriverCodecRegistry) = Props(new ShardedDriver(registry))
  def name(driverId: String) = driverId.toString

  val shardName: String = "driver-shards"
  var numberOfShards = 100

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case cmd: Command =>
      (cmd.driverId.toString, cmd)
  }
  val extractShardId: ShardRegion.ExtractShardId = {
    case cmd: Command =>
      (cmd.driverId.hashCode % numberOfShards).toString
  }
}

class ShardedDriver(registry: DriverCodecRegistry) extends Driver(registry) {
  val settings = Settings(context.system)
  context.setReceiveTimeout(settings.actor.passivateTimeout)

  override def unhandled(message: Any): Unit = message match {
    case ReceiveTimeout =>
      context.parent ! Passivate(stopMessage = Driver.StopDriverCmd)
    case StopDriverCmd =>
      context.stop(self)
  }
}