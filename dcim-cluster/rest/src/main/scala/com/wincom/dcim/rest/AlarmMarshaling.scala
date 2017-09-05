package com.wincom.dcim.rest

import com.wincom.dcim.domain.Alarm._
import com.wincom.dcim.domain.AlarmCondition._
import com.wincom.dcim.domain.ThresholdFunction._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait AlarmMarshaling extends DefaultJsonProtocol {
  implicit val anyValFormat: AnyValFormat.type = AnyValFormat
  implicit val dateTimeFormat: DateTimeJsonFormat.type = DateTimeJsonFormat


  implicit val thresholdFunctionVoFormat: RootJsonFormat[ThresholdFunctionVo] = jsonFormat2(ThresholdFunctionVo)
  implicit val alarmConditionVoFormat: RootJsonFormat[AlarmConditionVo] = jsonFormat4(AlarmConditionVo)
  implicit val alarmVoFormat: RootJsonFormat[AlarmVo] = jsonFormat4(AlarmVo)
  implicit val alarmValueVoFormat: RootJsonFormat[AlarmValueVo] = jsonFormat5(AlarmValueVo)
  implicit val createAlarmCmdFormat = jsonFormat4(CreateAlarmCmd)
  implicit val renameAlarmCmdFormat = jsonFormat2(RenameAlarmCmd)
  implicit val selectSignalCmdFormat = jsonFormat2(SelectSignalCmd)
  implicit val addConditionCmdFormat = jsonFormat2(AddConditionCmd)
  implicit val removeConditionCmdFormat = jsonFormat2(RemoveConditionCmd)
  implicit val replaceConditionCmdFormat = jsonFormat3(ReplaceConditionCmd)
  implicit val retrieveAlarmCmdFormat = jsonFormat1(RetrieveAlarmCmd)
  implicit val getAlarmValueCmdFormat = jsonFormat1(GetAlarmValueCmd)
  implicit val evalAlarmValueCmdFormat = jsonFormat1(EvalAlarmValueCmd)
  implicit val passivateAlarmCmdFormat = jsonFormat1(PassivateAlarmCmd)
}

object AlarmMarshaling extends AlarmMarshaling