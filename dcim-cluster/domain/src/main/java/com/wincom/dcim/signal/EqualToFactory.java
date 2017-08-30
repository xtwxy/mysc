package com.wincom.dcim.signal;

import scala.Option;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.lang.Math.*;

/**
 * Created by wangxy on 17-8-25.
 */
public class EqualToFactory implements UnaryFunctionFactory {
    private final Set<String> params;

    public EqualToFactory() {
        params = new HashSet<>();
        params.add("reference");
        params.add("delta");
    }
    @Override
    public String name() {
        return EqualTo.class.getSimpleName();
    }

    @Override
    public Set<String> paramNames() {
        return params;
    }

    @Override
    public Option<UnaryFunction> create(Map<String, String> params) {
        double reference = Double.parseDouble(params.get("reference"));
        double delta = Double.parseDouble(params.get("delta"));
        return Option.apply(new EqualTo(reference, abs(delta)));
    }

    public final class EqualTo implements UnaryFunction {
        private final double reference;
        private final double delta;
        public EqualTo(double reference, double delta) {
            this.reference = reference;
            this.delta = delta;
        }

        @Override
        public Object transform(Object input) {
            if(input instanceof Double) {
                Double x = (Double) input;
                return abs(x - reference) < delta;
            }
            return input;
        }
    }
}
