package com.wincom.dcim.signal;

import com.wincom.dcim.message.common.ParamMeta;
import com.wincom.dcim.message.common.ParamType;
import com.wincom.dcim.signal.EqualToFactory.EqualTo;
import com.wincom.dcim.signal.GreaterThanFactory.GreaterThan;
import com.wincom.dcim.signal.LessThanFactory.LessThan;
import scala.Option;
import scala.Option$;
import scala.collection.Map$;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.lang.Math.abs;

/**
 * Created by wangxy on 17-8-25.
 */
public class BetweenFactory implements UnaryFunctionFactory {
    private final Set<ParamMeta> params;

    public BetweenFactory() {
        params = new HashSet<>();
        params.add(new ParamMeta(
                "upper-bound",
                "上界",
                ParamType.FLOAT$.MODULE$,
                Option$.MODULE$.empty(),
                Option$.MODULE$.empty(),
                Map$.MODULE$.empty(),
                Option$.MODULE$.empty()
        ));
        params.add(new ParamMeta(
                "lower-bound",
                "下界",
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
        return "在...之间";
    }

    @Override
    public Set<ParamMeta> paramOptions() {
        return params;
    }

    @Override
    public Option<UnaryFunction> create(Map<String, String> params) {
        double upper = Double.parseDouble(params.get("upper-bound"));
        double lower = Double.parseDouble(params.get("lower-bound"));
        double insensitivityZone = Double.parseDouble(params.get("insensitivity-zone"));

        double range = 0;
        String rangeStr = params.get("range");
        if (rangeStr != null) {
            range = Double.parseDouble(rangeStr);
        }
        boolean usePercentage = false;
        String usePercentageStr = params.get("use-percentage");
        if (usePercentageStr != null) {
            usePercentage = Boolean.parseBoolean(usePercentageStr);
        }

        if (usePercentage) {
            insensitivityZone = (range * insensitivityZone) / 100.0;
        }

        return Option.apply(new Between(lower, upper, insensitivityZone));
    }

    public final class Between implements UnaryFunction, SetFunction {
        public final double upperBound;
        public final double lowerBound;
        public final double insensitivityZone;
        private Boolean value;

        public Between(double lowerBound, double upperBound, double insensitivityZone) {
            this.upperBound = upperBound;
            this.lowerBound = lowerBound;
            this.insensitivityZone = insensitivityZone;
        }

        @Override
        public Object transform(Object input) {
            if (input instanceof Double) {
                Double x = (Double) input;
                if (value != null) {
                    if (abs(x - lowerBound) > insensitivityZone || abs(x - upperBound) > insensitivityZone) {
                        value = (x < upperBound && x > lowerBound);
                    } else {
                        // within the insensitivity zone, remain unchanged.
                    }
                } else {
                    value = (x < upperBound && x > lowerBound);
                }
                return value;
            }
            return input;
        }

        @Override
        public boolean contains(Object e) {
            Object o = transform(e);
            if (o instanceof Boolean) {
                return (Boolean) o;
            }
            return false;
        }

        @Override
        public boolean subsetOf(SetFunction f) {
            if (f instanceof Between) {
                Between b = (Between) f;
                if (lowerBound >= b.lowerBound && upperBound <= b.upperBound) {
                    return true;
                }
            } else if (f instanceof GreaterThan) {
                GreaterThan g = (GreaterThan) f;
                if (lowerBound >= g.threshold) return true;
            } else if (f instanceof LessThan) {
                LessThan l = (LessThan) f;
                if (upperBound <= l.threshold) return true;
            } else if (f instanceof EqualTo) {
                EqualTo e = (EqualTo) f;
                if (abs(lowerBound - e.reference) < e.delta
                        && abs(upperBound - e.reference) < e.delta) return true;
            }
            return false;
        }

        @Override
        public boolean intersects(SetFunction f) {
            if (f instanceof Between) {
                Between b = (Between) f;
                if (lowerBound <= b.upperBound && upperBound >= b.lowerBound) {
                    return true;
                }
            } else if (f instanceof GreaterThan) {
                GreaterThan g = (GreaterThan) f;
                if (g.threshold <= upperBound) return true;
            } else if (f instanceof LessThan) {
                LessThan l = (LessThan) f;
                if (l.threshold >= lowerBound) return true;
            } else if (f instanceof EqualTo) {
                EqualTo e = (EqualTo) f;
                if (lowerBound <= e.reference && upperBound >= e.reference) return true;
            }
            return false;
        }
    }
}
