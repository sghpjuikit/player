
package Library;

import AudioPlayer.Player;
import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.playlist.SimpleItem;
import AudioPlayer.services.Database.DB;
import AudioPlayer.tagging.FormattedDuration;
import AudioPlayer.tagging.Metadata;
import static AudioPlayer.tagging.Metadata.Field.PATH;
import static AudioPlayer.tagging.Metadata.Field.TITLE;
import AudioPlayer.tagging.MetadataReader;
import Configuration.Config;
import Configuration.IsConfig;
import GUI.DragUtil;
import GUI.GUI;
import GUI.objects.ContextMenu.ContentContextMenu;
import GUI.objects.ContextMenu.TableContextMenuInstance;
import GUI.objects.InfoNode.TableInfo;
import static GUI.objects.InfoNode.TableInfo.DEFAULT_TEXT_FACTORY;
import GUI.objects.InfoNode.TaskInfo;
import GUI.objects.Table.FilteredTable;
import GUI.objects.Table.ImprovedTable;
import GUI.objects.Table.TableColumnInfo;
import GUI.objects.Table.TableColumnInfo.ColumnInfo;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Features.TaggingFeature;
import static Layout.Widgets.Widget.Group.LIBRARY;
import Layout.Widgets.Widget.Info;
import Layout.Widgets.WidgetManager;
import static Layout.Widgets.WidgetManager.WidgetSource.NOLAYOUT;
import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import java.io.File;
import java.util.Collection;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.singletonList;
import java.util.List;
import java.util.function.BiConsumer;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import static javafx.geometry.NodeOrientation.INHERIT;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ProgressIndicator;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import javafx.scene.control.TableColumn;
import javafx.scene.input.Dragboard;
import static javafx.scene.input.KeyCode.DELETE;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static javafx.scene.input.MouseEvent.MOUSE_RELEASED;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import static javafx.scene.layout.Priority.ALWAYS;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.reactfx.Subscription;
import util.Parser.File.AudioFileFormat;
import util.Parser.File.AudioFileFormat.Use;
import util.Parser.File.Enviroment;
import util.Parser.File.FileUtil;
import static util.Parser.File.FileUtil.getCommonRoot;
import static util.Util.DEFAULT_ALIGNED_CELL_FACTORY;
import static util.Util.consumeOnSecondaryButton;
import static util.Util.createmenuItem;
import util.access.Accessor;
import static util.async.Async.runAsTask;
import util.async.FxTimer;
import static util.functional.FunctUtil.list;
import static util.functional.FunctUtil.listM;
import util.functional.Runner;

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
    private final TaskInfo taskInfo = new TaskInfo(new Label(), new ProgressIndicator());
    private final FxTimer hideInfo = new FxTimer(Duration.seconds(5), 1, taskInfo::hideNunbind);
    private Subscription dbMonitor;
    private final FilteredTable<Metadata,Metadata.Field> table = new FilteredTable(Metadata.EMPTY.getMainField());
    
    @FXML Menu addMenu;
    @FXML Menu remMenu;
    @FXML MenuBar controlsBar;
    private final Runner runOnce = new Runner(1);
    
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
    public final Accessor<Integer> lvl = new Accessor<>(1, v -> {
        if(dbMonitor!=null) dbMonitor.unsubscribe();
        // listen for database changes to refresh library
        dbMonitor = DB.views.subscribe(v, (i,list) -> table.setItemsRaw(list));
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
            c.setCellFactory(DEFAULT_ALIGNED_CELL_FACTORY(f.getType(), ""));
            c.setUserData(f);
            return c;
        });
        columnInfo = table.getDefaultColumnInfo();
        
        // context menu & play
        table.setOnMouseClicked( e -> {
            if (e.getY()<table.getFixedCellSize()) return;
            if(e.getButton()==PRIMARY) {
                if(e.getClickCount()==2) {
                    Playlist p = new Playlist(listM(table.getItems(),Metadata::toPlaylistItem));
                    PlaylistManager.playPlaylistFrom(p, table.getSelectionModel().getSelectedIndex());
                }
            } else
            if(e.getButton()==SECONDARY) {
                contxt_menu.show(table, e);
            }
        });
        
        // key actions
        table.setOnKeyReleased( e -> {
            if (e.getCode() == ENTER) {     // play first of the selected
                if(!table.getSelectionModel().isEmpty()) {
                    Playlist p = new Playlist(listM(table.getItems(),Metadata::toPlaylistItem));
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
        
        // prevent scrol event to propagate up
        root.setOnScroll(Event::consume);
        
        // prevent overly eager selection change
        table.addEventFilter(MOUSE_PRESSED, consumeOnSecondaryButton);
        table.addEventFilter(MOUSE_RELEASED, consumeOnSecondaryButton);
        
        // update selected items for application
        table.getSelectionModel().getSelectedItems().addListener( (Observable o) -> Player.librarySelectedItemsES.push(table.getSelectedItemsCopy()));
        table.getSelectionModel().selectedItemProperty().addListener( (o,ov,nv) -> Player.librarySelectedItemES.push(nv));
        
        // task information label
        taskInfo.setVisible(false);        
        
        // table information label
        TableInfo<Metadata> infoL = new TableInfo(new Label(), table);
        infoL.textFactory = (all, list) -> {
            double d = list.stream().mapToDouble(Metadata::getLengthInMs).sum();
            return DEFAULT_TEXT_FACTORY.apply(all, list) + " - " + new FormattedDuration(d);
        };
        
        // controls bottom header
        Region padding = new Region();
        HBox controls = new HBox(controlsBar,infoL.node,padding,taskInfo.labeled, taskInfo.progressIndicator);
             controls.setSpacing(7);
             controls.setAlignment(Pos.CENTER_LEFT);
             controls.setPadding(new Insets(0,5,0,0));
        HBox.setHgrow(padding, ALWAYS);
        
        addMenu.setText("");
        remMenu.setText("");
        AwesomeDude.setIcon(addMenu, AwesomeIcon.PLUS, "11", "11");
        AwesomeDude.setIcon(remMenu, AwesomeIcon.MINUS, "11", "11");
        
        
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
        dbMonitor.unsubscribe();
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
        List<File> files;
        if(dir) {
            File f = Enviroment.chooseFile("Add folder to library", true, last_file,
                    root.getScene().getWindow(), AudioFileFormat.filter(Use.APP));
            files = f==null ? EMPTY_LIST : singletonList(f);
            if (f!=null) last_file = f;
        } else {
            files = Enviroment.chooseFiles("Add files to library", last_file,
                    root.getScene().getWindow(), AudioFileFormat.filter(Use.APP));
            File f = files==null ? null : getCommonRoot(files);
            if(f!=null) last_file=f;
        }

        if(files!=null) {
            Task ts = runAsTask("Discovering files",
                    () -> listM(FileUtil.getAudioFiles(files,Use.APP,6), SimpleItem::new),
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
                    List<PlaylistItem> to_play = listM(m.getValue(), Metadata::toPlaylistItem);
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
                    WidgetManager.use(TaggingFeature.class, NOLAYOUT,w->w.read(items));
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
}