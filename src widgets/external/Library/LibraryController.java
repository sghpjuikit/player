
package Library;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import javafx.beans.property.Property;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.TableColumn;
import javafx.scene.input.Dragboard;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import javafx.util.Callback;

import AudioPlayer.Item;
import AudioPlayer.Player;
import AudioPlayer.SimpleItem;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.services.Database.DB;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataReader;
import Configuration.Config;
import Configuration.IsConfig;
import Layout.Widgets.Widget.Info;
import Layout.Widgets.WidgetManager;
import Layout.Widgets.controller.FXMLController;
import Layout.Widgets.controller.io.IsInput;
import Layout.Widgets.controller.io.Output;
import Layout.Widgets.feature.FileExplorerFeature;
import Layout.Widgets.feature.SongReader;
import Layout.Widgets.feature.SongWriter;
import gui.GUI;
import gui.InfoNode.InfoTask;
import gui.objects.ContextMenu.ImprovedContextMenu;
import gui.objects.ContextMenu.SelectionMenuItem;
import gui.objects.ContextMenu.TableContextMenuⱤ;
import gui.objects.Table.FilteredTable;
import gui.objects.Table.ImprovedTable;
import gui.objects.Table.ImprovedTable.PojoV;
import gui.objects.Table.TableColumnInfo;
import gui.objects.TableRow.ImprovedTableRow;
import gui.objects.spinner.Spinner;
import main.App;
import util.File.AudioFileFormat;
import util.File.AudioFileFormat.Use;
import util.File.Environment;
import util.access.FieldValue.ObjectField.ColumnField;
import util.access.Ѵo;
import util.animation.Anim;
import util.animation.interpolator.ElasticInterpolator;
import util.async.executor.ExecuteN;
import util.async.executor.FxTimer;
import util.async.future.Fut;
import util.graphics.Util;
import util.graphics.drag.DragUtil;
import util.parsing.Parser;
import util.units.FormattedDuration;
import web.HttpSearchQueryBuilder;

import static AudioPlayer.tagging.Metadata.Field.*;
import static Layout.Widgets.Widget.Group.LIBRARY;
import static Layout.Widgets.WidgetManager.WidgetSource.NO_LAYOUT;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.PLAYLIST_PLUS;
import static gui.InfoNode.InfoTable.DEFAULT_TEXT_FACTORY;
import static java.util.stream.Collectors.toList;
import static javafx.scene.control.ProgressIndicator.INDETERMINATE_PROGRESS;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import static javafx.scene.control.TableView.UNCONSTRAINED_RESIZE_POLICY;
import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.TransferMode.COPY;
import static main.App.APP;
import static util.File.FileUtil.getCommonRoot;
import static util.File.FileUtil.getFilesAudio;
import static util.Util.menuItem;
import static util.Util.menuItems;
import static util.animation.Anim.Interpolators.reverse;
import static util.async.Async.FX;
import static util.async.future.Fut.fut;
import static util.functional.Util.filterMap;
import static util.functional.Util.map;
import static util.graphics.Util.setAnchors;
import static util.graphics.Util.setScaleXY;
import static util.graphics.drag.DragUtil.installDrag;
import static util.reactive.Util.maintain;


@Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Library",
    description = "Provides access to database.",
    howto = "Available actions:\n" +
            "    Item left click : Selects item\n" +
            "    Item right click : Opens context menu\n" +
            "    Item double click : Plays item\n" +
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
    notes = "",
    version = "1",
    year = "2015",
    group = LIBRARY
)
public class LibraryController extends FXMLController implements SongReader {

    private @FXML AnchorPane root;
    private final InfoTask taskInfo = new InfoTask(null, new Label(), new Spinner()){
        Anim a;
        {
            a = new Anim(at->Util.setScaleXY(progressIndicator,at*at)).dur(500).intpl(new ElasticInterpolator());
        }
        @Override
        public void setVisible(boolean v) {
            if(v) {
                super.setVisible(v);
                a.then(null)
                 .play();
            } else {
//                Async.run(3000, () -> a.then(() -> super.setVisible(v))
//                 .playClose());
                super.setVisible(v);
            }
        }
    };
    private final FxTimer hideInfo = new FxTimer(5000, 1,
        new Anim(at-> setScaleXY(taskInfo.progressIndicator,at*at)).dur(500)
                .intpl(reverse(new ElasticInterpolator())).then(taskInfo::hideNunbind)::play);
    private final FilteredTable<Metadata,Metadata.Field> table = new FilteredTable<>(Metadata.EMPTY.getMainField());
    private final SelectionMenuItem editOnAdd_menuItem = new SelectionMenuItem("Edit added items",false);

