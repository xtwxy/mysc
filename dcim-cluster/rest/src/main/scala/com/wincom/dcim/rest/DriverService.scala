package com.wincom.dcim.rest

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.wincom.dcim.message.common.ResponseType._
import com.wincom.dcim.message.common._
import com.wincom.dcim.message.driver._
import com.wincom.dcim.message.signal

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
          ).mapTo[ValueObject]) {
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
          pathEnd {
            entity(as[CreateDriverCmd]) { v =>
              print(v)
              onSuccess(drivers.ask(v).mapTo[ValueObject]) {
                case Response(SUCCESS, _) => complete(Created)
                case Response(ALREADY_EXISTS, _) => complete(NotModified)
                case _ => complete(NotFound)
              }
            }
          }
        } ~
          path("rename-driver") {
            pathEnd {
              entity(as[RenameDriverCmd]) { v =>
                onSuccess(drivers.ask(v).mapTo[ValueObject]) {
                  case Response(SUCCESS, _) => complete(NoContent)
                  case _ => complete(NotFound)
                }
              }
            }
          } ~
          path("change-model") {
            pathEnd {
              entity(as[ChangeModelCmd]) { v =>
                onSuccess(drivers.ask(v).mapTo[ValueObject]) {
                  case Response(SUCCESS, _) => complete(NoContent)
                  case _ => complete(NotFound)
                }
              }
            }
          } ~
          path("save-snapshot") {
            pathEnd {
              entity(as[SaveSnapshotCmd]) { v =>
                onSuccess(drivers.ask(v).mapTo[ValueObject]) {
                  case Response(SUCCESS, _) => complete(NoContent)
                  case _ => complete(NotFound)
                }
              }
            }
          } ~
          path("add-params") {
            pathEnd {
              entity(as[AddParamsCmd]) { v =>
                onSuccess(drivers.ask(v).mapTo[ValueObject]) {
                  case Response(SUCCESS, _) => complete(NoContent)
                  case _ => complete(NotFound)
                }
              }
            }
          } ~
          path("remove-params") {
            pathEnd {
              entity(as[RemoveParamsCmd]) { v =>
                onSuccess(drivers.ask(v).mapTo[ValueObject]) {
                  case Response(SUCCESS, _) => complete(NoContent)
                  case _ => complete(NotFound)
                }
              }
            }
          } ~
          path("map-signal-key-id") {
            pathEnd {
              entity(as[MapSignalKeyIdCmd]) { v =>
                onSuccess(drivers.ask(v).mapTo[ValueObject]) {
                  case Response(SUCCESS, _) => complete(NoContent)
                  case _ => complete(NotFound)
                }
              }
            }
          } ~
          path("get-signal-value") {
            pathEnd {
              entity(as[GetSignalValueCmd]) { x =>
                onSuccess(drivers.ask(x).mapTo[ValueObject]) {
                  case v: DriverSignalSnapshotVo =>
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
                onSuccess(drivers.ask(x).mapTo[ValueObject]) {
                  case v: DriverSignalSnapshotsVo =>
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
                onSuccess(drivers.ask(x).mapTo[ValueObject]) {
                  case v: signal.SetValueRsp =>
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
                onSuccess(drivers.ask(x).mapTo[ValueObject]) {
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
                onSuccess(drivers.ask(x).mapTo[ValueObject]) {
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
              onSuccess(drivers.ask(GetSupportedModelsCmd()).mapTo[ValueObject]) {
                case v: SupportedModelsVo => complete(v)
                case _ => complete(NotFound)
              }
            }
          } ~
          path("get-model-params" / Segment) { modelName =>
            pathEnd {
              onSuccess(drivers.ask(GetModelParamsCmd(modelName)).mapTo[ValueObject]) {
                case v: ModelParamsVo => complete(v)
                case _ => complete(NotFound)
              }
            }
          }
      }
  }
}
