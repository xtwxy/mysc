package com.wincom.dcim.domain

import akka.actor.{ActorRef, Props}
import akka.event.Logging
import akka.http.scaladsl.model.DateTime
import akka.persistence.{PersistentActor, SnapshotOffer}
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

  def name(alarmId: String, begin: DateTime) = s"${alarmId},${formatTimestamp(begin.clicks)}"
}

class AlarmRecord(notifier: () => ActorRef) extends PersistentActor {
  val log = Logging(context.system.eventStream, "sharded-alarms")

  val id = (s"${self.path.name}").split(",")
  val alarmId: String = id(0)
  val beginTks: Long = parseTimestamp(id(1)).getTime

  val beginTs: Timestamp = Timestamp(beginTks/1000, (beginTs % 1000) * 1000000)
  var alarmName: Option[String] = None
  var alarmLevel: Option[AlarmLevel] = None
  var signalId: Option[String] = None
  var alarmDesc: Option[String] = None
  var currentValue: Option[SignalSnapshotValueVo] = None
  var endTs: Option[Timestamp] = None

  var ackTime: Option[Timestamp] = None
  var ackByPerson: Option[String] = None
  var ackDesc: Option[String] = None

  var muteTime: Option[Timestamp] = None
  var muteByPerson: Option[String] = None
  var muteDesc: Option[String] = None

  var transitions: Seq[Event] = Seq()

  override def persistenceId: String = s"${self.path.name}"

  override def receiveRecover: Receive = {
    case evt: Event =>
      updateState(evt)
    case x => log.info("RECOVER *IGNORED*: {} {}", this, x)
  }

  override def receiveCommand: Receive = {
    case RaiseAlarmCmd(_, user, begin, name, level, sv, desc, event) =>
      persist(RaiseAlarmEvt(user, name, level, sv, desc, event))(updateState)
    case TransitAlarmCmd(_, user, begin, trans, level, sv, desc, event) =>
      if (isValid()) {
        persist(new TransitAlarmEvt(user, trans, level, sv, desc, event))(updateState)
      } else {
        sender() ! NOT_AVAILABLE
      }
    case EndAlarmCmd(_, user, begin, trans, sv, desc, event) =>
      if (isValid()) {
        persist(new EndAlarmEvt(user, trans, sv, desc, event))(updateState)
      } else {
        sender() ! NOT_AVAILABLE
      }
    case AckAlarmCmd(_, user, begin, trans, time, person, desc, event) =>
      if (isValid()) {
        persist(new AckAlarmEvt(user, trans, time, person, desc, event))(updateState)
      } else {
        sender() ! NOT_AVAILABLE
      }
    case MuteAlarmCmd(_, user, begin, trans, time, person, desc, event) =>
      if (isValid()) {
        persist(new MuteAlarmEvt(user, trans, time, person, desc, event))(updateState)
      } else {
        sender() ! NOT_AVAILABLE
      }
    case RetrieveAlarmRecordCmd(_, user, _) =>
      if (isValid()) {
        sender() ! AlarmRecordVo(
          alarmId,
          beginTs,
          alarmName.get,
          alarmLevel.get,
          signalId.get,
          alarmDesc,
          currentValue,
          ackTime,
          ackByPerson,
          ackDesc,
          muteTime,
          muteByPerson,
          muteDesc,
          (for(t <- transitions) yield Any(t.getClass.getName, t.toByteString)),
          endTs
        )
      } else {
        sender() ! NOT_AVAILABLE
      }
    case x => log.info("COMMAND *IGNORED*: {} {}", this, x)
  }

  private def updateState: (Event => Unit) = {
    case x@RaiseAlarmEvt(Raise, _, name, level, sv, desc) =>
      this.alarmName = Some(name)
      this.alarmLevel = Some(level)
      this.signalId = Some(sv.signalId)
      this.currentValue = Some(sv.value)
      this.valueTs = Some(sv.ts)
      this.alarmDesc = Some(desc)
      this.transitions = this.transitions :+ x
    case x@TransitAlarmEvt(Transit, _, level, sv, desc) =>
      this.alarmLevel = Some(level)
      this.signalId = Some(sv.signalId)
      this.currentValue = Some(sv.value)
      this.valueTs = Some(sv.ts)
      this.alarmDesc = Some(desc)

      this.ackByPerson = None
      this.ackTime = None
      this.ackDesc = None
      this.muteByPerson = None
      this.muteTime = None
      this.muteDesc = None
      this.transitions = this.transitions :+ x
    case x@EndAlarmEvt(End, _, sv, desc) =>
      this.signalId = Some(sv.signalId)
      this.currentValue = Some(sv.value)
      this.valueTs = Some(sv.ts)
      this.alarmDesc = Some(desc)
      this.transitions = this.transitions :+ x
    case x@AckAlarmEvt(Ack, _, time, person, desc) =>
      this.ackByPerson = Some(person)
      this.ackTime = Some(time)
      this.ackDesc = Some(desc)
      this.transitions = this.transitions :+ x
      replyToSender(Ok)
    case x@MuteAlarmEvt(Mute, _, time, person, desc) =>
      this.muteByPerson = Some(person)
      this.muteTime = Some(time)
      this.muteDesc = Some(desc)
      this.transitions = this.transitions :+ x
      replyToSender(Ok)
    case x => log.info("EVENT *IGNORED*: {} {}", this, x)
  }
  private def replyToSender(msg: Any) = {
    if ("deadLetters" != sender().path.name) sender() ! msg
  }
  private def isValid(): Boolean = {
    alarmName.isDefined && alarmLevel.isDefined && signalId.isDefined && !transitions.isEmpty
  }
}
