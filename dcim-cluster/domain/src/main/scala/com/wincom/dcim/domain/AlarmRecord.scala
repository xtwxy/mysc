package com.wincom.dcim.domain

import akka.actor.{ActorRef, Props}
import akka.event.Logging
import akka.http.scaladsl.model.DateTime
import akka.persistence.{PersistentActor, SnapshotOffer}
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

  sealed trait Command {
    def alarmId: String

    def begin: DateTime
  }

  sealed trait Event extends Serializable {
    def time: DateTime
  }

  /* value objects */
  final case class AlarmRecordVo(alarmId: String,
                                 begin: DateTime,
                                 name: String,
                                 level: Int,
                                 signalId: String,
                                 desc: String,
                                 value: AnyVal,
                                 valueTs: DateTime,
                                 ackTime: Option[DateTime],
                                 ackByPerson: Option[String],
                                 ackDesc: Option[String],
                                 muteTime: Option[DateTime],
                                 muteByPerson: Option[String],
                                 muteDesc: Option[String],
                                 transitions: Seq[Event],
                                 end: Option[DateTime]
                                ) extends Command

  /* commands */
  final case class RaiseAlarmCmd(alarmId: String,
                                 begin: DateTime,
                                 name: String,
                                 level: Int,
                                 signalValue: SignalValueVo,
                                 desc: String) extends Command

  final case class TransitAlarmCmd(alarmId: String,
                                   begin: DateTime,
                                   trans: DateTime,
                                   level: Int,
                                   signalValue: SignalValueVo,
                                   desc: String) extends Command

  final case class EndAlarmCmd(alarmId: String,
                               begin: DateTime,
                               end: DateTime,
                               signalValue: SignalValueVo,
                               desc: String) extends Command

  final case class AckAlarmCmd(alarmId: String, begin: DateTime, ackTime: DateTime, byPerson: String, desc: String) extends Command

  final case class MuteAlarmCmd(alarmId: String, begin: DateTime, muteTime: DateTime, byPerson: String, desc: String) extends Command

  /* transient commands */
  final case class RetrieveAlarmCmd(alarmId: String, begin: DateTime) extends Command
  final case class PassivateAlarmCmd(alarmId: String, begin: DateTime) extends Command

  /* persistent objects */
  final case class AlarmRecordPo(name: String,
                                 level: Int,
                                 signalId: String,
                                 transitions: Seq[Event],
                                 end: Option[DateTime]) extends Serializable

  final case class RaiseAlarmEvt(time: DateTime,
                                 name: String,
                                 level: Int,
                                 signalValue: SignalValueVo,
                                 desc: String) extends Event

  final case class TransitAlarmEvt(time: DateTime,
                                   level: Int,
                                   signalValue: SignalValueVo,
                                   desc: String) extends Event

  final case class EndAlarmEvt(time: DateTime,
                               signalValue: SignalValueVo,
                               desc: String) extends Event

  final case class AckAlarmEvt(time: DateTime, byPerson: String, desc: String) extends Event

  final case class MuteAlarmEvt(time: DateTime, byPerson: String, desc: String) extends Event

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
      persist(RaiseAlarmEvt(begin, name, level, sv, desc))(updateState)
    case TransitAlarmCmd(_, _, trans, level, sv, desc) =>
      persist(TransitAlarmEvt(trans, level, sv, desc))(updateState)
    case EndAlarmCmd(_, _, trans, sv, desc) =>
      persist(EndAlarmEvt(trans, sv, desc))(updateState)
    case AckAlarmCmd(_, _, time, person, desc) =>
      persist(AckAlarmEvt(time, person, desc))(updateState)
    case MuteAlarmCmd(_, _, time, person, desc) =>
      persist(MuteAlarmEvt(time, person, desc))(updateState)
    case RetrieveAlarmCmd(_, _) =>
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
    case x => log.info("COMMAND *IGNORED*: {} {}", this, x)
  }

  private def updateState: (Event => Unit) = {
    case x@RaiseAlarmEvt(_, name, level, sv, desc) =>
      this.alarmName = Some(name)
      this.alarmLevel = Some(level)
      this.signalId = Some(sv.signalId)
      this.currentValue = Some(sv.value)
      this.valueTs = Some(sv.ts)
      this.alarmDesc = Some(desc)
      this.transitions = this.transitions :+ x
    case x@TransitAlarmEvt(_, level, sv, desc) =>
      this.alarmLevel = Some(level)
      this.signalId = Some(sv.signalId)
      this.currentValue = Some(sv.value)
      this.valueTs = Some(sv.ts)
      this.alarmDesc = Some(desc)
      this.transitions = this.transitions :+ x
    case x@EndAlarmEvt(_, sv, desc) =>
      this.signalId = Some(sv.signalId)
      this.currentValue = Some(sv.value)
      this.valueTs = Some(sv.ts)
      this.alarmDesc = Some(desc)
      this.transitions = this.transitions :+ x
    case x@AckAlarmEvt(time, person, desc) =>
      this.ackByPerson = Some(person)
      this.ackTime = Some(time)
      this.ackDesc = Some(desc)
      this.transitions = this.transitions :+ x
    case x@MuteAlarmEvt(time, person, desc) =>
      this.muteByPerson = Some(person)
      this.muteTime = Some(time)
      this.muteDesc = Some(desc)
      this.transitions = this.transitions :+ x
    case x => log.info("EVENT *IGNORED*: {} {}", this, x)
  }
}
