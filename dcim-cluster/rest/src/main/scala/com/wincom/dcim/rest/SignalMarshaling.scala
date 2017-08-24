package com.wincom.dcim.rest

import com.wincom.dcim.domain.Signal._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

/**
  * Created by wangxy on 17-8-16.
  */
trait SignalMarshaling extends DefaultJsonProtocol {
  implicit val dateTimeFormat = DateTimeFormat
  implicit val anyValFormat = AnyValFormat

  implicit val transVoFormat = jsonFormat2(TransFunVo)
  implicit val signalVoFormat = jsonFormat5(SignalVo)
  implicit val signalValueFormat = jsonFormat3(SignalValueVo)

  implicit val okFormat: RootJsonFormat[Ok] = jsonFormat1(Ok)

  implicit val createSignalCmdFormat = jsonFormat4(CreateSignalCmd)
  implicit val renameSignalCmdFormat = jsonFormat2(RenameSignalCmd)
  implicit val selectDriverCmdFormat = jsonFormat2(SelectDriverCmd)
  implicit val selectKeyCmdFormat = jsonFormat2(SelectKeyCmd)
  implicit val retrieveSignalCmdFormat = jsonFormat1(RetrieveSignalCmd)
  implicit val saveSnapshotCmdFormat = jsonFormat1(SaveSnapshotCmd)

  implicit val updateValueCmdFormat = jsonFormat3(UpdateValueCmd)
  implicit val setValueCmdFormat = jsonFormat2(SetValueCmd)
  implicit val setValueRspFormat = jsonFormat2(SetValueRsp)
  implicit val getValueCmdFormat = jsonFormat1(GetValueCmd)

  implicit val startSignalCmdFormat = jsonFormat1(StartSignalCmd)
  implicit val stopSignalCmdFormat = jsonFormat1(StopSignalCmd)

  implicit val getSupportedFuncsCmdFormat: RootJsonFormat[GetSupportedFuncsCmd] = jsonFormat1(GetSupportedFuncsCmd)
  implicit val getSupportedFuncsRspFormat: RootJsonFormat[GetSupportedFuncsRsp] = jsonFormat2(GetSupportedFuncsRsp)
  implicit val getSupportedParamsCmdFormat: RootJsonFormat[GetFuncParamsCmd] = jsonFormat2(GetFuncParamsCmd)
  implicit val getSupportedParamsRspFormat: RootJsonFormat[GetFuncParamsRsp] = jsonFormat2(GetFuncParamsRsp)
}

object SignalMarshaling extends SignalMarshaling
