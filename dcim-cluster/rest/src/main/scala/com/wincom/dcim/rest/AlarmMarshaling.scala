package com.wincom.dcim.rest

import com.wincom.dcim.message.alarm._
import com.wincom.dcim.message.common.{ParamMeta, ParamRange}
import com.wincom.dcim.message.signal.{FuncParamsVo, SupportedFuncsVo}
import spray.json._

trait AlarmMarshaling extends DefaultJsonProtocol {
  implicit val anyValFormat: AnyValFormat.type = AnyValFormat
  implicit val dateTimeFormat: DateTimeJsonFormat.type = DateTimeJsonFormat

  implicit val alarmLevelJsonFormat = AlarmLevelJsonFormat
  implicit val signalTypeJsonFormat = SignalTypeJsonFormat
  implicit val thresholdFunctionVoFormat = jsonFormat2(ThresholdFunctionVo.apply)
  implicit val alarmConditionVoFormat = jsonFormat4(AlarmConditionVo.apply)
  implicit val inclusiveConditionVoFormat = jsonFormat1(InclusiveConditionVo.apply)
  implicit val exclusiveConditionVoFormat = jsonFormat1(ExclusiveConditionVo.apply)

  implicit val alarmVoFormat = jsonFormat4(AlarmVo.apply)
  implicit val alarmValueVoFormat = jsonFormat5(AlarmValueVo.apply)
  implicit val createAlarmCmdFormat = jsonFormat5(CreateAlarmCmd.apply)
  implicit val renameAlarmCmdFormat = jsonFormat3(RenameAlarmCmd.apply)
  implicit val selectSignalCmdFormat = jsonFormat3(SelectSignalCmd.apply)
  implicit val addConditionCmdFormat = jsonFormat3(AddConditionCmd.apply)
  implicit val removeConditionCmdFormat = jsonFormat3(RemoveConditionCmd.apply)
  implicit val replaceConditionCmdFormat = jsonFormat4(ReplaceConditionCmd.apply)
  implicit val retrieveAlarmCmdFormat = jsonFormat2(RetrieveAlarmCmd.apply)
  implicit val getAlarmValueCmdFormat = jsonFormat2(GetAlarmValueCmd.apply)
  implicit val evalAlarmValueCmdFormat = jsonFormat2(EvalAlarmValueCmd.apply)
  implicit val passivateAlarmCmdFormat = jsonFormat2(PassivateAlarmCmd.apply)

  implicit val getSupportedFuncsRspFormat = jsonFormat1(SupportedFuncsVo.apply)
  implicit val paramRangeVoFormat = jsonFormat2(ParamRange.apply)
  implicit val paramTypeFormat = ParamTypeJsonFormat
  implicit val paramMetaVoFormat = jsonFormat7(ParamMeta.apply)
  implicit val getSupportedParamsRspFormat = jsonFormat1(FuncParamsVo.apply)
}

object AlarmMarshaling extends AlarmMarshaling