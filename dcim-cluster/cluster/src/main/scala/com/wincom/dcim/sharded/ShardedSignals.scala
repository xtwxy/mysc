package com.wincom.dcim.sharded

import akka.actor.{Actor, ActorRef, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.event.Logging
import com.wincom.dcim.domain.Settings
import com.wincom.dcim.domain.Signal.Command

/**
  * Created by wangxy on 17-8-17.
  */
object ShardedSignals {
  def props(shardedDriver: () => ActorRef) = Props(new ShardedSignals(shardedDriver))
  def name = "sharded-signals"
}

class ShardedSignals(shardedDriver: () => ActorRef) extends Actor {
  val settings = Settings(context.system)
  context.setReceiveTimeout(settings.actor.passivateTimeout)
  ShardedSignal.numberOfShards = settings.actor.numberOfShards

  val log = Logging(context.system.eventStream, ShardedSignals.name)

  ClusterSharding(context.system).start(
    ShardedSignal.shardName,
    ShardedSignal.props(shardedDriver),
    ClusterShardingSettings(context.system),
    ShardedSignal.extractEntityId,
    ShardedSignal.extractShardId)

  def shardedSignal = {
    ClusterSharding(context.system).shardRegion(ShardedSignal.shardName)
  }

  override def receive: Receive = {
    case cmd: Command =>
      shardedSignal forward cmd
  }
}
