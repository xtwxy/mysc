package com.wincom.dcim.signal
import java.util._

/**
  * Created by wangxy on 17-8-24.
  */
class LinerFuncImpl(val params: Map[String, String]) extends SignalTransFunc {

  override def transform(input: AnyVal): AnyVal = {
    input match {
      case x: Boolean => !x
      case x: Double => x * 2;
      case x => x
    }
  }

  override def inverse(input: AnyVal): AnyVal = {
    input match {
      case x: Boolean => !x
      case x: Double => x / 2;
      case x => x
    }
  }
}

class LinerFuncFactory extends SignalTransFuncFactory {
  override def name(): String = "cafe-signal"

  override def create(params: Map[String, String]): Option[SignalTransFunc] = {
    Some(new LinerFuncImpl(params));
  }

  override def paramNames(): Set[String] = {
    val s: Set[String] = new HashSet()
    s.add("slope")
    s.add("intercept")
    return s
  }
}
