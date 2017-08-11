package com.wincom.dcim.sharded

import akka.actor.{ActorRef, Props}
import akka.cluster.sharding.ShardRegion
import akka.event.Logging
import akka.http.scaladsl.model.DateTime
import akka.pattern.ask
import akka.persistence.{PersistentActor, SnapshotOffer}
import akka.util.Timeout
import com.wincom.dcim.sharded.SignalActor._
import org.joda.time.Duration

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, SECONDS}
import scala.util.Success

object SignalActor {
  val numberOfShards = 100
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case cmd: Command => (cmd.signalId.toString, cmd)
  }
  val extractShardId: ShardRegion.ExtractShardId = {
    case cmd: Command => (cmd.signalId % numberOfShards).toString()
  }

  def props(signalId: Int, driverShard: ActorRef) = Props(new SignalActor(signalId, driverShard))

  def name(signalId: Int) = signalId.toString()

  trait Command {
    def signalId: Int
  }

  trait Event

  /* domain objects */
  final case class Signal(signalId: Int, driverId: Int, name: String) extends Command

  final case class SignalValue(signalId: Int, ts: DateTime, value: AnyVal) extends Command

  final case class Ok(signalId: Int) extends Command

  final case class NotAvailable(signalId: Int) extends Command

  final case class NotExist(signalId: Int) extends Command

  final case class AlreadyExists(signalId: Int) extends Command

  /* commands */
  final case class CreateSignalCmd(signalId: Int, driverId: Int, name: String) extends Command

  final case class RenameSignalCmd(signalId: Int, newName: String) extends Command

  final case class SelectDriverCmd(signalId: Int, driverId: Int) extends Command

  final case class SaveSnapshotCmd(signalId: Int) extends Command

  /* transient commands */
  final case class UpdateValueCmd(signalId: Int, value: AnyVal) extends Command

  final case class SetValueCmd(signalId: Int, value: AnyVal) extends Command

  final case class GetValueCmd(signalId: Int) extends Command

  /* events */
  final case class CreateSignalEvt(driverId: Int, name: String) extends Event

  final case class RenameSignalEvt(newName: String) extends Event

  final case class SelectDriverEvt(driverId: Int) extends Event
}

class SignalActor(val signalId: Int, driverShard: ActorRef) extends PersistentActor {

  val log = Logging(context.system.eventStream, "sharded-fsus")
  var driverId: Option[Int] = None
  var signalName: Option[String] = None
  var valueTs: Option[DateTime] = None
  var signalValue: AnyVal = 0

  override def persistenceId: String = s"${self.path.name}"

  implicit def requestTimeout: Timeout = FiniteDuration(20, SECONDS)

  implicit def executionContext: ExecutionContext = context.dispatcher

  def receiveRecover = {
    case evt: Event =>
      updateState(evt)
    case SnapshotOffer(_, Signal(_, driverId, signalName)) =>
      this.driverId = Some(driverId)
      this.signalName = Some(signalName)
    case x => log.info("RECOVER: {} {}", this, x)
  }

  def receiveCommand = {
    case CreateSignalCmd(_, driverId, signalName) =>
      persist(CreateSignalEvt(driverId, signalName))(updateState)
    case RenameSignalCmd(_, newName) =>
      persist(RenameSignalEvt(newName))(updateState)
    case SelectDriverCmd(_, driverId) =>
      persist(SelectDriverEvt(driverId))(updateState)
    case SaveSnapshotCmd =>
      if (initialized) {
        saveSnapshot(Signal(signalId, driverId.get, signalName.get))
      }
    case UpdateValueCmd(_, value) =>
      signalValue = value
    case cmd: SetValueCmd =>
      driverShard forward cmd
    case cmd: GetValueCmd =>
      if (available) {
        sender() ! SignalValue(signalId, valueTs.get, signalValue)
      } else {
        driverShard.ask(cmd).mapTo[Command].onComplete {
          case f: Success[Command] =>
            f.value match {
              case s@SignalValue(_, ts, value) =>
                valueTs = Some(ts)
                signalValue = value
                sender() ! s
              case _ =>
                sender() ! NotAvailable(signalId)
            }
          case _ =>
            sender() ! NotAvailable(signalId)
        }
      }
    case x => log.info("COMMAND: {} {}", this, x)
  }

  private def updateState: (Event => Unit) = {
    case CreateSignalEvt(driverId, signalName) =>
      this.driverId = Some(driverId)
      this.signalName = Some(signalName)
    case RenameSignalEvt(newName) =>
      this.signalName = Some(newName)
    case SelectDriverEvt(driverId) =>
      this.driverId = Some(driverId)
  }

  private def initialized: Boolean = (driverId.isDefined && signalName.isDefined)

  private def available: Boolean = {
    if (valueTs.isDefined) {
      Duration.millis(DateTime.now.clicks - valueTs.get.clicks).isShorterThan(Duration.standardMinutes(1))
    } else {
      false
    }
  }
}
