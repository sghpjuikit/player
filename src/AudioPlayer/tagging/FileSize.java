/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.tagging;

import java.io.File;
import jdk.nashorn.internal.ir.annotations.Immutable;

/**
 *
 * @author uranium
 */
@Immutable
public final class FileSize {
    private final long size;
    
    public FileSize(File f) {
        size = f.length();
    }
    
    /**
     * @return size in Bytes
     */
    public long getValue() {
        return size;
    }
    
    /**
     * Appends units after the value.
     * Handles conversion between kB to MB etc...
     * @return string representation of the object
     */
    @Override
    public String toString() {
        double kB = (size / 1024.0);
        double MB = (size / (1024.0*1024.0));
        double GB = (size / (1024.0*1024.0*1024.0));
        
        if (MB>=1024)
            return String.format("%.2f MB", GB);//String.valueOf(MB) + "MB";
        else if (MB>=1)
            return String.format("%.2f MB", MB);//String.valueOf(MB) + "MB";
        else
            return String.format("%.2f kB", kB);//String.valueOf(kB) + "kB";
    }    
}
