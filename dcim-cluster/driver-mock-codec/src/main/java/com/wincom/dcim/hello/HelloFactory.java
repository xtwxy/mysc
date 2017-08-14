package com.wincom.dcim.hello;

import java.util.Map;

import com.wincom.dcim.driver.DriverCodecFactory;

import akka.actor.Props;
import scala.Option;

public class HelloFactory implements DriverCodecFactory {

	@Override
	public String modelName() {
		return "Hello";
	}

	@Override
	public Option<Props> create(Map<String, String> params) {
		return Option.apply(null);
	}

}
