package com.wincom.dcim.fsu.hello;

import akka.actor.Props;
import com.wincom.dcim.fsu.FsuCodecFactory;
import scala.Option;

import java.util.Map;

public class HelloFactory implements FsuCodecFactory {

	@Override
	public String modelName() {
		return "Hello";
	}

	@Override
	public Option<Props> create(Map<String, String> params) {
		return Option.apply(null);
	}

}
