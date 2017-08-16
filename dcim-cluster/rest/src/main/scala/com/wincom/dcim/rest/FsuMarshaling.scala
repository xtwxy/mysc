package com.wincom.dcim.rest

import com.wincom.dcim.domain.Fsu._
import spray.json.DefaultJsonProtocol

/**
  * Created by wangxy on 17-8-16.
  */
trait FsuMarshaling extends DefaultJsonProtocol {
  implicit val anyValFormat = AnyValFormat
  implicit val dateTimeFormat = DateTimeFormat

  implicit val fsuVoFormat = jsonFormat4(FsuVo)
  implicit val createFsuCmdFormat = jsonFormat4(CreateFsuCmd)
  implicit val renameFsuCmdFormat = jsonFormat2(RenameFsuCmd)
  implicit val changeModelCmdFormat = jsonFormat2(ChangeModelCmd)
  implicit val addParamsCmdFormat = jsonFormat2(AddParamsCmd)
  implicit val removeParamsCmdFormat = jsonFormat2(RemoveParamsCmd)

  implicit val getPortCmdFormat = jsonFormat2(GetPortCmd)
  implicit val sendBytesCmdFormat = jsonFormat2(SendBytesCmd)
}

object FsuMarshaling extends FsuMarshaling
