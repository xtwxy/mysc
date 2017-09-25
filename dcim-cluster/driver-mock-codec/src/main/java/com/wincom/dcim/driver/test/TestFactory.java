package com.wincom.dcim.driver.test;

import akka.actor.Props;
import com.wincom.dcim.driver.DriverCodecFactory;
import com.wincom.dcim.driver.mock.DriverImpl;
import com.wincom.dcim.message.common.ParamMeta;
import com.wincom.dcim.message.common.ParamRange;
import com.wincom.dcim.message.common.ParamType;
import scala.Option;
import scala.collection.Map$;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TestFactory implements DriverCodecFactory {

    @Override
    public String modelName() {
        return "Test";
    }

    @Override
    public Set<ParamMeta> paramOptions() {
        Set<ParamMeta> s = new HashSet<>();
        s.add(new ParamMeta(
                        "a",
                        ParamType.FLOAT$.MODULE$,
                        Option.apply("1.23456"),
                        Option.apply(ParamRange.apply(Option.apply("2.71828"), Option.apply("3.14159"))),
                        Map$.MODULE$.empty(),
                        Option.apply("Kilogram")
                )
        );
        s.add(new ParamMeta(
                        "b",
                        ParamType.FLOAT$.MODULE$,
                        Option.apply("2.34567"),
                        Option.apply(ParamRange.apply(Option.apply("2.71828"), Option.apply("3.14159"))),
                        Map$.MODULE$.empty(),
                        Option.apply("Kilogram")
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
