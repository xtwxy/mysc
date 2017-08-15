package com.wincom.dcim.rest

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.pattern.ask
import akka.util.Timeout
import com.wincom.dcim.domain.Driver.{Command, CreateDriverCmd, DriverVo}

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

/**
  * Created by wangxy on 17-8-15.
  */
class DriverService(val drivers: ActorRef,
                    val system: ActorSystem,
                    val requestTimeout: Timeout
                   ) extends DriverRoutes {
  val executionContext = system.dispatcher
}

trait DriverRoutes extends DriverMarshaling {
  def drivers: ActorRef
  implicit def requestTimeout: Timeout
  implicit def executionContext: ExecutionContext

  def routes = path("driver") {
    post {
      pathEnd {
        entity(as[DriverVo]) { d =>
          onSuccess(drivers.ask(
            CreateDriverCmd(d.driverId, d.name, d.model, d.initParams, d.signalIdMap)).mapTo[Command]
          ) {
            case _ => complete(OK)
          }
        }
      }
    } ~
    pathPrefix("" / Segment) {
      driverId =>
        pathEnd {
          get {
            complete(driverId)
          } ~
          put {
            entity(as[DriverVo]) { d =>
              complete(d)
            }
          } ~
          delete {
            complete(OK)
          }
        }
    }
  }
}