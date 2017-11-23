package playlistView;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.Menu;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import sp.it.pl.audio.Item;
import sp.it.pl.audio.Player;
import sp.it.pl.audio.playlist.Playlist;
import sp.it.pl.audio.playlist.PlaylistItem;
import sp.it.pl.audio.playlist.PlaylistItem.Field;
import sp.it.pl.audio.playlist.PlaylistManager;
import sp.it.pl.gui.Gui;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.pl.gui.objects.table.PlaylistTable;
import sp.it.pl.gui.objects.table.TableColumnInfo;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.controller.FXMLController;
import sp.it.pl.layout.widget.controller.io.Output;
import sp.it.pl.layout.widget.feature.PlaylistFeature;
import sp.it.pl.layout.widget.feature.SongReader;
import sp.it.pl.util.access.V;
import sp.it.pl.util.access.Vo;
import sp.it.pl.util.async.executor.ExecuteN;
import sp.it.pl.util.conf.Config;
import sp.it.pl.util.conf.IsConfig;
import sp.it.pl.util.type.Util;
import sp.it.pl.util.units.Dur;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.FILTER;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.FILTER_OUTLINE;
import static java.util.stream.Collectors.toList;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import static sp.it.pl.gui.infonode.InfoTable.DEFAULT_TEXT_FACTORY;
import static sp.it.pl.layout.widget.Widget.Group.PLAYLIST;
import static sp.it.pl.layout.widget.WidgetManager.WidgetSource.NO_LAYOUT;
import static sp.it.pl.layout.widget.WidgetManager.WidgetSource.OPEN;
import static sp.it.pl.main.App.APP;
import static sp.it.pl.main.AppBuildersKt.appTooltip;
import static sp.it.pl.util.functional.Util.ISNTØ;
import static sp.it.pl.util.functional.Util.list;
import static sp.it.pl.util.functional.Util.stream;
import static sp.it.pl.util.graphics.Util.menuItem;
import static sp.it.pl.util.graphics.Util.setAnchors;
import static sp.it.pl.util.reactive.Util.maintain;

/**
 * Playlist FXML Controller class
 * Controls behavior of the Playlist FXML graphics.
 */
