package com.wincom.dcim.signal;

import scala.Option;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.wincom.dcim.signal.BetweenFactory.Between;
import com.wincom.dcim.signal.GreaterThanFactory.GreaterThan;
import com.wincom.dcim.signal.LessThanFactory.LessThan;

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

    public final class EqualTo implements UnaryFunction, SetFunction {
        public final double reference;
        public final double delta;
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
                Between b = (Between) f;
                if(reference >= b.lowerBound && reference <= b.upperBound) {
                    return true;
                }
            } else if(f instanceof GreaterThan) {
                GreaterThan g = (GreaterThan) f;
                if(reference >= g.threshold) return true;
            } else if(f instanceof LessThan) {
                LessThan l = (LessThan) f;
                if(reference <= l.threshold) return true;
            } else if(f instanceof EqualTo) {
                EqualTo e = (EqualTo) f;
                if (abs(reference - e.reference) < delta) return true;
            }
            return false;
        }

        @Override
        public boolean intersects(SetFunction f) {
            return false;
        }
    }
}
