package com.wincom.dcim.domain

import java.io.Serializable

import akka.actor.{ActorRef, Props, ReceiveTimeout}
import akka.event.Logging
import akka.http.scaladsl.model.DateTime
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import akka.util.Timeout
import akka.util.Timeout.durationToTimeout
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap.create
import com.wincom.dcim.domain.Driver._
import com.wincom.dcim.domain.Signal.UpdateValueCmd
import com.wincom.dcim.driver.DriverCodecRegistry

import scala.collection.convert.ImplicitConversions._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, SECONDS}


/**
  * Created by wangxy on 17-8-14.
  */

object Driver {

  def props(shardedSignal: () => ActorRef, registry: DriverCodecRegistry) = Props(new Driver(shardedSignal, registry))

  def name(driverId: String) = s"$driverId"

  sealed trait Command {
    def driverId: String
  }

  sealed trait Response

  sealed trait Event

  /* value objects */
  final case class DriverVo(driverId: String,
                            name: String,
                            model: String,
                            initParams: Map[String, String],
                            signalIdMap: Map[String, String]) extends Response

  final case class SignalValue(key: String, ts: DateTime, value: AnyVal) extends Serializable

  final case class SignalValueVo(driverId: String, key: String, ts: DateTime, value: AnyVal) extends Response

  final case class SignalValuesVo(driverId: String, signalValues: Seq[SignalValue]) extends Response

  final case object Ok extends Response

  final case object NotAvailable extends Response

  final case object NotExist extends Response

  final case object AlreadyExists extends Response

  /* commands */
  final case class CreateDriverCmd(driverId: String, name: String, model: String, initParams: Map[String, String], signalIdMap: Map[String, String]) extends Command

  final case class RenameDriverCmd(driverId: String, newName: String) extends Command

  final case class ChangeModelCmd(driverId: String, newModel: String) extends Command

  final case class SaveSnapshotCmd(driverId: String) extends Command

  final case class AddParamsCmd(driverId: String, params: Map[String, String]) extends Command

  final case class RemoveParamsCmd(driverId: String, params: Map[String, String]) extends Command

  final case class MapSignalKeyIdCmd(driverId: String, key: String, signalId: String) extends Command

  /* transient commands */
  final case class GetSignalValueCmd(driverId: String, key: String) extends Command

  final case class GetSignalValuesCmd(driverId: String, keys: Set[String]) extends Command

  final case class SetSignalValueCmd(driverId: String, key: String, value: AnyVal) extends Command

  final case class SetSignalValueRsp(driverId: String, key: String, result: String) extends Response

  final case class SetSignalValuesCmd(driverId: String, values: Map[String, AnyVal]) extends Command

  final case class SetSignalValuesRsp(driverId: String, results: Map[String, String]) extends Response

  final case class UpdateSignalValuesCmd(driverId: String, values: Seq[SignalValue]) extends Command

  final case class SendBytesCmd(driverId: String, bytes: Array[Byte]) extends Command

  final case class RetrieveDriverCmd(driverId: String) extends Command

  final case class StartDriverCmd(driverId: String) extends Command

  final case class StopDriverCmd(driverId: String) extends Command

  final case class GetSupportedModelsCmd(driverId: String) extends Command

  final case class GetSupportedModelsRsp(driverId: String, modelNames: Set[String]) extends Response

  final case class GetModelParamsCmd(driverId: String, modelName: String) extends Command

  final case class GetModelParamsRsp(driverId: String, paramNames: Set[String]) extends Response

  /* events */
  final case class CreateDriverEvt(name: String, model: String, initParams: Map[String, String], signalIdMap: Map[String, String]) extends Event

  final case class RenameDriverEvt(newName: String) extends Event

  final case class ChangeModelEvt(newModel: String) extends Event

  final case class AddParamsEvt(params: Map[String, String]) extends Event

  final case class RemoveParamsEvt(params: Map[String, String]) extends Event

  final case class MapSignalKeyIdEvt(key: String, signalId: String) extends Event

  /* persistent objects */
  final case class DriverPo(name: String, model: String, initParams: Map[String, String], signalIdMap: Map[String, String]) extends Serializable

}

class Driver(val shardedSignal: () => ActorRef, val registry: DriverCodecRegistry) extends PersistentActor {

  val log = Logging(context.system.eventStream, "sharded-drivers")

  var driverName: Option[String] = None
  var modelName: Option[String] = None
  var initParams: collection.mutable.Map[String, String] = new collection.mutable.HashMap()
  var driverCodec: Option[ActorRef] = None
  // key => id
  val signalIdMap: BiMap[String, String] = create()

  val driverId: String = s"${self.path.name}"

  override def persistenceId: String = s"${self.path.name}"

  implicit def requestTimeout: Timeout = FiniteDuration(20, SECONDS)

  implicit def executionContext: ExecutionContext = context.dispatcher

  def receiveRecover: PartialFunction[Any, Unit] = {
    case evt: Event =>
      updateState(evt)
    case SnapshotOffer(_, DriverPo(name, model, params, idMap)) =>
      this.driverName = Some(name)
      this.modelName = Some(model)
      this.initParams = this.initParams ++ params
      for ((k, v) <- idMap) this.signalIdMap.put(k, v)
    case x: RecoveryCompleted =>
      log.info("RECOVERY Completed: {} {}", this, x)
      if (!isValid || !createCodec()) {
        log.warning("Cannot create Driver Codec.")
      }
    case x => log.info("RECOVER: {} {}", this, x)
  }

