package com.wincom.dcim.rest

import akka.actor._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.DateTime
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern._
import akka.util.Timeout
import com.wincom.dcim.domain.AlarmRecord._
import com.wincom.dcim.util.DateFormat._

import scala.concurrent.ExecutionContext
import scala.util.Try

/**
  * Created by wangxy on 17-9-4.
  */
class AlarmRecordService(val alarmRecords: ActorRef,
                         val system: ActorSystem,
                         val requestTimeout: Timeout) extends AlarmRecordRoutes {
  val executionContext = system.dispatcher
}

trait AlarmRecordRoutes extends AlarmRecordMarshaling {
  def alarmRecords: ActorRef

  implicit def requestTimeout: Timeout

  implicit def executionContext: ExecutionContext

  val TimestampSegment = Segment.flatMap(id => Try(DateTime(parseTimestamp(id).getTime)).toOption)

  def routes: Route = pathPrefix("alarm-record") {
    get {
      path(Segment / TimestampSegment) { (alarmId, begin) =>
        pathEnd {
          onSuccess(alarmRecords.ask(RetrieveAlarmRecordCmd(alarmId, begin)).mapTo[ObjectValue]) {
            case alarm: AlarmRecordVo =>
              complete(alarm)
            case _ =>
              complete(NotFound)
          }
        }
      }
    } ~ post {
      path("raise-alarm") {
        pathEnd {
          entity(as[RaiseAlarmCmd]) { v =>
            alarmRecords ! v
            complete(Created)
          }
        }
      } ~
        path("transit-alarm") {
          pathEnd {
            entity(as[TransitAlarmCmd]) { v =>
              alarmRecords ! v
              complete(NoContent)
            }
          }
        } ~
        path("end-alarm") {
          pathEnd {
            entity(as[EndAlarmCmd]) { v =>
              alarmRecords ! v
              complete(NoContent)
            }
          }
        } ~
        path("ack-alarm") {
          pathEnd {
            entity(as[AckAlarmCmd]) { v =>
              onSuccess(alarmRecords.ask(v).mapTo[ObjectValue]) {
                case Ok =>
                  complete(NoContent)
                case _ =>
                  complete(NotFound)
              }
            }
          }
        } ~
        path("mute-alarm") {
          pathEnd {
            entity(as[MuteAlarmCmd]) { v =>
              onSuccess(alarmRecords.ask(v).mapTo[ObjectValue]) {
                case Ok =>
                  complete(NoContent)
                case _ =>
                  complete(NotFound)
              }
            }
          }
        }
    }
  }
}