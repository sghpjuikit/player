/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.tagging;

import java.io.File;
import jdk.nashorn.internal.ir.annotations.Immutable;

/**
 * Simple class for file size handling.
 * <p>
 * Provides human readable text output and handling of unknown size
 * 
 * @author uranium
 */
@Immutable
public final class FileSize {
    private final long size;
    
    /**
     * Creates filesize set to size of the specified file. If the value can not
     * be determined it will be set to 0l. 
     * @param f non null file to read filesize of
     * @throws NullPointerException if param null
     */
    public FileSize(File f) {
        size = f.length();
    }
    
    /**
     * Creates filesize set to specified value.
     * @param bytes amount of bytes as a size value or 0l if unknown.
     * @throws IllegalArgumentException if param negative
     */
    public FileSize(long bytes) {
        if(bytes<0) 
            throw new IllegalArgumentException("Filesize can not be negative");
        
        size = bytes;
    }
    
    /**
     * @return file size in bytes or 0L if unknown
     */
    public long inBytes() {
        return size;
    }
    
    /**
     * Returns human readable file size text.
     * <p>
     * Displays "Unknown" if value 0 - which is an 'unknown' value.
     * Appends byte units after the value.
     * <p>
     * Most appropriate unit prefix is calculated and the value converted. The
     * text value granularity is 0.01 of the used unit. Example 3.56
     * @return string representation of the object
     */
    @Override
    public String toString() {
        if(size == 0l) return "Unknown";
        
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
}
