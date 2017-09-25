package com.wincom.dcim.signal

import java.util

import com.wincom.dcim.message.common.{ParamMeta, ParamType}

/**
  * Created by wangxy on 17-8-24.
  */
class LinerFuncImpl(val params: java.util.Map[String, String]) extends UnaryFunction with InverseFunction {

  override def transform(input: Any): Any = {
    input match {
      case x: Boolean => !x
      case x: Double => x * 2;
      case x => x
    }
  }

  override def inverse(input: Any): Any = {
    input match {
      case x: Boolean => !x
      case x: Double => x / 2;
      case x => x
    }
  }
}

class LinerFuncFactory extends UnaryFunctionFactory {
  override def name(): String = "cafe-signal"

  override def create(params: java.util.Map[String, String]): Option[UnaryFunction] = {
    Some(new LinerFuncImpl(params));
  }

  override def paramOptions(): java.util.Set[ParamMeta] = {
    val s: java.util.Set[ParamMeta] = new java.util.HashSet()
    s.add(ParamMeta("slop", "斜率", ParamType.FLOAT, Some("1.0"), None, Map(), None))
    s.add(ParamMeta("intercept", "截距", ParamType.FLOAT, Some("0.0"), None, Map(), None))
    return s
  }

  override def displayName(): String = {
    "咖灰变换"
  }
}
