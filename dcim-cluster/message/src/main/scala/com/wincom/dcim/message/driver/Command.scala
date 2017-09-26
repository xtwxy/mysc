package com.wincom.dcim.message.driver;

/**
  * Created by wangxy on 17-9-11.
  */
trait Command extends com.wincom.dcim.message.common.Command {
  def driverId: String
  override def entityId: String = {
    s"driver,${driverId}"
  }
}
