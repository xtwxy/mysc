package com.wincom.dcim.domain

import akka.actor.{ActorRef, Props}
import akka.event.Logging
import akka.http.scaladsl.model.DateTime
import akka.pattern.ask
import akka.persistence.{PersistentActor, SnapshotOffer}
import akka.util.Timeout
import com.wincom.dcim.domain.Alarm._
import com.wincom.dcim.signal.{FunctionRegistry, UnaryFunction}
import org.joda.time.Duration

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success

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

  final case class AlarmValueVo(alarmId: String, valueTs: DateTime, value: Boolean) extends Serializable

  final case class ThresholdFuncVo(name: String, params: Map[String, String]) extends Serializable

  final case class Ok(alarmId: String) extends Command

  final case class NotAvailable(alarmId: String) extends Command

  /* commands */
  final case class CreateAlarmCmd(alarmId: String, name: String, level: Int, signalId: String, funcs: Seq[ThresholdFuncVo], positiveDesc: String, negativeDesc: String) extends Command

  final case class RenameAlarmCmd(alarmId: String, newName: String) extends Command

  final case class ChangeLevelCmd(alarmId: String, newLevel: Int) extends Command

  final case class ChangeSignalCmd(alarmId: String, newSignalId: String) extends Command

  final case class ChangeGenFuncsCmd(alarmId: String, newFuncs: Seq[ThresholdFuncVo]) extends Command

  final case class ChangePositiveDescCmd(alarmId: String, positiveDesc: String) extends Command

  final case class ChangeNegativeDescCmd(alarmId: String, negativeDesc: String) extends Command

  /* transient commands */
  final case class RetrieveAlarmCmd(alarmId: String) extends Command

  final case class GetAarmValueCmd(alarmId: String) extends Command

  final case class EvalAlarmValueCmd(alarmId: String) extends Command

  /* events */
  final case class CreateAlarmEvt(name: String, level: Int, signalId: String, funcs: Seq[ThresholdFuncPo], positiveDesc: String, negativeDesc: String) extends Event

  final case class RenameAlarmEvt(newName: String) extends Event

  final case class ChangeLevelEvt(newLevel: Int) extends Event

  final case class ChangeSignalEvt(newSignalId: String) extends Event

  final case class ChangeGenFuncsEvt(newFuncs: Seq[ThresholdFuncPo]) extends Event

  final case class ChangePositiveDescEvt(newDesc: String) extends Event

  final case class ChangeNegativeDescEvt(newDesc: String) extends Event

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

  val evalPeriod = Settings(context.system).alarm.evalPeriod.toMillis milliseconds

  override def preStart(): Unit = {
    super.preStart()
    context.system.scheduler.schedule(0 milliseconds,
      evalPeriod,
      self,
      EvalAlarmValueCmd(alarmId)
    )
  }

  implicit def requestTimeout: Timeout = FiniteDuration(20, SECONDS)
  implicit def executionContext: ExecutionContext = context.dispatcher
  
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
    case CreateAlarmCmd(_, name, level, signalId, funcs, posDesc, negDesc) =>
      persist(CreateAlarmEvt(name, level, signalId, for (f <- funcs) yield ThresholdFuncPo(f.name, f.params), posDesc, negDesc))(updateState)
    case RetrieveAlarmCmd(_) =>
      if (isValid()) {
        sender() ! AlarmVo(alarmId, alarmName.get, alarmLevel.get, signalId.get, funcConfigs, positiveDesc.get, negativeDesc.get)
      } else {
        sender() ! NotAvailable(alarmId)
      }
    case GetAarmValueCmd(_) =>
      if (available()) {
        sender() ! AlarmValueVo(alarmId, this.valueTs.get, this.value.get)
      } else {
        sender() ! NotAvailable(alarmId)
      }
    case EvalAlarmValueCmd(_) =>
      signalShard().ask(Signal.GetValueCmd(signalId.get)).mapTo[Signal.Command].onComplete {
        case f: Success[Signal.Command] =>
        f.value match {
          case sv: Signal.SignalValueVo =>
            updateAlarmValue(sv)
          case _ =>
        }
        case _ =>
      }
    case RenameAlarmCmd(_, newName) =>
      persist(RenameAlarmEvt(newName))(updateState)
    case ChangeLevelCmd(_, newLevel) =>
      persist(ChangeLevelEvt(newLevel))(updateState)
    case ChangeSignalCmd(_, newSignalId) =>
      persist(ChangeSignalEvt(newSignalId))(updateState)
    case ChangeGenFuncsCmd(_, newFuncs) =>
      persist(ChangeGenFuncsEvt(for (f <- newFuncs) yield ThresholdFuncPo(f.name, f.params)))(updateState)
    case ChangePositiveDescCmd(_, newDesc) =>
      persist(ChangePositiveDescEvt(newDesc))(updateState)
    case ChangeNegativeDescCmd(_, newDesc) =>
      persist(ChangeNegativeDescEvt(newDesc))(updateState)
    case x => log.info("COMMAND *IGNORED*: {} {}", this, x)
  }

  private def updateState: (Event => Unit) = {
    case CreateAlarmEvt(name, level, signalId, funcs, posDesc, negDesc) =>
      this.alarmName = Some(name)
      this.alarmLevel = Some(level)
      this.signalId = Some(signalId)
      funcs.foreach(x => this.funcConfigs = this.funcConfigs :+ ThresholdFuncVo(x.name, x.params))
      this.positiveDesc = Some(posDesc)
      this.negativeDesc = Some(negDesc)
    case RenameAlarmEvt(newName) =>
      this.alarmName = Some(newName)
    case ChangeLevelEvt(newLevel) =>
      this.alarmLevel = Some(newLevel)
    case ChangeSignalEvt(newSignalId) =>
      this.signalId = Some(newSignalId)
    case ChangeGenFuncsEvt(newFuncs) =>
      newFuncs.foreach(x => this.funcConfigs = this.funcConfigs :+ ThresholdFuncVo(x.name, x.params))
      updateFuncs(newFuncs)
    case ChangePositiveDescEvt(posDesc) =>
      this.positiveDesc = Some(posDesc)
    case ChangeNegativeDescEvt(negDesc) =>
      this.negativeDesc = Some(negDesc)
    case x => log.info("EVENT *IGNORED*: {} {}", this, x)
  }

  private def updateAlarmValue(sv: Signal.SignalValueVo): Unit = {
    this.valueTs = Some(sv.ts)
    var x = sv.value
    for (f <- funcs) {
      x = f.transform(x)
    }
    x match {
      case b: Boolean =>
        val old = this.value.getOrElse(false)
        if(b != old) {
          if(b) {
            AlarmRec.RaiseAlarmCmd(alarmId, valueTs.get, alarmLevel.get, sv, positiveDesc.get)
          } else {
            AlarmRec.EndAlarmCmd(alarmId, valueTs.get, sv, negativeDesc.get)
          }
        }
        this.value = Some(b)
      case _ =>
    }
  }
  private def updateFuncs(fs: Seq[ThresholdFuncPo]): Unit = {
    this.funcConfigs = mutable.ArraySeq()
    this.funcs = mutable.ArraySeq()
    fs.foreach(f => {
      this.funcConfigs = this.funcConfigs :+ ThresholdFuncVo(f.name, f.params)
      val func = registry.createUnary(f.name, f.params.asJava)
      if (func.isDefined) {
        this.funcs = this.funcs :+ func.get
      } else {
        log.warning("unary function cannot be initialized: {}", f)
      }
    })
  }

  private def isValid(): Boolean = {
    if (alarmName.isDefined
      && alarmLevel.isDefined
      && signalId.isDefined
      && positiveDesc.isDefined
      && negativeDesc.isDefined) true else false
  }

  private def available(): Boolean = {
    if (isValid() && value.isDefined && valueTs.isDefined) {
      val d = Duration.millis(DateTime.now.clicks - valueTs.get.clicks)
      val r = Duration.standardMinutes(1)
      val a = d.isShorterThan(r)
      return a
    } else {
      false
    }
  }
}
