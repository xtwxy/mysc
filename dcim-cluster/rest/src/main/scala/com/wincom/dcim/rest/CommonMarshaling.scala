package com.wincom.dcim.rest

import com.google.protobuf.ByteString
import com.google.protobuf.timestamp.Timestamp
import com.wincom.dcim.message.alarm.AlarmLevel
import com.wincom.dcim.message.common.{ParamType, ResponseType}
import com.wincom.dcim.message.signal.SignalType
import com.wincom.dcim.util.DateFormat
import spray.json.{DefaultJsonProtocol, JsBoolean, JsNumber, JsObject, JsString, JsValue, JsonFormat, RootJsonFormat}
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

object DateTimeJsonFormat extends RootJsonFormat[Timestamp] {
  override def read(json: JsValue): Timestamp = {
    json match {
      case JsString(value) =>
        DateFormat.parseTimestamp(value)
      case x =>
        throw new IllegalArgumentException("Unknown JsValue: '%s'".format(x))
    }
  }

  override def write(obj: Timestamp): JsValue = {
    JsString(DateFormat.formatTimestamp(obj))
  }
}

object ResponseTypeJsonFormat extends RootJsonFormat[ResponseType] {
  override def read(json: JsValue): ResponseType = {
    json match {
      case JsString(value) =>
        val v = ResponseType.fromName(value)
        if(v.isDefined) {
          v.get
        } else {
          throw new IllegalArgumentException("Unknown JsValue: '%s'".format(value))
        }
      case x =>
        throw new IllegalArgumentException("Unknown JsValue: '%s'".format(x))
    }
  }

  override def write(obj: ResponseType): JsValue = {
    JsString(obj.name)
  }
}

object AlarmLevelJsonFormat extends RootJsonFormat[AlarmLevel] {
  override def write(obj: AlarmLevel): JsValue = {
   JsString(obj.name)
  }

  override def read(json: JsValue): AlarmLevel = {
    json match {
      case JsString(value) =>
        val v = AlarmLevel.fromName(value)
        if(v.isDefined) {
          v.get
        } else {
          throw new IllegalArgumentException("Unknown JsValue: '%s'".format(value))
        }
      case x =>
        throw new IllegalArgumentException("Unknown JsValue: '%s'".format(x))
    }
  }
}

object SignalTypeJsonFormat extends RootJsonFormat[SignalType] {
  override def write(obj: SignalType): JsValue = {
    JsString(obj.name)
  }

  override def read(json: JsValue): SignalType = {
    json match {
      case JsString(value) =>
        val v = SignalType.fromName(value)
        if(v.isDefined) {
          v.get
        } else {
          throw new IllegalArgumentException("Unknown JsValue: '%s'".format(value))
        }
      case x =>
        throw new IllegalArgumentException("Unknown JsValue: '%s'".format(x))
    }
  }
}

object ByteStringJsonFormat extends RootJsonFormat[ByteString] with DefaultJsonProtocol {

  override def write(obj: ByteString): JsValue = {
    listFormat[Byte].write(obj.toByteArray.toList)
  }

  override def read(json: JsValue): ByteString = {
    val bytes = listFormat[Byte].read(json)
    ByteString.copyFrom(bytes.toArray)
  }
}

object ParamTypeJsonFormat extends RootJsonFormat[ParamType] {
  override def write(obj: ParamType): JsValue = {
    JsString(obj.name)
  }

  override def read(json: JsValue): ParamType = {
    json match {
      case JsString(value) =>
        val v = ParamType.fromName(value)
        if(v.isDefined) {
          v.get
        } else {
          throw new IllegalArgumentException("Unknown JsValue: '%s'".format(value))
        }
      case x =>
        throw new IllegalArgumentException("Unknown JsValue: '%s'".format(x))
    }
  }
}
