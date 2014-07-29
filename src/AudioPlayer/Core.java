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
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.util.Duration;
import utilities.FxTimer;
import utilities.Log;

/**
 *
 * @author uranium
 */
final class Core {
    
    final SimpleObjectProperty<Metadata> currentMetadataCache = new SimpleObjectProperty(Metadata.EMPTY());
    final SimpleObjectProperty<Metadata> nextMetadataCache = new SimpleObjectProperty(Metadata.EMPTY());

    final SimpleObjectProperty<Metadata> selectedMetadata = new SimpleObjectProperty();


    void initialize(){

        // playing item changed
        //-> change current Metadata
        //-> wait, preload metadata for next item
        PlaylistManager.playingItemProperty().addListener((observable, oldV, newV) -> {
            // change current Metadata
            if (nextMetadataCache.get() != null && newV.same(nextMetadataCache.get())){
                Metadata oldM = currentMetadataCache.get();
                currentMetadataCache.set(nextMetadataCache.get());
                itemChange.fireEvent(true, oldM, nextMetadataCache.get());
                Log.deb("Metadata cache copied from next item metadata " + "cache.");
            }
            else {
                Log.deb("Metadata cache copy failed. " +
                        "Next item metadata cache content doesnt " +
                        "correspond to current item.");
                loadCurrentMetadataCache(true);
            }

            // wait 400ms, preload metadata for next item
            FxTimer.run(Duration.millis(400), () -> preloadNextMetadataCache());
        });
        
        PlaylistManager.selectedItemProperty().addListener(lastSelectedLoader);
        
    }

/******************************** current *************************************/
    
    final ItemChangeEvent itemChange = new ItemChangeEvent();

    private void loadCurrentMetadataCache(boolean changeType){
        PlaylistItem item = PlaylistManager.getPlayingItem();
        MetadataReader.create(item, (success, result) -> {
            if (success){
                Metadata oldM = currentMetadataCache.get();
                currentMetadataCache.set(result);
                itemChange.fireEvent(changeType, oldM, result);
                Log.deb("Current metadata cache loaded.");
            }
            else {
                Metadata oldM = currentMetadataCache.get();
                currentMetadataCache.set(item.toMetadata());
                itemChange.fireEvent(changeType, oldM, currentMetadataCache.get());
                Log.deb("Current metadata cache load fail. Metadata will be empty.");
            }
        });
    }

    void updateCurrent(){
        loadCurrentMetadataCache(false);
    }

    void loadCurrent(){
        loadCurrentMetadataCache(true);
    }

/********************************** next ***************************************/
    
    private void preloadNextMetadataCache(){
        PlaylistItem next = PlaylistManager.playingItemSelector.getNextPlaying();
        if (next == null){
            Log.deb("Next item metadata cache preloading prevented. No next playing item.");
            return;
        }
        MetadataReader.create(next,(success, result) -> {
            if (success){
                nextMetadataCache.set(result);
                Log.deb("Next item metadata cache preloaded.");
            } else {
                // dont set any value, not even empty
                Log.deb("Preloading next item metadata into cache failed.");
            }
        });
    }

/******************************** selected ************************************/
    
    ChangeListener<Item> lastSelectedLoader = (o,ov,nv) -> loadPlaylistSelectedMetadata(nv);
    
    void loadPlaylistSelectedMetadata(Item lastSelected) {
        if(lastSelected==null) {
            selectedMetadata.set(Metadata.EMPTY());
        } else {
            MetadataReader.create(lastSelected, (success,result) -> {
                if (success) {
                    Log.deb("In playlist last selected item metadata loaded.");
                    selectedMetadata.set(result);
                } else {
                    Log.deb("In playlist last selected item metadata reading failed.");
                    selectedMetadata.set(Metadata.EMPTY());
                }
            });
        }
    }
}
