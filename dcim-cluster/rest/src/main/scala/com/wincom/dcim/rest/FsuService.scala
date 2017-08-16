package com.wincom.dcim.rest

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives.path
import akka.pattern.ask
import akka.util.Timeout
import com.wincom.dcim.domain.Fsu._

import scala.concurrent.ExecutionContext

/**
  * Created by wangxy on 17-8-16.
  */
class FsuService(val fsus: ActorRef,
                    val system: ActorSystem,
                    val requestTimeout: Timeout
                   ) extends FsuRoutes {
  val executionContext = system.dispatcher
}

trait FsuRoutes extends FsuMarshaling {
  def fsus: ActorRef

  implicit def requestTimeout: Timeout

  implicit def executionContext: ExecutionContext

  def routes = path("fsu" /) {
    post {
      entity(as[CreateFsuCmd]) { f =>
        onSuccess(fsus.ask(
          CreateFsuCmd(f.fsuId, f.name, f.model, f.params)
        ).mapTo[Command]) {
          case _ => complete(OK)
        }
      }
    } ~
    path(Segment /) { fsuId =>
      get {
        complete(OK)
      } ~
      delete {
        complete(OK)
      } ~
      post {
        path("rename") {
          pathEnd {
            entity(as[RenameFsuCmd]) { f =>
              complete(OK)
            }
          }
        } ~
          path("change-model") {
            pathEnd {
              entity(as[ChangeModelCmd]) { f =>
                complete(OK)
              }
            }
          } ~
          path("add-params") {
            pathEnd {
              entity(as[AddParamsCmd]) { f =>
                complete(OK)
              }
            }
          } ~
          path("remove-params") {
            pathEnd {
              entity(as[RemoveParamsCmd]) { f =>
                complete(OK)
              }
            }
          } ~
          path("get-port") {
            pathEnd {
              entity(as[GetPortCmd]) { f =>
                complete(OK)
              }
            }
          } ~
          path("send-bytes") {
            pathEnd {
              entity(as[SendBytesCmd]) { f =>
                complete(OK)
              }
            }
          }
      }
    }
  }
}