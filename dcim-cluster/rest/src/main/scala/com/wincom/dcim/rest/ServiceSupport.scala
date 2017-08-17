package com.wincom.dcim.rest

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.{complete, get, path}
import akka.http.scaladsl.server.RouteConcatenation._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.Config
import com.wincom.dcim.domain.Settings

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Created by wangxy on 17-8-15.
  */
trait ServiceSupport extends RequestTimeout {
  def startService(fsus: ActorRef,
                   drivers: ActorRef,
                   signals: ActorRef
                  )(implicit system: ActorSystem) = {
    val config = system.settings.config
    val settings = Settings(system)
    val host = settings.http.host
    val port = settings.http.port

    implicit val ec = system.dispatcher  //bindAndHandle requires an implicit ExecutionContext

    val fsuApi = new FsuService(fsus, system, requestTimeout(config)).routes // the RestApi provides a Route
    val driverApi = new DriverService(drivers, system, requestTimeout(config)).routes // the RestApi provides a Route
    val signalApi = new SignalService(signals, system, requestTimeout(config)).routes // the RestApi provides a Route
    val api = fsuApi ~ driverApi ~ signalApi
    implicit val materializer = ActorMaterializer()
    val bindingFuture: Future[ServerBinding] =
      Http().bindAndHandle(api, host, port)

    val log =  Logging(system.eventStream, "dcim-cluster")

    bindingFuture.onComplete {
      case s: Success[ServerBinding] =>
        log.info(s"dcim clust API bound to ${s.value.localAddress} ")
      case f: Failure[ServerBinding] =>
        log.error(f.exception, "Failed to bind to {}:{}!", host, port)
        system.terminate()
    }
  }
}

trait RequestTimeout {
  import scala.concurrent.duration._
  def requestTimeout(config: Config): Timeout = {
    val t = config.getString("akka.http.server.request-timeout")
    val d = Duration(t)
    FiniteDuration(d.length, d.unit)
  }
}

