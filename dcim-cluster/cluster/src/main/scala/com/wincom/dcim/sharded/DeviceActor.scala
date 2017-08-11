package com.wincom.dcim.sharded

import akka.actor.{ActorRef, Props}
import akka.cluster.sharding.ShardRegion
import akka.event.Logging
import akka.persistence.{PersistentActor, SnapshotOffer}
import com.wincom.dcim.rest.Settings
import com.wincom.dcim.sharded.DeviceActor._

object DeviceActor {
  val numberOfShards = 100
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case cmd: Command => (cmd.deviceId.toString, cmd)
  }
  val extractShardId: ShardRegion.ExtractShardId = {
    case cmd: Command => (cmd.deviceId % numberOfShards).toString()
  }

  def props(deviceId: Int, signalShard: ActorRef) = Props(new DeviceActor(deviceId, signalShard))

  def name(deviceId: Int) = deviceId.toString()

  trait Command {
    def deviceId: Int
  }

  trait Event {
    def deviceId: Int
  }

  /* domain object */
  final case class Device(deviceId: Int, deviceType: Int, name: String, signals: Set[Int])

  /* commands */
  final case class CreateDeviceCmd(deviceId: Int, deviceType: Int, name: String, signals: Set[Int]) extends Command

  final case class GetDeviceCmd(deviceId: Int) extends Command

  final case class GetSignalValuesCmd(deviceId: Int, signals: Set[Int]) extends Command

  final case class RenameDeviceCmd(deviceId: Int, newName: String) extends Command

  final case class AddSignalCmd(deviceId: Int, signalId: Int) extends Command

  final case class AddSignalsCmd(deviceId: Int, signals: Set[Int]) extends Command

  final case class RemoveSignalCmd(deviceId: Int, signalId: Int) extends Command

  final case class RemoveSignalsCmd(deviceId: Int, signals: Set[Int]) extends Command

  final case class SaveSnapshotCmd(deviceId: Int) extends Command

  /* events */
  final case class CreateDeviceEvt(deviceId: Int, deviceType: Int, name: String, signals: Set[Int]) extends Event

  final case class RenameDeviceEvt(deviceId: Int, newName: String) extends Event

  final case class AddSignalEvt(deviceId: Int, signalId: Int) extends Event

  final case class AddSignalsEvt(deviceId: Int, signals: Set[Int]) extends Event

  final case class RemoveSignalEvt(deviceId: Int, signalId: Int) extends Event

  final case class RemoveSignalsEvt(deviceId: Int, signals: Set[Int]) extends Event

  final case object NoSuchDevice

}

class DeviceActor(val deviceId: Int, val signalShard: ActorRef) extends PersistentActor {
  val log = Logging(context.system.eventStream, "sharded-devices")
  var deviceType: Option[Int] = None
  var deviceName: Option[String] = None
  var signals: Set[Int] = Set()

  override def persistenceId: String = s"${self.path.name}"

  context.setReceiveTimeout(Settings(context.system).passivateTimeout)

  def receiveRecover = {
    case evt: Event =>
      updateState(evt)
    case SnapshotOffer(_, Device(deviceId, deviceType, deviceName, signals)) =>
      this.deviceType = Some(deviceType)
      this.deviceName = Some(deviceName)
      this.signals = signals
    case x => log.info("RECOVER: {} {}", this, x)
  }

  def receiveCommand = {
    case CreateDeviceCmd(deviceId, deviceType, deviceName, signalIds) =>
      persist(CreateDeviceEvt(deviceId, deviceType, deviceName, signalIds))(updateState)
    case GetDeviceCmd =>
      if (valid) {
        sender() ! Device(deviceId, deviceType.get, deviceName.get, signals)
      } else {
        sender() ! NoSuchDevice
      }
    case GetSignalValuesCmd(_, signals) =>
      signals.foreach(s => signalShard forward s)
    case RenameDeviceCmd(deviceId, newName) =>
      persist(RenameDeviceEvt(deviceId, newName))(updateState)
    case AddSignalCmd(deviceId, signalId) =>
      persist(AddSignalEvt(deviceId, signalId))(updateState)
    case AddSignalsCmd(deviceId, signals) =>
      persist(AddSignalsEvt(deviceId, signals))(updateState)
    case RemoveSignalCmd(deviceId, signalId) =>
      persist(RemoveSignalEvt(deviceId, signalId))(updateState)
    case RemoveSignalsCmd(deviceId, signals) =>
      persist(RemoveSignalsEvt(deviceId, signals))(updateState)
    case SaveSnapshotCmd =>
      if (valid) {
        saveSnapshot(Device(deviceId, deviceType.get, deviceName.get, signals))
      }
    case x => log.info("COMMAND: {} {}", this, x)
  }

  private def updateState: (Event => Unit) = {
    case CreateDeviceEvt(_, deviceType, deviceName, signals) =>
      this.deviceType = Some(deviceType)
      this.deviceName = Some(deviceName)
      this.signals = signals
    case RenameDeviceEvt(_, newName) =>
      this.deviceName = Some(newName)
    case AddSignalEvt(_, signalId) =>
      this.signals += signalId
    case AddSignalsEvt(_, signals) =>
      this.signals &= signals
    case RemoveSignalEvt(_, signalId) =>
      this.signals -= signalId
    case RemoveSignalsEvt(_, signals) =>
      this.signals &~= signals
  }

  private def valid: Boolean = (deviceType.isDefined && deviceName.isDefined)
}
