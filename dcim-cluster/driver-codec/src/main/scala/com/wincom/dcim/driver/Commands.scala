package com.wincom.dcim.driver

import java.util.Map
import org.joda.time.DateTime

/**
  * Created by wangxy on 17-8-11.
  */

trait Handler {
 def handle(anyRef: AnyRef): Unit
}

trait DriverHandler extends Handler {
  def handleUpdateValues(values: Map[String, AnyVal]): Unit
}

trait DriverCodecHandler extends Handler {
  def handleBytesReceived(bytes: Array[Byte]): Unit
  def handleGetValue(key: String): Unit
  def handleGetValues(keys: Set[String]): Unit
  def handleSetValue(key: String, value: AnyVal): Unit
  def handleSetValues(values: Map[String, AnyVal]): Unit
}


sealed trait Command {
  def execute(handler: Handler): Unit
}

final case class Value(key: String, ts: DateTime, value: AnyVal)
final case class Values(values: List[Value])
final case object Ok
final case object NotExists
final case object NotAvailable

final case class SendBytesCmd(bytes: Array[Byte]) extends Command {
  override def execute(handler: Handler): Unit = {
    handler match {
      case h: DriverCodecHandler =>
        h.handleBytesReceived(bytes)
      case h: Handler =>
        h.handle(this)
    }
  }
}

final case class GetValueCmd(key: String) extends Command {
  override def execute(handler: Handler): Unit = {
    handler match {
      case h: DriverCodecHandler =>
        h.handleGetValue(key)
      case h: Handler =>
        h.handle(this)
    }
  }
}

final case class GetValuesCmd(keys: Set[String]) extends Command {
  override def execute(handler: Handler): Unit = {
    handler match {
      case h: DriverCodecHandler =>
        h.handleGetValues(keys)
      case h: Handler =>
        h.handle(this)
    }
  }
}

final case class SetValueCmd(key: String, value: AnyVal) extends Command {
  override def execute(handler: Handler): Unit = {
    handler match {
      case h: DriverCodecHandler =>
        h.handleSetValue(key, value)
      case h: Handler =>
        h.handle(this)
    }
  }
}

final case class SetValuesCmd(values: Map[String, AnyVal]) extends Command {
  override def execute(handler: Handler): Unit = {
    handler match {
      case h: DriverCodecHandler =>
        h.handleSetValues(values)
      case h: Handler =>
        h.handle(this)
    }
  }
}

final case class UpdateValuesCmd(values: Map[String, AnyVal]) extends Command {
  override def execute(handler: Handler): Unit = {
    handler match {
      case h: DriverHandler =>
        h.handleUpdateValues(values)
      case h: Handler =>
        h.handle(this)
    }
  }
}


