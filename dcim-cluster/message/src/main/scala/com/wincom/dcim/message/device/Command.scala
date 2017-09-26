package com.wincom.dcim.message.device;

/**
  * Created by wangxy on 17-9-11.
  */
trait Command extends com.wincom.dcim.message.common.Command {
  def deviceId: String
  override def entityId: String = {
    s"device_${deviceId}"
  }
}
