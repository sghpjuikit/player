
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
import Layout.Widgets.FXMLController;
import Layout.Widgets.Features.SongReader;
import Layout.Widgets.Features.SongWriter;
import static Layout.Widgets.Widget.Group.LIBRARY;
import Layout.Widgets.Widget.Info;
import Layout.Widgets.WidgetManager;
import static Layout.Widgets.WidgetManager.WidgetSource.NO_LAYOUT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.SQUARE_ALT;
import gui.GUI;
import gui.objects.ActionChooser;
import gui.objects.ContextMenu.CheckMenuItem;
import gui.objects.ContextMenu.ImprovedContextMenu;
import gui.objects.ContextMenu.TableContextMenuRInstance;
import gui.objects.Icon;
import gui.objects.Table.FilteredTable;
import gui.objects.Table.ImprovedTable.PojoV;
import gui.objects.Table.TableColumnInfo;
import gui.objects.Table.TableColumnInfo.ColumnInfo;
import gui.objects.TableCell.NumberRatingCellFactory;
import gui.objects.TableRow.ImprovedTableRow;
import static java.time.Duration.ofMillis;
import java.time.Year;
import java.util.*;
import static java.util.Collections.EMPTY_LIST;
import java.util.function.Predicate;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import java.util.stream.Stream;
import static javafx.application.Platform.runLater;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import static javafx.geometry.NodeOrientation.INHERIT;
import javafx.geometry.Pos;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.geometry.Pos.CENTER_RIGHT;
import javafx.scene.Node;
import javafx.scene.control.*;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import static javafx.scene.control.TableView.UNCONSTRAINED_RESIZE_POLICY;
import javafx.scene.input.Dragboard;
import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.TransferMode.COPY;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import static javafx.stage.WindowEvent.WINDOW_SHOWN;
import javafx.util.Callback;
import main.App;
import org.reactfx.EventStreams;
import org.reactfx.Subscription;
import util.File.Environment;
import static util.Util.*;
import util.access.Accessor;
import util.access.AccessorEnum;
import static util.async.Async.runNew;
import util.async.executor.LimitedExecutor;
import util.async.future.Fut;
import util.collections.Histogram;
import util.collections.TupleM6;
import static util.collections.Tuples.tuple;
import static util.functional.Util.*;
import util.graphics.drag.DragUtil;
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
    ActionChooser actPane = new ActionChooser();
    Icon lvlB = actPane.addIcon(SQUARE_ALT, "1", "Level", true, false);
    
    // dependencies
    private Subscription d1,d2;
    private boolean isRefreshing = false;
    private final LimitedExecutor runOnce = new LimitedExecutor(1);
    
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
            selectionStore();
            // update list
            setItems(list);
            selectionReStore();
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
            // rebuild value column
            find(table.getColumns(), c -> VALUE == c.getUserData()).ifPresent(c -> {
                TableColumn<MetadataGroup,?> t = table.getColumnFactory().call(VALUE);
                c.setText(t.getText());
                c.setCellFactory((Callback)t.getCellFactory());
                c.setCellValueFactory((Callback)t.getCellValueFactory());
            });
            // update filters
            table.getSearchBox().setPrefTypeSupplier(() -> tuple(VALUE.toString(v), VALUE.getType(v), VALUE));
            table.getSearchBox().setData(map(MetadataGroup.Field.values(), mgf->tuple(mgf.toString(v),mgf.getType(v),mgf)));
            // repopulate
            if(!isRefreshing) setItems(DB.views.getValue(lvl.getValue()));
        },
        ()->filter(Metadata.Field.values(), Field::isTypeStringRepresentable)
    );  
    

    @Override
    public void init() {
        content.getChildren().addAll(table.getRoot());
        VBox.setVgrow(table.getRoot(), Priority.ALWAYS);
        
        table.setFixedCellSize(GUI.font.getValue().getSize() + 5);
        table.getSelectionModel().setSelectionMode(MULTIPLE);
        table.searchSetColumn(VALUE);
        
        // set up table columns
        table.setkeyNameColMapper(name-> "#".equals(name) ? name : MetadataGroup.Field.valueOfEnumString(name).toString());
        table.setColumnStateFacory(f -> {
            double w = f==VALUE ? 250 : 70;
            return new ColumnInfo(f.toString(), f.ordinal(), f.isCommon(), w);
        });
        table.setColumnFactory(f -> {
            Metadata.Field mf = fieldFilter.getValue();
            TableColumn<MetadataGroup,?> c = new TableColumn(f.toString(mf));
            c.setCellValueFactory( cf -> cf.getValue()==null ? null : new PojoV(cf.getValue().getField(f)));
            Pos a = f.getType(mf).equals(String.class) ? CENTER_LEFT : CENTER_RIGHT;
            c.setCellFactory(f==AVG_RATING 
                ? (Callback) App.ratingCell.getValue()
                : f==W_RATING 
                ? (Callback) new NumberRatingCellFactory()
                : (Callback) col -> { TableCell cel = table.buildDefaultCell(f); cel.setAlignment(a); return cel;}
            );
            return c;
        });
        // maintain rating column cell style
        App.ratingCell.addListener((o,ov,nv) -> table.getColumn(AVG_RATING).ifPresent(c->c.setCellFactory((Callback)nv)));
        columnInfo = table.getDefaultColumnInfo();
        
        // rows
        table.setRowFactory(tbl -> new ImprovedTableRow<MetadataGroup>()
                // additional css styleclasses
                .styleRuleAdd("played", mg -> Player.playingtem.get().getField(fieldFilter.getValue()).equals(mg.getValue()))
                // add behavior
                .onLeftDoubleClick((row,e) -> playSelected())
                .onRightSingleClick((row,e) -> {
                    // prep selection for context menu
                    if(!row.isSelected())
                        tbl.getSelectionModel().clearAndSelect(row.getIndex());
                    // show context menu
                    contxt_menu.show(this, (TableView)table, e);
                })
        );
        // maintain playing item css by refreshing column
        d2 = Player.playingtem.subscribeToChanges(o -> table.updateStyleRules());
        
        // column context menu - add change field submenus
        Menu m = (Menu)table.columnVisibleMenu.getItems().stream().filter(i->i.getText().equals("Value")).findFirst().get();
        Stream.of(Field.values())
              .map(f -> new CheckMenuItem(f.toStringEnum(), false){{
                  this.setOnMouseClicked(() -> {
                        if(!selected.get()) {
                            // refresh menu
                            m.getItems().forEach(mi -> ((CheckMenuItem)mi).selected.set(false));
                            selected.set(true);
                            // apply
                            fieldFilter.setNapplyValue(f);
                        }
                  });
              }})
              .forEach(m.getItems()::add);
            // refresh when menu opens
        table.columnVisibleMenu.addEventHandler(WINDOW_SHOWN, e -> m.getItems().forEach(mi -> ((CheckMenuItem)mi).selected.set(fieldFilter.getValue().toStringEnum().equals(mi.getText()))));
        
        // key actions
        table.setOnKeyPressed( e -> {
            if (e.getCode() == ENTER)        // play first of the selected
                playSelected();
            else if (e.getCode() == ESCAPE)         // deselect
                table.getSelectionModel().clearSelection();
        });
        
        // drag&drop from
        table.setOnDragDetected(e -> {
            if (e.getButton() == PRIMARY && !table.getSelectedItems().isEmpty() 
                    && table.isRowFull(table.getRowS(e.getSceneX(), e.getSceneY()))) {
                Dragboard db = table.startDragAndDrop(COPY);
                DragUtil.setItemList(filerListToSelectedNsort(),db);
            }
            e.consume();
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
        
        // forward on selection
        EventStreams.changesOf(table.getSelectedItems()).reduceSuccessions((a,b)->b,ofMillis(100)).subscribe(c -> {
            if(!sel_lock)
                forwardItems(DB.views.getValue(lvl.getValue()));
        });
        
        // prevent volume change
        table.setOnScroll(Event::consume);
    }
    
    @Override
    public void refresh() {
        isRefreshing = true;
        runOnce.execute(()->table.setColumnState(columnInfo));
        
        table_orient.applyValue();
        zeropad.applyValue();
        orig_index.applyValue();
        show_header.applyValue();
        show_menu_button.applyValue();
        fieldFilter.applyValue();
        lvl.applyValue();   // apply last
        isRefreshing = false;
    }

    @Override
    public void close() {
        // stop listening for db changes
        d1.unsubscribe();
        d2.unsubscribe();
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
    
    private final Histogram<Object, Metadata, TupleM6<Long,Set<String>,Double,Long,Double,Year>> h = new Histogram();
    
    /** populates metadata groups to table from metadata list */
    private void setItems(List<Metadata> list) {if(lvl.get()==2) System.out.println("SEEEEEEEEEEET");
        // doesnt work ?
        new Fut<>(fieldFilter.getValue())
            .use(f -> {
                // make histogram
                h.keyMapper = metadata -> metadata.getField(f);
                h.histogramFactory = () -> new TupleM6(0l,new HashSet(),0d,0l,0d,null);
                h.elementAccumulator = (hist,metadata) -> {
                    hist.a++;
                    hist.b.add(metadata.getAlbum());
                    hist.c += metadata.getLengthInMs();
                    hist.d += metadata.getFilesizeInB();
                    hist.e += metadata.getRatingPercent();
                    if(!Metadata.EMPTY.getYear().equals(hist.f) && !metadata.getYear().equals(hist.f))
                        hist.f = hist.f==null ? metadata.getYear() : Metadata.EMPTY.getYear();
                };
                h.clear();
                h.accumulate(list);
                // read histogram
                List<MetadataGroup> l = h.toList((value,s)->new MetadataGroup(f, value, s.a, s.b.size(), s.c, s.d, s.e/s.a, s.f));
                runLater(() -> {
                    selectionStore();
                    table.setItemsRaw(l);
                    selectionReStore();
                    runNew(() -> {
                        List<Metadata> ml = filerList(list,true,false);
                        runLater(() -> DB.views.push(lvl.getValue()+1, ml));
                    });
                });
            })
            .run();
        
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
//            if(!Metadata.EMPTY.getYear().equals(hist.f) && !metadata.getYear().equals(hist.f))
//                hist.f = hist.f==null ? metadata.getYear() : Metadata.EMPTY.getYear();
//        };
//        h.clear();
//        h.accumulate(list);
//        // read histogram
//        table.setItemsRaw(h.toList((value,s)->new MetadataGroup(f, value, s.a, s.b.size(), s.c, s.d, s.e/s.a, s.f)));
//        // pass down the chain
//        forwardItems(list);
    }
    
    // Sends event to next level
    private void forwardItems(List<Metadata> list) {
        DB.views.push(lvl.getValue()+1, filerList(list,true,false));
    }
    
    private List<Metadata> filerList(List<Metadata> list, boolean orAll, boolean orEmpty) {

        List<MetadataGroup> mgs = orAll ? table.getSelectedOrAllItems() : table.getSelectedItems();
        
        // optimization : if empty, dont bother filtering
        if(mgs.isEmpty()) return orEmpty ? EMPTY_LIST : new ArrayList(list);
        
        // composed predicate, too much wasteful computation...
        // Predicate<Metadata> p = mgs.parallelStream()
        //        .map(MetadataGroup::toMetadataPredicate)
        //        .reduce(Predicate::or)
        //        .orElse(isFALSE);
        
        // optimisation : compute values ONCE if doable
        Field f = fieldFilter.getValue();
        List l = map(mgs,mg->mg.getValue());
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
    
    // get all items in grouped in the selected groups, sorts using library sort order \
    private List<Metadata> filerListToSelectedNsort() {
        List<Metadata> l = filerList(DB.views.getValue(lvl.getValue()),false,true);
                       l.sort(DB.library_sorter);
        return l;
    }
    private void playSelected() {
        play(filerList(DB.views.getValue(lvl.getValue()),false,true));
    }
    
/******************************* SELECTION RESTORE ****************************/
    
    private boolean sel_lock = false;
    private Set sel_old;

    private void selectionStore() {
        // remember selected
        sel_old = table.getSelectedItems().stream().map(MetadataGroup::getValue).collect(toSet());
        
        sel_lock = true;    // prevent selection from propagating change
    }
    private void selectionReStore() {
        // update selected - restore every available old one
        forEachI(table.getItems(), (i,mg) -> {
            if(sel_old.contains(mg.getValue())) {
                table.getSelectionModel().select(i);
            }
        });
        // performance optimization - prevents refreshed of a lot of items
        if(table.getSelectionModel().isEmpty() && !table.getItems().isEmpty())
            table.getSelectionModel().select(0);
        
        sel_lock = false;   // enable propagation
    }
    
/******************************** CONTEXT MENU ********************************/
    
    private static Menu searchMenu;
    private static final TableContextMenuRInstance<Metadata, LibraryViewController> contxt_menu = new TableContextMenuRInstance<>(
        () -> {
            ImprovedContextMenu<List<Metadata>> m = new ImprovedContextMenu();
            MenuItem[] is = menuItems(App.plugins.getPlugins(HttpSearchQueryBuilder.class), 
                                      q -> "in " + Parser.toS(q),
                                      q -> Environment.browse(q.apply(m.getValue().get(0).getAlbum())));
            searchMenu = new Menu("Search album cover",null,is);
            m.getItems().addAll(menuItem("Play items", e -> play(m.getValue())),
                menuItem("Enqueue items", e -> PlaylistManager.addItems(m.getValue())),
                menuItem("Update from file", e -> App.refreshItemsFromFileJob(m.getValue())),
                menuItem("Remove from library", e -> DB.removeItems(m.getValue())),
                new Menu("Show in",null,
                    menuItems(filterMap(WidgetManager.getFactories(),f->f.hasFeature(SongReader.class),f->f.name()),
                            (String f) -> f,
                            (String f) -> WidgetManager.use(w->w.name().equals(f),NO_LAYOUT,c->((SongReader)c.getController()).read(m.getValue())))
                ),
                new Menu("Edit tags in",null,
                    menuItems(filterMap(WidgetManager.getFactories(),f->f.hasFeature(SongWriter.class),f->f.name()),
                            (String f) -> f,
                            (String f) -> WidgetManager.use(w->w.name().equals(f),NO_LAYOUT,c->((SongWriter)c.getController()).read(m.getValue())))
                ),
                searchMenu
            );
            return m;
        }, (menu, w) -> {
            menu.setValue(w.filerListToSelectedNsort());
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
    

    
    

    {
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