    // input/output
    private Output<Metadata> out_sel;


    // configurables
    @IsConfig(name = "Table orientation", info = "Orientation of the table.")
    public final Ѵo<NodeOrientation> orient = new Ѵo<>(GUI.table_orient);
    @IsConfig(name = "Zeropad numbers", info = "Adds 0s for number length consistency.")
    public final Ѵo<Boolean> zeropad = new Ѵo<>(GUI.table_zeropad);
    @IsConfig(name = "Search show original index", info = "Show unfiltered table item index when filter applied.")
    public final Ѵo<Boolean> orig_index = new Ѵo<>(GUI.table_orig_index);
    @IsConfig(name = "Show table header", info = "Show table header with columns.")
    public final Ѵo<Boolean> show_header = new Ѵo<>(GUI.table_show_header);
    @IsConfig(name = "Show table footer", info = "Show table controls at the bottom of the table. Displays menubar and table items information.")
    public final Ѵo<Boolean> show_footer = new Ѵo<>(GUI.table_show_footer);
    @IsConfig(editable = false)
    private File last_file = new File("");
    @IsConfig(name = "Auto-edit added items")
    private final Property<Boolean> editOnAdd = editOnAdd_menuItem.selected;

    private final ExecuteN runOnce = new ExecuteN(1);


