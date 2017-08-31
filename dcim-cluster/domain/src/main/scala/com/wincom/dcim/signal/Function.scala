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
  def inverse(input: AnyVal): AnyVal
}

trait SetFunction {
  def contains(e: AnyVal): Boolean
  def subsetOf(f: SetFunction): Boolean
  def intersects(f: SetFunction): Boolean
}

trait UnaryFunction {
  def transform(input: AnyVal): AnyVal
}

trait UnaryFunctionFactory extends  FunctionFactory {
  def create(params: Map[String, String]): Option[UnaryFunction]
}

trait BinaryFunction {
  def transform(lhs: AnyVal, rhs: AnyVal): AnyVal
}

trait BinaryFunctionFactory extends  FunctionFactory {
  def create(params: Map[String, String]): Option[BinaryFunction]
}

