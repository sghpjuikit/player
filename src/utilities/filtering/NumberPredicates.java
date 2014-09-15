/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities.filtering;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;

/**
 *
 * @author Plutonium_
 */
public enum NumberPredicates implements Predicates<Number> {
    LESS((t,u) -> t.compareTo(u)<0, (t,u) -> t.compareTo(u)<0, (t,u) -> t.compareTo(u)<0, (t,u) -> t.compareTo(u)<0, (t,u) -> t.compareTo(u)<0),
    MORE((t,u) -> t.compareTo(u)>0, (t,u) -> t.compareTo(u)>0, (t,u) -> t.compareTo(u)>0, (t,u) -> t.compareTo(u)>0, (t,u) -> t.compareTo(u)>0),
    SAME((t,u) -> t.compareTo(u)==0, (t,u) -> t.compareTo(u)==0, (t,u) -> t.compareTo(u)==0, (t,u) -> t.compareTo(u)==0, (t,u) -> t.compareTo(u)==0);

    
    Map<Integer, BiPredicate<? extends Number,? extends Number>> ps = new HashMap();
    
    private NumberPredicates(BiPredicate<Short,Short> s, BiPredicate<Integer,Integer> i, BiPredicate<Long,Long> l, BiPredicate<Float,Float> f, BiPredicate<Double,Double> d) {
        ps.put(Short.class.hashCode(), s);
        ps.put(Integer.class.hashCode(), i);
        ps.put(Long.class.hashCode(), l);
        ps.put(Float.class.hashCode(), f);
        ps.put(Double.class.hashCode(), d);
    }
    
    @Override
    public BiPredicate<Number,Number> predicate(Class<Number> type) {
        if (!ps.containsKey(type.hashCode())) throw new RuntimeException("illegal class parameter");
        return (BiPredicate<Number,Number>) ps.get(type.hashCode());
    }
}
