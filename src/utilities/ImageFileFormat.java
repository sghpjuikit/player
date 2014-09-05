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
import java.util.Objects;

/**
 * All image filetypes known and supported by application except for UNKNOWN that
 * serves as a marker for all the other file types.
 * Any operation with unsupported file will produce undefined behavior. 
 */
public enum ImageFileFormat {
    jpg,
    bmp,
    png,
    gif,
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
     * @param uri
     * @return true if supported, false otherwise
     */    
    public static boolean isSupported(URI uri) {
        Objects.requireNonNull(uri);
        return of(uri).isSupported();
    }
    
    /**
     * Equivalent to {@link #isSupported(java.io.URI)} using file.toURI().
     * @param file
     * @return 
     */
    public static boolean isSupported(File file) {
        Objects.requireNonNull(file);
        return of(file.toURI()).isSupported();
    }
    
    /**
     * Equivalent to {@link #isSupported(java.io.URI)} using URI.create(url). If
     * the provided url can not be used to construct an URI, false is returned.
     * On the other hand, if true is returned the validity of the url is guaranteed.
     * @param url
     * @return 
     * @throws NullPointerException if param is null
     */
    public static boolean isSupported(String url) {
        try {
            URI uri = URI.create(url);
            return of(uri).isSupported();
        } catch(IllegalArgumentException e) {
            return false;
        }
    }
    /**
     * Labels file as one of the image file types the application recognizes.
     * @param uri
     * @return 
     */
    public static ImageFileFormat of(URI uri) {
        ImageFileFormat type = UNKNOWN;
        for(ImageFileFormat f: values())
            if (uri.getPath().endsWith(f.toString()))
                type = f;
        return type;
    }
    
/******************************************************************************/
    
    /**
     * List of supported extension strings in the format: '*.extension'
     * @return 
     */
    public static List<String> extensions() {
        List<String> ext = new ArrayList<>();
        for(ImageFileFormat format: values()) {
            if (format.isSupported())
                ext.add("*." + format.toString());
        }
        return ext;
    }
    
    /**
     * List of supported extension strings in the format: 'extension'
     * @return 
     */
    public static List<String> extensionsSimple() {
        List<String> ext = new ArrayList<>();
        for(ImageFileFormat format: values()) {
            if (format.isSupported())
                ext.add(format.toString());
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
    
    public static List<ImageFileFormat> valuesSupported() {
        List<ImageFileFormat> ext = new ArrayList<>();
        for(ImageFileFormat format: values()) {
            if (format.isSupported())
                ext.add(format);
        }
        return ext;
    }
}