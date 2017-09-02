package com.wincom.dcim.sharded

import akka.actor.{ActorRef, Props}
import com.wincom.dcim.domain.Alarm
import com.wincom.dcim.signal.FunctionRegistry

object ShardedAlarm {
  def props(signalShard: () => ActorRef,
            alarmRecordShard: () => ActorRef,
            registry: FunctionRegistry) = Props(new ShardedAlarm(signalShard, alarmRecordShard, registry))
}

class ShardedAlarm(signalShard: () => ActorRef,
                   alarmRecordShard: () => ActorRef,
                   registry: FunctionRegistry) extends Alarm(signalShard, alarmRecordShard, registry) {
}
