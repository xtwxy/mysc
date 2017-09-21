package com.wincom.dcim.driver.hello;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.wincom.dcim.driver.DriverCodecFactory;

import akka.actor.Props;
import com.wincom.dcim.driver.mock.DriverImpl;
import com.wincom.dcim.message.common.ParamMeta;
import com.wincom.dcim.message.common.ParamRange;
import com.wincom.dcim.message.common.ParamType;
import scala.Option;
import scala.collection.IndexedSeq$;
import scala.collection.immutable.IndexedSeq;
import scala.collection.immutable.List;
import scala.collection.immutable.Seq$;

public class HelloFactory implements DriverCodecFactory {

	@Override
	public String modelName() {
		return "Hello";
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
