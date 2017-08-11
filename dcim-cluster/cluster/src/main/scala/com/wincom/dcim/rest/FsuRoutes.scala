package com.wincom.dcim.rest

import akka.actor.{ActorSystem, _}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.wincom.dcim.sharded.FsuActor._
import spray.json.DefaultJsonProtocol

import scala.concurrent.ExecutionContext
import scala.util.Try


class FsuService(val fsuShard: ActorRef, val system: ActorSystem, val requestTimeout: Timeout) extends FsuRoutes {
  val executionContext = system.dispatcher
}

trait FsuRoutes extends DefaultJsonProtocol {
  implicit val fsuFormat = jsonFormat2(Fsu)
  implicit val fsusFormat = jsonFormat1(Fsus)

  def fsuShard: ActorRef

  def routes: Route =
    create ~ update

  implicit def requestTimeout: Timeout

  implicit def executionContext: ExecutionContext

  def create = {
    path("fsu") {
      post {
        pathEnd {
          entity(as[Fsu]) { fsu =>
            onSuccess(fsuShard.ask(CreateFsu(fsu.id, fsu.name)).mapTo[Command]) {
              case _ => complete(OK)
            }
          }
        }
      }
    }
  }

  def update =
    pathPrefix("fsu" / fsuIdSegment /) { fsuId =>
      get {
        pathEnd {
          fsuShard ! CreateFsu(fsuId, "Wangxy")
          complete(OK)
        }
      } ~
        put {
          complete(OK)
        } ~
        delete {
          complete(OK)
        }
    }

  def fsuIdSegment = Segment.flatMap(id => Try(id.toInt).toOption)
}