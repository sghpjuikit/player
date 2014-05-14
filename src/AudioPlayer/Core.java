/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer;

import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataReader;
import java.util.List;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import utilities.Log;
import utilities.functional.functor.Procedure;

/**
 *
 * @author uranium
 */
final class Core {
    final SimpleObjectProperty<Metadata> currentMetadataCache = new SimpleObjectProperty<>();
    final SimpleObjectProperty<Metadata> nextMetadataCache = new SimpleObjectProperty<>();
    
    final SimpleObjectProperty<Metadata> selectedMetadata = new SimpleObjectProperty<>();
    final ObservableList<Metadata> selectedMetadatas = FXCollections.observableArrayList();
    
    Core() {
    }
    
    void initialize() {
        
        // playing item changed
        //-> change current Metadata
        //-> wait, preload metadata for next item
        PlaylistManager.playingItemProperty().addListener((observable, oldV, newV) -> {
                // change current Metadata
                if (nextMetadataCache.get() != null && newV.same(nextMetadataCache.get())) {
                    currentMetadataCache.set(nextMetadataCache.get());
                    itemChange.fireEvent(true, nextMetadataCache.get());
                    Log.deb("Metadata cache copied from next item metadata cache.");
                } else {
                    Log.deb("Metadata cache copy failed. Next item metadata cache content doesnt correspond to current item.");
                    loadCurrentMetadataCache(true);
                }
                
                //new thread, wait 400ms, preload metadata for next item
                final PlaylistItem next = PlaylistManager.playingItemSelector.getNextPlaying();
                Thread thr = new Thread(() -> {
                    try {
                        Thread.sleep(400);
                        preloadNextMetadataCache();
                    } catch (InterruptedException ex) {
                        Log.err("Metadata preloading thread interrupted.");
                    }
                });
                thr.setDaemon(true);
                thr.start();
        });
        
        // playlist selection changed
        // -> manage selected playlist items' metadata list
        // -> manage lastly selected playlist item' list
        PlaylistManager.getSelectedItems().addListener((ListChangeListener.Change<? extends PlaylistItem> change) -> {
            while(change.next()) {
                if (change.wasAdded() || change.wasRemoved() || change.wasReplaced()) {
                    loadPlaylistSelectedMetadatas();
                }
            }
        });
    }

/******************************** current *************************************/
    
    final ItemChangeEvent itemChange = new ItemChangeEvent();
    
    private void loadCurrentMetadataCache(boolean changeType) {
        PlaylistItem item = PlaylistManager.getPlayingItem();
        MetadataReader.create(item, 
            result -> {
                currentMetadataCache.set(result);
                itemChange.fireEvent(changeType, result);
                Log.deb("Current metadata cache loaded.");
            },
            () -> {
                currentMetadataCache.set(item.toMetadata());
                itemChange.fireEvent(changeType, currentMetadataCache.get());
                Log.deb("Current metadata cache load fail. Metadata will be empty.");
            }
        );
    }
    
    void updateCurrent() {
        loadCurrentMetadataCache(false);
    }
    void loadCurrent() {
        loadCurrentMetadataCache(true);
    }

/********************************** next **************************************/
    
    private void preloadNextMetadataCache() {
        PlaylistItem next = PlaylistManager.playingItemSelector.getNextPlaying();
        if(next==null) {
            Log.deb("Next item metadata cache preloading prevented. No next playing item.");
            return;
        }
        MetadataReader.create(next,
            result -> {
                nextMetadataCache.set(result);
                Log.deb("Next item metadata cache preloaded.");       
            },
            () -> {
                // dont set any value, not even empty
                Log.deb("Preloading next item metadata into cache failed.");
            }
        );
    }
    
/******************************** selected ************************************/
    
    void loadPlaylistSelectedMetadata() {
        // this algorithm makes use of the fact that selected items are already
        // loaded and Last selected must always be among them. No need to add
        // more listeners and whatnot, just look it up
        
        
        selectedMetadata.set(null); // if empty leave null (null=empty)
        
        PlaylistItem lastSelected = PlaylistManager.getSelectedItem();
        for (Metadata m: selectedMetadatas) {
            if (m.same(lastSelected)) {
                selectedMetadata.set(m);
                Log.deb("In playlist last selected metadata loaded.");
                return; // return if found
            }
        }
        Log.deb("In playlist last selected metadata loaded. Empty.");
    }
    
    private void loadPlaylistSelectedMetadatas() {
        List<? extends Item> items = PlaylistManager.getSelectedItems();
        MetadataReader.readMetadata(items,
            result -> {
                selectedMetadatas.setAll(result);
                Log.deb("In playlist selected metadatas loaded.");
                loadPlaylistSelectedMetadata();
            },
            Procedure.DO_NOTHING()
        );
    }
}