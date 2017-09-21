package com.wincom.dcim.rest

import com.wincom.dcim.message.common._
import com.wincom.dcim.message.driver.SendBytesCmd
import com.wincom.dcim.message.fsu._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

/**
  * Created by wangxy on 17-8-16.
  */
trait FsuMarshaling extends DefaultJsonProtocol {
  implicit val anyValFormat = AnyValFormat
  implicit val dateTimeFormat = DateTimeJsonFormat

  implicit val fsuVoFormat = jsonFormat4(FsuVo.apply)
  implicit val createFsuCmdFormat = jsonFormat5(CreateFsuCmd.apply)
  implicit val renameFsuCmdFormat = jsonFormat3(RenameFsuCmd.apply)
  implicit val changeModelCmdFormat = jsonFormat3(ChangeModelCmd.apply)
  implicit val addParamsCmdFormat = jsonFormat3(AddParamsCmd.apply)
  implicit val removeParamsCmdFormat = jsonFormat3(RemoveParamsCmd.apply)

  implicit val getPortCmdFormat = jsonFormat3(GetPortCmd.apply)
  implicit val byteStringJsonFormat = ByteStringJsonFormat
  implicit val sendBytesCmdFormat = jsonFormat3(SendBytesCmd.apply)

  implicit val retrieveFsuCmdFormat = jsonFormat2(RetrieveFsuCmd.apply)
  implicit val startFsuCmdFormat = jsonFormat2(StartFsuCmd.apply)
  implicit val restartFsuCmdFormat = jsonFormat2(RestartFsuCmd.apply)
  implicit val stopFsuCmdFormat = jsonFormat2(StopFsuCmd.apply)

  implicit val getSupportedModelsRspFormat = jsonFormat1(SupportedModelsVo.apply)
  implicit val getSupportedParamsRspFormat = jsonFormat1(ModelParamsVo.apply)
}

object FsuMarshaling extends FsuMarshaling
