/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.itemnode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import static util.functional.Util.forEach;
import static util.functional.Util.rep;

/**
 *
 * @author Plutonium_
 */
public class StringSplitParser implements Function<String, List<String>> {
    public static final char PARRSE_KEY_LIMITER = '%';
    
    public final String pex;
    public final List<String> parse_keys = new ArrayList();
    public final List<String> key_separators = new ArrayList();

    /**
     * @param expression parsing expression, e.g. "%first% - %second%". The 'first'
     * and 'second' are parse keys and the ' - ' is delimiter. There can be any
     * number of parse keys (and delimiters).
     * @throws IllegalArgumentException if expression parameter can not be parsed
     */
    public StringSplitParser(String expression) {
        this.pex = expression;
        
        int limiter = 0;
        String key = "";
        String sep = "";
        for(int i=0; i<expression.length(); i++) {
            char c = expression.charAt(i);
            if(c==PARRSE_KEY_LIMITER) {
                if(limiter==1) {
                    limiter=0;
                    if(!key.isEmpty()) parse_keys.add(key);
                    else throw new IllegalArgumentException("Cant create split string expression. Wrong format. '" + expression + "'");
                    key="";
                } else {
                    limiter=1;
                    if(!sep.isEmpty()) key_separators.add(sep);
                    if(sep.isEmpty()&&i!=0) throw new IllegalArgumentException("Cant create split string expression. Wrong format. '" + expression + "'");
                    sep="";
                }
            } else {
                if(limiter==1) key+=c;
                if(limiter==0) sep+=c;
            }
        }
        if(limiter==1) throw new IllegalArgumentException("Cant create split string expression. Wrong format. '" + expression + "'");
        if(parse_keys.isEmpty()) throw new IllegalArgumentException("Cant create split string expression. Wrong format. '" + expression + "'");
    }
    
    /**
     * Parses text into parts.
     * @param text text to parse
     * @throws IllegalArgumentException if text parsing fails
     */
    @Override
    public List<String> apply(String text) {
        List<String> out = new ArrayList(parse_keys.size());
        int at = 0;
        for(int i=0; i<key_separators.size(); i++) {
            String sep = key_separators.get(i);
            at = text.indexOf(sep);
//            if(at==-1) throw new IllegalArgumentException("Cant parse string. No occurence of '" + sep + "' in: '" + text + "'");
            if(at==-1) {
                rep(parse_keys.size()-out.size(),() -> out.add(null));
                return out;
            }
            String val = text.substring(0,at);
            out.add(val);   // add ith value
            text = text.substring(at+sep.length());
        }
            out.add(text);  //add last value (N values, N-1 separators)
        return out;
    }
    
    /** 
     * Same as {@link #apply(java.lang.String)}, but returns the keys too.
     * @param text text to parse
     * @throws IllegalArgumentException if text parsing fails
     */
    public Map<String,String> applyM(String text) {
        List<String> splits = apply(text);
        Map<String,String> m = new HashMap();
        forEach(parse_keys, splits, m::put);
        return m;
    }

    @Override
    public String toString() {
        return pex;
    }
    
    public static class Split {
        public String parse_key;
        public String split;

        public Split(String parse_key, String split) {
            this.parse_key = parse_key;
            this.split = split;
        }

        @Override
        public String toString() {
            return parse_key + ":" + split;
        }
    }
    public static class SplitData extends ArrayList<Split> {}
}
