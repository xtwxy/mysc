package com.wincom.dcim.message.common

import com.google.protobuf.timestamp.Timestamp
import com.wincom.dcim.message.signal.{SignalSnapshotValueVo, SignalType, SignalValueVo}
import com.wincom.dcim.message.signal.SignalType.{AI, AO, DI, DO, SI, SO}

trait SignalValue {
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
  def create(t: SignalType, v: Any): Option[SignalValueVo] = {
    (t, v) match {
      case (AI, x: Double) => Some(SignalValueVo(signalType = t, analogValue = Some(x)))
      case (AO, x: Double) => Some(SignalValueVo(signalType = t, analogValue = Some(x)))
      case (DI, x: Boolean) => Some(SignalValueVo(signalType = t, digitalValue = Some(x)))
      case (DO, x: Boolean) => Some(SignalValueVo(signalType = t, digitalValue = Some(x)))
      case (SI, x: String) => Some(SignalValueVo(signalType = t, stringValue = Some(x)))
      case (SO, x: String) => Some(SignalValueVo(signalType = t, stringValue = Some(x)))
      case _ => null
    }
  }

  def create(id: String, ts: Timestamp, t: SignalType, v: Any): Option[SignalSnapshotValueVo] = {
    (t, v) match {
      case (AI, x: Double) => Some(SignalSnapshotValueVo(id = id, ts = ts, signalType = t, analogValue = Some(x)))
      case (AO, x: Double) => Some(SignalSnapshotValueVo(id = id, ts = ts, signalType = t, analogValue = Some(x)))
      case (DI, x: Boolean) => Some(SignalSnapshotValueVo(id = id, ts = ts, signalType = t, digitalValue = Some(x)))
      case (DO, x: Boolean) => Some(SignalSnapshotValueVo(id = id, ts = ts, signalType = t, digitalValue = Some(x)))
      case (SI, x: String) => Some(SignalSnapshotValueVo(id = id, ts = ts, signalType = t, stringValue = Some(x)))
      case (SO, x: String) => Some(SignalSnapshotValueVo(id = id, ts = ts, signalType = t, stringValue = Some(x)))
      case _ => None
    }
  }
}