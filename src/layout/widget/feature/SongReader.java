/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package layout.widget.feature;

import java.util.List;

import audio.Item;

import static java.util.Collections.singletonList;
import static util.dev.Util.noØ;
import static util.functional.Util.listRO;

/**
 * Capable of reading data to song tags
 * 
 * @author Martin Polakovic
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
    default void read(Item item) {
        noØ(item);
        read(item==null ? listRO() : singletonList(item));
    }
    
    /**
     * Passes items into this reader.
     * Displays metadata on items and displays them.
     * 
     * @param items list pf items or empty list to display no data if supported.
     * Non null.
     */    
    void read(List<? extends Item> items);
}
