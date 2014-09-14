
package Library;

import AudioPlayer.Player;
import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.playlist.SimpleItem;
import AudioPlayer.services.Database.DB;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.Metadata.Field;
import AudioPlayer.tagging.MetadataReader;
import Configuration.IsConfig;
import GUI.DragUtil;
import GUI.GUI;
import GUI.objects.ContextMenu.ContentContextMenu;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Features.TaggingFeature;
import static Layout.Widgets.Widget.Group.LIBRARY;
import Layout.Widgets.Widget.Info;
import Layout.Widgets.WidgetManager;
import static Layout.Widgets.WidgetManager.WidgetSource.NOLAYOUT;
import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import java.io.File;
import java.util.ArrayList;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.singletonList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.Dragboard;
import static javafx.scene.input.KeyCode.DELETE;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import static javafx.util.Duration.ZERO;
import org.reactfx.Subscription;
import utilities.FxTimer;
import utilities.Parser.File.Enviroment;
import utilities.Parser.File.FileUtil;
import static utilities.Parser.File.FileUtil.getCommonRoot;
import utilities.SingleInstance;
import utilities.Util;
import utilities.access.Accessor;
import utilities.functional.functor.BiProcedure;

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
    version = "0.3",
    year = "2014",
    group = LIBRARY
)
public class LibraryController extends FXMLController {
    
    private @FXML AnchorPane root;
    private TableView<Metadata> table = new TableView();
    private final Label progressL = new Label();
    private Subscription dbMonitor;
    
    @FXML Menu addMenu;
    @FXML Menu remMenu;
    @FXML MenuBar controlsBar;
    
    @Override
    public void init() {
        root.getChildren().add(table);
        Util.setAPAnchors(table, 0,0,30,0);
        
        table.getSelectionModel().setSelectionMode(MULTIPLE);
        table.setFixedCellSize(GUI.font.getValue().getSize() + 5);
        
        // add index column
        TableColumn indexColumn = Util.createIndexColumn("#");
        table.getColumns().add(indexColumn);
        
        // add data columns
        for(Field mf : Field.values()) {
            if(!mf.isTypeStringRepresentable()) continue;
            TableColumn<Metadata,Object> c = new TableColumn(mf.toCapitalizedS());
            c.setCellValueFactory((TableColumn.CellDataFeatures<Metadata,Object> cf) -> {
                if(cf.getValue()==null) return null;
                return new ReadOnlyObjectWrapper(cf.getValue().getField(mf));
            });
            c.setCellFactory(Util.DEFAULT_ALIGNED_CELL_FACTORY(mf.getType()));
            table.getColumns().add(c);
        }
        
        // context menu hide/show
        table.setOnMouseClicked( e -> {
            if(e.getButton()==PRIMARY) {
                if(e.getClickCount()==1) {
                    // hide CM on left click
                    ContentContextMenu cm = contxt_menu.get();
                    if(cm!=null && cm.isShowing())
                        cm.hide();
                } else {
                    Playlist p = new Playlist(table.getItems().stream()
                            .map(Metadata::toPlaylistItem)
                            .collect(Collectors.toList()));
                    PlaylistManager.playPlaylistFrom(p, table.getSelectionModel().getSelectedIndex());
                }
            } else
            if(e.getButton()==SECONDARY)
                // show CM on right click
                contxt_menu.get(table).show(table, e.getScreenX(), e.getScreenY());
        });
        // set key-induced actions
        table.setOnKeyReleased( e -> {
            if (e.getCode() == ENTER) {     // play first of the selected
                if(!table.getSelectionModel().isEmpty()) {
                    Playlist p = new Playlist(table.getItems().stream()
                            .map(Metadata::toPlaylistItem)
                            .collect(Collectors.toList()));
                    PlaylistManager.playPlaylistFrom(p, table.getSelectionModel().getSelectedIndex());
                }
            } else
            if (e.getCode() == DELETE) {    // delete selected
                DB.removeItems(table.getSelectionModel().getSelectedItems());
            } else
            if (e.getCode() == ESCAPE) {    // deselect
                table.getSelectionModel().clearSelection();
            }
        });
        
        // handle drag from - copy selected items
        table.setOnDragDetected( e -> {
            if (e.getButton() == PRIMARY && e.getY()>table.getFixedCellSize()) {
                Dragboard db = table.startDragAndDrop(TransferMode.COPY);
                DragUtil.setItemList(Util.copySelectedItems(table),db);
                e.consume();
            }
        });
        
        // prevent scrol event to propagate up
        root.setOnScroll(Event::consume);
        
        
        // update selected items for application
        table.getSelectionModel().getSelectedItems().addListener( (Observable o) -> Player.librarySelectedItemsES.push(Util.copySelectedItems(table)));
        table.getSelectionModel().selectedItemProperty().addListener( (o,ov,nv) -> Player.librarySelectedItemES.push(nv));
        
        
        progressL.setVisible(false);
        root.getChildren().add(progressL);
        AnchorPane.setBottomAnchor(progressL, 0d);
        AnchorPane.setRightAnchor(progressL, 0d);
                
        // information label
        Label infoL  = new Label();
            // updates info label
        BiProcedure<Boolean,List<? extends Metadata>> infoUpdate = (all,list) -> {
            if(!all & list.isEmpty()) {
                all = true;
                list = table.getItems();
            }
            Duration du = list.stream().map(m -> (Duration)m.getLength()).reduce(ZERO, Duration::add);
            String prefix1 = all ? "All:" : "Selected: ";
            String prefix2 = list.size()==1 ? " item " : " items ";
            infoL.setText(prefix1 + list.size() + prefix2 + "- " + Util.formatDuration(du));
        };
            // calls info label update on selection change
        ListChangeListener<Metadata> infoUpdater = c -> infoUpdate.accept(c==table.getItems(),c.getList());
        table.getSelectionModel().getSelectedItems().addListener(infoUpdater);
        table.getItems().addListener(infoUpdater);
            // initialize info label
        infoUpdate.accept(true,EMPTY_LIST);
        
        // controls bottom header
        HBox controls = new HBox(controlsBar,infoL);
             controls.setSpacing(7);
             controls.setAlignment(Pos.CENTER_LEFT);
             controls.setPadding(new Insets(2));
        
        root.getChildren().add(controls);
        AnchorPane.setBottomAnchor(controls, 0d);
        AnchorPane.setLeftAnchor(controls, 0d);
        
        // listen for database changes to refresh library
        dbMonitor = DB.fieldSelectionChange.subscribe( (field,value) -> {
            changeValue = value; // needs to be ready before the next line
            changeField.setValue(field); // this causes reloading of the data
            refresh();
        });
        
        
        
        addMenu.setText("");
        remMenu.setText("");
        AwesomeDude.setIcon(addMenu, AwesomeIcon.PLUS, "11", "11");
        AwesomeDude.setIcon(remMenu, AwesomeIcon.MINUS, "11", "11");
        
    }
    