@Widget.Info(
    author = "Martin Polakovic",
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
        Tooltip t = appTooltip(v ? on : of);
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
        playlist = PlaylistManager.playlists.computeIfAbsent(id, PlaylistView::getUnusedPlaylist);
        // when widget closes we must remove the playlist or it would get saved
        // and playlist list would infinitely grow. When widgets close naturally
        // on app close, the playlist will get removed after app state was saved => no problem
        d(() -> PlaylistManager.playlists.remove(playlist));

        // widget input/output
        outSelected = outputs.create(id,"Selected", Item.class, null);
        outPlaying = outputs.create(id,"Playing", Item.class, null);
        d(Player.playlistSelected.i.bind(outSelected));
        d(maintain(playlist.playingI, ι -> playlist.getPlaying(), outPlaying));
        d(Player.onItemRefresh(ms -> {
            if (outPlaying.getValue()!=null)
                ms.ifHasK(outPlaying.getValue().getUri(), m -> outPlaying.setValue(m.toPlaylist()));
            if (outSelected.getValue()!=null)
                ms.ifHasK(outSelected.getValue().getUri(), m -> outSelected.setValue(m.toPlaylist()));
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
            return DEFAULT_TEXT_FACTORY.apply(all, list) + " - " + new Dur(Σms);
        };
        // add more menu items
        table.menuAdd.getItems().addAll(
            menuItem("Add files", e -> PlaylistManager.chooseFilesToAdd()),
            menuItem("Add directory", e -> PlaylistManager.chooseFolderToAdd()),
            menuItem("Add URL", e -> PlaylistManager.chooseUrlToAdd()),
            menuItem("Play files", e -> PlaylistManager.chooseFilesToPlay()),
            menuItem("Play directory", e -> PlaylistManager.chooseFolderToPlay()),
            menuItem("Play URL", e -> PlaylistManager.chooseUrlToPlay()),
            menuItem("Duplicate selected (+)", e -> playlist.duplicateItemsByOne(table.getSelectedItems())),
            menuItem("Duplicate selected (*)", e -> playlist.duplicateItemsAsGroup(table.getSelectedItems()))
//            editOnAdd_menuItem    // add to lib option
//            editOnAdd_menuItem    // add to lib + edit option
        );
        table.menuRemove.getItems().addAll(
            menuItem("Remove selected", e -> playlist.removeAll(table.getSelectedItems())),
            menuItem("Remove not selected", e -> playlist.retainAll(table.getSelectedItems())),
            menuItem("Remove unsupported", e -> playlist.removeUnplayable()),
            menuItem("Remove duplicates", e -> playlist.removeDuplicates()),
            menuItem("Remove all", e -> playlist.clear())
        );
        table.menuOrder.getItems().addAll(
            menuItem("Order reverse", e -> playlist.reverse()),
            menuItem("Order randomly", e -> playlist.randomize()),
            menuItem("Edit selected", e -> APP.widgetManager.use(SongReader.class,NO_LAYOUT,w->w.read(table.getSelectedItems())))
            // menuItem("Save selected as...", e -> saveSelectedAsPlaylist())
        );
        Menu sortM = new Menu("Order by");
        stream(Field.FIELDS)
                .map(f -> menuItem(f.toStringEnum(), e -> table.sortBy(f)))
                .forEach(sortM.getItems()::add);
        table.menuOrder.getItems().add(0, sortM);

        // prevent scroll event to propagate up
        root.setOnScroll(Event::consume);

        // maintain outputs
        table.getSelectionModel().selectedItemProperty().addListener((o,ov,nv) -> {
            if (!table.movingItems)
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

    @Override
    public void refresh() {
        columnInitializer.execute(() -> {
            String c = getWidget().properties.getS("columns");
            table.setColumnState(c==null ? table.getDefaultColumnInfo() : TableColumnInfo.fromString(c));
        });
        filter_for_playback.applyValue();
    }

    @Override
    public Playlist getPlaylist() {
        return playlist;
    }

    // TODO: implement properly
//    void saveSelectedAsPlaylist() {
//        List<PlaylistItem> l = table.getSelectedItems();
//        if (l.isEmpty()) return;
//
//        App.APP.actions.doWithUserString("Save selected items as...", "Name",
//        Config<String> mc = new ValueConfig<>(String.class, "Name", "My Playlist").constraints(new StringNonEmpty());
//        SimpleConfigurator<?> sc = new SimpleConfigurator<>(mc, (String name) -> {
//            Playlist p = new Playlist(UUID.randomUUID());
//                     p.setAll(l);
//            APP.serializators.toXML(p, new File(APP.DIR_PLAYLISTS, name + ".xml"))
//                .ifError(e -> log(PlaylistView.class).error("Could not save playlist", e));
//        });
//        PopOver p = new PopOver<>(sc);
//                p.title.set("Save selected items as...");
//                p.show(ScreenPos.APP_CENTER);
//    }

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
                 .filter(w -> w.getController()!=null) // during load some widgets may not be loaded yet, not good
                 .map(w -> ((PlaylistFeature)w.getController()).getPlaylist())
                 .filter(ISNTØ)
                 .forEach(p -> pall.removeIf(pl -> pl.id.equals(p.id)));

        Playlist leaf = pall.isEmpty() ? null : pall.get(0);
        for (Playlist p : pall)
            if (p.id.equals(PlaylistManager.active))
                leaf = p;
        if (leaf!=null) PlaylistManager.playlists.remove(leaf);
        if (leaf!=null) {
            if (leaf.id.equals(PlaylistManager.active))
                PlaylistManager.active = id;
            Util.setField(Playlist.class, leaf, "id", id);
        }
        return leaf==null ? new Playlist(id) : leaf;
    }
}