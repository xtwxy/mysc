package com.wincom.dcim.rest

import com.wincom.dcim.domain.Alarm._
import com.wincom.dcim.domain.AlarmCondition._
import com.wincom.dcim.domain.ThresholdFunction._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait AlarmMarshaling extends DefaultJsonProtocol {
  implicit val anyValFormat: AnyValFormat.type = AnyValFormat
  implicit val dateTimeFormat: DateTimeJsonFormat.type = DateTimeJsonFormat

  implicit val okFormat: RootJsonFormat[Ok] = jsonFormat1(Ok)
  implicit val notAvailableFormat: RootJsonFormat[NotAvailable] = jsonFormat1(NotAvailable)
  implicit val badCmdFormat: RootJsonFormat[BadCmd] = jsonFormat1(BadCmd)

  implicit val thresholdFunctionVoFormat: RootJsonFormat[ThresholdFunctionVo] = jsonFormat2(ThresholdFunctionVo)
  implicit val alarmConditionVoFormat: RootJsonFormat[AlarmConditionVo] = jsonFormat4(AlarmConditionVo)
  implicit val alarmVoFormat: RootJsonFormat[AlarmVo] = jsonFormat4(AlarmVo)
  implicit val alarmValueVoFormat: RootJsonFormat[AlarmValueVo] = jsonFormat5(AlarmValueVo)
  implicit val createAlarmCmdFormat = jsonFormat4(CreateAlarmCmd)
  implicit val renameAlarmCmdFormat = jsonFormat2(RenameAlarmCmd)
}

object AlarmMarshaling extends AlarmMarshaling