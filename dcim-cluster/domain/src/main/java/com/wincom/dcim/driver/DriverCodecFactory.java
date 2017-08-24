package com.wincom.dcim.driver;

import java.util.Map;
import java.util.Set;

import scala.Option;
import akka.actor.Props;

public interface DriverCodecFactory {
	String modelName();
	Set<String> paramNames();
	Option<Props> create(Map<String, String> params);
}
