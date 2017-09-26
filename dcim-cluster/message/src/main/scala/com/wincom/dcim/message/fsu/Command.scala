package com.wincom.dcim.message.fsu;

/**
  * Created by wangxy on 17-9-11.
  */
trait Command extends com.wincom.dcim.message.common.Command {
  def fsuId: String
  override def entityId: String = {
    s"fsu_${fsuId}"
  }
}
