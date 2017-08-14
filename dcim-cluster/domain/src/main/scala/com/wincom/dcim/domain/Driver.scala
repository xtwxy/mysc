package com.wincom.dcim.domain

import java.util
import java.util.Map

import akka.actor.{ActorRef, Props}
import akka.event.Logging
import akka.persistence.{PersistentActor, SnapshotOffer}
import akka.util.Timeout
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap.create
import com.wincom.dcim.domain.Driver._
import com.wincom.dcim.driver.{DriverCodec, DriverCodecRegistry}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, SECONDS}

/**
  * Created by wangxy on 17-8-14.
  */

object Driver {

  def props(driverId: Int, fsuShard: ActorRef, registry: DriverCodecRegistry) = Props(new Driver(driverId, fsuShard, registry))

  def name(driverId: Int) = s"driver_$driverId"

  sealed trait Command {
    def driverId: Int
  }

  sealed trait Event

  /* domain objects */
  final case class DriverVo(driverId: Int,
                            name: String,
                            model: String,
                            initParams: Map[String, String],
                            signalIdMap: Map[String, Int]) extends Command

  final case class Ok(driverId: Int) extends Command

  final case class NotAvailable(driverId: Int) extends Command

  final case class NotExist(driverId: Int) extends Command

  final case class AlreadyExists(driverId: Int) extends Command

  /* commands */
  final case class CreateDriverCmd(driverId: Int, name: String, model: String, initParams: Map[String, String], signalIdMap: Map[String, Int]) extends Command

  final case class RenameDriverCmd(driverId: Int, newName: String) extends Command

  final case class ChangeModelCmd(driverId: Int, newModel: String) extends Command

  final case class SaveSnapshotCmd(driverId: Int) extends Command

  final case class MapSignalKeyIdCmd(driverId: Int, key: String, signalId: Int) extends Command

  /* transient commands */
  final case class GetSignalValueCmd(driverId: Int, key: String) extends Command

  final case class GetSignalValuesCmd(driverId: Int, keys: Set[String]) extends Command

  final case class SetSignalValueCmd(driverId: Int, key: String, value: AnyVal) extends Command

  final case class SetSignalValuesCmd(driverId: Int, values: Map[String, AnyVal]) extends Command

  /* events */
  final case class CreateDriverEvt(name: String, model: String, initParams: Map[String, String], signalIdMap: Map[String, Int]) extends Event

  final case class RenameDriverEvt(newName: String) extends Event

  final case class ChangeModelEvt(newModel: String) extends Event

  final case class MapSignalKeyIdEvt(key: String, signalId: Int) extends Event

  /* persistent objects */
  final case class DriverPo(name: String, model: String, initParams: Map[String, String], signalIdMap: Map[String, Int]) extends Event

}

class Driver(val driverId: Int, val fsuShard: ActorRef, val registry: DriverCodecRegistry) extends PersistentActor {

  val log = Logging(context.system.eventStream, "sharded-drivers")

  var driverName: Option[String] = None
  var modelName: Option[String] = None
  val initParams: Map[String, String] = new util.HashMap()
  var driver: Option[DriverCodec] = None
  val signalIdMap: BiMap[String, Int] = create()

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
    case x => log.info("COMMAND: {} {}", this, x)
  }

  private def updateState: (Event => Unit) = {
    case CreateDriverEvt(name, model, params, idMap) =>
      this.driverName = Some(name)
      this.modelName = Some(model)
      this.initParams.putAll(params)
      this.signalIdMap.putAll(idMap)
      this.driver = registry.create(this.modelName.get, this.initParams)
    case RenameDriverEvt(newName) =>
      this.driverName = Some(newName)
    case ChangeModelEvt(newModel) =>
      this.modelName = Some(newModel)
      if(this.driver.isDefined) {
        this.driver.get.stop();
      }
      this.driver = registry.create(this.modelName.get, this.initParams)
    case MapSignalKeyIdEvt(key, signalId) =>
      this.signalIdMap.put(key, signalId)
    case x => log.info("UPDATE IGNORED: {} {}", this, x)
  }

}