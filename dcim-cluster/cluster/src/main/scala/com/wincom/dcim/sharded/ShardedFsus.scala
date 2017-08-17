package com.wincom.dcim.sharded

import akka.actor.{Actor, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.event.Logging
import com.wincom.dcim.fsu.{FsuCodecFactory, FsuCodecRegistry}
import com.wincom.dcim.rest.Settings
import com.wincom.dcim.sharded.FsuActor._

object ShardedFsus {
  def props = Props(new ShardedFsus)
  def name = "sharded-fsus"
}

class ShardedFsus extends Actor {

  val settings = Settings(context.system)
  context.setReceiveTimeout(settings.passivateTimeout)
  ShardedFsu.numberOfShards = settings.numberOfShards

  val log = Logging(context.system.eventStream, "sharded-fsus")
  val registry = (new FsuCodecRegistry).initialize()
  ClusterSharding(context.system).start(
    ShardedFsu.shardName,
    ShardedFsu.props(registry),
    ClusterShardingSettings(context.system),
    ShardedFsu.extractEntityId,
    ShardedFsu.extractShardId)

  def shardedFsu = {
    ClusterSharding(context.system).shardRegion(ShardedFsu.shardName)
  }

  override def receive: Receive = {
    case cmd: Command =>
      shardedFsu forward cmd
  }
}