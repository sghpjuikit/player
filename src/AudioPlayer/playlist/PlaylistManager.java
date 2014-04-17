
package AudioPlayer.playlist;

import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import Configuration.IsAction;
import Configuration.Configurable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.util.Duration;
import utilities.Util;

/**
 * Provides unified handling to everything playlist related in the application
 */
public class PlaylistManager implements Configurable{
    private static final ObservablePlaylist playlist = new ObservablePlaylist(Player.state.playlist.playlist);
    private static final ReadOnlyObjectWrapper<PlaylistItem> playingItem = new ReadOnlyObjectWrapper<>();
    private static ObjectProperty<TableViewSelectionModel<PlaylistItem>> selectionModel = new SimpleObjectProperty<>();
    private static final ObservableList<PlaylistItem> selectedItems = FXCollections.observableArrayList();
    
    public static void initialize() {
        // initialize selectionModel (with kind of weird workaround)
        TableView<PlaylistItem> t = new TableView<>();
        selectionModel = t.selectionModelProperty();
    }
    
    /**
     * Initialize state from last session
     */
    public static void changeState() {
        addItems(Player.state.playlist.playlist);
        // fires metadata loading update (needed)  < ?
        playingItem.set(getItem(Player.state.playlist.playingItem.get()));
    }
    
/******************************************************************************/
    
    /**
     * Returns played item. Change to returned object is allowed but will not
     * be reflected. Consider it read only.
     * @return clone of played item or null if no item is being played.
     */
    public static PlaylistItem getPlayingItem() {
        return PlaylistItem.clone(playingItem.get());
    }
    /**
     * Internal use only !
     * @note: this needs to be forbidden access to from external sources.
     * @param item 
     */
    public static void setPlayingItem(PlaylistItem item) {
        playingItem.set(item);
    }
    /**
     * Returns true if some item on the playlist is playing.
     * @return true if any item is playing.
     */
    public static boolean isItemPlaying() {
        return playingItem.get() != null;
    }
    /**
     * Returns index of currently playing item.
     * @return index of playing item. -1 if no item is playing
     */
    public static int indexOfPlaying() {
        return isItemPlaying() ? playlist.indexOf(playingItem.get()) : -1;
    }
    /**
     * @return read only property of played item. Use to bind and observe. 
     */
    public static ReadOnlyObjectProperty<PlaylistItem> playingItemProperty() {
        return playingItem.getReadOnlyProperty();
    }
    /**
     * Returns playlist length. Changes to returned object are allowed but will
     * not  be reflected. Consider it read only.
     * @return length - total duration of the playlist.
     */
    public static Duration getLength() {
        return playlist.getLength();
    }   
    /**
     * Returns bindable read only property of total duration.
     * @return bindable total playlist duration playlistproperty.
     */
    public static ReadOnlyProperty<Duration> lengthProperty() {
        return playlist.lengthProperty();
    }
    /**
     * Returns selected item. Null if no item selected. Changes to returned
     * objects are allowed but wont be reflected. Consider it read only.
     * @return clone of selected item.
     */
    public static PlaylistItem getSelectedItem() {
        return PlaylistItem.clone(selectionModel.get().getSelectedItem());
    }
    /**
     * Returns bindable read only property of last selected item.
     * @return bindable selected item property.
     */
    public static ReadOnlyObjectProperty<PlaylistItem> selectedItemProperty() {
        return selectionModel.get().selectedItemProperty();
    }       
    /**
     * @return bindable unmodifiable observable list of selected items.
     */
    public static ObservableList<PlaylistItem> getSelectedItems() {
//        return FXCollections.unmodifiableObservableList(selectedItems);
        return selectedItems;
    }
    /**
     * Clears the ObservableList and add all elements from the collection.
     * @param to_select 
     * @throws java.lang.NullPointerException - if the specified collection
     * contains one or more null elements
     */
    public static void setSelectedItems(List<PlaylistItem> to_select) {
        selectedItems.setAll(to_select);
    }
    /**
     * Bind to table to get selecting functionality for custom playlist tables.
     * bind this bidirectionally to get your table work as playlist table easily   
     * @return 
     */
    public static ObjectProperty<TableView.TableViewSelectionModel<PlaylistItem>> selectionModelProperty() {
        return selectionModel;
    }
    
/******************************************************************************/
    
