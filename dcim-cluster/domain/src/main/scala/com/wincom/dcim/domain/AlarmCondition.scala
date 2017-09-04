package com.wincom.dcim.domain

import com.wincom.dcim.domain.ThresholdFunction.ThresholdFunctionVo
import com.wincom.dcim.signal.{FunctionRegistry, SetFunction}

import scala.language.postfixOps

/**
  * Created by wangxy on 17-8-28.
  */
object AlarmCondition {
  def apply(func: ThresholdFunction, level: Int, positiveDesc: String, negativeDesc: String): AlarmCondition = new AlarmCondition(func, level, positiveDesc, negativeDesc)

  def apply(c: AlarmConditionVo)(implicit registry: FunctionRegistry): AlarmCondition = new AlarmCondition(ThresholdFunction(c.func), c.level, c.positiveDesc, c.negativeDesc)

  final case class AlarmConditionVo(func: ThresholdFunctionVo, level: Int, positiveDesc: String, negativeDesc: String) {
    def this(cond: AlarmCondition) = this(new ThresholdFunctionVo(cond.func), cond.level, cond.positiveDesc, cond.negativeDesc)
  }
}

final class AlarmCondition(val func: ThresholdFunction, val level: Int, val positiveDesc: String, val negativeDesc: String) extends SetFunction {
  override def contains(e: AnyVal): Boolean = func.contains(e)

  override def subsetOf(f: SetFunction): Boolean = {
    f match {
      case t: AlarmCondition =>
        func.subsetOf(t.func.func)
      case _ =>
        func.subsetOf(f)
    }
  }

  override def intersects(f: SetFunction): Boolean = {
    f match {
      case t: AlarmCondition =>
        func.intersects(t.func.func)
      case _ =>
        func.subsetOf(f)
    }
  }

  override def equals(other: Any): Boolean = other match {
    case that: AlarmCondition =>
      func == that.func &&
        level == that.level &&
        positiveDesc == that.positiveDesc &&
        negativeDesc == that.negativeDesc
    case _ => {
      println("false: " + other)
      false
    }
  }

  override def hashCode(): Int = {
    val state = Seq(func, level, positiveDesc, negativeDesc)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

