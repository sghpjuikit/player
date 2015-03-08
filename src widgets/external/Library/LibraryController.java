
package Library;

import AudioPlayer.Player;
import AudioPlayer.playlist.*;
import AudioPlayer.services.Database.DB;
import AudioPlayer.tagging.Metadata;
import static AudioPlayer.tagging.Metadata.Field.*;
import AudioPlayer.tagging.MetadataReader;
import Configuration.Config;
import Configuration.IsConfig;
import GUI.DragUtil;
import GUI.GUI;
import GUI.objects.ActionChooser;
import GUI.objects.ContextMenu.ContentContextMenu;
import GUI.objects.ContextMenu.TableContextMenuInstance;
import GUI.objects.Table.FilteredTable;
import GUI.objects.Table.ImprovedTable;
import GUI.objects.Table.TableColumnInfo;
import GUI.objects.Table.TableColumnInfo.ColumnInfo;
import GUI.virtual.InfoNode.InfoTable;
import static GUI.virtual.InfoNode.InfoTable.DEFAULT_TEXT_FACTORY;
import GUI.virtual.InfoNode.InfoTask;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Features.TaggingFeature;
import static Layout.Widgets.Widget.Group.LIBRARY;
import Layout.Widgets.Widget.Info;
import Layout.Widgets.WidgetManager;
import static Layout.Widgets.WidgetManager.WidgetSource.NOLAYOUT;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.SQUARE_ALT;
import java.io.File;
import static java.lang.Math.floor;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
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
import static javafx.scene.control.ContentDisplay.CENTER;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.Dragboard;
import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.*;
import javafx.scene.input.TransferMode;
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
import util.File.AudioFileFormat;
import util.File.AudioFileFormat.Use;
import util.File.Enviroment;
import static util.File.FileUtil.getCommonRoot;
import static util.File.FileUtil.getFilesAudio;
import static util.Util.*;
import util.access.Accessor;
import static util.async.Async.runAsTask;
import util.async.FxTimer;
import util.functional.Runner;
import static util.functional.Util.list;
import static util.functional.Util.listM;
import util.graphics.Icons;
import util.units.FormattedDuration;

/**
 *
 * @author Plutonium_
 */
@Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Library",
    description = "Provides access to database.",
    howto = "Available actions:\n" +
            "    Item left click : Selects item\n" +
            "    Item right click : Opens context menu\n" +
            "    Item double click : Plays item\n" +
            "    Item drag : Starts drag & drop\n" +
            "    Press ENTER : Plays item\n" +
            "    Press DELETE : Removes items from library\n" +
            "    Press ESC : Clear selection & filter\n" +
            "    Scroll : Scroll table vertically\n" +
            "    Scroll + SHIFT : Scroll table horizontally\n" +
            "    Drag column : Changes column order\n" +
            "    Click column : Changes sort order - ascending,\n" +
            "                   descending, none\n" +
            "    Click column + SHIFT : Sorts by multiple columns\n" +
            "    Menu bar : Opens additional actions\n",
    notes = "",
    version = "0.6",
    year = "2014",
    group = LIBRARY
)
public class LibraryController extends FXMLController {
    
    private @FXML AnchorPane root;
    private @FXML VBox content;
    private final InfoTask taskInfo = new InfoTask(null, new Label(), new ProgressIndicator());
    private final FxTimer hideInfo = new FxTimer(5000, 1, taskInfo::hideNunbind);
    private final FilteredTable<Metadata,Metadata.Field> table = new FilteredTable(Metadata.EMPTY.getMainField());
    ActionChooser actPane = new ActionChooser();
    Labeled lvlB = actPane.addIcon(SQUARE_ALT, "1", "Level", false);
    
    @FXML Menu addMenu;
    @FXML Menu remMenu;
    @FXML MenuBar controlsBar;
    private final Runner runOnce = new Runner(1);
    // dependencies to disopose of
    private Subscription d1;
    private Subscription d2;
    private Subscription d3;
    
