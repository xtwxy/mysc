package com.wincom.dcim.fsu.test;

import akka.actor.Props;
import com.wincom.dcim.fsu.FsuCodecFactory;
import scala.Option;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TestFactory implements FsuCodecFactory {

	@Override
	public String modelName() {
		return "Test";
	}

	@Override
	public Set<String> paramNames() {
		Set<String> s = new HashSet<>();
		s.add("a");
		s.add("b");
		return s;
	}

	@Override
	public Option<Props> create(Map<String, String> params) {
		return null;
	}

}
