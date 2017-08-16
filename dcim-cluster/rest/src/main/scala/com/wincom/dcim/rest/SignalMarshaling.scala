package com.wincom.dcim.rest

import com.wincom.dcim.domain.Signal._
import spray.json.DefaultJsonProtocol

/**
  * Created by wangxy on 17-8-16.
  */
trait SignalMarshaling extends DefaultJsonProtocol {
  implicit val anyValFormat = AnyValFormat
  implicit val dateTimeFormat = DateTimeFormat

  implicit val signalVoFormat = jsonFormat4(SignalVo)
  implicit val signalValueFormat = jsonFormat3(SignalValue)
  implicit val createSignalCmdFormat = jsonFormat4(CreateSignalCmd)
  implicit val renameSignalCmdFormat = jsonFormat2(RenameSignalCmd)
  implicit val selectDriverCmdFormat = jsonFormat2(SelectDriverCmd)
  implicit val selectKeyCmdFormat = jsonFormat2(SelectKeyCmd)
  implicit val saveSnapshotCmdFormat = jsonFormat1(SaveSnapshotCmd)

  implicit val updateValueCmdFormat = jsonFormat2(UpdateValueCmd)
  implicit val setValueCmdFormat = jsonFormat2(SetValueCmd)
  implicit val getValuecmdFormat = jsonFormat1(GetValueCmd)
}

object SignalMarshaling extends SignalMarshaling
