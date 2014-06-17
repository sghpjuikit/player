/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * All image filetypes known and supported by application except for UNKNOWN that
 * serves as a marker for all the other file types.
 * Any operation with unsupported file will produce undefined behavior. 
 */
public enum ImageFileFormat {
    jpg,
    bmp,
    png,
    UNKNOWN;
    
    public boolean isSupported() {
        return this != UNKNOWN;
    }
    /**
     * Checks whether the format is supported image format. Unsupported formats
     * dont get any official support for any of the app's features and by default
     * are ignored.
     * @param f
     * @return true if supported, false otherwise
     */
    public static boolean isSupported(ImageFileFormat f) {
        return f.isSupported();
    }
    /**
     * Checks whether the file has supported image format. Unsupported formats
     * dont get any official support for any of the app's features and by default
     * are ignored.
     * @param file
     * @return true if supported, false otherwise
     */
    public static boolean isSupported(File file) {
        if (file == null) return false;
        return formatOf(file.toURI()).isSupported();
    }
    
    /**
     * Labels file as one of the image file types the application recognizes.
     * @param uri
     * @return 
     */
    public static ImageFileFormat formatOf(URI uri) {
        ImageFileFormat type = UNKNOWN;
        for(ImageFileFormat f: values())
            if (uri.getPath().endsWith(f.toString()))
                type = f;
        return type;
    }
    
/******************************************************************************/
    
    public static List<String> extensions() {
        List<String> ext = new ArrayList<>();
        for(ImageFileFormat format: values()) {
            if (format.isSupported())
                ext.add("*." + format.toString());
        }
        return ext;
    }
    /**
     * Writes up list of all image files recognized and supported by the application.
     * @return 
     */    
    public static String listSupportedImageFormats() {
        String out = "";
        for(String ft: extensions())
            out = out + ft +"\n";
        return out;
    }
}