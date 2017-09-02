package com.wincom.dcim.sharded

import akka.actor.{Actor, Props}

/**
  * Created by wangxy on 17-8-29.
  */
object ShardedNotifier {
  def props = Props(new ShardedNotifier)
  def name(notifierName: String) = notifierName
}
class ShardedNotifier extends Actor {
  override def receive: Receive = {
    case _ =>
  }
}
