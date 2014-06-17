/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities;

import AudioPlayer.playlist.Item;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

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
        return this != UNKNOWN && this != flac && this != ogg;
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
        return get(item.getURI()).isSupported();
    }
    /**
     * Checks whether the file has supported audio format. Unsupported file
     * dont get any official support for any of the app's features and by default
     * are ignored.
     * @param file
     * @return true if supported, false otherwise
     */
    public static boolean isSupported(File file) {
        return get(file.toURI()).isSupported();
    }
    
    /**
     * Labels file as one of the audio file types the application recognizes.
     * @param uri
     * @return 
     */
    public static AudioFileFormat get(URI uri) {
        AudioFileFormat type = UNKNOWN;
        for(AudioFileFormat f: values())
            if (uri.getPath().endsWith(f.toString()))
                type = f;
        return type;
    }    
    
/******************************************************************************/
    
    public static List<String> extensions() {
        List<String> ext = new ArrayList<>();
        for(AudioFileFormat format: values())
            if (format.isSupported())
                ext.add("*." + format.toString());
        return ext;
    }
    
    /**
     * Writes up list of all audio files recognized and supported by the application.
     * @return 
     */
    public static String listSupportedAudioFormats() {
        String out = "";
        for(String ft: extensions())
            out = out + ft +"\n";
        return out;
    }
    
}

