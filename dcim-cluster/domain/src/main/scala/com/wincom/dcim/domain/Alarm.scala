package com.wincom.dcim.domain

import akka.actor.{ActorRef, Props}
import akka.event.Logging
import akka.http.scaladsl.model.DateTime
import akka.persistence.PersistentActor
import akka.util.Timeout
import com.wincom.dcim.domain.Alarm._
import com.wincom.dcim.domain.AlarmCondition.AlarmConditionVo
import com.wincom.dcim.domain.Signal.SignalValueVo
import com.wincom.dcim.signal.FunctionRegistry

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by wangxy on 17-8-28.
  */
object Alarm {
  def props(signalShard: () => ActorRef,
            alarmRecordShard: () => ActorRef,
            registry: FunctionRegistry) = Props(new Alarm(signalShard, alarmRecordShard, registry))

  def name(alarmId: String) = s"$alarmId"

  sealed trait Command {
    def alarmId: String
  }

  sealed trait Response

  sealed trait Event

  /* value objects */
  final case class AlarmVo(alarmId: String,
                           name: String,
                           signalId: String,
                           conditions: Set[Seq[AlarmConditionVo]]) extends Response

  final case class AlarmValueVo(alarmId: String,
                                value: Option[Boolean],
                                matched: Option[AlarmConditionVo],
                                beginTs: Option[DateTime],
                                endTs: Option[DateTime]) extends Response

  final case object Ok extends Response

  final case object NotAvailable extends Response

  final case object BadCmd extends Response

  /* commands */
  final case class CreateAlarmCmd(alarmId: String, name: String, signalId: String, conditions: Set[Seq[AlarmConditionVo]]) extends Command

  final case class RenameAlarmCmd(alarmId: String, newName: String) extends Command

  final case class SelectSignalCmd(alarmId: String, newSignalId: String) extends Command

  final case class AddConditionCmd(alarmId: String, condition: AlarmConditionVo) extends Command

  final case class RemoveConditionCmd(alarmId: String, condition: AlarmConditionVo) extends Command

  final case class ReplaceConditionCmd(alarmId: String, oldCondition: AlarmConditionVo, newCondition: AlarmConditionVo) extends Command

  /* transient commands */
  final case class RetrieveAlarmCmd(alarmId: String) extends Command

  final case class GetAlarmValueCmd(alarmId: String) extends Command

  final case class EvalAlarmValueCmd(alarmId: String) extends Command

  final case class PassivateAlarmCmd(alarmId: String) extends Command


  /* persistent objects */
  /* events */
  final case class CreateAlarmEvt(name: String, signalId: String, conditions: Set[Seq[AlarmConditionVo]]) extends Event

  final case class RenameAlarmEvt(newName: String) extends Event

  final case class SelectSignalEvt(newSignalId: String) extends Event

  final case class AddConditionEvt(condition: AlarmConditionVo) extends Event

  final case class RemoveConditionEvt(condition: AlarmConditionVo) extends Event

  final case class ReplaceConditionEvt(old: AlarmConditionVo, newOne: AlarmConditionVo) extends Event

  /* snapshot */
  final case class AlarmPo(alarmId: String, name: String, signalId: String, conditions: Set[Seq[AlarmConditionVo]])

}

