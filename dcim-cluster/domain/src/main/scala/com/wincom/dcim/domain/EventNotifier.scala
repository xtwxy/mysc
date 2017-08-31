package com.wincom.dcim.domain

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Subscribe

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

/**
  * Created by wangxy on 17-8-30.
  */
object EventNotifier{
  def props(topicName: String) = Props(new EventNotifier(topicName))
  def name(topicName: String) = s"Notifier,${topicName}"
}

class EventNotifier(topicName: String) extends Actor with ActorLogging {

  val mediator = DistributedPubSub(context.system).mediator

  implicit def executionContext: ExecutionContext = context.dispatcher

  mediator ! Subscribe(topicName, self)

  override def receive: Receive = {
    case x => log.info("{}", x)
  }
}
