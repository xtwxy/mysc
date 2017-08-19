package com.wincom.dcim.rest

import com.wincom.dcim.domain.Signal._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

/**
  * Created by wangxy on 17-8-16.
  */
trait SignalMarshaling extends DefaultJsonProtocol {
  implicit val anyValFormat = AnyValFormat
  implicit val dateTimeFormat = DateTimeFormat

  implicit val signalVoFormat = jsonFormat4(SignalVo)
  implicit val signalValueFormat = jsonFormat3(SignalValueVo)

  implicit val okFormat: RootJsonFormat[Ok] = jsonFormat1(Ok)

  implicit val createSignalCmdFormat = jsonFormat4(CreateSignalCmd)
  implicit val renameSignalCmdFormat = jsonFormat2(RenameSignalCmd)
  implicit val selectDriverCmdFormat = jsonFormat2(SelectDriverCmd)
  implicit val selectKeyCmdFormat = jsonFormat2(SelectKeyCmd)
  implicit val retrieveSignalCmdFormat = jsonFormat1(RetrieveSignalCmd)
  implicit val saveSnapshotCmdFormat = jsonFormat1(SaveSnapshotCmd)

  implicit val updateValueCmdFormat = jsonFormat2(UpdateValueCmd)
  implicit val setValueCmdFormat = jsonFormat2(SetValueCmd)
  implicit val getValueCmdFormat = jsonFormat1(GetValueCmd)

  implicit val startSignalCmdFormat = jsonFormat1(StartSignalCmd)
  implicit val stopSignalCmdFormat = jsonFormat1(StopSignalCmd)
}

object SignalMarshaling extends SignalMarshaling
