/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities.filtering;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import org.reactfx.util.Tuple2;
import org.reactfx.util.Tuples;
import utilities.SteroidObject;

/**
 *
 * @author Plutonium_
 */
public enum NumberPredicates implements SteroidObject{
    LESS((t,u) -> t.compareTo(u)<0, (t,u) -> t.compareTo(u)<0, (t,u) -> t.compareTo(u)<0, (t,u) -> t.compareTo(u)<0, (t,u) -> t.compareTo(u)<0),
    MORE((t,u) -> t.compareTo(u)>0, (t,u) -> t.compareTo(u)>0, (t,u) -> t.compareTo(u)>0, (t,u) -> t.compareTo(u)>0, (t,u) -> t.compareTo(u)>0),
    SAME((t,u) -> t.compareTo(u)==0, (t,u) -> t.compareTo(u)==0, (t,u) -> t.compareTo(u)==0, (t,u) -> t.compareTo(u)==0, (t,u) -> t.compareTo(u)==0),
    NOT_LESS((t,u) -> t.compareTo(u)>=0, (t,u) -> t.compareTo(u)>=0, (t,u) -> t.compareTo(u)>=0, (t,u) -> t.compareTo(u)>=0, (t,u) -> t.compareTo(u)>=0),
    NOT_MORE((t,u) -> t.compareTo(u)<=0, (t,u) -> t.compareTo(u)<=0, (t,u) -> t.compareTo(u)<=0, (t,u) -> t.compareTo(u)<=0, (t,u) -> t.compareTo(u)<=0),
    NOT_SAME((t,u) -> t.compareTo(u)!=0, (t,u) -> t.compareTo(u)!=0, (t,u) -> t.compareTo(u)!=0, (t,u) -> t.compareTo(u)!=0, (t,u) -> t.compareTo(u)!=0);

    
    Map<Integer, BiPredicate<? extends Number,? extends Number>> ps = new HashMap();
    
    private NumberPredicates(BiPredicate<Short,Short> s, BiPredicate<Integer,Integer> i, BiPredicate<Long,Long> l, BiPredicate<Float,Float> f, BiPredicate<Double,Double> d) {
        ps.put(Short.class.hashCode(), s);
        ps.put(Integer.class.hashCode(), i);
        ps.put(Long.class.hashCode(), l);
        ps.put(Float.class.hashCode(), f);
        ps.put(Double.class.hashCode(), d);
    }
    
    public static List<Class> getSupportedClasses() {
        return Arrays.asList(Short.class,Integer.class,Long.class,Float.class,Double.class);
    }

    
    public Tuple2<String, BiPredicate<Number, Number>> predicate(Class<Number> type) {
        if (!ps.containsKey(type.hashCode())) throw new RuntimeException("illegal class parameter");
        return Tuples.t(toStringEnum(), (BiPredicate<Number,Number>) ps.get(type.hashCode()));
    }
}
