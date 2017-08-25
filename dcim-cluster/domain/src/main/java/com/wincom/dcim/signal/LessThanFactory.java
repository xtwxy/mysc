package com.wincom.dcim.signal;

import scala.Option;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by wangxy on 17-8-25.
 */
public class LessThanFactory implements UnaryFunctionFactory {
    private final Set<String> params;

    public LessThanFactory() {
        params = new HashSet<>();
        params.add("reference");
    }
    @Override
    public String name() {
        return "less-than";
    }

    @Override
    public Set<String> paramNames() {
        return params;
    }

    @Override
    public Option<UnaryFunction> create(Map<String, String> params) {
        double reference = Double.parseDouble(params.get("reference"));
        return Option.apply(new LessThan(reference));
    }

    class LessThan implements UnaryFunction {
        private final double reference;
        public LessThan(double reference) {
            this.reference = reference;
        }

        @Override
        public Object transform(Object input) {
            if(input instanceof Double) {
                Double x = (Double) input;
                return (x < reference);
            }
            return input;
        }

        @Override
        public Object inverse(Object input) {
            return input;
        }
    }
}
