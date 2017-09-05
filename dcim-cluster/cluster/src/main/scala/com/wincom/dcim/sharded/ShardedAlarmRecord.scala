package com.wincom.dcim.sharded

import akka.actor.{ActorRef, Props, ReceiveTimeout}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.Passivate
import akka.http.scaladsl.model.DateTime
import com.wincom.dcim.domain.{AlarmRecord, Settings}
import com.wincom.dcim.domain.AlarmRecord._
import com.wincom.dcim.util.DateFormat._
import java.lang.Math._
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
      (s"${cmd.alarmId},${formatTimestamp(cmd.begin.clicks)}", cmd)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case cmd: Command =>
      (abs(s"${cmd.alarmId},${formatTimestamp(cmd.begin.clicks)}".hashCode) % numberOfShards).toString
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
