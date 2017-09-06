package com.wincom.dcim.domain

import akka.actor.{ActorRef, Props, ReceiveTimeout}
import akka.event.Logging
import akka.pattern.ask
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import akka.util.Timeout
import akka.util.Timeout.durationToTimeout
import com.wincom.dcim.domain.Fsu._
import com.wincom.dcim.fsu.FsuCodecRegistry

import scala.collection.convert.ImplicitConversions._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, SECONDS}
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

  sealed trait Response

  sealed trait Event

  /* value objects */
  final case class FsuVo(fsuId: String, name: String, model: String, params: Map[String, String]) extends Response

  final case object Ok extends Response

  final case object NotAvailable extends Response

  final case object NotExist extends Response

  final case object AlreadyExists extends Response

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

  final case class GetSupportedModelsCmd(fsuId: String) extends Command

  final case class GetSupportedModelsRsp(fsuId: String, modelNames: Set[String]) extends Response

  final case class GetModelParamsCmd(fsuId: String, modelName: String) extends Command

  final case class GetModelParamsRsp(fsuId: String, paramNames: Set[String]) extends Response

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
      for ((k, v) <- params) this.initParams.put(k, v)
    case x: RecoveryCompleted =>
      log.info("RECOVERY Completed: {} {}", this, x)
      if (!isValid() || !createCodec()) {
        log.warning("Cannot create FSU Codec.")
      }
    case x => log.info("RECOVER: {} {}", this, x)

  }

  override def receiveCommand: Receive = {
    case CreateFsuCmd(_, name, model, params) =>
      persist(CreateFsuEvt(name, model, params))(updateState)
    case RenameFsuCmd(_, newName) =>
      if (isValid()) {
        persist(RenameFsuEvt(newName))(updateState)
      } else {
        replyToSender(NotExist)
      }
    case ChangeModelCmd(_, newModel) =>
      if (isValid()) {
        persist(ChangeModelEvt(newModel))(updateState)
      } else {
        replyToSender(NotExist)
      }
    case SaveSnapshotCmd(_) =>
      if (isValid()) {
        saveSnapshot(FsuPo(fsuName.get, modelName.get, initParams.toMap))
        replyToSender(Ok)
      } else {
        replyToSender(NotExist)
      }
    case AddParamsCmd(_, params) =>
      if (isValid()) {
        persist(AddParamsEvt(params))(updateState)
      } else {
        replyToSender(NotExist)
      }
    case RemoveParamsCmd(_, params) =>
      if (isValid()) {
        persist(RemoveParamsEvt(params))(updateState)
      } else {
        replyToSender(NotExist)
      }
    case cmd: GetPortCmd =>
      val theSender = sender()
      if (!isValid()) {
        theSender ! NotExist
      } else {
        this.fsuCodec.get.ask(cmd).mapTo[ActorRef].onComplete {
          case f: Success[ActorRef] =>
            theSender ! f.value
          case _ =>
            theSender ! NotAvailable
        }
      }
    case cmd: SendBytesCmd =>
      if (isValid()) {
        if (this.fsuCodec.isDefined) {
          this.fsuCodec.get forward cmd
        } else {
          log.warning("Message CANNOT be delivered because codec not started: {} {}", this, cmd)
        }
      } else {
        replyToSender(NotExist)
      }
    case RetrieveFsuCmd(_) =>
      if (!isValid()) {
        sender() ! NotExist
      } else {
        sender() ! FsuVo(fsuId, fsuName.get, modelName.get, initParams.toMap)
      }
    case StartFsuCmd(_) =>
    case StopFsuCmd(_) =>
      stop()
    case GetSupportedModelsCmd(_) =>
      if (isValid()) {
        sender() ! GetSupportedModelsRsp(fsuId, registry.names.toSet)
      } else {
        replyToSender(NotExist)
      }
    case GetModelParamsCmd(_, model) =>
      if (isValid()) {
        sender() ! GetSupportedModelsRsp(fsuId, registry.paramNames(model).toSet)
      } else {
        replyToSender(NotExist)
      }
    case _: ReceiveTimeout =>
      stop()
    case x => log.info("default COMMAND: {} {}", this, x)
  }

  private def updateState: (Event => Unit) = {
    case CreateFsuEvt(name, model, params) =>
      this.fsuName = Some(name)
      this.modelName = Some(model)
      this.initParams = this.initParams ++ params
      replyToSender(Ok)
    case RenameFsuEvt(newName) =>
      this.fsuName = Some(newName)
      replyToSender(Ok)
    case ChangeModelEvt(newModel) =>
      this.modelName = Some(newModel)
      replyToSender(Ok)
    case AddParamsEvt(params) =>
      this.initParams = this.initParams ++ params
      replyToSender(Ok)
    case RemoveParamsEvt(params) =>
      this.initParams = this.initParams.filter(p => !params.contains(p._1))
      replyToSender(Ok)
    case x => log.info("UPDATE IGNORED: {} {}", this, x)
  }

  private def stop() = {
    log.info("Stopping: {}", this)
    if (fsuCodec.isDefined) {
      context.stop(fsuCodec.get)
      fsuCodec = None
    }
    context.stop(self)
  }

  private def isValid(): Boolean = {
    fsuName.isDefined && modelName.isDefined
  }

  private def createCodec(): Boolean = {
    val p = registry.create(this.modelName.get, this.initParams)
    if (p.isDefined) {
      log.warning("{}: {}", this.modelName.get, p.get)
      this.fsuCodec = Some(context.system.actorOf(p.get, s"${this.modelName.get}_${fsuId}"))
      true
    } else {
      false
    }
  }

  private def replyToSender(msg: Any) = {
    if ("deadLetters" != sender().path.name) sender() ! msg
  }
}