    /**
     * Returns the first playlist item passing the equalSource()=true for specified item.
     * Use this method to look for item with URI by wrapping it into SimpleItem
     * object before passing it as argument.
     * @param item
     * @return PlaylistItem with specified source. Null if no result, param item is
     * null or playlist is empty.
     */
    public static PlaylistItem getItem(Item item) {
        return playlist.getItem(item);
    }
    /**
     * Returns the index of the first occurrence of the specified element in this list,
     * or -1 if this list does not contain the element. More formally, returns the 
     * lowest index i such that (o==null ? get(i)==null : o.equals(get(i))), or -1 if
     * there is no such index.
     * @param item element to search for
     * @return item the index of the first occurrence of the specified element 
     * in this list, or -1 if this list does not contain the element
     */
    public static int getIndexOf(PlaylistItem item) {
        return playlist.indexOf(item);
    }
    /**
     * Returns index of the first item in playlist with same source as parameter.
     * Method utilizes item.equalsSource() equality test.
     * @param item
     * @return item index. -1 if not in playlist.
     */
    public static int getIndexOf(Item item) {
        return playlist.indexOf(item);
    }
    /**
     * Returns item following the one specified. If the item is last item
     * of the playlist, 1st item is returned, making playlist appear to have cyclic
     * nature.
     * 
     * @param item
     * @return Next item. Null if specified item is null, does not exist in the
     * playlist or playlist is empty.
     * @throws NullPointerException when param null.
     */
    public static PlaylistItem getNextItem(PlaylistItem item) {
        return playlist.getNextItem(item);
    }
    /**
     * Returns playlist item following the one specified. If the item is last item
     * of the playlist, 1st item is returned.
     * Returns null if specified item is null, does not exist in the playlist or
     * playlist is empty.
     * @return 
     */
    public static PlaylistItem getNextPlayingItem() {
        return getNextItem(playingItem.get());
    }
    /**
     * @return all items of playlist. 
     */
    public static ObservableList<PlaylistItem> getItems() {
        return playlist.list();
    }
    
    /**
     * Adds new item to end of this playlist, based on the url (String). Use this method
     * for  URL based Items.
     * @param url
     * @throws NullPointerException when param null.
     */
    public static void addUrl(String url) {
        playlist.addUrl(url);
    }
    /**
     * Adds new item to specified index of playlist.
     * Dont use for physical files..
     * @param url
     * @param at
     * @throws NullPointerException when param null.
     */
    public static void addUrl(String url, int at) {
        playlist.addUrls(url, at);
    }
    /**
     * Adds specified item to end of this playlist. 
     * @param item
     * @throws NullPointerException when param null.
     */
    public static void addUri(URI item) {
        playlist.addUri(item);
    }
    /**
     * Adds items at the end of this playlist and updates them if necessary.
     * Adding produces immediate response for all items. Updating will take time
     * and take effect the moment it is applied one by one.
     * @param uris
     * @throws NullPointerException when param null.
     */
    public static void addUris(List<URI> uris) {
        playlist.addUris(uris);
    }
    /**
     * Adds new items at the end of this playlist and updates them if necessary.
     * Adding produces immediate response for all items. Updating will take time
     * and take effect the moment it is applied one by one.
     * @param uris
     * @param at Index at which items will be added. Out of bounds index will
     * be converted:
     * index < 0     --> 0(first)
     * index >= size --> size (last item)
     * @throws NullPointerException when param null.
     */
    public static void addUris(List<URI> uris, int at) {
        playlist.addUris(uris, at);
    }
    /**
     * Adds all specified items to end of this playlist.
     * @param items
     * @throws NullPointerException when param null.
     */
    public static void addItems(List<? extends Item> items) {
        playlist.addItems(items);
    }
    /**
     * Adds all specified items to specified position of this playlist.
     * @param items
     * @param _at
     * Index at which items will be added. Out of bounds index will be converted:
     * index < 0     --> 0(first)
     * index >= size --> size (last item)
     * @throws NullPointerException when param null.
     */
    public static void addItems(List<? extends Item> items, int _at) {
        playlist.addItems(items, _at);
    }
    /**
     * Adds all items on the specified playlist at the end playlist.
     * @param p
     * @throws NullPointerException when param null.
     */
    public static void addPlaylist(Playlist p) {
        playlist.addPlaylist(p);
    }
    /**
     * Adds all items on the specified playlist to the specified position of this
     * playlist.
     * @param p
     * @param at Index at which items will be added. Out of bounds index will be
     * converted:
     * index < 0     --> 0(first)
     * index >= size --> size (last item)
     * @throws NullPointerException when param null.
     */
    public static void addItems(Playlist p, int at) {
        playlist.addPlaylist(p, at);
    }
    
    /**
     * Removes specified item from this playlist. Does nothing if null or not on
     * playlist.
     * @param item 
     */
    public static void removeItem(PlaylistItem item) {
        playlist.removeItem(item);
        selectionModel.get().clearSelection(); selectedItems.clear();
    }
    
    /**
     * Removes all playlist items in the specified List from playlist.
     */
    public static void removeItems(List<PlaylistItem> items) {
        playlist.removeItems(items);
        selectionModel.get().clearSelection(); selectedItems.clear();
    }
    
