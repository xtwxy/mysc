package com.wincom.dcim.signal

import java.util.{Map, Set};
/**
  * Created by wangxy on 17-8-24.
  */

trait FunctionFactory {
  def name(): String
  def paramNames(): Set[String]
}

trait InverseFunction {
  def inverse(input: Any): Any
}

trait SetFunction {
  def contains(e: Any): Boolean
  def subsetOf(f: SetFunction): Boolean
  def intersects(f: SetFunction): Boolean
}

trait UnaryFunction {
  def transform(input: Any): Any
}

trait UnaryFunctionFactory extends  FunctionFactory {
  def create(params: Map[String, String]): Option[UnaryFunction]
}

trait BinaryFunction {
  def transform(lhs: Any, rhs: Any): Any
}

trait BinaryFunctionFactory extends  FunctionFactory {
  def create(params: Map[String, String]): Option[BinaryFunction]
}

