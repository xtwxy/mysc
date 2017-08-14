package com.wincom.dcim.driver;

import java.util.Map;

import scala.Option;

public interface DriverCodecFactory {
	String modelName();
	Option<DriverCodec> create(Map<String, String> params);
}
