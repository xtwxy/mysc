package com.wincom.dcim.domain

import akka.actor.{ActorLogging, Props}
import akka.persistence.{PersistentActor, SnapshotOffer}
import com.wincom.dcim.message.common._
import com.wincom.dcim.message.device._
import com.wincom.dcim.message.common.ResponseType._

import scala.collection.mutable

/**
  * Created by wangxy on 17-9-5.
  */
object Device {
  def props = Props[Device]

  def name(id: String) = id
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
      this.vendorModel = vendorModel
      this.propertyTagCode = propertyTagCode
      this.signals = mutable.Seq() ++ signals
      this.alarms = mutable.Seq() ++ alarms
      this.children = mutable.Seq() ++ children
    case x => log.info("RECOVER: {} {}", this, x)
  }

  override def receiveCommand: Receive = {
    case CreateDeviceCmd(_, user, name, deviceType, model, propertyTagCode, signals, alarms, children) =>
      if(isValid) {
        sender() ! Response(ALREADY_EXISTS, None)
      } else {
        persist(CreateDeviceEvt(user, name, deviceType, model, propertyTagCode, signals, alarms, children))(updateState)
      }
    case RenameDeviceCmd(_, user, newName) =>
      if(isValid()) {
        persist(RemoveAlarmEvt(user, newName))(updateState)
      } else {
        replyToSender(Response(NOT_EXIST, None))
      }
    case ChangeDeviceTypeCmd(_, user, newType) =>
      if(isValid()) {
        persist(ChangeDeviceTypeEvt(user, newType))(updateState)
      } else {
        replyToSender(Response(NOT_EXIST, None))
      }
    case ChangeVendorModelCmd(_, user, newModel) =>
      if(isValid()) {
        persist(ChangeVendorModelEvt(user, newModel))(updateState)
      } else {
        replyToSender(Response(NOT_EXIST, None))
      }
    case ChangePropertyTagCodeCmd(_, user, newCode) =>
      if(isValid()) {
        persist(ChangePropertyTagCodeEvt(user, newCode))(updateState)
      } else {
        replyToSender(Response(NOT_EXIST, None))
      }
    case AddSignalCmd(_, user, signalId) =>
      if(isValid()) {
        persist(AddSignalEvt(user, signalId))(updateState)
      } else {
        replyToSender(Response(NOT_EXIST, None))
      }
    case RemoveSignalCmd(_, user, signalId) =>
      if(isValid()) {
        persist(RemoveSignalEvt(user, signalId))(updateState)
      } else {
        replyToSender(Response(NOT_EXIST, None))
      }
    case AddAlarmCmd(_, user, alarmId) =>
      if(isValid()) {
        persist(AddAlarmEvt(user, alarmId))(updateState)
      } else {
        replyToSender(Response(NOT_EXIST, None))
      }
    case RemoveAlarmCmd(_, user, alarmId) =>
      if(isValid()) {
        persist(RemoveAlarmEvt(user, alarmId))(updateState)
      } else {
        replyToSender(Response(NOT_EXIST, None))
      }
    case AddChildCmd(_, user, deviceId) =>
      if(isValid()) {
        persist(AddChildEvt(user, deviceId))(updateState)
      } else {
        replyToSender(Response(NOT_EXIST, None))
      }
    case RemoveChildCmd(_, user, deviceId) =>
      if(isValid()) {
        persist(RemoveChildEvt(user, deviceId))(updateState)
      } else {
        replyToSender(Response(NOT_EXIST, None))
      }
    case x => log.info("COMMAND: {} {}", this, x)
  }

  private def updateState: (Event => Unit) = {
    case CreateDeviceEvt(user, deviceName, deviceType, vendorModel, propertyTagCode, signals, alarms, children) =>
      this.deviceName = Some(deviceName)
      this.deviceType = Some(deviceType)
      this.vendorModel = vendorModel
      this.propertyTagCode = propertyTagCode
      this.signals = mutable.Seq() ++ signals
      this.alarms = mutable.Seq() ++ alarms
      this.children = mutable.Seq() ++ children
      replyToSender(Response(SUCCESS, None))
    case RenameDeviceEvt(user, newName) =>
      this.deviceName = Some(newName)
      replyToSender(Response(SUCCESS, None))
    case ChangeDeviceTypeEvt(user, newType) =>
      this.deviceType = Some(newType)
      replyToSender(Response(SUCCESS, None))
    case ChangeVendorModelEvt(user, newModel) =>
      this.vendorModel = Some(newModel)
      replyToSender(Response(SUCCESS, None))
    case ChangePropertyTagCodeEvt(user, newPropertyCode) =>
      this.propertyTagCode = Some(newPropertyCode)
      replyToSender(Response(SUCCESS, None))
    case AddSignalEvt(user, signalId) =>
      this.signals :+= signalId
      replyToSender(Response(SUCCESS, None))
    case RemoveSignalEvt(user, signalId) =>
      this.signals = this.signals.filter(!_.eq(signalId))
      replyToSender(Response(SUCCESS, None))
    case AddAlarmEvt(user, alarmId) =>
      this.alarms :+= alarmId
      replyToSender(Response(SUCCESS, None))
    case AddChildEvt(user, childDeviceId) =>
      this.children :+= childDeviceId
      replyToSender(Response(SUCCESS, None))
    case RemoveChildEvt(user, childDeviceId) =>
      this.children = this.children.filter(!_.equals(childDeviceId))
      replyToSender(Response(SUCCESS, None))
    case x => log.info("UPDATE IGNORED: {} {}", this, x)
  }

  private def isValid(): Boolean = {
    deviceName.isDefined && deviceType.isDefined && vendorModel.isDefined
  }

  private def replyToSender(msg: Any) = {
    if ("deadLetters" != sender().path.name) sender() ! msg
  }
}
