package sp.it.pl.audio.playlist.sequence;

import java.util.ArrayList;
import java.util.List;
import sp.it.pl.audio.playlist.PlaylistSong;
import sp.it.util.access.Values;
import static sp.it.util.dev.FailKt.noNull;

/**
 * Determines playing items. Provides customizable item selector and also the
 * ability to filter items before the selection takes place.
 */
public class PlayingSequence {
    private PlaylistSongSelector<PlaylistSong> selector = LoopMode.PLAYLIST.selector();
    private static final List<PlaylistSong> history = new ArrayList<>();
    private static int history_pos = -1;

    /**
     * Sets the logic that determines how the next item should be selected.
     */
    public void setSelector(PlaylistSongSelector<PlaylistSong> selector) {
        noNull(selector);
        this.selector = selector;
    }

    /**
     * Returns item from the list following the specified item according to
     * selection logic. If specified item is null or not in the list, first item
     * is returned or null if the list is empty.
     *
     * @return next item
     */
    public PlaylistSong getNext(PlaylistSong current, List<PlaylistSong> playlist) {
        if (current==null || !playlist.contains(current))
            return playlist.isEmpty() ? null : playlist.getFirst();
        else
            return selector.next(playlist.size(), playlist.indexOf(current), current, playlist);
    }

    /**
     * Returns item from the list preceding the specified item according to
     * selection logic. If specified item is null or not in the list, first item
     * is returned or null if the list is empty.
     *
     * @return previous item
     */
    public PlaylistSong getPrevious(PlaylistSong current, List<PlaylistSong> playlist) {
        if (current==null || !playlist.contains(current))
            return playlist.isEmpty() ? null : playlist.getFirst();
        else
            return selector.previous(playlist.size(), playlist.indexOf(current), current, playlist);
    }

    /** Playback looping mode. */
    public enum LoopMode {
        OFF {
            @Override
            public PlaylistSongSelector<PlaylistSong> selector() {
                return new PlaylistSongSelector<>(
                        (size, index, current_item, playlist) -> {
                            if (size==0 || index==0) return null;
                            if (current_item==null) return playlist.getFirst();
                            return playlist.get(Values.decrIndex(size, index));
                        },
                        (size, index, current_item, playlist) -> {
                            if (size==0 || index==size - 1) return null;
                            if (current_item==null) return playlist.getFirst();
                            return playlist.get(Values.incrIndex(size, index));
                        });
            }
        },
        SONG {
            @Override
            public PlaylistSongSelector<PlaylistSong> selector() {
                Selection<PlaylistSong> sel = (size, index, current_item, playlist) -> {
                    if (current_item==null && size>0) return playlist.getFirst();
                    return current_item;
                };
                return new PlaylistSongSelector<>(sel, sel);
            }
        },
        RANDOM {
            @Override
            public PlaylistSongSelector<PlaylistSong> selector() {
                return new PlaylistSongSelector<>(
                        (size, index, current_item, playlist) -> {
                            if (size==0) return null;
                            // generate random index
                            history_pos--;
                            if (history_pos>0 && history_pos<history.size() - 1) {
                                return history.get(history_pos);
                            }
                            int i = (int) Math.round(Math.random()*(size - 1));
                            return playlist.get(i);
                        },
                        (size, index, current_item, playlist) -> {
                            if (size==0) return null;
                            if (history_pos>-1 && history_pos<history.size() - 2) {
                                history_pos++;
                                return history.get(history_pos);
                            }
                            // generate random index
                            int i = (int) Math.round(Math.random()*(size - 1));
                            PlaylistSong p = playlist.get(i);
                            history_pos++;
                            history.add(history_pos, p);
                            return p;
                        }
                );
            }
        },
        PLAYLIST {
            @Override
            public PlaylistSongSelector<PlaylistSong> selector() {
                return new PlaylistSongSelector<>(
                        (size, index, current_item, playlist) -> {
                            if (size==0) return null;
                            if (current_item==null) return playlist.getFirst();
                            else return playlist.get(Values.decrIndex(size, index));
                        },
                        (size, index, current_item, playlist) -> {
                            if (size==0) return null;
                            if (current_item==null) return playlist.getFirst();
                            else return playlist.get(Values.incrIndex(size, index));
                        });
            }
        };

        /** @return {@link Selection}. */
        public abstract PlaylistSongSelector<PlaylistSong> selector();
    }
}