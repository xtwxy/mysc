package com.wincom.dcim.test;

import java.util.Map;

import com.wincom.dcim.driver.DriverCodecFactory;

import akka.actor.Props;
import scala.Option;

public class TestFactory implements DriverCodecFactory {

	@Override
	public String modelName() {
		return "Test";
	}

	@Override
	public Option<Props> create(Map<String, String> params) {
		return Option.apply(null);
	}

}
