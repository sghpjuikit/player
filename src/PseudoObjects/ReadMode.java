/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package PseudoObjects;

/**
 * How is application subcomponent supposed to pick upinformation about playlist
 * item it is centered about.
 *  - PLAYLIST_SELECTED = read data of item selected in playlist
 *  - PLAYLIST_PLAYING = read data of item currently playing
 *  - LIBRARY_SELECTED = read data of item selected in media library
 *  - CUSTOM = read data of item inserted manually (drag & drop, etc)
 */
public enum ReadMode {
    PLAYLIST_SELECTED,
    PLAYING,
    LIBRARY_SELECTED,
    CUSTOM;
    
    public static ReadMode fromString(String string) {
        if (string == null) { 
            return null;
        } else if (string.equals("PLAYLIST_SELECTED")) {
            return ReadMode.PLAYLIST_SELECTED;
        } else if (string.equals("PLAYLIST_PLAYING")) {
            return ReadMode.PLAYING;
        } else if (string.equals("LIBRARY_SELECTED")) {
            return ReadMode.LIBRARY_SELECTED;
        } else if (string.equals("CUSTOM")) {
            return ReadMode.CUSTOM;  
        } else {
            return null;
        }
    }
}
