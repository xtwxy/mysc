package com.wincom.dcim.domain

import akka.actor.{ActorRef, Props}
import akka.event.Logging
import akka.persistence._
import com.google.protobuf.any.Any
import com.google.protobuf.timestamp.Timestamp
import com.wincom.dcim.util.DateFormat._
import com.wincom.dcim.message.common._
import com.wincom.dcim.message.common.ResponseType._
import com.wincom.dcim.message.alarm._
import com.wincom.dcim.message.alarmrecord._
import com.wincom.dcim.message.signal.SignalSnapshotValueVo

/**
  * Created by wangxy on 17-8-28.
  */
object AlarmRecord {
  def props(notifier: () => ActorRef) = Props(new AlarmRecord(notifier))

  def name(alarmId: String, begin: Timestamp) = s"${alarmId},${formatTimestamp(begin)}"
}

class AlarmRecord(notifier: () => ActorRef) extends PersistentActor {
  val log = Logging(context.system.eventStream, "sharded-alarms")

  val id = (s"${self.path.name}").split(",")
  val alarmId: String = id(0)
  val beginTs: Timestamp = parseTimestamp(id(1))
  var alarmName: Option[String] = None
  var alarmLevel: Option[AlarmLevel] = None
  var alarmDesc: Option[String] = None
  var currentValue: Option[SignalSnapshotValueVo] = None
  var endTs: Option[Timestamp] = None

  var ackTime: Option[Timestamp] = None
  var ackByPerson: Option[String] = None
  var ackDesc: Option[String] = None

  var muteTime: Option[Timestamp] = None
  var muteByPerson: Option[String] = None
  var muteDesc: Option[String] = None

  var transitions: Seq[AlarmEvent] = Seq()

  override def persistenceId: String = s"${self.path.name}"

  override def receiveRecover: Receive = {
    case evt: AlarmEvent =>
      updateState(evt)
    case x => log.info("RECOVER *IGNORED*: {} {}", this, x)
  }

  override def receiveCommand: Receive = {
    case RaiseAlarmCmd(_, user, begin, name, level, sv, desc) =>
      persist(RaiseAlarmEvt(user, name, level, sv, desc))(updateState)
    case TransitAlarmCmd(_, user, begin, trans, level, sv, desc) =>
      if (isValid()) {
        persist(new TransitAlarmEvt(user, trans, level, sv, desc))(updateState)
      } else {
        sender() ! Response(NOT_AVAILABLE, None)
      }
    case EndAlarmCmd(_, user, begin, trans, sv, desc) =>
      if (isValid()) {
        persist(new EndAlarmEvt(user, trans, sv, desc))(updateState)
      } else {
        sender() ! Response(NOT_AVAILABLE, None)
      }
    case AckAlarmCmd(_, user, begin, time, person, desc) =>
      if (isValid()) {
        persist(new AckAlarmEvt(user, time, person, desc))(updateState)
      } else {
        sender() ! Response(NOT_AVAILABLE, None)
      }
    case MuteAlarmCmd(_, user, begin, time, person, desc) =>
      if (isValid()) {
        persist(new MuteAlarmEvt(user, time, person, desc))(updateState)
      } else {
        sender() ! Response(NOT_AVAILABLE, None)
      }
    case RetrieveAlarmRecordCmd(_, user, _) =>
      if (isValid()) {
        sender() ! AlarmRecordVo(
          alarmId,
          beginTs,
          alarmName.get,
          alarmLevel.get,
          alarmDesc,
          currentValue,
          ackTime,
          ackByPerson,
          ackDesc,
          muteTime,
          muteByPerson,
          muteDesc,
          (for(t <- transitions) yield Any(t.event.name, t.toByteString)),
          endTs
        )
      } else {
        sender() ! Response(NOT_AVAILABLE, None)
      }
    case x => log.info("COMMAND *IGNORED*: {} {}", this, x)
  }

  private def updateState: (AlarmEvent => Unit) = {
    case x@RaiseAlarmEvt(_, name, level, sv, desc) =>
      this.alarmName = Some(name)
      this.alarmLevel = Some(level)
      this.currentValue = Some(sv)
      this.alarmDesc = desc
      this.transitions = this.transitions :+ x
    case x@TransitAlarmEvt(_, transTime, level, sv, desc) =>
      this.alarmLevel = Some(level)
      this.currentValue = Some(sv)
      this.alarmDesc = desc

      this.ackByPerson = None
      this.ackTime = None
      this.ackDesc = None
      this.muteByPerson = None
      this.muteTime = None
      this.muteDesc = None
      this.transitions = this.transitions :+ x
    case x@EndAlarmEvt(_, endTime, sv, desc) =>
      this.currentValue = Some(sv)
      this.alarmDesc = desc
      this.transitions = this.transitions :+ x
    case x@AckAlarmEvt(_, time, person, desc) =>
      this.ackByPerson = Some(person)
      this.ackTime = Some(time)
      this.ackDesc = desc
      this.endTs = Some(time)
      this.transitions = this.transitions :+ x
      replyToSender(SUCCESS)
    case x@MuteAlarmEvt(_, time, person, desc) =>
      this.muteByPerson = Some(person)
      this.muteTime = Some(time)
      this.muteDesc = desc
      this.transitions = this.transitions :+ x
      replyToSender(SUCCESS)
    case x => log.info("EVENT *IGNORED*: {} {}", this, x)
  }
  private def replyToSender(msg: ValueObject) = {
    if ("deadLetters" != sender().path.name) sender() ! msg
  }
  private def isValid(): Boolean = {
    alarmName.isDefined && alarmLevel.isDefined && !transitions.isEmpty
  }
}
