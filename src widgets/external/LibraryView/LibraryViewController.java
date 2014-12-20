
package LibraryView;

import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.services.Database.DB;
import AudioPlayer.tagging.Metadata;
import static AudioPlayer.tagging.Metadata.Field.CATEGORY;
import AudioPlayer.tagging.MetadataGroup;
import static AudioPlayer.tagging.MetadataGroup.Field.VALUE;
import Configuration.IsConfig;
import GUI.GUI;
import GUI.objects.ContextMenu.ContentContextMenu;
import GUI.objects.ContextMenu.TableContextMenuInstance;
import GUI.objects.Table.FilterableTable;
import GUI.objects.Table.ImprovedTable;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Features.TaggingFeature;
import static Layout.Widgets.Widget.Group.LIBRARY;
import Layout.Widgets.Widget.Info;
import Layout.Widgets.WidgetManager;
import static Layout.Widgets.WidgetManager.WidgetSource.NOLAYOUT;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import static javafx.geometry.NodeOrientation.INHERIT;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static javafx.scene.input.MouseEvent.MOUSE_RELEASED;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.reactfx.Subscription;
import org.reactfx.util.Tuples;
import util.Util;
import static util.Util.DEFAULT_ALIGNED_CELL_FACTORY;
import static util.Util.consumeOnSecondaryButton;
import static util.Util.createmenuItem;
import util.access.Accessor;
import static util.async.Async.run;

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
//            "    Item drag : \n" +
            "    Press ENTER : Plays item\n" +
            "    Press ESC : Clear selection & filter\n" +
//            "    Type : Searches for item - applies filter\n" +
            "    Scroll : Scroll table vertically\n" +
            "    Scroll + SHIFT : Scroll table horizontally\n" +
            "    Drag column : Changes column order\n" +
            "    Click column : Changes sort order - ascending,\n" +
            "                   descending, none\n" +
            "    Click column + SHIFT : Sorts by multiple columns\n",
    notes = "",
    version = "0.6",
    year = "2014",
    group = LIBRARY
)
public class LibraryViewController extends FXMLController {
    
    private @FXML AnchorPane root;
    private @FXML VBox content;
    private final FilterableTable<MetadataGroup,MetadataGroup.Field> table = new FilterableTable<>(VALUE);
    private Subscription dbMonitor;
    
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
    @IsConfig(name = "Field")
    public final Accessor<Metadata.Field> fieldFilter = new Accessor<>(CATEGORY, v -> {
        table.getSelectionModel().clearSelection();
        table.getColumns().removeAll(table.getColumns().subList(1, table.getColumns().size()));
        
        // get new data
        List<MetadataGroup> result = DB.getAllGroups(v);
        // reconstruct columns
        if (table.getColumns().size() <= 1) {
            for(MetadataGroup.Field f : MetadataGroup.Field.values()) {
                String name = f.toString(v);
                TableColumn<MetadataGroup,Object> c = new TableColumn(name);
                c.setCellValueFactory( cf -> {
                    if(cf.getValue()==null) return null;
                    return new ReadOnlyObjectWrapper(cf.getValue().getField(f));
                });
                Callback cellfactory = f==f.VALUE ? DEFAULT_ALIGNED_CELL_FACTORY(f.getType(v),"<none>")
                                                  : DEFAULT_ALIGNED_CELL_FACTORY(f.getType(v), null);
                c.setCellFactory(cellfactory);
                table.getColumns().add(c);
            }
        }
        
        table.setItemsRaw(FXCollections.observableArrayList(result));
        
//        // unfortunately the table cells dont get updated for some reason, resizing
//        // table or column manually with cursor will do the job, so we invoke that
//        // action programmatically, with a delay (or it wont work)
        run(100, ()->{
            TableColumn c = table.getColumns().get(table.getColumns().size()-1);
            table.columnResizePolicyProperty().get().call(new TableView.ResizeFeatures(table, c, c.getWidth()));
        });
        
        table.getSearchBox().setPrefTypeSupplier(() -> Tuples.t(VALUE.toString(v), VALUE.getType(v), VALUE));
        table.getSearchBox().setData(Arrays.asList(MetadataGroup.Field.values()).stream()
                .map(mgf->Tuples.t(mgf.toString(v),mgf.getType(v),mgf)).collect(Collectors.toList()));
    });

