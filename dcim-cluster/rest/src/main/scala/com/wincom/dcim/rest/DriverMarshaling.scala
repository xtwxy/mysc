package com.wincom.dcim.rest

import com.wincom.dcim.domain.Driver._
import com.wincom.dcim.domain.Signal.SignalValue
import spray.json.DefaultJsonProtocol

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

  implicit val dateTimeFormat = DateTimeFormat
  implicit val signalValueFormat = jsonFormat3(SignalValue)
  implicit val updateSignalValuesCmdFormat = jsonFormat2(UpdateSignalValuesCmd)

  implicit val sendBytesCmdFormat = jsonFormat2(SendBytesCmd)

  implicit val startDriverCmdFormat = jsonFormat1(StartDriverCmd)
  implicit val stopDriverCmdFormat = jsonFormat1(StopDriverCmd)
}

object DriverMarshaling extends DriverMarshaling
