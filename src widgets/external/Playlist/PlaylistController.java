package Playlist;


import AudioPlayer.playlist.NamedPlaylist;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.tagging.FormattedDuration;
import Configuration.IsConfig;
import Configuration.MapConfigurable;
import Configuration.ValueConfig;
import GUI.objects.PopOver.PopOver;
import GUI.objects.SimpleConfigurator;
import GUI.objects.Table.PlaylistTable;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Features.PlaylistFeature;
import Layout.Widgets.Features.TaggingFeature;
import Layout.Widgets.Widget;
import Layout.Widgets.WidgetManager;
import static Layout.Widgets.WidgetManager.WidgetSource.NOLAYOUT;
import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import static de.jensd.fx.fontawesome.AwesomeIcon.ERASER;
import static de.jensd.fx.fontawesome.AwesomeIcon.FILTER;
import java.util.Collection;
import java.util.Date;
import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import static javafx.geometry.NodeOrientation.INHERIT;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import utilities.Util;
import utilities.access.Accessor;

/**
 * Playlist FXML Controller class
 * Controls behavior of the Playlist FXML graphics.
 */
@Widget.Info(
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
            "    Click column : Changes sort order - ascending,\n" +
            "                   descending, none\n" +
            "    Click column + SHIFT : Sorts by multiple columns\n" +
            "    Drag column : Changes column order\n" +
            "    Menu bar : Opens additional actions\n",
    notes = "Plans: multiple playlists through tabs.\n" + 
            "Bugs: sorting through menubar buttons is broken",
    version = "1",
    year = "2014",
    group = Widget.Group.PLAYLIST
)
public class PlaylistController extends FXMLController implements PlaylistFeature {

    @FXML AnchorPane root;
    private @FXML Label duration;
    private @FXML AnchorPane tablePane;
    private @FXML AnchorPane optionPane;
    private final PlaylistTable table = new PlaylistTable();
    
    @FXML Menu addMenu;
    @FXML Menu remMenu;
    @FXML Menu selMenu;
    @FXML Menu orderMenu;
    
    // auto applied configurables
    @IsConfig(name = "Table orientation", info = "Orientation of table.")
    public final Accessor<NodeOrientation> table_orient = new Accessor<>(INHERIT, table::setNodeOrientation);
    @IsConfig(name = "Zeropad numbers", info = "Adds 0 to uphold number length consistency.")
    public final Accessor<Boolean> zeropad = new Accessor<>(true, table::zeropadIndex);
    @IsConfig(name = "Show table menu button", info = "Show table menu button for controlling columns.")
    public final Accessor<Boolean> show_menu_button = new Accessor<>(true, table::setTableMenuButtonVisible);
    @IsConfig(name = "Show table header", info = "Show table header with columns.")
    public final Accessor<Boolean> show_header = new Accessor<>(true, table::setHeaderVisible);
    @IsConfig(name = "Search show original index", info = "Show index of the itme as it was in the unfiltered playlist when filter applied.")
    public final Accessor<Boolean> orig_index = new Accessor<>(true, table::setShowOriginalIndex);
    @IsConfig(name = "Show bottom header", info = "Show contorls pane at the bottom.")
    public final Accessor<Boolean> show_bottom_header = new Accessor<>(true, v -> {
        optionPane.setVisible(v);
        AnchorPane.setBottomAnchor(tablePane, v ? 28d : 0d);
    });
    @IsConfig(name = "Play displayed only", info = "Only displayed items will be played. Applies search filter for playback.")
    public final Accessor<Boolean> filter_for_playback = new Accessor<>(false, v -> {
        String off = "      Enable filter for playback\n Causes the playback "
                   + "to play only displayed items.";
        String on = "       Disable filter for playback\n Causes the playback "
                  + "to ignore the filter.";
        Tooltip t = new Tooltip(v ? on : off);
                t.setWrapText(true);
                t.setMaxWidth(200);
        table.getSearchBox().setButton(v ? ERASER : FILTER, t, filterToggler());
        setUseFilterForPlayback(v);
    });
    
    // non applied configurables
    @IsConfig(name = "Search show always", info = "Forbid hiding of the search paneat all times. It will always be displayed.")
    public boolean always_show_search = false;
    @IsConfig(name = "Search hide always", info = "Allows hiding search pane even if in effect.")
    public boolean always_hide_search = false;
    @IsConfig(name = "Search ignore case", info = "Ignore case when comparing for search results.")
    public boolean ignoreCase = true;
    
    private final ListChangeListener<PlaylistItem> playlistitemsL = c -> 
            table.setItemsRaw((Collection<PlaylistItem>) c.getList());
    private final InvalidationListener predicateL = o -> 
            PlaylistManager.playingItemSelector.setFilter(table.getFilterPredicate());
    
    
    @Override
    public void init() {        
        tablePane.getChildren().add(table.getRoot());
        Util.setAPAnchors(table.getRoot(), 0d);
        
        // for now...  either get rid of PM and allow multiple playlists OR allow binding
        PlaylistManager.getItems().addListener(playlistitemsL);
        table.getItems().addListener((ListChangeListener.Change<?> c) -> updateLength());
        
        // consume scroll event to prevent other scroll behavior // optional
        table.setOnScroll(Event::consume);
        
        // menubar - change text to icons
        addMenu.setText("");
        remMenu.setText("");
        selMenu.setText("");
        orderMenu.setText("");
        AwesomeDude.setIcon(addMenu, AwesomeIcon.PLUS, "11", "11");
        AwesomeDude.setIcon(remMenu, AwesomeIcon.MINUS, "11", "11");
        AwesomeDude.setIcon(selMenu, AwesomeIcon.CROP, "11", "11");
        AwesomeDude.setIcon(orderMenu, AwesomeIcon.NAVICON, "11", "11");
    }

