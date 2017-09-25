package com.wincom.dcim.domain

import akka.actor.{ActorRef, Props, ReceiveTimeout}
import akka.event.Logging
import akka.pattern.ask
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import akka.util.Timeout
import akka.util.Timeout.durationToTimeout
import com.wincom.dcim.fsu.FsuCodecRegistry
import com.wincom.dcim.message.common.ResponseType._
import com.wincom.dcim.message.common._
import com.wincom.dcim.message.driver.SendBytesCmd
import com.wincom.dcim.message.fsu._

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
    case CreateFsuCmd(_, user, name, model, params) =>
      if(isValid) {
        sender() ! Response(ALREADY_EXISTS, None)
      } else {
        persist(CreateFsuEvt(user, name, model, params))(updateState)
      }
    case RenameFsuCmd(_, user, newName) =>
      if (isValid()) {
        persist(RenameFsuEvt(user, newName))(updateState)
      } else {
        sender() ! Response(NOT_EXIST, None)
      }
    case ChangeModelCmd(_, user, newModel) =>
      if (isValid()) {
        persist(ChangeModelEvt(user, newModel))(updateState)
      } else {
        sender() ! Response(NOT_EXIST, None)
      }
    case SaveSnapshotCmd =>
      if (isValid()) {
        saveSnapshot(FsuPo(fsuName.get, modelName.get, initParams.toMap))
        sender() ! Response(SUCCESS, None)
      } else {
        sender() ! Response(NOT_EXIST, None)
      }
    case AddParamsCmd(_, user, params) =>
      if (isValid()) {
        persist(AddParamsEvt(user, params))(updateState)
      } else {
        sender() ! Response(NOT_EXIST, None)
      }
    case RemoveParamsCmd(_, user, params) =>
      if (isValid()) {
        persist(RemoveParamsEvt(user, params))(updateState)
      } else {
        sender() ! Response(NOT_EXIST, None)
      }
    case cmd: GetPortCmd =>
      val theSender = sender()
      if (!isValid()) {
        theSender ! Response(NOT_EXIST, None)
      } else {
        this.fsuCodec.get.ask(cmd).mapTo[ActorRef].onComplete {
          case f: Success[ActorRef] =>
            theSender ! f.value
          case _ =>
            theSender ! Response(NOT_AVAILABLE, None)
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
        sender() ! Response(NOT_EXIST, None)
      }
    case RetrieveFsuCmd(_, user) =>
      if (!isValid()) {
        sender() ! Response(NOT_EXIST, None)
      } else {
        sender() ! FsuVo(fsuId, fsuName.get, modelName.get, initParams.toMap)
      }
    case StartFsuCmd(_, user) =>
    case StopFsuCmd(_, user) =>
      stop()
    case _: ReceiveTimeout =>
      stop()
    case x => log.info("default COMMAND: {} {}", this, x)
  }

  private def updateState: (Event => Unit) = {
    case CreateFsuEvt(user, name, model, params) =>
      this.fsuName = Some(name)
      this.modelName = Some(model)
      this.initParams = this.initParams ++ params
      replyToSender(Response(SUCCESS, None))
    case RenameFsuEvt(user, newName) =>
      this.fsuName = Some(newName)
      replyToSender(Response(SUCCESS, None))
    case ChangeModelEvt(user, newModel) =>
      this.modelName = Some(newModel)
      replyToSender(Response(SUCCESS, None))
    case AddParamsEvt(user, params) =>
      this.initParams = this.initParams ++ params
      replyToSender(Response(SUCCESS, None))
    case RemoveParamsEvt(user, params) =>
      this.initParams = this.initParams.filter(p => !params.contains(p._1))
      replyToSender(Response(SUCCESS, None))
    case x => log.info("UPDATE IGNORED: {} {} {}", this, x.getClass.getName, x)
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
