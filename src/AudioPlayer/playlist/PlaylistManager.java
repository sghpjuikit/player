
package AudioPlayer.playlist;

import Action.IsAction;
import Action.IsActionable;
import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playlist.ItemSelection.PlayingItemSelector;
import Configuration.Configurable;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import Configuration.ValueConfig;
import GUI.objects.PopOver.PopOver;
import GUI.objects.SimpleConfigurator;
import GUI.objects.Text;
import de.jensd.fx.fontawesome.AwesomeDude;
import static de.jensd.fx.fontawesome.AwesomeIcon.INFO;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import static java.util.Collections.EMPTY_LIST;
import java.util.List;
import java.util.Objects;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import main.App;
import utilities.AudioFileFormat;
import utilities.Enviroment;
import utilities.FileUtil;
import utilities.access.AccessibleStream;

/**
 * Provides unified handling to everything playlist related in the application
 */
@IsConfigurable("Playlist")
@IsActionable
public class PlaylistManager implements Configurable {
    
    private static final ObservablePlaylist playlist = new ObservablePlaylist(Player.state.playlist.playlist);
    private static final ReadOnlyObjectWrapper<PlaylistItem> playingItem = new ReadOnlyObjectWrapper<>();
    
    public static final PlayingItemSelector playingItemSelector = new PlayingItemSelector();
    
    /**
     * Last selected item on playlist or null if none.
     */
    public static final AccessibleStream<PlaylistItem> selectedItemES = new AccessibleStream(null);
    /**
     * Selected items on playlist or empty list if none.
     */
    public static final AccessibleStream<List<PlaylistItem>> selectedItemsES = new AccessibleStream(EMPTY_LIST);
    
    /**
     * Initialize state from last session
     */
    public static void changeState() {
        addItems(Player.state.playlist.playlist);
        // fires metadata loading update (needed ?)
        playingItem.set(getItem(Player.state.playlist.playingItem.get()));
    }
    
/******************************************************************************/
    
