package com.wincom.dcim.rest

import akka.actor.{ExtendedActorSystem, Extension, ExtensionKey}
import com.typesafe.config.Config

import scala.concurrent.duration.Duration

object Settings extends ExtensionKey[Settings]

class Settings(config: Config) extends Extension {
  val passivateTimeout = Duration(config.getString("passivate-timeout"))
  val numberOfShards = config.getInt("number-of-shards")

  def this(system: ExtendedActorSystem) = this(system.settings.config)

  object http {
    val host = config.getString("http.host")
    val port = config.getInt("http.port")
  }

}