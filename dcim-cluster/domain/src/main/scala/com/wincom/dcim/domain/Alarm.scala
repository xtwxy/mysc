package com.wincom.dcim.domain

import akka.actor.{ActorRef, Props}
import akka.event.Logging
import akka.http.scaladsl.model.DateTime
import akka.persistence.{PersistentActor, SnapshotOffer}
import com.wincom.dcim.domain.Alarm.{AlarmPo, Event, ThresholdFuncVo}
import com.wincom.dcim.domain.Signal.SignalValueVo
import com.wincom.dcim.signal.{FunctionRegistry, UnaryFunction}

import scala.collection.mutable

/**
  * Created by wangxy on 17-8-28.
  */
object Alarm {
  def props(signalShard: () => ActorRef, registry: FunctionRegistry) = Props(new Alarm(signalShard, registry))

  def name(alarmId: String) = s"$alarmId"

  sealed trait Command {
    def alarmId: String
  }

  sealed trait Event extends Serializable
  /* value objects */
  final case class AlarmVo(alarmId: String, name: String, level: Int, signalId: String, funcs: Seq[ThresholdFuncVo], positiveDesc: String, negativeDesc: String) extends Serializable
  final case class ThresholdFuncVo(name: String, params: Map[String, String]) extends Serializable

  /* commands */
  final case class CreateAlarmCmd(alarmId: String, name: String, level: Int, signalId: String, funcs: Seq[ThresholdFuncVo], positiveDesc: String, negativeDesc: String) extends Command
  final case class RenameAlarmCmd(alarmId: String, newName: String) extends Command
  final case class ChangeLevelCmd(alarmId: String, newLevel: Int) extends Command
  final case class ChangeSignalCmd(alarmId: String, newSignalId: String) extends Command
  final case class ChangeGenFuncsCmd(alarmId: String, newFuncs: Seq[UnaryFunction]) extends Command
  final case class ChangePositiveDescCmd(alarmId: String, positiveDesc: String) extends Command
  final case class ChangeNegativeDescCmd(alarmId: String, negativeDesc: String) extends Command

  /* events */
  final case class CreateAlarmEvt(name: String, level: Int, signalId: String, funcs: Seq[ThresholdFuncPo], positiveDesc: String, negativeDesc: String) extends Event
  final case class RenameAlarmEvt(newName: String) extends Event
  final case class ChangeLevelEvt(newLevel: Int) extends Event
  final case class ChangeSignalEvt(newSignalId: String) extends Event
  final case class ChangeGenFuncsEvt(newFuncs: Seq[UnaryFunction]) extends Event
  final case class ChangePositiveDescEvt(positiveDesc: String) extends Event
  final case class ChangeNegativeDescEvt(negativeDesc: String) extends Event

  final case class AlarmPo(name: String, level: Int, signalId: String, funcs: Seq[ThresholdFuncPo], positiveDesc: String, negativeDesc: String) extends Serializable
  final case class ThresholdFuncPo(name: String, params: Map[String, String]) extends Serializable
}

class Alarm(signalShard: () => ActorRef, registry: FunctionRegistry) extends PersistentActor {
  val log = Logging(context.system.eventStream, "sharded-alarms")

  val alarmId = s"${self.path.name}"
  var alarmName: Option[String] = None
  var alarmLevel: Option[Int] = None
  var signalId: Option[String] = None
  var funcConfigs: collection.mutable.Seq[ThresholdFuncVo] = mutable.ArraySeq()
  var positiveDesc: Option[String] = None
  var negativeDesc: Option[String] = None

  // transient values
  var funcs: collection.mutable.Seq[UnaryFunction] = mutable.ArraySeq()
  var value: Option[Boolean] = None
  var valueTs: Option[DateTime] = None

  override def persistenceId: String = s"${self.path.name}"

  override def receiveRecover: Receive = {
    case evt: Event => updateState(evt)
    case SnapshotOffer(_, AlarmPo(name, level, signalId, funcs, posDesc, negDesc)) =>
      this.alarmName = Some(name)
      this.alarmLevel = Some(level)
      this.signalId = Some(signalId)
      funcs.foreach(x => this.funcConfigs = this.funcConfigs :+ ThresholdFuncVo(x.name, x.params))
      this.positiveDesc = Some(posDesc)
      this.negativeDesc = Some(negDesc)
    case x => log.info("RECOVER *IGNORED*: {} {}", this, x)
  }

  override def receiveCommand: Receive = {
    case _ =>
  }

  private def updateState: (Event => Unit) = {
    case _ =>
  }
}
