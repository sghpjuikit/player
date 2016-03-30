/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package audio.playlist.sequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import audio.playlist.PlaylistItem;
import util.access.CyclicEnum;

import static util.access.SequentialValue.decrIndex;
import static util.access.SequentialValue.incrIndex;

/**
 * Determines playing items. Provides customizable item selector and also the
 * ability to filter items before the selection takes place.
 * <p/>
 * @author Martin Polakovic
 */
public class PlayingSequence {
    private ItemSelector<PlaylistItem> selector = LoopMode.PLAYLIST.selector();
    private static List<PlaylistItem> history = new ArrayList<>();
    private static int history_pos = -1;

    /**
     * Sets the logic that determines how the next item should be selected.
     * @param selector it
     */
    public void setSelector(ItemSelector<PlaylistItem> selector) {
        Objects.requireNonNull(selector);
        this.selector = selector;
    }

    /**
     * Returns item from the list following the specified item according to
     * selection logic. If specified item is null or not in the list, first item
     * is returned or null if the list is empty.
     *
     * @return next item
     */
    public PlaylistItem getNext(PlaylistItem current, List<PlaylistItem> playlist) {
        // if none playing, return first
        if(current==null || !playlist.contains(current)) {
            return playlist.isEmpty() ? null : playlist.get(0);
        }
        // else calculate
        PlaylistItem p = selector.next(playlist.size(), playlist.indexOf(current), current, playlist);
        return p;
    }
    /**
     * Returns item from the list preceding the specified item according to
     * selection logic. If specified item is null or not in the list, first item
     * is returned or null if the list is empty.
     *
     * @return previous item
     */
    public PlaylistItem getPrevious(PlaylistItem current, List<PlaylistItem> playlist) {
        // if none playing, return first
        if(current==null || !playlist.contains(current)) {
            return playlist.isEmpty() ? null : playlist.get(0);
        }
        // else calculate
        return selector.previous(playlist.size(), playlist.indexOf(current), current, playlist);
    }

/********************************** LOOP MODE *********************************/

    /**
     * Playback Loop mode type variable. Values are: PLAYLIST, SONG, OFF.
     */
    public static enum LoopMode implements CyclicEnum<LoopMode> {
        PLAYLIST {
            @Override public ItemSelector<PlaylistItem> selector() {
                return new ItemSelector<>(
                    (size,index,current_item,playlist) -> {
                        if(size==0) return null;
                        if(current_item==null) return playlist.get(0);
                        else return playlist.get(decrIndex(size, index));
                    },
                    (size,index,current_item,playlist) -> {
                        if(size==0) return null;
                        if(current_item==null) return playlist.get(0);
                        else return playlist.get(incrIndex(size, index));
                });
            }
        },
        SONG {
            @Override public ItemSelector<PlaylistItem> selector() {
                Selection<PlaylistItem> sel = (size,index,current_item,playlist) -> {
                        if(current_item==null && size>0) return playlist.get(0);
                        return current_item;
                };
                return new ItemSelector<>(sel,sel);
            }
        },
        OFF {
            @Override public ItemSelector<PlaylistItem> selector() {
                return new ItemSelector<>(
                    (size,index,current_item,playlist) -> {
                        if(size==0 || index==0) return null;
                        if(current_item==null) return playlist.get(0);
                        return playlist.get(decrIndex(size, index));
                    },
                    (size,index,current_item,playlist) -> {
                        if(size==0 || index==size-1) return null;
                        if(current_item==null) return playlist.get(0);
                        return playlist.get(incrIndex(size, index));
                    });
            }
        },
        RANDOM {
            @Override public ItemSelector<PlaylistItem> selector() {
//                Selection<PlaylistItem> sel = (size,index,current_item,playlist) -> {
//                    if(size==0) return null;
//                    // generate random index
//                    int i = (int)Math.round(Math.random()*(size-1));
//                    return playlist.get(i);
//                };
//                return new ItemSelector(sel,sel);
                return new ItemSelector<>(
                    (size,index,current_item,playlist) -> {
                        if(size==0) return null;
                        // generate random index
                        history_pos--;
                        System.out.println("prev " + history_pos + " " + history.size());
                        for(int k = 0; k<history.size(); k++) {
                            System.out.println(k + " " + history.get(k));
                        }
                        if(history_pos>0 && history_pos<history.size()-1) {
                            return history.get(history_pos);
                        }
                        int i = (int)Math.round(Math.random()*(size-1));
                        return playlist.get(i);
                    },
                    (size,index,current_item,playlist) -> {
                        System.out.println("next " + history_pos + " " + history.size());
                        if(size==0) return null;
                        for(int k = 0; k<history.size(); k++) {
                            System.out.println(k + " " + history.get(k));
                        }
                        if(history_pos>-1 && history_pos<history.size()-2) {
                            history_pos++;
                            PlaylistItem p = history.get(history_pos);
                            return history.get(history_pos);
                        }
                        // generate random index
                        int i = (int)Math.round(Math.random()*(size-1));
                        PlaylistItem p = playlist.get(i);
                        history_pos++;
                        history.add(history_pos,p);
                        return p;
                    }
                );
            }
        };

        /** @return {@link Selection}.*/
        public abstract ItemSelector<PlaylistItem> selector();
    }
}