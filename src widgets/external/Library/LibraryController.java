
package Library;

import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.playlist.SimpleItem;
import AudioPlayer.services.Database.DB;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.Metadata.MetadataField;
import AudioPlayer.tagging.MetadataReader;
import GUI.GUI;
import GUI.objects.ContextMenu.ContentContextMenu;
import GUI.objects.FadeButton;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Features.TaggingFeature;
import Layout.Widgets.Widget;
import Layout.Widgets.WidgetInfo;
import Layout.Widgets.WidgetManager;
import static Layout.Widgets.WidgetManager.Widget_Source.FACTORY;
import de.jensd.fx.fontawesome.AwesomeIcon;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.MenuItem;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import org.reactfx.Subscription;
import utilities.Enviroment;
import utilities.FileUtil;

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
    group = Widget.Group.LIBRARY
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
                    setAlignment(Pos.CENTER_RIGHT);
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
        for(MetadataField mf : MetadataField.values()) {
            if(!mf.isStringRepresentable()) continue;
            TableColumn<Metadata,Object> c = new TableColumn(mf.toString());
            c.setCellValueFactory((TableColumn.CellDataFeatures<Metadata,Object> cf) -> {
                if(cf.getValue()==null) return null;
                return new ReadOnlyObjectWrapper(cf.getValue().getField(mf));
            });
            table.getColumns().add(c);
        }
        
        // context menu hide/show
        table.setOnMouseClicked( e -> {
            if(e.getButton()==PRIMARY) {
                if(e.getClickCount()==1) {
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
                getCM(table).show(table, e.getScreenX(), e.getScreenY());
        });
        // set key-induced actions
        table.setOnKeyReleased( e -> {
            if (e.getCode() == KeyCode.ENTER) {     // play first of the selected
                if(!table.getSelectionModel().isEmpty()) {
                    Playlist p = new Playlist(table.getItems().stream()
                            .map(Metadata::toPlaylistItem)
                            .collect(Collectors.toList()));
                    PlaylistManager.playPlaylistFrom(p, table.getSelectionModel().getSelectedIndex());
                }
            } else
            if (e.getCode() == KeyCode.DELETE) {    // delete selected
                DB.removeItems(table.getSelectionModel().getSelectedItems());
            } else
            if (e.getCode() == KeyCode.ESCAPE) {    // deselect
                table.getSelectionModel().clearSelection();
            }
        });
        
        
        FadeButton b1 = new FadeButton(AwesomeIcon.PLUS, 13);
                   b1.setOnMouseClicked( e -> {
                        DirectoryChooser fc = new DirectoryChooser();
                        File f = fc.showDialog(root.getScene().getWindow());
                        System.out.println("START");
                        List<Metadata> metas = FileUtil.getAudioFiles(f,111).stream()
                                .map(SimpleItem::new)
                                .map(SimpleItem::toMetadata)
                                .collect(Collectors.toList());

                        MetadataReader.readAaddMetadata(metas);                        
                       e.consume();
                   });
        root.getChildren().add(b1);
        AnchorPane.setBottomAnchor(b1, 0d);
        AnchorPane.setLeftAnchor(b1, 0d);
        
        
        // listen for database changes to refresh library
        dbMonitor = DB.librarychange.subscribe( nothing -> refresh());
    }

    @Override
    public void refresh() {
        table.getSelectionModel().clearSelection();
        table.setItems(FXCollections.observableArrayList(DB.getAllItems()));
    }

    @Override
    public void OnClosing() {
        // stop listening for db changes
        dbMonitor.unsubscribe();
    }
    
    
    
    
    
/****************************** HELPER METHODS ********************************/

    private static<T> List<T> getSelected(TableView<T> table_source) {
        return new ArrayList(table_source.getSelectionModel().getSelectedItems());
    }
    
/****************************** CONTEXT MENU **********************************/
    
    private static ContentContextMenu<List<Metadata>> cm;
    
    private static ContentContextMenu getCM(TableView<Metadata> t) {
        if(cm==null) cm = buildCM();
        // note: we need to create a copy of the list to avoid modification
        cm.setItem(getSelected(t));
        return cm;
    }
    
    private static ContentContextMenu buildCM() {
        final ContentContextMenu<List<Metadata>> contextMenu = new ContentContextMenu();
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
    }
}
