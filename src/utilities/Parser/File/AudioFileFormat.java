/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities.Parser.File;

import AudioPlayer.playlist.Item;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.stage.FileChooser;

/**
 * All audio file formats known and supported by application except for UNKNOWN that
 * servers for all the other file types.
 * Any operation with unsupported file will produce undefined behavior. They
 * should be discovered and ignored.
 */
public enum AudioFileFormat {
    mp3,
    ogg,
    flac,
    wav,
    UNKNOWN;
    
    /**
     * Checks whether this format supported audio format. Unsupported formats
     * dont get any official support for any of the app's features and by default
     * are ignored.
     * @return true if supported, false otherwise
     */
    public boolean isSupported() {
        return this == mp3 || this == wav;
    }
    
    public String toExt() {
        return "*." + toString();
    }
    
    public FileChooser.ExtensionFilter toExtFilter() {
        return new FileChooser.ExtensionFilter(toString(), toExt());
    }
    
    
    public static boolean isSupported(AudioFileFormat f) {
        return f.isSupported();
    }
    /**
     * Checks whether the item is of supported audio format. Unsupported file
     * dont get any official support for any of the app's features and by default
     * are ignored.
     * @param item
     * @return true if supported, false otherwise
     */
    public static boolean isSupported(Item item) {
        return of(item.getURI()).isSupported();
    }
    
    /**
     * Checks whether the file has supported audio format. Unsupported file
     * dont get any official support for any of the app's features and by default
     * are ignored.
     * @param uri
     * @return true if supported, false otherwise
     * @throws NullPointerException if param is null
     */
    public static boolean isSupported(URI uri) {
        Objects.requireNonNull(uri);
        return of(uri).isSupported();
    }
    
    /**
     * Equivalent to {@link #isSupported(java.io.URI)} using file.toURI().
     * @param file
     * @return 
     * @throws NullPointerException if param is null
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
     * Labels file as one of the audio file types the application recognizes.
     * @param uri
     * @return 
     */
    public static AudioFileFormat of(URI uri) {
        return of(uri.getPath());
    }
    
    /**
     * Labels file as one of the audio file types the application recognizes.
     * @param uri
     * @return 
     */
    public static AudioFileFormat of(String path) {
        String suffix = FileUtil.getSuffix(path);
        for(AudioFileFormat f: values())
            if (suffix.equals(f.toString()))
                return f;
        return UNKNOWN;
    }    
    
/******************************************************************************/
    
    /** Writes up list of all supported values. */    
    public static String supportedExtensionsS() {
        String out = "";
        for(String ft: exts())
            out = out + ft +"\n";
        return out;
    }
    
    public static List<AudioFileFormat> supportedValues() {
        List<AudioFileFormat> ext = new ArrayList();
        for(AudioFileFormat format: values()) {
            if (format.isSupported())
                ext.add(format);
        }
        return ext;
    }
    
    public static FileChooser.ExtensionFilter filter() {
        return new FileChooser.ExtensionFilter("Audio files", exts());
    }
    
    
    // List of supported extension strings in the format: '*.extension'
    private static List<String> exts() {
        List<String> ext = new ArrayList();
        for(AudioFileFormat format: supportedValues()) {
            if (format.isSupported())
                ext.add(format.toExt());
        }
        return ext;
    }
}

