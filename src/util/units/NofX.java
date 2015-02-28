/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.units;

import static java.util.Objects.hash;
import jdk.nashorn.internal.ir.annotations.Immutable;
import util.parsing.StringParseStrategy;
import static util.parsing.StringParseStrategy.From.FROM_STRING_METHOD;
import static util.parsing.StringParseStrategy.To.TO_STRING_METHOD;

/** Defines number within an amount. For example 15/20. */
@Immutable
@StringParseStrategy(from = FROM_STRING_METHOD, to = TO_STRING_METHOD)
public class NofX implements Comparable<NofX>{
    public final int n;
    public final int of ;

    public NofX(int n, int of) {
        this.n = n;
        this.of = of;
    }

    @Override
    public int compareTo(NofX o) {
        int i = Integer.compare(of, o.of);
        return i!=0 ? i : Integer.compare(n, o.n);
    }
    
    @Override
    public String toString() {
        return toString("/");
    }
    
    public String toString(String separator) {
        return (n==-1 ? "?" : n) + separator + (of==-1 ? "?" : of);
    }

    @Override
    public boolean equals(Object ob) {
        if(this==ob) return true;
        if(ob instanceof NofX) {
            NofX o = (NofX)ob;
            return n==o.n && of==o.of;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return hash(n,of);
    }
    
    public static NofX fromString(String s) {
        String[] a = s.split("/", 0);
        return new NofX(Integer.parseInt(a[0]), Integer.parseInt(a[1]));
    } 
}
