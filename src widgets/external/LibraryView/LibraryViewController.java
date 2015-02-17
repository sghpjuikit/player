
package LibraryView;

import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.services.Database.DB;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.Metadata.Field;
import static AudioPlayer.tagging.Metadata.Field.CATEGORY;
import AudioPlayer.tagging.MetadataGroup;
import static AudioPlayer.tagging.MetadataGroup.Field.VALUE;
import Configuration.Config;
import Configuration.IsConfig;
import GUI.GUI;
import GUI.objects.ContextMenu.ContentContextMenu;
import GUI.objects.ContextMenu.TableContextMenuRInstance;
import GUI.objects.Table.FilteredTable;
import GUI.objects.Table.TableColumnInfo;
import GUI.objects.Table.TableColumnInfo.ColumnInfo;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Features.TaggingFeature;
import static Layout.Widgets.Widget.Group.LIBRARY;
import Layout.Widgets.Widget.Info;
import Layout.Widgets.WidgetManager;
import static Layout.Widgets.WidgetManager.WidgetSource.NOLAYOUT;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import static javafx.geometry.NodeOrientation.INHERIT;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import static javafx.scene.control.TableView.UNCONSTRAINED_RESIZE_POLICY;
import javafx.scene.input.ContextMenuEvent;
import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.reactfx.Subscription;
import static util.Util.*;
import util.access.Accessor;
import util.access.AccessorEnum;
import util.collections.Histogram;
import util.collections.TupleM4;
import static util.collections.Tuples.tuple;
import static util.functional.FunctUtil.*;
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
    private final FilteredTable<MetadataGroup,MetadataGroup.Field> table = new FilteredTable<>(VALUE);
    private Subscription dbMonitor;
    private final Runner runOnce = new Runner(1);
    private boolean lock = false;
    
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
        if(dbMonitor!=null) dbMonitor.unsubscribe();
        // listen for database changes to refresh library
        dbMonitor = DB.views.subscribe(v, (lvl,list) -> {
            // remember selected
            Set<Object> oldSel = table.getSelectedItems().stream()
                                      .map(MetadataGroup::getValue).collect(toSet());
            // prevent selection from propagating change
            lock = true;
            // update list
            setItems(list);
            // update selected - restore every available old one
            forEachIndexed(table.getItems(), (i,mg) -> {
                if(oldSel.contains(mg.getValue()))
                    table.getSelectionModel().select(i);
            });
            // enable propagation
            lock = false;
            // propagate in 1 event
            forwardItems(filerList(list));
        });
        // initialize
        setItems(DB.views.getValue(v));
        // store
        table.setUserData(v);
    });
    @IsConfig(name = "Field")
    public final AccessorEnum<Metadata.Field> fieldFilter = new AccessorEnum<Metadata.Field>(CATEGORY, v -> {
            // rebuild value column
            find(table.getColumns(), c -> VALUE == c.getUserData()).ifPresent(c -> {
                TableColumn<MetadataGroup,?> t = table.getColumnFactory().call(VALUE.toString());
                c.setText(t.getText());
                c.setCellFactory((Callback)t.getCellFactory());
                c.setCellValueFactory((Callback)t.getCellValueFactory());
            });
            // update filters
            table.getSearchBox().setPrefTypeSupplier(() -> tuple(VALUE.toString(v), VALUE.getType(v), VALUE));
            table.getSearchBox().setData(listM(MetadataGroup.Field.values(), mgf->tuple(mgf.toString(v),mgf.getType(v),mgf)));
            // repopulate
            setItems(DB.views.getValue(lvl.getValue()));
        },
        ()->list(Metadata.Field.values(), Field::isTypeStringRepresentable)
    );  
    

    @Override
    public void init() {
        content.getChildren().addAll(table.getRoot());
        VBox.setVgrow(table.getRoot(), Priority.ALWAYS);
        
        table.setFixedCellSize(GUI.font.getValue().getSize() + 5);
        table.getSelectionModel().setSelectionMode(MULTIPLE);
        
        // set up table columns
        table.setkeyNameColMapper(name-> "#".equals(name) ? name : MetadataGroup.Field.valueOfEnumString(name).toString());
        table.setColumnStateFacory( f -> {
            double w = f==VALUE ? 200 : 50;
            return new ColumnInfo(f.toString(), f.ordinal(), true, w);
        });
        table.setColumnFactory( f -> {
            Metadata.Field v = fieldFilter.getValue();
            TableColumn<MetadataGroup,?> c = new TableColumn(f.toString(v));
            c.setCellValueFactory( cf -> {
                if(cf.getValue()==null) return null;
                return new ReadOnlyObjectWrapper(cf.getValue().getField(f));
            });
            String no_val = f==VALUE ? "<none>" : "";
            c.setCellFactory(DEFAULT_ALIGNED_CELL_FACTORY(f.getType(v), no_val));
            c.setUserData(f);
            return c;
        });
        columnInfo = table.getDefaultColumnInfo();
        
        // context menu
        table.setOnMouseClicked( e -> {
            if (e.getY()<table.getTableHeaderHeight()) return;
            if(e.getButton()==PRIMARY) {
                if(e.getClickCount()==2)
                    play();
            } else
            if(e.getButton()==SECONDARY)
                contxt_menu.show(this,(TableView)table, e);
        });
        
        // key actions
        table.setOnKeyReleased( e -> {
            if (e.getCode() == ENTER)        // play first of the selected
                play();
            else if (e.getCode() == ESCAPE)         // deselect
                table.getSelectionModel().clearSelection();
            else if (e.isControlDown() && e.getCode() == L)  // layout columns
                resizeMainColumn();
        });
        
        // alleviates user from column resizing after layout changes
        table.setColumnResizePolicy(resize -> {
            boolean b = UNCONSTRAINED_RESIZE_POLICY.call(resize);
            resizeMainColumn();
            return b;
        });
        
        // send selection changed events, do not use InvalidationListener
        table.getSelectionModel().getSelectedItems().addListener(
                (Observable o) -> forwardItems(DB.views.getValue(lvl.getValue())));
        
        // prevent selection change on right click
        table.addEventFilter(MOUSE_PRESSED, consumeOnSecondaryButton);
        table.addEventFilter(MOUSE_RELEASED, consumeOnSecondaryButton);
        table.addEventFilter(MOUSE_CLICKED, consumeOnSecondaryButton);
        // prevent context menu changing selection despite the above
        table.addEventFilter(ContextMenuEvent.ANY, Event::consume);
        // prevent volume change
        table.setOnScroll(Event::consume);
    }

    
    @Override
    public void refresh() {
        runOnce.run(()->table.setColumnState(columnInfo));
        
        table_orient.applyValue();
        zeropad.applyValue();
        orig_index.applyValue();
        show_header.applyValue();
        show_menu_button.applyValue();
        fieldFilter.applyValue();
        // apply last as it loads data
        lvl.applyValue();
    }
    
    public void resizeMainColumn() {
        find(table.getColumns(),c -> VALUE == c.getUserData()).ifPresent(c->{
            double w = table.getColumns().stream().filter(TableColumn::isVisible).mapToDouble(TableColumn::getWidth).sum();
            double itemsHeight = (table.getItems().size()+1)*table.getFixedCellSize();
            double scrollbar = itemsHeight < table.getHeight() ? 0 : 15;
            c.setPrefWidth(table.getWidth()-(scrollbar+w-c.getWidth()));
        });
    }

    @Override
    public void close() {
        // stop listening for db changes
        dbMonitor.unsubscribe();
    }

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
    
    
/******************************** PRIVATE API **********************************/
    
    private final Histogram<Object, Metadata, TupleM4<Long,Set<String>,Double,Long>> h = new Histogram();
    
    /** populates metadata groups to table from metadata list */
    private void setItems(List<Metadata> list) {
        Field f = fieldFilter.getValue();
        // make histogram
        h.keyMapper = metadata -> metadata.getField(f);
        h.histogramFactory = () -> new TupleM4(0l,new HashSet(),0d,0l);
        h.elementAccumulator = (hist,metadata) -> {
            hist.a++;
            hist.b.add(metadata.getAlbum());
            hist.c += metadata.getLengthInMs();
            hist.d += metadata.getFilesizeInB();
        };
        h.clear();
        h.accumulate(list);
        // read histogram
        table.setItemsRaw(h.toList((value,s)->new MetadataGroup(f, value, s.a, s.b.size(), s.c, s.d)));
        // pass down the chain
        forwardItems(list);
    }
    
    /** Sends event to next level. */
    private void forwardItems(List<Metadata> list) {
        if(!lock)
            DB.views.push(lvl.getValue()+1, filerList(list));
    }
    
    private List<Metadata> filerList(List<Metadata> list) {
        List<Metadata> l;
        if(table.getSelectionModel().isEmpty()) {
            // no selection -> fetch everything, optimalization
            // same as filtering out nothing
            l = list;
        } else {
            Predicate<Metadata> p = table.getSelectedItems().stream()
                    .map(MetadataGroup::toMetadataPredicate)
                    .reduce(Predicate::or)
                    .orElse(isFALSE);
            l = list.stream().filter(p).collect(toList());
        }
        return l;
    }
    
