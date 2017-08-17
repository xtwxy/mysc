package com.wincom.dcim.rest

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives.{path, _}
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

  def routes =
    path("fsu") {
      get {
        complete(CreateFsuCmd("id-1", "name", "model", Map("key" -> "value")))
      } ~
        post {
          entity(as[CreateFsuCmd]) { f =>
            onSuccess(fsus.ask(
              CreateFsuCmd(f.fsuId, f.name, f.model, f.params)
            ).mapTo[Command]) {
              case _ => complete(Created)
            }
          }
        }
    } ~
      path("fsu" / Segment) { fsuId =>
        get {
          pathEnd {
            onSuccess(fsus.ask(
              RetrieveFsuCmd(fsuId)
            ).mapTo[FsuVo]) { f =>
              complete(f)
            }
          }
        } ~
          delete {
            pathEnd {
               fsus ! StopFsuCmd(fsuId)
                complete(NoContent)
            }
          } ~
          post {
            path("rename") {
              pathEnd {
                entity(as[RenameFsuCmd]) { f =>
                  fsus ! f
                  complete(NoContent)
                }
              }
            } ~
              path("change-model") {
                pathEnd {
                  entity(as[ChangeModelCmd]) { f =>
                    fsus ! f
                    complete(NoContent)
                  }
                }
              } ~
              path("add-params") {
                pathEnd {
                  entity(as[AddParamsCmd]) { f =>
                    fsus ! f
                    complete(NoContent)
                  }
                }
              } ~
              path("remove-params") {
                pathEnd {
                  entity(as[RemoveParamsCmd]) { f =>
                    fsus ! f
                    complete(NoContent)
                  }
                }
              } ~
              path("get-port") {
                pathEnd {
                  entity(as[GetPortCmd]) { f =>
                    onSuccess(fsus.ask(
                      RetrieveFsuCmd(fsuId)
                    ).mapTo[ActorRef]) { f =>
                      complete(OK)
                    }
                  }
                }
              } ~
              path("send-bytes") {
                pathEnd {
                  entity(as[SendBytesCmd]) { f =>
                    fsus ! f
                    complete(NoContent)
                  }
                }
              }
          }
      }
}