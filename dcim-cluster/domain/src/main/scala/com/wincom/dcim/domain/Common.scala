package com.wincom.dcim.domain

import akka.actor._

import com.typesafe.config.Config

import scala.concurrent.duration.Duration

final case object Ok extends Serializable

final case object NotAvailable extends Serializable

final case object NotExist extends Serializable

final case object AlreadyExists extends Serializable

object Settings extends ExtensionKey[Settings]

class Settings(config: Config) extends Extension {
  def this(system: ExtendedActorSystem) = this(system.settings.config)

  val passivateTimeout = Duration(config.getString("passivate-timeout"))
  object http {
    val host = config.getString("http.host")
    val port = config.getInt("http.port")
  }
}