    @Override
    public void close() {
        // remove listeners
        setUseFilterForPlayback(false);
        PlaylistManager.getItems().removeListener(playlistitemsL);
        table.clearResources();
    }
    
/******************************** PUBLIC API **********************************/
    
    @Override
    public void refresh() {
        table_orient.applyValue();
        zeropad.applyValue();
        show_menu_button.applyValue();
        show_header.applyValue();
        show_bottom_header.applyValue();
        orig_index.applyValue();
        filter_for_playback.applyValue();
        table.setItemsRaw(PlaylistManager.getItems());
        updateLength();
    }
    
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
        PlaylistManager.removeItems(table.getSelectedItems());
    }
    @FXML public void removeUnselectedItems() {
        PlaylistManager.retainItems(table.getSelectedItems());
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
        PlaylistManager.duplicateItemsAsGroup(table.getSelectedItems());
    }
    @FXML public void duplicateSelectedItemsByOne() {
        PlaylistManager.duplicateItemsByOne(table.getSelectedItems());
    }
    
    @FXML public void selectAll() {
        table.selectAll();
    }
    @FXML public void selectInverse() {
        table.selectInverse();
    }
    @FXML public void selectNone() {
        table.selectNone();
    }
    
    @FXML public void sortByName() {
        table.sortByName();
    }
    @FXML public void sortByArtist() {
        table.sortByArtist();
    }
    @FXML public void sortByLength() {
        table.sortByLength();
    }
    @FXML public void sortByTitle() {
        table.sortByTitle();
    }
    @FXML public void sortByFilename() {
        table.sortByLocation();
    }
    
    @FXML public void reverseOrder() {
        PlaylistManager.reversePlaylist();
    }
    @FXML public void randomOrder() {
        PlaylistManager.randomizePlaylist();
    }
    
    @FXML
    public void tagEditSelected() {
        WidgetManager.use(TaggingFeature.class,NOLAYOUT,w->w.read(table.getSelectedItems()));
    }
    
    @FXML
    public void savePlaylist() {
        if(table.getItemsFiltered().isEmpty()) return;
        
        String initialName = "ListeningTo " + new Date(System.currentTimeMillis());
        MapConfigurable cs = new MapConfigurable(
                new ValueConfig("Name", initialName),
                new ValueConfig("Category", "Listening to..."));
        SimpleConfigurator sc = new SimpleConfigurator<String>(cs, c -> {
            String name = c.getField("Name").getValue();
            NamedPlaylist p = new NamedPlaylist(name, table.getItems());
                          p.addCategory("Listening to...");
                          p.serialize();
        });
        PopOver p = new PopOver(sc);
                p.setTitle("Save playlist as...");
                p.show(PopOver.ScreenCentricPos.AppCenter);
    }
    
    @FXML
    public void saveSelectedAsPlaylist() {
        if(table.getSelectedItems().isEmpty()) return;
        
        MapConfigurable mc = new MapConfigurable();
                        mc.addField(new ValueConfig("Name", "My Playlist"));
                        mc.addField(new ValueConfig("Category", "Custom"));
        SimpleConfigurator sc = new SimpleConfigurator<String>(mc, c -> {
            String name = c.getField("Name").getValue();
            String category = c.getField("Category").getValue();
            NamedPlaylist p = new NamedPlaylist(name, table.getSelectedItems());
                          p.addCategory(category);
                          p.serialize();
        });
        PopOver p = new PopOver(sc);
                p.setTitle("Save selected items as...");
                p.show(PopOver.ScreenCentricPos.AppCenter);
    }
    
/***************************** HELPER METHODS *********************************/
    
    private void updateLength() {
        double d = table.getItems().stream().mapToDouble(PlaylistItem::getTimeInMs).sum();
        duration.setText(table.getItems().size() + " items: " + new FormattedDuration(d));
    }
    
    private void setUseFilterForPlayback(boolean v) {
        if(v) {
            filter_for_playback.setValue(true);
            table.getItemsFiltered().predicateProperty().addListener(predicateL);
            PlaylistManager.playingItemSelector.setFilter(table.getFilterPredicate());
        } else {
            filter_for_playback.setValue(false);
            table.getItemsFiltered().predicateProperty().removeListener(predicateL);
            PlaylistManager.playingItemSelector.setFilter(null);
        }
    }
    
    private EventHandler<MouseEvent> filterToggler() {
        return e -> {
            filter_for_playback.setCycledNapplyValue();
            e.consume();
        };
    }
}