/******************************** CONTEXT MENU ********************************/
    
    private static final TableContextMenuRInstance<Metadata, LibraryViewController> contxt_menu = new TableContextMenuRInstance<>(
        () -> {
            ContentContextMenu<List<Metadata>> m = new ContentContextMenu();
            
            m.getItems().addAll(
                createmenuItem("Play items", e -> play(m.getValue())),
                createmenuItem("Enqueue items", e -> PlaylistManager.addItems(m.getValue())),
                createmenuItem("Update from file", e -> DB.updateItemsFromFile(m.getValue())),
                createmenuItem("Remove from library", e -> DB.removeItems(m.getValue())),
                createmenuItem("Edit the item/s in tag editor", e -> WidgetManager.use(TaggingFeature.class, NOLAYOUT,w->w.read(m.getValue()))));
//                createmenuItem("Edit the item/s in tag editor", e -> m.getValue().re);
            return m;
        }, (menu, w) -> menu.setValue(w.filerList(DB.views.getValue(w.lvl.getValue()))));
    
    private static void play(List<Metadata> items) {
        if(items.isEmpty()) return;
        Playlist p = new Playlist();
        items.stream().sorted().map(Metadata::toPlaylistItem).forEach(p::addItem);
        PlaylistManager.playPlaylist(p);
    }
    
    private void play() {
        play(filerList(DB.views.getValue(lvl.getValue())));
    }
    
}