package com.wincom.dcim.sharded

import akka.actor.{Props, ReceiveTimeout}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.Passivate
import com.wincom.dcim.domain.Fsu._
import com.wincom.dcim.domain.{Fsu, Settings}
import com.wincom.dcim.fsu.FsuCodecRegistry
import scala.math.Numeric.IntIsIntegral._

/**
  * Created by wangxy on 17-8-16.
  */
object ShardedFsu {
  def props(registry: FsuCodecRegistry) = Props(new ShardedFsu(registry))

  def name(fsuId: String): String = fsuId.toString

  val shardName: String = "fsu-shards"
  var numberOfShards = 100

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case cmd: Command =>
      (cmd.id.toString, cmd)
  }
  val extractShardId: ShardRegion.ExtractShardId = {
    case cmd: Command =>
      (abs(cmd.id.hashCode) % numberOfShards).toString
  }
}

class ShardedFsu(registry: FsuCodecRegistry) extends Fsu(registry) {
  val settings = Settings(context.system)
  context.setReceiveTimeout(settings.actor.passivateTimeout)

  override def unhandled(message: Any): Unit = message match {
    case ReceiveTimeout =>
      context.parent ! Passivate(stopMessage = Fsu.StopFsuCmd)
    case StopFsuCmd =>
      context.stop(self)
    case x => log.info("unhandled COMMAND: {} {}", this, x)
  }
}
