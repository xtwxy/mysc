package com.wincom.dcim.signal;

import scala.Option;

import java.util.*;

/**
 * Created by wangxy on 17-8-25.
 */
public class LinearFactory implements UnaryFunctionFactory {
    private final Set<String> params;

    public LinearFactory() {
        params = new HashSet<>();
        params.add("slope");
        params.add("intercept");
    }

    @Override
    public String name() {
        return Linear.class.getSimpleName();
    }

    @Override
    public Set<String> paramNames() {
        return params;
    }

    @Override
    public Option<UnaryFunction> create(Map<String, String> params) {
        double slope = Double.parseDouble(params.get("slope"));
        double intercept = Double.parseDouble(params.get("intercept"));
        return Option.apply(new Linear(slope, intercept));
    }

    public final class Linear implements UnaryFunction, InverseFunction {
        private final double slope;
        private final double intercept;
        public Linear(double slope, double intercept) {
            this.slope = slope;
            this.intercept = intercept;
        }

        @Override
        public Object transform(Object input) {
            if(input instanceof Double) {
                Double x = (Double) input;
                return slope * x + intercept;
            }
            return input;
        }

        @Override
        public Object inverse(Object input) {
            if(input instanceof Double) {
                Double y = (Double) input;
                return (y - intercept)/slope;
            }
            return input;
        }
    }
}
