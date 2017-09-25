package com.wincom.dcim.rest

import com.wincom.dcim.message.common.{ParamMeta, ParamRange}
import com.wincom.dcim.message.signal._
import spray.json._

/**
  * Created by wangxy on 17-8-16.
  */
trait SignalMarshaling extends DefaultJsonProtocol {
  implicit val dateTimeFormat = DateTimeJsonFormat
  implicit val anyValFormat = AnyValFormat

  implicit val signalTypeJsonFormat = SignalTypeJsonFormat
  implicit val responseTypeJsonFormat = ResponseTypeJsonFormat
  implicit val transVoFormat = jsonFormat2(TransFuncVo.apply)
  implicit val signalVoFormat = jsonFormat6(SignalVo.apply)

  implicit val createSignalCmdFormat = jsonFormat7(CreateSignalCmd.apply)
  implicit val renameSignalCmdFormat = jsonFormat3(RenameSignalCmd.apply)
  implicit val selectDriverCmdFormat = jsonFormat3(SelectDriverCmd.apply)
  implicit val selectTypeCmdFormat = jsonFormat3(SelectTypeCmd.apply)
  implicit val selectKeyCmdFormat = jsonFormat3(SelectKeyCmd.apply)
  implicit val updateFuncsCmdFormat = jsonFormat3(UpdateFuncsCmd.apply)
  implicit val retrieveSignalCmdFormat = jsonFormat2(RetrieveSignalCmd.apply)
  implicit val saveSnapshotCmdFormat = jsonFormat2(SaveSnapshotCmd.apply)

  implicit val signalValueVoFormat = jsonFormat4(SignalValueVo.apply)
  implicit val signalSnapshotValueVoFormat = jsonFormat3(SignalSnapshotValueVo.apply)
  implicit val updateValueCmdFormat = jsonFormat3(UpdateValueCmd.apply)
  implicit val setValueCmdFormat = jsonFormat3(SetValueCmd.apply)
  implicit val setValueRspFormat = jsonFormat2(SetValueRsp.apply)
  implicit val getValueCmdFormat = jsonFormat2(GetValueCmd.apply)

  implicit val startSignalCmdFormat = jsonFormat2(StartSignalCmd.apply)
  implicit val stopSignalCmdFormat = jsonFormat2(StopSignalCmd.apply)

  implicit val getSupportedFuncsRspFormat = jsonFormat1(SupportedFuncsVo.apply)
  implicit val paramRangeVoFormat = jsonFormat2(ParamRange.apply)
  implicit val paramTypeFormat = ParamTypeJsonFormat
  implicit val paramMetaVoFormat = jsonFormat7(ParamMeta.apply)
  implicit val getSupportedParamsRspFormat = jsonFormat1(FuncParamsVo.apply)
}

object SignalMarshaling extends SignalMarshaling
