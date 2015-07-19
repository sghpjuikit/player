package Playlist;


import AudioPlayer.Player;
import AudioPlayer.playlist.NamedPlaylist;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistItem.Field;
import AudioPlayer.playlist.PlaylistManager;
import Configuration.Config;
import Configuration.IsConfig;
import Configuration.MapConfigurable;
import Configuration.ValueConfig;
import Layout.Widgets.Widget;
import static Layout.Widgets.Widget.Group.PLAYLIST;
import Layout.Widgets.WidgetManager;
import static Layout.Widgets.WidgetManager.WidgetSource.NO_LAYOUT;
import Layout.Widgets.controller.FXMLController;
import Layout.Widgets.controller.io.Output;
import Layout.Widgets.feature.PlaylistFeature;
import Layout.Widgets.feature.SongReader;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.*;
import static gui.InfoNode.InfoTable.DEFAULT_TEXT_FACTORY;
import gui.objects.ActionChooser;
import gui.objects.PopOver.PopOver;
import gui.objects.Table.PlaylistTable;
import gui.objects.Table.TableColumnInfo;
import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.control.Menu;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import org.reactfx.Subscription;
import unused.SimpleConfigurator;
import static util.Util.menuItem;
import static util.Util.setAnchors;
import util.access.Accessor;
import util.async.executor.LimitedExecutor;
import util.async.runnable.Run;
import static util.functional.Util.isNotNULL;
import static util.reactive.Util.maintain;
import util.units.FormattedDuration;

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
            "    Item drag : Activates Drag&Drop\n" +
            "    Item drag + CTRL : Moves item within playlist\n" +
            "    Type : search & filter\n" +
            "    Press ENTER : Plays item\n" +
            "    Press ESC : Clear selection & filter\n" +
            "    Scroll : Scroll table vertically\n" +
            "    Scroll + SHIFT : Scroll table horizontally\n" +
            "    Column drag : swap columns\n" +
            "    Column right click: show column menu\n" +
            "    Click column : Sort - ascending | descending | none\n" +
            "    Click column + SHIFT : Sorts by multiple columns\n" +
            "    Menu bar : Opens additional actions\n",
    notes = "Plans: multiple playlists through tabs",
    version = "1",
    year = "2015",
    group = PLAYLIST
)
public class PlaylistController extends FXMLController implements PlaylistFeature {

    private @FXML AnchorPane root;
    private final PlaylistTable table = new PlaylistTable();
    
    private Output<PlaylistItem> out_sel;
    
    ActionChooser<Supplier<File>> actPane;
    
    // configurables
    @IsConfig(name = "Table orientation", info = "Orientation of the table.")
    public final ObjectProperty<NodeOrientation> table_orient = table.nodeOrientationProperty();
    @IsConfig(name = "Zeropad numbers", info = "Adds 0s for number length consistency.")
    public final BooleanProperty zeropad = table.zeropadIndex;
    @IsConfig(name = "Search show original index", info = "Show unfiltered table item index when filter applied.")
    public final BooleanProperty orig_index = table.showOriginalIndex;
    @IsConfig(name = "Show table header", info = "Show table header with columns.")
    public final BooleanProperty show_header = table.headerVisible;
    @IsConfig(name = "Play displayed only", info = "Only displayed items will be played. Applies search filter for playback.")
    public final Accessor<Boolean> filter_for_playback = new Accessor<>(false, v -> {
        String of = "Enable filter for playback. Causes the playback "
                  + "to play only displayed items.";
        String on = "Disable filter for playback. Causes the playback "
                  + "to ignore the filter.";
        Tooltip t = new Tooltip(v ? on : of);
                t.setWrapText(true);
                t.setMaxWidth(200);
        table.filterPane.setButton(v ? ERASER : FILTER, t, filterToggler());
        setUseFilterForPlayback(v);
    });
    
    private final LimitedExecutor runOnce = new LimitedExecutor(1);
    private final ListChangeListener<PlaylistItem> playlistitemsL = c -> 
            table.setItemsRaw((Collection) c.getList());
    private final InvalidationListener predicateL = o -> 
            PlaylistManager.playingItemSelector.setFilter((Predicate)table.itemsPredicate.get());
    
    // disposables
    Subscription d;
    