    @Override
    public void init() {
        out_sel = outputs.create(widget.id,"Selected", Metadata.class, null);
        d(Player.librarySelected.i.bind(out_sel));

        // add table to scene graph
        root.getChildren().add(table.getRoot());
        setAnchors(table.getRoot(),0d);

        // table properties
        table.setFixedCellSize(GUI.font.getValue().getSize() + 5);
        table.getSelectionModel().setSelectionMode(MULTIPLE);
        table.searchSetColumn(TITLE);
        d(maintain(orient,table.nodeOrientationProperty()));
        d(maintain(zeropad,table.zeropadIndex));
        d(maintain(orig_index,table.showOriginalIndex));
        d(maintain(show_header,table.headerVisible));
        d(maintain(show_footer,table.footerVisible));

        // add progress indicator to bottom controls
        table.footerPane.setRight(new HBox(7,taskInfo.message, taskInfo.progressIndicator));
        taskInfo.setVisible(false);
        // extend table items information
        table.items_info.textFactory = (all, list) -> {
            double Σms = list.stream().mapToDouble(Metadata::getLengthInMs).sum();
            return DEFAULT_TEXT_FACTORY.apply(all, list) + " - " + new FormattedDuration(Σms);
        };
        // add more menu items
        table.menuAdd.getItems().addAll(
            menuItem("Add files",this::addFiles),
            menuItem("Add directory",this::addDirectory),
            editOnAdd_menuItem
        );
        table.menuRemove.getItems().addAll(
            menuItem("Remove invalid items",this::removeInvalid),
            menuItem("Remove all items",DB::removeAllItems)
        );

        // set up table columns
        table.setColumnFactory(f -> {
            TableColumn<Metadata,?> c = new TableColumn(f.toString());
            c.setCellValueFactory(cf -> cf.getValue()==null ? null : new PojoV(cf.getValue().getField(f)));
            c.setCellFactory(f==RATING
                ? (Callback)APP.ratingCell.getValue()
                : (Callback) col -> table.buildDefaultCell(f)
            );
            return c;
        });

        // let resizing as it is
        table.setColumnResizePolicy(resize -> {
            boolean b = UNCONSTRAINED_RESIZE_POLICY.call(resize);
            // resize index column
            table.getColumn(ColumnField.INDEX)
                 .ifPresent(i->i.setPrefWidth(table.calculateIndexColumnWidth()));
            return b;
        });

        // maintain rating column cell style
        APP.ratingCell.addListener((o,ov,nv) -> table.getColumn(RATING).ifPresent(c->c.setCellFactory((Callback)nv)));

        table.getDefaultColumnInfo();

        // row behavior
        table.setRowFactory(tbl -> new ImprovedTableRow<Metadata>()
                .onLeftDoubleClick((r,e) ->
                    PlaylistManager.use(pl->pl.setNplayFrom(table.getItems(), r.getIndex()))
                )
                .onRightSingleClick((r,e) -> {
                    // prep selection for context menu
                    if(!r.isSelected())
                        tbl.getSelectionModel().clearAndSelect(r.getIndex());
                    // show context menu
                    contxt_menu.show(table, e);
                })
                // additional css styleclasses
                .styleRuleAdd("played", m -> Player.playingtem.get().same(m)) // dont use mthod reference!
        );
        // maintain playing item css by refreshing column
        d(Player.playingtem.onUpdate(o -> table.updateStyleRules()));

        // maintain outputs
        table.getSelectionModel().selectedItemProperty().addListener((o,ov,nv) -> out_sel.setValue(nv));

        // key actions
        table.setOnKeyPressed(e -> {
            if (e.getCode() == ENTER) {     // play first of the selected
                if(!table.getSelectionModel().isEmpty()) {
                    PlaylistManager.use(pl ->pl.setNplayFrom(table.getItems(), table.getSelectionModel().getSelectedIndex()));
                }
            }
            else if (e.getCode() == DELETE)    // delete selected
                DB.removeItems(table.getSelectedItems());
            else if (e.getCode() == ESCAPE)    // deselect
                table.getSelectionModel().clearSelection();
        });

        // drag&drop
        installDrag(
            table, PLAYLIST_PLUS,
            () -> "Add to library" + (editOnAdd.getValue() ? " and edit" : ""),
            e -> DragUtil.hasAudio(e),
            e -> e.getGestureSource()==table,
            e -> addNeditDo(DragUtil.getSongs(e), editOnAdd.getValue())
        );

        // drag&drop from
        table.setOnDragDetected(e -> {
            if (e.getButton() == PRIMARY && !table.getSelectedItems().isEmpty()
                    && table.isRowFull(table.getRowS(e.getSceneX(), e.getSceneY()))) {
                Dragboard db = table.startDragAndDrop(COPY);
                DragUtil.setItemList(table.getSelectedItemsCopy(),db,true);
            }
            e.consume();
        });

        // prevent volume change
        table.setOnScroll(Event::consume);

        // update library comparator
        maintain(table.itemsComparator,DB.library_sorter);
    }

    @Override
    public void refresh() {
        runOnce.execute(() -> {
            String c = getWidget().properties.getS("columns");
            table.setColumnState(c==null ? table.getDefaultColumnInfo() : TableColumnInfo.fromString(c));
        });

        getFields().stream().filter(c->!c.getName().equals("Library level")&&!c.getName().equals("columnInfo")).forEach(Config::applyValue);
        table.getSelectionModel().clearSelection();
    }

    @IsInput("To display")
    public void setItems(List<? extends Metadata> items) {
        if(items==null) return;
        table.setItemsRaw(items);
    }

    @FXML private void addDirectory() {
        addNedit(editOnAdd.getValue(),true);
    }
    @FXML private void addFiles() {
        addNedit(editOnAdd.getValue(),false);
    }

    private void addNedit(boolean edit, boolean dir) {
        Window w = root.getScene().getWindow();
        ExtensionFilter ef = AudioFileFormat.filter(Use.APP);
        if(dir) {
            File f = Environment.chooseFile("Add folder to library", true, last_file, w, ef);
            if(f!=null) {
                addNeditDo(fut(() -> {
                    last_file = f.getParentFile()==null ? f : f.getParentFile();
                    Stream<File> files = getFilesAudio(f,Use.APP,Integer.MAX_VALUE);
                    return files.map(SimpleItem::new);
                }), edit);
            }
        } else {
            List<File> fs = Environment.chooseFiles("Add files to library", last_file, w, ef);
            if(fs!=null) {
                addNeditDo(fut(() -> {
                    File fr = getCommonRoot(fs);
                    if(fr!=null) last_file=fr;
                    Stream<File> files = fs.stream();
                    return files.map(SimpleItem::new);
                }), edit);
            }
        }
    }

