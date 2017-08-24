package com.wincom.dcim.rest

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.wincom.dcim.domain.Driver._

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.language.postfixOps

/**
  * Created by wangxy on 17-8-15.
  */
class DriverService(val drivers: ActorRef,
                    val system: ActorSystem,
                    val requestTimeout: Timeout
                   ) extends DriverRoutes {
  val executionContext: ExecutionContextExecutor = system.dispatcher
}

trait DriverRoutes extends DriverMarshaling {
  def drivers: ActorRef

  implicit def requestTimeout: Timeout

  implicit def executionContext: ExecutionContext

  def routes: Route = pathPrefix("driver") {
    get {
      path(Segment) { driverId =>
        pathEnd {
          onSuccess(drivers.ask(
            RetrieveDriverCmd(driverId)
          ).mapTo[Command]) {
            case v: DriverVo =>
              complete(v)
            case _ =>
              complete(NotFound)
          }
        }
      }
    } ~
      post {
        path("create-driver") {
          entity(as[CreateDriverCmd]) { v =>
            drivers ! v
            complete(Created)
          }
        } ~
          path("rename-driver") {
            pathEnd {
              entity(as[RenameDriverCmd]) { v =>
                drivers ! v
                complete(NoContent)
              }
            }
          } ~
          path("change-model") {
            pathEnd {
              entity(as[ChangeModelCmd]) { v =>
                drivers ! v
                complete(NoContent)
              }
            }
          } ~
          path("save-snapshot") {
            pathEnd {
              entity(as[SaveSnapshotCmd]) { v =>
                drivers ! v
                complete(NoContent)
              }
            }
          } ~
          path("add-params") {
            pathEnd {
              entity(as[AddParamsCmd]) { v =>
                drivers ! v
                complete(NoContent)
              }
            }
          } ~
          path("remove-params") {
            pathEnd {
              entity(as[RemoveParamsCmd]) { v =>
                drivers ! v
                complete(NoContent)
              }
            }
          } ~
          path("map-signal-key-id") {
            pathEnd {
              entity(as[MapSignalKeyIdCmd]) { v =>
                drivers ! v
                complete(NoContent)
              }
            }
          } ~
          path("get-signal-value") {
            pathEnd {
              entity(as[GetSignalValueCmd]) { x =>
                onSuccess(drivers.ask(x).mapTo[Command]) {
                  case v: SignalValueVo =>
                    complete(v)
                  case _ =>
                    complete(NotFound)
                }
              }
            }
          } ~
          path("get-signal-values") {
            pathEnd {
              entity(as[GetSignalValuesCmd]) { x =>
                onSuccess(drivers.ask(x).mapTo[Command]) {
                  case v: SignalValuesVo =>
                    complete(v)
                  case _ =>
                    complete(NotFound)
                }
              }
            }
          } ~
          path("set-signal-value") {
            pathEnd {
              entity(as[SetSignalValueCmd]) { x =>
                onSuccess(drivers.ask(x).mapTo[Command]) {
                  case v: SetSignalValueRsp =>
                    complete(v)
                  case _ =>
                    complete(NotFound)
                }
              }
            }
          } ~
          path("set-signal-values") {
            pathEnd {
              entity(as[SetSignalValuesCmd]) { x =>
                onSuccess(drivers.ask(x).mapTo[Command]) {
                  case v: SetSignalValuesRsp =>
                    complete(v)
                  case _ =>
                    complete(NotFound)
                }
              }
            }
          } ~
          path("update-signal-values") {
            pathEnd {
              entity(as[UpdateSignalValuesCmd]) { v =>
                drivers ! v
                complete(NoContent)
              }
            }
          } ~
          path("send-bytes") {
            pathEnd {
              entity(as[SendBytesCmd]) { v =>
                drivers ! v
                complete(NoContent)
              }
            }
          } ~
          path("retrieve-driver") {
            pathEnd {
              entity(as[RetrieveDriverCmd]) { x =>
                onSuccess(drivers.ask(x).mapTo[Command]) {
                  case v: DriverVo =>
                    complete(v)
                  case _ =>
                    complete(NotFound)
                }
              }
            }
          } ~
          path("start-driver") {
            pathEnd {
              entity(as[StartDriverCmd]) { v =>
                drivers ! v
                complete(NoContent)
              }
            }
          } ~
          path("stop-driver") {
            pathEnd {
              entity(as[StopDriverCmd]) { v =>
                drivers ! v
                complete(NoContent)
              }
            }
          } ~
          path("get-supported-models") {
            pathEnd {
              entity(as[GetSupportedModelsCmd]) { x =>
                onSuccess(drivers.ask(x).mapTo[Command]) {
                  case v: GetSupportedModelsRsp => complete(v)
                  case _ => complete(NotFound)
                }
              }
            }
          } ~
          path("get-model-params") {
            pathEnd {
              entity(as[GetModelParamsCmd]) { x =>
                onSuccess(drivers.ask(x).mapTo[Command]) {
                  case v: GetModelParamsRsp => complete(v)
                  case _ => complete(NotFound)
                }
              }
            }
          }
      }
  }
}