  def receiveCommand: PartialFunction[Any, Unit] = {
    case CreateDriverCmd(_, name, model, params, idMap) =>
      persist(CreateDriverEvt(name, model, params, idMap))(updateState)
    case RenameDriverCmd(_, newName) =>
      if (isValid) {
        persist(RenameDriverEvt(newName))(updateState)
      } else {
        sender() ! NotExist
      }
    case ChangeModelCmd(_, newModel) =>
      if (isValid) {
        persist(ChangeModelEvt(newModel))(updateState)
      } else {
        sender() ! NotExist
      }
    case AddParamsCmd(_, params) =>
      if (isValid) {
        persist(AddParamsEvt(params))(updateState)
      } else {
        sender() ! NotExist
      }
    case RemoveParamsCmd(_, params) =>
      if (isValid) {
        persist(RemoveParamsEvt(params))(updateState)
      } else {
        sender() ! NotExist
      }
    case SaveSnapshotCmd(_) =>
      if (isValid) {
        saveSnapshot(DriverPo(driverName.get, modelName.get, initParams.toMap, signalIdMap.toMap))
        sender() ! Ok
      } else {
        log.warning("Save snapshot failed - Not a valid object")
        sender() ! NotExist
      }
    case MapSignalKeyIdCmd(_, key, signalId) =>
      if (isValid) {
        persist(MapSignalKeyIdEvt(key, signalId))(updateState)
      } else {
        sender() ! NotExist
      }

    case cmd: GetSignalValueCmd =>
      if (isValid) {
        driverCodec.get forward cmd
      } else {
        sender() ! NotExist
      }
    case cmd: GetSignalValuesCmd =>
      if (isValid) {
        driverCodec.get forward cmd
      } else {
        sender() ! NotExist
      }
    case cmd: SetSignalValueCmd =>
      if (isValid) {
        driverCodec.get forward cmd
      } else {
        sender() ! NotExist
      }
    case cmd: SetSignalValuesCmd =>
      if (isValid) {
        driverCodec.get forward cmd
      } else {
        sender() ! NotExist
      }
    case UpdateSignalValuesCmd(_, values) =>
      if (isValid) {
        for (v <- values) {
          shardedSignal() ! UpdateValueCmd(v.key, v.ts, v.value)
        }
      } else {
        sender() ! NotExist
      }
    case cmd: SendBytesCmd =>
      if (isValid) {
        driverCodec.get forward cmd
      } else {
        sender() ! NotExist
      }
    case RetrieveDriverCmd(_) =>
      if (isValid) {
        sender() ! DriverVo(driverId, driverName.get, modelName.get, initParams.toMap, signalIdMap.toMap)
      } else {
        sender() ! NotExist
      }
    case StartDriverCmd(_) =>
    case StopDriverCmd(_) =>
      stop()
    case GetSupportedModelsCmd(_) =>
      if (isValid) {
        sender() ! GetSupportedModelsRsp(driverId, registry.names.toSet)
      } else {
        sender() ! NotExist
      }
    case GetModelParamsCmd(_, model) =>
      if (isValid) {
        sender() ! GetSupportedModelsRsp(driverId, registry.paramNames(model).toSet)
      } else {
        sender() ! NotExist
      }
    case _: ReceiveTimeout =>
      stop()
    case x => log.info("COMMAND: {} {}", this, x)
  }

  private def updateState: (Event => Unit) = {
    case CreateDriverEvt(name, model, params, idMap) =>
      this.driverName = Some(name)
      this.modelName = Some(model)
      this.initParams = this.initParams ++ params
      for ((k, v) <- idMap) this.signalIdMap.put(k, v)
      replyTo(Ok)
    case RenameDriverEvt(newName) =>
      this.driverName = Some(newName)
      replyTo(Ok)
    case ChangeModelEvt(newModel) =>
      this.modelName = Some(newModel)
      replyTo(Ok)
    case AddParamsEvt(params) =>
      this.initParams = this.initParams ++ params
      replyTo(Ok)
    case RemoveParamsEvt(params) =>
      this.initParams = this.initParams.filter(p => !params.contains(p._1))
      replyTo(Ok)
    case MapSignalKeyIdEvt(key, signalId) =>
      this.signalIdMap.put(key, signalId)
      replyTo(Ok)
    case x => log.info("UPDATE IGNORED: {} {}", this, x)
  }

  private def stop() = {
    log.info("Stopping: {}", this)
    if (driverCodec.isDefined) {
      context.stop(driverCodec.get)
      driverCodec = None
    }
    context.stop(self)
  }

  private def isValid: Boolean = {
    if (driverName.isDefined && modelName.isDefined) true else false
  }

  private def createCodec(): Boolean = {
    val p = registry.create(this.modelName.get, this.initParams)
    if (p.isDefined) {
      this.driverCodec = Some(context.system.actorOf(p.get, s"${this.modelName.get}_${driverId}"))
      log.info("Driver Codec: {}", this.driverCodec)
      true
    } else {
      log.info("No Driver Codec Factory: {}", this.modelName)
      false
    }
  }

  private def replyTo(msg: Any) = {
    if ("deadLetters" != sender().path.name) sender() ! msg
  }
}