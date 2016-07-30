package util.units;

import java.util.regex.PatternSyntaxException;

import util.parsing.ParsesFromString;
import util.parsing.ParsesToString;
import util.parsing.StringParseStrategy;
import util.parsing.StringParseStrategy.From;

import static java.util.Objects.hash;
import static util.parsing.StringParseStrategy.To.TO_STRING_METHOD;

/** Defines number within an amount. For example 15/20. */
@StringParseStrategy(
    from = From.ANNOTATED_METHOD,
    to = TO_STRING_METHOD,
    exFrom = { PatternSyntaxException.class, NumberFormatException.class, ArrayIndexOutOfBoundsException.class}
)
public class NofX implements Comparable<NofX> {
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

    @ParsesToString
    @Override
    public String toString() {
        return toString("/");
    }

    public String toString(String separator) {
        return (n==-1 ? "?" : n) + separator + (of==-1 ? "?" : of);
    }

    @Override
    public boolean equals(Object ob) {
        if (this==ob) return true;
        if (ob instanceof NofX) {
            NofX o = (NofX)ob;
            return n==o.n && of==o.of;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return hash(n,of);
    }

    @ParsesFromString
    public static NofX fromString(String s) throws PatternSyntaxException, NumberFormatException, ArrayIndexOutOfBoundsException {
        String[] a = s.split("/", 0);
        if (a.length!=2) throw new ArrayIndexOutOfBoundsException("Not in an 'X/Y' format");
        return new NofX(Integer.parseInt(a[0]), Integer.parseInt(a[1]));
    }
}
