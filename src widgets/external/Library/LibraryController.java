
package Library;

import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.playlist.SimpleItem;
import AudioPlayer.services.Database.DB;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.Metadata.Field;
import AudioPlayer.tagging.MetadataReader;
import GUI.GUI;
import GUI.objects.ContextMenu.ContentContextMenu;
import GUI.objects.FadeButton;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Features.TaggingFeature;
import Layout.Widgets.Widget;
import static Layout.Widgets.Widget.Group.LIBRARY;
import Layout.Widgets.WidgetInfo;
import Layout.Widgets.WidgetManager;
import static Layout.Widgets.WidgetManager.Widget_Source.FACTORY;
import static de.jensd.fx.fontawesome.AwesomeIcon.PLUS;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import static javafx.geometry.Pos.CENTER_RIGHT;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import static javafx.scene.input.KeyCode.DELETE;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;
import static javafx.util.Duration.ZERO;
import org.reactfx.Subscription;
import utilities.Enviroment;
import utilities.FileUtil;
import utilities.FxTimer;
import utilities.SingleInstance;
import utilities.Util;

/**
 *
 * @author Plutonium_
 */
@WidgetInfo(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Library",
    description = "Provides access to database.",
    howto = "Available actions:\n" +
            "    Item left click : Selects item\n" +
            "    Item right click : Opens context menu\n" +
            // not yet implemented
//            "    Item double click : Plays item\n" +
//            "    Item drag : Moves item within playlist\n" +
//            "    Item drag + CTRL : Activates Drag&Drop\n" +
//            "    Press ENTER : Plays item\n" +
//            "    Press ESC : Clear selection & filter\n" +
//            "    Type : Searches for item - applies filter\n" +
            "    Scroll : Scroll table vertically\n" +
            "    Scroll + SHIFT : Scroll table horizontally\n" +
            "    Drag column : Changes column order\n" +
            "    Click column : Changes sort order - ascending,\n" +
            "                   descending, none\n" +
            "    Click column + SHIFT : Sorts by multiple columns\n" +
            "    Menu bar : Opens additional actions\n",
    notes = "",
    version = "0.1",
    year = "2014",
    group = LIBRARY
)
public class LibraryController extends FXMLController {
    
    @FXML AnchorPane root;
    TableView<Metadata> table = new TableView();
    private Subscription dbMonitor;

