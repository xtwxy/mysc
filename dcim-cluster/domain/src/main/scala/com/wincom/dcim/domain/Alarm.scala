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
final case class AlarmConditionVo(func: ThresholdFuncVo, positiveDesc: String, negativeDesc: String)

object ThresholdFuncVo {
  def apply(name: String, params: Map[String, String]): ThresholdFuncVo = new ThresholdFuncVo(name, params)
  def apply(func: ThresholdFunc): ThresholdFuncVo = new ThresholdFuncVo(func.name, func.params)
}

object AlarmConditionVo {
  def apply(func: ThresholdFuncVo, positiveDesc: String, negativeDesc: String): AlarmConditionVo = new AlarmConditionVo(func, positiveDesc, negativeDesc)
  def apply(cond: AlarmCondition): AlarmConditionVo = new AlarmConditionVo(ThresholdFuncVo(cond.func), cond.positiveDesc, cond.negativeDesc)
}

final case class ThresholdFunc(name: String, params: Map[String, String], func: SetFunction) extends SetFunction {
  override def contains(e: AnyVal): Boolean = func.contains(e)
  override def subsetOf(f: SetFunction): Boolean = func.subsetOf(f)
  override def intersects(f: SetFunction): Boolean = func.intersects(f)
}

final case class AlarmCondition(func: ThresholdFunc, positiveDesc: String, negativeDesc: String) extends SetFunction {
  override def contains(e: AnyVal): Boolean = func.contains(e)
  override def subsetOf(f: SetFunction): Boolean = func.subsetOf(f)
  override def intersects(f: SetFunction): Boolean = func.intersects(f)
}

object Alarm {
  def props(signalShard: () => ActorRef, registry: FunctionRegistry) = Props(new Alarm(signalShard, registry))

  def name(alarmId: String) = s"$alarmId"

  sealed trait Command {
    def alarmId: String
  }

  sealed trait Event extends Serializable

  /* value objects */


  final case class Ok(alarmId: String) extends Command

  final case class NotAvailable(alarmId: String) extends Command

  /* commands */

  /* transient commands */

  /* events */

}

class Alarm(signalShard: () => ActorRef, registry: FunctionRegistry) extends PersistentActor {
  val log = Logging(context.system.eventStream, "sharded-alarms")

  val alarmId = s"${self.path.name}"
  var alarmName: Option[String] = None
  var alarmLevel: Option[Int] = None
  var signalId: Option[String] = None

  // transient values
  var conditions: Set[Seq[AlarmCondition]] = Set()
  var value: Option[Boolean] = None
  var desc: Option[String] = None
  var matched: Option[AlarmCondition] = None
  var beginTs: Option[DateTime] = None
  var endTs: Option[DateTime] = None

  val evalPeriod = Settings(context.system).alarm.evalPeriod.toMillis milliseconds

  override def preStart(): Unit = {
    super.preStart()
    context.system.scheduler.schedule(0 milliseconds,
      evalPeriod,
      self,
      alarmId
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
    case x => log.info("COMMAND *IGNORED*: {} {}", this, x)
  }

  private def updateState: (Event => Unit) = {
    case x => log.info("EVENT *IGNORED*: {} {}", this, x)
  }

  private def evalConditions(sv: SignalValueVo): Unit = {
    for(a <- conditions) {
      for(c <- a) {
        if(c.contains(sv.value)) {
          if(value.isDefined && value.get) {
            if(c.eq(matched.get)) {
              // no changes.
            } else {
              // transition.
            }
          } else {
              // new alarm.
          }
          return
        }
      }
    }
    // None matched.
    return
  }
}
