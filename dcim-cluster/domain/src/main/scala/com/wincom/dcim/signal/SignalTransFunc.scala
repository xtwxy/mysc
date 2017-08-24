package com.wincom.dcim.signal

import java.util.{Map, Set};
/**
  * Created by wangxy on 17-8-24.
  */
trait SignalTransFunc {
  def transform(input: Any): Any
}

trait SignalTransFuncFactory {
  def name(): String
  def paramNames(): Set[String]
  def create(params: Map[String, String]): Option[SignalTransFunc]
}

