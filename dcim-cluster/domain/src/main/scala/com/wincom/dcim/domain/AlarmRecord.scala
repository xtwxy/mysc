package com.wincom.dcim.domain

import akka.actor.{ActorRef, Props}
import akka.event.Logging
import akka.http.scaladsl.model.DateTime
import akka.persistence.{PersistentActor, SnapshotOffer}
import com.wincom.dcim.domain.AlarmRecord.EventType._
import com.wincom.dcim.domain.AlarmRecord._
import com.wincom.dcim.domain.Signal.SignalValueVo
import com.wincom.dcim.util.DateFormat._

import scala.collection.mutable

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
  val beginTs: DateTime = DateTime(parseTimestamp(id(1)).getTime)
  var alarmName: Option[String] = None
  var alarmLevel: Option[Int] = None
  var signalId: Option[String] = None
  var alarmDesc: Option[String] = None
  var currentValue: Option[AnyVal] = None
  var valueTs: Option[DateTime] = None
  var endTs: Option[DateTime] = None

  var ackTime: Option[DateTime] = None
  var ackByPerson: Option[String] = None
  var ackDesc: Option[String] = None

  var muteTime: Option[DateTime] = None
  var muteByPerson: Option[String] = None
  var muteDesc: Option[String] = None

  var transitions: mutable.Seq[Event] = mutable.ArraySeq()

  override def persistenceId: String = s"${self.path.name}"

  override def receiveRecover: Receive = {
    case evt: Event =>
      updateState(evt)
    case SnapshotOffer(_, AlarmRecordPo(name, level, signalId, trans, end)) =>
      this.alarmName = Some(name)
      this.alarmLevel = Some(level)
      this.signalId = Some(signalId)
      this.transitions = this.transitions ++ trans
      this.endTs = end
    case x => log.info("RECOVER *IGNORED*: {} {}", this, x)
  }

  override def receiveCommand: Receive = {
    case RaiseAlarmCmd(_, begin, name, level, sv, desc) =>
      persist(new RaiseAlarmEvt(begin, name, level, sv, desc))(updateState)
    case TransitAlarmCmd(_, _, trans, level, sv, desc) =>
      if (isValid()) {
        persist(new TransitAlarmEvt(trans, level, sv, desc))(updateState)
      } else {
        sender() ! NotAvailable
      }
    case EndAlarmCmd(_, _, trans, sv, desc) =>
      if (isValid()) {
        persist(new EndAlarmEvt(trans, sv, desc))(updateState)
      } else {
        sender() ! NotAvailable
      }
    case AckAlarmCmd(_, _, trans, time, person, desc) =>
      if (isValid()) {
        persist(new AckAlarmEvt(trans, time, person, desc))(updateState)
      } else {
        sender() ! NotAvailable
      }
    case MuteAlarmCmd(_, _, trans, time, person, desc) =>
      if (isValid()) {
        persist(new MuteAlarmEvt(trans, time, person, desc))(updateState)
      } else {
        sender() ! NotAvailable
      }
    case RetrieveAlarmRecordCmd(_, _) =>
      if (isValid()) {
        sender() ! AlarmRecordVo(
          alarmId,
          beginTs,
          alarmName.get,
          alarmLevel.get,
          signalId.get,
          alarmDesc.get,
          currentValue.get,
          valueTs.get,
          ackTime,
          ackByPerson,
          ackDesc,
          muteTime,
          muteByPerson,
          muteDesc,
          transitions: Seq[Event],
          endTs
        )
      } else {
        sender() ! NotAvailable
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
