/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package util.Parser.File;

import AudioPlayer.playlist.Item;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
    public boolean isSupported(Use use) {
        return this!=UNKNOWN && !((this==flac || this==ogg) && use==Use.PLAYBACK);
    }
    
    public String toExt() {
        return "*." + toString();
    }
    
    public FileChooser.ExtensionFilter toExtFilter() {
        return new FileChooser.ExtensionFilter(toString(), toExt());
    }
    
    /**
     * Checks whether the item is of supported audio format. Unsupported file
     * dont get any official support for any of the app's features and by default
     * are ignored.
     * @param item
     * @return true if supported, false otherwise
     */
    public static boolean isSupported(Item item, Use use) {
        return of(item.getURI()).isSupported(use);
    }
    
    /**
     * Checks whether the file has supported audio format. Unsupported file
     * dont get any official support for any of the app's features and by default
     * are ignored.
     * @param uri
     * @return true if supported, false otherwise
     * @throws NullPointerException if param is null
     */
    public static boolean isSupported(URI uri, Use use) {
        Objects.requireNonNull(uri);
        return of(uri).isSupported(use);
    }
    
    /**
     * Equivalent to {@link #isSupported(java.io.URI)} using file.toURI().
     * @param file
     * @return 
     * @throws NullPointerException if param is null
     */
    public static boolean isSupported(File file, Use use) {
        Objects.requireNonNull(file);
        return of(file.toURI()).isSupported(use);
    }
    
    /**
     * Equivalent to {@link #isSupported(java.io.URI)} using URI.create(url). If
     * the provided url can not be used to construct an URI, false is returned.
     * On the other hand, if true is returned the validity of the url is guaranteed.
     * @param url
     * @return 
     * @throws NullPointerException if param is null
     */
    public static boolean isSupported(String url, Use use) {
        try {
            URI uri = URI.create(url);
            return of(uri).isSupported(use);
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
    public static String supportedExtensionsS(Use use) {
        String out = "";
        for(String ft: exts(use))
            out = out + ft +"\n";
        return out;
    }
    
    public static List<AudioFileFormat> supportedValues(Use use) {
        return Stream.of(values()).filter(f->f.isSupported(use)).collect(Collectors.toList());
    }
    
    public static FileChooser.ExtensionFilter filter(Use use) {
        return new FileChooser.ExtensionFilter("Audio files", exts(use));
    }
    
    
    // List of supported extension strings in the format: '*.extension'
    private static List<String> exts(Use use) {
        return supportedValues(use).stream().map(f->f.toExt()).collect(Collectors.toList());
    }
    
    
    public static enum Use {
        APP,
        PLAYBACK,
        DB;
    }
}

