package com.wincom.dcim.sharded

import java.lang.Math._

import akka.actor.{ActorRef, Props, ReceiveTimeout}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.Passivate
import akka.http.scaladsl.model.DateTime
import com.wincom.dcim.domain.{AlarmRecord, Settings}
import com.wincom.dcim.message.alarmrecord.PassivateAlarmRecordCmd
import com.wincom.dcim.message.common.Command
import com.wincom.dcim.util.DateFormat._
/**
  * Created by wangxy on 17-8-29.
  */
object ShardedAlarmRecord {
  def props(notifier: () => ActorRef) = Props(new ShardedAlarmRecord(notifier))
  def name(alarmId: String, beginTime: DateTime): String = s"${alarmId},${formatTimestamp(beginTime.clicks)}"

  val shardName = "alarm-record-shard"
  var numberOfShards = 100

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case cmd: Command =>
      (cmd.entityId, cmd)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case cmd: Command =>
      (abs(cmd.entityId.hashCode) % numberOfShards).toString
  }
}

class ShardedAlarmRecord(notifier: () => ActorRef) extends AlarmRecord(notifier) {
  val settings = Settings(context.system)
  context.setReceiveTimeout(settings.actor.passivateTimeout)

  override def unhandled(message: Any): Unit = message match {
    case ReceiveTimeout =>
      context.parent ! Passivate(stopMessage = PassivateAlarmRecordCmd)
    case PassivateAlarmRecordCmd =>
      context.stop(self)
    case x => log.info("unhandled COMMAND: {} {}", this, x)
  }
}
