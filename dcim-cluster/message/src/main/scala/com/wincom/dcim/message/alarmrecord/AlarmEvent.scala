package com.wincom.dcim.message.alarmrecord

import com.google.protobuf.any.Any
import com.wincom.dcim.message.alarmrecord.EventType._
import com.wincom.dcim.message.common.Event

/**
  * Created by wangxy on 17-9-14.
  */
trait AlarmEvent extends Event {
  def event: EventType = {
    this match {
      case _: RaiseAlarmEvt => RAISE
      case _: AckAlarmEvt => ACK
      case _: EndAlarmEvt => END
      case _: MuteAlarmEvt => MUTE
      case _: TransitAlarmEvt => TRANSIT
    }
  }
}

object AlarmEvent {
  def apply(any: Any): AlarmEvent = {
    any.typeUrl match {
      case RAISE.name => RaiseAlarmEvt.parseFrom(any.value.newCodedInput())
      case ACK.name => AckAlarmEvt.parseFrom(any.value.newCodedInput())
      case END.name => EndAlarmEvt.parseFrom(any.value.newCodedInput())
      case MUTE.name => MuteAlarmEvt.parseFrom(any.value.newCodedInput())
      case TRANSIT.name => TransitAlarmEvt.parseFrom(any.value.newCodedInput())
      case _ => throw new IllegalArgumentException("un-parsable message.")
    }
  }
}
