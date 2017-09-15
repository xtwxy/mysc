package com.wincom.dcim.rest

import akka.actor._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern._
import akka.util.Timeout
import com.wincom.dcim.message.alarm._
import com.wincom.dcim.message.common.ResponseType._
import com.wincom.dcim.message.common._

import scala.concurrent.ExecutionContext

/**
  * Created by wangxy on 17-9-4.
  */
class AlarmService(val alarms: ActorRef,
                   val system: ActorSystem,
                   val requestTimeout: Timeout) extends AlarmRoutes {
  val executionContext = system.dispatcher
}

trait AlarmRoutes extends AlarmMarshaling {
  def alarms: ActorRef

  implicit def requestTimeout: Timeout

  implicit def executionContext: ExecutionContext

  def routes: Route = pathPrefix("alarm") {
    get {
      path(Segment) { alarmId =>
        pathEnd {
          onSuccess(alarms.ask(RetrieveAlarmCmd(alarmId)).mapTo[ValueObject]) {
            case alarm: AlarmVo =>
              complete(alarm)
            case _ =>
              complete(NotFound)
          }
        }
      }
    } ~
    post {
      path("create-alarm") {
        pathEnd {
          entity(as[CreateAlarmCmd]) { v =>
            onSuccess(alarms.ask(v).mapTo[ValueObject]) {
              case SUCCESS =>
                complete(Created)
              case ALREADY_EXISTS => complete(NotModified)
              case _ =>
                complete(NotFound)
            }
          }
        }
      } ~
      path("rename-alarm") {
        pathEnd {
          entity(as[RenameAlarmCmd]) { v =>
            onSuccess(alarms.ask(v).mapTo[ValueObject]) {
              case SUCCESS =>
                complete(Created)
              case _ =>
                complete(NotFound)
            }
          }
        }
      }
    } ~
      path("select-signal") {
        pathEnd {
          entity(as[SelectSignalCmd]) { v =>
            onSuccess(alarms.ask(v).mapTo[ValueObject]) {
              case SUCCESS =>
                complete(Created)
              case _ =>
                complete(NotFound)
            }
          }
        }
      } ~
      path("add-condition") {
        pathEnd {
          entity(as[AddConditionCmd]) { v =>
            onSuccess(alarms.ask(v).mapTo[ValueObject]) {
              case SUCCESS =>
                complete(Created)
              case _ =>
                complete(NotFound)
            }
          }
        }
      } ~
      path("remove-condition") {
        pathEnd {
          entity(as[RemoveConditionCmd]) { v =>
            onSuccess(alarms.ask(v).mapTo[ValueObject]) {
              case SUCCESS =>
                complete(Created)
              case _ =>
                complete(NotFound)
            }
          }
        }
      } ~
      path("replace-condition") {
        pathEnd {
          entity(as[ReplaceConditionCmd]) { v =>
            onSuccess(alarms.ask(v).mapTo[ValueObject]) {
              case SUCCESS =>
                complete(Created)
              case _ =>
                complete(NotFound)
            }
          }
        }
      } ~
      path("get-alarm-value") {
        pathEnd {
          entity(as[GetAlarmValueCmd]) { v =>
            onSuccess(alarms.ask(v).mapTo[ValueObject]) {
              case SUCCESS =>
                complete(Created)
              case _ =>
                complete(NotFound)
            }
          }
        }
      } ~
      path("eval-alarm-value") {
        pathEnd {
          entity(as[EvalAlarmValueCmd]) { v =>
            onSuccess(alarms.ask(v).mapTo[ValueObject]) {
              case SUCCESS =>
                complete(Created)
              case _ =>
                complete(NotFound)
            }
          }
        }
      } ~
      path("passivate-alarm") {
        pathEnd {
          entity(as[PassivateAlarmCmd]) { v =>
            onSuccess(alarms.ask(v).mapTo[ValueObject]) {
              case SUCCESS =>
                complete(Created)
              case _ =>
                complete(NotFound)
            }
          }
        }
      }
  }
}
