
package LibraryView;

import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.services.Database.DB;
import AudioPlayer.services.Database.POJO.MetadataGroup;
import AudioPlayer.tagging.Metadata;
import static AudioPlayer.tagging.Metadata.Field.CATEGORY;
import Configuration.IsConfig;
import GUI.GUI;
import GUI.objects.ContextMenu.ContentContextMenu;
import GUI.objects.ContextMenu.TableContextMenuInstance;
import GUI.objects.Filter;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Features.TaggingFeature;
import Layout.Widgets.WidgetManager;
import static Layout.Widgets.WidgetManager.WidgetSource.NOLAYOUT;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import javafx.scene.layout.AnchorPane;
import org.reactfx.Subscription;
import utilities.FxTimer;
import utilities.Util;
import static utilities.Util.createmenuItem;
import utilities.access.Accessor;

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
    
    private @FXML AnchorPane root;
    private final Filter searchBox = new Filter();
    private final TableView<MetadataGroup> table = new TableView();
    private final ObservableList<MetadataGroup> allitems = FXCollections.observableArrayList();
    private final FilteredList<MetadataGroup> filtereditems = new FilteredList(allitems);
    private final SortedList<MetadataGroup> sortedItems = new SortedList<>(filtereditems);
    private Subscription dbMonitor;
    
    // configurables
    @IsConfig(name = "Field")
    public final Accessor<Metadata.Field> fieldFilter = new Accessor<>(CATEGORY, v -> {
        table.getSelectionModel().clearSelection();
        table.getColumns().removeAll(table.getColumns().subList(1, table.getColumns().size()));
        table.setItems(sortedItems);
        sortedItems.comparatorProperty().bind(table.comparatorProperty());
        
        // get new data
        List<MetadataGroup> result = DB.getAllGroups(v);
        // reconstruct columns
        if (table.getColumns().size() <= 1) {
            for(MetadataGroup.Field field : MetadataGroup.Field.values()) {
                String name = field.toString(v);
                TableColumn<MetadataGroup,Object> c = new TableColumn(name);
                c.setCellValueFactory( cf -> {
                    if(cf.getValue()==null) return null;
                    return new ReadOnlyObjectWrapper(cf.getValue().getField(field));
                });
                c.setCellFactory(Util.DEFAULT_ALIGNED_CELL_FACTORY(field.getType(v)));
                table.getColumns().add(c);
            }
        }
        
        allitems.setAll(FXCollections.observableArrayList(result));
        
        // unfortunately the table cells dont get updated for some reason, resizing
        // table or column manually with cursor will do the job, so we invoke that
        // action programmatically, with a delay (or it wont work)
        FxTimer.run(100, ()->{
            TableColumn c = table.getColumns().get(table.getColumns().size()-1);
            table.columnResizePolicyProperty().get().call(new TableView.ResizeFeatures(table, c, c.getWidth()));
        });
    });
    
    public final Accessor<MetadataGroup.Field> columnFilter = new Accessor<>(MetadataGroup.Field.VALUE, v -> {
        searchBox.setOnFilterChange( f -> filtereditems.setPredicate(m -> f.test(m.getField(v))));
        searchBox.setClass(v.getType(fieldFilter.getValue()));
    });

    @Override
    public void init() {
        root.getChildren().add(table);
        Util.setAPAnchors(table, 25,0,0,0);
        
        table.getSelectionModel().setSelectionMode(MULTIPLE);
        table.setFixedCellSize(GUI.font.getValue().getSize() + 5);
        
        // add index column
        TableColumn indexColumn = Util.createIndexColumn("#");
        table.getColumns().add(indexColumn);
        
        // context menu
        table.setOnMouseClicked( e -> {
            if(e.getButton()==PRIMARY) {
                if(e.getClickCount()==2)
                    play(Util.copySelectedItems(table));
            } else
            if(e.getButton()==SECONDARY)
                contxt_menu.show(table, e);
        });
        
        // key actions
        table.setOnKeyReleased( e -> {
            if (e.getCode() == ESCAPE) {    // deselect
                table.getSelectionModel().clearSelection();
                e.consume();
            }
        });
        
        // listen for database changes to refresh library
        dbMonitor = DB.librarychange.subscribe( nothing -> fieldFilter.applyValue());
        
        table.getSelectionModel().selectedItemProperty().addListener( (o,ov,nv) -> {
            if(nv!=null)
                DB.fieldSelectionChange.push(fieldFilter.getValue(),nv.getValue());
        });
        
        // prevent scrol event to propagate up
        root.setOnScroll(Event::consume);
                
        searchBox.setPrefHeight(25);
        root.getChildren().add(searchBox);
        AnchorPane.setTopAnchor(searchBox, 0d);
        AnchorPane.setRightAnchor(searchBox, 0d);
        AnchorPane.setLeftAnchor(searchBox, 0d);

        // column filter criteria combo box, populate box with enum values
        ComboBox<MetadataGroup.Field> fieldType = new ComboBox(FXCollections.observableArrayList(MetadataGroup.Field.values()));
        // use dynamic toString() when populating combobox values
        fieldType.setCellFactory( view -> {
            return new ListCell<MetadataGroup.Field>(){
                @Override
                protected void updateItem(MetadataGroup.Field item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item.toString(fieldFilter.getValue()));
                }
            };
        });
        // fire filter changes when value changes
        fieldType.valueProperty().addListener((o,ov,nv) -> columnFilter.setNapplyValue(nv));
        // initial value & fire first change
        fieldType.setValue(MetadataGroup.Field.VALUE);
        searchBox.getChildren().add(0, fieldType);
    }

    @Override
    public void refresh() {
        fieldFilter.applyValue();
        columnFilter.applyValue();
    }

    @Override
    public void close() {
        // stop listening for db changes
        dbMonitor.unsubscribe();
    }
    
    
/******************************** PUBLIC API **********************************/
    
    int id = 1;
    int listening_to = 0;
    
    
    
    
    
    private static final TableContextMenuInstance<MetadataGroup> contxt_menu = new TableContextMenuInstance<>(
        () -> {
            ContentContextMenu<List<MetadataGroup>> m = new ContentContextMenu();
            m.getItems().addAll(
                createmenuItem("Play items", e -> {                     
                    play(m.getValue());
                }),
                createmenuItem("Enqueue items", e -> {
                    PlaylistManager.addItems(dbFetch(m.getValue()));
                }),
                createmenuItem("Update from file", e -> {
                    DB.updateItemsFromFile(dbFetch(m.getValue()));
                }),
                createmenuItem("Remove from library", e -> {
                    DB.removeItems(dbFetch(m.getValue()));
                }),
                createmenuItem("Edit the item/s in tag editor", e -> {
                    WidgetManager.use(TaggingFeature.class, NOLAYOUT,w->w.read(dbFetch(m.getValue())));
                })
               );
            return m;
        },
        (menu,table) -> menu.setValue(Util.copySelectedItems(table))
    );
    
    private static List<Metadata> dbFetch(List<MetadataGroup> filters) {
        return DB.getAllItemsWhere(filters.get(0).getField(), filters.get(0).getValue());
    }
    private static void play(List<MetadataGroup> filters) {
        List<PlaylistItem> to_play = new ArrayList();
        dbFetch(filters).stream().map(Metadata::toPlaylistItem).forEach(to_play::add);
        PlaylistManager.playPlaylist(new Playlist(to_play));
    }
}