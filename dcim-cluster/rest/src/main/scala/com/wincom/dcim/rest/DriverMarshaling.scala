package com.wincom.dcim.rest

import com.wincom.dcim.domain.Driver._
import com.wincom.dcim.domain.Signal.SignalValue
import spray.json.{DefaultJsonProtocol, JsObject, JsString, JsValue, RootJsonFormat}

/**
  * Created by wangxy on 17-8-15.
  */
trait DriverMarshaling extends DefaultJsonProtocol {
  implicit val driverVoFormat = jsonFormat5(DriverVo)
  implicit val renameDriverCmdFormat = jsonFormat2(RenameDriverCmd)
  implicit val changeModelCmdFormat = jsonFormat2(ChangeModelCmd)
  implicit val saveSnapshotCmdFormat = jsonFormat1(SaveSnapshotCmd)
  implicit val mapSignalKeyIdCmdFormat = jsonFormat3(MapSignalKeyIdCmd)
  implicit val getSignalValueCmdFormat = jsonFormat2(GetSignalValueCmd)
  implicit val getSignalValuesCmdFormat = jsonFormat2(GetSignalValuesCmd)
  implicit val anyValFormat = AnyValFormat
  implicit val setSignalValueCmdFormat = jsonFormat3(SetSignalValueCmd)
  implicit val setSignalValuesCmdFormat = jsonFormat2(SetSignalValuesCmd)
  implicit val signalValueFormat = jsonFormat3(SignalValue)
  implicit val updateSignalValuesCmdFormat = jsonFormat2(UpdateSignalValuesCmd)
  implicit val sendBytesCmdFormat = jsonFormat2(SendBytesCmd)
  implicit val startDriverCmdFormat = jsonFormat1(StartDriverCmd)
  implicit val stopDriverCmdFormat = jsonFormat1(StopDriverCmd)
}

object DriverMarshaling extends DriverMarshaling
object AnyValFormat extends RootJsonFormat[AnyVal] {
  override def write(obj: AnyVal) = {
    obj match {
      case x: Boolean =>
        JsObject(("type", JsString("Bool")), ("value", JsString(x.toString)))
      case x: Double =>
        JsObject(("type", JsString("Double")), ("value", JsString(x.toString)))
      case x =>
        JsObject(("type", JsString("Unknow")), ("value", JsString(x.toString)))
    }
  }

  override def read(json: JsValue): AnyVal = {
    json match {
      case JsObject(fields) =>
        fields.get("type").get match {
          case JsString("Bool") =>
            fields.get("value").get match {
              case JsString(x) => x.toBoolean
              case _ => 0
            }
          case JsString("Double") =>
            fields.get("value").get match {
              case JsString(x) => x.toDouble
              case _ => 0
            }
          case _ => 0
        }
      case _ => 0
    }
  }
}
