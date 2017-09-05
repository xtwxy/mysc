package com.wincom.dcim.rest

import com.wincom.dcim.domain.AlarmRecord.EventType._
import com.wincom.dcim.domain.AlarmRecord._
import com.wincom.dcim.domain.Signal
import spray.json._
import spray.json.DefaultJsonProtocol._
/**
  * Created by wangxy on 17-9-4.
  */
trait AlarmRecordMarshaling extends DefaultJsonProtocol {
  implicit val anyValFormat = AnyValFormat
  implicit val dateTimeFormat = DateTimeJsonFormat

  implicit val signalValueVoFormat = jsonFormat3(Signal.SignalValueVo)

  implicit val alarmEventFormat = AlarmEventFormat
  implicit val alarmRecordVoFormat = jsonFormat16(AlarmRecordVo)
  implicit val raiseAlarmCmdFormat = jsonFormat6(RaiseAlarmCmd)
  implicit val transitAlarmCmdFormat = jsonFormat6(TransitAlarmCmd)
  implicit val endAlarmCmdFormat = jsonFormat5(EndAlarmCmd)
  implicit val ackAlarmCmdFormat = jsonFormat5(AckAlarmCmd)
  implicit val muteAlarmCmdFormat = jsonFormat5(MuteAlarmCmd)
  implicit val retrieveAlarmCmdFormat = jsonFormat2(RetrieveAlarmCmd)
  implicit val passivateAlarmCmdFormat = jsonFormat2(PassivateAlarmCmd)
}

object AlarmRecordMarshaling extends AlarmRecordMarshaling
object EventTypeFormat extends RootJsonFormat[EventType] {
  override def write(obj: EventType): JsValue = {
    JsString(obj.name)
  }

  override def read(json: JsValue): EventType = json match {
    case JsString(Raise.name) => Raise
    case JsString(Transit.name) => Transit
    case JsString(End.name) => End
    case JsString(Ack.name) => Ack
    case JsString(Mute.name) => Mute
    case x =>
      throw new IllegalArgumentException("Unknown event: '%s'".format(x))
  }
}

object AlarmEventFormat extends RootJsonFormat[Event] {
  implicit val anyValFormat = AnyValFormat
  implicit val dateTimeFormat = DateTimeJsonFormat
  implicit val eventTypeFormat = EventTypeFormat
  implicit val signalValueVoFormat = jsonFormat3(Signal.SignalValueVo)
  implicit val raiseAlarmEvtFormat = jsonFormat6(RaiseAlarmEvt)
  implicit val transitAlarmEvtFormat = jsonFormat5(TransitAlarmEvt)
  implicit val endAlarmEvtFormat = jsonFormat4(EndAlarmEvt)
  implicit val ackAlarmEvtFormat = jsonFormat4(AckAlarmEvt)
  implicit val muteAlarmEvtFormat = jsonFormat4(MuteAlarmEvt)

  override def read(json: JsValue): Event = json match {
    case JsObject(fields) =>
      fields("event") match {
        case JsString(Raise.name) =>
          raiseAlarmEvtFormat.read(json)
        case JsString(Transit.name) =>
          transitAlarmEvtFormat.read(json)
        case JsString(End.name) =>
          endAlarmEvtFormat.read(json)
        case JsString(Ack.name) =>
          ackAlarmEvtFormat.read(json)
        case JsString(Mute.name) =>
          muteAlarmEvtFormat.read(json)
        case x =>
          throw new IllegalArgumentException("Unknown event: '%s'".format(x))
      }
    case x =>
      throw new IllegalArgumentException("Unknown JsValue: '%s'".format(x))
  }

  override def write(obj: Event): JsValue = obj match {
    case e: RaiseAlarmEvt =>
      raiseAlarmEvtFormat.write(e)
    case e: TransitAlarmEvt =>
      transitAlarmEvtFormat.write(e)
    case e: EndAlarmEvt =>
      endAlarmEvtFormat.write(e)
    case e: AckAlarmEvt =>
      ackAlarmEvtFormat.write(e)
    case e: MuteAlarmEvt =>
      muteAlarmEvtFormat.write(e)
  }
}