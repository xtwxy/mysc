package com.wincom.dcim.signal;

import scala.Option;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by wangxy on 17-8-25.
 */
public class BetweenFactory implements UnaryFunctionFactory {
    private final Set<String> params;

    public BetweenFactory() {
        params = new HashSet<>();
        params.add("upper-bound");
        params.add("lower-bound");
    }
    @Override
    public String name() {
        return "between";
    }

    @Override
    public Set<String> paramNames() {
        return params;
    }

    @Override
    public Option<UnaryFunction> create(Map<String, String> params) {
        double upper = Double.parseDouble(params.get("upper-bound"));
        double lower = Double.parseDouble(params.get("lower-bound"));
        return Option.apply(new Between(lower, upper));
    }

    class Between implements UnaryFunction {
        private final double upperBound;
        private final double lowerBound;
        public Between(double lowerBound, double upperBound) {
            this.upperBound = upperBound;
            this.lowerBound = lowerBound;
        }

        @Override
        public Object transform(Object input) {
            if(input instanceof Double) {
                Double x = (Double) input;
                return (x < upperBound && x > lowerBound);
            }
            return input;
        }

        @Override
        public Object inverse(Object input) {
            return input;
        }
    }
}
