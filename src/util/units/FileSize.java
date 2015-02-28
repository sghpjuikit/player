/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package util.units;

import java.io.File;
import jdk.nashorn.internal.ir.annotations.Immutable;
import util.dev.Dependency;
import util.parsing.StringParseStrategy;
import static util.parsing.StringParseStrategy.From.CONSTRUCTOR_STR;
import static util.parsing.StringParseStrategy.To.TO_STRING_METHOD;

/**
 * Simple class for file size handling, used primarily for its string representation.
 * <p>
 * Simple example of use: {@code new FileSize(bytes).toString() }.
 * 
 * @author uranium
 */
@Immutable
@StringParseStrategy(from = CONSTRUCTOR_STR, to = TO_STRING_METHOD)
public final class FileSize implements Comparable<FileSize> {
    
    private final long size;
    
    /**
     * Creates filesize set to size of the specified file. If the value can not
     * be determined it will be set to -l. 
     * @param f non null file to read filesize of
     * @throws NullPointerException if param null
     */
    public FileSize(File f) {
        long l = f.length();
        size = l==0 ? -1 : l;
    }
    
    /**
     * Creates filesize set to specified value.
     * @param bytes amount of bytes as a size value or -l if unknown.
     * @throws IllegalArgumentException if param negative
     */
    public FileSize(long bytes) {
        if(bytes<-1) throw new IllegalArgumentException("Bitrate value must be -1 or larger");
        
        size = bytes;
    }
    /**
     * Creates filesize set to specified value.
     * @param number of bytes as String, optionally with unit appended. Consistent
     * with toStrong().
     */
    public FileSize(String bytes) {
        this(val(bytes));
    }
    
    /** @return file size in bytes or -1 if unknown */
    public long inBytes() {
        return size;
    }
    /** @return file size in kB or -1 if unknown */
    public long inkBytes() {
        return size==-1 ? -1 : size/1024;
    }
    /** @return file size in MB or -1 if unknown */
    public long inMBytes() {
        return (long) (size==-1 ? -1 : size/(1024d*1024d));
    }
    /** @return file size in GB or -1 if unknown */
    public long inGBytes() {
        return (long) (size==-1 ? -1 : size/(1024d*1024d*1024d));
    }
    
    /**
     * Returns human readable file size text.
     * <p>
     * Displays "Unknown" if value -1 which is an 'unknown' value.
     * Appends byte units after the value.
     * <p>
     * Most appropriate unit prefix is calculated and the value converted. The
     * text value granularity is 0.01 of the used unit. Example 3.56
     * @return string representation of the object
     */
    @Override
    @Dependency("Designed to be used in tables, filters and gui.")
    @Dependency("Supports different units. kB - GB")
    public String toString() {
        if(size == -1) return "Unknown";
        
        double kB = (size / 1024d);
        double MB = (size / (1024d*1024d));
        double GB = (size / (1024d*1024d*1024d));
        
        if (GB>1024)
            return String.format("%.2f TB", GB/1024d);
        if (MB>1024)
            return String.format("%.2f GB", GB);
        else if (MB>=1)
            return String.format("%.2f MB", MB);
        else if (kB>1)
            return String.format("%.2f kB", kB);
        else
            return String.format("%d B", size);
    }

    /** @return compares by the value in bytes */
    @Override
    public int compareTo(FileSize o) {
        return Long.compare(size, o.size);
    }
    
    /** @return true if the value is the same */
    @Override
    public boolean equals(Object o) {
        if(this==o) return true;
        return o instanceof Bitrate && ((FileSize)o).size==size;
    }

    @Override
    public int hashCode() {
        return 13 * 3 + (int) (this.size ^ (this.size >>> 32));
    }
    
    
    
    private static long val(String s) {
        int i = 0;
        
        if (s.contains("B")) {
            int b = s.indexOf("B");
            String prefix = s.substring(b-1, b);
            boolean number = true;
            if("k".equalsIgnoreCase(prefix)) {
                i=1;
                number = false;
            } else
            if("m".equalsIgnoreCase(prefix)) {
                i=2;
                number = false;
            } else
            if("g".equalsIgnoreCase(prefix)) {
                i=3;
                number = false;
            }
            s = s.substring(0, number ? b : b-1).trim();
        }
        
        long number = Long.parseLong(s);
        int unit = (int) Math.pow(1024, i);
        return unit*number;
    }
}
