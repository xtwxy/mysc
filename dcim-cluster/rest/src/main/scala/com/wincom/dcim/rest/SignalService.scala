package com.wincom.dcim.rest

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import com.wincom.dcim.domain.Signal.{CreateSignalCmd, _}

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
      entity(as[CreateSignalCmd]) { s =>
        onSuccess(signals.ask(
          CreateSignalCmd(s.signalId, s.name, s.driverId, s.key)).mapTo[Command]) {
          case _ => complete(OK)
        }
      }
    } ~
      path(Segment /) { signalId =>
        get {
          complete(OK)
        } ~
          delete {
            complete(OK)
          } ~
          post {
            path("rename") {
              pathEnd {
                entity(as[RenameSignalCmd]) { s =>
                  complete(OK)
                }
              }
            } ~
              path("select-driver") {
                pathEnd {
                  entity(as[SelectDriverCmd]) { s =>
                    complete(OK)
                  }
                }
              } ~
              path("select-key") {
                pathEnd {
                  entity(as[SelectKeyCmd]) { s =>
                    complete(OK)
                  }
                }
              } ~
              path("save-snapshot") {
                pathEnd {
                  entity(as[SaveSnapshotCmd]) { s =>
                    complete(OK)
                  }
                }
              } ~
              path("update-value") {
                pathEnd {
                  entity(as[UpdateValueCmd]) { s =>
                    complete(OK)
                  }
                }
              } ~
              path("set-value") {
                pathEnd {
                  entity(as[SetValueCmd]) { s =>
                    complete(OK)
                  }
                }
              } ~
              path("get-value") {
                pathEnd {
                  entity(as[GetValueCmd]) { s =>
                    complete(OK)
                  }
                }
              }
          }
      }
  }
}