package com.wincom.dcim.domain

import akka.actor.{ActorRef, Props, ReceiveTimeout}
import akka.event.Logging
import akka.pattern.ask
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import akka.util.Timeout
import com.wincom.dcim.message.common.ResponseType.{ALREADY_EXISTS, BAD_COMMAND, NOT_AVAILABLE, NOT_EXIST, SUCCESS}
import com.wincom.dcim.signal.{FunctionRegistry, InverseFunction, UnaryFunction}
import com.wincom.dcim.message.common._
import com.wincom.dcim.message.signal._
import com.wincom.dcim.message.driver
import com.wincom.dcim.message.driver.MapSignalKeyIdCmd
import org.joda.time.{DateTime, Duration}

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
}

class Signal(driverShard: () => ActorRef, registry: FunctionRegistry) extends PersistentActor {

  val log = Logging(context.system.eventStream, "sharded-signals")
  // configuration
  var signalName: Option[String] = None
  var signalType: Option[SignalType] = None
  var driverId: Option[String] = None
  var key: Option[String] = None
  var funcConfigs: collection.mutable.Seq[TransFuncVo] = mutable.ArraySeq()
  var deleted: Option[Boolean] = Some(true)

  // transient values
  var funcs: collection.mutable.Seq[UnaryFunction] = mutable.ArraySeq()
  var value: Option[SignalSnapshotValueVo] = None
  log.warning("{}", self.path.name)
  val signalId: String = s"${self.path.name.split("_")(1)}"

  override def persistenceId: String = s"${self.path.name}"

  implicit def requestTimeout: Timeout = FiniteDuration(20, SECONDS)

  implicit def executionContext: ExecutionContext = context.dispatcher

  def receiveRecover: PartialFunction[Any, Unit] = {
    case evt: Event =>
      updateState(evt)
    case SnapshotOffer(_, SignalPo(name, signalType, driverId, key, tf)) =>
      this.driverId = Some(driverId)
      this.signalName = Some(name)
      this.key = Some(key)
      this.signalType = Some(signalType)
      tf.foreach(x => this.funcConfigs = this.funcConfigs :+ TransFuncVo(x.name, x.params))
    case _: RecoveryCompleted =>
      if (isValid()) {
        driverShard() ! MapSignalKeyIdCmd(driverId.get, None, key.get, signalId)
      }
    case x => log.info("RECOVER *IGNORED*: {} {}", this, x)
  }

