package com.wincom.dcim.domain

import akka.actor._

import com.typesafe.config.Config

import scala.concurrent.duration.Duration

final case object Ok extends Serializable

final case object NotAvailable extends Serializable

final case object NotExist extends Serializable

final case object AlreadyExists extends Serializable

object Settings {
  def apply(config: Config): Settings = new Settings(config)

  def apply(system: ActorSystem): Settings = new Settings(system)
}

class Settings(config: Config) extends Extension {
  def this(system: ActorSystem) = this(system.settings.config)

  object actor {
    val passivateTimeout: Duration = Duration(config.getString("passivate-timeout"))
    val numberOfShards: Int = config.getInt("number-of-shards")
  }

  object http {
    val host: String = config.getString("http.host")
    val port: Int = config.getInt("http.port")
  }

}
