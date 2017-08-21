package com.wincom.dcim.fsu;

import java.util.Map;

import akka.actor.Props;
import scala.Option;

public interface FsuCodecFactory {
	String modelName();
	Option<Props> create(Map<String, String> params);
}
