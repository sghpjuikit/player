/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package util.units;

import java.io.File;
import jdk.nashorn.internal.ir.annotations.Immutable;
import util.dev.Dependency;

/**
 * Simple class for file size handling, used primarily for its string representation.
 * <p>
 * Simple example of use: {@code new FIlesize(bytes).toString() }.
 * 
 * @author uranium
 */
@Immutable
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
    
    /** @return file size in bytes or -1 if unknown */
    public long inBytes() {
        return size;
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
    @Dependency("Designed to be used in tables and gui.")
    @Dependency("Must be consistent with fromString().")
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
        else
            return String.format("%.2f kB", kB);
    }

    @Override
    public int compareTo(FileSize o) {
        return Long.compare(size, o.size);
    }
    
    @Dependency("Name. Used by String Parser by reflection discovered by method name.")
    @Dependency("Must be consistent with toStirng()")
    @Dependency("Supports different units to allow convenient search filters.")
    public static FileSize fromString(String s) {
        int i = 0;
        
        if (s.contains("B")) {
            int b = s.indexOf("B");
            String prefix = s.substring(b-1, b);
            boolean number = true;
            if("k".equals(prefix) || "K".equals(prefix)) {
                i=1;
                number = false;
            } else
            if("m".equals(prefix) || "M".equals(prefix)) {
                i=2;
                number = false;
            } else
            if("g".equals(prefix) || "G".equals(prefix)) {
                i=3;
                number = false;
            }
            s = s.substring(0, number ? b : b-1);
        }
        
        long number = Long.parseLong(s);
        int unit = (int) Math.pow(1024, i);
        return new FileSize(unit*number);
    }
}
