package com.wincom.dcim.test;

import java.util.Map;

import com.wincom.dcim.driver.DriverCodec;
import com.wincom.dcim.driver.DriverCodecFactory;

import scala.Option;

public class TestFactory implements DriverCodecFactory {

	@Override
	public String modelName() {
		return "Test";
	}

	@Override
	public Option<DriverCodec> create(Map<String, String> params) {
		return null;
	}

}
