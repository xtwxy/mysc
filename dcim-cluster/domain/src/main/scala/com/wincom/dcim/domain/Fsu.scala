package com.wincom.dcim.domain

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
import com.wincom.dcim.domain.Driver.SendBytesCmd

import scala.util.Success

/**
  * Created by wangxy on 17-8-14.
  */
object Fsu {

  def props(fsuId: String, registry: FsuCodecRegistry) = Props(new Fsu(fsuId, registry))

  def name(fsuId: String) = s"fsu_$fsuId"

  sealed trait Command {
    def fsuId: String
  }

  sealed trait Event

  /* value objects */
  final case class FsuVo(id: String, name: String, model: String, params: Map[String, String]) extends Serializable

  /* commands */
  final case class CreateFsuCmd(fsuId: String, name: String, model: String, params: Map[String, String]) extends Command
  final case class RenameFsuCmd(fsuId: String, newName: String) extends Command
  final case class ChangeModelCmd(fsuId: String, newModel: String) extends Command
  final case class AddParamsCmd(fsuId: String, params: Map[String, String]) extends Command
  final case class RemoveParamsCmd(fsuId: String, params: Map[String, String]) extends Command

  final case class GetPortCmd(fsuId: String, params: Map[String, String]) extends Command
  final case class SendBytesCmd(fsuId: String, bytes: Array[Byte]) extends Command

  /* events */
  final case class CreateFsuEvt(name: String, model: String, params: Map[String, String]) extends Event
  final case class RenameFsuEvt(newName: String) extends Event
  final case class ChangeModelEvt(newModel: String) extends Event
  final case class AddParamsEvt(params: Map[String, String]) extends Event
  final case class RemoveParamsEvt(params: Map[String, String]) extends Event

  /* persistent snapshot object */
  final case class FsuPo(name: String, model: String, params: Map[String, String]) extends Serializable
}

class Fsu(val fsuId: String, val registry: FsuCodecRegistry) extends PersistentActor {
  val log = Logging(context.system.eventStream, "sharded-fsus")

  var fsuName: Option[String] = None
  var modelName: Option[String] = None
  val initParams: java.util.Map[String, String] = new java.util.HashMap()
  var fsuCodec: Option[ActorRef] = None

  override def persistenceId: String = s"fsu_${self.path.name}"

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
    case x => log.info("COMMAND: {} {}", this, x)
  }

  private def updateState: (Event => Unit) = {
    case CreateFsuEvt(name, model, params) =>
      this.fsuName = Some(name)
      this.modelName = Some(model)
      for((k, v) <- params) this.initParams.put(k, v)
    case RenameFsuEvt(newName) =>
      this.fsuName = Some(newName)
    case x => log.info("UPDATE IGNORED: {} {}", this, x)
  }

  private def createCodec(): Boolean = {
    val p = registry.create(this.modelName.get, this.initParams)
    if(p.isDefined) {
      this.fsuCodec = Some(context.system.actorOf(p.get, s"${this.modelName.get}_${fsuId}"))
      return true
    } else {
      return false
    }
  }
}