    @Override
    public void init() {
        content.getChildren().addAll(table.getRoot());
        VBox.setVgrow(table.getRoot(), Priority.ALWAYS);
        
        table.setFixedCellSize(GUI.font.getValue().getSize() + 5);
        table.getSelectionModel().setSelectionMode(MULTIPLE);
        
        // add index column
        TableColumn indexColumn = Util.createIndexColumn("#");
        table.getColumns().add(indexColumn);
        
        // context menu
        table.setOnMouseClicked( e -> {
            if (e.getY()<table.getFixedCellSize()) return;
            if(e.getButton()==PRIMARY) {
                if(e.getClickCount()==2)
                    play(table.getSelectedItemsCopy());
            } else
            if(e.getButton()==SECONDARY)
                contxt_menu.show(table, e);
        });
        
        // key actions
        table.setOnKeyReleased( e -> {
            if (e.getCode() == ENTER)     // play first of the selected
                play(table.getSelectedItems());
            else if (e.getCode() == ESCAPE)    // deselect
                table.getSelectionModel().clearSelection();
        });
        
        // listen for database changes to refresh library
        dbMonitor = DB.librarychange.subscribe( nothing -> fieldFilter.applyValue());
        
        table.getSelectionModel().selectedItemProperty().addListener( (o,ov,nv) -> {
            if(nv!=null)
                DB.fieldSelectionChange.push(fieldFilter.getValue(),nv.getValue());
        });
        
        // prevent scrol event to propagate up
        root.setOnScroll(Event::consume);
        
        // prevent overly eager selection change
        table.addEventFilter(MOUSE_PRESSED, consumeOnSecondaryButton);
        table.addEventFilter(MOUSE_RELEASED, consumeOnSecondaryButton);
    }

    @Override
    public void refresh() {
        fieldFilter.applyValue();
    }

    @Override
    public void close() {
        // stop listening for db changes
        dbMonitor.unsubscribe();
    }
    
    
/******************************** PUBLIC API **********************************/
    
/******************************** CONTEXT MENU ********************************/
    
    private static final TableContextMenuInstance<MetadataGroup> contxt_menu = new TableContextMenuInstance<>(
        () -> {
            ContentContextMenu<List<MetadataGroup>> m = new ContentContextMenu();
            m.getItems().addAll(
                createmenuItem("Play items", e -> play(m.getValue())),
                createmenuItem("Enqueue items", e -> PlaylistManager.addItems(dbFetch(m.getValue()))),
                createmenuItem("Update from file", e -> DB.updateItemsFromFile(dbFetch(m.getValue()))),
                createmenuItem("Remove from library", e -> DB.removeItems(dbFetch(m.getValue()))),
                createmenuItem("Edit the item/s in tag editor", e -> WidgetManager.use(TaggingFeature.class, NOLAYOUT,w->w.read(dbFetch(m.getValue())))));
            return m;
        },
        (menu,table) -> menu.setValue(ImprovedTable.class.cast(table).getSelectedItemsCopy())
    );
    
    private static List<Metadata> dbFetch(List<MetadataGroup> filters) {
        return DB.getAllItemsWhere(filters.get(0).getField(), filters.get(0).getValue());
    }
    private static void play(List<MetadataGroup> filters) {
        if(filters.isEmpty()) return;
        List<PlaylistItem> to_play = new ArrayList();
        dbFetch(filters).stream().map(Metadata::toPlaylistItem).forEach(to_play::add);
        PlaylistManager.playPlaylist(new Playlist(to_play));
    }
}