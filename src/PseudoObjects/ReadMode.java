
package PseudoObjects;

import util.Util;
import static util.Util.*;
import static util.Util.mapEnumConstant;

/**
 * 
 */
public enum ReadMode {
    PLAYING,
    SELECTED_PLAYLIST,
    SELECTED_LIBRARY,
    SELECTED_ANY,
    ANY,
    CUSTOM;
    
    private ReadMode() {
        mapEnumConstant(this, Util::enumToHuman);
    }
}
