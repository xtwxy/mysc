package com.wincom.dcim.sharded

import akka.actor.{Props, ReceiveTimeout}
import akka.cluster.sharding.ShardRegion
import akka.event.Logging
import akka.persistence.{PersistentActor, SnapshotOffer}
import com.wincom.dcim.rest.Settings
import com.wincom.dcim.sharded.FsuActor._
import org.joda.time.DateTime

object FsuActor {
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case cmd: Command => (cmd.fsuId.toString, cmd)
  }
  val extractShardId: ShardRegion.ExtractShardId = {
    case cmd: Command => (cmd.fsuId % numberOfShards).toString()
  }
  var numberOfShards = 100

  def props(fsuId: Int) = Props(new FsuActor(fsuId))

  def name(fsuId: Int) = fsuId.toString()

  trait Command {
    def fsuId: Int
  }

  case class Fsu(id: Int, name: String)

  case class Fsus(fsus: List[Fsu])

  final case class CreateFsu(fsuId: Int, name: String) extends Command

  final case class Ping(fsuId: Int) extends Command

  final case class Pong(fsuId: Int, ts: DateTime) extends Command

  final case class StopFsu(fsuId: Int) extends Command
}

class FsuActor(val fsuId: Int) extends PersistentActor {
  val log = Logging(context.system.eventStream, "sharded-fsus")
  var fsuName: String = null
  var isDirty: Boolean = true

  def receiveRecover = {
    case cmd: CreateFsu =>
      updateState(cmd)
    case SnapshotOffer(_, Fsu(fsuId, name)) =>
      this.fsuName = name
    case x => log.info("RECOVER: {} {}", this, x)
  }

  context.setReceiveTimeout(Settings(context.system).passivateTimeout)

  private def updateState: (Command => Unit) = {
    case CreateFsu(fsuId, name) =>
      log.info("UPDATE: persistenceId = {} fsuId = {} name = {}", persistenceId, fsuId, name)
      this.fsuName = name
  }

  override def persistenceId: String = s"${self.path.name}"

  def receiveCommand = {
    case cmd: CreateFsu =>
      persist(cmd)(updateState)
      this.isDirty = true
    case x: ReceiveTimeout =>
      log.info("COMMAND: {} {}", this, x)
      if (isDirty) {
        saveSnapshot(Fsu(fsuId, fsuName))
        isDirty = false
      }
    case cmd@Ping(id) =>
      log.info("COMMAND: {} {}", this, cmd)
    case StopFsu => context.stop(self)
    case x => log.info("COMMAND: {} {}", this, x)
  }
}