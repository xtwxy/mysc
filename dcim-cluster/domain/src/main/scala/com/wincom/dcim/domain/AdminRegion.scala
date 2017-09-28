package com.wincom.dcim.domain

import akka.actor._
import akka.persistence.{PersistentActor, SnapshotOffer}
import com.wincom.dcim.message.adminregion._
import com.wincom.dcim.message.common.ResponseType._
import com.wincom.dcim.message.common._

import scala.collection.mutable

/**
  * Created by wangxy on 17-9-5.
  */
object AdminRegion {
  def props()  = Props(new AdminRegion)

  def name(id: String) = id
}

class AdminRegion extends PersistentActor with ActorLogging {

  val regionId: String = s"${self.path.name.split("_")(1)}"
  var regionType: Option[AdminRegionType] = None
  var regionName: Option[String] = None
  var abbreviation: Option[String] = None
  var regionCode: Option[String] = None
  var longitude: Option[Double] = None
  var latitude: Option[Double] = None

  var childrenIds: mutable.Seq[String] = mutable.Seq() // child region ids

  override def persistenceId: String = s"$self.path.name"

  override def receiveRecover: Receive = {
    case evt: Event =>
      updateState(evt)
    case SnapshotOffer(_, AdminRegionPo(regionType, name, abbrev, code, longitude, latitude, childrenIds)) =>
      this.regionType = Some(regionType)
      this.regionName = Some(name)
      this.abbreviation = abbrev
      this.regionCode = code
      this.longitude = longitude
      this.latitude = latitude
      this.childrenIds ++= childrenIds
    case x => log.info("RECOVER: {} {}", this, x)
  }

  override def receiveCommand: Receive = {
    case CreateAdminRegionCmd(_, user, regionType, name, abbrev, code, longitude, latitude, children) =>
      if(isValid) {
        sender() ! Response(ALREADY_EXISTS, None)
      } else {
        persist(CreateAdminRegionEvt(user, regionType, name, abbrev, code, longitude, latitude, children))(updateState)
      }
    case RenameRegionCmd(_, user, newName) =>
      if(isValid()) {
        persist(RenameRegionEvt(user, newName))(updateState)
      } else {
        replyToSender(Response(NOT_EXIST, None))
      }
    case ChangeRegionTypeCmd(_, user, newType) =>
      if(isValid()) {
        persist(ChangeRegionTypeEvt(user, newType))(updateState)
      } else {
        replyToSender(Response(NOT_EXIST, None))
      }
    case ChangeAbbrevCmd(_, user, newAbbrev) =>
      if(isValid()) {
        persist(ChangeAbbrevEvt(user, newAbbrev))(updateState)
      } else {
        replyToSender(Response(NOT_EXIST, None))
      }
    case ChangeCodeCmd(_, user, newCode) =>
      if(isValid()) {
        persist(ChangeCodeEvt(user, newCode))(updateState)
      } else {
        replyToSender(Response(NOT_EXIST, None))
      }
    case ChangeLongitudeCmd(_, user, newLongitude) =>
      if(isValid()) {
        persist(ChangeLongitudeEvt(user, newLongitude))(updateState)
      } else {
        replyToSender(Response(NOT_EXIST, None))
      }
    case ChangeLatitudeCmd(_, user, newLatitude) =>
      if(isValid()) {
        persist(ChangeLatitudeEvt(user, newLatitude))(updateState)
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
    case x => log.info("COMMAND *IGNORED*: {} {}", this, x)
  }

  private def updateState: (Event => Unit) = {
    case CreateAdminRegionEvt(user, regionType, name, abbrev, code, longitude, latitude, children) =>
      this.regionType = Some(regionType)
      this.regionName = Some(name)
      this.abbreviation = abbrev
      this.regionCode = code
      this.longitude = longitude
      this.latitude = latitude
      this.childrenIds ++= childrenIds
      replyToSender(Response(SUCCESS, None))
    case RenameRegionEvt(user, newName) =>
      this.regionName = Some(newName)
      replyToSender(Response(SUCCESS, None))
    case ChangeRegionTypeEvt(user, newType) =>
      this.regionType = Some(newType)
      replyToSender(Response(SUCCESS, None))
    case ChangeAbbrevEvt(user, newAbbrev) =>
      this.abbreviation = Some(newAbbrev)
      replyToSender(Response(SUCCESS, None))
    case ChangeCodeEvt(user, newCode) =>
      this.regionCode = Some(newCode)
      replyToSender(Response(SUCCESS, None))
    case ChangeLongitudeEvt(user, longitude) =>
      this.longitude = Some(longitude)
      replyToSender(Response(SUCCESS, None))
    case ChangeLatitudeEvt(user, latitude) =>
      this.latitude = Some(latitude)
      replyToSender(Response(SUCCESS, None))
    case AddChildEvt(user, childId) =>
      this.childrenIds :+= childId
      replyToSender(Response(SUCCESS, None))
    case RemoveChildEvt(user, childId) =>
      this.childrenIds = this.childrenIds.filter(!_.equals(childId))
      replyToSender(Response(SUCCESS, None))
    case x => log.info("UPDATE IGNORED: {} {}", this, x)
  }

  private def isValid(): Boolean = {
    regionName.isDefined && regionCode.isDefined
  }

  private def replyToSender(msg: Any) = {
    if ("deadLetters" != sender().path.name) sender() ! msg
  }
}
