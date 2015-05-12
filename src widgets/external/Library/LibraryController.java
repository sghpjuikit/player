
package Library;

import AudioPlayer.Player;
import AudioPlayer.playlist.*;
import AudioPlayer.services.Database.DB;
import AudioPlayer.tagging.Metadata;
import static AudioPlayer.tagging.Metadata.Field.*;
import AudioPlayer.tagging.MetadataReader;
import Configuration.Config;
import Configuration.IsConfig;
import GUI.GUI;
import GUI.InfoNode.InfoTable;
import static GUI.InfoNode.InfoTable.DEFAULT_TEXT_FACTORY;
import GUI.InfoNode.InfoTask;
import GUI.objects.ActionChooser;
import GUI.objects.ContextMenu.ContentContextMenu;
import GUI.objects.ContextMenu.TableContextMenuInstance;
import GUI.objects.Icon;
import GUI.objects.Spinner.Spinner;
import GUI.objects.Table.FilteredTable;
import GUI.objects.Table.ImprovedTable;
import GUI.objects.Table.TableColumnInfo;
import GUI.objects.Table.TableColumnInfo.ColumnInfo;
import GUI.objects.TableRow.ImprovedTableRow;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Features.TaggingFeature;
import static Layout.Widgets.Widget.Group.LIBRARY;
import Layout.Widgets.Widget.Info;
import Layout.Widgets.WidgetManager;
import static Layout.Widgets.WidgetManager.WidgetSource.NOLAYOUT;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.SQUARE_ALT;
import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import static java.util.stream.Collectors.toList;
import java.util.stream.Stream;
import javafx.beans.property.ReadOnlyObjectWrapper;
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
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.*;
import static javafx.scene.input.TransferMode.COPY;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import static javafx.scene.layout.Priority.ALWAYS;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import main.App;
import static org.reactfx.EventStreams.changesOf;
import static org.reactfx.EventStreams.nonNullValuesOf;
import org.reactfx.Subscription;
import util.Animation.Anim;
import static util.Animation.Anim.Interpolators.reverse;
import util.Animation.Interpolators.ElasticInterpolator;
import util.File.AudioFileFormat;
import util.File.AudioFileFormat.Use;
import util.File.Environment;
import static util.File.FileUtil.getCommonRoot;
import static util.File.FileUtil.getFilesAudio;
import static util.Util.*;
import util.access.Accessor;
import static util.async.Async.FX;
import util.async.executor.FxTimer;
import util.async.executor.LimitedExecutor;
import util.async.future.Fut;
import static util.functional.Util.list;
import static util.functional.Util.listM;
import util.functional.functor.FunctionC;
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
public class LibraryController extends FXMLController {public String a() { return ""; }
    
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
//                 .playBFrom());
                super.setVisible(v);
            }
        }
    };
    private final FxTimer hideInfo = new FxTimer(5000, 1, () -> {
        new Anim(at->setScaleXY(taskInfo.progressIndicator,at*at)).dur(500).intpl(reverse(new ElasticInterpolator()))
            .then(taskInfo::hideNunbind)
            .play();
        
    });
