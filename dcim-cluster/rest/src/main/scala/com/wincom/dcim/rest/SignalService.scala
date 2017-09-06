package com.wincom.dcim.rest

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import com.wincom.dcim.domain.Signal._

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

  private def validateSignalType(t: String): Boolean = {
    t matches("AI|DI|SI|AO|DO|SO")
  }
  val tests =
    path("hello") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
      }
    }

  def routes = tests ~ pathPrefix("signal") {
    get {
      path(Segment) { signalId =>
        pathEnd {
          onSuccess(signals.ask(
            RetrieveSignalCmd(signalId)
          ).mapTo[Response]) {
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
              if(validateSignalType(v.t)) {
                signals ! v
                complete(Created)
              } else {
                complete(BadRequest)
              }
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
          path("update-funcs") {
            pathEnd {
              entity(as[UpdateFuncsCmd]) { v =>
                signals ! v
                complete(NoContent)
              }
            }
          } ~
          path("retrieve-signal") {
            pathEnd {
              entity(as[RetrieveSignalCmd]) { x =>
                onSuccess(signals.ask(x).mapTo[Response]) {
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
                onSuccess(signals.ask(x).mapTo[Response]) {
                  case v: SetValueRsp => complete(v)
                  case _ => complete(NotFound)
                }
              }
            }
          } ~
          path("get-value") {
            pathEnd {
              entity(as[GetValueCmd]) { x =>
                onSuccess(signals.ask(x).mapTo[Response]) {
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
          } ~
          path("get-supported-funcs") {
            pathEnd {
              entity(as[GetSupportedFuncsCmd]) { x =>
                onSuccess(signals.ask(x).mapTo[Response]) {
                  case v: GetSupportedFuncsRsp => complete(v)
                  case _ => complete(NotFound)
                }
              }
            }
          } ~
          path("get-func-params") {
            pathEnd {
              entity(as[GetFuncParamsCmd]) { x =>
                onSuccess(signals.ask(x).mapTo[Response]) {
                  case v: GetFuncParamsRsp => complete(v)
                  case _ => complete(NotFound)
                }
              }
            }
          }
      }
  }
}