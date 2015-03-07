
package AudioPlayer.playlist;

import AudioPlayer.tagging.ActionTask;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javafx.util.Duration;
import static util.File.AudioFileFormat.Use.PLAYBACK;
import util.async.Async;

/**
 * Playlist handles PlaylistItem groups and provides methods for easy
 * manipulation.
 * Length is read only as it depends solely on internal state.
 * 
 * @author uranium
 */
abstract public class AbstractPlaylist {
    
    /**
     * @return List backing the Playlist implementation of this class.
     */
    protected abstract List<PlaylistItem> list();
   /**
     * @return Items of playlist. Depending on implementation, this can be
     * unmodifiable.
     */
    abstract public List<PlaylistItem> getItems();
    /**
     * @return total duration of this playlist
     */
    abstract public Duration getLength();
    
/***************************** LIST METHODS ***********************************/
    
    /**
     * @return size of the playlist. Number of items. 
     */
    public int getSize() {
        return list().size();
    }
    /**
     * @return true if the list contains no elements. 
     */
    public boolean isEmpty() {
        return list().isEmpty();
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
    public int indexOf(PlaylistItem item) {
        return list().indexOf(item);
    }
    /**
     * Returns index of the first item in playlist with same source as parameter.
     * Method utilizes item.equalsSource() equality test.
     * @param item
     * @return item index. -1 if not in playlist.
     */
    public int indexOf(Item item) {
        for (PlaylistItem i: list()) {
            if (i.same(item)) return list().indexOf(i);
        }
        return -1;
    }

    /**
     * Adds new item to end of this playlist, based on the url (String). Use this method
     * for  URL based Items.
     * @param url
     * @throws NullPointerException when param null.
     */
    public void addUrl(String url) {
        addUrls(Collections.singletonList(url));
    }
    /**
     * Adds new items to end of this playlist, based on the urls (String). Use 
     * this method for URL based Items.
     * Malformed url's will be ignored.
     * @param urls
     * @throws NullPointerException when param null.
     */
    public void addUrls(List<String> urls) {
        List<URI> add = new ArrayList<>();
        for(String url: urls) {
            try{
                URI u = new URI(url);
                URL r = new URL(url);
                add.add(URI.create(url));
            } catch(URISyntaxException | MalformedURLException e) {
                throw new RuntimeException("Invalid URL.");
            }
        }
        addUris(add, list().size());
    }
    /**
     * Adds new item to specified index of playlist.
     * Dont use for physical files..
     * @param url
     * @param at
     * @throws NullPointerException when param null.
     */
    public void addUrls(String url, int at) {
        List<URI> to_add = Collections.singletonList(URI.create(url));
        addUris(to_add, at);
    }
    /**
     * Adds specified item to end of this playlist. 
     * @param file 
     * @throws NullPointerException when param null.
     */
    public void addFile(File file) {
        addUri(file.toURI());
    }
    /**
     * Adds specified files to end of this playlist. 
     * @param files 
     * @throws NullPointerException when param null.
     */
    public void addFiles(List<File> files) {
        addUris(files.stream().map(File::toURI).collect(Collectors.toList()));
    }
    /**
     * Adds specified item to end of this playlist. 
     * @param uri
     * @throws NullPointerException when param null.
     */
    public void addUri(URI uri) {
        addUris(Collections.singletonList(uri));
    }
    /**
     * Adds items at the end of this playlist and updates them if necessary.
     * Adding produces immediate response for all items. Updating will take time
     * and take effect the moment it is applied one by one.
     * @param uris
     * @throws NullPointerException when param null.
     */
    public void addUris(List<URI> uris) {
        addUris(uris, list().size());
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
    public void addUris(List<URI> uris, int at) {
        int _at = at;
        if (_at < 0)             _at = 0;
        if (_at > list().size()) _at = list().size();
        
        Playlist p = new Playlist();
        uris.forEach(uri->p.list().add(new PlaylistItem(uri)));
        
        addPlaylist(p, _at);
    }
    public void addItem(Item item) {
        addItems(Collections.singletonList(item), list().size());
    }
    /**
     * Adds all specified items to end of this playlist.
     * @param items
     * @throws NullPointerException when param null.
     */
    public void addItems(List<? extends Item> items) {
        addItems(items, list().size());
    }
    /**
     * Adds all specified items to specified position of this playlist.
     * @param items
     * @param at
     * Index at which items will be added. Out of bounds index will be converted:
     * index < 0     --> 0(first)
     * index >= size --> size (last item)
     * @throws NullPointerException when param null.
     */
    public void addItems(List<? extends Item> items, int at) {
        int _at = at;
        if (_at < 0)             _at = 0;
        if (_at > list().size()) _at = list().size();
        
        Playlist p = new Playlist();
        items.stream().map(Item::toPlaylistItem).forEach(p.list()::add);
        
        addPlaylist(p, _at);
    }
    /**
     * Adds all playlist items on the specified playlist at the end of this
     * playlist.
     * @param p
     * @throws NullPointerException when param null.
     */
    public void addPlaylist(Playlist p) {
        addPlaylist(p, list().size());
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
    public void addPlaylist(Playlist p, int at) {
        int _at = at;
        if (_at < 0)             _at = 0;
        if (_at > list().size()) _at = list().size();
        
        list().addAll(_at, p.list());
        updateItems(p.list());
        updateDuration();
    }
    /**
     * Changes items to that of specified playlist. Clear and set operation.
     * @param p
     */
    public void setItems(Playlist p) {
        list().clear();
        list().addAll(p.getItems());
        updateItems(list());
        updateDuration();
    }
    /**
     * Returns PlaylistItem at specified index.
     * @param index
     * @return item at index. Null if index out of bounds.
     * @throws IndexOutOfBoundsException when out of bounds index.
     */
    public PlaylistItem getItem(int index) {
        return list().get(index);
    }
    /**
     * Returns the first playlist item passing the equalSource()=true for specified item.
     * Use this method to look for item with URi by wrapping it into SimpleItem
     * object before passing it as argument.
     * @param item
     * @return PlaylistItem with specified source. Null if no result, param item is
     * null or playlist is empty.
     */
    public PlaylistItem getItem(Item item) {
        for(PlaylistItem i: list())
            if (i.same(item))
                return i;
        return null;
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
    public PlaylistItem getNextItem(PlaylistItem item) {
        if (list().isEmpty() || !list().contains(item)) return null;
        int i = list().indexOf(item);
        return (i == list().size()-1) ? list().get(0) : list().get(i+1);
    }
    /**
     * Returns true when playlist contains the specified PlaylistItem according
     * to List specifications.
     * @param item
     * @return true if playlist contains playlist item
     */
    public boolean contains(PlaylistItem item) {
        return list().contains(item);
    }   
    /**
     * Returns true when the same source already exists in the playlist.
     * Method utilizes Item's equalsSource().
     * @param item
     * @return true if item with the same source as parameter's exists.
     */
    public boolean containsItem(Item item) {
        return list().stream().anyMatch(item::same);      
    }
    /**
     * Removes specified item from this playlist. Does nothing if null or not on
     * playlist.
     * @param item 
     * @return true if this list contained the specified element
     */
    public boolean removeItem(PlaylistItem item) {
        boolean changed = list().remove(item);
        updateDuration();
        return changed;
    }
    /**
     * Removes specified items from this playlist. Does nothing if null or not on
     * playlist.
     * @param items to remove
     * @return true if this list contained the specified element
     */
    public boolean removeItems(List<PlaylistItem> items) {
        boolean changed = true;
        list().removeAll(items);        
        updateDuration();
        return changed;
    }
    /**
     * Retains only the elements in this list that are contained in the specified
     * collection (optional operation). In other words, removes from this list
     * all of its elements that are not contained in the specified collection.
     * @param items to retain
     * @return true if this list changed as a result of the call
     */
    public boolean retainItems(List<PlaylistItem> items) {
        boolean changed = list().retainAll(items);
        updateDuration();
        return changed;
    }
    /**
     * Removes all playlist items. The playlist remains empty after this method
     * is invoked.
     */
    public void clear() {
        list().clear();
        updateDuration();
    }
    /**
     * Removes all corrupt items from this playlist.
     */
    public void removeCorrupt() {
        List<PlaylistItem> l = list();
        //iterate backwards and delete one by one
        for (int i=l.size()-1; i>=0; i--) {
            if (l.get(i).isCorrupt(PLAYBACK)) l.remove(i);
        }
        updateDuration();
    }
    /**
     * Removes all duplicates of all items of this playlist. After this method
     * is called, there will be no duplicates on the list. Exactly one of each
     * unique item will be left.
     * Duplicates/not unique items are any two item for which equalsSource()
     * returns true.
     */
    public void removeDuplicates() {
        List<PlaylistItem> _items = new ArrayList<>();
        for (PlaylistItem item: list()) {
            boolean contains = false;
            for (PlaylistItem i: _items) {
                if (item.same(i))
                    contains = true;
            }
            if (!contains)_items.add(item);
        }
        list().clear();
        list().addAll(_items);
        
        updateDuration();
    }
    
    /**
     * Duplicates the item if it is in playlist. If it isnt, does nothing.
     * Duplicate will appear on the next index following the original.
     * @param item
     * @throws NullPointerException if param null
     */
    public void duplicateItem(PlaylistItem item) {
        if (!list().contains(item)) return;
        list().add(list().indexOf(item)+1, item.clone());
        updateDuration();
    }
    
    /**
     * Duplicates the items if they are in playlist. If they arent, does nothing.
     * Duplicates will appear on the next index following the last items's index.
     * @param items items to duplicate
     * @throws NullPointerException if param null
     */
    public void duplicateItemsAsGroup(List<PlaylistItem> items) {
        Objects.requireNonNull(items);
        int index = 0;
        List<PlaylistItem> to_dup = new ArrayList<>();
        for (PlaylistItem item: items) {
            int i = list().indexOf(item); 
            if (i > 0) { // if contains
                to_dup.add(item.clone());   // item must be cloned
                index = i+1;
            }
        }
        if (to_dup.isEmpty()) return;
        list().addAll(index, to_dup);
        updateDuration();
    }
    
    /**
     * Duplicates the items if they are in playlist. If they arent, does nothing.
     * Each duplicate will appear at index right after its original - sort of
     * couples will appear.
     * @param items items to duplicate
     * @throws NullPointerException if param null
     */
    public void duplicateItemsByOne(List<PlaylistItem> items) {
        Objects.requireNonNull(items);
        items.forEach(this::duplicateItem);
    }
/******************************** ORDERING ************************************/
    
    /**
     * Reverses order of the items. Operation can not be undone.
     */
    public void reverse() {
        Collections.reverse(list());
    }
    /**
     * Randomizes order of the items. Operation can not be undone.
     */
    public void randomize() {
        Collections.shuffle(list());
    }

    /**
     * Moves/shifts all specified items by specified distance.
     * Selected items retain their relative positions. Items stop moving when
     * any of them hits end/start of the playlist. Items wont rotate the list.
     * @note If this method requires real time response (for example reacting on mouse
     * drag in table), it is important to 'cache' the behavior and allow values
     * >1 && <-1 so the moved items dont lag behind.
     * @param indexes of items to move. Must be List<Integer>.
     * @param by distance to move items by. Negative moves back. Zero does nothing.
     * @return updated indexes of moved items.
     */
    public List<Integer> moveItemsBy(List<Integer> indexes, int by){ 
        List<List<Integer>> blocks = slice(indexes);
        List<Integer> newSelected = new ArrayList();
        
        try {
            if(by>0) {
                for(int i=blocks.size()-1; i>=0; i--) {
                    newSelected.addAll(moveItemsByBlock(blocks.get(i), by));
                }
            } else if ( by < 0) {
                for(int i=0; i<blocks.size(); i++) {
                    newSelected.addAll(moveItemsByBlock(blocks.get(i), by));
                }
            }
        } catch(IndexOutOfBoundsException e) {
            return indexes;
        }
        return newSelected;
    }
//    private List<Integer> moveItemsBy(List<Integer> indexes, int by) {
//        List<Integer> newSelected = new ArrayList<>();
//        try {
//            if(by > 0) {
//                for (int i = indexes.size()-1; i >= 0; i--) {
//                    int ii = indexes.get(i);
//                    Collections.swap(list(), ii, ii+by);
//                    newSelected.add(ii+by);
//                }
//
//            } else if ( by < 0) {
//                for (int i = 0; i < indexes.size(); i++) {
//                    int ii = indexes.get(i);
//                    Collections.swap(list(), ii, ii+by);
//                    newSelected.add(ii+by);
//                }
//            }
//        } catch ( IndexOutOfBoundsException ex) {
//            // thrown if moved block hits start or end of the playlist
//            // this gets rid of enormously complicated if statement
//            // return old indexes
//            return indexes;
//        }
//        return newSelected;
//    }
    
    private List<Integer> moveItemsByBlock(List<Integer> indexes, int by) throws IndexOutOfBoundsException {
        List<Integer> newSelected = new ArrayList<>();
        try {
            if(by > 0) {
                for (int i = indexes.size()-1; i >= 0; i--) {
                    int ii = indexes.get(i);
                    Collections.swap(list(), ii, ii+by);
                    newSelected.add(ii+by);
                }
                
            } else if ( by < 0) {
                for (int i = 0; i < indexes.size(); i++) {
                    int ii = indexes.get(i);
                    Collections.swap(list(), ii, ii+by);
                    newSelected.add(ii+by);
                }
            }
        } catch ( IndexOutOfBoundsException ex) {
            // thrown if moved block hits start or end of the playlist
            // this gets rid of enormously complicated if statement
            // return old indexes
//            return indexes;
            throw new IndexOutOfBoundsException();
        }
        return newSelected;
    }
    // slice to monolithic blocks
    private List<List<Integer>> slice(List<Integer> indexes){
        if(indexes.isEmpty()) return new ArrayList();
        
        List<List<Integer>> blocks = new ArrayList();
                            blocks.add(new ArrayList());
        int last = indexes.get(0);
        int list = 0;
        blocks.get(list).add(indexes.get(0));
        for (int i=1; i<indexes.size(); i++) {
            int index = indexes.get(i);
            if(index==last+1) {
                blocks.get(list).add(index);
                last++;
            }
            else {
                list++;
                last = index;
                List<Integer> newL = new ArrayList();
                              newL.add(index);
                blocks.add(newL);
            }
        }
        
        return blocks;
    }
    
/***************************** PLAYLIST METHODS *******************************/
    
    /**
     * Returns true only if all items of this playlist are marked corrupt.
     * No I/O. Performs O(n).
     * @return cached corruptness. 
     */
    public boolean isMarkedAsCorrupted() {
        return list().stream().allMatch(PlaylistItem::markedAsCorrupted);
    }
    /**
     * Checks and returns true only if all items on this playlist are corrupt.
     * Requires file checks (I/O). Performs O(n).
     * @return corruptness. 
     */
    public boolean isCorrupt() {
        return list().stream().allMatch(PlaylistItem::isNotPlayable);
    }
    /**
     * Checks and returns true only if no item on this playlist is corrupt.
     * Requires file checks (I/O). Performs lineary - O(n).
     * @return corruptness. 
     */
    public boolean isValid() {
        return list().stream().noneMatch(PlaylistItem::isNotPlayable);
    }
    /**
     * Updates item.
     * Updates all instances of the Item that are in the playlist. When application
     * internally updates PlaylistItem it must make sure all its duplicates are 
     * updated too. Thats what this method does.
     * @param item item to update
     */
    public void updateItem(Item item) {
        list().stream().filter(item::same).forEach(PlaylistItem::update);
        updateDuration();
    }
    /**
     * Updates all unupdated items. This method doesnt guarantee that all items
     * will be up to date, it guarantees that no item will have invalid data
     * resulting from lack of any update on the item during its lifetime.
     * After this method is invoked, every item will be updated at least once.
     * It is sufficient to use this method to reupdate all items as it is not
     * expected for items to change their values externally because all internal
     * changes of the item by the application and the resulting need for updatets
     * are expected to be handled on the spot.
     * 
     * Utilizes bgr thread. Its safe to call this method without any performance
     * impact.
     * @param items items on playlist. If the item is not on playlist it will also
     * be updated but it will have no effect on playlist.
     * @throws NullPointerException when param null.
     */
    public void updateItems(List<PlaylistItem> items) {
        items.removeIf(PlaylistItem::updated);
        if (items.isEmpty()) return;
        
        new ActionTask<Void>("") {
            @Override protected Void call() throws Exception {
               for (PlaylistItem i: items) {
                    if (this.isCancelled()) return null;
                    i.update();
                }
                return null;
            }
        }
            .setOnDone((ok,none) -> updateDuration())
            .run(Async::executeBgr);
    }
    /**
     * Use to completely refresh playlist.
     * Updates all items on playlist. This method guarantees that all items
     * will be up to date.
     * After this method is invoked, every item will be updated at least once
     * and reflect metadata written in the physical file.
     * 
     * Utilizes bgr thread. Its safe to call this method without any performance
     * impact.
     */
    public void updateItems() {
         if (isEmpty()) return;
        
         new ActionTask<Void>("") {
            @Override protected Void call() throws Exception {
               for (PlaylistItem i: list()) {
                    if (this.isCancelled()) break;
                    i.update();
                }
                return null;
            }
        }
            .setOnDone((ok,none) -> updateDuration())
            .run(Async::executeBgr);
    }
    
/***************************** INTERNAL METHODS *******************************/
    
    /**
     * Calculates the total time duration of the playlist.
     * @return up to date length of the playlist.
     */
    protected Duration calculateLength() {
        double total = list().stream()
                .map(PlaylistItem::getTime)
                .filter(d->!d.isIndefinite()&&!d.isUnknown())
                .mapToDouble(d->d.toMillis())
                .sum();
        return Duration.millis(total);
    }
    /**
     * For internal use only.
     * Sets duration to updated value.
     */
    protected void updateDuration(){};
    
}