//    private final FxTimer hideInfo = new FxTimer(5000, 1, taskInfo::hideNunbind);
    private final FilteredTable<Metadata,Metadata.Field> table = new FilteredTable(Metadata.EMPTY.getMainField());
    ActionChooser actPane = new ActionChooser();
    Icon lvlB = actPane.addIcon(SQUARE_ALT, "1", "Level", true, false);
    
    @FXML Menu addMenu;
    @FXML Menu remMenu;
    @FXML MenuBar controlsBar;
    private final LimitedExecutor runOnce = new LimitedExecutor(1);
    // dependencies to disopose of
    private Subscription d1, d2, d3;
    
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
    @IsConfig(name = "Library level", info = "", min = 1, max = 8)
    public final Accessor<Integer> lvl = new Accessor<Integer>(DB.views.getLastLvl()+1, v -> {
        // maintain info text
        lvlB.setText(v.toString());
        if(d1!=null) d1.unsubscribe();
        // listen for database changes to refresh library
        d1 = DB.views.subscribe(v, (i,list) -> table.setItemsRaw(list));
        // initialize
        table.setItemsRaw(DB.views.getValue(v));
    });
    
    @IsConfig(editable = false)
    private File last_file = new File("");
    
    
    @Override
    public void init() {
        table.setFixedCellSize(GUI.font.getValue().getSize() + 5);
        table.getSelectionModel().setSelectionMode(MULTIPLE);
        
        // set up table columns
        table.setColumnStateFacory( f -> {
            double w = f==PATH || f==TITLE ? 150 : 50;
            return new ColumnInfo(f.toString(), f.ordinal(), f.isCommon(), w);
        });
        table.setColumnFactory( f -> {
            TableColumn<Metadata,?> c = new TableColumn(f.toString());
            c.setCellValueFactory( cf -> {
                if(cf.getValue()==null) return null;
                return new ReadOnlyObjectWrapper(cf.getValue().getField(f));
            });
            c.setCellFactory(f==RATING
                ? (FunctionC) App.ratingCell.getValue()
                : cellFactoryAligned(f.getType(), ""));
            if(f==Metadata.Field.TRACK || f==Metadata.Field.DISC || 
               f==Metadata.Field.TRACKS_TOTAL || f==Metadata.Field.DISCS_TOTAL) {
                c.setComparator((Comparator) new Comparator<Integer>() {
                    @Override
                    public int compare(Integer o1, Integer o2) {
                        return Integer.compare(o1, o2);
                    }
                });
            }
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
                    Playlist p = new Playlist(listM(table.getItems(),Metadata::toPlaylist));
                    PlaylistManager.playPlaylistFrom(p, r.getIndex());
                })
                .onRightSingleClick((r,e) -> {
                    // prep selection for context menu
                    if(!r.isSelected())
                        tbl.getSelectionModel().clearAndSelect(r.getIndex());
                    // show context menu
                    contxt_menu.show(table, e);
                })
        );
        
        // key actions
        table.setOnKeyReleased( e -> {
            if (e.getCode() == ENTER) {     // play first of the selected
                if(!table.getSelectionModel().isEmpty()) {
                    Playlist p = new Playlist(listM(table.getItems(),Metadata::toPlaylist));
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
            addNeditDo(DragUtil.getSongs(e), false);
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
        
        // prevent selection change on right click
        table.addEventFilter(MOUSE_PRESSED, consumeOnSecondaryButton);
        // table.addEventFilter(MOUSE_RELEASED, consumeOnSecondaryButton);
        // prevent context menu changing selection
        // table.addEventFilter(ContextMenuEvent.ANY, Event::consume);
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
        
        
        content.getChildren().addAll(table.getRoot(), controls);
        VBox.setVgrow(table.getRoot(),ALWAYS);
    }

    @Override
    public void refresh() {
        runOnce.execute(()->table.setColumnState(columnInfo));
        
        getFields().stream().filter(c->!c.getName().equals("Library level")&&!c.getName().equals("columnInfo")).forEach(Config::applyValue);
        table.getSelectionModel().clearSelection();
        lvl.applyValue();
        
    }

    @Override
    public void close() {
        // stop listening
        d1.unsubscribe();
        d2.unsubscribe();
        d3.unsubscribe();
    }
    
    
    @FXML private void addDirectory() {
        addNedit(false,true);
    }
    
    @FXML private void addDirectoryNedit() {
        addNedit(true,true);
    }
    
    @FXML private void addFiles() {
        addNedit(false,false);
    }
    
    @FXML private void addFilesNedit() {
        addNedit(true,false);
    }
    
    private void addNedit(boolean edit, boolean dir) {
        // get files        
        if(dir) {
            File f = Environment.chooseFile("Add folder to library", true, last_file,
                    root.getScene().getWindow(), AudioFileFormat.filter(Use.APP));
            addNeditDo(CompletableFuture.supplyAsync(() -> {
                Stream<File> files = f==null ? Stream.empty() : getFilesAudio(f,Use.APP,Integer.MAX_VALUE);
                if (f!=null) last_file = f.getParentFile()==null ? f : f.getParentFile();
                return files.map(SimpleItem::new);
            }), edit);
        } else {
            List<File> fs = Environment.chooseFiles("Add files to library", last_file,
                    root.getScene().getWindow(), AudioFileFormat.filter(Use.APP));
            addNeditDo(CompletableFuture.supplyAsync(() -> {
                Stream<File> files = fs.stream();
                File f = files==null ? null : getCommonRoot(fs);
                if(f!=null) last_file=f;
                return files.map(SimpleItem::new);
            }), edit);
        }
    }
    
    private void addNeditDo(CompletableFuture<Stream<Item>> files, boolean edit) {
        new Fut<>()
            .thenR(()->{
                taskInfo.setVisible(true);
                taskInfo.message.setText("Discovering files...");
                taskInfo.progressIndicator.setProgress(INDETERMINATE_PROGRESS);
            }, FX)
            .then(files)
            .then(items -> items.collect(toList()))
            .use(items ->{
                Task t = MetadataReader.readAaddMetadata(items,(ok,added) -> {
                    if(ok & edit)
                        WidgetManager.use(TaggingFeature.class, NOLAYOUT, w -> w.read(added));

                    hideInfo.restart();
//                    taskInfo.hideNunbind();
                },false);
                taskInfo.bind(t);
            }, FX)
            .run();
    }
    
    @FXML private void removeInvalid() {
        Task t = MetadataReader.removeMissingFromLibrary((success,result) -> {
            hideInfo.restart();
//            taskInfo.hideNunbind();
        });
       taskInfo.showNbind(t);
    }
    @FXML private void removeAll() {
        DB.clearLib();
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
            ContentContextMenu<List<Metadata>> m = new ContentContextMenu();
            m.getItems().addAll(menuItem("Play items", e -> {                     
                    List<PlaylistItem> to_play = listM(m.getValue(), Metadata::toPlaylist);
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
                menuItem("Edit the item/s in tag editor", e -> {
                    List<Metadata> items = m.getValue();
                    WidgetManager.use(TaggingFeature.class, NOLAYOUT ,w->w.read(items));
                }),
                menuItem("Explore items's directory", e -> {
                    List<Metadata> items = m.getValue();
                    List<File> files = list(items, Item::isFileBased, Item::getLocation);
                    Environment.browse(files,true);
                }),
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
    
    {
        lvlB.setOnMouseClicked(e -> {
            if(e.getButton()==PRIMARY)
                lvl.setNapplyValue(clip(1,lvl.getValue()+1,8));
            if(e.getButton()==SECONDARY)
                lvl.setNapplyValue(clip(1,lvl.getValue()-1,8));
            e.consume();
        });
    }
    
    @Override
    public Node getActivityNode() {
        return actPane;
    }
}