
package LibraryView;

import AudioPlayer.Player;
import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.services.Database.DB;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.Metadata.Field;
import static AudioPlayer.tagging.Metadata.Field.ALBUM;
import static AudioPlayer.tagging.Metadata.Field.CATEGORY;
import AudioPlayer.tagging.MetadataGroup;
import static AudioPlayer.tagging.MetadataGroup.Field.*;
import Configuration.Config;
import Configuration.IsConfig;
import GUI.GUI;
import GUI.objects.ActionChooser;
import GUI.objects.ContextMenu.ContentContextMenu;
import GUI.objects.ContextMenu.TableContextMenuRInstance;
import GUI.objects.Table.FilteredTable;
import GUI.objects.Table.TableColumnInfo;
import GUI.objects.Table.TableColumnInfo.ColumnInfo;
import GUI.objects.TableCell.NumberRatingCellFactory;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Features.TaggingFeature;
import static Layout.Widgets.Widget.Group.LIBRARY;
import Layout.Widgets.Widget.Info;
import Layout.Widgets.WidgetManager;
import static Layout.Widgets.WidgetManager.WidgetSource.NOLAYOUT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.SQUARE_ALT;
import java.util.*;
import static java.util.Collections.EMPTY_LIST;
import java.util.function.Predicate;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static javafx.application.Platform.runLater;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import static javafx.geometry.NodeOrientation.INHERIT;
import javafx.scene.Node;
import javafx.scene.control.*;
import static javafx.scene.control.ContentDisplay.CENTER;
import static javafx.scene.control.SelectionMode.MULTIPLE;
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
import main.App;
import org.reactfx.Subscription;
import util.File.Enviroment;
import static util.Util.*;
import util.access.Accessor;
import util.access.AccessorEnum;
import util.async.Async;
import util.collections.Histogram;
import util.collections.ListCacheMap;
import util.collections.TupleM6;
import static util.collections.Tuples.tuple;
import util.functional.Runner;
import static util.functional.Util.*;
import util.parsing.Parser;
import web.HttpSearchQueryBuilder;

@Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Library View",
    description = "Provides database filtering.",
    howto = "Available actions:\n" +
            "    Item left click : Selects item\n" +
            "    Item right click : Opens context menu\n" +
            "    Item double click : Plays item\n" +
            "    Type : search & filter\n" +
            "    Press ENTER : Plays item\n" +
            "    Press ESC : Clear selection & filter\n" +
            "    Scroll : Scroll table vertically\n" +
            "    Scroll + SHIFT : Scroll table horizontally\n" +
            "    Column drag : swap columns\n" +
            "    Column right click: show column menu\n" +
            "    Click column : Sort - ascending | descending | none\n" +
            "    Click column + SHIFT : Sorts by multiple columns\n",
    notes = "",
    version = "1",
    year = "2015",
    group = LIBRARY
)
public class LibraryViewController extends FXMLController {
    
    private @FXML AnchorPane root;
    private @FXML VBox content;
    private final FilteredTable<MetadataGroup,MetadataGroup.Field> table = new FilteredTable<>(VALUE);
    
    // dependencies
    private Subscription d1;
    
    private final Runner runOnce = new Runner(1);
    private boolean lock = false;
    ActionChooser actPane = new ActionChooser();
    Labeled lvlB = actPane.addIcon(SQUARE_ALT, "1", "Level", false);
    private ListCacheMap<Metadata,Object> cache = null;
    
