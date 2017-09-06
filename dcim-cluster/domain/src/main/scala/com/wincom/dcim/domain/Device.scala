package com.wincom.dcim.domain

import akka.actor.{ActorLogging, Props}
import akka.persistence.{PersistentActor, SnapshotOffer}
import com.wincom.dcim.domain.Device._

import scala.collection.mutable

/**
  * Created by wangxy on 17-9-5.
  */
object Device {
  def props = Props[Device]

  def name(id: String) = id

  sealed trait Command {
    def deviceId: String
  }

  sealed trait Response

  sealed trait Event

  /* value objects & responses */
  final case class DeviceVo(deviceId: String,
                            deviceName: String,
                            deviceType: String,
                            vendorModel: String,
                            propertyTagCode: String,
                            signals: Seq[String],
                            alarms: Seq[String],
                            children: Seq[String]
                           ) extends Response
  final case object Ok extends Response
  final case object NotExist extends Response
  final case object NotAvailable extends Response
  /* commands */
  final case class CreateDeviceCmd(deviceId: String,
                                   deviceName: String,
                                   deviceType: String,
                                   vendorModel: String,
                                   propertyTagCode: String,
                                   signals: Seq[String],
                                   alarms: Seq[String],
                                   children: Seq[String]
                                  ) extends Command

  final case class RenameDeviceCmd(deviceId: String, newName: String) extends Command

  final case class ChangeDeviceTypeCmd(deviceId: String, newType: String) extends Command

  final case class ChangeVendorModelCmd(deviceId: String, newType: String) extends Command

  final case class ChangePropertyTagCodeCmd(deviceId: String, newPropertyCode: String) extends Command

  final case class AddSignalCmd(deviceId: String, signalId: String) extends Command

  final case class RemoveSignalCmd(deviceId: String, signalId: String) extends Command

  final case class AddAlarmCmd(deviceId: String, alarmId: String) extends Command

  final case class RemoveAlarmCmd(deviceId: String, alarmId: String) extends Command

  final case class AddChildCmd(deviceId: String, childDeviceId: String) extends Command

  final case class RemoveChildCmd(deviceId: String, childDeviceId: String) extends Command

  /* events */
  final case class CreateDeviceEvt(deviceName: String,
                                   deviceType: String,
                                   vendorModel: String,
                                   propertyTagCode: String,
                                   signals: Seq[String],
                                   alarms: Seq[String],
                                   children: Seq[String]
                                  ) extends Event

  final case class RenameDeviceEvt(newName: String) extends Event

  final case class ChangeDeviceTypeEvt(newType: String) extends Event

  final case class ChangeVendorModelEvt(newModel: String) extends Event

  final case class ChangePropertyTagCodeEvt(newPropertyCode: String) extends Event

  final case class AddSignalEvt(signalId: String) extends Event

  final case class RemoveSignalEvt(signalId: String) extends Event

  final case class AddAlarmEvt(alarmId: String) extends Event

  final case class RemoveAlarmEvt(alarmId: String) extends Event

  final case class AddChildEvt(childDeviceId: String) extends Event

  final case class RemoveChildEvt(childDeviceId: String) extends Event

  /* persistent objects */
  final case class DevicePo(deviceName: String,
                            deviceType: String,
                            vendorModel: String,
                            propertyTagCode: String,
                            signals: Seq[String],
                            alarms: Seq[String],
                            children: Seq[String]
                           ) extends Event

}

class Device extends PersistentActor with ActorLogging {

  val deviceId: String = s"$self.path.name"
  var deviceName: Option[String] = None
  var deviceType: Option[String] = None
  var vendorModel: Option[String] = None
  var propertyTagCode: Option[String] = None

  var signals: mutable.Seq[String] = mutable.Seq() // signal ids
  var alarms: mutable.Seq[String] = mutable.Seq() // alarm ids
  var children: mutable.Seq[String] = mutable.Seq() // child module ids

  override def persistenceId: String = s"$self.path.name"

  override def receiveRecover: Receive = {
    case evt: Event =>
      updateState(evt)
    case SnapshotOffer(_, DevicePo(deviceName, deviceType, vendorModel, propertyTagCode, signals, alarms, children)) =>
      this.deviceName = Some(deviceName)
      this.deviceType = Some(deviceType)
      this.vendorModel = Some(vendorModel)
      this.propertyTagCode = Some(propertyTagCode)
      this.signals = mutable.Seq() ++ signals
      this.alarms = mutable.Seq() ++ alarms
      this.children = mutable.Seq() ++ children
    case x => log.info("RECOVER: {} {}", this, x)
  }