class Alarm(signalShard: () => ActorRef,
            alarmRecordShard: () => ActorRef,
            implicit val registry: FunctionRegistry) extends PersistentActor {
  val log = Logging(context.system.eventStream, "sharded-alarms")

  val alarmId = s"${self.path.name}"
  var alarmName: Option[String] = None
  var signalId: Option[String] = None

  // transient values
  var conditions: mutable.Set[Seq[AlarmCondition]] = mutable.Set()
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
      if (isValid()) {
        persist(CreateAlarmEvt(name, signalId, conds))(updateState)
      } else {
        sender() ! NotAvailable
      }
    case SelectSignalCmd(_, newSignalId) =>
      if (isValid()) {
        persist(SelectSignalEvt(newSignalId))(updateState)
      } else {
        sender() ! NotAvailable
      }
    case AddConditionCmd(_, condition) =>
      if (isValid()) {
        persist(AddConditionEvt(condition))(updateState)
      } else {
        sender() ! NotAvailable
      }
    case RemoveConditionCmd(_, condition) =>
      if (isValid()) {
        persist(RemoveConditionEvt(condition))(updateState)
      } else {
        sender() ! NotAvailable
      }
    case ReplaceConditionCmd(_, old, newOne) =>
      if (isValid()) {
        persist(ReplaceConditionEvt(old, newOne))(updateState)
      } else {
        sender() ! NotAvailable
      }
    case EvalAlarmValueCmd(_) =>
      if (isValid()) {
        signalShard() ! Signal.GetValueCmd(signalId.get)
      }
    case sv: Signal.SignalValueVo =>
      evalConditionsWith(sv)
    case RetrieveAlarmCmd(_) =>
      if (isValid()) {
        sender() ! AlarmVo(alarmId, alarmName.get, signalId.get, conditionsAsVo)
      } else {
        sender() ! NotAvailable
      }
    case GetAlarmValueCmd(_) =>
      if (isValid()) {
        sender() ! AlarmValueVo(alarmId, value, if (matched.isDefined) Some(new AlarmConditionVo(matched.get)) else None, beginTs, endTs)
      } else {
        sender() ! NotAvailable
      }
    case x => log.info("COMMAND *IGNORED*: {} {}", this, x)
  }

  private def conditionsAsVo(): Set[Seq[AlarmConditionVo]] = {
    var conds: mutable.Set[Seq[AlarmConditionVo]] = mutable.Set()
    for (cs <- conditions) {
      var s = mutable.ArraySeq[AlarmConditionVo]()
      for (c <- cs) {
        s :+= new AlarmConditionVo(c)
      }
      if (!(s.isEmpty)) conds += s
    }
    conds.toSet
  }

  private def updateState: (Event => Unit) = {
    case CreateAlarmEvt(name, signalId, conds) =>
      this.alarmName = Some(name)
      this.signalId = Some(signalId)
      createConditions(conds)
      replyTo(Ok)
    case SelectSignalEvt(newSignalId) =>
      this.signalId = Some(newSignalId)
      replyTo(Ok)
    case AddConditionEvt(condition) =>
      if (addCondition(condition)) {
        replyTo(Ok)
      } else {
        replyTo(BadCmd)
      }
    case RemoveConditionEvt(condition) =>
      removeCondition(condition)
      replyTo(Ok)
    case ReplaceConditionEvt(old, newOne) =>
      if (replaceCondition(old, newOne)) {
        replyTo(Ok)
      } else {
        replyTo(BadCmd)
      }
    case x => log.info("EVENT *IGNORED*: {} {}", this, x)
  }

  private def replyTo(msg: Any) = {
    if ("deadLetters" != sender().path.name) sender() ! msg
  }

  private def evalConditionsWith(sv: SignalValueVo): Unit = {
    for (a <- conditions) {
      for (c <- a) {
        if (c.contains(sv.value)) {
          if (value.isDefined && value.get) {
            if (c.eq(matched.get)) {
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
    if (value.isDefined && value.get) {
      value = Some(false)
      endTs = Some(sv.ts)
      alarmRecordShard() ! AlarmRecord.EndAlarmCmd(alarmId, beginTs.get, endTs.get, sv, matched.get.negativeDesc)
    }
  }

  private def createConditions(conds: Set[Seq[AlarmConditionVo]]): Unit = {
    for (cs <- conds) {
      var seq: mutable.Seq[AlarmCondition] = mutable.ArraySeq()
      for (c <- cs) {
        seq = seq :+ AlarmCondition(c)
      }
      if (!seq.isEmpty) this.conditions = this.conditions + seq
    }
  }

  private def addCondition(cond: AlarmConditionVo): Boolean = {
    var set: mutable.Set[AlarmCondition] = mutable.Set()
    set = set + AlarmCondition(cond)
    for (cs <- conditions) {
      for (c <- cs) {
        set = set + c
      }
    }
    val result = partitionConditions(set)
    if (result._1) conditions = result._2
    result._1
  }

  def validatePartitions(conds: mutable.Set[Seq[AlarmCondition]]): Boolean = {
    var seq: mutable.Seq[AlarmCondition] = mutable.Seq()
    for (cs <- conds) {
      if (!cs.isEmpty) {
        seq :+= cs.last
      }
    }
    for (i <- 0 to seq.length) {
      for (j <- i to seq.length) {
        if (seq(i).intersects(seq(j))) false
      }
    }
    true
  }

  private def partitionConditions(set: mutable.Set[AlarmCondition]): (Boolean, mutable.Set[Seq[AlarmCondition]]) = {
    var conds: mutable.Set[Seq[AlarmCondition]] = mutable.Set()
    val ordering = Ordering.fromLessThan[AlarmCondition]((x, y) => x != y && x.subsetOf(y))
    while (!set.isEmpty) {
      var s = mutable.TreeSet.empty(ordering)
      var seq: Seq[AlarmCondition] = mutable.ArraySeq()
      set.foreach(x => s += x)
      set --= s
      s.foreach(x => seq :+= x)
      conds += seq
    }
    val result = validatePartitions(conds)
    (result, conds)
  }

  private def removeCondition(cond: AlarmConditionVo): Unit = {
    var newConditions: Set[Seq[AlarmCondition]] = Set()
    for (cs <- conditions) {
      var seq: mutable.Seq[AlarmCondition] = mutable.ArraySeq()
      for (c <- cs) {
        if (!cond.equals(new AlarmConditionVo(c))) {
          seq = seq :+ c
        }
      }
      if (!seq.isEmpty) newConditions = newConditions + seq
    }
  }

  private def replaceCondition(old: AlarmConditionVo, newOne: AlarmConditionVo): Boolean = {
    removeCondition(old)
    addCondition(newOne)
  }

  private def isValid(): Boolean = {
    alarmName.isDefined && signalId.isDefined
  }
}
