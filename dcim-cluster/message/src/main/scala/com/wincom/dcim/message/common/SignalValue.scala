package com.wincom.dcim.message.common

import com.wincom.dcim.message.signal.SignalType
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