  override def receiveCommand: Receive = {
    case CreateDeviceCmd(_, name, deviceType, model, propertyTagCode, signals, alarms, children) =>
      persist(CreateDeviceEvt(name, deviceType, model, propertyTagCode, signals, alarms, children)) (updateState)
    case RenameDeviceCmd(_, newName) =>
      if(isValid()) {
        persist(RemoveAlarmEvt(newName))(updateState)
      } else {
        replyToSender(NotExist)
      }
    case ChangeDeviceTypeCmd(_, newType) =>
      if(isValid()) {
        persist(ChangeDeviceTypeEvt(newType))(updateState)
      } else {
        replyToSender(NotExist)
      }
    case ChangeVendorModelCmd(_, newModel) =>
      if(isValid()) {
        persist(ChangeVendorModelEvt(newModel))(updateState)
      } else {
        replyToSender(NotExist)
      }
    case ChangePropertyTagCodeCmd(_, newCode) =>
      if(isValid()) {
        persist(ChangePropertyTagCodeEvt(newCode))(updateState)
      } else {
        replyToSender(NotExist)
      }
    case AddSignalCmd(_, signalId) =>
      if(isValid()) {
        persist(AddSignalEvt(signalId))(updateState)
      } else {
        replyToSender(NotExist)
      }
    case RemoveSignalCmd(_, signalId) =>
      if(isValid()) {
        persist(RemoveSignalEvt(signalId))(updateState)
      } else {
        replyToSender(NotExist)
      }
    case AddAlarmCmd(_, alarmId) =>
      if(isValid()) {
        persist(AddAlarmEvt(alarmId))(updateState)
      } else {
        replyToSender(NotExist)
      }
    case RemoveAlarmCmd(_, alarmId) =>
      if(isValid()) {
        persist(RemoveAlarmEvt(alarmId))(updateState)
      } else {
        replyToSender(NotExist)
      }
    case AddChildCmd(_, deviceId) =>
      if(isValid()) {
        persist(AddChildEvt(deviceId))(updateState)
      } else {
        replyToSender(NotExist)
      }
    case RemoveChildCmd(_, deviceId) =>
      if(isValid()) {
        persist(RemoveChildEvt(deviceId))(updateState)
      } else {
        replyToSender(NotExist)
      }
    case x => log.info("COMMAND: {} {}", this, x)
  }

  private def updateState: (Event => Unit) = {
    case CreateDeviceEvt(deviceName, deviceType, vendorModel, propertyTagCode, signals, alarms, children) =>
      this.deviceName = Some(deviceName)
      this.deviceType = Some(deviceType)
      this.vendorModel = Some(vendorModel)
      this.propertyTagCode = Some(propertyTagCode)
      this.signals = mutable.Seq() ++ signals
      this.alarms = mutable.Seq() ++ alarms
      this.children = mutable.Seq() ++ children
      replyToSender(Ok)
    case RenameDeviceEvt(newName) =>
      this.deviceName = Some(newName)
      replyToSender(Ok)
    case ChangeDeviceTypeEvt(newType) =>
      this.deviceType = Some(newType)
      replyToSender(Ok)
    case ChangeVendorModelEvt(newModel) =>
      this.vendorModel = Some(newModel)
      replyToSender(Ok)
    case ChangePropertyTagCodeEvt(newPropertyCode) =>
      this.propertyTagCode = Some(newPropertyCode)
      replyToSender(Ok)
    case AddSignalEvt(signalId) =>
      this.signals :+= signalId
      replyToSender(Ok)
    case RemoveSignalEvt(signalId) =>
      this.signals = this.signals.filter(!_.eq(signalId))
      replyToSender(Ok)
    case AddAlarmEvt(alarmId) =>
      this.alarms :+= alarmId
      replyToSender(Ok)
    case AddChildEvt(childDeviceId) =>
      this.children :+= childDeviceId
      replyToSender(Ok)
    case RemoveChildEvt(childDeviceId) =>
      this.children = this.children.filter(!_.equals(childDeviceId))
      replyToSender(Ok)
    case x => log.info("UPDATE IGNORED: {} {}", this, x)
  }

  private def isValid(): Boolean = {
    deviceName.isDefined && deviceType.isDefined && vendorModel.isDefined
  }

  private def replyToSender(msg: Any) = {
    if ("deadLetters" != sender().path.name) sender() ! msg
  }
}
