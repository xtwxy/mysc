package com.wincom.dcim.domain

import akka.event.NoLogging
import com.wincom.dcim.signal.FunctionRegistry

import scala.collection.mutable

/**
  * Created by wangxy on 17-8-31.
  */
object Main extends App {
  implicit val registry: FunctionRegistry = new FunctionRegistry(NoLogging).initialize()

  val ordering = Ordering.fromLessThan[AlarmCondition]((x, y) => x.subsetOf(y))

  var set: mutable.Set[AlarmCondition] = mutable.TreeSet.empty(ordering)
  val ac0 = AlarmConditionVo(ThresholdFuncVo("LessThan", Map("threshold" -> "1.0", "insensitivity-zone" -> "0.1")), 1, "Critical", "Normal")
  val ac1 = AlarmConditionVo(ThresholdFuncVo("LessThan", Map("threshold" -> "1.0", "insensitivity-zone" -> "0.1")), 1, "Critical", "Normal")
  val ac2 = AlarmConditionVo(ThresholdFuncVo("LessThan", Map("threshold" -> "2.0", "insensitivity-zone" -> "0.1")), 1, "Critical", "Normal")
  val ac3 = AlarmConditionVo(ThresholdFuncVo("LessThan", Map("threshold" -> "-2.0", "insensitivity-zone" -> "0.1")), 1, "Critical", "Normal")
  val ac4 = AlarmConditionVo(ThresholdFuncVo("GreaterThan", Map("threshold" -> "-2.0", "insensitivity-zone" -> "0.1")), 1, "Critical", "Normal")
  val ac5 = AlarmConditionVo(ThresholdFuncVo("GreaterThan", Map("threshold" -> "2.0", "insensitivity-zone" -> "0.1")), 1, "Critical", "Normal")
  set += AlarmCondition(ac0)
  set += AlarmCondition(ac1)
  set += AlarmCondition(ac2)
  set += AlarmCondition(ac3)
  set += AlarmCondition(ac4)
  set += AlarmCondition(ac5)
  set.foreach(println(_))
}
