/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.widget.feature;

import AudioPlayer.Item;

import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.singletonList;

import java.util.List;

import static util.dev.Util.noØ;

/**
 * Capable of reading data to song tags
 * 
 * @author Plutonium_
 */
@Feature(
  name = "Song metadata reader", 
  description = "Capable of displaying song metadata", 
  type = SongReader.class
)
public interface SongReader {
    
    /**
     * Passes item into this reader.
     * 
     * @param item or null to display no data if supported
     * @see #read(java.util.List)  
     */    
    public default void read(Item item) {
        noØ(item);
        read(item==null ? EMPTY_LIST : singletonList(item));
    };
    
    /**
     * Passes items into this reader.
     * Dispays metadata on items and displays them.
     * 
     * @param items list pf items or empty list to display no data if supported.
     * Non null.
     */    
    public void read(List<? extends Item> items);
}
