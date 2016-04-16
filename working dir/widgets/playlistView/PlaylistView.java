package playlistView;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.Menu;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;

import audio.Item;
import audio.Player;
import audio.playlist.Playlist;
import audio.playlist.PlaylistItem;
import audio.playlist.PlaylistItem.Field;
import audio.playlist.PlaylistManager;
import gui.Gui;
import gui.objects.icon.Icon;
import gui.objects.popover.PopOver;
import gui.objects.table.PlaylistTable;
import gui.objects.table.TableColumnInfo;
import layout.widget.Widget;
import layout.widget.controller.FXMLController;
import layout.widget.controller.io.Output;
import layout.widget.feature.PlaylistFeature;
import layout.widget.feature.SongReader;
import unused.SimpleConfigurator;
import util.access.V;
import util.access.Vo;
import util.async.executor.ExecuteN;
import util.conf.Config;
import util.conf.IsConfig;
import util.conf.ValueConfig;
import util.type.Util;
import util.units.FormattedDuration;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.FILTER;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.FILTER_OUTLINE;
import static gui.infonode.InfoTable.DEFAULT_TEXT_FACTORY;
import static java.util.stream.Collectors.toList;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import static layout.widget.Widget.Group.PLAYLIST;
import static layout.widget.WidgetManager.WidgetSource.NO_LAYOUT;
import static layout.widget.WidgetManager.WidgetSource.OPEN;
import static main.App.APP;
import static util.functional.Util.*;
import static util.graphics.Util.menuItem;
import static util.graphics.Util.setAnchors;
import static util.reactive.Util.maintain;

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
public class PlaylistView extends FXMLController implements PlaylistFeature {

    private @FXML AnchorPane root;
    private Playlist playlist;
    private PlaylistTable table;
    private final ExecuteN columnInitializer = new ExecuteN(1);

    private Output<PlaylistItem> outSelected, outPlaying;

    // configurables
    @IsConfig(name = "Table orientation", info = "Orientation of the table.")
    public final Vo<NodeOrientation> orient = new Vo<>(Gui.table_orient);
    @IsConfig(name = "Zeropad numbers", info = "Adds 0s for number length consistency.")
    public final Vo<Boolean> zeropad = new Vo<>(Gui.table_zeropad);
    @IsConfig(name = "Search show original index", info = "Show unfiltered table item index when filter applied.")
    public final Vo<Boolean> orig_index = new Vo<>(Gui.table_orig_index);
    @IsConfig(name = "Show table header", info = "Show table header with columns.")
    public final Vo<Boolean> show_header = new Vo<>(Gui.table_show_header);
    @IsConfig(name = "Show table footer", info = "Show table controls at the bottom of the table. Displays menubar and table items information.")
    public final Vo<Boolean> show_footer = new Vo<>(Gui.table_show_footer);
    @IsConfig(name = "Scroll to playing", info = "Scroll table to playing item when it changes.")
    public final V<Boolean> scrollToPlaying = new V<>(true);
    @IsConfig(name = "Play displayed only", info = "Only displayed items will be played when filter is active.")
    public final V<Boolean> filter_for_playback = new V<>(false, v -> {
        String of = "Enable filter for playback. Causes the playback "
                  + "to play only displayed items.";
        String on = "Disable filter for playback. Causes the playback "
                  + "to ignore the filter.";
        Tooltip t = new Tooltip(v ? on : of);
                t.setWrapText(true);
                t.setMaxWidth(200);
        Icon i = table.filterPane.getButton();
             i.icon(v ? FILTER : FILTER_OUTLINE);
             i.onClick(this::filterToggle);
             i.tooltip(v ? on : of);
             i.setDisable(false); // needed
        setUseFilterForPlayback(v);
    });

