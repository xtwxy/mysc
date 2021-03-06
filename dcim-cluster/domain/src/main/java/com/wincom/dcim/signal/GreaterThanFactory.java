package com.wincom.dcim.signal;

import com.wincom.dcim.message.common.ParamMeta;
import com.wincom.dcim.message.common.ParamType;
import scala.Option;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static java.lang.Math.*;

import com.wincom.dcim.signal.LessThanFactory.*;
import com.wincom.dcim.signal.BetweenFactory.*;
import com.wincom.dcim.signal.EqualToFactory.*;
import scala.Option$;
import scala.collection.Map$;

/**
 * Created by wangxy on 17-8-25.
 */
public class GreaterThanFactory implements UnaryFunctionFactory {
    private final Set<ParamMeta> params;

    public GreaterThanFactory() {
        params = new HashSet<>();
        params.add(new ParamMeta(
                "threshold",
                "门限",
                ParamType.FLOAT$.MODULE$,
                Option$.MODULE$.empty(),
                Option$.MODULE$.empty(),
                Map$.MODULE$.empty(),
                Option$.MODULE$.empty()
        ));
        params.add(new ParamMeta(
                "insensitivity-zone",
                "门限死区(非敏感区宽度)",
                ParamType.FLOAT$.MODULE$,
                Option$.MODULE$.empty(),
                Option$.MODULE$.empty(),
                Map$.MODULE$.empty(),
                Option$.MODULE$.empty()
        ));
        params.add(new ParamMeta(
                "use-percentage",
                "使用百分比阈值",
                ParamType.FLOAT$.MODULE$,
                Option$.MODULE$.empty(),
                Option$.MODULE$.empty(),
                Map$.MODULE$.empty(),
                Option$.MODULE$.empty()
        ));
        params.add(new ParamMeta(
                "range",
                "量程",
                ParamType.FLOAT$.MODULE$,
                Option$.MODULE$.empty(),
                Option$.MODULE$.empty(),
                Map$.MODULE$.empty(),
                Option$.MODULE$.empty()
        ));
    }
    @Override
    public String name() {
        return getClass().getSimpleName();
    }

    @Override
    public String displayName() {
        return "大于...";
    }

    @Override
    public Set<ParamMeta> paramOptions() {
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

        return Option.apply(new GreaterThan(threshold, abs(insensitivityZone)));
    }

    public final class GreaterThan implements UnaryFunction , SetFunction {
        public final double threshold;
        public final double insensitivityZone;
        private Boolean value;
        public GreaterThan(double threshold, double insensitivityZone) {
            this.threshold = threshold;
            this.insensitivityZone = insensitivityZone;
            this.value = null;
        }

        @Override
        public Object transform(Object input) {
            if(input instanceof Double) {
                Double x = (Double) input;
                if(value != null) {
                    if(abs(x - threshold) > insensitivityZone) {
                        value = (x > threshold);
                    } else {
                        // within the insensitivity zone, remain unchanged.
                    } 
                } else {
                    value = (x > threshold);
                }
                return value;
            }
            return input;
        }

        @Override
        public boolean contains(Object e) {
            Object o = transform(e);
            if(o instanceof Boolean) {
                return (Boolean) o;
            }
            return false;
        }

        @Override
        public boolean subsetOf(SetFunction f) {
            if(f instanceof Between) {
                return false;
            } else if(f instanceof GreaterThan) {
                GreaterThan g = (GreaterThan) f;
                if(threshold >= g.threshold) return true;
            } else if(f instanceof LessThan) {
                return false;
            }
            return false;
        }

        @Override
        public boolean intersects(SetFunction f) {
            if(f instanceof Between) {
                Between b = (Between) f;
                if(threshold <= b.upperBound) {
                    return true;
                }
            } else if(f instanceof GreaterThan) {
                return true;
            } else if(f instanceof LessThan) {
                LessThan l = (LessThan) f;
                if(l.threshold >= threshold) return true;
            } else if(f instanceof EqualTo) {
                EqualTo e = (EqualTo) f;
                if(threshold <= e.reference) return true;
            }
            return false;

        }
    }
}
