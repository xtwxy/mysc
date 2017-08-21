package com.wincom.dcim.fsu.test;

import akka.actor.Props;
import com.wincom.dcim.fsu.FsuCodecFactory;
import scala.Option;

import java.util.Map;

public class TestFactory implements FsuCodecFactory {

	@Override
	public String modelName() {
		return "Test";
	}

	@Override
	public Option<Props> create(Map<String, String> params) {
		return null;
	}

}
