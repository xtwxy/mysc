package com.wincom.dcim.domain

import akka.actor.{ActorRef, Props, ReceiveTimeout}
import akka.event.Logging
import akka.http.scaladsl.model.DateTime
import akka.pattern.ask
import akka.persistence.{PersistentActor, SnapshotOffer}
import akka.util.Timeout
import com.wincom.dcim.domain.Signal._
import com.wincom.dcim.signal.{SignalTransFunc, SignalTransFuncRegistry}
import org.joda.time.Duration

import scala.collection.convert.ImplicitConversions._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, SECONDS}
import scala.util.Success

/**
  * Created by wangxy on 17-8-14.
  */
object Signal {
  def props(driverShard: () => ActorRef, registry: SignalTransFuncRegistry) = Props(new Signal(driverShard, registry))

  def name(signalId: String) = s"$signalId"

  sealed trait Command {
    def signalId: String
  }

  sealed trait Event extends Serializable

  /* value objects */
  final case class TransFunVo(funcName: String, params: Map[String, String]) extends Serializable
  final case class SignalVo(signalId: String, name: String, driverId: String, key: String, trans: Seq[TransFunVo]) extends Command

  final case class SignalValueVo(signalId: String, ts: DateTime, value: AnyVal) extends Command

  final case class Ok(signalId: String) extends Command

  final case class NotAvailable(signalId: String) extends Command

  final case class NotExist(signalId: String) extends Command

  final case class AlreadyExists(signalId: String) extends Command

  /* commands */
  final case class CreateSignalCmd(signalId: String, name: String, driverId: String, key: String) extends Command

  final case class RenameSignalCmd(signalId: String, newName: String) extends Command

  final case class SelectDriverCmd(signalId: String, driverId: String) extends Command

  final case class SelectKeyCmd(signalId: String, key: String) extends Command

  final case class RetrieveSignalCmd(signalId: String) extends Command
  final case class SaveSnapshotCmd(signalId: String) extends Command

  /* transient commands */
  final case class UpdateValueCmd(signalId: String, ts: DateTime, value: AnyVal) extends Command

  final case class SetValueCmd(signalId: String, value: AnyVal) extends Command
  final case class SetValueRsp(signalId: String, result: String) extends Command

  final case class GetValueCmd(signalId: String) extends Command

  final case class StartSignalCmd(signalId: String) extends Command

  final case class StopSignalCmd(signalId: String) extends Command

  final case class GetSupportedFuncsCmd(signalId: String) extends Command
  final case class GetSupportedFuncsRsp(signalId: String, funcNames: Set[String]) extends Command
  final case class GetFuncParamsCmd(signalId: String, funcName: String) extends Command
  final case class GetFuncParamsRsp(signalId: String, paramNames: Set[String]) extends Command
  /* events */
  final case class CreateSignalEvt(name: String, driverId: String, key: String) extends Event

  final case class RenameSignalEvt(newName: String) extends Event

  final case class SelectDriverEvt(driverId: String) extends Event

  final case class SelectKeyEvt(key: String) extends Event

  /* persistent objects */
  final case class TransFunPo(funcName: String, params: Map[String, String]) extends Serializable
  final case class SignalPo(name: String, driverId: String, key: String, trans: Seq[TransFunPo]) extends Event

}

class Signal(driverShard: () => ActorRef, registry: SignalTransFuncRegistry) extends PersistentActor {

  val log = Logging(context.system.eventStream, "sharded-signals")
  // configuration
  var signalName: Option[String] = None
  var driverId: Option[String] = None
  var key: Option[String] = None
  var trans: collection.mutable.Seq[TransFunVo] = collection.mutable.ArraySeq()

  // transient values
  var funcs: collection.mutable.Seq[SignalTransFunc] = collection.mutable.ArraySeq()
  var value: Option[AnyVal] = None
  var valueTs: Option[DateTime] = None

  val signalId: String = s"${self.path.name}"
  override def persistenceId: String = s"${self.path.name}"

  implicit def requestTimeout: Timeout = FiniteDuration(20, SECONDS)

  implicit def executionContext: ExecutionContext = context.dispatcher

