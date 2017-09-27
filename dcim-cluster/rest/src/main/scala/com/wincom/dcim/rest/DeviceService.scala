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
import com.wincom.dcim.message.device._

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.language.postfixOps

/**
  * Created by wangxy on 17-8-15.
  */
class DeviceService(val devices: ActorRef,
                    val system: ActorSystem,
                    val requestTimeout: Timeout
                   ) extends DeviceRoutes {
  val executionContext: ExecutionContextExecutor = system.dispatcher
}

trait DeviceRoutes extends DeviceMarshaling {
  def devices: ActorRef

  implicit def requestTimeout: Timeout

  implicit def executionContext: ExecutionContext

  def routes: Route = pathPrefix("device") {
      post {
        path("create-device") {
          pathEnd {
            entity(as[CreateDeviceCmd]) { v =>
              print(v)
              onSuccess(devices.ask(v).mapTo[ValueObject]) {
                case Response(SUCCESS, _) => complete(Created)
                case Response(ALREADY_EXISTS, _) => complete(NotModified)
                case _ => complete(NotFound)
              }
            }
          }
        } ~
          path("rename-device") {
            pathEnd {
              entity(as[RenameDeviceCmd]) { v =>
                onSuccess(devices.ask(v).mapTo[ValueObject]) {
                  case Response(SUCCESS, _) => complete(NoContent)
                  case _ => complete(NotFound)
                }
              }
            }
          } ~
          path("add-alarm") {
            pathEnd {
              entity(as[AddAlarmCmd]) { v =>
                onSuccess(devices.ask(v).mapTo[ValueObject]) {
                  case Response(SUCCESS, _) => complete(NoContent)
                  case _ => complete(NotFound)
                }
              }
            }
          } ~
          path("add-child") {
            pathEnd {
              entity(as[AddChildCmd]) { v =>
                onSuccess(devices.ask(v).mapTo[ValueObject]) {
                  case Response(SUCCESS, _) => complete(NoContent)
                  case _ => complete(NotFound)
                }
              }
            }
          } ~
          path("add-signal") {
            pathEnd {
              entity(as[AddSignalCmd]) { v =>
                onSuccess(devices.ask(v).mapTo[ValueObject]) {
                  case Response(SUCCESS, _) => complete(NoContent)
                  case _ => complete(NotFound)
                }
              }
            }
          } ~
          path("change-device-type") {
            pathEnd {
              entity(as[ChangeDeviceTypeCmd]) { v =>
                onSuccess(devices.ask(v).mapTo[ValueObject]) {
                  case Response(SUCCESS, _) => complete(NoContent)
                  case _ => complete(NotFound)
                }
              }
            }
          } ~
          path("change-property-tag-code") {
            pathEnd {
              entity(as[ChangePropertyTagCodeCmd]) { v =>
                onSuccess(devices.ask(v).mapTo[ValueObject]) {
                  case Response(SUCCESS, _) => complete(NoContent)
                  case _ => complete(NotFound)
                }
              }
            }
          } ~
          path("change-vendor-model") {
            pathEnd {
              entity(as[ChangeVendorModelCmd]) { v =>
                onSuccess(devices.ask(v).mapTo[ValueObject]) {
                  case Response(SUCCESS, _) => complete(NoContent)
                  case _ => complete(NotFound)
                }
              }
            }
          } ~
          path("remove-alarm") {
            pathEnd {
              entity(as[RemoveAlarmCmd]) { v =>
                onSuccess(devices.ask(v).mapTo[ValueObject]) {
                  case Response(SUCCESS, _) => complete(NoContent)
                  case _ => complete(NotFound)
                }
              }
            }
          } ~
          path("remove-child") {
            pathEnd {
              entity(as[RemoveChildCmd]) { v =>
                onSuccess(devices.ask(v).mapTo[ValueObject]) {
                  case Response(SUCCESS, _) => complete(NoContent)
                  case _ => complete(NotFound)
                }
              }
            }
          } ~
          path("remove-signal") {
            pathEnd {
              entity(as[RemoveSignalCmd]) { v =>
                onSuccess(devices.ask(v).mapTo[ValueObject]) {
                  case Response(SUCCESS, _) => complete(NoContent)
                  case _ => complete(NotFound)
                }
              }
            }
          } ~
          path("retrieve-device") {
            pathEnd {
              entity(as[RetrieveDeviceCmd]) { v =>
                onSuccess(devices.ask(v).mapTo[ValueObject]) {
                  case Response(SUCCESS, _) => complete(NoContent)
                  case _ => complete(NotFound)
                }
              }
            }
          }
      }
  }
}
