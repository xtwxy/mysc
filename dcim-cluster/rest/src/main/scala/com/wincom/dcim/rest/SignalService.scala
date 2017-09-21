package com.wincom.dcim.rest

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import com.wincom.dcim.message.common.ResponseType._
import com.wincom.dcim.message.common._
import com.wincom.dcim.message.signal._

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
          ).mapTo[ValueObject]) {
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
              onSuccess(signals.ask(v).mapTo[ValueObject]) {
                case Response(SUCCESS, _) => complete(Created)
                case Response(ALREADY_EXISTS, _) => complete(NotModified)
                case _ => complete(NotFound)
              }
            }
          }
        } ~
          path("rename-signal") {
            pathEnd {
              entity(as[RenameSignalCmd]) { v =>
                onSuccess(signals.ask(v).mapTo[ValueObject]) {
                  case Response(SUCCESS, _) => complete(NoContent)
                  case _ => complete(NotFound)
                }
              }
            }
          } ~
          path("select-driver") {
            pathEnd {
              entity(as[SelectDriverCmd]) { v =>
                onSuccess(signals.ask(v).mapTo[ValueObject]) {
                  case Response(SUCCESS, _) => complete(NoContent)
                  case _ => complete(NotFound)
                }
              }
            }
          } ~
          path("select-key") {
            pathEnd {
              entity(as[SelectKeyCmd]) { v =>
                onSuccess(signals.ask(v).mapTo[ValueObject]) {
                  case Response(SUCCESS, _) => complete(NoContent)
                  case _ => complete(NotFound)
                }
              }
            }
          } ~
          path("update-funcs") {
            pathEnd {
              entity(as[UpdateFuncsCmd]) { v =>
                onSuccess(signals.ask(v).mapTo[ValueObject]) {
                  case Response(SUCCESS, _) => complete(NoContent)
                  case _ => complete(NotFound)
                }
              }
            }
          } ~
          path("retrieve-signal") {
            pathEnd {
              entity(as[RetrieveSignalCmd]) { x =>
                onSuccess(signals.ask(x).mapTo[ValueObject]) {
                  case v: SignalVo => complete(v)
                  case _ => complete(NotFound)
                }
              }
            }
          } ~
          path("save-snapshot") {
            pathEnd {
              entity(as[SaveSnapshotCmd]) { v =>
                onSuccess(signals.ask(v).mapTo[ValueObject]) {
                  case Response(SUCCESS, _) =>
                    complete(NoContent)
                  case _ =>
                    complete(NotFound)
                }
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
                onSuccess(signals.ask(x).mapTo[ValueObject]) {
                  case v: SetValueRsp => complete(v)
                  case _ => complete(NotFound)
                }
              }
            }
          } ~
          path("get-value") {
            pathEnd {
              entity(as[GetValueCmd]) { x =>
                onSuccess(signals.ask(x).mapTo[ValueObject]) {
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
              onSuccess(signals.ask(GetSupportedFuncsCmd()).mapTo[ValueObject]) {
                case v: SupportedFuncsVo => complete(v)
                case _ => complete(NotFound)
              }
            }
          } ~
          path("get-func-params" / Segment) { funcName =>
            pathEnd {
              onSuccess(signals.ask(GetFuncParamsCmd(funcName)).mapTo[ValueObject]) {
                case v: FuncParamsVo => complete(v)
                case _ => complete(NotFound)
              }
            }
          }
      }
  }
}
