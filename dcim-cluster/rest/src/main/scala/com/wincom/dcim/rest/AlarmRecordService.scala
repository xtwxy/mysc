package com.wincom.dcim.rest

import akka.actor._
import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.DateTime
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.wincom.dcim.domain.AlarmRecord.{AlarmRecordVo, Event, RaiseAlarmEvt, TransitAlarmEvt}
import com.wincom.dcim.domain.Signal.SignalValueVo

import scala.collection.mutable
import scala.concurrent.ExecutionContext

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

  def routes: Route = pathPrefix("alarm-record") {
    path(Segment) { alarmId=>
      pathEnd {
        get {
          var trans: mutable.Seq[Event] = mutable.ArraySeq()
          trans :+= RaiseAlarmEvt(DateTime.now, "alarm", 1, SignalValueVo("sig-2000", DateTime.now, 32), "alarm")
          trans :+= TransitAlarmEvt(DateTime.now, 2, SignalValueVo("sig-2000", DateTime.now, 36), "alarm")
          val alarm = AlarmRecordVo(alarmId,
            DateTime.now,
            "temperature-alarm",
            1,
            "sig-2000",
            "alarm",
            true,
            DateTime.now,
            None,
            None,
            None,
            None,
            None,
            None,
            trans,
            None
          )
          complete(alarm)
        }
      }
    }
  }
}