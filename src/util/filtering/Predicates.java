/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.filtering;

import java.util.ArrayList;
import static java.util.Collections.EMPTY_LIST;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import static util.Util.isNonEmptyPalindrome;
import util.collections.Tuple2;
import static util.collections.Tuples.tuple;
import util.units.Bitrate;
import util.units.FileSize;
import util.units.FormattedDuration;
import util.units.NofX;

/**
 *
 * @author Plutonium_
 */
public class Predicates {
    
    private static final Map<Class,List<Tuple2<String,BiPredicate>>> predicates = new HashMap();
    private static final Map<Class,Tuple2<String,BiPredicate>> prefpredicates = new HashMap();
    
    static {
        for(NumberPredicates nb : NumberPredicates.values()) {
            for(Class c : NumberPredicates.getSupportedClasses()) {
                Tuple2<String,BiPredicate<Number,Number>> p = nb.predicate(c);
                registerPredicate(c, p._1, p._2);
            }
        }
        
        registerPredicate(FileSize.class, "More", (x,y) -> x.compareTo(y)>0);
        registerPredicate(FileSize.class, "Is", (x,y) -> x.compareTo(y)==0);
        registerPredicate(FileSize.class, "Less", (x,y) -> x.compareTo(y)<0, true);
        registerPredicate(FileSize.class, "Not more", (x,y) -> x.compareTo(y)<=0);
        registerPredicate(FileSize.class, "Is not", (x,y) -> x.compareTo(y)!=0);
        registerPredicate(FileSize.class, "Not less", (x,y) -> x.compareTo(y)>=0);
        registerPredicate(FileSize.class, "Is unknown", (x,y) -> x.inBytes()==-1);
        registerPredicate(FileSize.class, "Is known", (x,y) -> x.inBytes()>-1);
        registerPredicate(FileSize.class, "Is from the future", (x,y) -> x.inGBytes()==1.21);
        
        registerPredicate(String.class, "Is",(text,b) -> text.equals(b));
        registerPredicate(String.class, "Contains", (text,b) -> text.contains(b));
        registerPredicate(String.class, "Ends with", (text,b) -> text.endsWith(b));
        registerPredicate(String.class, "Starts with", (text,b) -> text.startsWith(b));
        registerPredicate(String.class, "Is (no case)", (text,b) -> text.equalsIgnoreCase(b));
        registerPredicate(String.class, "Contains (no case)", (text,b) -> text.toLowerCase().contains(b.toLowerCase()), true);
        registerPredicate(String.class, "Ends with (no case)", (text,b) -> text.toLowerCase().endsWith(b.toLowerCase()));
        registerPredicate(String.class, "Starts with (no case)", (text,b) -> text.toLowerCase().startsWith(b.toLowerCase()));
        registerPredicate(String.class, "Matches regular expression", (text,b) -> text.matches(b));
        registerPredicate(String.class, "Is not",(text,b) -> !text.equals(b));
        registerPredicate(String.class, "Contains not", (text,b) -> !text.contains(b));
        registerPredicate(String.class, "Not ends with", (text,b) -> !text.endsWith(b));
        registerPredicate(String.class, "Not starts with", (text,b) -> !text.startsWith(b));
        registerPredicate(String.class, "Is not (no case)", (text,b) -> !text.equalsIgnoreCase(b));
        registerPredicate(String.class, "Contains not (no case)", (text,b) -> !text.toLowerCase().contains(b.toLowerCase()));
        registerPredicate(String.class, "Not ends with (no case)", (text,b) -> !text.toLowerCase().endsWith(b.toLowerCase()));
        registerPredicate(String.class, "Not starts with (no case)", (text,b) -> !text.toLowerCase().startsWith(b.toLowerCase()));
        registerPredicate(String.class, "Not matches regular expression", (text,b) -> !text.matches(b));
        registerPredicate(String.class, "More", (x,y) -> x.compareTo(y)>0);
        registerPredicate(String.class, "Less", (x,y) -> x.compareTo(y)<0);
        registerPredicate(String.class, "Not more", (x,y) -> x.compareTo(y)<=0);
        registerPredicate(String.class, "Not less", (x,y) -> x.compareTo(y)>=0);
        registerPredicate(String.class, "Longer than", (x,y) -> x.length()>Integer.parseInt(y));
        registerPredicate(String.class, "Shorter than", (x,y) -> x.length()<Integer.parseInt(y));
        registerPredicate(String.class, "Not longer than", (x,y) -> x.length()<=Integer.parseInt(y));
        registerPredicate(String.class, "Not shorter than", (x,y) -> x.length()>=Integer.parseInt(y));
        registerPredicate(String.class, "Long exactly", (x,y) -> x.length()==Integer.parseInt(y));
        registerPredicate(String.class, "Is empty", (x,y) -> x.isEmpty());
        registerPredicate(String.class, "Is funny", (x,y) -> x.contains("fun") && x.contains("y"));
        registerPredicate(String.class, "Is palindrome", (x,y) -> isNonEmptyPalindrome(x));
        
        registerPredicate(Bitrate.class, "More", (x,y) -> x.compareTo(y)>0, true);
        registerPredicate(Bitrate.class, "Is", (x,y) -> x.compareTo(y)==0);
        registerPredicate(Bitrate.class, "Less", (x,y) -> x.compareTo(y)<0);
        registerPredicate(Bitrate.class, "Not more", (x,y) -> x.compareTo(y)<=0);
        registerPredicate(Bitrate.class, "Is not", (x,y) -> x.compareTo(y)!=0);
        registerPredicate(Bitrate.class, "Not less", (x,y) -> x.compareTo(y)>=0);
        registerPredicate(Bitrate.class, "Is good", (x,y) -> x.getValue()>=320);
        registerPredicate(Bitrate.class, "Is bad", (x,y) -> x.getValue()<=128);
        registerPredicate(Bitrate.class, "Is unknown", (x,y) -> x.getValue()==-1);
        registerPredicate(Bitrate.class, "Is known", (x,y) -> x.getValue()>-1);
        
        registerPredicate(FormattedDuration.class, "Less", (x,y) -> x.compareTo(y)<0);
        registerPredicate(FormattedDuration.class, "Is", (x,y) ->  x.compareTo(y)==0);
        registerPredicate(FormattedDuration.class, "More", (x,y) ->  x.compareTo(y)>0, true);
        registerPredicate(FormattedDuration.class, "Not less", (x,y) -> x.compareTo(y)>=0);
        registerPredicate(FormattedDuration.class, "Is not", (x,y) ->  x.compareTo(y)!=0);
        registerPredicate(FormattedDuration.class, "Not more", (x,y) ->  x.compareTo(y)<=0);
        
        registerPredicate(NofX.class, "Less", (x,y) -> x.compareTo(y)<0);
        registerPredicate(NofX.class, "Is", (x,y) ->  x.compareTo(y)==0);
        registerPredicate(NofX.class, "More", (x,y) ->  x.compareTo(y)>0, true);
        registerPredicate(NofX.class, "Not less", (x,y) -> x.compareTo(y)>=0);
        registerPredicate(NofX.class, "Is not", (x,y) ->  x.compareTo(y)!=0);
        registerPredicate(NofX.class, "Not more", (x,y) ->  x.compareTo(y)<=0);
    }
    
    public static<T> void registerPredicate(Class<T> c, String name, BiPredicate<T,T> p) {
        registerPredicate(c, name, p, false);
    }
    public static<T> void registerPredicate(Class<T> c, String name, BiPredicate<T,T> p, boolean pref) {
        List<Tuple2<String,BiPredicate>> ps = predicates.get(c);
        if (ps==null) ps = new ArrayList();
        ps.add(tuple(name, p));
        predicates.put(c, ps);
        if(pref) prefpredicates.put(c, tuple(name, p));
    }
    
    public static List<Tuple2<String,BiPredicate>> getPredicates(Class c) {
        return predicates.getOrDefault(c, EMPTY_LIST);
    }
    
    public static Tuple2<String,BiPredicate> getPrefPredicate(Class c) {
        return prefpredicates.get(c);
    }
    
}
