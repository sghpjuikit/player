package Playlist;


import AudioPlayer.playlist.NamedPlaylist;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import Configuration.IsConfig;
import GUI.Dialogs.ContentDialog;
import GUI.ItemHolders.PlaylistTable;
import Layout.Widgets.FXMLController;
import Layout.Widgets.SupportsTagging;
import Layout.Widgets.WidgetManager;
import java.util.Date;
import java.util.function.Predicate;
import javafx.beans.value.ChangeListener;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;
import utilities.Util;



/**
 * Playlist FXML Controller class
 * Controls behavior of the Playlist FXML graphics.
 */
public class PlaylistController extends FXMLController {

    @FXML AnchorPane root;
    @FXML TextField searchBox;
    @FXML Label duration;
    @FXML AnchorPane searchPane;
    @FXML AnchorPane tablePane;
    @FXML AnchorPane optionPane;
    private PlaylistTable playlist;
    
    // properties
    @IsConfig(name = "Table orientation", info = "Orientation of table.")
    public NodeOrientation table_orient = NodeOrientation.INHERIT;
    @IsConfig(name = "Table text orientation", info = "Orientation of text within table cells.")
    public Pos cell_align = Pos.CENTER_LEFT;
    @IsConfig(name = "Zeropad numbers", info = "Adds 0 to uphold number length consistency.")
    public boolean zeropad = false;
    @IsConfig(name = "Show table menu button", info = "Show table menu button for controlling columns.")
    public boolean show_menu_button = true;
    @IsConfig(name = "Show table header", info = "Show table header with columns.")
    public boolean show_header = true;
    @IsConfig(name = "Search show always", info = "Forbid hiding of the search paneat all times. It will always be displayed.")
    public boolean always_show_search = false;
    @IsConfig(name = "Search hide always", info = "Allows hiding search pane even if in effect.")
    public boolean always_hide_search = false;
    @IsConfig(name = "Search ignore case", info = "Ignore case when comparing for search results.")
    public boolean ignoreCase = true;
    @IsConfig(name = "Search show original index", info = "Show index of the itme as it was in the unfiltered playlisteven when filter applied.")
    public boolean orig_index = true;
    
    @Override
    public void init() {
        searchBox.setPromptText("search playlist");
        playlist = new PlaylistTable(tablePane);
        playlist.setItems(PlaylistManager.getItems());

        PlaylistManager.lengthProperty().addListener(lengthListener);   // add listener
        updateLength(PlaylistManager.getLength());                      // init value
        
        
        // hide search on click outside | on press ESCAPE
        playlist.getTable().setOnMousePressed(e->hideFilter());
        optionPane.setOnMousePressed(e->hideFilter());
        
        root.setOnKeyPressed( e -> {
            // cancel search on ESC | BACKSPACE
            if(e.getCode()==KeyCode.BACK_SPACE) {
                cancelFilter();
                hideFilter();
                e.consume();
                return;
            }
            // activate search when 
            //    pressing CTRL+F
            if(e.isControlDown() && e.getCode()==KeyCode.F) {
                if(isFilterVisible()) {
                    cancelFilter();
                    hideFilter();
                } else {
                    showFilter();
                    searchBox.setText(e.getCharacter());
                    searchBox.requestFocus();
                }
                e.consume();
                return;
            }
            //    typing (only letters | numbers)
            if( !e.isShortcutDown() &&
                    (e.getCode().isLetterKey() || e.getCode().isDigitKey())) {
                showFilter();
                searchBox.setText(e.getCharacter());
                searchBox.requestFocus();
                searchBox.selectRange(1, 1);
                e.consume();
            }
        });
        root.addEventFilter(KeyEvent.KEY_PRESSED, e->{ // filter, because table consumes ESCAPE pressed handler
            // cancel search on ESC | BACKSPACE
            if(e.getCode()==KeyCode.ESCAPE) {
                cancelFilter();
                hideFilter();
                e.consume();
            }
        });
        // cancel search from searchField on ESC | BACKSPACE if empty
        searchBox.setOnKeyPressed(e->{
            if(e.getCode()==KeyCode.ESCAPE || (e.getCode()==KeyCode.BACK_SPACE && searchBox.getText().isEmpty())) {
                cancelFilter();
                hideFilter();
            }
        });

        
        // search on text change
        searchBox.textProperty().addListener((o,oldV,newV)->{
            filter(newV);
        });
        
        // consume scroll event to prevent other scroll behavior // optional
        playlist.getTable().setOnScroll(Event::consume);
        
    }
    
    @Override
    public void refresh() {
        cancelFilter();
        
        playlist.setShowOriginalIndex(orig_index);
        playlist.zeropadIndex(zeropad);
        playlist.setNodeOrientation(table_orient);
        playlist.setCellAlign(cell_align);
        playlist.setMenuButtonVisible(show_menu_button);
        playlist.setHeaderVisible(show_header);
        playlist.refresh();
    }

    @Override
    public void OnClosing() {
        // remove listeners
        PlaylistManager.lengthProperty().removeListener(lengthListener);
        playlist.clearResources();
        if(active) deactivateUseFilterForPlayback();
        
    }
    
/******************************************************************************/
    
    ChangeListener<Duration> lengthListener = (o,oldV,newV) -> updateLength(newV);
    
    private void updateLength(Duration d) {
        duration.setText(Util.formatDuration(d));
    }
    
/******************************************************************************/   
    @FXML public void chooseFiles() {
        PlaylistManager.chooseFilestoAdd();
    }
    
