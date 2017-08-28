package com.wincom.dcim.signal;

import scala.Option;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by wangxy on 17-8-25.
 */
public class NotFactory implements UnaryFunctionFactory {
    private final Set<String> params;

    public NotFactory() {
        params = new HashSet<>();
    }
    @Override
    public String name() {
        return Not.class.getSimpleName();
    }

    @Override
    public Set<String> paramNames() {
        return params;
    }

    @Override
    public Option<UnaryFunction> create(Map<String, String> params) {
        return Option.apply(new Not());
    }

    class Not implements UnaryFunction {
        public Not() {
        }

        @Override
        public Object transform(Object input) {
            if(input instanceof Boolean) {
                Boolean x = (Boolean) input;
                return !x;
            }
            return input;
        }

        @Override
        public Object inverse(Object input) {
            return input;
        }
    }
}