    /**
     * Removes all playlist items. The playlist remains empty after this method
     * is invoked.
     */
    public static void removeAllItems() {
        playlist.clear();
        selectionModel.get().clearSelection(); selectedItems.clear();
    }
    /**
     * Removes all selected items from playlist. Does nothing if playlist is empty.
     */
    public static void removeSelectedItems() {
        if ( playlist.isEmpty()) { return; }
        
        // it is necessary to clone the selected indexes not just items = selected;
        // dont change addAll for items=...getSelectedIndeces();
        List<PlaylistItem> items = new ArrayList<>();
        items.addAll(selectionModel.get().getSelectedItems());

        playlist.removeItems(items);
        selectionModel.get().clearSelection(); selectedItems.clear();
    }
    /**
     * Removes all unselecteed items from playlist. Does nothing if playlist is empty.
     */
    public static void removeUnselectedItems() {
        if ( playlist.isEmpty()) { return; }
        
        // it is necessary to clone the selected indexes not just items = selected;
        // dont change addAll for items=...getSelectedIndeces();
        List<PlaylistItem> items = new ArrayList<>();
        items.addAll(selectionModel.get().getSelectedItems());
        
        playlist.retainItems(items);
        selectionModel.get().clearSelection(); selectedItems.clear();
    }
    /**
     * Removes all corrupt items from this playlist.
     */
    public static void removeCorrupt() {
        playlist.removeCorrupt();
        selectionModel.get().clearSelection(); selectedItems.clear();
    }
    /**
     * Removes all duplicates of all items of this playlist. After this method
     * is called, there will be no duplicates on the list. Exactly one of each
     * unique item will be left.
     * Duplicates/not unique items are any two item for which equalsSource()
     * returns true.
     */
    public static void removeDuplicates() {
        selectionModel.get().clearSelection(); selectedItems.clear();
        playlist.removeDuplicates();
    }
    /**
     * Duplicates the item if it is in playlist. If it isnt, does nothing.
     * Duplicate will appear on the next index following the original.
     * @param item 
     */
    public static void duplicateItem(PlaylistItem item) {
        playlist.duplicateItem(item);
        selectionModel.get().clearSelection(); selectedItems.clear();
    }

    /**
     * Duplicates the items if they are in playlist. If they arent, does nothing.
     * Duplicates will appear on the next index following the last items's index.
     * @param items items to duplicate
     */
    public static void duplicateItemsAsGroup(List<PlaylistItem> items) {
        playlist.duplicateItemsAsGroup(items);
        selectionModel.get().clearSelection(); selectedItems.clear();
    }
    
    /**
     * Duplicates the items if they are in playlist. If they arent, does nothing.
     * Each duplicate will appear at index right after its original - sort of
     * couples will appear.
     * @param items items to duplicate
     */    
    public static void duplicateItemsByOne(List<PlaylistItem> items) {
        playlist.duplicateItemsByOne(items);
        selectionModel.get().clearSelection(); selectedItems.clear();
    }
    
    /**
     * Updates item.
     * Updates all instances of the Item that are in the playlist. When application
     * internally updates PlaylistItem it must make sure all its instances are 
     * updated too. Thats what this method does.
     * @param item item to update
     */
    public static void updateItemsOf(Item item) {
        playlist.updateItem(item);
    }
    /**
     * Updates all unupdated items. This method doesnt guarantee that all items
     * will be up to date, it guarantees that no item will have invalid data
     * resulting from lack of previous update on the item.
     * After this method is invoked, every item will be updated at least once.
     * It is sufficient to use this method to reupdate all items as it is not
     * expected for items to change their values externally because all internal
     * changes of the item by the application and the resulting need for updatets
     * are expected to be handled on the spot.
     * 
     * Utilizes bgr thread. Its safe to call this method without any performance
     * impact.
     * 
     * This method shouldnt be needed to be used outside of Playlist managing
     * scope.
     * @param items
     * @throws NullPointerException when param null.
     */
    public static void updateItems(List<PlaylistItem> items) {
        playlist.updateItems(items);
    }
    
    public static int getSize() {
        return playlist.getSize();
    }
    
    public static boolean isEmpty() {
        return playlist.getSize() == 0;
    }
    
