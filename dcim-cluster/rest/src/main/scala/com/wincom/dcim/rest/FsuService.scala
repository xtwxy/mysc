package com.wincom.dcim.rest

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives.{path, _}
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.wincom.dcim.message.common._
import com.wincom.dcim.message.common.ResponseType._
import com.wincom.dcim.message.driver.SendBytesCmd
import com.wincom.dcim.message.fsu._

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

/**
  * Created by wangxy on 17-8-16.
  */
class FsuService(val fsus: ActorRef,
                 val system: ActorSystem,
                 val requestTimeout: Timeout
                ) extends FsuRoutes {
  val executionContext: ExecutionContextExecutor = system.dispatcher
}

trait FsuRoutes extends FsuMarshaling {
  def fsus: ActorRef

  implicit def requestTimeout: Timeout

  implicit def executionContext: ExecutionContext

  def routes: Route =
    pathPrefix("fsu") {
      get {
        path(Segment) { fsuId =>
          pathEnd {
            onSuccess(fsus.ask(
              RetrieveFsuCmd(fsuId)
            ).mapTo[ValueObject]) {
              case v: FsuVo =>
                complete(v)
              case _ =>
                complete(NotFound)
            }
          }
        }
      } ~
        post {
          path("create-fsu") {
            entity(as[CreateFsuCmd]) { v =>
              onSuccess(fsus.ask(v).mapTo[ValueObject]) {
                case SUCCESS => complete(NoContent)
                case _ => complete(NotFound)
              }
            }
          } ~
            path("rename-fsu") {
              pathEnd {
                entity(as[RenameFsuCmd]) { v =>
                  onSuccess(fsus.ask(v).mapTo[ValueObject]) {
                    case SUCCESS => complete(NoContent)
                    case _ => complete(NotFound)
                  }
                }
              }
            } ~
            path("change-model") {
              pathEnd {
                entity(as[ChangeModelCmd]) { v =>
                  onSuccess(fsus.ask(v).mapTo[ValueObject]) {
                    case SUCCESS => complete(NoContent)
                    case _ => complete(NotFound)
                  }
                }
              }
            } ~
            path("add-params") {
              pathEnd {
                entity(as[AddParamsCmd]) { v =>
                  onSuccess(fsus.ask(v).mapTo[ValueObject]) {
                    case SUCCESS => complete(NoContent)
                    case _ => complete(NotFound)
                  }
                }
              }
            } ~
            path("remove-params") {
              pathEnd {
                entity(as[RemoveParamsCmd]) { v =>
                  onSuccess(fsus.ask(v).mapTo[ValueObject]) {
                    case SUCCESS => complete(NoContent)
                    case _ => complete(NotFound)
                  }
                }
              }
            } ~
            path("get-port") {
              pathEnd {
                entity(as[GetPortCmd]) { f =>
                  onSuccess(fsus.ask(f).mapTo[Any]) {
                    case v: ActorRef =>
                      complete(v.toString)
                    case _ =>
                      complete(NotFound)
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
            } ~
            path("retrieve-fsu") {
              pathEnd {
                entity(as[RetrieveFsuCmd]) { f =>
                  onSuccess(fsus.ask(f).mapTo[ValueObject]) {
                    case v: FsuVo =>
                      complete(v)
                    case _ =>
                      complete(NotFound)
                  }
                }
              }
            } ~
            path("start-fsu") {
              pathEnd {
                entity(as[StartFsuCmd]) { f =>
                  fsus ! f
                  complete(NoContent)
                }
              }
            } ~
            path("stop-fsu") {
              pathEnd {
                entity(as[StopFsuCmd]) { f =>
                  fsus ! f
                  complete(NoContent)
                }
              }
            } ~
            path("get-supported-models") {
              pathEnd {
                entity(as[GetSupportedModelsCmd]) { x =>
                  onSuccess(fsus.ask(x).mapTo[ValueObject]) {
                    case v: SupportedModelsVo => complete(v)
                    case _ => complete(NotFound)
                  }
                }
              }
            } ~
            path("get-model-params") {
              pathEnd {
                entity(as[GetModelParamsCmd]) { x =>
                  onSuccess(fsus.ask(x).mapTo[ValueObject]) {
                    case v: ModelParamsVo => complete(v)
                    case _ => complete(NotFound)
                  }
                }
              }
            }
        }
    }
}