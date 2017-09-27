package com.wincom.dcim.rest

import com.wincom.dcim.message.alarm._
import com.wincom.dcim.message.device._
import com.wincom.dcim.message.signal._
import spray.json._

/**
  * Created by wangxy on 17-8-15.
  */
trait DeviceMarshaling extends DefaultJsonProtocol {
  implicit val anyValFormat: AnyValFormat.type = AnyValFormat
  implicit val dateTimeFormat: DateTimeJsonFormat.type = DateTimeJsonFormat

  implicit val signalTypeJsonFormat = SignalTypeJsonFormat
  implicit val responseTypeJsonFormat = ResponseTypeJsonFormat
  implicit val transVoFormat = jsonFormat2(TransFuncVo.apply)
  implicit val signalVoFormat = jsonFormat6(SignalVo.apply)

  implicit val alarmLevelJsonFormat = AlarmLevelJsonFormat
  implicit val thresholdFunctionVoFormat = jsonFormat2(ThresholdFunctionVo.apply)
  implicit val alarmConditionVoFormat = jsonFormat4(AlarmConditionVo.apply)
  implicit val inclusiveConditionVoFormat = jsonFormat1(InclusiveConditionVo.apply)
  implicit val exclusiveConditionVoFormat = jsonFormat1(ExclusiveConditionVo.apply)

  implicit val alarmVoFormat = jsonFormat6(AlarmVo.apply)
  implicit val deviceModuleVoFormat = jsonFormat7(DeviceModuleVo.apply)
  implicit val deviceBatchVoFormat = jsonFormat8(DeviceBatchVo.apply)
  implicit val deviceVoFormat = jsonFormat8(DeviceVo.apply)

  implicit val removeAlarmCmdFormat = jsonFormat3(RemoveAlarmCmd.apply)
  implicit val retrieveDeviceCmdFormat = jsonFormat2(RetrieveDeviceCmd.apply)
  implicit val createDeviceCmdFormat = jsonFormat9(CreateDeviceCmd.apply)
  implicit val removeSignalCmdFormat = jsonFormat3(RemoveSignalCmd.apply)
  implicit val addChildCmdFormat = jsonFormat3(AddChildCmd.apply)
  implicit val changePropertyTagCodeCmdFormat = jsonFormat3(ChangePropertyTagCodeCmd.apply)
  implicit val changeDeviceTypeCmdFormat = jsonFormat3(ChangeDeviceTypeCmd.apply)
  implicit val removeChildCmdFormat = jsonFormat3(RemoveChildCmd.apply)
  implicit val renameDeviceCmdFormat = jsonFormat3(RenameDeviceCmd.apply)
  implicit val changeVendorModelCmdFormat = jsonFormat3(ChangeVendorModelCmd.apply)
  implicit val addSignalCmdFormat = jsonFormat3(AddSignalCmd.apply)
  implicit val addAlarmCmdFormat = jsonFormat3(AddAlarmCmd.apply)
}

object DeviceMarshaling extends DeviceMarshaling
