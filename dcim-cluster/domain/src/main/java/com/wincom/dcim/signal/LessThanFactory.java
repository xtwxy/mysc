package com.wincom.dcim.signal;

import scala.Option;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.lang.Math.abs;

/**
 * Created by wangxy on 17-8-25.
 */
public class LessThanFactory implements UnaryFunctionFactory {
    private final Set<String> params;

    public LessThanFactory() {
        params = new HashSet<>();
        params.add("threshold");
        params.add("insensitivity-zone");
        params.add("use-percentage");
    }
    @Override
    public String name() {
        return LessThan.class.getSimpleName();
    }

    @Override
    public Set<String> paramNames() {
        return params;
    }

    @Override
    public Option<UnaryFunction> create(Map<String, String> params) {
        double threshold = Double.parseDouble(params.get("threshold"));
        double insensitivityZone = Double.parseDouble(params.get("insensitivity-zone"));

        double range = 0;
        String rangeStr = params.get("range");
        if(rangeStr != null) {
            range = Double.parseDouble(rangeStr);
        }
        boolean usePercentage = false;
        String usePercentageStr = params.get("use-percentage");
        if(usePercentageStr != null) {
            usePercentage = Boolean.parseBoolean(usePercentageStr);
        }

        if(usePercentage) {
            insensitivityZone = (range * insensitivityZone) / 100.0;
        }

        return Option.apply(new LessThan(threshold, insensitivityZone));
    }

    class LessThan implements UnaryFunction {
        private final double threshold;
        private final double insensitivityZone;
        private Boolean value;
        public LessThan(double threshold, double insensitivityZone) {
            this.threshold = threshold;
            this.insensitivityZone = insensitivityZone;
        }

        @Override
        public Object transform(Object input) {
            if(input instanceof Double) {
                Double x = (Double) input;
                if(value != null) {
                    if(abs(x - threshold) > insensitivityZone) {
                        value = (x < threshold);
                    } else {
                        // within the insensitivity zone, remain unchanged.
                    }
                } else {
                    value = (x < threshold);
                }
                return value;
            }
            return input;
        }

        @Override
        public Object inverse(Object input) {
            return input;
        }
    }
}
