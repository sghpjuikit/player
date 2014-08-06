package Playlist;


import AudioPlayer.playlist.NamedPlaylist;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import Configuration.IsConfig;
import Configuration.MapConfigurable;
import Configuration.ValueConfig;
import GUI.objects.PlaylistTable;
import GUI.objects.PopOver.PopOver;
import GUI.objects.SimpleConfigurator;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Features.PlaylistFeature;
import Layout.Widgets.Features.TaggingFeature;
import Layout.Widgets.Widget;
import Layout.Widgets.WidgetInfo;
import Layout.Widgets.WidgetManager;
import Layout.Widgets.WidgetManager.Widget_Source;
import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import static de.jensd.fx.fontawesome.AwesomeIcon.ERASER;
import static de.jensd.fx.fontawesome.AwesomeIcon.FILTER;
import java.util.Date;
import java.util.function.Predicate;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;
import utilities.Util;



/**
 * Playlist FXML Controller class
 * Controls behavior of the Playlist FXML graphics.
 */
@WidgetInfo(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Playlist",
    description = "Provides list of items to play. Highlights playing and unplayable "
                + "items.",
    howto = "Available actions:\n" +
            "    Item left click : Selects item\n" +
            "    Item right click : Opens context menu\n" +
            "    Item double click : Plays item\n" +
            "    Item drag : Moves item within playlist\n" +
            "    Item drag + CTRL : Activates Drag&Drop\n" +
            "    Press ENTER : Plays item\n" +
            "    Press ESC : Clear selection & filter\n" +
            "    Type : Searches for item - applies filter\n" +
            "    Filter button : Uses filter for playback\n" +
            "    Drag column : Changes column order\n" +
            "    Click column : Changes sort order - ascending,\n" +
            "                   descending, none\n" +
            "    Click column + SHIFT : Sorts by multiple columns\n" +
            "    Menu bar : Opens additional actions\n",
    notes = "Plans: multiple playlists through tabs.\n" + 
            "Bugs: sorting through menubar broken",
    version = "0.9",
    year = "2014",
    group = Widget.Group.PLAYLIST
)
public class PlaylistController extends FXMLController implements PlaylistFeature {

    @FXML AnchorPane root;
    @FXML TextField searchBox;
    @FXML Label duration;
    @FXML AnchorPane searchPane;
    @FXML AnchorPane tablePane;
    @FXML AnchorPane optionPane;
    private PlaylistTable playlist;
    
    @FXML Menu addMenu;
    @FXML Menu remMenu;
    @FXML Menu selMenu;
    @FXML Menu orderMenu;
    @FXML Button filterB;
    
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
        
        // filter button - change text for icon
        filterB.setText("");
        AwesomeDude.setIcon(filterB, filter_for_playback.get() ? ERASER : FILTER, "11");
        String fOff = "      Enable filter for playback\n Causes the playback "
                    + "to play only displayed items.";
        String fOn = "       Disable filter for playback\n Causes the playback "
                    + "to ignore the filter.";
        Tooltip ft = new Tooltip(filter_for_playback.get() ? fOn : fOff);
                ft.setWrapText(true);
                ft.setMaxWidth(200);
        filterB.setTooltip(ft);
        // monitor value for icon change
        filter_for_playback.addListener((o,ov,nv) -> {
            AwesomeDude.setIcon(filterB, nv ? ERASER : FILTER, "11");
            ft.setText(nv ? fOn : fOff);
        });
        
        // menubar - change text to icons
        addMenu.setText("");
        remMenu.setText("");
        selMenu.setText("");
        orderMenu.setText("");
        AwesomeDude.setIcon(addMenu, AwesomeIcon.PLUS, "11", "11");
        AwesomeDude.setIcon(remMenu, AwesomeIcon.MINUS, "11", "11");
        AwesomeDude.setIcon(selMenu, AwesomeIcon.CROP, "11", "11");
        AwesomeDude.setIcon(orderMenu, AwesomeIcon.NAVICON, "11", "11");
        
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
            if( !e.isShortcutDown() && !e.isAltDown() &&
                    (e.getCode().isLetterKey() || e.getCode().isDigitKey())) {
                showFilter();
                searchBox.setText(e.getCharacter());
                searchBox.requestFocus();
                searchBox.selectRange(1, 1);
                e.consume();
            }
        });
        root.addEventHandler(KeyEvent.KEY_PRESSED, e->{ // filter, because table consumes ESCAPE pressed handler
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
        searchBox.textProperty().addListener((o,ov,nv) -> filter(nv));
        
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
        if(filter_for_playback.get()) deactivateUseFilterForPlayback();
        
    }
    
/******************************************************************************/
    
    ChangeListener<Duration> lengthListener = (o,ov,nv) -> updateLength(nv);
    
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
        Widget w = WidgetManager.getWidget(TaggingFeature.class,Widget_Source.FACTORY);
        if (w!=null) {
            TaggingFeature t = (TaggingFeature) w.getController();
                           t.read(playlist.getSelectedItems());
        }
    }
    @FXML
    public void savePlaylist() {
        if(playlist.getItemsF().isEmpty()) return;
        
        String initialName = "ListeningTo " + new Date(System.currentTimeMillis());
        MapConfigurable cs = new MapConfigurable(
                new ValueConfig("Name", initialName),
                new ValueConfig("Category", "Listening to..."));
        SimpleConfigurator sc = new SimpleConfigurator(cs, c -> {
            String name = (String) c.getField("Name").getValue();
            NamedPlaylist p = new NamedPlaylist(name, playlist.getItemsF());
                          p.addCategory("Listening to...");
                          p.serialize();
        });
        PopOver p = new PopOver(sc);
                p.setTitle("Save playlist as...");
                p.show(PopOver.ScreenCentricPos.AppCenter);
    }
    @FXML
    public void saveSelectedAsPlaylist() {
        if(playlist.getSelectedItems().isEmpty()) return;
        
        MapConfigurable cs = new MapConfigurable(
                new ValueConfig("Name", "My Playlist"),
                new ValueConfig("Category", "Custom"));
        SimpleConfigurator sc = new SimpleConfigurator(cs, c -> {
            String name = (String) c.getField("Name").getValue();
            String category = (String) c.getField("Category").getValue();
            NamedPlaylist p = new NamedPlaylist(name, playlist.getItemsF());
                          p.addCategory(category);
                          p.serialize();
        });
        PopOver p = new PopOver(sc);
                p.setTitle("Save selected items as...");
                p.show(PopOver.ScreenCentricPos.AppCenter);
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
    
    
    private final BooleanProperty filter_for_playback = new SimpleBooleanProperty(false);
    
    public void toggleUseFilterForPlayback() {
        if(filter_for_playback.get())
            deactivateUseFilterForPlayback();
        else
            activateUseFilterForPlayback();
    }
    
    public void activateUseFilterForPlayback() {
        // avoid pointless operation
        if(filter_for_playback.get()) return;
        
        filter_for_playback.set(true);
        playlist.getItemsF().predicateProperty().addListener(o->
            PlaylistManager.playingItemSelector.setFilter((Predicate<PlaylistItem>)playlist.getItemsF().getPredicate())
        );
        PlaylistManager.playingItemSelector.setFilter((Predicate<PlaylistItem>)playlist.getItemsF().getPredicate());
    }
    
    public void deactivateUseFilterForPlayback() {
        // avoid pointless operation
        if(!filter_for_playback.get()) return;
        
        filter_for_playback.set(false);
        playlist.getItemsF().predicateProperty().addListener(o->{});
        PlaylistManager.playingItemSelector.setFilter(null);
    }
}