package com.wincom.dcim.rest

import akka.actor._
import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.DateTime
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout

import akka.util.Timeout

import scala.concurrent.ExecutionContext

/**
  * Created by wangxy on 17-9-4.
  */
class AlarmService(val alarmRecords: ActorRef,
                   val system: ActorSystem,
                   val requestTimeout: Timeout) extends AlarmRoutes {
  val executionContext = system.dispatcher
}

trait AlarmRoutes extends AlarmMarshaling {
  def alarmRecords: ActorRef

  implicit def requestTimeout: Timeout

  implicit def executionContext: ExecutionContext

  def routes: Route = pathPrefix("alarm-record") {
    path(Segment) { alarmId =>
      pathEnd {
        get {
          complete(OK)
        }
      }
    }
  }
}
