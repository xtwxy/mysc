package com.wincom.dcim.domain

import java.util

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.SECONDS
import com.wincom.dcim.domain.Fsu._
import com.wincom.dcim.fsu.FsuCodecRegistry
import akka.actor.ActorRef
import akka.pattern.ask
import akka.actor.Props
import akka.event.Logging
import akka.persistence.PersistentActor
import akka.persistence.SnapshotOffer
import akka.util.Timeout
import akka.util.Timeout.durationToTimeout
import com.wincom.dcim.domain.Driver.{Command, SendBytesCmd}

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
  final case class FsuVo(fsuId: String, name: String, model: String, params: Map[String, String]) extends Serializable

  /* commands */
  final case class CreateFsuCmd(fsuId: String, name: String, model: String, params: Map[String, String]) extends Command
  final case class RenameFsuCmd(fsuId: String, newName: String) extends Command
  final case class ChangeModelCmd(fsuId: String, newModel: String) extends Command
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
      if(!createCodec()) {
        context.stop(self)
      }
    case x => log.info("RECOVER: {} {}", this, x)

  }

  override def receiveCommand: Receive = {
    case CreateFsuCmd(_, name, model, params) =>
      persist(CreateFsuEvt(name, model, params))(updateState)
      if(!createCodec()) {
        context.stop(self)
      }
    case RenameFsuCmd(_, newName) =>
      persist(RenameFsuEvt(newName))(updateState)
    case ChangeModelCmd(_, newModel) =>
      persist(ChangeModelEvt(newModel))(updateState)
    case AddParamsCmd(_, params) =>
      persist(AddParamsEvt(params))(updateState)
    case RemoveParamsCmd(_, params) =>
      persist(RemoveParamsEvt(params))(updateState)
    case cmd: GetPortCmd =>
      this.fsuCodec.get.ask(cmd).mapTo[ActorRef].onComplete {
        case f: Success[ActorRef] =>
          sender() ! f.value
        case _ =>
          sender() ! NotAvailable
      }
    case cmd: SendBytesCmd =>
      this.fsuCodec.get forward cmd
    case RetrieveFsuCmd =>
      sender() ! FsuVo(fsuId, fsuName.get, modelName.get, initParams.toMap)
    case StartFsuCmd =>
    case StopFsuCmd =>
      context.stop(self)
    case x => log.info("COMMAND: {} {}", this, x)
  }

  private def updateState: (Event => Unit) = {
    case CreateFsuEvt(name, model, params) =>
      this.fsuName = Some(name)
      this.modelName = Some(model)
      this.initParams = this.initParams ++ params
    case RenameFsuEvt(newName) =>
      this.fsuName = Some(newName)
    case x => log.info("UPDATE IGNORED: {} {}", this, x)
  }

  private def createCodec(): Boolean = {
    val params = new util.HashMap[String, String]()
    for((k, v) <- initParams) params.put(k, v)
    val p = registry.create(this.modelName.get, params)
    if(p.isDefined) {
      this.fsuCodec = Some(context.system.actorOf(p.get, s"${this.modelName.get}_${fsuId}"))
      return true
    } else {
      return false
    }
  }
}
