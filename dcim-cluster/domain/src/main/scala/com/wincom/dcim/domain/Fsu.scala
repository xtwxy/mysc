package com.wincom.dcim.domain

import java.util

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.SECONDS
import com.wincom.dcim.domain.Fsu._
import com.wincom.dcim.fsu.FsuCodecRegistry
import akka.actor.{ActorRef, Props, ReceiveTimeout}
import akka.pattern.ask
import akka.event.Logging
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import akka.util.Timeout
import akka.util.Timeout.durationToTimeout
import com.wincom.dcim.domain.Driver.DriverPo

import scala.util.Success

/**
  * Created by wangxy on 17-8-14.
  */
object Fsu {

  def props(registry: FsuCodecRegistry) = Props(new Fsu(registry))

  def name(fsuId: String) = s"$fsuId"

  sealed trait Command {
    def fsuId: String
  }

  sealed trait Event

  /* value objects */
  final case class FsuVo(fsuId: String, name: String, model: String, params: Map[String, String]) extends Command

  final case class Ok(fsuId: String) extends Command

  final case class NotAvailable(fsuId: String) extends Command

  final case class NotExist(fsuId: String) extends Command

  final case class AlreadyExists(fsuId: String) extends Command

  /* commands */
  final case class CreateFsuCmd(fsuId: String, name: String, model: String, params: Map[String, String]) extends Command
  final case class RenameFsuCmd(fsuId: String, newName: String) extends Command
  final case class ChangeModelCmd(fsuId: String, newModel: String) extends Command
  final case class SaveSnapshotCmd(fsuId: String) extends Command
  final case class AddParamsCmd(fsuId: String, params: Map[String, String]) extends Command
  final case class RemoveParamsCmd(fsuId: String, params: Map[String, String]) extends Command

  final case class GetPortCmd(fsuId: String, params: Map[String, String]) extends Command
  final case class SendBytesCmd(fsuId: String, bytes: Array[Byte]) extends Command

  final case class RetrieveFsuCmd(fsuId: String) extends Command
  final case class StartFsuCmd(fsuId: String) extends Command
  final case class StopFsuCmd(fsuId: String) extends Command

  /* events */
  final case class CreateFsuEvt(name: String, model: String, params: Map[String, String]) extends Event
  final case class RenameFsuEvt(newName: String) extends Event
  final case class ChangeModelEvt(newModel: String) extends Event
  final case class AddParamsEvt(params: Map[String, String]) extends Event
  final case class RemoveParamsEvt(params: Map[String, String]) extends Event

  /* persistent snapshot object */
  final case class FsuPo(name: String, model: String, params: Map[String, String]) extends Serializable
}

class Fsu(val registry: FsuCodecRegistry) extends PersistentActor {
  val log = Logging(context.system.eventStream, "sharded-fsus")

  var fsuName: Option[String] = None
  var modelName: Option[String] = None
  var initParams: collection.mutable.Map[String, String] = collection.mutable.HashMap()
  var fsuCodec: Option[ActorRef] = None

  def fsuId: String = s"${self.path.name}"
  override def persistenceId: String = s"${self.path.name}"

  implicit def requestTimeout: Timeout = FiniteDuration(20, SECONDS)

  implicit def executionContext: ExecutionContext = context.dispatcher

  override def receiveRecover: Receive = {
    case evt: Event =>
      updateState(evt)
    case SnapshotOffer(_, FsuPo(name, model, params)) =>
      this.fsuName = Some(name)
      this.modelName = Some(model)
      for((k, v) <- params) this.initParams.put(k, v)
    case x: RecoveryCompleted =>
      log.info("RECOVERY Completed: {} {}", this, x)
      if(!valid() || !createCodec()) {
        log.warning("Cannot create FSU Codec.")
      }
    case x => log.info("RECOVER: {} {}", this, x)

  }

  override def receiveCommand: Receive = {
    case CreateFsuCmd(_, name, model, params) =>
      persist(CreateFsuEvt(name, model, params))(updateState)
    case RenameFsuCmd(_, newName) =>
      persist(RenameFsuEvt(newName))(updateState)
    case ChangeModelCmd(_, newModel) =>
      persist(ChangeModelEvt(newModel))(updateState)
    case SaveSnapshotCmd(_) =>
      if(valid()) saveSnapshot(FsuPo(fsuName.get, modelName.get, initParams.toMap))
    case AddParamsCmd(_, params) =>
      persist(AddParamsEvt(params))(updateState)
    case RemoveParamsCmd(_, params) =>
      persist(RemoveParamsEvt(params))(updateState)
    case cmd: GetPortCmd =>
      if(!valid()) {
        sender() ! NotExist(fsuId)
      } else {
        this.fsuCodec.get.ask(cmd).mapTo[ActorRef].onComplete {
          case f: Success[ActorRef] =>
            sender() ! f.value
          case _ =>
            sender() ! NotAvailable(fsuId)
        }
      }
    case cmd: SendBytesCmd =>
      if(this.fsuCodec.isDefined) {
        this.fsuCodec.get forward cmd
      } else {
        log.warning("Message CANNOT be delivered because codec not started: {} {}", this, cmd)
      }
    case RetrieveFsuCmd(_) =>
      if(!valid()) {
        sender() ! NotExist(fsuId)
      } else {
        sender() ! FsuVo(fsuId, fsuName.get, modelName.get, initParams.toMap)
      }
    case StartFsuCmd(_) =>
    case StopFsuCmd(_) =>
      stop()
    case _: ReceiveTimeout =>
      stop()
    case x => log.info("default COMMAND: {} {}", this, x)
  }

  private def updateState: (Event => Unit) = {
    case CreateFsuEvt(name, model, params) =>
      this.fsuName = Some(name)
      this.modelName = Some(model)
      this.initParams = this.initParams ++ params
    case RenameFsuEvt(newName) =>
      this.fsuName = Some(newName)
    case ChangeModelEvt(newModel) =>
      this.modelName = Some(newModel)
    case AddParamsEvt(params) =>
      this.initParams = this.initParams ++ params
    case RemoveParamsEvt(params) =>
      this.initParams = this.initParams.filter(p => !params.contains(p._1))
    case x => log.info("UPDATE IGNORED: {} {}", this, x)
  }

  private def createCodec(): Boolean = {
    val params = new util.HashMap[String, String]()
    for((k, v) <- initParams) params.put(k, v)
    val p = registry.create(this.modelName.get, params)
    if(p.isDefined) {
      log.warning("{}: {}", this.modelName.get, p.get)
      this.fsuCodec = Some(context.system.actorOf(p.get, s"${this.modelName.get}_${fsuId}"))
      true
    } else {
      false
    }
  }
  private def stop() = {
    log.info("Stopping: {}", this)
    if(fsuCodec.isDefined) {
      context.stop(fsuCodec.get)
      fsuCodec = None
    }
    context.stop(self)
  }
  private def valid(): Boolean = {
    fsuName.isDefined && modelName.isDefined
  }
}