    @Override
    public void init() {        
        out_sel = outputs.create(widget.id,"Selected", PlaylistItem.class, null);
        Player.playlistSelected.i.bind(out_sel);
        actPane = new ActionChooser(this);
        
        // add table to scene graph
        root.getChildren().add(table.getRoot());
        setAnchors(table.getRoot(),0);
        
        // table properties
        table.setFixedCellSize(gui.GUI.font.getValue().getSize() + 5);
        table.getSelectionModel().setSelectionMode(MULTIPLE);
        d = maintain(gui.GUI.show_table_controls,table.bottomControlsVisible);
        
        // extend table items information
        table.items_info.textFactory = (all, list) -> {
            double d = list.stream().filter(isNotNULL).mapToDouble(PlaylistItem::getTimeMs).sum();
            return DEFAULT_TEXT_FACTORY.apply(all, list) + " - " + new FormattedDuration(d);
        };
        // add more menu items
        table.menuAdd.getItems().addAll(
            menuItem("Add files",PlaylistManager::chooseFilestoAdd),
            menuItem("Add directory",PlaylistManager::chooseFoldertoAdd),
            menuItem("Add URL",PlaylistManager::chooseUrltoAdd),
            menuItem("Play files",PlaylistManager::chooseFilesToPlay),
            menuItem("Play directory",PlaylistManager::chooseFolderToPlay),
            menuItem("Play URL",PlaylistManager::chooseUrlToPlay),
            menuItem("Duplicate selected (+)",() -> PlaylistManager.duplicateItemsByOne(table.getSelectedItems())),
            menuItem("Duplicate selected (*)",() -> PlaylistManager.duplicateItemsAsGroup(table.getSelectedItems()))
//            editOnAdd_menuItem    // add to lib option
//            editOnAdd_menuItem    // add to lib + edit option
        );
        table.menuRemove.getItems().addAll(
            menuItem("Remove selected", () -> PlaylistManager.removeItems(table.getSelectedItems())),
            menuItem("Remove not selected", () -> PlaylistManager.retainItems(table.getSelectedItems())),
            menuItem("Remove unsupported", PlaylistManager::removeCorrupt),
            menuItem("Remove duplicates", PlaylistManager::removeDuplicates),
            menuItem("Remove all", PlaylistManager::removeAllItems)
        );
        table.menuSelected.getItems().addAll(
            menuItem("Select inverse", table::selectInverse),
            menuItem("Select all", table::selectAll),
            menuItem("Select none", table::selectNone)
        );
        table.menuOrder.getItems().addAll(
            menuItem("Order reverse", PlaylistManager::reversePlaylist),
            menuItem("Order randomly", PlaylistManager::randomizePlaylist),
            menuItem("Edit selected", () -> WidgetManager.use(SongReader.class,NO_LAYOUT,w->w.read(table.getSelectedItems()))),
            menuItem("Save selected as...", this::saveSelectedAsPlaylist),
            menuItem("Save playlist as...", this::savePlaylist)
        );
        Menu sortM = new Menu("Order by");
        for(Field f : Field.values()) 
            sortM.getItems().add(menuItem(f.toStringEnum(), () -> table.sortBy(f)));
        table.menuOrder.getItems().add(0, sortM);
        
        // for now...  either get rid of PM and allow multiple playlists OR allow binding
        PlaylistManager.getItems().addListener(playlistitemsL);
        
        // prevent scrol event to propagate up
        root.setOnScroll(Event::consume);
        
        // maintain outputs
        table.getSelectionModel().selectedItemProperty().addListener((o,ov,nv) -> out_sel.setValue(nv));
    }
    
    @Override
    public void onClose() {
        setUseFilterForPlayback(false);
        PlaylistManager.getItems().removeListener(playlistitemsL);
        Player.playlistSelected.i.unbind(out_sel);
        table.dispose();
        d.unsubscribe();
    }
    
    @Override
    public Collection<Config<Object>> getFields() {
        // serialize column state when requested
        getWidget().properties.put("columns", table.getColumnState().toString());
        return super.getFields();
    }
    
/******************************** PUBLIC API **********************************/
    
    @Override
    public void refresh() {
        runOnce.execute(() -> {
            String c = getWidget().properties.getS("columns");
            table.setColumnState(c==null ? table.getDefaultColumnInfo() : TableColumnInfo.fromString(c));
        });
        filter_for_playback.applyValue();
        table.setItemsRaw(PlaylistManager.getItems());
    }
    
    @Override
    public Node getActivityNode() {
        return actPane;
    }
    
/***************************** HELPER METHODS *********************************/
    
    void savePlaylist() {
        if(table.getItems().isEmpty()) return;
        
        String initialName = "ListeningTo " + new Date(System.currentTimeMillis());
        MapConfigurable mc = new MapConfigurable(
                new ValueConfig("Name", initialName),
                new ValueConfig("Category", "Listening to..."));
        SimpleConfigurator sc = new SimpleConfigurator<String>(mc, c -> {
            String name = c.getField("Name").getValue();
            NamedPlaylist p = new NamedPlaylist(name, table.getItems());
                          p.addCategory("Listening to...");
                          p.serialize();
        });
        PopOver p = new PopOver(sc);
                p.title.set("Save playlist as...");
                p.show(PopOver.ScreenCentricPos.App_Center);
    }
    
    void saveSelectedAsPlaylist() {
        if(table.getSelectedItems().isEmpty()) return;
        
        MapConfigurable mc = new MapConfigurable(
                        new ValueConfig("Name", "My Playlist"),
                        new ValueConfig("Category", "Custom"));
        SimpleConfigurator sc = new SimpleConfigurator<String>(mc, c -> {
            String name = c.getField("Name").getValue();
            String category = c.getField("Category").getValue();
            NamedPlaylist p = new NamedPlaylist(name, table.getSelectedItems());
                          p.addCategory(category);
                          p.serialize();
        });
        PopOver p = new PopOver(sc);
                p.title.set("Save selected items as...");
                p.show(PopOver.ScreenCentricPos.App_Center);
    }
    
    private void setUseFilterForPlayback(boolean v) {
        if(v) {
            filter_for_playback.setValue(true);
            table.itemsPredicate.addListener(predicateL);
            PlaylistManager.playingItemSelector.setFilter((Predicate)table.itemsPredicate.get());
        } else {
            filter_for_playback.setValue(false);
            table.itemsPredicate.removeListener(predicateL);
            PlaylistManager.playingItemSelector.setFilter(null);
        }
    }
    
    private Run filterToggler() {
        return filter_for_playback::setCycledNapplyValue;
    }
}