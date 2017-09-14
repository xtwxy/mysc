package com.wincom.dcim.sharded

import java.lang.Math._

import akka.actor._
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion._
import com.wincom.dcim.domain.{Alarm, Settings}
import com.wincom.dcim.message.alarm.PassivateAlarmCmd
import com.wincom.dcim.message.common.Command
import com.wincom.dcim.signal.FunctionRegistry

object ShardedAlarm {
  def props(signalShard: () => ActorRef,
            alarmRecordShard: () => ActorRef,
            registry: FunctionRegistry) = Props(new ShardedAlarm(signalShard, alarmRecordShard, registry))
  def name(alarmId: String) = alarmId

  val shardName = "alarm-shard"
  var numberOfShards = 100

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case cmd: Command =>
      (cmd.id, cmd)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case cmd: Command =>
      (abs(cmd.id.hashCode) % numberOfShards).toString
  }
}

class ShardedAlarm(signalShard: () => ActorRef,
                   alarmRecordShard: () => ActorRef,
                   registry: FunctionRegistry) extends Alarm(signalShard, alarmRecordShard, registry) {
  val settings = Settings(context.system)
  context.setReceiveTimeout(settings.actor.passivateTimeout)

  override def unhandled(msg: Any): Unit = msg match {
    case ReceiveTimeout =>
      context.parent ! Passivate(stopMessage = PassivateAlarmCmd)
    case PassivateAlarmCmd =>
      context.stop(self)
    case x => log.info("unhandled COMMAND: {} {}", this, x)
  }
}
