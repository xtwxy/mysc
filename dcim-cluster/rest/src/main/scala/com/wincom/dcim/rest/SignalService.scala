package com.wincom.dcim.rest

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.pattern.ask
import akka.util.Timeout
import com.wincom.dcim.domain.Signal.{CreateSignalCmd, SignalVo, _}

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

/**
  * Created by wangxy on 17-8-16.
  */
class SignalService(val signals: ActorRef,
                    val system: ActorSystem,
                    val requestTimeout: Timeout
                   ) extends SignalRoutes {
  val executionContext = system.dispatcher
}

trait SignalRoutes extends SignalMarshaling {
  def signals: ActorRef

  implicit def requestTimeout: Timeout

  implicit def executionContext: ExecutionContext

  def routes = path("signal" /) {
    post {
      entity(as[SignalVo]) { s =>
        onSuccess(signals.ask(
          CreateSignalCmd(s.signalId, s.name, s.driverId, s.key)).mapTo[Command]) {
          case _ => complete(OK)
        }
      }
    }
  }
}