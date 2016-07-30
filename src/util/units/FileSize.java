package util.units;

import java.io.File;

import util.dev.Dependency;
import util.parsing.ParsesFromString;
import util.parsing.StringParseStrategy;
import util.parsing.StringParseStrategy.From;

import static java.lang.Integer.max;
import static util.parsing.StringParseStrategy.To.TO_STRING_METHOD;

/**
 * Simple class for file size handling, used primarily for its string representation.
 * <p/>
 * Simple example of use: {@code new FileSize(bytes).toString() }.
 *
 * @author Martin Polakovic
 */
@StringParseStrategy(
    from = From.ANNOTATED_METHOD,
    to = TO_STRING_METHOD,
    exFrom = NumberFormatException.class
)
public final class FileSize implements Comparable<FileSize> {

    /** {@code 1024^1} */
    public static final long Ki = 1024;
    /** {@code 1024^2} */
    public static final long Mi = Ki*Ki;
    /** {@code 1024^3} */
    public static final long Gi = Ki*Mi;
    /** {@code 1024^4} */
    public static final long Ti = Mi*Mi;
    /** {@code 1024^5} */
    public static final long Pi = Ti*Ki;
    /** {@code 1024^6} */
    public static final long Ei = Ti*Mi;
    /** {@code 0} */
    public static final long MIN = 0;
    /** {@code 2^63-1} */
    public static final long MAX = Long.MAX_VALUE;
    /** Not available value. {@code -1} */
    public static final long NA = -1;
    /** Not available string value. {@code "Unknown"}*/
    public static final String NAString = "Unknown";


    private final long v;

    /**
     * Creates file size set to size of the specified file. If the value can not
     * be determined it will be set to -l.
     *
     * @param f non null file to read file size of
     * @throws NullPointerException if param null
     */
    public FileSize(File f) {
        this(inBytes(f));
    }

    /**
     * Creates file size set to specified value.
     *
     * @param bytes amount of bytes as a size value or -l if unknown.
     * @throws IllegalArgumentException if param negative
     */
    public FileSize(long bytes) {
        if (bytes<-1) throw new IllegalArgumentException("File size value must be -1 or larger");
        v = bytes;
    }

    /**
     * Creates file size set to specified value.
     *
     * @param bytes of bytes as String, optionally with unit appended. Consistent with toStrong().
     */
    @ParsesFromString
    public FileSize(String bytes) {
        this(val(bytes));
    }

    /**
     * Returns file size in bytes. Equivalent to {@code new FileSize(f).inBytes;}, but does not create an object.
     *
     * @return size of the file in bytes
     */
    public static long inBytes(File f) {
        long l = f.length();
        return l==0 ? -1 : l;
    }

    /** @return file size in bytes or -1 if unknown */
    public long inBytes() {
        return v;
    }

    /** @return file size in kB or -1 if unknown */
    public long inkBytes() {
        return v==-1 ? -1 : v/Ki;
    }

    /** @return file size in MB or -1 if unknown */
    public long inMBytes() {
        return v==-1 ? -1 : v/Mi;
    }

    /** @return file size in GB or -1 if unknown */
    public long inGBytes() {
        return v==-1 ? -1 : v/Gi;
    }

    /** @return file size in TB or -1 if unknown */
    public long inTBytes() {
        return v==-1 ? -1 : v/Ti;
    }

    /** @return file size in PB or -1 if unknown */
    public long inPBytes() {
        return v==-1 ? -1 : v/Pi;
    }

    /** @return file size in EB or -1 if unknown */
    public long inEBytes() {
        return v==-1 ? -1 : v/Ei;
    }

    /**
     * Returns human readable file size text.
     * <p/>
     * Displays "Unknown" if value -1 which is an 'unknown' value.
     * Appends byte units after the value.
     * <p/>
     * Most appropriate unit prefix is calculated and the value converted. The
     * text value granularity is 0.01 of the used unit. Example 3.56
     *
     * @return string representation of the object
     */
    @Override
    @Dependency("Designed to be used in tables, filters and gui.")
    @Dependency("Supports different units. B - EB")
    public String toString() {
        if (v == -1) return NAString;
        double EB = (v / (double)Ei);
        if (EB>=1) return String.format("%.2f EiB", EB);
        double PB = (v / (double)Pi);
        if (PB>=1) return String.format("%.2f PiB", PB);
        double TB = (v / (double)Ti);
        if (TB>=1) return String.format("%.2f TiB", TB);
        double GB = (v / (double)Gi);
        if (GB>=1) return String.format("%.2f GiB", GB);
        double MB = (v / (double)Mi);
        if (MB>=1) return String.format("%.2f MiB", MB);
        double kB = (v / (double)Ki);
        if (kB>1) return String.format("%.2f kiB", kB);
        else return String.format("%d B", v);
    }

    /** @return compares by the value in bytes */
    @Override
    public int compareTo(FileSize o) {
        return Long.compare(v, o.v);
    }

    /** @return true if the value is the same */
    @Override
    public boolean equals(Object o) {
        return (this == o) || (o instanceof FileSize && ((FileSize)o).v == v);
    }

    @Override
    public int hashCode() {
        return 13 * 3 + (int) (this.v ^ (this.v >>> 32));
    }


    private static long val(String s) throws NumberFormatException {
        long unit = 1;

        if (s.equals(NAString)) return NA;
        int b = max(s.indexOf("B"),s.indexOf("b"));
        if (b>0) {
            String prefix = s.substring(b-1, b);
            int skip = 0;
            if ("k".equalsIgnoreCase(prefix)) {
                unit = Ki;
                skip++;
            } else
            if ("m".equalsIgnoreCase(prefix)) {
                unit = Mi;
                skip++;
            } else
            if ("g".equalsIgnoreCase(prefix)) {
                unit = Gi;
                skip++;
            } else
            if ("t".equalsIgnoreCase(prefix)) {
                unit = Ti;
                skip++;
            } else
            if ("p".equalsIgnoreCase(prefix)) {
                unit = Pi;
                skip++;
            } else
            if ("e".equalsIgnoreCase(prefix)) {
                unit = Ei;
                skip++;
            }
            s = s.substring(0, b-skip).trim();
        }

        double number = Double.parseDouble(s);
        return (long)(unit*number);
    }
}