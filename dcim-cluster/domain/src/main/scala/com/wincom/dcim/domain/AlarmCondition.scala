package com.wincom.dcim.domain

import com.wincom.dcim.message.alarm.{AlarmConditionVo, AlarmLevel, ThresholdFunctionVo}
import com.wincom.dcim.signal.{FunctionRegistry, SetFunction}

import scala.language.postfixOps

/**
  * Created by wangxy on 17-8-28.
  */
object AlarmCondition {
  def apply(func: ThresholdFunction, level: AlarmLevel, positiveDesc: String, negativeDesc: String): AlarmCondition = new AlarmCondition(func, level, positiveDesc, negativeDesc)

  def apply(c: AlarmConditionVo)(implicit registry: FunctionRegistry): AlarmCondition = new AlarmCondition(ThresholdFunction(c.func), c.level, c.positiveDesc, c.negativeDesc)

  def valueObjectOf(c: AlarmCondition): AlarmConditionVo = {
    AlarmConditionVo(ThresholdFunction.valueObjectOf(c.func), c.level, c.positiveDesc, c.negativeDesc)
  }
}

final class AlarmCondition(val func: ThresholdFunction, val level: AlarmLevel, val positiveDesc: String, val negativeDesc: String) extends SetFunction {
  override def contains(e: Any): Boolean = func.contains(e)

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