    @Override
    public void init() {

        // obtain playlist by id, we will use this widget's id
        UUID id = getWidget().id;
        playlist = PlaylistManager.playlists.getOr(id, getUnusedPlaylist(id));
        // maybe this widget was created & no playlist exists, add it to list
        PlaylistManager.playlists.add(playlist); // if exists, nothing happens
        // when widget closes we must remove the playlist or it would get saved
        // and playlist list would infinitely grow. When widgets close naturally
        // on app close, the playlist will get removed after app state was saved => no problem
        d(() -> PlaylistManager.playlists.remove(playlist));

        // widget input/output
        outSelected = outputs.create(widget.id,"Selected", Item.class, null);
        outPlaying = outputs.create(widget.id,"Playing", Item.class, null);
        d(Player.playlistSelected.i.bind(outSelected));
        d(maintain(playlist.playingI, ι -> playlist.getPlaying(), outPlaying));
        d(Player.onItemRefresh(ms -> {
            if(outPlaying.getValue()!=null)
                ms.ifHasK(outPlaying.getValue().getURI(), m -> outPlaying.setValue(m.toPlaylist()));
            if(outSelected.getValue()!=null)
                ms.ifHasK(outSelected.getValue().getURI(), m -> outSelected.setValue(m.toPlaylist()));
        }));

        table = new PlaylistTable(playlist);

        // add table to scene graph
        root.getChildren().add(table.getRoot());
        setAnchors(table.getRoot(),0d);

        // table properties
        table.setFixedCellSize(Gui.font.getValue().getSize() + 5);
        table.getSelectionModel().setSelectionMode(MULTIPLE);
        d(maintain(orient,table.nodeOrientationProperty()));
        d(maintain(zeropad,table.zeropadIndex));
        d(maintain(orig_index,table.showOriginalIndex));
        d(maintain(show_header,table.headerVisible));
        d(maintain(show_footer,table.footerVisible));
        d(maintain(scrollToPlaying,table.scrollToPlaying));

        // extend table items information
        table.items_info.textFactory = (all, list) -> {
            double Σms = list.stream().mapToDouble(PlaylistItem::getTimeMs).sum();
            return DEFAULT_TEXT_FACTORY.apply(all, list) + " - " + new FormattedDuration(Σms);
        };
        // add more menu items
        table.menuAdd.getItems().addAll(
            menuItem("Add files",PlaylistManager::chooseFilestoAdd),
            menuItem("Add directory",PlaylistManager::chooseFoldertoAdd),
            menuItem("Add URL",PlaylistManager::chooseUrltoAdd),
            menuItem("Play files",PlaylistManager::chooseFilesToPlay),
            menuItem("Play directory",PlaylistManager::chooseFolderToPlay),
            menuItem("Play URL",PlaylistManager::chooseUrlToPlay),
            menuItem("Duplicate selected (+)",() -> playlist.duplicateItemsByOne(table.getSelectedItems())),
            menuItem("Duplicate selected (*)",() -> playlist.duplicateItemsAsGroup(table.getSelectedItems()))
//            editOnAdd_menuItem    // add to lib option
//            editOnAdd_menuItem    // add to lib + edit option
        );
        table.menuRemove.getItems().addAll(
            menuItem("Remove selected", () -> playlist.removeAll(table.getSelectedItems())),
            menuItem("Remove not selected", () -> playlist.retainAll(table.getSelectedItems())),
            menuItem("Remove unsupported", playlist::removeUnplayable),
            menuItem("Remove duplicates", playlist::removeDuplicates),
            menuItem("Remove all", playlist::clear)
        );
        table.menuOrder.getItems().addAll(
            menuItem("Order reverse", playlist::reverse),
            menuItem("Order randomly", playlist::randomize),
            menuItem("Edit selected", () -> APP.widgetManager.use(SongReader.class,NO_LAYOUT,w->w.read(table.getSelectedItems()))),
            menuItem("Save selected as...", this::saveSelectedAsPlaylist),
            menuItem("Save playlist as...", this::savePlaylist)
        );
        Menu sortM = new Menu("Order by");
        stream(Field.values())
                .map(f -> menuItem(f.toStringEnum(), () -> table.sortBy(f)))
                .forEach(sortM.getItems()::add);
        table.menuOrder.getItems().add(0, sortM);

        // prevent scroll event to propagate up
        root.setOnScroll(Event::consume);

        // maintain outputs
        table.getSelectionModel().selectedItemProperty().addListener((o,ov,nv) -> {
            if(!table.movingitems)
                outSelected.setValue(nv);
        });

        d(table::dispose);
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
        columnInitializer.execute(() -> {
            String c = getWidget().properties.getSorThrow("columns");
            table.setColumnState(c==null ? table.getDefaultColumnInfo() : TableColumnInfo.fromString(c));
        });
        filter_for_playback.applyValue();
    }

    @Override
    public Playlist getPlaylist() {
        return playlist;
    }

/***************************** HELPER METHODS *********************************/

    void savePlaylist() {
        List<PlaylistItem> l = table.getItems();
        if(l.isEmpty()) return;

        String initialName = "ListeningTo " + new Date(System.currentTimeMillis());
        ValueConfig<String> mc = new ValueConfig<>(String.class, "Name", initialName);
        SimpleConfigurator sc = new SimpleConfigurator<>(mc, c -> {
            String n = c.getField("Name").getValue();
            Playlist p = new Playlist(UUID.randomUUID());
                     p.setAll(l);
                     p.serializeToFile(new File(APP.DIR_PLAYLISTS, n + ".xml"));
        });
        PopOver p = new PopOver<>(sc);
                p.title.set("Save playlist as...");
                p.show(PopOver.ScreenPos.App_Center);
    }

    void saveSelectedAsPlaylist() {
        List<PlaylistItem> l = table.getSelectedItems();
        if(l.isEmpty()) return;

        ValueConfig<String> mc = new ValueConfig<>(String.class, "Name", "My Playlist");
        SimpleConfigurator sc = new SimpleConfigurator<>(mc, c -> {
            String n = c.getField("Name").getValue();
            Playlist p = new Playlist(UUID.randomUUID());
                     p.setAll(l);
                     p.serializeToFile(new File(APP.DIR_PLAYLISTS, n + ".xml"));
        });
        PopOver p = new PopOver<>(sc);
                p.title.set("Save selected items as...");
                p.show(PopOver.ScreenPos.App_Center);
    }

    private void setUseFilterForPlayback(boolean v) {
        playlist.setTransformation(v
            ? orig -> list(table.getItems())
            : orig -> orig.stream().sorted(table.itemsComparator.get()).collect(toList())
        );
    }

    private void filterToggle() {
        filter_for_playback.setCycledNapplyValue();
    }

    private static Playlist getUnusedPlaylist(UUID id) {
        List<Playlist> pall = list(PlaylistManager.playlists);
        APP.widgetManager.findAll(OPEN).filter(w -> w.getInfo().hasFeature(PlaylistFeature.class))
                 .filter(w -> w.getController()!=null) // during load some widgets may not be loaded yet, this is not good
                 .map(w -> ((PlaylistFeature)w.getController()).getPlaylist())
                 .filter(ISNTØ)
                 .forEach(p -> pall.removeIf(pl -> pl.id.equals(p.id)));

        Playlist leaf = pall.isEmpty() ? null : pall.get(0);
        for(Playlist p : pall)
            if(p.id.equals(PlaylistManager.active))
                leaf = p;
        if(leaf!=null) PlaylistManager.playlists.remove(leaf);
        if(leaf!=null) {
            if(leaf.id.equals(PlaylistManager.active))
                PlaylistManager.active = id;
            Util.setField(Playlist.class, leaf, "id", id);
        }
        return leaf==null ? new Playlist(id) : leaf;
    }
}