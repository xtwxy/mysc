package com.wincom.dcim.domain

import akka.actor.{ActorRef, Props}
import akka.event.Logging
import akka.http.scaladsl.model.DateTime
import akka.persistence.{PersistentActor, SnapshotOffer}
import com.wincom.dcim.domain.AlarmRec._
import com.wincom.dcim.domain.Signal.SignalValueVo
import com.wincom.dcim.signal.FunctionRegistry
import com.wincom.dcim.util.DateFormat._

import scala.collection.mutable

/**
  * Created by wangxy on 17-8-28.
  */
object AlarmRec {
  def props(signalShard: () => ActorRef, registry: FunctionRegistry) = Props(new AlarmRec(signalShard, registry))

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
                                 transitions: Seq[Command],
                                 end: Option[DateTime]) extends Command

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

  final case class AckAlarmCmd(alarmId: String, begin: DateTime, byPerson: String, muteTime: DateTime) extends Command

  final case class MuteAlarmCmd(alarmId: String, begin: DateTime, byPerson: String, muteTime: DateTime) extends Command

  /* transient commands */
  final case class RetrieveAlarmCmd(alarmId: String, begin: DateTime) extends Command

  /* persistent objects */
  final case class AlarmRecordPo(name: String,
                                 level: Int,
                                 signalId: String,
                                 transitions: Seq[Event],
                                 end: Option[DateTime]) extends Serializable

  final case class RaiseAlarmEvt(time:DateTime,
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

  final case class AckAlarmEvt(time: DateTime, byPerson: String) extends Event

  final case class MuteAlarmEvt(time: DateTime, byPerson: String) extends Event

}

class AlarmRec(signalShard: () => ActorRef, registry: FunctionRegistry) extends PersistentActor {
  val log = Logging(context.system.eventStream, "sharded-alarms")

  val alarmId = s"${self.path.name}"
  var beginTs: Option[DateTime] = None
  var alarmName: Option[String] = None
  var alarmLevel: Option[Int] = None
  var signalId: Option[String] = None
  var alarmDesc: Option[String] = None
  var currentValue: Option[AnyVal] = None
  var valueTs: Option[DateTime] = None
  var endTs: Option[DateTime] = None

  var ackByPerson: Option[String] = None
  var ackTime: Option[DateTime] = None

  var muteByPerson: Option[String] = None
  var muteTime: Option[DateTime] = None

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
    case _ =>
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
    case x@AckAlarmEvt(time, person) =>
      this.ackByPerson = Some(person)
      this.ackTime = Some(time)
      this.transitions = this.transitions :+ x
    case x@MuteAlarmEvt(time, person) =>
      this.muteByPerson = Some(person)
      this.muteTime = Some(time)
      this.transitions = this.transitions :+ x
    case x => log.info("EVENT *IGNORED*: {} {}", this, x)
  }
}
