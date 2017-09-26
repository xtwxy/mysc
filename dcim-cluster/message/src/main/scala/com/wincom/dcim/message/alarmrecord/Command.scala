package com.wincom.dcim.message.alarmrecord

import com.google.protobuf.timestamp.Timestamp
import com.wincom.dcim.message.util.DateFormat._;
/**
  * Created by wangxy on 17-9-11.
  */
trait Command extends com.wincom.dcim.message.common.Command {
  def alarmId: String
  def beginTime: Timestamp
  override def entityId: String = {
    s"${alarmId}_${formatTimestamp(beginTime.seconds * 1000 + beginTime.nanos / 1000000)}"
  }
}
