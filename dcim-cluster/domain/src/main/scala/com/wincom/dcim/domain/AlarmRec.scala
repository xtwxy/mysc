package com.wincom.dcim.domain

import akka.actor.{ActorRef, Props}
import akka.event.Logging
import akka.http.scaladsl.model.DateTime
import akka.persistence.PersistentActor
import com.wincom.dcim.domain.AlarmRec.{Event, ThresholdFuncVo}
import com.wincom.dcim.domain.Signal.SignalValueVo
import com.wincom.dcim.signal.{FunctionRegistry, UnaryFunction}

import scala.collection.mutable

/**
  * Created by wangxy on 17-8-28.
  */
object AlarmRec {
  def props(signalShard: () => ActorRef, registry: FunctionRegistry) = Props(new AlarmRec(signalShard, registry))

  def name(alarmRecId: String) = s"$alarmRecId"

  sealed trait Command {
    def alarmRecId: String
  }

  sealed trait Event extends Serializable

  final case class ThresholdFuncVo(name: String, params: Map[String, String]) extends Serializable

  final case class RaiseAlarmCmd(alarmRecId: String, ts: DateTime, level:Int, signalValue: SignalValueVo, desc: String)
  final case class TransitAlarmCmd(alarmRecId: String, ts: DateTime, level:Int, signalValue: SignalValueVo, desc: String)
  final case class EndAlarmCmd(alarmRecId: String, ts: DateTime, signalValue: SignalValueVo, desc: String)

}

class AlarmRec(signalShard: () => ActorRef, registry: FunctionRegistry) extends PersistentActor {
  val log = Logging(context.system.eventStream, "sharded-alarms")

  val alarmRecId = s"${self.path.name}"
  var alarmName: Option[String] = None
  var alarmLevel: Option[Int] = None
  var signalId: Option[String] = None
  var funcConfigs: collection.mutable.Seq[ThresholdFuncVo] = mutable.ArraySeq()

  // transient values
  var funcs: collection.mutable.Seq[UnaryFunction] = mutable.ArraySeq()
  var value: Option[AnyVal] = None
  var valueTs: Option[DateTime] = None

  override def persistenceId: String = s"${self.path.name}"

  override def receiveRecover: Receive = {
    case _ =>
  }

  override def receiveCommand: Receive = {
    case _ =>
  }

  private def updateState: (Event => Unit) = {
    case _ =>
  }
}
