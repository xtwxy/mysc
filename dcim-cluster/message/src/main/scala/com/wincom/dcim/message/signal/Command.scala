package com.wincom.dcim.message.signal;

/**
  * Created by wangxy on 17-9-11.
  */
trait Command extends com.wincom.dcim.message.common.Command {
  def signalId: String
  override def entityId: String = {
    s"alarm_${signalId}"
  }
}
