package com.wincom.dcim.domain

import akka.event.NoLogging
import com.wincom.dcim.message.alarm.AlarmLevel.LEVEL_1
import com.wincom.dcim.message.alarm._
import com.wincom.dcim.signal.FunctionRegistry

import scala.collection.mutable

/**
  * Created by wangxy on 17-8-31.
  */
object Main extends App {
  implicit val registry: FunctionRegistry = new FunctionRegistry(NoLogging).initialize()

  val ordering = Ordering.fromLessThan[AlarmCondition]((x, y) => x != y && x.subsetOf(y))

  var set2: mutable.Set[AlarmCondition] = mutable.Set()
  val ac0 = new AlarmConditionVo(ThresholdFunctionVo("LessThan", Map("threshold" -> "1.0", "insensitivity-zone" -> "0.1")), LEVEL_1, "Critical", "Normal")
  val ac1 = new AlarmConditionVo(ThresholdFunctionVo("LessThan", Map("threshold" -> "1.0", "insensitivity-zone" -> "0.1")), LEVEL_1, "Critical", "Normal")
  val ac2 = new AlarmConditionVo(ThresholdFunctionVo("LessThan", Map("threshold" -> "2.0", "insensitivity-zone" -> "0.1")), LEVEL_1, "Critical", "Normal")
  val ac3 = new AlarmConditionVo(ThresholdFunctionVo("LessThan", Map("threshold" -> "-2.0", "insensitivity-zone" -> "0.1")), LEVEL_1, "Critical", "Normal")
  val ac4 = new AlarmConditionVo(ThresholdFunctionVo("GreaterThan", Map("threshold" -> "-2.0", "insensitivity-zone" -> "0.1")), LEVEL_1, "Critical", "Normal")
  val ac5 = new AlarmConditionVo(ThresholdFunctionVo("GreaterThan", Map("threshold" -> "2.0", "insensitivity-zone" -> "0.1")), LEVEL_1, "Critical", "Normal")
  set2 += AlarmCondition(ac0)
  set2 += AlarmCondition(ac1)
  set2 += AlarmCondition(ac2)
  set2 += AlarmCondition(ac3)
  set2 += AlarmCondition(ac4)
  set2 += AlarmCondition(ac5)

  while (!set2.isEmpty) {
    var s = mutable.TreeSet.empty(ordering)
    set2.foreach(x => s += x)
    println("partition:")
    s.foreach(println(_))
    set2 --= s
  }
  set2.foreach(println(_))
}