  def receiveCommand: PartialFunction[Any, Unit] = {
    case CreateSignalCmd(_, user, name, t, driverId, key, fs) =>
      if (!deleted.get && signalName.isDefined) {
        sender() ! Response(ALREADY_EXISTS, None)
      } else {
        persist(CreateSignalEvt(user, name, t, driverId, key, for (f <- fs) yield TransFuncPo(f.name, f.params)))(updateState)
      }
    case RenameSignalCmd(_, user, newName) =>
      if (signalName.isDefined) {
        persist(RenameSignalEvt(user, newName))(updateState)
      } else {
        sender() ! Response(NOT_EXIST, None)
      }
    case SelectDriverCmd(_, user, driverId) =>
      if (signalName.isDefined) {
        persist(SelectDriverEvt(user, driverId))(updateState)
      } else {
        sender() ! Response(NOT_EXIST, None)
      }
    case SelectTypeCmd(_, user, newType) =>
      if (signalName.isDefined) {
        persist(SelectTypeEvt(user, newType))(updateState)
      } else {
        sender() ! Response(NOT_EXIST, None)
      }
    case SelectKeyCmd(_, user, key) =>
      if (driverId.isDefined && signalName.isDefined) {
        persist(SelectKeyEvt(user, key))(updateState)
      } else {
        sender() ! Response(NOT_AVAILABLE, None)
      }
    case DeleteSignalCmd(_, user) =>
      if (signalName.isDefined) {
        persist(DeleteSignalEvt(user))(updateState)
      } else {
        sender() ! Response(NOT_EXIST, None)
      }
    case UpdateFuncsCmd(_, user, fs) =>
      if (signalName.isDefined) {
        this.funcConfigs = mutable.ArraySeq() ++ fs
        persist(UpdateFuncsEvt(user, for (f <- fs) yield TransFuncPo(f.name, f.params)))(updateState)
      } else {
        sender() ! Response(NOT_EXIST, None)
      }
    case RetrieveSignalCmd(_, user) =>
      if (signalName.isDefined) {
        sender() ! SignalVo(signalId, signalName.get, signalType.get, driverId.get, key.get, funcConfigs)
      } else {
        sender() ! Response(NOT_AVAILABLE, None)
      }
    case SaveSnapshotCmd(_, user) =>
      if (signalName.isDefined) {
        saveSnapshot(SignalPo(signalName.get, signalType.get, driverId.get, key.get, for (x <- funcConfigs) yield TransFuncPo(x.name, x.params)))
        sender() ! Response(SUCCESS, None)
      } else {
        sender() ! Response(NOT_EXIST, None)
      }
    case UpdateValueCmd(_, user, v) =>
      this.value = Some(v)
    case SetValueCmd(_, user, v) =>
      val theSender = sender()
      if (isValid()) {
        if (v.value.isDefined) {
          var x = v.value.get
          for (f <- funcs) {
            f match {
              case func: InverseFunction => x = func.inverse(x)
              case _ =>
            }
          }
          driverShard().ask(driver.SetSignalValueCmd(this.driverId.get, user, this.key.get, SignalValue.create(v.signalType, x))).mapTo[ValueObject].onComplete {
            case f: Success[ValueObject] =>
              f.value match {
                case x: SetValueRsp =>
                  theSender ! x
                case _ =>
                  theSender ! Response(NOT_AVAILABLE, None)
              }
            case _ =>
              theSender ! Response(NOT_AVAILABLE, None)
          }
        } else {
          sender() ! Response(BAD_COMMAND, None)
        }
      } else {
        sender() ! Response(NOT_EXIST, None)
      }
    case GetValueCmd(_, user) =>
      val theSender = sender()
      if (available()) {
        sender() ! this.value.get
      } else {
        driverShard().ask(driver.GetSignalValueCmd(driverId.get, user, key.get)).mapTo[ValueObject].onComplete {
          case f: Success[ValueObject] =>
            f.value match {
              case driver.DriverSignalSnapshotVo(driverId, key, ts, v) =>
                if (this.driverId.get.equals(driverId) && this.key.get.equals(key)) {
                  if (v.value.isDefined) {
                    var x = v.value.get
                    for (f <- this.funcs) {
                      x = f.transform(x)
                    }
                    this.value = Some(SignalValue.create(signalId, ts, v.signalType, x))
                    theSender ! this.value.get
                  } else {
                    theSender ! Response(NOT_AVAILABLE, None)
                  }
                } else {
                  theSender ! Response(BAD_COMMAND, None)
                }
              case _ =>
                theSender ! Response(NOT_AVAILABLE, None)
            }
          case _ =>
            theSender ! Response(NOT_AVAILABLE, None)
        }
      }
    case StartSignalCmd(_, _) =>
    case StopSignalCmd(_, _) =>
      context.stop(self)
    case x => log.info("COMMAND *IGNORED*: {} {}", this, x)
  }

  private def updateState: (Event => Unit) = {
    case CreateSignalEvt(user, name, t, driverId, key, fs) =>
      this.signalName = Some(name)
      this.driverId = Some(driverId)
      this.key = Some(key)
      this.signalType = Some(t)
      this.deleted = Some(false)
      updateFuncs(fs)
      replyToSender(Response(SUCCESS, None))
    case RenameSignalEvt(user, newName) =>
      this.signalName = Some(newName)
      replyToSender(Response(SUCCESS, None))
    case SelectDriverEvt(user, driverId) =>
      this.driverId = Some(driverId)
      replyToSender(Response(SUCCESS, None))
    case SelectTypeEvt(user, newType) =>
      this.signalType = Some(newType)
      replyToSender(Response(SUCCESS, None))
    case SelectKeyEvt(user, key) =>
      this.key = Some(key)
      driverShard() ! MapSignalKeyIdCmd(driverId.get, user, key, signalId)
      replyToSender(Response(SUCCESS, None))
    case DeleteSignalEvt(user) =>
      this.deleted = Some(true)
    case UpdateFuncsEvt(user, fs) =>
      updateFuncs(fs)
      replyToSender(Response(SUCCESS, None))
    case x => log.info("EVENT: {} {}", this, x)
  }

  private def updateFuncs(fs: Seq[TransFuncPo]): Unit = {
    this.funcConfigs = mutable.ArraySeq()
    this.funcs = mutable.ArraySeq()
    fs.foreach(f => {
      this.funcConfigs = this.funcConfigs :+ TransFuncVo(f.name, f.params)
      val func = registry.createUnary(f.name, f.params.asJava)
      if (func.isDefined) {
        this.funcs = this.funcs :+ func.get
      } else {
        log.warning("unary function cannot be initialized: {}", f)
      }
    })
  }

  private def isValid(): Boolean = {
    if (signalName.isDefined && driverId.isDefined && key.isDefined && !deleted.get) true else false
  }

  private def available(): Boolean = {
    if (isValid() && value.isDefined) {
      val d = Duration.millis(DateTime.now.getMillis - value.get.ts.seconds * 1000)
      val r = Duration.standardMinutes(1)
      val a = d.isShorterThan(r)
      return a
    } else {
      false
    }
  }

  private def replyToSender(msg: Any) = {
    if ("deadLetters" != sender().path.name) sender() ! msg
  }
}
