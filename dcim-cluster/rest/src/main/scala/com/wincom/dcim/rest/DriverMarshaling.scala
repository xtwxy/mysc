package com.wincom.dcim.rest

import com.wincom.dcim.message.common._
import com.wincom.dcim.message.driver._
import com.wincom.dcim.message.signal
import spray.json._

/**
  * Created by wangxy on 17-8-15.
  */
trait DriverMarshaling extends DefaultJsonProtocol {
  implicit val anyValFormat: AnyValFormat.type = AnyValFormat
  implicit val dateTimeFormat: DateTimeJsonFormat.type = DateTimeJsonFormat

  implicit val signalTypeJsonFormat = SignalTypeJsonFormat
  implicit val responseTypeJsonFormat = ResponseTypeJsonFormat

  implicit val driverVoFormat = jsonFormat4(DriverVo.apply)
  implicit val signalValueVoFormat = jsonFormat4(signal.SignalValueVo.apply)
  implicit val signalSnapshotValueVoFormat = jsonFormat3(signal.SignalSnapshotValueVo.apply)
  implicit val driverSignalSnapshotValueVoFormat = jsonFormat4(DriverSignalSnapshotVo.apply)
  implicit val signalSnapshotValuesVoFormat = jsonFormat1(DriverSignalSnapshotsVo.apply)

  implicit val renameDriverCmdFormat = jsonFormat3(RenameDriverCmd.apply)
  implicit val createDriverCmdFormat = jsonFormat5(CreateDriverCmd.apply)
  implicit val changeModelCmdFormat = jsonFormat3(ChangeModelCmd.apply)
  implicit val saveSnapshotCmdFormat = jsonFormat2(SaveSnapshotCmd.apply)
  implicit val addParamsCmdFormat = jsonFormat3(AddParamsCmd.apply)
  implicit val removeParamsCmdFormat = jsonFormat3(RemoveParamsCmd.apply)
  implicit val mapSignalKeyIdCmdFormat = jsonFormat4(MapSignalKeyIdCmd.apply)

  implicit val getSignalValueCmdFormat = jsonFormat3(GetSignalValueCmd.apply)
  implicit val getSignalValuesCmdFormat = jsonFormat3(GetSignalValuesCmd.apply)

  implicit val setSignalValueCmdFormat = jsonFormat4(SetSignalValueCmd.apply)
  implicit val setSignalValueRspFormat = jsonFormat2(signal.SetValueRsp.apply)
  implicit val setSignalValuesCmdFormat = jsonFormat3(SetSignalValuesCmd.apply)
  implicit val setSignalValuesRspFormat = jsonFormat1(SetSignalValuesRsp.apply)

  implicit val updateSignalValuesCmdFormat = jsonFormat3(UpdateSignalValuesCmd.apply)

  implicit val byteStringJsonFormat = ByteStringJsonFormat
  implicit val sendBytesCmdFormat = jsonFormat3(SendBytesCmd.apply)

  implicit val retrieveDriverCmdFormat = jsonFormat2(RetrieveDriverCmd.apply)
  implicit val startDriverCmdFormat = jsonFormat2(StartDriverCmd.apply)
  implicit val restartDriverCmdFormat = jsonFormat2(RestartDriverCmd.apply)
  implicit val stopDriverCmdFormat = jsonFormat2(StopDriverCmd.apply)

  implicit val getSupportedModelsRspFormat = jsonFormat1(SupportedModelsVo.apply)
  implicit val paramRangeVoFormat = jsonFormat2(ParamRange.apply)
  implicit val paramTypeFormat = ParamTypeJsonFormat
  implicit val paramMetaVoFormat = jsonFormat6(ParamMeta.apply)
  implicit val getSupportedParamsRspFormat = jsonFormat1(ModelParamsVo.apply)
}

object DriverMarshaling extends DriverMarshaling
