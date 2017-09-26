package com.wincom.dcim.sharded

import akka.actor.{Actor, ActorRef, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.event.Logging
import com.wincom.dcim.domain.{EventNotifier, Settings}
import com.wincom.dcim.message.common.Command

object ShardedAlarmRecords {
  def props = Props(new ShardedAlarmRecords)
  def name = "sharded-alarm-records"

  val topicName = "alarm-events"
}

class ShardedAlarmRecords extends Actor {
  val log = Logging(context.system.eventStream, ShardedAlarmRecords.name)

  val settings = Settings(context.system)
  ShardedAlarmRecord.numberOfShards = settings.actor.numberOfShards

  ClusterSharding(context.system).start(
    ShardedAlarmRecord.shardName,
    ShardedAlarmRecord.props(notifier),
    ClusterShardingSettings(context.system),
    ShardedAlarmRecord.extractEntityId,
    ShardedAlarmRecord.extractShardId
  )

  def notifier(): ActorRef = {
    context.actorOf(EventNotifier.props(ShardedAlarmRecords.topicName), EventNotifier.name(ShardedAlarmRecords.topicName))
  }

  def shardedAlarmRecord(): ActorRef = {
    ClusterSharding(context.system).shardRegion(ShardedAlarmRecord.shardName)
  }

  override def receive: Receive = {
    case cmd: Command =>
      shardedAlarmRecord forward cmd
    case x => log.info("COMMAND: {} {}", this, x)
  }
}