    // configurables
    @IsConfig(name = "Table orientation", info = "Orientation of the table.")
    public final Accessor<NodeOrientation> table_orient = new Accessor<>(INHERIT, table::setNodeOrientation);
    @IsConfig(name = "Zeropad numbers", info = "Adds 0 to uphold number length consistency.")
    public final Accessor<Boolean> zeropad = new Accessor<>(true, table::setZeropadIndex);
    @IsConfig(name = "Search show original index", info = "Show index of the table items as in unfiltered state when filter applied.")
    public final Accessor<Boolean> orig_index = new Accessor<>(true, table::setShowOriginalIndex);
    @IsConfig(name = "Show table header", info = "Show table header with columns.")
    public final Accessor<Boolean> show_header = new Accessor<>(true, table::setHeaderVisible);
    @IsConfig(name = "Show table menu button", info = "Show table menu button for setting up columns.")
    public final Accessor<Boolean> show_menu_button = new Accessor<>(false, table::setTableMenuButtonVisible);
    @IsConfig(editable = false)
    private TableColumnInfo columnInfo;
    @IsConfig(name = "Library level", info = "", min=1, max = 8)
    public final Accessor<Integer> lvl = new Accessor<>(DB.views.getLastLvl()+1, v -> {
        // maintain info text
        lvlB.setText(v.toString());
        if(d1!=null) d1.unsubscribe();
        // listen for database changes to refresh library
        d1 = DB.views.subscribe(v, (lvl,list) -> {
            // remember selected
            Set oldSel = table.getSelectedItems().stream()
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
            forwardItems(filerList(list,true,false));
        });
        // initialize
        setItems(DB.views.getValue(v));
        // store
        table.setUserData(v);
    });
    @IsConfig(name = "Field")
    public final AccessorEnum<Metadata.Field> fieldFilter = new AccessorEnum<>(CATEGORY, v -> {
            // rebuild cache
            if(cache!=null) {
                cache.clear();
                cache.keyMapper = m -> m.getField(v);
            }
            // rebuild value column
            find(table.getColumns(), c -> VALUE == c.getUserData()).ifPresent(c -> {
                TableColumn<MetadataGroup,?> t = table.getColumnFactory().call(VALUE);
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
            double w = f==VALUE ? 250 : 70;
            return new ColumnInfo(f.toString(), f.ordinal(), f.isCommon(), w);
        });
        table.setColumnFactory( mgf -> {
            Metadata.Field mf = fieldFilter.getValue();
            TableColumn<MetadataGroup,?> c = new TableColumn(mgf.toString(mf));
            c.setCellValueFactory( cf -> {
                if(cf.getValue()==null) return null;
                return new ReadOnlyObjectWrapper(cf.getValue().getField(mgf));
            });
            String no_val = mgf==VALUE ? "<none>" : "";
            c.setCellFactory(mgf==AVG_RATING 
                ? (Callback) App.ratingCell.getValue()
                : mgf==W_RATING ? (Callback)new NumberRatingCellFactory()
                : cellFactoryAligned(mgf.getType(mf), no_val));
            return c;
        });
        // maintain rating column cell style
        App.ratingCell.addListener((o,ov,nv) -> table.getColumn(AVG_RATING).ifPresent(c->c.setCellFactory((Callback)nv)));
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
        });
        
        // resizing
        table.setColumnResizePolicy(resize -> {
            boolean b = UNCONSTRAINED_RESIZE_POLICY.call(resize);
            // resize index column
            table.getColumn("#").ifPresent(i->i.setPrefWidth(table.calculateIndexColumnWidth()));
            // resize main column to span remaining space
            find(table.getColumns(),c -> VALUE == c.getUserData()).ifPresent(c->{
                double w = table.getColumns().stream().filter(TableColumn::isVisible).mapToDouble(TableColumn::getWidth).sum();
                double itemsHeight = (table.getItems().size()+1)*table.getFixedCellSize();
                double scrollbar = itemsHeight < table.getHeight() ? 0 : 15;
                c.setPrefWidth(table.getWidth()-(scrollbar+w-c.getWidth()));
            });
            return b;
        });
        
        // send selection changed events, do not use InvalidationListener
        
//        EventSource<Void> ses = new EventSource<>();
//        table.getSelectionModel().getSelectedItems().addListener((Observable o) -> ses.push(null));
//        ses.successionEnds(Duration.ofMillis(100)).subscribe(a -> forwardItems(DB.views.getValue(lvl.getValue())));
        
//        table.getSelectionModel().getSelectedItems().addListener(
//                (Observable o) -> forwardItems(DB.views.getValue(lvl.getValue())));
        
