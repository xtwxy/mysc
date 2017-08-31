package com.wincom.dcim.domain

import akka.actor.{ActorRef, Props}
import akka.event.Logging
import akka.http.scaladsl.model.DateTime
import akka.pattern.ask
import akka.persistence.{PersistentActor, SnapshotOffer}
import akka.util.Timeout
import com.wincom.dcim.domain.Alarm._
import com.wincom.dcim.domain.Signal.SignalValueVo
import com.wincom.dcim.signal.{FunctionRegistry, SetFunction, UnaryFunction}
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
final case class ThresholdFuncVo(name: String, params: Map[String, String])
final case class AlarmConditionVo(func: ThresholdFuncVo, level:Int, positiveDesc: String, negativeDesc: String)

object ThresholdFuncVo {
  def apply(name: String, params: Map[String, String]): ThresholdFuncVo = new ThresholdFuncVo(name, params)
  def apply(func: ThresholdFunc): ThresholdFuncVo = new ThresholdFuncVo(func.name, func.params)
}

object AlarmConditionVo {
  def apply(func: ThresholdFuncVo, level:Int, positiveDesc: String, negativeDesc: String): AlarmConditionVo = new AlarmConditionVo(func, level, positiveDesc, negativeDesc)
  def apply(cond: AlarmCondition): AlarmConditionVo = new AlarmConditionVo(ThresholdFuncVo(cond.func), cond.level, cond.positiveDesc, cond.negativeDesc)
}

final case class ThresholdFunc(name: String, params: Map[String, String], func: SetFunction) extends SetFunction {
  override def contains(e: AnyVal): Boolean = func.contains(e)
  override def subsetOf(f: SetFunction): Boolean = func.subsetOf(f)
  override def intersects(f: SetFunction): Boolean = func.intersects(f)
}

final case class AlarmCondition(func: ThresholdFunc, level:Int, positiveDesc: String, negativeDesc: String) extends SetFunction {
  override def contains(e: AnyVal): Boolean = func.contains(e)
  override def subsetOf(f: SetFunction): Boolean = func.subsetOf(f)
  override def intersects(f: SetFunction): Boolean = func.intersects(f)
}

object Alarm {
  def props(signalShard: () => ActorRef,
            alarmRecordShard: () => ActorRef,
            registry: FunctionRegistry) = Props(new Alarm(signalShard, alarmRecordShard, registry))

  def name(alarmId: String) = s"$alarmId"

  sealed trait Command {
    def alarmId: String
  }

  sealed trait Event extends Serializable

  /* value objects */
  final case class AlarmValueVo(alarmId: String,
                                name: String,
                                signalId: String,
                                conditions: Set[Seq[AlarmCondition]],
                                value: Option[Boolean],
                                matched: Option[AlarmCondition],
                                beginTs: Option[DateTime],
                                endTs: Option[DateTime])

  final case class Ok(alarmId: String) extends Command

  final case class NotAvailable(alarmId: String) extends Command

  /* commands */
  final case class CreateAlarmCmd(alarmId: String, name: String, signalId: String, conditions: Set[Seq[AlarmConditionVo]]) extends Command
  final case class RenameAlarmCmd(alarmId: String, newName: String) extends Command
  final case class SelectSignalCmd(alarmId: String, newSignalId: String) extends Command
  final case class AddConditionCmd(alarmId: String, condition: AlarmConditionVo) extends Command
  final case class RemoveConditionCmd(alarmId: String, condition: AlarmConditionVo) extends Command
  final case class ReplaceConditionCmd(alarmId: String, oldCondition: AlarmConditionVo, newCondition: AlarmConditionVo) extends Command

  /* transient commands */
  final case class RetrieveAlarmCmd(alarmId: String) extends Command
  final case class EvalAlarmValueCmd(alarmId: String) extends Command

