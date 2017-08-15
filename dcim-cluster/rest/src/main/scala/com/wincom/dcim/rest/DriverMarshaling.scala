package com.wincom.dcim.rest

import java.util.Map

import com.wincom.dcim.domain.Driver.DriverVo
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

/**
  * Created by wangxy on 17-8-15.
  */
trait DriverMarshaling extends DefaultJsonProtocol {
  implicit val driverVoFormat: RootJsonFormat[DriverVo] = jsonFormat5(DriverVo)
}

object DriverMarshaling extends DriverMarshaling
