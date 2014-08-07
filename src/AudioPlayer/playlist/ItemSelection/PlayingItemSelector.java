/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package AudioPlayer.playlist.ItemSelection;

import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import java.util.Objects;
import java.util.function.Predicate;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import utilities.CyclicEnum;

/**
 * Determines playing items. Provides customizable item selector and also the
 * ability to filter items before the selection takes place.
 * <p>
 * @author Plutonium_
 */
public class PlayingItemSelector {
    private final ObservableList<PlaylistItem> playlist = PlaylistManager.getItems();
    private ItemSelector<PlaylistItem> selector = LoopMode.PLAYLIST.selector();
    private final SimpleObjectProperty<Predicate<PlaylistItem>> filter = new SimpleObjectProperty(null);
    
    /**
     * Sets the logic that determines how the next item should be selected.
     * @param selector it
     */
    public void setSelector(ItemSelector<PlaylistItem> selector) {
        Objects.requireNonNull(selector);
        this.selector = selector;
    }
    
    /**
     * Sets filter for item selection. Any item that doesnt test true for 
     * he specified filter will not be left out.
     * @param filter predicate to rule out items. Null will have the same effect
     * as filter that always tests true.
     */
    public void setFilter(Predicate<PlaylistItem> filter) {
        this.filter.set(filter);
    }
    
    /**
     * Observable and bindable  filter property. Contains the filter predicate 
     * for item selection. Also see {@link #setFilter(Predicate)}
     * @return 
     */
    public SimpleObjectProperty<Predicate<PlaylistItem>> filterProperty() {
        return filter;
    }
    
    /**
     * Returns next playing item according to current selector and filter.
     * @param current
     * @return 
     */
    public PlaylistItem getNext(PlaylistItem current) {
        if(filter.get() == null) return getNextNoFilter(current);
        else return getNext(filter.get(),current);
    }
    /**
     * Returns previous playing item according to current selector and filter.
     * @param current
     * @return 
     */
    public PlaylistItem getPrevious(PlaylistItem current) {
        if(filter.get() == null) return getPreviousNoFilter(current);
        else return getPrevious(filter.get(),current);
    }
    
    /**
     * Returns next playing item according to current selector. Convenience 
     * method.
     * @return 
     */
    public PlaylistItem getNextPlaying() {
        if(filter.get() == null) return getNextNoFilter(PlaylistManager.getPlayingItem());
        else return getNext(filter.get(),PlaylistManager.getPlayingItem());
    }
    /**
     * Returns previous playing item according to current selector. Convenience 
     * method.
     * @return 
     */
    public PlaylistItem getPreviousPlaying() {
        if(filter.get() == null) return getPreviousNoFilter(PlaylistManager.getPlayingItem());
        else return getPrevious(filter.get(),PlaylistManager.getPlayingItem());
    }
    
    /**
     * Returns playlist item following the one specified according to current
     * selection logic. No filter will be applied.
     * Returns null if specified item is null, does not exist in the playlist or
     * playlist is empty.
     * @return 
     */
    public PlaylistItem getNextNoFilter(PlaylistItem current) {
        if(current==null || !playlist.contains(current)) return null;
        return selector.next(playlist.size(), PlaylistManager.indexOfPlaying(), current, playlist);
    }
    /**
     * Returns playlist item preceding the one specified according to current
     * selection logic. No filter will be applied.
     * Returns null if specified item is null, does not exist in the playlist or
     * playlist is empty.
     * @return 
     */
    public PlaylistItem getPreviousNoFilter(PlaylistItem current) {
        if(current==null || !playlist.contains(current)) return null;
        return selector.previous(playlist.size(), PlaylistManager.indexOfPlaying(), current, playlist);
    }
    
    public PlaylistItem getNext(Predicate<PlaylistItem> filter, PlaylistItem current) {
        if(current==null || !playlist.contains(current)) return null;
        FilteredList<PlaylistItem> items = new FilteredList(playlist, filter);
        return selector.next(items.size(),items.indexOf(current), current, items);
    }
    public PlaylistItem getPrevious(Predicate<PlaylistItem> filter, PlaylistItem current) {
        if(current==null || !playlist.contains(current)) return null;
        FilteredList<PlaylistItem> items = new FilteredList(playlist, filter);
        return selector.previous(items.size(),items.indexOf(current), current, items);
    }  
    
/********************************** LOOP MODE *********************************/
    
    /**
     * Playback Loop mode type variable. Values are: PLAYLIST, SONG, OFF.
     */
    public static enum LoopMode implements CyclicEnum<LoopMode> {
        PLAYLIST {
            @Override public ItemSelector<PlaylistItem> selector() { 
                return new ItemSelector(
                    (size,index,current_item,playlist) -> {
                        if(size==0) return null;
                        if(current_item==null) return playlist.get(0);
                        else return playlist.get(Selection.decrIndex(size, index));
                    },
                    (size,index,current_item,playlist) -> {
                        if(size==0) return null;
                        if(current_item==null) return playlist.get(0);
                        else return playlist.get(Selection.incrIndex(size, index));
                });
            }
        },
        SONG {
            @Override public ItemSelector<PlaylistItem> selector() {
                Selection<PlaylistItem> sel = (size,index,current_item,playlist) -> {
                        if(current_item==null && size>0) return playlist.get(0);
                        return current_item;
                };
                return new ItemSelector(sel,sel);
            }
        },
        OFF {
            @Override public ItemSelector<PlaylistItem> selector() {
                return new ItemSelector(
                    (size,index,current_item,playlist) -> {
                        if(size==0 || index==0) return null;
                        if(current_item==null) return playlist.get(0);
                        return playlist.get(Selection.decrIndex(size, index));
                    },
                    (size,index,current_item,playlist) -> {
                        if(size==0 || index==size-1) return null;
                        if(current_item==null) return playlist.get(0);
                        return playlist.get(Selection.incrIndex(size, index));
                    });
            }
        },
        RANDOM {
            @Override public ItemSelector<PlaylistItem> selector() {
                Selection<PlaylistItem> sel = (size,index,current_item,playlist) -> {
                    if(size==0) return null;
                    // generate random index
                    int i = (int)Math.round(Math.random()*(size-1));
                    return playlist.get(i);
                };
                return new ItemSelector(sel,sel);
            }
        };

        /** @return {@link Selection}.*/
        public abstract ItemSelector<PlaylistItem> selector();
        
    }
}