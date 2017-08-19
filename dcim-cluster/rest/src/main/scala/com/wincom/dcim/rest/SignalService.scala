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

  def routes = pathPrefix("signal") {
    get {
      path(Segment) { signalId =>
        pathEnd {
          onSuccess(signals.ask(
            RetrieveSignalCmd(signalId)
          ).mapTo[Command]) {
            case v: SignalVo => complete(v)
            case _ => complete(NotFound)
          }
        }
      }
    } ~
      post {
        path("create-signal") {
          pathEnd {
            entity(as[CreateSignalCmd]) { v =>
              signals ! v
              complete(Created)
            }
          }
        } ~
          path("rename-signal") {
            pathEnd {
              entity(as[RenameSignalCmd]) { v =>
                signals ! v
                complete(NoContent)
              }
            }
          } ~
          path("select-driver") {
            pathEnd {
              entity(as[SelectDriverCmd]) { v =>
                signals ! v
                complete(NoContent)
              }
            }
          } ~
          path("select-key") {
            pathEnd {
              entity(as[SelectKeyCmd]) { v =>
                signals ! v
                complete(NoContent)
              }
            }
          } ~
          path("retrieve-signal") {
            pathEnd {
              entity(as[RetrieveSignalCmd]) { x =>
                onSuccess(signals.ask(x).mapTo[Command]) {
                  case v: SignalVo => complete(v)
                  case _ => complete(NotFound)
                }
              }
            }
          } ~
          path("save-snapshot") {
            pathEnd {
              entity(as[SaveSnapshotCmd]) { v =>
                signals ! v
                complete(NoContent)
              }
            }
          } ~
          path("update-value") {
            pathEnd {
              entity(as[UpdateValueCmd]) { v =>
                signals ! v
                complete(NoContent)
              }
            }
          } ~
          path("set-value") {
            pathEnd {
              entity(as[SetValueCmd]) { x =>
                onSuccess(signals.ask(x).mapTo[Command]) {
                  case v: Ok => complete(v)
                  case _ => complete(NotFound)
                }
              }
            }
          } ~
          path("get-value") {
            pathEnd {
              entity(as[GetValueCmd]) { x =>
                onSuccess(signals.ask(x).mapTo[Command]) {
                  case v: SignalValueVo => complete(v)
                  case _ => complete(NotFound)
                }
              }
            }
          } ~
          path("start-signal") {
            pathEnd {
              entity(as[StartSignalCmd]) { v =>
                signals ! v
                complete(NoContent)
              }
            }
          } ~
          path("stop-signal") {
            pathEnd {
              entity(as[StopSignalCmd]) { v =>
                signals ! v
                complete(NoContent)
              }
            }
          }
      }
  }
}