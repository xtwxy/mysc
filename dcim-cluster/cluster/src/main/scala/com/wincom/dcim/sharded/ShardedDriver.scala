package com.wincom.dcim.sharded

import akka.actor.{ActorRef, Props, ReceiveTimeout}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.Passivate
import com.wincom.dcim.domain.Driver._
import com.wincom.dcim.domain.{Driver, Settings}
import com.wincom.dcim.driver.DriverCodecRegistry

object ShardedDriver {

  def props(driverId: String, fsuShard: ActorRef, registry: DriverCodecRegistry) = Props(new ShardedDriver(driverId, fsuShard, registry))
  def name(driverId: String) = s"driver_$driverId"

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

class ShardedDriver(driverId: String, fsuShard: ActorRef, registry: DriverCodecRegistry) extends Driver(driverId, fsuShard, registry) {
  val settings = Settings(context.system)
  context.setReceiveTimeout(settings.passivateTimeout)

  override def unhandled(message: Any): Unit = message match {
    case ReceiveTimeout =>
      context.parent ! Passivate(stopMessage = Driver.StopDriverCmd)
    case StopDriverCmd =>
      context.stop(self)
  }
}