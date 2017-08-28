package com.wincom.dcim.rest

import com.wincom.dcim.domain.Driver._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

/**
  * Created by wangxy on 17-8-15.
  */
trait DriverMarshaling extends DefaultJsonProtocol {
  implicit val anyValFormat: AnyValFormat.type = AnyValFormat
  implicit val dateTimeFormat: DateTimeJsonFormat.type = DateTimeJsonFormat

  implicit val okFormat: RootJsonFormat[Ok] = jsonFormat1(Ok)
  implicit val driverVoFormat: RootJsonFormat[DriverVo] = jsonFormat5(DriverVo)
  implicit val signalValueFormat: RootJsonFormat[SignalValue] = jsonFormat3(SignalValue)
  implicit val signalValueVoFormat: RootJsonFormat[SignalValueVo] = jsonFormat4(SignalValueVo)
  implicit val signalValuesVoFormat: RootJsonFormat[SignalValuesVo] = jsonFormat2(SignalValuesVo)

  implicit val renameDriverCmdFormat: RootJsonFormat[RenameDriverCmd] = jsonFormat2(RenameDriverCmd)
  implicit val createDriverCmdFormat: RootJsonFormat[CreateDriverCmd] = jsonFormat5(CreateDriverCmd)
  implicit val changeModelCmdFormat: RootJsonFormat[ChangeModelCmd] = jsonFormat2(ChangeModelCmd)
  implicit val saveSnapshotCmdFormat: RootJsonFormat[SaveSnapshotCmd] = jsonFormat1(SaveSnapshotCmd)
  implicit val addParamsCmdFormat: RootJsonFormat[AddParamsCmd] = jsonFormat2(AddParamsCmd)
  implicit val removeParamsCmdFormat: RootJsonFormat[RemoveParamsCmd] = jsonFormat2(RemoveParamsCmd)
  implicit val mapSignalKeyIdCmdFormat: RootJsonFormat[MapSignalKeyIdCmd] = jsonFormat3(MapSignalKeyIdCmd)

  implicit val getSignalValueCmdFormat: RootJsonFormat[GetSignalValueCmd] = jsonFormat2(GetSignalValueCmd)
  implicit val getSignalValuesCmdFormat: RootJsonFormat[GetSignalValuesCmd] = jsonFormat2(GetSignalValuesCmd)

  implicit val setSignalValueCmdFormat: RootJsonFormat[SetSignalValueCmd] = jsonFormat3(SetSignalValueCmd)
  implicit val setSignalValueRspFormat: RootJsonFormat[SetSignalValueRsp] = jsonFormat3(SetSignalValueRsp)
  implicit val setSignalValuesCmdFormat: RootJsonFormat[SetSignalValuesCmd] = jsonFormat2(SetSignalValuesCmd)
  implicit val setSignalValuesRspFormat: RootJsonFormat[SetSignalValuesRsp] = jsonFormat2(SetSignalValuesRsp)

  implicit val updateSignalValuesCmdFormat: RootJsonFormat[UpdateSignalValuesCmd] = jsonFormat2(UpdateSignalValuesCmd)

  implicit val sendBytesCmdFormat: RootJsonFormat[SendBytesCmd] = jsonFormat2(SendBytesCmd)

  implicit val retrieveDriverCmdFormat: RootJsonFormat[RetrieveDriverCmd] = jsonFormat1(RetrieveDriverCmd)
  implicit val startDriverCmdFormat: RootJsonFormat[StartDriverCmd] = jsonFormat1(StartDriverCmd)
  implicit val stopDriverCmdFormat: RootJsonFormat[StopDriverCmd] = jsonFormat1(StopDriverCmd)

  implicit val getSupportedModelsCmdFormat: RootJsonFormat[GetSupportedModelsCmd] = jsonFormat1(GetSupportedModelsCmd)
  implicit val getSupportedModelsRspFormat: RootJsonFormat[GetSupportedModelsRsp] = jsonFormat2(GetSupportedModelsRsp)
  implicit val getSupportedParamsCmdFormat: RootJsonFormat[GetModelParamsCmd] = jsonFormat2(GetModelParamsCmd)
  implicit val getSupportedParamsRspFormat: RootJsonFormat[GetModelParamsRsp] = jsonFormat2(GetModelParamsRsp)
}

object DriverMarshaling extends DriverMarshaling
