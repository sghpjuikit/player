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
import Layout.Widgets.FXMLWidget;
import Layout.Widgets.Widget;
import static Layout.Widgets.Widget.Group.PLAYLIST;
import Layout.Widgets.WidgetManager;
import static Layout.Widgets.WidgetManager.WidgetSource.NO_LAYOUT;
import Layout.Widgets.controller.FXMLController;
import Layout.Widgets.controller.io.Output;
import Layout.Widgets.feature.PlaylistFeature;
import Layout.Widgets.feature.SongReader;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.*;
import gui.InfoNode.InfoTable;
import static gui.InfoNode.InfoTable.DEFAULT_TEXT_FACTORY;
import gui.objects.ActionChooser;
import gui.objects.PopOver.PopOver;
import gui.objects.SimpleConfigurator;
import gui.objects.Table.PlaylistTable;
import gui.objects.Table.TableColumnInfo;
import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import static javafx.geometry.NodeOrientation.INHERIT;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.Tooltip;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static javafx.scene.input.MouseEvent.MOUSE_RELEASED;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import static util.Util.consumeOnSecondaryButton;
import static util.Util.menuItem;
import util.access.Accessor;
import util.async.executor.LimitedExecutor;
import util.async.runnable.Run;
import static util.functional.Util.isNotNULL;
import util.graphics.Icons;
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

    @FXML VBox root;
    private @FXML Label duration;
    private @FXML StackPane optionPane;
    private final PlaylistTable table = new PlaylistTable();
    
    @FXML Menu addMenu;
    @FXML Menu remMenu;
    @FXML Menu selMenu;
    @FXML Menu orderMenu;
    
    private final Output<PlaylistItem> out_sel;
    
    // configurables
    @IsConfig(name = "Table orientation", info = "Orientation of the table.")
    public final Accessor<NodeOrientation> table_orient = new Accessor<>(INHERIT, table::setNodeOrientation);
    @IsConfig(name = "Zeropad numbers", info = "Adds 0 to uphold number length consistency.")
    public final Accessor<Boolean> zeropad = new Accessor<>(true, table::setZeropadIndex);
    @IsConfig(name = "Search show original index", info = "Show index of the table items as in unfiltered state when filter applied.")
    public final Accessor<Boolean> orig_index = new Accessor<>(true, table::setShowOriginalIndex);
    @IsConfig(name = "Show table header", info = "Show table header with columns.")
    public final Accessor<Boolean> show_header = new Accessor<>(true, table::setHeaderVisible);
    @IsConfig(name = "Show table menu button", info = "Show table menu button for setting up columns.")
    public final Accessor<Boolean> show_menu_button = new Accessor<>(false, table::setTableMenuButtonVisible);
    @IsConfig(name = "Show bottom header", info = "Show contorls pane at the bottom.")
    public final Accessor<Boolean> show_bottom_header = new Accessor<>(true, v -> {
        if(v) root.getChildren().setAll(table.getRoot(),optionPane);
        else root.getChildren().setAll(table.getRoot());
        optionPane.setVisible(v);
    });
    @IsConfig(name = "Play displayed only", info = "Only displayed items will be played. Applies search filter for playback.")
    public final Accessor<Boolean> filter_for_playback = new Accessor<>(false, v -> {
        String of = "Enable filter for playback. Causes the playback "
                  + "to play only displayed items.";
        String on = "Disable filter for playback. Causes the playback "
                  + "to ignore the filter.";
        Tooltip t = new Tooltip(v ? on : of);
                t.setWrapText(true);
                t.setMaxWidth(200);
        table.getSearchBox().setButton(v ? ERASER : FILTER, t, filterToggler());
        setUseFilterForPlayback(v);
    });
    
    private final LimitedExecutor runOnce = new LimitedExecutor(1);
    private final ListChangeListener<PlaylistItem> playlistitemsL = c -> 
            table.setItemsRaw((Collection) c.getList());
    private final InvalidationListener predicateL = o -> 
            PlaylistManager.playingItemSelector.setFilter((Predicate)table.predicate.get());
    
    public PlaylistController(FXMLWidget widget) {
        super(widget);
        out_sel = outputs.create(widget.id,"Selected", PlaylistItem.class, null);
        Player.playlistSelected.i.bind(out_sel);
        actPane = new ActionChooser(this);
    }
    
    @Override
    public void init() {        
        root.getChildren().setAll(table.getRoot(),optionPane);
        VBox.setVgrow(table.getRoot(), Priority.ALWAYS);
        
        // information label
        InfoTable<PlaylistItem> infoL = new InfoTable(duration, table);
        infoL.textFactory = (all, list) -> {
            if(list==null)return "";
            double d = list.stream().filter(isNotNULL).mapToDouble(PlaylistItem::getTimeMs).sum();
            return DEFAULT_TEXT_FACTORY.apply(all, list) + " - " + new FormattedDuration(d);
        };
        
        // for now...  either get rid of PM and allow multiple playlists OR allow binding
        PlaylistManager.getItems().addListener(playlistitemsL);
        
        // prevent scrol event to propagate up
        root.setOnScroll(Event::consume);
        
        // prevent overly eager selection change
        table.addEventFilter(MOUSE_PRESSED, consumeOnSecondaryButton);
        table.addEventFilter(MOUSE_RELEASED, consumeOnSecondaryButton);
        
        // menubar - change text to icons
        addMenu.setText("");
        remMenu.setText("");
        selMenu.setText("");
        orderMenu.setText("");
        Icons.setIcon(addMenu, PLUS, "11", "11");
        Icons.setIcon(remMenu, MINUS, "11", "11");
        Icons.setIcon(selMenu, CROP, "11", "11");
        Icons.setIcon(orderMenu, NAVICON, "11", "11");
        
        // add sort submenu
        Menu sortM = new Menu("Sort by");
        for(Field f : Field.values()) 
            sortM.getItems().add(menuItem(f.toStringEnum(), () -> table.sortBy(f)));
        orderMenu.getItems().add(0, sortM);
        
        // maintain outputs
        table.getSelectionModel().selectedItemProperty().addListener((o,ov,nv) -> out_sel.setValue(nv));
    }
    
    @Override
    public void onClose() {
        setUseFilterForPlayback(false);
        PlaylistManager.getItems().removeListener(playlistitemsL);
        Player.playlistSelected.i.unbind(out_sel);
        table.dispose();
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
        table_orient.applyValue();
        zeropad.applyValue();
        show_menu_button.applyValue();
        show_header.applyValue();
        show_bottom_header.applyValue();
        orig_index.applyValue();
        filter_for_playback.applyValue();
        table.setItemsRaw(PlaylistManager.getItems());
    }
    
    final ActionChooser<Supplier<File>> actPane;
    
    @Override
    public Node getActivityNode() {
        return actPane;
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
    
    
    @FXML public void reverseOrder() {
        PlaylistManager.reversePlaylist();
    }
    @FXML public void randomOrder() {
        PlaylistManager.randomizePlaylist();
    }
    
    @FXML
    public void tagEditSelected() {
        WidgetManager.use(SongReader.class,NO_LAYOUT,w->w.read(table.getSelectedItems()));
    }
    
    @FXML
    public void savePlaylist() {
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
    
    @FXML
    public void saveSelectedAsPlaylist() {
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
    
/***************************** HELPER METHODS *********************************/
    
    private void setUseFilterForPlayback(boolean v) {
        if(v) {
            filter_for_playback.setValue(true);
            table.predicate.addListener(predicateL);
            PlaylistManager.playingItemSelector.setFilter((Predicate)table.predicate.get());
        } else {
            filter_for_playback.setValue(false);
            table.predicate.removeListener(predicateL);
            PlaylistManager.playingItemSelector.setFilter(null);
        }
    }
    
    private Run filterToggler() {
        return filter_for_playback::setCycledNapplyValue;
    }
}