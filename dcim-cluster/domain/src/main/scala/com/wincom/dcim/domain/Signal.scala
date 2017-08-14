package com.wincom.dcim.domain

import akka.actor.{ActorRef, Props}
import akka.event.Logging
import akka.http.scaladsl.model.DateTime
import akka.pattern.ask
import akka.persistence.{PersistentActor, SnapshotOffer}
import akka.util.Timeout
import com.wincom.dcim.domain.Signal._
import org.joda.time.Duration

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, SECONDS}
import scala.util.Success

/**
  * Created by wangxy on 17-8-14.
  */
object Signal {
  def props(signalId: Int, driverShard: ActorRef) = Props(new Signal(signalId, driverShard))
  def name(signalId: Int) = s"signal_$signalId"

  sealed trait Command {
    def signalId: Int
  }

  sealed trait Event extends Serializable

  /* domain objects */
  final case class SignalVo(signalId: Int, name: String, driverId: Int, key: String) extends Command

  final case class SignalValue(signalId: Int, ts: DateTime, value: AnyVal) extends Command

  final case class Ok(signalId: Int) extends Command

  final case class NotAvailable(signalId: Int) extends Command

  final case class NotExist(signalId: Int) extends Command

  final case class AlreadyExists(signalId: Int) extends Command

  /* commands */
  final case class CreateSignalCmd(signalId: Int, name: String, driverId: Int, key: String) extends Command

  final case class RenameSignalCmd(signalId: Int, newName: String) extends Command

  final case class SelectDriverCmd(signalId: Int, driverId: Int) extends Command

  final case class SelectKeyCmd(signalId: Int, key: String) extends Command

  final case class SaveSnapshotCmd(signalId: Int) extends Command

  /* transient commands */
  final case class UpdateValueCmd(signalId: Int, value: SignalValue) extends Command

  final case class SetValueCmd(signalId: Int, value: AnyVal) extends Command

  final case class GetValueCmd(signalId: Int) extends Command

  /* events */
  final case class CreateSignalEvt(name: String, driverId: Int, key: String) extends Event

  final case class RenameSignalEvt(newName: String) extends Event

  final case class SelectDriverEvt(driverId: Int) extends Event

  final case class SelectKeyEvt(key: String) extends Event

  /* persistent objects */
  final case class SignalPo(name: String, driverId: Int, key: String) extends Event
}

class Signal(val signalId: Int, val driverShard: ActorRef) extends PersistentActor {

  val log = Logging(context.system.eventStream, "sharded-fsus")
  // configuration
  var signalName: Option[String] = None
  var driverId: Option[Int] = None
  var key: Option[String] = None

  // transient values
  var signalValue: Option[SignalValue] = None

  override def persistenceId: String = s"${self.path.name}"

  implicit def requestTimeout: Timeout = FiniteDuration(20, SECONDS)

  implicit def executionContext: ExecutionContext = context.dispatcher

  def receiveRecover = {
    case evt: Event =>
      updateState(evt)
    case SnapshotOffer(_, SignalPo(name, driverId, key)) =>
      this.driverId = Some(driverId)
      this.signalName = Some(name)
      this.key = Some(key)
    case x => log.info("RECOVER: {} {}", this, x)
  }

  def receiveCommand = {
    case CreateSignalCmd(_, driverId, signalName, key) =>
      persist(CreateSignalEvt(driverId, signalName, key))(updateState)
    case RenameSignalCmd(_, newName) =>
      persist(RenameSignalEvt(newName))(updateState)
    case SelectDriverCmd(_, driverId) =>
      persist(SelectDriverEvt(driverId))(updateState)
    case SaveSnapshotCmd =>
      if (initialized) {
        saveSnapshot(SignalPo(signalName.get, driverId.get, key.get))
      }
    case UpdateValueCmd(_, value) =>
      signalValue = Some(value)
    case cmd: SetValueCmd =>
      driverShard forward cmd
    case cmd: GetValueCmd =>
      if (available) {
        sender() ! signalValue
      } else {
        driverShard.ask(cmd).mapTo[Command].onComplete {
          case f: Success[Command] =>
            f.value match {
              case s: SignalValue =>
                signalValue = Some(s)
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
    case CreateSignalEvt(name, driverId, key) =>
      this.signalName = Some(name)
      this.driverId = Some(driverId)
      this.key = Some(key)
    case RenameSignalEvt(newName) =>
      this.signalName = Some(newName)
    case SelectDriverEvt(driverId) =>
      this.driverId = Some(driverId)
    case SelectKeyEvt(key) =>
      this.key = Some(key)
    case x => log.info("EVENT: {} {}", this, x)
  }

  private def initialized: Boolean = (driverId.isDefined && signalName.isDefined)

  private def available: Boolean = {
    if (signalValue.isDefined) {
      Duration.millis(DateTime.now.clicks - signalValue.get.ts.clicks).isShorterThan(Duration.standardMinutes(1))
    } else {
      false
    }
  }
}