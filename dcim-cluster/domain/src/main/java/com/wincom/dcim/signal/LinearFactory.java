package com.wincom.dcim.signal;

import com.wincom.dcim.message.common.ParamMeta;
import com.wincom.dcim.message.common.ParamType;
import scala.Option;
import scala.Option$;
import scala.collection.Map$;

import java.util.*;

/**
 * Created by wangxy on 17-8-25.
 */
public class LinearFactory implements UnaryFunctionFactory {
    private final Set<ParamMeta> params;

    public LinearFactory() {
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
    public String name() {
        return EqualToFactory.EqualTo.class.getSimpleName();
    }

    @Override
    public String displayName() {
        return "线性变换";
    }

    @Override
    public Set<ParamMeta> paramOptions() {
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