  def receiveRecover: PartialFunction[Any, Unit] = {
    case evt: Event =>
      updateState(evt)
    case SnapshotOffer(_, SignalPo(name, driverId, key, tf)) =>
      this.driverId = Some(driverId)
      this.signalName = Some(name)
      this.key = Some(key)
      tf.foreach(x => this.trans = this.trans :+ TransFunVo(x.funcName, x.params))
    case x => log.info("RECOVER: {} {}", this, x)
  }

  def receiveCommand: PartialFunction[Any, Unit] = {
    case CreateSignalCmd(_, driverId, signalName, key) =>
      persist(CreateSignalEvt(driverId, signalName, key))(updateState)
    case RenameSignalCmd(_, newName) =>
      persist(RenameSignalEvt(newName))(updateState)
    case SelectDriverCmd(_, driverId) =>
      persist(SelectDriverEvt(driverId))(updateState)
    case SelectKeyCmd(_, key) =>
      persist(SelectKeyEvt(key))(updateState)
    case RetrieveSignalCmd(_) =>
      if (isValid) {
        sender() ! SignalVo(signalId, signalName.get, driverId.get, key.get, trans)
      } else {
        sender() ! NotAvailable(signalId)
      }
    case SaveSnapshotCmd(_) =>
      if (isValid) {
        saveSnapshot(SignalPo(signalName.get, driverId.get, key.get, for(x <- trans) yield TransFunPo(x.funcName, x.params)))
      }
    case UpdateValueCmd(_, ts, v) =>
      this.valueTs = Some(ts)
      this.value = Some(v)
    case SetValueCmd(_, v) =>
      val theSender = sender()
      var x = v
      for(f <- funcs) {
        x = f.inverse(x)
      }
      driverShard().ask(Driver.SetSignalValueCmd(this.driverId.get, this.key.get, x)).mapTo[Driver.Command].onComplete {
        case f: Success[Driver.Command] =>
          f.value match {
            case Driver.SetSignalValueRsp(_, _, result) =>
              theSender ! SetValueRsp(signalId, result)
            case _ =>
              theSender ! NotAvailable(signalId)
          }
        case _ =>
          theSender ! NotAvailable(signalId)
      }
    case GetValueCmd(_) =>
      val theSender = sender()
      if (available()) {
        sender() ! SignalValueVo(signalId, this.valueTs.get, this.value.get)
      } else {
        driverShard().ask(Driver.GetSignalValueCmd(driverId.get, key.get)).mapTo[Driver.Command].onComplete {
          case f: Success[Driver.Command] =>
            f.value match {
              case Driver.SignalValueVo(driverId, key, ts, v) =>
                if(this.driverId.get.equals(driverId) && this.key.get.equals(key)) {
                  var x = v
                  for(f <- this.funcs) {
                    x = f.transform(x)
                  }
                  this.value = Some(x)
                  this.valueTs = Some(ts)
                  theSender ! SignalValueVo(signalId, ts, x)
                } else {
                  theSender ! NotAvailable(signalId)
                }
              case _ =>
                theSender ! NotAvailable(signalId)
            }
          case _ =>
            theSender ! NotAvailable(signalId)
        }
      }
    case StartSignalCmd(_) =>
    case StopSignalCmd(_) =>
      context.stop(self)
    case GetSupportedFuncsCmd(_) =>
      sender() ! GetSupportedFuncsRsp(signalId, registry.names.toSet)
    case GetFuncParamsCmd(_, model) =>
      sender() ! GetFuncParamsRsp(signalId, registry.paramNames(model).toSet)
    case _: ReceiveTimeout =>
      context.stop(self)
    case x => log.info("COMMAND: {} {}", this, x)
  }

  private def updateState: (Event => Unit) = {
    case CreateSignalEvt(name, driverId, key) =>
      this.signalName = Some(name)
      this.driverId = Some(driverId)
      this.key = Some(key)
    case RenameSignalEvt(newName) =>
      this.signalName = Some(newName)
    case SelectDriverEvt(driverId) =>
      this.driverId = Some(driverId)
    case SelectKeyEvt(key) =>
      this.key = Some(key)
    case x => log.info("EVENT: {} {}", this, x)
  }

  private def isValid(): Boolean = {
    if (signalName.isDefined && driverId.isDefined && key.isDefined) true else false
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