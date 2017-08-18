package com.wincom.dcim.domain

import akka.actor._

import com.typesafe.config.Config

import scala.concurrent.duration.Duration

object Settings {
  def apply(config: Config): Settings = new Settings(config)

  def apply(system: ActorSystem): Settings = new Settings(system)
}

class Settings(config: Config) extends Extension {
  def this(system: ActorSystem) = this(system.settings.config)

  object actor {
    val passivateTimeout: Duration = Duration(config.getString("akka.actor.passivate-timeout"))
    val numberOfShards: Int = config.getInt("akka.actor.number-of-shards")
  }

  object http {
    val host: String = config.getString("akka.http.server.host")
    val port: Int = config.getInt("akka.http.server.port")
  }

}
