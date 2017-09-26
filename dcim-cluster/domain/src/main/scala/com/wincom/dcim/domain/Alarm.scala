package com.wincom.dcim.domain

import akka.actor.{ActorRef, Props}
import akka.event.Logging
import akka.persistence.PersistentActor
import akka.util.Timeout
import com.google.protobuf.timestamp.Timestamp
import com.wincom.dcim.message.common._
import com.wincom.dcim.message.common.ResponseType._
import com.wincom.dcim.message.alarm._
import com.wincom.dcim.message.alarmrecord
import com.wincom.dcim.message.signal
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

  def name(alarmId: String) = s"${alarmId}"
}

class Alarm(signalShard: () => ActorRef,
            alarmRecordShard: () => ActorRef,
            implicit val registry: FunctionRegistry) extends PersistentActor {
  val log = Logging(context.system.eventStream, "sharded-alarms")

  val alarmId = s"${self.path.name.split(",")(1)}"
  var alarmName: Option[String] = None
  var signalId: Option[String] = None

  // transient values
  var conditions: mutable.Set[Seq[AlarmCondition]] = mutable.Set()
  var value: Option[Boolean] = None
  var matched: Option[AlarmCondition] = None
  var beginTs: Option[Timestamp] = None
  var endTs: Option[Timestamp] = None

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
    case CreateAlarmCmd(_, user, name, signalId, conds) =>
      if (isValid()) {
        sender() ! Response(ALREADY_EXISTS, None)
      } else {
        persist(CreateAlarmEvt(user, name, signalId, conds))(updateState)
      }
    case SelectSignalCmd(_, user, newSignalId) =>
      if (isValid()) {
        persist(SelectSignalEvt(user, newSignalId))(updateState)
      } else {
        sender() ! Response(NOT_AVAILABLE, None)
      }
    case AddConditionCmd(_, user, condition) =>
      if (isValid()) {
        persist(AddConditionEvt(user, condition))(updateState)
      } else {
        sender() ! Response(NOT_AVAILABLE, None)
      }
    case RemoveConditionCmd(_, user, condition) =>
      if (isValid()) {
        persist(RemoveConditionEvt(user, condition))(updateState)
      } else {
        sender() ! Response(NOT_AVAILABLE, None)
      }
    case ReplaceConditionCmd(_, user, old, newOne) =>
      if (isValid()) {
        persist(ReplaceConditionEvt(user, old, newOne))(updateState)
      } else {
        sender() ! Response(NOT_AVAILABLE, None)
      }
    case EvalAlarmValueCmd(_, _) =>
      if (isValid()) {
        signalShard() ! signal.GetValueCmd(signalId.get)
      }
    case sv: signal.SignalSnapshotValueVo =>
      evalConditionsWith(sv)
    case RetrieveAlarmCmd(_, _) =>
      if (isValid()) {
        sender() ! AlarmVo(alarmId, alarmName.get, signalId.get, exclusiveConditions(conditions))
      } else {
        sender() ! Response(NOT_AVAILABLE, None)
      }
    case GetAlarmValueCmd(_, _) =>
      if (isValid()) {
        sender() ! AlarmValueVo(alarmId, value, if (matched.isDefined) Some(AlarmCondition.valueObjectOf(matched.get)) else None, beginTs, endTs)
      } else {
        sender() ! Response(NOT_AVAILABLE, None)
      }
    case x => log.info("COMMAND *IGNORED*: {} {}", this, x)
  }

  private def conditionsAsVo(): Set[Seq[AlarmConditionVo]] = {
    var conds: mutable.Set[Seq[AlarmConditionVo]] = mutable.Set()
    for (cs <- conditions) {
      var s = mutable.ArraySeq[AlarmConditionVo]()
      for (c <- cs) {
        s :+= AlarmCondition.valueObjectOf(c)
      }
      if (!(s.isEmpty)) conds += s
    }
    conds.toSet
  }

  private def updateState: (Event => Unit) = {
    case CreateAlarmEvt(_, name, signalId, conds) =>
      this.alarmName = Some(name)
      this.signalId = signalId
      if(conds.isDefined) createConditions(conds.get)
      replyToSender(Response(SUCCESS, None))
    case SelectSignalEvt(_, newSignalId) =>
      this.signalId = Some(newSignalId)
      replyToSender(Response(SUCCESS, None))
    case AddConditionEvt(_, condition) =>
      if (addCondition(condition)) {
        replyToSender(Response(SUCCESS, None))
      } else {
        replyToSender(Response(BAD_COMMAND, None))
      }
    case RemoveConditionEvt(_, condition) =>
      removeCondition(condition)
      replyToSender(Response(SUCCESS, None))
    case ReplaceConditionEvt(_, old, newOne) =>
      if (replaceCondition(old, newOne)) {
        replyToSender(Response(SUCCESS, None))
      } else {
        replyToSender(Response(BAD_COMMAND, None))
      }
    case x => log.info("EVENT *IGNORED*: {} {}", this, x)
  }

  private def replyToSender(msg: Any) = {
    if ("deadLetters" != sender().path.name) sender() ! msg
  }

  private def evalConditionsWith(sv: signal.SignalSnapshotValueVo): Unit = {
    for (a <- conditions) {
      for (c <- a) {
        if (c.contains(sv.value)) {
          if (value.isDefined && value.get) {
            if (c.eq(matched.get)) {
              // no changes. continue alarming state, no transition and no ending.
            } else {
              // transition.
              matched = Some(c)
              alarmRecordShard() ! alarmrecord.TransitAlarmCmd(alarmId, None, beginTs.get, sv.ts, c.level, sv, Some(c.positiveDesc))
            }
          } else {
            // new alarm.
            value = Some(true)
            matched = Some(c)
            beginTs = Some(sv.ts)
            alarmRecordShard() ! alarmrecord.RaiseAlarmCmd(alarmId, None, beginTs.get, alarmName.get, c.level, sv, Some(c.positiveDesc))
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
      alarmRecordShard() ! alarmrecord.EndAlarmCmd(alarmId, None, beginTs.get, endTs.get, sv, Some(matched.get.negativeDesc))
    }
  }

  private def createConditions(conds: ExclusiveConditionVo): Unit = {
    for (cs <- conds.exclusive) {
      var seq: mutable.Seq[AlarmCondition] = mutable.ArraySeq()
      for (c <- cs.ordered) {
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
        if (!cond.equals(AlarmCondition.valueObjectOf(c))) {
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

  def exclusiveConditions(conditions: mutable.Set[Seq[AlarmCondition]]): Option[ExclusiveConditionVo] = {
    None
  }

  private def isValid(): Boolean = {
    alarmName.isDefined && signalId.isDefined
  }
}
