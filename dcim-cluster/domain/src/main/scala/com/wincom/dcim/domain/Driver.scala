package com.wincom.dcim.domain


import akka.actor.{ActorRef, Props, ReceiveTimeout}
import akka.event.Logging
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import akka.util.Timeout
import akka.util.Timeout.durationToTimeout
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap.create
import com.wincom.dcim.driver.DriverCodecRegistry
import com.wincom.dcim.message.common._
import com.wincom.dcim.message.driver._
import com.wincom.dcim.message.common.ResponseType._

import scala.collection.convert.ImplicitConversions._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, SECONDS}


/**
  * Created by wangxy on 17-8-14.
  */

object Driver {

  def props(shardedSignal: () => ActorRef, registry: DriverCodecRegistry) = Props(new Driver(shardedSignal, registry))

  def name(driverId: String) = s"$driverId"
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
    case CreateDriverCmd(_, user, name, model, params, idMap) =>
      persist(CreateDriverEvt(user, name, model, params, idMap))(updateState)
    case RenameDriverCmd(_, user, newName) =>
      if (isValid) {
        persist(RenameDriverEvt(user, newName))(updateState)
      } else {
        sender() ! NOT_EXIST
      }
    case ChangeModelCmd(_, user, newModel) =>
      if (isValid) {
        persist(ChangeModelEvt(user, newModel))(updateState)
      } else {
        sender() ! NOT_EXIST
      }
    case AddParamsCmd(_, user, params) =>
      if (isValid) {
        persist(AddParamsEvt(user, params))(updateState)
      } else {
        sender() ! NOT_EXIST
      }
    case RemoveParamsCmd(_, user, params) =>
      if (isValid) {
        persist(RemoveParamsEvt(user, params))(updateState)
      } else {
        sender() ! NOT_EXIST
      }
    case SaveSnapshotCmd(_, user) =>
      if (isValid) {
        saveSnapshot(DriverPo(driverName.get, modelName.get, initParams.toMap, signalIdMap.toMap))
        sender() ! SUCCESS
      } else {
        log.warning("Save snapshot failed - Not a valid object")
        sender() ! NOT_EXIST
      }
    case MapSignalKeyIdCmd(_, user, key, signalId) =>
      if (isValid) {
        persist(MapSignalKeyIdEvt(user, key, signalId))(updateState)
      } else {
        sender() ! NOT_EXIST
      }

    case cmd: GetSignalValueCmd =>
      if (isValid) {
        driverCodec.get forward cmd
      } else {
        sender() ! NOT_EXIST
      }
    case cmd: GetSignalValuesCmd =>
      if (isValid) {
        driverCodec.get forward cmd
      } else {
        sender() ! NOT_EXIST
      }
    case cmd: SetSignalValueCmd =>
      if (isValid) {
        driverCodec.get forward cmd
      } else {
        sender() ! NOT_EXIST
      }
    case cmd: SetSignalValuesCmd =>
      if (isValid) {
        driverCodec.get forward cmd
      } else {
        sender() ! NOT_EXIST
      }
    case UpdateSignalValuesCmd(_, user, values) =>
      if (isValid) {
        for (v <- values) {
          shardedSignal() ! UpdateValueCmd(v.key, v.ts, v.value)
        }
      } else {
        sender() ! NOT_EXIST
      }
    case cmd: SendBytesCmd =>
      if (isValid) {
        driverCodec.get forward cmd
      } else {
        sender() ! NOT_EXIST
      }
    case RetrieveDriverCmd(_, user) =>
      if (isValid) {
        sender() ! DriverVo(driverId, driverName.get, modelName.get, initParams.toMap, signalIdMap.toMap)
      } else {
        sender() ! NOT_EXIST
      }
    case StartDriverCmd(_, user) =>
    case StopDriverCmd(_, user) =>
      stop()
    case GetSupportedModelsCmd(_, user) =>
      if (isValid) {
        sender() ! SupportedModelsVo(registry.names.toSeq)
      } else {
        sender() ! NOT_EXIST
      }
    case GetModelParamsCmd(_, user, model) =>
      if (isValid) {
        sender() ! ModelParamsVo(registry.paramNames(model).toSeq)
      } else {
        sender() ! NOT_EXIST
      }
    case _: ReceiveTimeout =>
      stop()
    case x => log.info("COMMAND: {} {}", this, x)
  }

  private def updateState: (Event => Unit) = {
    case CreateDriverEvt(user, name, model, params, idMap) =>
      this.driverName = Some(name)
      this.modelName = Some(model)
      this.initParams = this.initParams ++ params
      for ((k, v) <- idMap) this.signalIdMap.put(k, v)
      replyToSender(SUCCESS)
    case RenameDriverEvt(user, newName) =>
      this.driverName = Some(newName)
      replyToSender(SUCCESS)
    case ChangeModelEvt(user, newModel) =>
      this.modelName = Some(newModel)
      replyToSender(SUCCESS)
    case AddParamsEvt(user, params) =>
      this.initParams = this.initParams ++ params
      replyToSender(SUCCESS)
    case RemoveParamsEvt(user, params) =>
      this.initParams = this.initParams.filter(p => !params.contains(p._1))
      replyToSender(SUCCESS)
    case MapSignalKeyIdEvt(user, key, signalId) =>
      this.signalIdMap.put(key, signalId)
      replyToSender(SUCCESS)
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

  private def replyToSender(msg: Any) = {
    if ("deadLetters" != sender().path.name) sender() ! msg
  }
}