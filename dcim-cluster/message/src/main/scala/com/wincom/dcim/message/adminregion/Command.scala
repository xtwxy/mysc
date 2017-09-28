package com.wincom.dcim.message.adminregion;

/**
  * Created by wangxy on 17-9-11.
  */
trait Command extends com.wincom.dcim.message.common.Command {
  def regionId: String
  override def entityId: String = {
    s"alarm_${regionId}"
  }
}
