package com.wincom.dcim.domain

import java.util
import java.util.Map

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.SECONDS
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap.create
import com.wincom.dcim.domain.Driver._
import com.wincom.dcim.driver.DriverCodecRegistry
import akka.actor.ActorRef
import akka.actor.Props
import akka.event.Logging
import akka.persistence.PersistentActor
import akka.persistence.SnapshotOffer
import akka.util.Timeout
import akka.util.Timeout.durationToTimeout


/**
  * Created by wangxy on 17-8-14.
  */

object Driver {

  def props(driverId: String, fsuShard: ActorRef, registry: DriverCodecRegistry) = Props(new Driver(driverId, fsuShard, registry))

  def name(driverId: String) = s"driver_$driverId"

  sealed trait Command {
    def driverId: String
  }

  sealed trait Event

  /* value objects */
  final case class DriverVo(driverId: String,
                            name: String,
                            model: String,
                            initParams: Map[String, String],
                            signalIdMap: Map[String, String]) extends Serializable

  /* commands */
  final case class CreateDriverCmd(driverId: String, name: String, model: String, initParams: Map[String, String], signalIdMap: Map[String, String]) extends Command

  final case class RenameDriverCmd(driverId: String, newName: String) extends Command

  final case class ChangeModelCmd(driverId: String, newModel: String) extends Command

  final case class SaveSnapshotCmd(driverId: String) extends Command

  final case class MapSignalKeyIdCmd(driverId: String, key: String, signalId: String) extends Command

  /* transient commands */
  final case class GetSignalValueCmd(driverId: String, key: String) extends Command

  final case class GetSignalValuesCmd(driverId: String, keys: Set[String]) extends Command

  final case class SetSignalValueCmd(driverId: String, key: String, value: AnyVal) extends Command

  final case class SetSignalValuesCmd(driverId: String, values: Map[String, AnyVal]) extends Command

  final case class SendBytesCmd(driverId: String, bytes: Array[Byte]) extends Command

  final case class StartDriverCmd(driverId: String) extends Command
  final case class StopDriverCmd(driverId: String) extends Command

  /* events */
  final case class CreateDriverEvt(name: String, model: String, initParams: Map[String, String], signalIdMap: Map[String, String]) extends Event

  final case class RenameDriverEvt(newName: String) extends Event

  final case class ChangeModelEvt(newModel: String) extends Event

  final case class MapSignalKeyIdEvt(key: String, signalId: String) extends Event

  /* persistent objects */
  final case class DriverPo(name: String, model: String, initParams: Map[String, String], signalIdMap: Map[String, String]) extends Serializable

}

class Driver(val driverId: String, val fsuShard: ActorRef, val registry: DriverCodecRegistry) extends PersistentActor {

  val log = Logging(context.system.eventStream, "sharded-drivers")

  var driverName: Option[String] = None
  var modelName: Option[String] = None
  val initParams: Map[String, String] = new util.HashMap()
  var driverCodec: Option[ActorRef] = None
  val signalIdMap: BiMap[String, String] = create()

  override def persistenceId: String = s"driver_${self.path.name}"

  implicit def requestTimeout: Timeout = FiniteDuration(20, SECONDS)

  implicit def executionContext: ExecutionContext = context.dispatcher

  def receiveRecover = {
    case evt: Event =>
      updateState(evt)
    case SnapshotOffer(_, DriverPo(name, model, params, idMap)) =>
      this.driverName = Some(name)
      this.modelName = Some(model)
      this.initParams.putAll(params)
      this.signalIdMap.putAll(idMap)
    case x => log.info("RECOVER: {} {}", this, x)
  }

  def receiveCommand = {
    case CreateDriverCmd(_, name, model, params, idMap) =>
      persist(CreateDriverEvt(name, model, params, idMap))(updateState)
    case RenameDriverCmd(_, newName) =>
      persist(RenameDriverEvt(newName))(updateState)
    case ChangeModelCmd(_, newModel) =>
      persist(ChangeModelEvt(newModel))(updateState)
    case MapSignalKeyIdCmd(_, key, signalId) =>
      persist(MapSignalKeyIdEvt(key, signalId))(updateState)
    case SaveSnapshotCmd =>
      if(isValid) {
        saveSnapshot(DriverPo(driverName.get, modelName.get, initParams, signalIdMap))
      } else {
        log.warning("Save snapshot failed - Not a valid object")
      }
    case cmd: GetSignalValueCmd =>
      driverCodec.get forward cmd
    case cmd: GetSignalValuesCmd =>
      driverCodec.get forward cmd
    case cmd: SetSignalValueCmd =>
      driverCodec.get forward cmd
    case cmd: SetSignalValuesCmd =>
      driverCodec.get forward cmd
    case x => log.info("COMMAND: {} {}", this, x)
  }

  private def updateState: (Event => Unit) = {
    case CreateDriverEvt(name, model, params, idMap) =>
      this.driverName = Some(name)
      this.modelName = Some(model)
      this.initParams.putAll(params)
      this.signalIdMap.putAll(idMap)
      if(!createCodec()) {
        context.stop(self)
      }
    case RenameDriverEvt(newName) =>
      this.driverName = Some(newName)
    case ChangeModelEvt(newModel) =>
      this.modelName = Some(newModel)
      if (this.driverCodec.isDefined) {
        driverCodec.get ! StopDriverCmd(driverId);
      }
      if(!createCodec()) {
        context.stop(self)
      }
    case MapSignalKeyIdEvt(key, signalId) =>
      this.signalIdMap.put(key, signalId)
    case x => log.info("UPDATE IGNORED: {} {}", this, x)
  }

  private def isValid: Boolean = {
    if (driverName.isDefined && modelName.isDefined) true else false
  }
  private def createCodec(): Boolean = {
    val p = registry.create(this.modelName.get, this.initParams)
    if(p.isDefined) {
      this.driverCodec = Some(context.system.actorOf(p.get, s"${this.modelName.get}_${driverId}"))
      return true
    } else {
      return false
    }
  }
}