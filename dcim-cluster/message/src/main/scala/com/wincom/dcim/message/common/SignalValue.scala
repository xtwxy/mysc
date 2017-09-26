package com.wincom.dcim.message.common

import com.google.protobuf.timestamp.Timestamp
import com.wincom.dcim.message.signal.{SignalSnapshotValueVo, SignalType, SignalValueVo}
import com.wincom.dcim.message.signal.SignalType.{AI, AO, DI, DO, SI, SO}

trait SignalValue extends ValueObject{
  def signalType: SignalType

  def digitalValue: Option[Boolean]

  def analogValue: Option[Double]

  def stringValue: Option[String]

  def value: Option[_] = {
    signalType match {
      case AI => analogValue
      case AO => analogValue
      case DI => digitalValue
      case DO => digitalValue
      case SI => stringValue
      case SO => stringValue
      case _ => None
    }
  }
}

object SignalValue {
  def create(t: SignalType, v: Any): SignalValueVo = {
    (t, v) match {
      case (AI, x: Double) => SignalValueVo(signalType = t, analogValue = Some(x))
      case (AO, x: Double) => SignalValueVo(signalType = t, analogValue = Some(x))
      case (DI, x: Boolean) => SignalValueVo(signalType = t, digitalValue = Some(x))
      case (DO, x: Boolean) => SignalValueVo(signalType = t, digitalValue = Some(x))
      case (SI, x: String) => SignalValueVo(signalType = t, stringValue = Some(x))
      case (SO, x: String) => SignalValueVo(signalType = t, stringValue = Some(x))
      case _ => null
    }
  }

  def create(id: String, ts: Timestamp, t: SignalType, v: Any): SignalSnapshotValueVo = {
    SignalSnapshotValueVo(signalId = id, ts = ts, create(t, v))
  }
}