    private void addNeditDo(Fut<Stream<Item>> files, boolean edit) {
        fut().then(() -> {
                 taskInfo.setVisible(true);
                 taskInfo.message.setText("Discovering files...");
                 taskInfo.progressIndicator.setProgress(INDETERMINATE_PROGRESS);
             }, FX)
             .supply(files)
             .use(items -> {
                 Task t = MetadataReader.readAaddMetadata(items.collect(toList()),(ok,added) -> {
                     if(ok && edit && !added.isEmpty())
                         WidgetManager.use(SongWriter.class, NO_LAYOUT, w -> w.read(added));
                     hideInfo.start();
                 },false);
                 taskInfo.bind(t);
             },FX)
             .showProgress(App.getWindow().taskAdd())
             .run();
    }

    private void removeInvalid() {
        Task t = MetadataReader.removeMissingFromLibrary((success,result) -> {
            hideInfo.start();
        });
        taskInfo.showNbind(t);
    }


/******************************** PUBLIC API **********************************/

    /**
     * Converts items to Metadata using {@link Item#toMeta()} (using no I/O)
     * and displays them in the table.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void read(List<? extends Item> items) {
        table.setItemsRaw(map(items,Item::toMeta));
    }

/********************************* CONFIGS ************************************/

    @Override
    public Collection<Config<Object>> getFields() {
        // serialize column state when requested
        getWidget().properties.put("columns", table.getColumnState().toString());
        return super.getFields();
    }

/****************************** CONTEXT MENU **********************************/

    private static final TableContextMenuⱤ<Metadata> contxt_menu = new TableContextMenuⱤ<> (
        () -> {
            ImprovedContextMenu<List<Metadata>> m = new ImprovedContextMenu();
            m.getItems().addAll(menuItem("Play items", e ->
                    PlaylistManager.use(p -> p.setNplay(m.getValue()))
                ),
                menuItem("Enqueue items", e ->
                    PlaylistManager.use(p -> p.addItems(m.getValue()))
                ),
                menuItem("Update from file", e ->
                    App.refreshItemsFromFileJob(m.getValue())
                ),
                menuItem("Remove from library", e ->
                    DB.removeItems(m.getValue())
                ),
                new Menu("Show in",null,
                    menuItems(filterMap(WidgetManager.getFactories(),f->f.hasFeature(SongReader.class),f->f.name()),
                            (String f) -> f,
                            (String f) -> WidgetManager.use(w->w.name().equals(f),NO_LAYOUT,c->((SongReader)c.getController()).read(m.getValue())))
                ),
                new Menu("Edit tags in",null,
                    menuItems(filterMap(WidgetManager.getFactories(),f->f.hasFeature(SongWriter.class),f->f.name()),
                            (String f) -> f,
                            (String f) -> WidgetManager.use(w->w.name().equals(f),NO_LAYOUT,c->((SongWriter)c.getController()).read(m.getValue())))
                ),
                menuItem("Explore items's directory", e -> {
                    List<Metadata> items = m.getValue();
                    List<File> files = filterMap(items,Item::isFileBased,Item::getLocation);
                    Environment.browse(files,true);
                }),
                new Menu("Explore items's directory in",null,
                    menuItems(filterMap(WidgetManager.getFactories(),f->f.hasFeature(FileExplorerFeature.class),f->f.name()),
                            (String f) -> f,
                            (String f) -> WidgetManager.use(w->w.name().equals(f),NO_LAYOUT,c->((FileExplorerFeature)c.getController()).exploreFile(m.getValue().get(0).getFile())))
                ),
                new Menu("Search album cover",null,
                    menuItems(APP.plugins.getPlugins(HttpSearchQueryBuilder.class),
                            q -> "in " + Parser.toS(q),
                            q -> Environment.browse(q.apply(m.getValue().get(0).getAlbum())))
                )
               );
            return m;
        },
        (menu,table) -> menu.setValue(ImprovedTable.class.cast(table).getSelectedItemsCopy())
    );

}