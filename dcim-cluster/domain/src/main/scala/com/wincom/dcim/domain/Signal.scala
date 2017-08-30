package com.wincom.dcim.domain

import akka.actor.{ActorRef, Props, ReceiveTimeout}
import akka.event.Logging
import akka.http.scaladsl.model.DateTime
import akka.pattern.ask
import akka.persistence.{PersistentActor, SnapshotOffer}
import akka.util.Timeout
import com.wincom.dcim.domain.Signal._
import com.wincom.dcim.signal.{FunctionRegistry, InverseFunction, UnaryFunction}
import org.joda.time.Duration

import scala.collection.JavaConverters._
import scala.collection.convert.ImplicitConversions._
import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, SECONDS}
import scala.util.Success

/**
  * Created by wangxy on 17-8-14.
  */
object Signal {
  def props(driverShard: () => ActorRef, registry: FunctionRegistry) = Props(new Signal(driverShard, registry))

  def name(signalId: String) = s"$signalId"

  sealed trait Command {
    def signalId: String
  }

  sealed trait Event extends Serializable

  /* value objects */
  final case class TransFuncVo(name: String, params: Map[String, String]) extends Serializable
  final case class SignalVo(signalId: String, name: String, t: String, driverId: String, key: String, funcs: Seq[TransFuncVo]) extends Command

  final case class SignalValueVo(signalId: String, ts: DateTime, value: AnyVal) extends Command

  final case class Ok(signalId: String) extends Command

  final case class NotAvailable(signalId: String) extends Command

  final case class NotExist(signalId: String) extends Command

  final case class AlreadyExists(signalId: String) extends Command

  /* commands */
  final case class CreateSignalCmd(signalId: String, name: String, t:String, driverId: String, key: String, funcs: Seq[TransFuncVo]) extends Command

  final case class RenameSignalCmd(signalId: String, newName: String) extends Command

  final case class SelectDriverCmd(signalId: String, driverId: String) extends Command
  final case class SelectTypeCmd(signalId: String, newType: String) extends Command

  final case class SelectKeyCmd(signalId: String, key: String) extends Command
  final case class UpdateFuncsCmd(signalId: String, funcs: Seq[TransFuncVo]) extends Command

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
  final case class CreateSignalEvt(name: String, t: String, driverId: String, key: String, funcs: Seq[TransFuncPo]) extends Event

  final case class RenameSignalEvt(newName: String) extends Event

  final case class SelectDriverEvt(driverId: String) extends Event
  final case class SelectTypeEvt(newType: String) extends Event

  final case class SelectKeyEvt(key: String) extends Event
  final case class UpdateFuncsEvt(funcs: Seq[TransFuncPo]) extends Event

  /* persistent objects */
  final case class TransFuncPo(name: String, params: Map[String, String]) extends Serializable
  final case class SignalPo(name: String, t: String, driverId: String, key: String, funcs: Seq[TransFuncPo]) extends Event

}

class Signal(driverShard: () => ActorRef, registry: FunctionRegistry) extends PersistentActor {

  val log = Logging(context.system.eventStream, "sharded-signals")
  // configuration
  var signalName: Option[String] = None
  var signalType: Option[String] = None
  var driverId: Option[String] = None
  var key: Option[String] = None
  var funcConfigs: collection.mutable.Seq[TransFuncVo] = mutable.ArraySeq()

  // transient values
  var funcs: collection.mutable.Seq[UnaryFunction] = mutable.ArraySeq()
  var value: Option[AnyVal] = None
  var valueTs: Option[DateTime] = None

  val signalId: String = s"${self.path.name}"
  override def persistenceId: String = s"${self.path.name}"

  implicit def requestTimeout: Timeout = FiniteDuration(20, SECONDS)

  implicit def executionContext: ExecutionContext = context.dispatcher

  def receiveRecover: PartialFunction[Any, Unit] = {
    case evt: Event =>
      updateState(evt)
    case SnapshotOffer(_, SignalPo(name, t, driverId, key, tf)) =>
      this.driverId = Some(driverId)
      this.signalName = Some(name)
      this.key = Some(key)
      this.signalType = Some(t)
      tf.foreach(x => this.funcConfigs = this.funcConfigs :+ TransFuncVo(x.name, x.params))
    case x => log.info("RECOVER *IGNORED*: {} {}", this, x)
  }

  def receiveCommand: PartialFunction[Any, Unit] = {
    case CreateSignalCmd(_, name, t, driverId, key, fs) =>
      persist(CreateSignalEvt(name, t, driverId, key, for(f <- fs) yield TransFuncPo(f.name, f.params)))(updateState)
    case RenameSignalCmd(_, newName) =>
      persist(RenameSignalEvt(newName))(updateState)
    case SelectDriverCmd(_, driverId) =>
      persist(SelectDriverEvt(driverId))(updateState)
    case SelectTypeCmd(_, newType) =>
      persist(SelectTypeEvt(newType))(updateState)
    case SelectKeyCmd(_, key) =>
      persist(SelectKeyEvt(key))(updateState)
    case UpdateFuncsCmd(_, fs) =>
      this.funcConfigs = mutable.ArraySeq() ++ fs
      persist(UpdateFuncsEvt(for(f <- fs) yield TransFuncPo(f.name, f.params)))(updateState)
    case RetrieveSignalCmd(_) =>
      if (isValid) {
        sender() ! SignalVo(signalId, signalName.get, signalType.get, driverId.get, key.get, funcConfigs)
      } else {
        sender() ! NotAvailable(signalId)
      }
    case SaveSnapshotCmd(_) =>
      if (isValid) {
        saveSnapshot(SignalPo(signalName.get, signalType.get, driverId.get, key.get, for(x <- funcConfigs) yield TransFuncPo(x.name, x.params)))
      }
    case UpdateValueCmd(_, ts, v) =>
      this.valueTs = Some(ts)
      this.value = Some(v)
    case SetValueCmd(_, v) =>
      val theSender = sender()
      var x = v
      for(f <- funcs) {
        if(f.isInstanceOf[InverseFunction]) {
          x = f.asInstanceOf[InverseFunction].inverse(x)
        }
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
    case x => log.info("COMMAND *IGNORED*: {} {}", this, x)
  }

  private def updateState: (Event => Unit) = {
    case CreateSignalEvt(name, t, driverId, key, fs) =>
      this.signalName = Some(name)
      this.driverId = Some(driverId)
      this.key = Some(key)
      this.signalType = Some(t)
      updateFuncs(fs)
    case RenameSignalEvt(newName) =>
      this.signalName = Some(newName)
    case SelectDriverEvt(driverId) =>
      this.driverId = Some(driverId)
    case SelectTypeEvt(newType) =>
      this.signalType = Some(newType)
    case SelectKeyEvt(key) =>
      this.key = Some(key)
    case UpdateFuncsEvt(fs) =>
      updateFuncs(fs)
    case x => log.info("EVENT: {} {}", this, x)
  }

  private def updateFuncs(fs: Seq[TransFuncPo]): Unit = {
    this.funcConfigs = mutable.ArraySeq()
    this.funcs = mutable.ArraySeq()
    fs.foreach(f => {
      this.funcConfigs = this.funcConfigs:+ TransFuncVo(f.name, f.params)
      val func = registry.createUnary(f.name, f.params.asJava)
      if(func.isDefined) {
        this.funcs = this.funcs :+ func.get
      } else {
        log.warning("unary function cannot be initialized: {}", f)
      }
    })
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