    @Override
    public void init() {
        root.getChildren().add(table);
        
        AnchorPane.setTopAnchor(table, 0d);
        AnchorPane.setRightAnchor(table, 0d);
        AnchorPane.setBottomAnchor(table, 30d);
        AnchorPane.setLeftAnchor(table, 0d);
        
        table.getSelectionModel().setSelectionMode(MULTIPLE);
        table.setFixedCellSize(GUI.font.getSize() + 5);
        
        // add index column
        TableColumn indexColumn = new TableColumn("#");
        indexColumn.setCellFactory( column -> 
            new TableCell() {
                {
                    // we want to align the index to the right, not left
                    setAlignment(CENTER_RIGHT);
                }
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) setText("");
                    else setText(String.valueOf(getIndex()+1)+ ".");
                }
            }
        );
        table.getColumns().add(indexColumn);
        
        // add data columns
        for(Field mf : Field.values()) {
            if(!mf.isStringRepresentable()) continue;
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
        
        Label progressL = new Label();
              progressL.setVisible(false);
        root.getChildren().add(progressL);
        AnchorPane.setBottomAnchor(progressL, 0d);
        AnchorPane.setRightAnchor(progressL, 0d);
        
        FadeButton b1 = new FadeButton(PLUS, 13);
                   b1.setOnMouseClicked( e -> {
                        DirectoryChooser fc = new DirectoryChooser();
                        File f = fc.showDialog(root.getScene().getWindow());
                        System.out.println("START");
                        List<Metadata> metas = FileUtil.getAudioFiles(f,111).stream()
                                .map(SimpleItem::new)
                                .map(SimpleItem::toMetadata)
                                .collect(Collectors.toList());

                        Task t = MetadataReader.readAaddMetadata(metas);
                        // display progress & hide on end
                        progressL.setVisible(true);
                        progressL.textProperty().bind(t.messageProperty());
                        EventHandler onEnd = event -> {
                            progressL.textProperty().unbind();
                            FxTimer.run(Duration.seconds(5), () -> progressL.setVisible(false));
                        };
                        t.setOnFailed(onEnd);
                        t.setOnSucceeded(onEnd);
                        
                       e.consume();
                   });
        Label infoL  = new Label();
        ListChangeListener<Metadata> infoUpdater = c -> {
//            while(c.next()) {
                List<? extends Metadata> content = c.getList().isEmpty() ? table.getItems() : c.getList();
                Duration du = content.stream().map(m -> (Duration)m.getLength()).reduce(ZERO, Duration::add);
                String prefix = c.getList().isEmpty() ? "All:" : "Selected:";
                       prefix += content.size()==1 ? " item " : " items ";
                infoL.setText(prefix + content.size() + " - " + Util.formatDuration(du));
//            }
        };
        table.getSelectionModel().getSelectedItems().addListener(infoUpdater);
        table.getItems().addListener(infoUpdater);
        
        HBox controls = new HBox(b1, infoL);
             controls.setSpacing(8);
        
        root.getChildren().add(controls);
        AnchorPane.setBottomAnchor(controls, 0d);
        AnchorPane.setLeftAnchor(controls, 0d);
        
        // listen for database changes to refresh library
//        dbMonitor = DB.librarychange.subscribe( nothing -> refresh());
        dbMonitor = DB.fieldSelectionChange.subscribe( c -> {
            change = c;
            refresh();
        });
    }
    
    DB.MetadataFieldSelection change;

    @Override
    public void refresh() {
        table.getSelectionModel().clearSelection();
        if(change!=null)
        table.setItems(FXCollections.observableArrayList(DB.getAllItemsWhere(change.field, change.value)));
    }

    @Override
    public void OnClosing() {
        // stop listening for db changes
        dbMonitor.unsubscribe();
    }
    
/****************************** CONTEXT MENU **********************************/
    
    private static final SingleInstance<ContentContextMenu<List<Metadata>>,TableView<Metadata>> contxt_menu = new SingleInstance<>(
        () -> {
            ContentContextMenu<List<Metadata>> contextMenu = new ContentContextMenu();
            MenuItem item00 = new MenuItem("Play items");        
                     item00.setOnAction(e -> {                     
                        List<PlaylistItem> to_play = new ArrayList();
                        contextMenu.getItem().stream().map(Metadata::toPlaylistItem).forEach(to_play::add);
                        PlaylistManager.playPlaylist(new Playlist(to_play));
                     });
            MenuItem item0 = new MenuItem("Enqueue items");        
                     item0.setOnAction(e -> {
                         List<Metadata> items = contextMenu.getItem();
                         PlaylistManager.addItems(items);
                     });
            MenuItem item1 = new MenuItem("Update from file");        
                     item1.setOnAction(e -> {
                         List<Metadata> items = contextMenu.getItem();
                         DB.updateItemsFromFile(items);
                     });
            MenuItem item2 = new MenuItem("Remove from library");        
                     item2.setOnAction(e -> {
                         List<Metadata> items = contextMenu.getItem();
                         DB.removeItems(items);
                     });
            MenuItem item3 = new MenuItem("Edit the item/s in tag editor");        
                     item3.setOnAction(e -> {
                         List<Metadata> items = contextMenu.getItem();
                         Widget w = WidgetManager.getWidget(TaggingFeature.class,FACTORY);
                         if (w!=null) {
                             TaggingFeature t = (TaggingFeature) w.getController();
                                            t.read(items);
                         }
                     });
            MenuItem item4 = new MenuItem("Explore items's directory");        
                     item4.setOnAction(e -> {
                         List<Metadata> items = contextMenu.getItem();
                         List<File> files = items.stream()
                                 .filter(Item::isFileBased)
                                 .map(Item::getLocation)
                                 .collect(Collectors.toList());
                         Enviroment.browse(files,true);
                     });

            contextMenu.getItems().addAll(item00, item0, item1, item2, item3, item4);
            contextMenu.setConsumeAutoHidingEvents(false);
            return contextMenu;
        },
        (menu,table) -> menu.setItem(Util.copySelectedItems(table))
    );
}