    /**
     * Returns currently played item.
     * @return played item or null if no item is being played.
     */
    public static PlaylistItem getPlayingItem() {
        return playingItem.get();
    }
    /**
     * Internal use only !
     * this needs to be removed
     * @param item 
     */
    public static void setPlayingItem(PlaylistItem item) {
        playingItem.set(item);
    }
    /**
     * Returns true if any item on the playlist is playing.
     * @return true if any item is playing.
     */
    public static boolean isItemPlaying() {
        return playingItem.get() != null;
    }
    /**
     * Returns true if specified item is playing item on the playlist. There can
     * only be one item in the application for which this method returns true.
     * Note the disctinction between same file of the items and two items being
     * the very same item.
     * @return true if item is played.
     */
    public static boolean isItemPlaying(Item item) {
        return getPlayingItem()==item; // playlistItem equals
    }
    /**
     * Returns true if file to specified item is being played item on the playlist.
     * Note the disctinction between same file of the items and two items being
     * the very same item.
     * @return true if item is played.
     */
    public static boolean isSameItemPlaying(Item item) {
        Objects.requireNonNull(item);
        return item.same(getPlayingItem());
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
     * Returns read-only playlist length.
     * @return length total duration of the playlist.
     */
    public static Duration getLength() {
        return lengthProperty().getValue();
    }   
    /**
     * Returns bindable read only property of total duration.
     * @return bindable total playlist duration playlistproperty.
     */
    public static ReadOnlyProperty<Duration> lengthProperty() {
        return playlist.lengthProperty();
    }
       
/******************************************************************************/
    
    /**
     * Returns the first playlist item passing the equalSource()=true for 
     * specified item. Use this method to look for item by URI by wrapping the 
     * URI into an instance of {@link Item} and passing it as an argument.
     * @param item
     * @return PlaylistItem with specified source. Null if no result, param item is
     * null or playlist is empty.
     */
    public static PlaylistItem getItem(Item item) {
        return playlist.getItem(item);
    }
    /**
     * Returns the index of the first occurrence of the specified element in this list,
     * or -1 if this list does not contain the element. Formally, returns the 
     * lowest index i such that (o==null ? get(i)==null : o.equals(get(i))), or 
     * -1 if there is no such index.
     * @param item element to search for index for
     * @return item the index of the first occurrence of the specified element 
     * in this list, or -1 if this list does not contain the element
     */
    public static int getIndexOf(PlaylistItem item) {
        return playlist.indexOf(item);
    }
    /**
     * Returns index of the first item in playlist with same source as parameter.
     * Method utilizes item.same() equality test.
     * @param item
     * @return item index. -1 if not in playlist.
     */
    public static int getIndexOf(Item item) {
        return playlist.indexOf(item);
    }
    /**
     * Returns item following the one specified. If the item is last item of the
     * playlist, 1st item is returned, making playlist appear to have cyclic
     * nature.
     * @param item
     * @return Next item. Null if specified item is null, does not exist in the
     * playlist or playlist is empty.
     * @throws NullPointerException when param null.
     */
    public static PlaylistItem getNextItem(PlaylistItem item) {
        return playlist.getNextItem(item);
    }

    /**
     * @return observable list of all items of playlist. 
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
    }
    
    /**
     * Removes all playlist items in the specified List from playlist.
     */
    public static void removeItems(List<PlaylistItem> items) {
        playlist.removeItems(items);
    }
    
    /**
     * Removes all playlist items. The playlist remains empty after this method
     * is invoked.
     */
    public static void removeAllItems() {
        playlist.clear();
    }
    /**
     * Retains only the elements in this list that are contained in the specified 
     * collection (optional operation). In other words, removes from this list all of
     * its elements that are not contained in the specified collection.
     */
    public static void retainItems(List<PlaylistItem> items) {
        playlist.retainItems(items);
    }
    /**
     * Removes all corrupt items from this playlist.
     */
    public static void removeCorrupt() {
        playlist.removeCorrupt();
    }
    /**
     * Removes all duplicates of all items of this playlist. After this method
     * is called, there will be no duplicates on the list. Exactly one of each
     * unique item will be left.
     * Duplicates/not unique items are any two item for which equalsSource()
     * returns true.
     */
    public static void removeDuplicates() {
        playlist.removeDuplicates();
    }
    /**
     * Duplicates the item if it is in playlist. If it isnt, does nothing.
     * Duplicate will appear on the next index following the original.
     * @param item 
     */
    public static void duplicateItem(PlaylistItem item) {
        playlist.duplicateItem(item);
    }

    /**
     * Duplicates the items if they are in playlist. If they arent, does nothing.
     * Duplicates will appear on the next index following the last items's index.
     * @param items items to duplicate
     */
    public static void duplicateItemsAsGroup(List<PlaylistItem> items) {
        playlist.duplicateItemsAsGroup(items);
    }
    
    /**
     * Duplicates the items if they are in playlist. If they arent, does nothing.
     * Each duplicate will appear at index right after its original - sort of
     * couples will appear.
     * @param items items to duplicate
     */    
    public static void duplicateItemsByOne(List<PlaylistItem> items) {
        playlist.duplicateItemsByOne(items);
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
     * any of them hits end/start of the playlist - items wont rotate in the playlist.
     * @param by distance to move items by. Negative moves back. Zero does nothing.
     */
    public static List<Integer> moveItemsBy(List<Integer> indexes, int by) {
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
    
    /** Plays first item on playlist.*/
    @IsAction(name = "Play first", description = "Plays first item on playlist.", shortcut = "ALT+W", global = true)
    public static void playFirstItem() {
        playItem(0);
    }
    
    /** Plays last item on playlist.*/
    @IsAction(name = "Play last", description = "Plays last item on playlist.", global = true)
    public static void playLastItem() {
        playItem(playlist.getSize()-1);
    }
    
    /** Plays next item on playlist according to its selector logic.*/
    @IsAction(name = "Play next", description = "Plays next item on playlist.", shortcut = "ALT+Z", global = true)
    public static void playNextItem() {
        playItem(playingItemSelector.getNextPlaying());
    }
    
    /** Plays previous item on playlist according to its selector logic.*/
    @IsAction(name = "Play previous", description = "Plays previous item on playlist.", shortcut = "ALT+BACK_SLASH", global = true)
    public static void playPreviousItem() {
        playItem(playingItemSelector.getPreviousPlaying());
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
    
    /** Plays previous item on playlist according to its selector logic.*/
    @IsAction(name = "Play previous", description = "Plays previous item on playlist.", shortcut = "ALT+\\", global = true)
    public static void openFile() {
        playItem(playingItemSelector.getPreviousPlaying());
    }
    
/******************************************************************************/
    
    @IsConfig(name = "Default browse location", info = "Opens this location for file dialogs.")
    public static File browse = App.getLocation();
    @IsConfig(name = "File search depth", info = "Depth for recursive search within directories. 0 denotes specified directory.")
    public static int folder_depth = 1;
    
    /** Open chooser and add new to end of playlist. */
    @IsAction(name = "Choose and Add Files", description = "Open file chooser to add files to playlist.")
    public static void chooseFilestoAdd() {
        addOrEnqueueFiles(true);
    }
    /** Open chooser and add new to end of playlist. */
    @IsAction(name = "Choose and Add Directory", description = "Open file chooser to add files from directory to playlist.")
    public static void chooseFoldertoAdd() {
        addOrEnqueueFolder(true);
    }
    /** Open chooser and add new to end of playlist. */
    @IsAction(name = "Choose and Add Url", description = "Open file chooser to add url to playlist.")
    public static void chooseUrltoAdd() {
        addOrEnqueueUrl(true);
    }
    /** Open chooser and play new items. Clears previous playlist */
    @IsAction(name = "Choose and Play Files", description = "Open file chooser to play files to playlist.")
    public static void chooseFilesToPlay() {
        addOrEnqueueFiles(true);
    }
    /** Open chooser and play new items. Clears previous playlist */
    @IsAction(name = "Choose and Play Directory", description = "Open file chooser to play files from directory to playlist.")
    public static void chooseFolderToPlay() {
        addOrEnqueueFolder(true);
    }
    /** Open chooser and play new items. Clears previous playlist */
    @IsAction(name = "Choose and Play Url", description = "Open file chooser to add url play playlist.")
    public static void chooseUrlToPlay() {
        addOrEnqueueUrl(true);
    }
    
    /** 
     * Open chooser and add or play new items.
     * @param add true to add items, false to clear playlist and play items
     */
    public static void addOrEnqueueFiles(boolean add) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose Audio Files");
        if (FileUtil.isValidDirectory(browse))
            fc.setInitialDirectory(browse);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "supported audio", AudioFileFormat.extensions()));
        List<File> files = fc.showOpenMultipleDialog(App.getWindowOwner().getStage());
        if (files != null) {
            browse = files.get(0).getParentFile();
            List<URI> queue = new ArrayList<>();
            files.forEach(f -> queue.add(f.toURI()));
            
            if(add) addUris(queue);
            else {
                PLAYBACK.stop();
                removeAllItems();
                addUris(queue);
                playFirstItem();
            }
        }
    }
    /** 
     * Open chooser and add or play new items.
     * @param add true to add items, false to clear playlist and play items
     */
    public static void addOrEnqueueFolder(boolean add) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Choose Audio Files From Directory Tree");
        if (FileUtil.isValidDirectory(browse)) 
            dc.setInitialDirectory(browse);
        File dir = dc.showDialog(App.getWindowOwner().getStage());
        if (dir != null) {
            browse = dir;
            List<URI> queue = new ArrayList<>();
            List<File> files = FileUtil.getAudioFiles(dir, folder_depth);
            files.forEach(f -> queue.add(f.toURI()));
            
            if(add) addUris(queue);
            else {
                PLAYBACK.stop();
                removeAllItems();
                addUris(queue);
                playFirstItem();
            }
        }
    }
    /** 
     * Open chooser and add or play new items.
     * @param add true to add items, false to clear playlist and play items
     */
    private static void addOrEnqueueUrl(boolean add) {
        // build content
        String title = add ? "Add url item." : "Play url item.";
        SimpleConfigurator content = new SimpleConfigurator<String>(
            new ValueConfig("Url", "url", title),
            c -> {
                String url = c.getField("Url").getValue();
                if(add) {
                    addUrl(url);
                } else {
                    PLAYBACK.stop();
                    removeAllItems();
                    addUrl(url);
                    playFirstItem();
                }
            });
        
        // build help content
        String uri = "http://www.musicaddict.com";
        Text t1 = new Text("Use direct url to a file, for example\n"
                         + "a file on the web. The url is the\n"
                         + "address to the file and should end \n"
                         + "with file suffix like '.mp3'. Try\n"
                         + "visiting: ");
        Text t2 = new Text(uri);
             // turn to hyperlink by assigning proper styleclass
             t2.getStyleClass().add("hyperlink");
        VBox cnt = new VBox(t1,t2);
             cnt.setSpacing(8);
        VBox.setMargin(t2, new Insets(0, 0, 0, 20));
        Label infoB = AwesomeDude.createIconLabel(INFO, "", "11", "11", ContentDisplay.CENTER);
              infoB.setTooltip(new Tooltip("Help"));
              infoB.setOnMouseClicked( e -> {
                PopOver helpP = PopOver.createHelpPopOver("");
                        helpP.setContentNode(cnt);
                        // open the uri in browser
                        helpP.getContentNode().setOnMouseClicked( pe -> {
                            Enviroment.browse(URI.create(uri));
                            pe.consume();
                        });
                        helpP.show(infoB);
              });
        // build popup
        PopOver p = new PopOver(title, content);
                p.getHeaderIcons().add(infoB);
                p.show(PopOver.ScreenCentricPos.AppCenter);
                p.setDetached(true);
    }

}