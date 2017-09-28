package com.wincom.dcim.sharded

import akka.actor.ActorSystem
import com.wincom.dcim.rest.ServiceSupport

object Main extends App with ServiceSupport {
  implicit val system = ActorSystem("dcim")

  val shardedFsus = system.actorOf(ShardedFsus.props, ShardedFsus.name)
  val shardedDevices = system.actorOf(ShardedDevices.props, ShardedDevices.name)
  val shardedDrivers = system.actorOf(ShardedDrivers.props, ShardedDrivers.name)
  val shardedSignals = system.actorOf(ShardedSignals.props, ShardedSignals.name)
  val shardedAlarms = system.actorOf(ShardedAlarms.props, ShardedAlarms.name)
  val shardedAlarmRecords = system.actorOf(ShardedAlarmRecords.props, ShardedAlarmRecords.name)
  val shardedNotifiler = system.actorOf(ShardedNotifier.props, ShardedNotifier.name("alarm-publish"))

  startService(
    shardedFsus,
    shardedDevices,
    shardedDrivers,
    shardedSignals,
    shardedAlarms,
    shardedAlarmRecords
  )
}