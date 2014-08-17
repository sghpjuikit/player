
package LibraryFilter;

import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.services.Database.DB;
import AudioPlayer.services.Database.POJO.MetadataGroup;
import AudioPlayer.tagging.Metadata;
import Configuration.IsConfig;
import GUI.GUI;
import GUI.objects.ContextMenu.ContentContextMenu;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Features.TaggingFeature;
import Layout.Widgets.Widget;
import Layout.Widgets.WidgetManager;
import static Layout.Widgets.WidgetManager.Widget_Source.FACTORY;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import static javafx.scene.input.KeyCode.ESCAPE;
import javafx.scene.layout.AnchorPane;
import org.reactfx.Subscription;
import utilities.Enviroment;
import utilities.SingleInstance;
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
public class LibraryFilterController extends FXMLController {
    
    @FXML AnchorPane root;
    TableView<MetadataGroup> table = new TableView();
    private Subscription dbMonitor;
    @IsConfig(name = "Field")
    Metadata.Field fieldFilter = Metadata.Field.CATEGORY;

    @Override
    public void init() {
        root.getChildren().add(table);
        
        AnchorPane.setTopAnchor(table, 0d);
        AnchorPane.setRightAnchor(table, 0d);
        AnchorPane.setBottomAnchor(table, 30d);
        AnchorPane.setLeftAnchor(table, 0d);
        
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
                DB.fieldSelectionChange.push(new DB.MetadataFieldSelection(fieldFilter,nv.getValue()));
        });
        DB.fieldSelectionChange.subscribe( ms -> System.out.println(ms.value));
    }

    @Override
    public void refresh() {
        table.getSelectionModel().clearSelection();
        table.getColumns().removeAll(table.getColumns().subList(1, table.getColumns().size()));
        
        List<MetadataGroup> result = DB.getAllGroups(fieldFilter);
        
        if (table.getColumns().size() <= 1) {
            MetadataGroup oo = result.get(0);
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
//
//package LibraryFilter;
//
//import AudioPlayer.playlist.Item;
//import AudioPlayer.playlist.Playlist;
//import AudioPlayer.playlist.PlaylistItem;
//import AudioPlayer.playlist.PlaylistManager;
//import AudioPlayer.services.Database.DB;
//import AudioPlayer.tagging.Metadata;
//import GUI.GUI;
//import GUI.objects.ContextMenu.ContentContextMenu;
//import Layout.Widgets.FXMLController;
//import Layout.Widgets.Features.TaggingFeature;
//import Layout.Widgets.Widget;
//import Layout.Widgets.WidgetManager;
//import static Layout.Widgets.WidgetManager.Widget_Source.FACTORY;
//import java.io.File;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.stream.Collectors;
//import javafx.beans.property.ReadOnlyObjectWrapper;
//import javafx.collections.FXCollections;
//import javafx.fxml.FXML;
//import javafx.geometry.Pos;
//import javafx.scene.control.MenuItem;
//import static javafx.scene.control.SelectionMode.MULTIPLE;
//import javafx.scene.control.TableCell;
//import javafx.scene.control.TableColumn;
//import javafx.scene.control.TableView;
//import static javafx.scene.input.KeyCode.ESCAPE;
//import javafx.scene.layout.AnchorPane;
//import org.reactfx.Subscription;
//import utilities.Enviroment;
//import utilities.SingleInstance;
//import utilities.Util;
//
///**
// *
// * @author Plutonium_
// */
////@WidgetInfo(
////    author = "Martin Polakovic",
////    programmer = "Martin Polakovic",
////    name = "Library",
////    description = "Provides access to database.",
////    howto = "Available actions:\n" +
////            "    Item left click : Selects item\n" +
////            "    Item right click : Opens context menu\n" +
////            // not yet implemented
//////            "    Item double click : Plays item\n" +
//////            "    Item drag : Moves item within playlist\n" +
//////            "    Item drag + CTRL : Activates Drag&Drop\n" +
//////            "    Press ENTER : Plays item\n" +
//////            "    Press ESC : Clear selection & filter\n" +
//////            "    Type : Searches for item - applies filter\n" +
////            "    Scroll : Scroll table vertically\n" +
////            "    Scroll + SHIFT : Scroll table horizontally\n" +
////            "    Drag column : Changes column order\n" +
////            "    Click column : Changes sort order - ascending,\n" +
////            "                   descending, none\n" +
////            "    Click column + SHIFT : Sorts by multiple columns\n" +
////            "    Menu bar : Opens additional actions\n",
////    notes = "",
////    version = "0.1",
////    year = "2014",
////    group = LIBRARY
////)
//public class LibraryFilterController extends FXMLController {
//    
//    @FXML AnchorPane root;
//    TableView<Object[]> table = new TableView();
//    private Subscription dbMonitor;
//
//    @Override
//    public void init() {
//        root.getChildren().add(table);
//        
//        AnchorPane.setTopAnchor(table, 0d);
//        AnchorPane.setRightAnchor(table, 0d);
//        AnchorPane.setBottomAnchor(table, 30d);
//        AnchorPane.setLeftAnchor(table, 0d);
//        
//        table.getSelectionModel().setSelectionMode(MULTIPLE);
//        table.setFixedCellSize(GUI.font.getSize() + 5);
//        
//        // add index column
//        TableColumn indexColumn = Util.createIndexColumn("#");
//        table.getColumns().add(indexColumn);
//        
//        
////        // context menu hide/show
////        table.setOnMouseClicked( e -> {
////            if(e.getButton()==PRIMARY) {
////                if(e.getClickCount()==1) {
////                    // hide CM on left click
////                    ContentContextMenu cm = contxt_menu.get();
////                    if(cm!=null && cm.isShowing())
////                        cm.hide();
////                } else {
////                    Playlist p = new Playlist(table.getItems().stream()
////                            .map(Metadata::toPlaylistItem)
////                            .collect(Collectors.toList()));
////                    PlaylistManager.playPlaylistFrom(p, table.getSelectionModel().getSelectedIndex());
////                }
////            } else
////            if(e.getButton()==SECONDARY)
////                // show CM on right click
////                contxt_menu.get(table).show(table, e.getScreenX(), e.getScreenY());
////        });
//        
//        // set key-induced actions
//        table.setOnKeyReleased( e -> {
//            if (e.getCode() == ESCAPE) {    // deselect
//                table.getSelectionModel().clearSelection();
//                e.consume();
//            }
//        });
//        
//        // listen for database changes to refresh library
//        dbMonitor = DB.librarychange.subscribe( nothing -> refresh());
//        
//        table.getSelectionModel().selectedItemProperty().addListener( (o,ov,nv) -> {
//            DB.fieldSelectionChange.push(new DB.MetadataFieldSelection(Metadata.Field.ARTIST,nv[0]));
//        });
//        DB.fieldSelectionChange.subscribe( ms -> System.out.println(ms.value));
//    }
//
//    @Override
//    public void refresh() {
//        table.getSelectionModel().clearSelection();
//        
//        List<Object[]> result = DB.getAllGroups(Metadata.Field.ARTIST);
//        
//        if (table.getColumns().size() <= 1) {
//            Object[] oo = result.get(0);
//            for(int i=0; i<oo.length; i++) {
//                final int j = i;
//                System.out.println(oo[j].getClass());
//                TableColumn<Object[],Object> c = new TableColumn(oo[j].toString());
//                c.setCellValueFactory((TableColumn.CellDataFeatures<Object[],Object> cf) -> {
//                    if(cf.getValue()==null) return null;
//                    return new ReadOnlyObjectWrapper(cf.getValue()[j]);
//                });
//                Pos al = (oo[j].getClass().equals(String.class)) ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT;
//                c.setCellFactory( column -> {
//                    TableCell cell = TableColumn.DEFAULT_CELL_FACTORY.call(column);
//                              cell.setAlignment(al);
//                    return cell;
//                });
//                table.getColumns().add(c);
//            }
//        }
//        table.setItems(FXCollections.observableArrayList(result));
//    }
//
//    @Override
//    public void OnClosing() {
//        // stop listening for db changes
//        dbMonitor.unsubscribe();
//    }
//    
///****************************** CONTEXT MENU **********************************/
//    
//    private static final SingleInstance<ContentContextMenu<List<Metadata>>,TableView<Metadata>> contxt_menu = new SingleInstance<>(
//        () -> {
//            ContentContextMenu<List<Metadata>> contextMenu = new ContentContextMenu();
//            MenuItem item00 = new MenuItem("Play items");        
//                     item00.setOnAction(e -> {                     
//                        List<PlaylistItem> to_play = new ArrayList();
//                        contextMenu.getItem().stream().map(Metadata::toPlaylistItem).forEach(to_play::add);
//                        PlaylistManager.playPlaylist(new Playlist(to_play));
//                     });
//            MenuItem item0 = new MenuItem("Enqueue items");        
//                     item0.setOnAction(e -> {
//                         List<Metadata> items = contextMenu.getItem();
//                         PlaylistManager.addItems(items);
//                     });
//            MenuItem item1 = new MenuItem("Update from file");        
//                     item1.setOnAction(e -> {
//                         List<Metadata> items = contextMenu.getItem();
//                         DB.updateItemsFromFile(items);
//                     });
//            MenuItem item2 = new MenuItem("Remove from library");        
//                     item2.setOnAction(e -> {
//                         List<Metadata> items = contextMenu.getItem();
//                         DB.removeItems(items);
//                     });
//            MenuItem item3 = new MenuItem("Edit the item/s in tag editor");        
//                     item3.setOnAction(e -> {
//                         List<Metadata> items = contextMenu.getItem();
//                         Widget w = WidgetManager.getWidget(TaggingFeature.class,FACTORY);
//                         if (w!=null) {
//                             TaggingFeature t = (TaggingFeature) w.getController();
//                                            t.read(items);
//                         }
//                     });
//            MenuItem item4 = new MenuItem("Explore items's directory");        
//                     item4.setOnAction(e -> {
//                         List<Metadata> items = contextMenu.getItem();
//                         List<File> files = items.stream()
//                                 .filter(Item::isFileBased)
//                                 .map(Item::getLocation)
//                                 .collect(Collectors.toList());
//                         Enviroment.browse(files,true);
//                     });
//
//            contextMenu.getItems().addAll(item00, item0, item1, item2, item3, item4);
//            contextMenu.setConsumeAutoHidingEvents(false);
//            return contextMenu;
//        },
//        (menu,table) -> menu.setItem(Util.copySelectedItems(table))
//    );
//}