package com.wincom.dcim.sharded

import akka.actor.ActorSystem
import com.wincom.dcim.rest.ServiceSupport

object Main extends App with ServiceSupport {
  implicit val system = ActorSystem("dcim")

  val shardedFsus = system.actorOf(ShardedFsus.props, ShardedFsus.name)
  val shardedDrivers = system.actorOf(ShardedDrivers.props, ShardedDrivers.name)
  val shardedSignals = system.actorOf(ShardedSignals.props, ShardedSignals.name)

  startService(
    shardedFsus,
    shardedDrivers,
    shardedSignals
  )
}