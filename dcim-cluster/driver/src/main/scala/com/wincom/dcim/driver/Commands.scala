package com.wincom.dcim.driver

/**
  * Created by wangxy on 17-8-11.
  */
sealed trait Command {
  def driverId: Int
}

final case class GetValue(driverId: Int, signalId: Int, value: AnyVal)