  /* persistent objects */
  /* events */
  final case class CreateAlarmEvt(name: String, signalId: String, conditions: Set[Seq[AlarmConditionVo]]) extends Event
  final case class RenameAlarmEvt(newName: String) extends Event
  final case class SelectSignalEvt(newSignalId: String) extends Event
  final case class AddConditionEvt(condition: AlarmConditionVo) extends Event
  final case class RemoveConditionEvt(condition: AlarmConditionVo) extends Event
  final case class ReplaceConditionEvt(oldCondition: AlarmConditionVo, newCondition: AlarmConditionVo) extends Event

  /* snapshot */
  final case class AlarmPo(alarmId: String, name: String, signalId: String, conditions: Set[Seq[AlarmConditionVo]])
}

class Alarm(signalShard: () => ActorRef,
            alarmRecordShard: () => ActorRef,
            registry: FunctionRegistry) extends PersistentActor {
  val log = Logging(context.system.eventStream, "sharded-alarms")

  val alarmId = s"${self.path.name}"
  var alarmName: Option[String] = None
  var signalId: Option[String] = None

  // transient values
  var conditions: Set[Seq[AlarmCondition]] = Set()
  var value: Option[Boolean] = None
  var matched: Option[AlarmCondition] = None
  var beginTs: Option[DateTime] = None
  var endTs: Option[DateTime] = None

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
    case x => log.info("RECOVER *IGNORED*: {} {}", this, x)
  }

  override def receiveCommand: Receive = {
    case CreateAlarmCmd(_, name, signalId, conds) =>
      persist(CreateAlarmEvt(name, signalId, conds))(updateState)
    case SelectSignalCmd(_, newSignalId) =>
      persist(SelectSignalEvt(newSignalId))(updateState)
    case AddConditionCmd(_, condition) =>
      persist(AddConditionEvt(condition))(updateState)
    case RemoveConditionCmd(_, condition) =>
      persist(RemoveConditionEvt(condition))(updateState)
    case ReplaceConditionCmd(_, old, newOne) =>
      persist(ReplaceConditionEvt(old, newOne))(updateState)
    case EvalAlarmValueCmd(_) =>
      signalShard() ! Signal.GetValueCmd(signalId.get)
    case sv: Signal.SignalValueVo =>
      evalConditionsWith(sv)
    case RetrieveAlarmCmd(_) =>
      sender() ! AlarmValueVo(alarmId, alarmName.get, signalId.get, conditions, value, matched, beginTs, endTs)
    case x => log.info("COMMAND *IGNORED*: {} {}", this, x)
  }

  private def updateState: (Event => Unit) = {
    case x => log.info("EVENT *IGNORED*: {} {}", this, x)
  }

  private def evalConditionsWith(sv: SignalValueVo): Unit = {
    for(a <- conditions) {
      for(c <- a) {
        if(c.contains(sv.value)) {
          if(value.isDefined && value.get) {
            if(c.eq(matched.get)) {
              // no changes. continue alarming state, no transition and no ending.
            } else {
              // transition.
              matched = Some(c)
              alarmRecordShard() ! AlarmRecord.TransitAlarmCmd(alarmId, beginTs.get, sv.ts, c.level, sv, c.positiveDesc)
            }
          } else {
            // new alarm.
            value = Some(true)
            matched = Some(c)
            beginTs = Some(sv.ts)
            alarmRecordShard() ! AlarmRecord.RaiseAlarmCmd(alarmId, beginTs.get, alarmName.get, c.level, sv, c.positiveDesc)
          }
          // break the iterations. only the first match matters.
          return
        }
      }
    }
    // None matched. no alarm or, end of alarm if current state is in alarming state.
    if(value.isDefined && value.get) {
      value = Some(false)
      endTs = Some(sv.ts)
      alarmRecordShard() ! AlarmRecord.EndAlarmCmd(alarmId, beginTs.get, endTs.get, sv, matched.get.negativeDesc)
    }
  }
}
