package com.wincom.dcim.rest

import com.wincom.dcim.domain.Fsu._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

/**
  * Created by wangxy on 17-8-16.
  */
trait FsuMarshaling extends DefaultJsonProtocol {
  implicit val anyValFormat: AnyValFormat.type = AnyValFormat
  implicit val dateTimeFormat: DateTimeFormat.type = DateTimeFormat

  implicit val fsuVoFormat: RootJsonFormat[FsuVo] = jsonFormat4(FsuVo)
  implicit val createFsuCmdFormat: RootJsonFormat[CreateFsuCmd] = jsonFormat4(CreateFsuCmd)
  implicit val renameFsuCmdFormat: RootJsonFormat[RenameFsuCmd] = jsonFormat2(RenameFsuCmd)
  implicit val changeModelCmdFormat: RootJsonFormat[ChangeModelCmd] = jsonFormat2(ChangeModelCmd)
  implicit val addParamsCmdFormat: RootJsonFormat[AddParamsCmd] = jsonFormat2(AddParamsCmd)
  implicit val removeParamsCmdFormat: RootJsonFormat[RemoveParamsCmd] = jsonFormat2(RemoveParamsCmd)

  implicit val getPortCmdFormat: RootJsonFormat[GetPortCmd] = jsonFormat2(GetPortCmd)
  implicit val sendBytesCmdFormat: RootJsonFormat[SendBytesCmd] = jsonFormat2(SendBytesCmd)

  implicit val retrieveFsuCmdFormat: RootJsonFormat[RetrieveFsuCmd] = jsonFormat1(RetrieveFsuCmd)
  implicit val startFsuCmdFormat: RootJsonFormat[StartFsuCmd] = jsonFormat1(StartFsuCmd)
  implicit val stopFsuCmdFormat: RootJsonFormat[StopFsuCmd] = jsonFormat1(StopFsuCmd)
}

object FsuMarshaling extends FsuMarshaling
