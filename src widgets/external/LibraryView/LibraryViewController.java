
package LibraryView;

import AudioPlayer.services.Database.DB;
import AudioPlayer.services.Database.POJO.MetadataGroup;
import AudioPlayer.tagging.Metadata;
import Configuration.IsConfig;
import GUI.GUI;
import Layout.Widgets.FXMLController;
import java.util.List;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import static javafx.scene.input.KeyCode.ESCAPE;
import javafx.scene.layout.AnchorPane;
import org.reactfx.Subscription;
import utilities.Util;

/**
 *
 * @author Plutonium_
 */
//@WidgetInfo(
//    author = "Martin Polakovic",
//    programmer = "Martin Polakovic",
//    name = "Library",
//    description = "Provides access to database.",
//    howto = "Available actions:\n" +
//            "    Item left click : Selects item\n" +
//            "    Item right click : Opens context menu\n" +
//            // not yet implemented
////            "    Item double click : Plays item\n" +
////            "    Item drag : Moves item within playlist\n" +
////            "    Item drag + CTRL : Activates Drag&Drop\n" +
////            "    Press ENTER : Plays item\n" +
////            "    Press ESC : Clear selection & filter\n" +
////            "    Type : Searches for item - applies filter\n" +
//            "    Scroll : Scroll table vertically\n" +
//            "    Scroll + SHIFT : Scroll table horizontally\n" +
//            "    Drag column : Changes column order\n" +
//            "    Click column : Changes sort order - ascending,\n" +
//            "                   descending, none\n" +
//            "    Click column + SHIFT : Sorts by multiple columns\n" +
//            "    Menu bar : Opens additional actions\n",
//    notes = "",
//    version = "0.1",
//    year = "2014",
//    group = LIBRARY
//)
public class LibraryViewController extends FXMLController {
    
    @FXML AnchorPane root;
    TableView<MetadataGroup> table = new TableView();
    private Subscription dbMonitor;
    @IsConfig(name = "Field")
    Metadata.Field fieldFilter = Metadata.Field.CATEGORY;

    @Override
    public void init() {
        root.getChildren().add(table);
        Util.setAPAnchors(table, 0);
        
//        table.getSelectionModel().setSelectionMode(MULTIPLE);
        table.setFixedCellSize(GUI.font.getSize() + 5);
        
        // add index column
        TableColumn indexColumn = Util.createIndexColumn("#");
        table.getColumns().add(indexColumn);
        
//        // context menu hide/show
//        table.setOnMouseClicked( e -> {
//            if(e.getButton()==PRIMARY) {
//                if(e.getClickCount()==1) {
//                    // hide CM on left click
//                    ContentContextMenu cm = contxt_menu.get();
//                    if(cm!=null && cm.isShowing())
//                        cm.hide();
//                } else {
//                    Playlist p = new Playlist(table.getItems().stream()
//                            .map(Metadata::toPlaylistItem)
//                            .collect(Collectors.toList()));
//                    PlaylistManager.playPlaylistFrom(p, table.getSelectionModel().getSelectedIndex());
//                }
//            } else
//            if(e.getButton()==SECONDARY)
//                // show CM on right click
//                contxt_menu.get(table).show(table, e.getScreenX(), e.getScreenY());
//        });
        
        // set key-induced actions
        table.setOnKeyReleased( e -> {
            if (e.getCode() == ESCAPE) {    // deselect
                table.getSelectionModel().clearSelection();
                e.consume();
            }
        });
        
        // listen for database changes to refresh library
        dbMonitor = DB.librarychange.subscribe( nothing -> refresh());
        
        table.getSelectionModel().selectedItemProperty().addListener( (o,ov,nv) -> {
            if(nv!=null)
                DB.fieldSelectionChange.push(fieldFilter,nv.getValue());
        });
        
        // prevent scrol event to propagate up
        root.setOnScroll(Event::consume);
    }

    @Override
    public void refresh() {
        table.getSelectionModel().clearSelection();
        table.getColumns().removeAll(table.getColumns().subList(1, table.getColumns().size()));
        
        List<MetadataGroup> result = DB.getAllGroups(fieldFilter);
        
        if (table.getColumns().size() <= 1) {
            for(MetadataGroup.Field field : MetadataGroup.Field.values()) {
                String name = field.toString(fieldFilter);
                TableColumn<MetadataGroup,Object> c = new TableColumn(name);
                c.setCellValueFactory( cf -> {
                    if(cf.getValue()==null) return null;
                    return new ReadOnlyObjectWrapper(cf.getValue().getField(field));
                });
                c.setCellFactory(Util.DEFAULT_ALIGNED_CELL_FACTORY(field.getType(fieldFilter)));
                table.getColumns().add(c);
            }
        }
        table.setItems(FXCollections.observableArrayList(result));
    }

    @Override
    public void OnClosing() {
        // stop listening for db changes
        dbMonitor.unsubscribe();
    }
    
    
/******************************** PUBLIC API **********************************/
    
    int id = 1;
    int listening_to = 0;
}