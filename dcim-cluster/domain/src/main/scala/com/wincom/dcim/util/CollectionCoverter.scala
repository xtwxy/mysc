package com.wincom.dcim.util

import scala.collection.JavaConverters._
/**
  * Created by wangxy on 17-8-22.
  */
object CollectionCoverter {
  def toImmutableMap[A, B](jm: java.util.Map[A, B]): Map[A, B] = {
    jm.asScala.toMap
  }
}
