package com.wincom.dcim.rest

import com.google.protobuf.any
import com.wincom.dcim.message.alarmrecord.EventType._
import com.wincom.dcim.message.common._
import com.wincom.dcim.message.signal._
import com.wincom.dcim.message.alarmrecord._
import spray.json._
import spray.json.DefaultJsonProtocol._
/**
  * Created by wangxy on 17-9-4.
  */
trait AlarmRecordMarshaling extends DefaultJsonProtocol {
  implicit val anyValFormat = AnyValFormat
  implicit val dateTimeFormat = DateTimeJsonFormat

  implicit val responseTypeJsonFormat = ResponseTypeJsonFormat
  implicit val signalTypeJsonFormat = SignalTypeJsonFormat
  implicit val alarmLevelJsonFormat = AlarmLevelJsonFormat

  implicit val signalValueVoFormat = jsonFormat4(SignalValueVo.apply)
  implicit val signalSnapshotValueVoFormat = jsonFormat3(SignalSnapshotValueVo.apply)

  implicit val alarmEventFormat = AlarmEventFormat
  implicit val anyAlarmEventFormat = AnyAlarmEventFormat
  implicit val alarmRecordVoFormat = jsonFormat14(AlarmRecordVo.apply)
  implicit val raiseAlarmCmdFormat = jsonFormat7(RaiseAlarmCmd.apply)
  implicit val transitAlarmCmdFormat = jsonFormat7(TransitAlarmCmd.apply)
  implicit val endAlarmCmdFormat = jsonFormat6(EndAlarmCmd.apply)
  implicit val ackAlarmCmdFormat = jsonFormat6(AckAlarmCmd.apply)
  implicit val muteAlarmCmdFormat = jsonFormat6(MuteAlarmCmd.apply)
  implicit val retrieveAlarmCmdFormat = jsonFormat3(RetrieveAlarmRecordCmd.apply)
  implicit val passivateAlarmCmdFormat = jsonFormat3(PassivateAlarmRecordCmd.apply)
}

object AlarmRecordMarshaling extends AlarmRecordMarshaling
object EventTypeFormat extends RootJsonFormat[EventType] {
  override def write(obj: EventType): JsValue = {
    JsString(obj.name)
  }

  override def read(json: JsValue): EventType = json match {
    case JsString(RAISE.name) => RAISE
    case JsString(TRANSIT.name) => TRANSIT
    case JsString(END.name) => END
    case JsString(ACK.name) => ACK
    case JsString(MUTE.name) => MUTE
    case x =>
      throw new IllegalArgumentException("Unknown event: '%s'".format(x))
  }
}

object AlarmEventFormat extends RootJsonFormat[AlarmEvent] {
  implicit val anyValFormat = AnyValFormat
  implicit val dateTimeFormat = DateTimeJsonFormat
  implicit val eventTypeFormat = EventTypeFormat

  implicit val alarmLevelJsonFormat = AlarmLevelJsonFormat
  implicit val signalTypeJsonFormat = SignalTypeJsonFormat

  implicit val signalValueVoFormat = jsonFormat4(SignalValueVo.apply)
  implicit val signalSnapshotValueVoFormat = jsonFormat3(SignalSnapshotValueVo.apply)
  implicit val raiseAlarmEvtFormat = jsonFormat5(RaiseAlarmEvt.apply)
  implicit val transitAlarmEvtFormat = jsonFormat5(TransitAlarmEvt.apply)
  implicit val endAlarmEvtFormat = jsonFormat4(EndAlarmEvt.apply)
  implicit val ackAlarmEvtFormat = jsonFormat4(AckAlarmEvt.apply)
  implicit val muteAlarmEvtFormat = jsonFormat4(MuteAlarmEvt.apply)

  override def read(json: JsValue): AlarmEvent = json match {
    case JsObject(fields) =>
      fields("event") match {
        case JsString(RAISE.name) =>
          raiseAlarmEvtFormat.read(json)
        case JsString(TRANSIT.name) =>
          transitAlarmEvtFormat.read(json)
        case JsString(END.name) =>
          endAlarmEvtFormat.read(json)
        case JsString(ACK.name) =>
          ackAlarmEvtFormat.read(json)
        case JsString(MUTE.name) =>
          muteAlarmEvtFormat.read(json)
        case x =>
          throw new IllegalArgumentException("Unknown event: '%s'".format(x))
      }
    case x =>
      throw new IllegalArgumentException("Unknown JsValue: '%s'".format(x))
  }

  override def write(obj: AlarmEvent): JsValue = obj match {
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

object AnyAlarmEventFormat extends RootJsonFormat[com.google.protobuf.any.Any] {
  implicit val alarmEventFormat = AlarmEventFormat
  override def write(obj: any.Any): JsValue = {
    val event = AlarmEvent.apply(obj)
    alarmEventFormat.write(event)
  }

  override def read(json: JsValue): any.Any = {
    val event = alarmEventFormat.read(json)
    any.Any(event.event.name, event.toByteString)
  }
}