package com.wincom.dcim.hello;

import java.util.Map;

import com.wincom.dcim.driver.DriverCodec;
import com.wincom.dcim.driver.DriverCodecFactory;

import scala.Option;

public class HelloFactory implements DriverCodecFactory {

	@Override
	public String modelName() {
		return "Hello";
	}

	@Override
	public Option<DriverCodec> create(Map<String, String> params) {
		return Option.apply(null);
	}

}