        table.getSelectionModel().getSelectedItems().addListener(
                (Observable o) -> runLater(()->forwardItems(DB.views.getValue(lvl.getValue()))));
//        EventStreams.changesOf(table.getItemsFiltered().predicateProperty()).successionEnds(Duration.ofMillis(200)).subscribe(e->forwardItems(DB.views.getValue(lvl.getValue())));
        
        // prevent selection change on right click
        table.addEventFilter(MOUSE_PRESSED, consumeOnSecondaryButton);
        table.addEventFilter(MOUSE_RELEASED, consumeOnSecondaryButton);
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
    
//    public void resizeMainColumn() {

//    }

    @Override
    public void close() {
        // stop listening for db changes
        d1.unsubscribe();
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
    
    private final Histogram<Object, Metadata, TupleM6<Long,Set<String>,Double,Long,Double,String>> h = new Histogram();
    
    /** populates metadata groups to table from metadata list */
    private void setItems(List<Metadata> list) {
        Async.runBgr(() -> {
            Field f = fieldFilter.getValue();
            // make histogram
            h.keyMapper = metadata -> metadata.getField(f);
            h.histogramFactory = () -> new TupleM6(0l,new HashSet(),0d,0l,0d,null);
            h.elementAccumulator = (hist,metadata) -> {
                hist.a++;
                hist.b.add(metadata.getAlbum());
                hist.c += metadata.getLengthInMs();
                hist.d += metadata.getFilesizeInB();
                hist.e += metadata.getRatingPercent();
                if(!"...".equals(hist.f) && !metadata.getYear().equals(hist.f))
                    hist.f = hist.f==null ? metadata.getYear() : "...";
            };
            h.clear();
            h.accumulate(list);
            // read histogram
            return h.toList((value,s)->new MetadataGroup(f, value, s.a, s.b.size(), s.c, s.d, s.e/s.a, s.f));
        }, l -> {
            table.setItemsRaw(l);
            forwardItems(list);
        });
        
//        Field f = fieldFilter.getValue();
//        // make histogram
//        h.keyMapper = metadata -> metadata.getField(f);
//        h.histogramFactory = () -> new TupleM6(0l,new HashSet(),0d,0l,0d,null);
//        h.elementAccumulator = (hist,metadata) -> {
//            hist.a++;
//            hist.b.add(metadata.getAlbum());
//            hist.c += metadata.getLengthInMs();
//            hist.d += metadata.getFilesizeInB();
//            hist.e += metadata.getRatingPercent();
//            if(!"...".equals(hist.f) && !metadata.getYear().equals(hist.f))
//                hist.f = hist.f==null ? metadata.getYear() : "...";
//        };
//        h.clear();
//        h.accumulate(list);
//        // read histogram
//        table.setItemsRaw(h.toList((value,s)->new MetadataGroup(f, value, s.a, s.b.size(), s.c, s.d, s.e/s.a, s.f)));
//        // pass down the chain
//        forwardItems(list);
    }
    
    /** Sends event to next level. */
    private void forwardItems(List<Metadata> list) {
        if(!lock)
            DB.views.push(lvl.getValue()+1, filerList(list,true,false));
    }
    
    private List<Metadata> filerList(List<Metadata> list, boolean orAll, boolean orEmpty) {
//        // use cache if needed
//        boolean needed = lvl.getValue()==1 && list.size()>5000;
//        if(lvl.getValue()==1) System.out.println("need " + needed);
//        // build cache if not yet
//        if(needed && cache==null) {System.out.println("building cache");
//            cache = new ListCacheMap<>(m -> m.getField(fieldFilter.getValue()));
//        }
//        // get rid of cache if not needed
//        if(!needed && cache!= null) {System.out.println("disposing cache");
//            cache.clear();
//            cache = null;
//        }
//        // accumulate cache if not yet
//        if(needed && cache.isEmpty()) {System.out.println("accumulating cache");
//            cache.accumulate(list);
//        }
//        
//        if(needed) {System.out.println("returning cached");
//            List<MetadataGroup> mgs = table.getSelectedOrAllItemsCopy();
//            Stream keys = mgs.stream().map(mg->mg.getValue());
//            return cache.getElementsOf(keys);
//        }

        // build filter
        List<MetadataGroup> mgs = orAll ? table.getSelectedOrAllItems() : table.getSelectedItems();
//        List<MetadataGroup> mgs = orAll ? table.getSelectedOrAllItemsCopy() : table.getSelectedItemsCopy();
        
        // optimisation : if empty, dont bother filtering
        if(mgs.isEmpty()) return orEmpty ? EMPTY_LIST : new ArrayList(list);
        
        // composed predicate, too much wasteful computation...
        // Predicate<Metadata> p = mgs.parallelStream()
        //        .map(MetadataGroup::toMetadataPredicate)
        //        .reduce(Predicate::or)
        //        .orElse(isFALSE);
        
        // optimisation : compute values ONCE if doable
        Field f = fieldFilter.getValue();
        List l = listM(mgs,mg->mg.getValue());
        Predicate<Metadata> p;
        // optimisation : if only 1, dont use list
        if(l.size()==1) {
            // optimisation : dont use equals for primitive types
            boolean primitive = f.getType().isPrimitive();
            Object v = l.get(0);
            p = primitive ? m -> m.getField(f)==v : m -> v.equals(m.getField(f));
        } else {
            // optimisation : dont use equals for primitive types
            boolean primitive = f.getType().isPrimitive();
            p = primitive ? m -> isInR(m.getField(f), l) : m -> isIn(m.getField(f), l);
        }
        
        // filter
        // optimisation : use parallel stream
        return list.parallelStream().filter(p).collect(toList());
    }
    
/******************************** CONTEXT MENU ********************************/
    
    private static Menu searchMenu;
    private static final TableContextMenuRInstance<Metadata, LibraryViewController> contxt_menu = new TableContextMenuRInstance<>(
        () -> {
            ContentContextMenu<List<Metadata>> m = new ContentContextMenu();
            MenuItem[] is = menuItems(App.plugins.getPlugins(HttpSearchQueryBuilder.class), 
                                      q -> "in " + Parser.toS(q),
                                      q -> Enviroment.browse(q.apply(m.getValue().get(0).getAlbum())));
            searchMenu = new Menu("Search album cover",null,is);
            m.getItems().addAll(
                menuItem("Play items", e -> play(m.getValue())),
                menuItem("Enqueue items", e -> PlaylistManager.addItems(m.getValue())),
                menuItem("Update from file", e -> Player.refreshItems(m.getValue())),
                menuItem("Remove from library", e -> DB.removeItems(m.getValue())),
                menuItem("Edit the item/s in tag editor", e -> WidgetManager.use(TaggingFeature.class, NOLAYOUT,w->w.read(m.getValue()))),
                searchMenu
            );
            return m;
        }, (menu, w) -> {
            List<Metadata> l = w.filerList(DB.views.getValue(w.lvl.getValue()),false,true);
                           l.sort(DB.library_sorter);
            menu.setValue(l);
            if(w.fieldFilter.getValue()==ALBUM && menu.getItems().size()==5)
                menu.getItems().add(searchMenu);
            if(w.fieldFilter.getValue()!=ALBUM && menu.getItems().size()==6)
                menu.getItems().remove(searchMenu);
        });
    
    private static void play(List<Metadata> items) {
        if(items.isEmpty()) return;
        Playlist p = new Playlist();
        items.stream().sorted(DB.library_sorter).map(Metadata::toPlaylist).forEach(p::addItem);
        PlaylistManager.playPlaylist(p);
    }
    
    private void play() {
        play(filerList(DB.views.getValue(lvl.getValue()),false,true));
    }
    
    

    {
        lvlB.setContentDisplay(CENTER);
        lvlB.setOnMouseClicked(e -> {
            if(e.getButton()==PRIMARY)
                lvl.setNapplyValue(clip(1,lvl.getValue()+1,8));
            if(e.getButton()==SECONDARY)
                lvl.setNapplyValue(clip(1,lvl.getValue()-1,8));
            e.consume();
        });
    }
    
    @Override
    public Node getActivityNode() {
        return actPane;
    }
}