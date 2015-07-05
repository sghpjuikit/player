
package Library;

import AudioPlayer.Player;
import AudioPlayer.playlist.*;
import AudioPlayer.services.Database.DB;
import AudioPlayer.tagging.Metadata;
import static AudioPlayer.tagging.Metadata.Field.*;
import AudioPlayer.tagging.MetadataReader;
import Configuration.Config;
import Configuration.IsConfig;
import Layout.Widgets.FXMLWidget;
import static Layout.Widgets.Widget.Group.LIBRARY;
import Layout.Widgets.Widget.Info;
import Layout.Widgets.WidgetManager;
import static Layout.Widgets.WidgetManager.WidgetSource.NO_LAYOUT;
import Layout.Widgets.controller.FXMLController;
import Layout.Widgets.controller.io.Input;
import Layout.Widgets.controller.io.Output;
import Layout.Widgets.feature.FileExplorerFeature;
import Layout.Widgets.feature.SongReader;
import Layout.Widgets.feature.SongWriter;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
import gui.GUI;
import gui.InfoNode.InfoTable;
import static gui.InfoNode.InfoTable.DEFAULT_TEXT_FACTORY;
import gui.InfoNode.InfoTask;
import gui.objects.ActionChooser;
import gui.objects.ContextMenu.ImprovedContextMenu;
import gui.objects.ContextMenu.TableContextMenuInstance;
import gui.objects.Table.FilteredTable;
import gui.objects.Table.ImprovedTable;
import gui.objects.Table.ImprovedTable.PojoV;
import gui.objects.Table.TableColumnInfo;
import gui.objects.Table.TableColumnInfo.ColumnInfo;
import gui.objects.TableRow.ImprovedTableRow;
import gui.objects.spinner.Spinner;
import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import static java.util.stream.Collectors.toList;
import java.util.stream.Stream;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import static javafx.geometry.NodeOrientation.INHERIT;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import static javafx.scene.control.ProgressIndicator.INDETERMINATE_PROGRESS;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import static javafx.scene.control.TableColumn.SortType.ASCENDING;
import static javafx.scene.control.TableView.UNCONSTRAINED_RESIZE_POLICY;
import javafx.scene.input.*;
import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.TransferMode.COPY;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import static javafx.scene.layout.Priority.ALWAYS;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import javafx.util.Callback;
import main.App;
import static org.reactfx.EventStreams.changesOf;
import static org.reactfx.EventStreams.nonNullValuesOf;
import org.reactfx.Subscription;
import util.File.AudioFileFormat;
import util.File.AudioFileFormat.Use;
import util.File.Environment;
import static util.File.FileUtil.getCommonRoot;
import static util.File.FileUtil.getFilesAudio;
import static util.Util.*;
import util.access.Accessor;
import util.animation.Anim;
import static util.animation.Anim.Interpolators.reverse;
import util.animation.interpolator.ElasticInterpolator;
import static util.async.Async.FX;
import util.async.executor.FxTimer;
import util.async.executor.LimitedExecutor;
import static util.async.future.Fut.fut;
import static util.functional.Util.filterMap;
import static util.functional.Util.map;
import util.graphics.Icons;
import util.graphics.drag.DragUtil;
import util.parsing.Parser;
import util.units.FormattedDuration;
import web.HttpSearchQueryBuilder;

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
    private @FXML VBox content;
    private final InfoTask taskInfo = new InfoTask(null, new Label(), new Spinner()){
        Anim a;
        {
            a = new Anim(at->setScaleXY(progressIndicator,at*at)).dur(500).intpl(new ElasticInterpolator());
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
    private final FxTimer hideInfo = new FxTimer(5000, 1, () -> {
        new Anim(at->setScaleXY(taskInfo.progressIndicator,at*at)).dur(500).intpl(reverse(new ElasticInterpolator()))
            .then(taskInfo::hideNunbind)
            .play();
        
    });
    private final FilteredTable<Metadata,Metadata.Field> table = new FilteredTable(Metadata.EMPTY.getMainField());
    ActionChooser actPane;
    
    @FXML Menu addMenu;
    @FXML Menu remMenu;
    @FXML MenuBar controlsBar;
    private final LimitedExecutor runOnce = new LimitedExecutor(1);
    
    // input/output
    private final Output<Metadata> out_sel;
    private final Input<List<Metadata>> in_items = inputs.create("To display", (Class)List.class, table::setItemsRaw);
    
    // dependencies to disopose of
    private Subscription d2, d3, d4;
    
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
    @IsConfig(editable = false)
    private TableColumnInfo columnInfo;
    
    @IsConfig(editable = false)
    private File last_file = new File("");
    @IsConfig(name = "Auto-edit added items")
    private final BooleanProperty editOnAdd = new SimpleBooleanProperty(false);
    
        
    public LibraryController(FXMLWidget widget) {
        super(widget);
        
        out_sel = outputs.create(widget.id,"Selected", Metadata.class, null).setStringConverter(Metadata::getTitle);
        Player.librarySelected.i.bind(out_sel);
        
        actPane = new ActionChooser(this);
    }
    
    @Override
    public void init() {
        table.setFixedCellSize(GUI.font.getValue().getSize() + 5);
        table.getSelectionModel().setSelectionMode(MULTIPLE);
        table.searchSetColumn(TITLE);
        
        // set up table columns
        table.setColumnStateFacory( f -> {
            double w = f==PATH || f==TITLE ? 150 : 50;
            return new ColumnInfo(f.toString(), f.ordinal(), f.isCommon(), w);
        });
        table.setColumnFactory( f -> {
            TableColumn<Metadata,?> c = new TableColumn(f.toString());
            c.setCellValueFactory( cf -> cf.getValue()==null ? null : new PojoV(cf.getValue().getField(f)));
            c.setCellFactory(f==RATING
                ? (Callback)App.ratingCell.getValue()
                : (Callback) col -> table.buildDefaultCell(f)
            );
            return c;
        });
        
        // let resizing as it is
        table.setColumnResizePolicy(resize -> {
            boolean b = UNCONSTRAINED_RESIZE_POLICY.call(resize);
            // resize index column
            table.getColumn("#").ifPresent(i->i.setPrefWidth(table.calculateIndexColumnWidth()));
            return b;
        });
        
        // maintain rating column cell style
        App.ratingCell.addListener((o,ov,nv) -> table.getColumn(RATING).ifPresent(c->c.setCellFactory((Callback)nv)));
        columnInfo = table.getDefaultColumnInfo();
        
        // row behavior
        table.setRowFactory(tbl -> new ImprovedTableRow<Metadata>()
                .onLeftDoubleClick((r,e) -> {
                    Playlist p = new Playlist(map(table.getItems(),Metadata::toPlaylist));
                    PlaylistManager.playPlaylistFrom(p, r.getIndex());
                })
                .onRightSingleClick((r,e) -> {
                    // prep selection for context menu
                    if(!r.isSelected())
                        tbl.getSelectionModel().clearAndSelect(r.getIndex());
                    // show context menu
                    contxt_menu.show(table, e);
                })                
                // additional css styleclasses
                .styleRuleAdd("played", m -> Player.playingtem.get().same(m))
        );
        // maintain playing item css by refreshing column
        d4 = Player.playingtem.subscribeToChanges(o -> table.updateStyleRules());
        
        // maintain outputs
        table.getSelectionModel().selectedItemProperty().addListener((o,ov,nv) -> out_sel.setValue(nv));
        
        // key actions
        table.setOnKeyReleased(e -> {
            if (e.getCode() == ENTER) {     // play first of the selected
                if(!table.getSelectionModel().isEmpty()) {
                    Playlist p = new Playlist(map(table.getItems(),Metadata::toPlaylist));
                    PlaylistManager.playPlaylistFrom(p, table.getSelectionModel().getSelectedIndex());
                }
            }
            else if (e.getCode() == DELETE)    // delete selected
                DB.removeItems(table.getSelectedItems());
            else if (e.getCode() == ESCAPE)    // deselect
                table.getSelectionModel().clearSelection();
        });
        

        // drag&drop to accept
        table.setOnDragOver_NoSelf(e -> {
            // huh? something missing... like THE CODE!! dont just accept anything!
            e.acceptTransferModes(COPY);
            e.consume();
        });
        // drag&drop to
        table.setOnDragDropped(e-> {
            addNeditDo(DragUtil.getSongs(e), editOnAdd.get());
            e.setDropCompleted(true);
            e.consume();
        });
        // drag&drop from
        table.setOnDragDetected(e -> {
            if (e.getButton() == PRIMARY && !table.getSelectedItems().isEmpty() 
                    && table.isRowFull(table.getRowS(e.getSceneX(), e.getSceneY()))) {
                Dragboard db = table.startDragAndDrop(COPY);
                DragUtil.setItemList(table.getSelectedItemsCopy(),db);
            }
            e.consume();
        });
        
        // prevent volume change
        table.setOnScroll(Event::consume);
        
        // update selected items for application
        d2 = Player.librarySelectedItemES.feedFrom(nonNullValuesOf(table.getSelectionModel().selectedItemProperty()));
        d3 = Player.librarySelectedItemsES.feedFrom(changesOf(table.getSelectionModel().getSelectedItems()).map(i->table.getSelectedItemsCopy()));
        
        // update library comparator
        changesOf(table.getSortOrder()).subscribe( c -> 
            DB.library_sorter = c.getList().stream().map(column -> {
                    Metadata.Field f = (Metadata.Field) column.getUserData();
                    int type = column.getSortType()==ASCENDING ? 1 : -1;
                    return (Comparator<Metadata>)(m1,m2) -> type*((Comparable)m1.getField(f)).compareTo((m2.getField(f)));
                })
                .reduce((m1,m2) -> 0, Comparator::thenComparing)
        );
        
        // task info init
        taskInfo.setVisible(false);
        
        // table information label
        InfoTable<Metadata> infoL = new InfoTable(new Label(), table);
        infoL.textFactory = (all, list) -> {
            double d = list.stream().mapToDouble(Metadata::getLengthInMs).sum();
            return DEFAULT_TEXT_FACTORY.apply(all, list) + " - " + new FormattedDuration(d);
        };
        
        // controls bottom header
        Region padding = new Region();
        HBox controls = new HBox(controlsBar,infoL.node,padding,taskInfo.message, taskInfo.progressIndicator);
             controls.setSpacing(7);
             controls.setAlignment(Pos.CENTER_LEFT);
             controls.setPadding(new Insets(0,5,0,0));
        HBox.setHgrow(padding, ALWAYS);
        
        addMenu.setText("");
        remMenu.setText("");
        Icons.setIcon(addMenu, FontAwesomeIconName.PLUS, "11", "11");
        Icons.setIcon(remMenu, FontAwesomeIconName.MINUS, "11", "11");
        
        // add 'edit on add to library' option menu
        gui.objects.ContextMenu.CheckMenuItem editOnAddmi = new gui.objects.ContextMenu.CheckMenuItem("Edit added items");
        editOnAddmi.selected.bindBidirectional(editOnAdd);
        controlsBar.getMenus().get(0).getItems().add(editOnAddmi);
        
        content.getChildren().addAll(table.getRoot(), controls);
        VBox.setVgrow(table.getRoot(),ALWAYS);
    }

    @Override
    public void refresh() {
        runOnce.execute(()->table.setColumnState(columnInfo));
        
        getFields().stream().filter(c->!c.getName().equals("Library level")&&!c.getName().equals("columnInfo")).forEach(Config::applyValue);
        table.getSelectionModel().clearSelection();
    }

    @Override
    public void onClose() {
        Player.librarySelected.i.unbind(out_sel);
        // stop listening
        d2.unsubscribe();
        d3.unsubscribe();
        d4.unsubscribe();
    }
    
    @Override
    public Node getActivityNode() {
        return actPane;
    }
    
    @FXML private void addDirectory() {
        addNedit(editOnAdd.get(),true);
    }
    @FXML private void addFiles() {
        addNedit(editOnAdd.get(),false);
    }
    
    private void addNedit(boolean edit, boolean dir) {
        Window w = root.getScene().getWindow();
        ExtensionFilter ef = AudioFileFormat.filter(Use.APP);
        if(dir) {
            File f = Environment.chooseFile("Add folder to library", true, last_file, w, ef);
            if(f!=null) {
                addNeditDo(() -> {
                    last_file = f.getParentFile()==null ? f : f.getParentFile();
                    Stream<File> files = getFilesAudio(f,Use.APP,Integer.MAX_VALUE);
                    return files.map(SimpleItem::new);
                }, edit);
            }
        } else {
            List<File> fs = Environment.chooseFiles("Add files to library", last_file, w, ef);
            if(fs!=null) {
                addNeditDo(() -> {
                    File fr = getCommonRoot(fs);
                    if(fr!=null) last_file=fr;
                    Stream<File> files = fs.stream();
                    return files.map(SimpleItem::new);
                }, edit);
            }
        }
    }
    
    private void addNeditDo(Supplier<Stream<Item>> files, boolean edit) {
        fut().thenR(() -> {
                 taskInfo.setVisible(true);
                 taskInfo.message.setText("Discovering files...");
                 taskInfo.progressIndicator.setProgress(INDETERMINATE_PROGRESS);
             }, FX)
             .supply(() -> files.get().collect(toList()))
             .use(items -> {
                 Task t = MetadataReader.readAaddMetadata(items,(ok,added) -> {
                     if(ok && edit && !added.isEmpty())
                         WidgetManager.use(SongWriter.class, NO_LAYOUT, w -> w.read(added));
                     hideInfo.restart();
                 },false);
                 taskInfo.bind(t);
             },FX)
             .showProgress(App.getWindow().taskAdd())
             .run();
    }
    
    @FXML private void removeInvalid() {
        Task t = MetadataReader.removeMissingFromLibrary((success,result) -> {
            hideInfo.restart();
        });
       taskInfo.showNbind(t);
    }
    @FXML private void removeAll() {
        DB.removeAllItems();
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
        columnInfo = table.getColumnState();
        return super.getFields();
    }

    @Override
    public Config getField(String name) {
        // serialize column state when requested
        if("columnInfo".equals(name)) columnInfo = table.getColumnState();
        return super.getField(name);
    }
    
/****************************** CONTEXT MENU **********************************/
    
    private static final TableContextMenuInstance<Metadata> contxt_menu = new TableContextMenuInstance<>(
        () -> {
            
            ImprovedContextMenu<List<Metadata>> m = new ImprovedContextMenu();
            m.getItems().addAll(menuItem("Play items", e -> {                     
                    List<PlaylistItem> to_play = map(m.getValue(), Metadata::toPlaylist);
                    PlaylistManager.playPlaylist(new Playlist(to_play));
                }),
                menuItem("Enqueue items", e -> {
                    List<Metadata> items = m.getValue();
                    PlaylistManager.addItems(items);
                }),
                menuItem("Update from file", e -> {
                    List<Metadata> items = m.getValue();
                    App.refreshItemsFromFileJob(items);
                }),
                menuItem("Remove from library", e -> {
                    List<Metadata> items = m.getValue();
                    DB.removeItems(items);
                }),
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
                    menuItems(App.plugins.getPlugins(HttpSearchQueryBuilder.class), 
                            q -> "in " + Parser.toS(q),
                            q -> Environment.browse(q.apply(m.getValue().get(0).getAlbum())))
                )
               );
            return m;
        },
        (menu,table) -> menu.setValue(ImprovedTable.class.cast(table).getSelectedItemsCopy())
    );
    
}
//
//package Library;
//
//import AudioPlayer.Player;
//import AudioPlayer.playlist.*;
//import AudioPlayer.services.Database.DB;
//import AudioPlayer.tagging.Metadata;
//import static AudioPlayer.tagging.Metadata.Field.*;
//import AudioPlayer.tagging.MetadataReader;
//import Configuration.Config;
//import Configuration.IsConfig;
//import Layout.Widgets.FXMLWidget;
//import static Layout.Widgets.Widget.Group.LIBRARY;
//import Layout.Widgets.Widget.Info;
//import Layout.Widgets.WidgetManager;
//import static Layout.Widgets.WidgetManager.WidgetSource.NO_LAYOUT;
//import Layout.Widgets.controller.FXMLController;
//import Layout.Widgets.controller.io.Input;
//import Layout.Widgets.controller.io.Output;
//import Layout.Widgets.feature.FileExplorerFeature;
//import Layout.Widgets.feature.SongReader;
//import Layout.Widgets.feature.SongWriter;
//import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
//import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.SQUARE_ALT;
//import gui.GUI;
//import gui.InfoNode.InfoTable;
//import static gui.InfoNode.InfoTable.DEFAULT_TEXT_FACTORY;
//import gui.InfoNode.InfoTask;
//import gui.objects.ActionChooser;
//import gui.objects.ContextMenu.ImprovedContextMenu;
//import gui.objects.ContextMenu.TableContextMenuInstance;
//import gui.objects.Table.FilteredTable;
//import gui.objects.Table.ImprovedTable;
//import gui.objects.Table.ImprovedTable.PojoV;
//import gui.objects.Table.TableColumnInfo;
//import gui.objects.Table.TableColumnInfo.ColumnInfo;
//import gui.objects.TableRow.ImprovedTableRow;
//import gui.objects.icon.Icon;
//import gui.objects.spinner.Spinner;
//import java.io.File;
//import java.util.Collection;
//import java.util.Comparator;
//import java.util.List;
//import java.util.function.Supplier;
//import static java.util.stream.Collectors.toList;
//import java.util.stream.Stream;
//import javafx.beans.property.BooleanProperty;
//import javafx.beans.property.SimpleBooleanProperty;
//import javafx.concurrent.Task;
//import javafx.event.Event;
//import javafx.fxml.FXML;
//import javafx.geometry.Insets;
//import javafx.geometry.NodeOrientation;
//import static javafx.geometry.NodeOrientation.INHERIT;
//import javafx.geometry.Pos;
//import javafx.scene.Node;
//import javafx.scene.control.*;
//import static javafx.scene.control.ProgressIndicator.INDETERMINATE_PROGRESS;
//import static javafx.scene.control.SelectionMode.MULTIPLE;
//import static javafx.scene.control.TableColumn.SortType.ASCENDING;
//import static javafx.scene.control.TableView.UNCONSTRAINED_RESIZE_POLICY;
//import javafx.scene.input.*;
//import static javafx.scene.input.KeyCode.*;
//import static javafx.scene.input.MouseButton.PRIMARY;
//import static javafx.scene.input.MouseButton.SECONDARY;
//import static javafx.scene.input.TransferMode.COPY;
//import javafx.scene.layout.AnchorPane;
//import javafx.scene.layout.HBox;
//import static javafx.scene.layout.Priority.ALWAYS;
//import javafx.scene.layout.Region;
//import javafx.scene.layout.VBox;
//import javafx.stage.FileChooser.ExtensionFilter;
//import javafx.stage.Window;
//import javafx.util.Callback;
//import main.App;
//import static org.reactfx.EventStreams.changesOf;
//import static org.reactfx.EventStreams.nonNullValuesOf;
//import org.reactfx.Subscription;
//import util.File.AudioFileFormat;
//import util.File.AudioFileFormat.Use;
//import util.File.Environment;
//import static util.File.FileUtil.getCommonRoot;
//import static util.File.FileUtil.getFilesAudio;
//import static util.Util.*;
//import util.access.Accessor;
//import util.animation.Anim;
//import static util.animation.Anim.Interpolators.reverse;
//import util.animation.interpolator.ElasticInterpolator;
//import static util.async.Async.FX;
//import util.async.executor.FxTimer;
//import util.async.executor.LimitedExecutor;
//import static util.async.future.Fut.fut;
//import static util.functional.Util.filterMap;
//import static util.functional.Util.map;
//import util.graphics.Icons;
//import util.graphics.drag.DragUtil;
//import util.parsing.Parser;
//import util.units.FormattedDuration;
//import web.HttpSearchQueryBuilder;
//
//@Info(
//    author = "Martin Polakovic",
//    programmer = "Martin Polakovic",
//    name = "Library",
//    description = "Provides access to database.",
//    howto = "Available actions:\n" +
//            "    Item left click : Selects item\n" +
//            "    Item right click : Opens context menu\n" +
//            "    Item double click : Plays item\n" +
//            "    Type : search & filter\n" +
//            "    Press ENTER : Plays item\n" +
//            "    Press ESC : Clear selection & filter\n" +
//            "    Scroll : Scroll table vertically\n" +
//            "    Scroll + SHIFT : Scroll table horizontally\n" +
//            "    Column drag : swap columns\n" +
//            "    Column right click: show column menu\n" +
//            "    Click column : Sort - ascending | descending | none\n" +
//            "    Click column + SHIFT : Sorts by multiple columns\n" +
//            "    Menu bar : Opens additional actions\n",
//    notes = "",
//    version = "1",
//    year = "2015",
//    group = LIBRARY
//)
//public class LibraryController extends FXMLController implements SongReader {
//    
//    private @FXML AnchorPane root;
//    private @FXML VBox content;
//    private final InfoTask taskInfo = new InfoTask(null, new Label(), new Spinner()){
//        Anim a;
//        {
//            a = new Anim(at->setScaleXY(progressIndicator,at*at)).dur(500).intpl(new ElasticInterpolator());
//        }
//        @Override
//        public void setVisible(boolean v) {
//            if(v) {
//                super.setVisible(v);
//                a.then(null)
//                 .play();
//            } else {
////                Async.run(3000, () -> a.then(() -> super.setVisible(v))
////                 .playClose());
//                super.setVisible(v);
//            }
//        }
//    };
//    private final FxTimer hideInfo = new FxTimer(5000, 1, () -> {
//        new Anim(at->setScaleXY(taskInfo.progressIndicator,at*at)).dur(500).intpl(reverse(new ElasticInterpolator()))
//            .then(taskInfo::hideNunbind)
//            .play();
//        
//    });
////    private final FxTimer hideInfo = new FxTimer(5000, 1, taskInfo::hideNunbind);
//    private final FilteredTable<Metadata,Metadata.Field> table = new FilteredTable(Metadata.EMPTY.getMainField());
//    ActionChooser actPane;
//    Icon lvlB;
//    
//    @FXML Menu addMenu;
//    @FXML Menu remMenu;
//    @FXML MenuBar controlsBar;
//    private final LimitedExecutor runOnce = new LimitedExecutor(1);
//    
//    // input/output
//    private final Output<Metadata> out_sel;
//    private final Input<List<Metadata>> in_items;
//    
//    // dependencies to disopose of
//    private Subscription d1, d2, d3, d4;
//    
//    // configurables
//    @IsConfig(name = "Table orientation", info = "Orientation of the table.")
//    public final Accessor<NodeOrientation> table_orient = new Accessor<>(INHERIT, table::setNodeOrientation);
//    @IsConfig(name = "Zeropad numbers", info = "Adds 0 to uphold number length consistency.")
//    public final Accessor<Boolean> zeropad = new Accessor<>(true, table::setZeropadIndex);
//    @IsConfig(name = "Search show original index", info = "Show index of the table items as in unfiltered state when filter applied.")
//    public final Accessor<Boolean> orig_index = new Accessor<>(true, table::setShowOriginalIndex);
//    @IsConfig(name = "Show table header", info = "Show table header with columns.")
//    public final Accessor<Boolean> show_header = new Accessor<>(true, table::setHeaderVisible);
//    @IsConfig(name = "Show table menu button", info = "Show table menu button for setting up columns.")
//    public final Accessor<Boolean> show_menu_button = new Accessor<>(false, table::setTableMenuButtonVisible);
//    @IsConfig(editable = false)
//    private TableColumnInfo columnInfo;
//    @IsConfig(name = "Library level", info = "", min = 1, max = 8)
//    public final Accessor<Integer> lvl = new Accessor<Integer>(DB.views.getLastLvl()+1, v -> {
//        // maintain info text
//        lvlB.setText(v.toString());
//        if(d1!=null) d1.unsubscribe();
//        // listen for database changes to refresh library
//        d1 = DB.views.subscribe(v, (i,list) -> table.setItemsRaw(list));
//        // initialize
//        table.setItemsRaw(DB.views.getValue(v));
//    });
//    
//    @IsConfig(editable = false)
//    private File last_file = new File("");
//    @IsConfig(name = "Auto-edit added items")
//    private final BooleanProperty editOnAdd = new SimpleBooleanProperty(false);
//    
//        
//    public LibraryController(FXMLWidget widget) {
//        super(widget);
//        
//        out_sel = outputs.create(widget.id,"Selected", Metadata.class, null).setStringConverter(Metadata::getTitle);
//        Player.librarySelected.i.bind(out_sel);
//        in_items = inputs.create("To display", (Class)List.class, table::setItemsRaw);
//        
//        actPane = new ActionChooser(this);
//        lvlB = actPane.addIcon(SQUARE_ALT, "1", "Level");
//        lvlB.setOnMouseClicked(e -> {
//            if(e.getButton()==PRIMARY)
//                lvl.setNapplyValue(clip(1,lvl.getValue()+1,8));
//            if(e.getButton()==SECONDARY)
//                lvl.setNapplyValue(clip(1,lvl.getValue()-1,8));
//            e.consume();
//        });
//    }
//    
//    @Override
//    public void init() {
//        table.setFixedCellSize(GUI.font.getValue().getSize() + 5);
//        table.getSelectionModel().setSelectionMode(MULTIPLE);
//        table.searchSetColumn(TITLE);
//        
//        // set up table columns
//        table.setColumnStateFacory( f -> {
//            double w = f==PATH || f==TITLE ? 150 : 50;
//            return new ColumnInfo(f.toString(), f.ordinal(), f.isCommon(), w);
//        });
//        table.setColumnFactory( f -> {
//            TableColumn<Metadata,?> c = new TableColumn(f.toString());
//            c.setCellValueFactory( cf -> cf.getValue()==null ? null : new PojoV(cf.getValue().getField(f)));
//            c.setCellFactory(f==RATING
//                ? (Callback)App.ratingCell.getValue()
//                : (Callback) col -> table.buildDefaultCell(f)
//            );
//            return c;
//        });
//        
//        // let resizing as it is
//        table.setColumnResizePolicy(resize -> {
//            boolean b = UNCONSTRAINED_RESIZE_POLICY.call(resize);
//            // resize index column
//            table.getColumn("#").ifPresent(i->i.setPrefWidth(table.calculateIndexColumnWidth()));
//            return b;
//        });
//        
//        // maintain rating column cell style
//        App.ratingCell.addListener((o,ov,nv) -> table.getColumn(RATING).ifPresent(c->c.setCellFactory((Callback)nv)));
//        columnInfo = table.getDefaultColumnInfo();
//        
//        // row behavior
//        table.setRowFactory(tbl -> new ImprovedTableRow<Metadata>()
//                .onLeftDoubleClick((r,e) -> {
//                    Playlist p = new Playlist(map(table.getItems(),Metadata::toPlaylist));
//                    PlaylistManager.playPlaylistFrom(p, r.getIndex());
//                })
//                .onRightSingleClick((r,e) -> {
//                    // prep selection for context menu
//                    if(!r.isSelected())
//                        tbl.getSelectionModel().clearAndSelect(r.getIndex());
//                    // show context menu
//                    contxt_menu.show(table, e);
//                })                
//                // additional css styleclasses
//                .styleRuleAdd("played", m -> Player.playingtem.get().same(m))
//        );
//        // maintain playing item css by refreshing column
//        d4 = Player.playingtem.subscribeToChanges(o -> table.updateStyleRules());
//        
//        // maintain outputs
//        table.getSelectionModel().selectedItemProperty().addListener((o,ov,nv) -> out_sel.setValue(nv));
//        
//        // key actions
//        table.setOnKeyReleased(e -> {
//            if (e.getCode() == ENTER) {     // play first of the selected
//                if(!table.getSelectionModel().isEmpty()) {
//                    Playlist p = new Playlist(map(table.getItems(),Metadata::toPlaylist));
//                    PlaylistManager.playPlaylistFrom(p, table.getSelectionModel().getSelectedIndex());
//                }
//            }
//            else if (e.getCode() == DELETE)    // delete selected
//                DB.removeItems(table.getSelectedItems());
//            else if (e.getCode() == ESCAPE)    // deselect
//                table.getSelectionModel().clearSelection();
//        });
//        
//
//        // drag&drop to accept
//        table.setOnDragOver_NoSelf(e -> {
//            // huh? something missing... like THE CODE!! dont just accept anything!
//            e.acceptTransferModes(COPY);
//            e.consume();
//        });
//        // drag&drop to
//        table.setOnDragDropped(e-> {
//            addNeditDo(DragUtil.getSongs(e), editOnAdd.get());
//            e.setDropCompleted(true);
//            e.consume();
//        });
//        // drag&drop from
//        table.setOnDragDetected(e -> {
//            if (e.getButton() == PRIMARY && !table.getSelectedItems().isEmpty() 
//                    && table.isRowFull(table.getRowS(e.getSceneX(), e.getSceneY()))) {
//                Dragboard db = table.startDragAndDrop(COPY);
//                DragUtil.setItemList(table.getSelectedItemsCopy(),db);
//            }
//            e.consume();
//        });
//        
//        // prevent volume change
//        table.setOnScroll(Event::consume);
//        
//        // update selected items for application
//        d2 = Player.librarySelectedItemES.feedFrom(nonNullValuesOf(table.getSelectionModel().selectedItemProperty()));
//        d3 = Player.librarySelectedItemsES.feedFrom(changesOf(table.getSelectionModel().getSelectedItems()).map(i->table.getSelectedItemsCopy()));
//        
//        // update library comparator
//        changesOf(table.getSortOrder()).subscribe( c -> 
//            DB.library_sorter = c.getList().stream().map(column -> {
//                    Metadata.Field f = (Metadata.Field) column.getUserData();
//                    int type = column.getSortType()==ASCENDING ? 1 : -1;
//                    return (Comparator<Metadata>)(m1,m2) -> type*((Comparable)m1.getField(f)).compareTo((m2.getField(f)));
//                })
//                .reduce((m1,m2) -> 0, Comparator::thenComparing)
//        );
//        
//        // task info init
//        taskInfo.setVisible(false);
//        
//        // table information label
//        InfoTable<Metadata> infoL = new InfoTable(new Label(), table);
//        infoL.textFactory = (all, list) -> {
//            double d = list.stream().mapToDouble(Metadata::getLengthInMs).sum();
//            return DEFAULT_TEXT_FACTORY.apply(all, list) + " - " + new FormattedDuration(d);
//        };
//        
//        // controls bottom header
//        Region padding = new Region();
//        HBox controls = new HBox(controlsBar,infoL.node,padding,taskInfo.message, taskInfo.progressIndicator);
//             controls.setSpacing(7);
//             controls.setAlignment(Pos.CENTER_LEFT);
//             controls.setPadding(new Insets(0,5,0,0));
//        HBox.setHgrow(padding, ALWAYS);
//        
//        addMenu.setText("");
//        remMenu.setText("");
//        Icons.setIcon(addMenu, FontAwesomeIconName.PLUS, "11", "11");
//        Icons.setIcon(remMenu, FontAwesomeIconName.MINUS, "11", "11");
//        
//        // add 'edit on add to library' option menu
//        gui.objects.ContextMenu.CheckMenuItem editOnAddmi = new gui.objects.ContextMenu.CheckMenuItem("Edit added items");
//        editOnAddmi.selected.bindBidirectional(editOnAdd);
//        controlsBar.getMenus().get(0).getItems().add(editOnAddmi);
//        
//        content.getChildren().addAll(table.getRoot(), controls);
//        VBox.setVgrow(table.getRoot(),ALWAYS);
//    }
//
//    @Override
//    public void refresh() {
//        runOnce.execute(()->table.setColumnState(columnInfo));
//        
//        getFields().stream().filter(c->!c.getName().equals("Library level")&&!c.getName().equals("columnInfo")).forEach(Config::applyValue);
//        table.getSelectionModel().clearSelection();
//        lvl.applyValue();
//    }
//
//    @Override
//    public void onClose() {
//        Player.librarySelected.i.unbind(out_sel);
//        // stop listening
//        d1.unsubscribe();
//        d2.unsubscribe();
//        d3.unsubscribe();
//        d4.unsubscribe();
//    }
//    
//    @Override
//    public Node getActivityNode() {
//        return actPane;
//    }
//    
//    @FXML private void addDirectory() {
//        addNedit(editOnAdd.get(),true);
//    }
//    @FXML private void addFiles() {
//        addNedit(editOnAdd.get(),false);
//    }
//    
//    private void addNedit(boolean edit, boolean dir) {
//        Window w = root.getScene().getWindow();
//        ExtensionFilter ef = AudioFileFormat.filter(Use.APP);
//        if(dir) {
//            File f = Environment.chooseFile("Add folder to library", true, last_file, w, ef);
//            if(f!=null) {
//                addNeditDo(() -> {
//                    last_file = f.getParentFile()==null ? f : f.getParentFile();
//                    Stream<File> files = getFilesAudio(f,Use.APP,Integer.MAX_VALUE);
//                    return files.map(SimpleItem::new);
//                }, edit);
//            }
//        } else {
//            List<File> fs = Environment.chooseFiles("Add files to library", last_file, w, ef);
//            if(fs!=null) {
//                addNeditDo(() -> {
//                    File fr = getCommonRoot(fs);
//                    if(fr!=null) last_file=fr;
//                    Stream<File> files = fs.stream();
//                    return files.map(SimpleItem::new);
//                }, edit);
//            }
//        }
//    }
//    
//    private void addNeditDo(Supplier<Stream<Item>> files, boolean edit) {
//        fut().thenR(() -> {
//                 taskInfo.setVisible(true);
//                 taskInfo.message.setText("Discovering files...");
//                 taskInfo.progressIndicator.setProgress(INDETERMINATE_PROGRESS);
//             }, FX)
//             .supply(() -> files.get().collect(toList()))
//             .use(items -> {
//                 Task t = MetadataReader.readAaddMetadata(items,(ok,added) -> {
//                     if(ok && edit && !added.isEmpty())
//                         WidgetManager.use(SongWriter.class, NO_LAYOUT, w -> w.read(added));
//                     hideInfo.restart();
//                 },false);
//                 taskInfo.bind(t);
//             },FX)
//             .showProgress(App.getWindow().taskAdd())
//             .run();
//    }
//    
//    @FXML private void removeInvalid() {
//        Task t = MetadataReader.removeMissingFromLibrary((success,result) -> {
//            hideInfo.restart();
//        });
//       taskInfo.showNbind(t);
//    }
//    @FXML private void removeAll() {
//        DB.removeAllItems();
//    }
//
//    
///******************************** PUBLIC API **********************************/
//    
//    /**
//     * Converts items to Metadata using {@link Item#toMeta()} (using no I/O)
//     * and displays them in the table.
//     * <p>
//     * {@inheritDoc}
//     */
//    @Override
//    public void read(List<? extends Item> items) {
//        table.setItemsRaw(map(items,Item::toMeta));
//    }
//    
///********************************* CONFIGS ************************************/
//    
//    @Override
//    public Collection<Config<Object>> getFields() {
//        // serialize column state when requested
//        columnInfo = table.getColumnState();
//        return super.getFields();
//    }
//
//    @Override
//    public Config getField(String name) {
//        // serialize column state when requested
//        if("columnInfo".equals(name)) columnInfo = table.getColumnState();
//        return super.getField(name);
//    }
//    
///****************************** CONTEXT MENU **********************************/
//    
//    private static final TableContextMenuInstance<Metadata> contxt_menu = new TableContextMenuInstance<>(
//        () -> {
//            
//            ImprovedContextMenu<List<Metadata>> m = new ImprovedContextMenu();
//            m.getItems().addAll(menuItem("Play items", e -> {                     
//                    List<PlaylistItem> to_play = map(m.getValue(), Metadata::toPlaylist);
//                    PlaylistManager.playPlaylist(new Playlist(to_play));
//                }),
//                menuItem("Enqueue items", e -> {
//                    List<Metadata> items = m.getValue();
//                    PlaylistManager.addItems(items);
//                }),
//                menuItem("Update from file", e -> {
//                    List<Metadata> items = m.getValue();
//                    App.refreshItemsFromFileJob(items);
//                }),
//                menuItem("Remove from library", e -> {
//                    List<Metadata> items = m.getValue();
//                    DB.removeItems(items);
//                }),
//                new Menu("Show in",null,
//                    menuItems(filterMap(WidgetManager.getFactories(),f->f.hasFeature(SongReader.class),f->f.name()),
//                            (String f) -> f,
//                            (String f) -> WidgetManager.use(w->w.name().equals(f),NO_LAYOUT,c->((SongReader)c.getController()).read(m.getValue())))
//                ),
//                new Menu("Edit tags in",null,
//                    menuItems(filterMap(WidgetManager.getFactories(),f->f.hasFeature(SongWriter.class),f->f.name()),
//                            (String f) -> f,
//                            (String f) -> WidgetManager.use(w->w.name().equals(f),NO_LAYOUT,c->((SongWriter)c.getController()).read(m.getValue())))
//                ),
//                menuItem("Explore items's directory", e -> {
//                    List<Metadata> items = m.getValue();
//                    List<File> files = filterMap(items,Item::isFileBased,Item::getLocation);
//                    Environment.browse(files,true);
//                }),
//                new Menu("Explore items's directory in",null,
//                    menuItems(filterMap(WidgetManager.getFactories(),f->f.hasFeature(FileExplorerFeature.class),f->f.name()),
//                            (String f) -> f,
//                            (String f) -> WidgetManager.use(w->w.name().equals(f),NO_LAYOUT,c->((FileExplorerFeature)c.getController()).exploreFile(m.getValue().get(0).getFile())))
//                ),
//                new Menu("Search album cover",null,
//                    menuItems(App.plugins.getPlugins(HttpSearchQueryBuilder.class), 
//                            q -> "in " + Parser.toS(q),
//                            q -> Environment.browse(q.apply(m.getValue().get(0).getAlbum())))
//                )
//               );
//            return m;
//        },
//        (menu,table) -> menu.setValue(ImprovedTable.class.cast(table).getSelectedItemsCopy())
//    );
//    
//}