    /**
     * Reverses order of the items. Operation can not be undone.
     */
    public static void reversePlaylist() {
        playlist.reverse();
    }
    /**
     * Randomizes order of the items. Operation can not be undone.
     */
    public static void randomizePlaylist() {
        playlist.randomize();
    }
    /**
     * Moves/shifts all selected items by specified distance.
     * Selected items retain their relative positions. Items stop moving when
     * any of them hits end/start of the playlist. Items wont rotate the list.
     * @note If this method requires real time response (for example reacting on mouse
     * drag in table), it is important to 'cache' the behavior and allow values
     * >1 && <-1 so the moved items dont lag behind.
     * @param indexes of items to move. Must be List<Integer>.
     * @param by distance to move items by. Negative moves back. Zero does nothing.
     * @return updated indexes of moved items. 
     */
    public static List<Integer> moveItemsBy(List<Object> indexes, int by) {
         return playlist.moveItemsBy(indexes, by);
    }
    
/******************************************************************************/
    
    /**
     * Plays item with given index.
     * In case index is out of bounds (<0,>=playlist.size) playback is stopped.
     * @param index 
     */
    public static void playItem(int index) {
        try {
            playItem(playlist.getItem(index));
        } catch (IndexOutOfBoundsException ex) {
            PLAYBACK.stop();
        }
    }
    
    /**
     * Plays given item. Does nothing if item not on playlist or null.
     * @param item
     */
    public static void playItem(PlaylistItem item) {
        if (item != null && playlist.contains(item)) {
            if (item.isCorrupt()) {
                // prevent infinite check loop when whole playlist corrupted (checks once per playlsit size)
                if (playlist.indexOf(item)==0 && playlist.isMarkedAsCorrupted()) {
                    PLAYBACK.stop();
                    return;
                }
                playItem(getNextItem(item));
            } else {
                PLAYBACK.play(item);
            }
        }
    }
    
    public static void playSelectedItem() {
        playItem(selectionModel.get().getSelectedItem());
    }
    
    /** Plays first item on playlist.*/
    @IsAction(name = "Play first", info = "Plays first item on playlist.", shortcut = "ALT+W")
    public static void playFirstItem() {
        playItem(0);
    }
    
    /** Plays last item on playlist.*/
    @IsAction(name = "Play last", info = "Plays last item on playlist.")
    public static void playLastItem() {
        playItem(playlist.getSize()-1);
    }
    
    /** Plays next item on playlist.*/
    @IsAction(name = "Play next", info = "Plays next item on playlist.", shortcut = "ALT+Z")
    public static void playNextItem() {
        int index = playlist.indexOf(playingItem.get());
        switch (PLAYBACK.getLoopMode()) {
            case OFF:
                if (index == getSize()-1) { PLAYBACK.stop(); }
                else { playItem(index+1); }
                break;
            case SONG:
                playItem(index);
                break;
            case PLAYLIST:
                if (index < getSize()-1) {
                    playItem(index+1);
                } else {
                    playFirstItem();
                }
                break;
            default:
        }
    }
    
    /** Plays previous item on playlist.*/
    @IsAction(name = "Play previous", info = "Plays previous item on playlist.", shortcut = "ALT+\\")
    public static void playPreviousItem() {
        int index = playlist.indexOf(playingItem.get());
        switch (PLAYBACK.getLoopMode()) {
            case OFF:
                if (index == 0) { PLAYBACK.stop(); }
                else { playItem(index-1); }
                break;
            case SONG:
                playItem(index);
                break;
            case PLAYLIST:
                if (index > 0) {
                    playItem(index-1);
                } else {
                    playLastItem();
                }
                break;
            default:
        }
    }
    
    /**
     * Plays new playlist.
     * Clears active playlist completely and adds all items from new playlist.
     * Starts playing first file.
     * @param p new playlist.
     * @throws NullPointerException if param null.
     */
    public static void playPlaylist(Playlist p) {
        Objects.requireNonNull(p);
        playlist.setItems(p);
        playFirstItem();
    }
    /**
     * Plays new playlist.
     * Clears active playlist completely and adds all items from new playlist.
     * Starts playing item with the given index. If index is out of range for new
     * playlist, handles according to behavior in playItem(index int) method.
     * @param p new playlist.
     * @param from index of item to play from
     * @throws NullPointerException if param null.
     */
    public static void playPlaylistFrom(Playlist p, int from) {
        Objects.requireNonNull(p);
        playlist.setItems(p);
        playItem(from);
    }
    
    /**
     * Returns state object representing the most up to date state of the playlist.
     * Stored information includes all items, playing item and total time length.
     * @return 
     */
    public static PlaylistState getState() {
        return new PlaylistState(getItems(), playingItem.get());
    }
    
    /**
     * Serializes active playlist into new native playlist file with name consisting of
     * "Listening To" and current datetime.
     */
    public static void saveActivePlaylist() {
        String filename = Util.filenamizeString("ListeningTo " + new Date(System.currentTimeMillis()));
        Playlist p = new Playlist(filename, getItems());
                 p.addCategory("Listening to...");
        p.serialize();
    }

}