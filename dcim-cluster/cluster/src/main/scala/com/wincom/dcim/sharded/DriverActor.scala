package com.wincom.dcim.sharded

import akka.actor.{ActorRef, Props}
import akka.cluster.sharding.ShardRegion
import akka.event.Logging
import akka.persistence.{PersistentActor, SnapshotOffer}
import akka.util.Timeout
import com.wincom.dcim.sharded.DriverActor._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, SECONDS}

object DriverActor {
  val numberOfShards = 100
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case cmd: Command => (cmd.driverId.toString, cmd)
  }
  val extractShardId: ShardRegion.ExtractShardId = {
    case cmd: Command => (cmd.driverId % numberOfShards).toString()
  }

  def props(driverId: Int, fsuShard: ActorRef) = Props(new DriverActor(driverId, fsuShard))

  def name(driverId: Int) = driverId.toString()

  trait Command {
    def driverId: Int
  }

  trait Event

  /* domain objects */
  final case class Driver(driverId: Int, name: String, params: Map[String, String]) extends Command

  final case class Ok(driverId: Int) extends Command

  final case class NotAvailable(driverId: Int) extends Command

  final case class NotExist(driverId: Int) extends Command

  final case class AlreadyExists(driverId: Int) extends Command

  /* commands */
  final case class CreateDriverCmd(driverId: Int, name: String, params: Map[String, String]) extends Command

  final case class RenameDriverCmd(driverId: Int, newName: String) extends Command

  final case class SaveSnapshotCmd(driverId: Int) extends Command

  /* transient commands */

  /* events */
  final case class CreateDriverEvt(name: String) extends Event

  final case class RenameDriverEvt(newName: String) extends Event
}

class DriverActor(val driverId: Int, fsuShard: ActorRef) extends PersistentActor {

  val log = Logging(context.system.eventStream, "sharded-fsus")

  var driverName: Option[String] = None
  var params: Map[String, String] = Map()

  override def persistenceId: String = s"${self.path.name}"

  implicit def requestTimeout: Timeout = FiniteDuration(20, SECONDS)

  implicit def executionContext: ExecutionContext = context.dispatcher

  def receiveRecover = {
    case evt: Event =>
      updateState(evt)
    case SnapshotOffer(_, Driver(_, driverId, signalName)) =>
    case x => log.info("RECOVER: {} {}", this, x)
  }

  def receiveCommand = {
    case x => log.info("COMMAND: {} {}", this, x)
  }

  private def updateState: (Event => Unit) = {
    case x =>
  }

}
