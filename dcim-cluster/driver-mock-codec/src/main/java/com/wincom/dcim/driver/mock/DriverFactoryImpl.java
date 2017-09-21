package com.wincom.dcim.driver.mock;

import akka.actor.Props;
import com.wincom.dcim.driver.DriverCodecFactory;
import com.wincom.dcim.message.common.ParamMeta;
import com.wincom.dcim.message.common.ParamRange;
import com.wincom.dcim.message.common.ParamType;
import scala.Option;
import scala.collection.immutable.List;
import scala.collection.immutable.Seq$;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DriverFactoryImpl implements DriverCodecFactory {
	
	@Override
	public String modelName() {
		return "driver-mock1";
	}

	@Override
	public Set<ParamMeta> paramOptions() {
		Set<ParamMeta> s = new HashSet<>();
		s.add(new ParamMeta(
						"a",
						ParamType.FLOAT$.MODULE$,
						Option.apply("1.23456"),
						Option.apply(ParamRange.apply(Option.apply("2.71828"), Option.apply("3.14159"))),
				Seq$.MODULE$.empty()
				)
		);
		s.add(new ParamMeta(
						"b",
						ParamType.FLOAT$.MODULE$,
						Option.apply("2.34567"),
						Option.apply(ParamRange.apply(Option.apply("2.71828"), Option.apply("3.14159"))),
				Seq$.MODULE$.empty()
				)
		);
		return s;
	}

	@Override
	public Option<Props> create(Map<String, String> params) {
		Props p = Props.create(DriverImpl.class, params);
		return Option.apply(p);
	}
}
