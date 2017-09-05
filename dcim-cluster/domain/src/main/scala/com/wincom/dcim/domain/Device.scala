package com.wincom.dcim.domain

import akka.actor.{ActorLogging, Props}
import akka.persistence.PersistentActor

/**
  * Created by wangxy on 17-9-5.
  */
object Device {
  def props = Props[Device]
  def name(id: String) = id
}

class Device extends PersistentActor with ActorLogging {
  override def persistenceId: String = s"$self.path.name"
  override def receiveRecover: Receive = {
    case x => log.info("RECOVER: {} {}", this, x)
  }

  override def receiveCommand: Receive = {
    case x => log.info("COMMAND: {} {}", this, x)
  }
}