    // configurables
    @IsConfig(name = "Table orientation", info = "Orientation of the table.")
    public final Accessor<NodeOrientation> table_orient = new Accessor<>(INHERIT, table::setNodeOrientation);
    @IsConfig(name = "Zeropad numbers", info = "Adds 0 to uphold number length consistency.")
    public final Accessor<Boolean> zeropad = new Accessor<>(true, table::setZeropadIndex);
    @IsConfig(name = "Search show original index", info = "Show index of the table items as in unfiltered state when filter applied.")
    public final Accessor<Boolean> orig_index = new Accessor<>(true, table::setShowOriginalIndex);
    @IsConfig(name = "Show table header", info = "Show table header with columns.")
    public final Accessor<Boolean> show_header = new Accessor<>(true, table::setHeaderVisible);
    @IsConfig(name = "Show table menu button", info = "Show table menu button for controlling columns.")
    public final Accessor<Boolean> show_menu_button = new Accessor<>(true, table::setTableMenuButtonVisible);
    @IsConfig(editable = false)
    private TableColumnInfo columnInfo;
    @IsConfig(name = "Library level", info = "")
    public final Accessor<Integer> lvl = new Accessor<>(DB.views.getLastLvl()+1, v -> {
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
            c.setCellValueFactory( f==RATING 
                ? cf -> {
                        if(cf.getValue()==null) return null;
                        String s = cf.getValue().getRatingPercentAsString();
                        if(s.length()>4) s = s.substring(0,4);
                        return new ReadOnlyObjectWrapper(s);
                    }
                : cf -> {
                        if(cf.getValue()==null) return null;
                        return new ReadOnlyObjectWrapper(cf.getValue().getField(f));
                    }
            );
            c.setCellValueFactory(cf -> {
                        if(cf.getValue()==null) return null;
                        return new ReadOnlyObjectWrapper(cf.getValue().getField(f));
                    });
            c.setCellFactory(f==RATING
                ? (Callback)App.ratingCell.getValue()
                : DEFAULT_ALIGNED_CELL_FACTORY(f.getType(), ""));
            c.setUserData(f);
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
        // maintain rating column cell style
        App.ratingCell.addListener((o,ov,nv) -> table.getColumn(RATING).ifPresent(c->c.setCellFactory((Callback)nv)));
        columnInfo = table.getDefaultColumnInfo();
        
        
        
        // context menu & play
        table.addEventHandler(MOUSE_CLICKED, e -> {
            if (table.isTableHeaderVisible() && e.getY()<table.getTableHeaderHeight()) return;
            if(e.getButton()==PRIMARY) {
                if(e.getClickCount()==2) {
                    Playlist p = new Playlist(listM(table.getItems(),Metadata::toPlaylist));
                    PlaylistManager.playPlaylistFrom(p, table.getSelectionModel().getSelectedIndex());
                }
            } else
            if(e.getButton()==SECONDARY) {
                // prepare selection for context menu
                double h = table.isTableHeaderVisible() ? e.getY() - table.getTableHeaderHeight() : e.getY();
                int i = (int)floor(h/table.getFixedCellSize()); // row index
                if(!table.getSelectionModel().isSelected(i))
                    table.getSelectionModel().clearAndSelect(i);
                // show context menu
                contxt_menu.show(table, e);
                e.consume();
            }
        });
        
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
        
        // handle drag from - copy selected items
        table.setOnDragDetected( e -> {
            if (e.getButton() == PRIMARY && e.getY()>table.getFixedCellSize()) {
                Dragboard db = table.startDragAndDrop(TransferMode.COPY);
                DragUtil.setItemList(table.getSelectedItemsCopy(),db);
                e.consume();
            }
        });
        table.setOnDragOver(e -> {
            e.acceptTransferModes(COPY);
            e.consume();
        });
//        table.setOnDragOver(DragUtil.audioDragAccepthandler);
        table.setOnDragDropped(e-> addNeditDo(DragUtil.getAudioItems(e).stream().map(Item::getFile),false));
        
        // prevent selection change on right click
        table.addEventFilter(MOUSE_PRESSED, consumeOnSecondaryButton);
        table.addEventFilter(MOUSE_RELEASED, consumeOnSecondaryButton);
        // prevent context menu changing selection despite the above
        table.addEventFilter(ContextMenuEvent.ANY, Event::consume);
        // prevent volume change
        table.setOnScroll(Event::consume);
        
        // update selected items for application
        d2 = Player.librarySelectedItemES.feedFrom(nonNullValuesOf(table.getSelectionModel().selectedItemProperty()));
        d3 = Player.librarySelectedItemsES.feedFrom(changesOf(table.getSelectionModel().getSelectedItems()).map(i->table.getSelectedItemsCopy()));
        
        // task information label
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
        runOnce.run(()->table.setColumnState(columnInfo));
        
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
        Stream<File> files;
        if(dir) {
            File f = Enviroment.chooseFile("Add folder to library", true, last_file,
                    root.getScene().getWindow(), AudioFileFormat.filter(Use.APP));
            files = f==null ? Stream.empty() : getFilesAudio(f,Use.APP,Integer.MAX_VALUE);
            if (f!=null) last_file = f;
        } else {
            List<File> fs = Enviroment.chooseFiles("Add files to library", last_file,
                    root.getScene().getWindow(), AudioFileFormat.filter(Use.APP));
            files = fs.stream();
            File f = files==null ? null : getCommonRoot(fs);
            if(f!=null) last_file=f;
        }
        
        addNeditDo(files, edit);
    }
    
    private void addNeditDo(Stream<File> files, boolean edit) {
        if(files!=null) {
            Task ts = runAsTask("Discovering files",
                    () -> files.map(SimpleItem::new).collect(toList()),
                    (success,result) -> {
                        if (success) {
                            BiConsumer<Boolean,List<Metadata>> onEnd = (succes,added) -> {
                                if(succes & edit)
                                    WidgetManager.use(TaggingFeature.class, NOLAYOUT, w -> w.read(added));
                                    
                                hideInfo.restart();
                            };

                            Task t = MetadataReader.readAaddMetadata(result,onEnd,false);
                            taskInfo.showNbind(t);
                        } else {
                            hideInfo.restart();
                        }
                    });
            taskInfo.showNbind(ts);
        }
    }
    
    @FXML private void removeInvalid() {
        Task t = MetadataReader.removeMissingFromLibrary((success,result) -> {
            hideInfo.restart();
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
            m.getItems().addAll(
                createmenuItem("Play items", e -> {                     
                    List<PlaylistItem> to_play = listM(m.getValue(), Metadata::toPlaylist);
                    PlaylistManager.playPlaylist(new Playlist(to_play));
                }),
                createmenuItem("Enqueue items", e -> {
                    List<Metadata> items = m.getValue();
                    PlaylistManager.addItems(items);
                }),
                createmenuItem("Update from file", e -> {
                    List<Metadata> items = m.getValue();
                    DB.updateItemsFromFile(items);
                }),
                createmenuItem("Remove from library", e -> {
                    List<Metadata> items = m.getValue();
                    DB.removeItems(items);
                }),
                createmenuItem("Edit the item/s in tag editor", e -> {
                    List<Metadata> items = m.getValue();
                    WidgetManager.use(TaggingFeature.class, NOLAYOUT ,w->w.read(items));
                }),
                createmenuItem("Explore items's directory", e -> {
                    List<Metadata> items = m.getValue();
                    List<File> files = list(items, Item::isFileBased, Item::getLocation);
                    Enviroment.browse(files,true);
                })
               );
            return m;
        },
        (menu,table) -> menu.setValue(ImprovedTable.class.cast(table).getSelectedItemsCopy())
    );
    
    {
        lvlB.setContentDisplay(CENTER);
        lvlB.setOnMouseClicked(e -> {
            if(e.getButton()==PRIMARY) {  
                lvl.setNapplyValue(lvl.getValue()+1);
            }
            if(e.getButton()==SECONDARY) {  
                lvl.setNapplyValue(lvl.getValue()-1);
            }
            e.consume();
        });
    }
    
    @Override
    public Node getActivityNode() {
        return actPane;
    }
}