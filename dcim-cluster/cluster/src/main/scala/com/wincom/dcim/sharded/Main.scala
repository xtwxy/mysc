package com.wincom.dcim.sharded

import akka.actor.ActorSystem
import com.wincom.dcim.rest.WebServer

object Main extends App with WebServer {
  implicit val system = ActorSystem("dcim")

  val shardedFsus = system.actorOf(ShardedFsus.props, ShardedFsus.name)
  startService(shardedFsus)
}