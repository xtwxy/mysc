package com.wincom.dcim.rest

import akka.http.scaladsl.model.DateTime
import spray.json.{JsBoolean, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}
/**
  * Created by wangxy on 17-8-15.
  */
object AnyValFormat extends RootJsonFormat[AnyVal] {
  override def write(obj: AnyVal): JsObject = {
    obj match {
      case x: Boolean =>
        JsObject(("type", JsString("Bool")), ("value", JsString(x.toString)))
      case x: Double =>
        JsObject(("type", JsString("Double")), ("value", JsString(x.toString)))
      case x =>
        JsObject(("type", JsString("Unknown")), ("value", JsString(x.toString)))
    }
  }

  override def read(json: JsValue): AnyVal = {
    json match {
      case JsObject(fields) =>
        fields("type") match {
          case JsString("Bool") =>
            fields("value") match {
              case JsString(x) => x.toBoolean
              case JsBoolean(x) => x
              case x =>
                throw new IllegalArgumentException("Unknown type: '%s'".format(x))
            }
          case JsString("Double") =>
            fields("value") match {
              case JsString(x) => x.toDouble
              case JsNumber(x) => x.doubleValue
              case x =>
                throw new IllegalArgumentException("Unknown type: '%s'".format(x))
            }
          case x =>
            throw new IllegalArgumentException("Unknown type: '%s'".format(x))
        }
      case x =>
        throw new IllegalArgumentException("Unknown JsValue: '%s'".format(x))
    }
  }
}

object DateTimeFormat extends RootJsonFormat[DateTime] {
  override def read(json: JsValue): DateTime = {
    json match {
      case JsString(value) =>
        DateTime.fromIsoDateTimeString(value).get
      case x =>
        throw new IllegalArgumentException("Unknown JsValue: '%s'".format(x))
    }
  }

  override def write(obj: DateTime): JsValue = {
    JsString(Symbol(obj.toIsoDateTimeString()))
  }
}