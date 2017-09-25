package com.wincom.dcim.signal;

import com.wincom.dcim.message.common.ParamMeta;
import com.wincom.dcim.message.common.ParamType;
import scala.Option;
import scala.Option$;
import scala.collection.Map$;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by wangxy on 17-8-24.
 */
public class BabeFactory implements UnaryFunctionFactory {

    private final Set<ParamMeta> params;

    public BabeFactory() {
        params = new HashSet<>();
        params.add(new ParamMeta(
                "slope",
                "倍率",
                ParamType.FLOAT$.MODULE$,
                Option$.MODULE$.empty(),
                Option$.MODULE$.empty(),
                Map$.MODULE$.empty(),
                Option$.MODULE$.empty()
        ));
        params.add(new ParamMeta(
                "intercept",
                "偏移量",
                ParamType.FLOAT$.MODULE$,
                Option$.MODULE$.empty(),
                Option$.MODULE$.empty(),
                Map$.MODULE$.empty(),
                Option$.MODULE$.empty()
        ));
    }

    @Override
    public String displayName() {
        return "萌萌哒变换";
    }

    @Override
    public Set<ParamMeta> paramOptions() {
        return params;
    }
    @Override
    public String name() {
        return "babe-signal";
    }

    @Override
    public Option<UnaryFunction> create(Map<String, String> params) {
        return Option.apply(new BabeImpl(params));
    }
}
