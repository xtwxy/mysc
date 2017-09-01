package com.wincom.dcim.domain

import com.wincom.dcim.signal.{FunctionRegistry, SetFunction}

import scala.collection.JavaConverters._
import scala.language.postfixOps

/**
  * Created by wangxy on 17-8-28.
  */
object ThresholdFunction {
  def apply(name: String, params: Map[String, String], func: SetFunction): ThresholdFunction = new ThresholdFunction(name, params, func)

  def apply(f: ThresholdFunctionVo)(implicit registry: FunctionRegistry): ThresholdFunction = {
    val func = registry.createUnary(f.name, f.params.asJava)
    if (func.isDefined) {
      new ThresholdFunction(f.name, f.params, func.get.asInstanceOf[SetFunction])
    } else {
      throw new IllegalArgumentException(f.toString)
    }
  }

  final case class ThresholdFunctionVo(name: String, params: Map[String, String])

  object ThresholdFunctionVo {
    def apply(name: String, params: Map[String, String]): ThresholdFunctionVo = new ThresholdFunctionVo(name, params)

    def apply(func: ThresholdFunction): ThresholdFunctionVo = new ThresholdFunctionVo(func.name, func.params)
  }

}

final case class ThresholdFunction(val name: String, val params: Map[String, String], val func: SetFunction) extends SetFunction {
  override def contains(e: AnyVal): Boolean = func.contains(e)

  override def subsetOf(f: SetFunction): Boolean = func.subsetOf(f)

  override def intersects(f: SetFunction): Boolean = func.intersects(f)

  override def equals(other: Any): Boolean = other match {
    case that: ThresholdFunction =>
      name == that.name &&
        params == that.params
    case _ => {
      println("false: " + other)
      false
    }
  }

  override def hashCode(): Int = {
    val state = Seq(name, params)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

