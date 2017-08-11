package com.wincom.dcim.sharded

import akka.actor.{Actor, ActorInitializationException, DeathPactException, OneForOneStrategy, Props, SupervisorStrategy}


class SuppervisedFsu extends Actor {
  override val supervisorStrategy = OneForOneStrategy() {
    case _: IllegalArgumentException ⇒ SupervisorStrategy.Restart
    case _: ActorInitializationException ⇒ SupervisorStrategy.Restart
    case _: DeathPactException ⇒ SupervisorStrategy.Restart
    case _: Exception ⇒ SupervisorStrategy.Restart
  }
  val fsuActor = context.actorOf(Props[FsuActor], s"${self.path.name}")

  def receive = {
    case msg ⇒ fsuActor forward msg
  }
}