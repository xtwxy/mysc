package com.wincom.dcim.signal

import java.util.{Map, Set};
/**
  * Created by wangxy on 17-8-24.
  */

trait Function;

trait FunctionFactory {
  def name(): String
  def paramNames(): Set[String]
}

trait UnaryFunction extends Function {
  def transform(input: AnyVal): AnyVal
  def inverse(input: AnyVal): AnyVal
}

trait UnaryFunctionFactory extends  FunctionFactory {
  def create(params: Map[String, String]): Option[UnaryFunction]
}

trait BinaryFunction extends Function {
  def transform(lhs: AnyVal, rhs: AnyVal): AnyVal
}

trait BinaryFunctionFactory extends  FunctionFactory {
  def create(params: Map[String, String]): Option[BinaryFunction]
}

