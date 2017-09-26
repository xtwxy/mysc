package com.wincom.dcim.message.alarm

import com.google.protobuf.timestamp.Timestamp
import com.wincom.dcim.message.util.DateFormat._;

/**
  * Created by wangxy on 17-9-11.
  */
trait Command extends com.wincom.dcim.message.common.Command {
  def alarmId: String
  override def entityId: String = {
    s"alarm_${alarmId}"
  }
}
