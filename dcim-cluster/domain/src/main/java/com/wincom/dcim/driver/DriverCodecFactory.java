package com.wincom.dcim.driver;

import java.util.Map;

import scala.Option;
import akka.actor.Props;

public interface DriverCodecFactory {
	String modelName();
	Option<Props> create(Map<String, String> params);
}