    @FXML public void chooseFolder() {
        PlaylistManager.chooseFoldertoAdd();
    }
    
    @FXML public void chooseUrl() {
        PlaylistManager.chooseUrltoAdd();
    }
    
    @FXML public void removeSelectedItems() {
        PlaylistManager.removeItems(playlist.getSelectedItems());
    }
    @FXML public void removeUnselectedItems() {
        PlaylistManager.retainItems(playlist.getSelectedItems());
    }
    @FXML public void removeUnplayableItems() {
        PlaylistManager.removeCorrupt();
    }
    @FXML public void removeDuplicateItems() {
        PlaylistManager.removeDuplicates();
    }
    @FXML public void removeAllItems() {
        PlaylistManager.removeAllItems();
    }
    @FXML public void duplicateSelectedItemsAsGroup() {
        PlaylistManager.duplicateItemsAsGroup(playlist.getSelectedItems());
    }
    @FXML public void duplicateSelectedItemsByOne() {
        PlaylistManager.duplicateItemsByOne(playlist.getSelectedItems());
    }
    
    @FXML public void selectAll() {
        playlist.selectAll();
    }
    @FXML public void selectInverse() {
        playlist.selectInverse();
    }
    @FXML public void selectNone() {
        playlist.selectNone();
    }
    
    @FXML public void sortByName() {
        playlist.sortByName();
    }
    @FXML public void sortByArtist() {
        playlist.sortByArtist();
    }
    @FXML public void sortByLength() {
        playlist.sortByLength();
    }
    @FXML public void sortByTitle() {
        playlist.sortByTitle();
    }
    @FXML public void sortByFilename() {
        playlist.sortByLocation();
    }
    
    @FXML public void reverseOrder() {
        PlaylistManager.reversePlaylist();
    }
    @FXML public void randomOrder() {
        PlaylistManager.randomizePlaylist();
    }
    
    @FXML
    public void tagEditSelected() {
        SupportsTagging t = WidgetManager.getTaggerOrCreate();
        if (t!=null) t.read(playlist.getSelectedItems());
    }
    @FXML
    public void savePlaylist() {
        if(playlist.getItemsF().isEmpty()) return;
        // build content
        TextField f = new TextField();
                  f.setText("ListeningTo " + new Date(System.currentTimeMillis()));
                  f.setPromptText("Playlist name");
        // build dialog
        ContentDialog<TextField> dialog = new ContentDialog();
        dialog.setContent(f);
        dialog.setTitle("Save as...");
        dialog.setOnOk( c -> {
            String name = c.getText();
            NamedPlaylist p = new NamedPlaylist(name, playlist.getItemsF());
                          p.addCategory("Listening to...");
                          p.serialize();
            return true;
        });
        f.textProperty().addListener(text -> dialog.setMessagee(""));
        dialog.show();
    }
    @FXML
    public void saveSelectedAsPlaylist() {
        if(playlist.getSelectedItems().isEmpty()) return;
        // build content
        TextField f = new TextField();
                  f.setPromptText("Playlist name");
        // build dialog
        ContentDialog<TextField> dialog = new ContentDialog();
        dialog.setContent(f);
        dialog.setTitle("Save as...");
        dialog.setOnOk( c -> {
            String name = c.getText();
            NamedPlaylist p = new NamedPlaylist(name, playlist.getSelectedItems());
                          p.addCategory("Listening to...");
                          p.serialize();
            return true;
        });
        f.textProperty().addListener(text -> dialog.setMessagee(""));
        dialog.show();
    }
    
/******************************* SEARCHING ************************************/
        
    private void showFilter() {
        searchPane.setVisible(true);
        AnchorPane.setTopAnchor(tablePane, 28d);
    }
    private void hideFilter() {
        if(always_show_search) return;
        if(!always_hide_search && isFilterOn()) return;
        
        searchPane.setVisible(false);
        AnchorPane.setTopAnchor(tablePane, 0d);
    }
    private boolean isFilterVisible() {
        return searchPane.isVisible();
    }
    
    public void filter(String text) {
        if(text==null || text.isEmpty()){
            playlist.getItemsF().setPredicate(item->true);
        } else {
            Predicate<PlaylistItem> filter = ignoreCase 
                ? item->item.getName().toLowerCase().contains(text.toLowerCase())
                : item->item.getName().contains(text);
            playlist.getItemsF().setPredicate(filter);
        }
    }
    
    public void cancelFilter() {
        searchBox.setText("");  // this should disable filter but doesnt
        filter("");             // disable filter manually 
        hideFilter();           // init visibility for search
    }
    
    public boolean isFilterOn() {
        return PlaylistManager.getItems().size() != playlist.getItemsF().size();
    }
    
    private boolean active = false;
    public void toggleUseFilterForPlayback() {
        if(active)
            deactivateUseFilterForPlayback();
        else
            activateUseFilterForPlayback();
    }
    public void activateUseFilterForPlayback() {
        if(active) return;
        active = true;
        playlist.getItemsF().predicateProperty().addListener(o->
            PlaylistManager.playingItemSelector.setFilter((Predicate<PlaylistItem>)playlist.getItemsF().getPredicate())
        );
        PlaylistManager.playingItemSelector.setFilter((Predicate<PlaylistItem>)playlist.getItemsF().getPredicate());
    }
    public void deactivateUseFilterForPlayback() {
        if(!active) return;
        active = false;
        playlist.getItemsF().predicateProperty().addListener(o->{});
        PlaylistManager.playingItemSelector.setFilter(null);
    }
}