package actor

/**
  * Created by wangxy on 17-7-24.
  */

import akka.actor.Actor
import akka.event.Logging

class MyActor extends Actor {
  val log = Logging(context.system, this)

  def receive: PartialFunction[Any, Unit] = {
    case "test" => log.info("received test")
    case _ => log.info("received unknown message")
  }
}