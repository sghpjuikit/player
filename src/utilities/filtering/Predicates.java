/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities.filtering;

import AudioPlayer.tagging.Bitrate;
import AudioPlayer.tagging.FileSize;
import java.util.ArrayList;
import static java.util.Collections.EMPTY_LIST;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import org.reactfx.util.Tuple2;
import org.reactfx.util.Tuples;

/**
 *
 * @author Plutonium_
 */
public class Predicates {
    
    private static final Map<Class,List<Tuple2<String,BiPredicate>>> predicates = new HashMap();
    
    static {
        for(NumberPredicates nb : NumberPredicates.values()) {
            for(Class c : NumberPredicates.getSupportedClasses()) {
                Tuple2<String,BiPredicate<Number,Number>> p = nb.predicate(c);
                registerPredicate(c, p._1, p._2);
            }
        }
        
        registerPredicate(FileSize.class, "More", (x,y) -> x.compareTo(y)>0);
        registerPredicate(FileSize.class, "Is", (x,y) -> x.compareTo(y)==0);
        registerPredicate(FileSize.class, "Less", (x,y) -> x.compareTo(y)<0);
        registerPredicate(Bitrate.class, "More", (x,y) -> x.compareTo(y)>0);
        registerPredicate(Bitrate.class, "Is", (x,y) -> x.compareTo(y)==0);
        registerPredicate(Bitrate.class, "Less", (x,y) -> x.compareTo(y)<0);
        
        registerPredicate(String.class, "Is",(text,b) -> text.equals(b));
        registerPredicate(String.class, "Contains", (text,b) -> text.contains(b));
        registerPredicate(String.class, "Ends with", (text,b) -> text.endsWith(b));
        registerPredicate(String.class, "Starts with", (text,b) -> text.startsWith(b));
        registerPredicate(String.class, "Is (no case)", (text,b) -> text.equalsIgnoreCase(b));
        registerPredicate(String.class, "Contains (no case)", (text,b) -> text.toLowerCase().contains(b.toLowerCase()));
        registerPredicate(String.class, "Ends with (no case)", (text,b) -> text.toLowerCase().endsWith(b.toLowerCase()));
        registerPredicate(String.class, "Starts with (no case)", (text,b) -> text.toLowerCase().startsWith(b.toLowerCase()));
        registerPredicate(String.class, "Matches regular expression", (text,b) -> text.matches(b));
    }
    
    public static<T> void registerPredicate(Class<T> c, String name, BiPredicate<T,T> p) {
        List<Tuple2<String,BiPredicate>> ps = predicates.get(c);
        if (ps==null) ps = new ArrayList();
        ps.add(Tuples.t(name, p));
        predicates.put(c, ps);
    }
    
    public static List<Tuple2<String,BiPredicate>> getPredicate(Class c) {
        return predicates.getOrDefault(c, EMPTY_LIST);
    }
    
}
