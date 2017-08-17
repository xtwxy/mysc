package com.wincom.dcim.rest

import com.wincom.dcim.domain.Driver._
import com.wincom.dcim.domain.Signal.SignalValue
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

/**
  * Created by wangxy on 17-8-15.
  */
trait DriverMarshaling extends DefaultJsonProtocol {
  implicit val driverVoFormat: RootJsonFormat[DriverVo] = jsonFormat5(DriverVo)

  implicit val renameDriverCmdFormat: RootJsonFormat[RenameDriverCmd] = jsonFormat2(RenameDriverCmd)
  implicit val changeModelCmdFormat: RootJsonFormat[ChangeModelCmd] = jsonFormat2(ChangeModelCmd)
  implicit val saveSnapshotCmdFormat: RootJsonFormat[SaveSnapshotCmd] = jsonFormat1(SaveSnapshotCmd)
  implicit val mapSignalKeyIdCmdFormat: RootJsonFormat[MapSignalKeyIdCmd] = jsonFormat3(MapSignalKeyIdCmd)

  implicit val getSignalValueCmdFormat: RootJsonFormat[GetSignalValueCmd] = jsonFormat2(GetSignalValueCmd)
  implicit val getSignalValuesCmdFormat: RootJsonFormat[GetSignalValuesCmd] = jsonFormat2(GetSignalValuesCmd)

  implicit val anyValFormat: AnyValFormat.type = AnyValFormat
  implicit val setSignalValueCmdFormat: RootJsonFormat[SetSignalValueCmd] = jsonFormat3(SetSignalValueCmd)
  implicit val setSignalValuesCmdFormat: RootJsonFormat[SetSignalValuesCmd] = jsonFormat2(SetSignalValuesCmd)

  implicit val dateTimeFormat: DateTimeFormat.type = DateTimeFormat
  implicit val signalValueFormat: RootJsonFormat[SignalValue] = jsonFormat3(SignalValue)
  implicit val updateSignalValuesCmdFormat: RootJsonFormat[UpdateSignalValuesCmd] = jsonFormat2(UpdateSignalValuesCmd)

  implicit val sendBytesCmdFormat: RootJsonFormat[SendBytesCmd] = jsonFormat2(SendBytesCmd)

  implicit val startDriverCmdFormat: RootJsonFormat[StartDriverCmd] = jsonFormat1(StartDriverCmd)
  implicit val stopDriverCmdFormat: RootJsonFormat[StopDriverCmd] = jsonFormat1(StopDriverCmd)
}

object DriverMarshaling extends DriverMarshaling
