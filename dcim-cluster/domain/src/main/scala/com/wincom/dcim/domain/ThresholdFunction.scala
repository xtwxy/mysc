package com.wincom.dcim.domain

import com.wincom.dcim.message.alarm.ThresholdFunctionVo
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

  def valueObjectOf(f: ThresholdFunction): ThresholdFunctionVo = {
    ThresholdFunctionVo(f.name, f.params)
  }
}

final case class ThresholdFunction(name: String, params: Map[String, String], func: SetFunction) extends SetFunction {
  override def contains(e: Any): Boolean = func.contains(e)

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

