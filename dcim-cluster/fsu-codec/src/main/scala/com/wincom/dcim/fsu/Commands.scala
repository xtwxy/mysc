package com.wincom.dcim.fsu

import java.util
import java.util.Map

import org.joda.time.DateTime


/**
  * Created by wangxy on 17-8-11.
  */
trait FsuCodecHandler {
  def handleGetPort(params: util.Map[String, String]): Unit
  def handleBytesReceived(bytes: Array[Byte]): Unit
}


sealed trait Command {
  def execute(handler: AnyRef): Unit
}

final case object Ok
final case object NotExists
final case object NotAvailable

final case class SendBytesCmd(bytes: Array[Byte]) extends Command {
  override def execute(handler: AnyRef): Unit = {
    handler match {
      case h: FsuCodecHandler =>
        h.handleBytesReceived(bytes)
      case _ =>
    }
  }
}

final case class GetPort(params: Map[String, String]) extends Command {
  override def execute(handler: AnyRef): Unit = {
    handler match {
      case h: FsuCodecHandler =>
        h.handleGetPort(params)
      case _ =>
    }
  }
}



