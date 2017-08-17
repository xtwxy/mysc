package com.wincom.dcim.sharded

import akka.actor.{Actor, ActorInitializationException, DeathPactException, OneForOneStrategy, Props, SupervisorStrategy}
import com.wincom.dcim.domain.Fsu


class SuppervisedFsu extends Actor {
  override val supervisorStrategy = OneForOneStrategy() {
    case _: IllegalArgumentException ⇒ SupervisorStrategy.Restart
    case _: ActorInitializationException ⇒ SupervisorStrategy.Restart
    case _: DeathPactException ⇒ SupervisorStrategy.Restart
    case _: Exception ⇒ SupervisorStrategy.Restart
  }
  val fsuActor = context.actorOf(Props[Fsu], s"${self.path.name}")

  def receive = {
    case msg ⇒ fsuActor forward msg
  }
}