    @IsConfig(editable = false)
    private Object changeValue = "";
    
    @IsConfig(editable = false)
    private final Accessor<Metadata.Field> changeField = new Accessor<>(Metadata.Field.ARTIST, v -> {
        table.getSelectionModel().clearSelection();
        table.getItems().setAll(DB.getAllItemsWhere(v, changeValue));
    });
    
    @IsConfig(editable = false)
    private File last_file = new File("");
    

    @Override
    public void refresh() {
        table.getSelectionModel().clearSelection();
        table.getItems().setAll(DB.getAllItemsWhere(changeField.getValue(), changeValue));
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
            File f = Enviroment.chooseFile("Add folder to library", true, last_file, root.getScene().getWindow());
            files = f==null ? EMPTY_LIST : singletonList(f);
            if (f!=null) last_file = f;
        } else {
            files = Enviroment.chooseFiles("Add files to library", last_file, root.getScene().getWindow());
            File d = getCommonRoot(files);
            if(d!=null) last_file=d;
        }

        if(files!=null) {
            List<Metadata> metas = FileUtil.getAudioFiles(files,0).stream()
                   .map(SimpleItem::new)
                   .map(SimpleItem::toMetadata)
                   .collect(Collectors.toList());
            
            BiConsumer<Boolean,List<Metadata>> onEnd = !edit ? null : (success,added) -> {
                if(success) {
                    progressL.textProperty().unbind();
                    FxTimer.run(Duration.seconds(5), () -> progressL.setVisible(false));
                    WidgetManager.use(TaggingFeature.class, NOLAYOUT, w -> w.read(added));
                }
            };
            
            Task t = MetadataReader.readAaddMetadata(metas,onEnd);
            // display progress & hide on end
            progressL.setVisible(true);
            progressL.textProperty().bind(t.messageProperty());
        }
    }
    
    @FXML private void removeInvalid() {
        Task t = MetadataReader.removeMissingFromLibrary((success,result) -> {
            progressL.textProperty().unbind();
            FxTimer.run(Duration.seconds(5), () -> progressL.setVisible(false));
        });
        // display progress & hide on end
        progressL.setVisible(true);
        progressL.textProperty().bind(t.messageProperty());
    }
    
/****************************** CONTEXT MENU **********************************/
    
    private static final SingleInstance<ContentContextMenu<List<Metadata>>,TableView<Metadata>> contxt_menu = new SingleInstance<>(
        () -> {
            ContentContextMenu<List<Metadata>> contextMenu = new ContentContextMenu();
            contextMenu.getItems().addAll(
                Util.createmenuItem("Play items", e -> {                     
                    List<PlaylistItem> to_play = new ArrayList();
                    contextMenu.getItem().stream().map(Metadata::toPlaylistItem).forEach(to_play::add);
                    PlaylistManager.playPlaylist(new Playlist(to_play));
                }),
                Util.createmenuItem("Enqueue items", e -> {
                    List<Metadata> items = contextMenu.getItem();
                    PlaylistManager.addItems(items);
                }),
                Util.createmenuItem("Update from file", e -> {
                    List<Metadata> items = contextMenu.getItem();
                    DB.updateItemsFromFile(items);
                }),
                Util.createmenuItem("Remove from library", e -> {
                    List<Metadata> items = contextMenu.getItem();
                    DB.removeItems(items);
                }),
                Util.createmenuItem("Edit the item/s in tag editor", e -> {
                    List<Metadata> items = contextMenu.getItem();
                    WidgetManager.use(TaggingFeature.class, NOLAYOUT,w->w.read(items));
                }),
                Util.createmenuItem("Explore items's directory", e -> {
                    List<Metadata> items = contextMenu.getItem();
                    List<File> files = items.stream()
                            .filter(Item::isFileBased)
                            .map(Item::getLocation)
                            .collect(Collectors.toList());
                    Enviroment.browse(files,true);
                })
               );
            contextMenu.setConsumeAutoHidingEvents(false);
            return contextMenu;
        },
        (menu,table) -> menu.setItem(Util.copySelectedItems(table))
    );
}