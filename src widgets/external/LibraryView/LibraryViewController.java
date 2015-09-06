
package LibraryView;

import java.time.Year;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.layout.AnchorPane;
import javafx.util.Callback;

import org.reactfx.EventStreams;

import AudioPlayer.Player;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.services.Database.DB;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.Metadata.Field;
import AudioPlayer.tagging.MetadataGroup;
import Configuration.Config;
import Configuration.IsConfig;
import Layout.Widgets.Widget.Info;
import Layout.Widgets.WidgetManager;
import Layout.Widgets.controller.FXMLController;
import Layout.Widgets.controller.io.Input;
import Layout.Widgets.controller.io.Output;
import Layout.Widgets.feature.SongReader;
import Layout.Widgets.feature.SongWriter;
import gui.GUI;
import gui.objects.ContextMenu.ImprovedContextMenu;
import gui.objects.ContextMenu.SelectionMenuItem;
import gui.objects.ContextMenu.TableContextMenuMⱤ;
import gui.objects.Table.FilteredTable;
import gui.objects.Table.ImprovedTable.PojoV;
import gui.objects.Table.TableColumnInfo;
import gui.objects.Table.TableColumnInfo.ColumnInfo;
import gui.objects.TableCell.NumberRatingCellFactory;
import gui.objects.TableRow.ImprovedTableRow;
import main.App;
import util.File.Environment;
import util.access.FieldValue.FieldEnum.ColumnField;
import util.access.VarEnum;
import util.access.Ѵo;
import util.async.executor.ExecuteN;
import util.collections.Histogram;
import util.collections.TupleM6;
import util.graphics.drag.DragUtil;
import util.parsing.Parser;
import web.HttpSearchQueryBuilder;

import static AudioPlayer.tagging.Metadata.Field.ALBUM;
import static AudioPlayer.tagging.Metadata.Field.CATEGORY;
import static AudioPlayer.tagging.MetadataGroup.Field.*;
import static Layout.Widgets.Widget.Group.LIBRARY;
import static Layout.Widgets.WidgetManager.WidgetSource.NO_LAYOUT;
import static java.time.Duration.ofMillis;
import static java.util.Collections.EMPTY_LIST;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static javafx.application.Platform.runLater;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.geometry.Pos.CENTER_RIGHT;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import static javafx.scene.control.TableView.UNCONSTRAINED_RESIZE_POLICY;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.TransferMode.COPY;
import static javafx.stage.WindowEvent.WINDOW_SHOWN;
import static util.Util.menuItem;
import static util.Util.menuItems;
import static util.async.future.Fut.fut;
import static util.collections.Tuples.tuple;
import static util.functional.Util.*;
import static util.graphics.Util.setAnchors;
import static util.reactive.Util.maintain;

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
    private final FilteredTable<MetadataGroup,MetadataGroup.Field> table = new FilteredTable<>(VALUE);
    
    // input/output
    private Output<MetadataGroup> out_sel;
    private Output<List<Metadata>> out_sel_met;
    private Input<List<Metadata>> in_items;
    
    // configurables
    @IsConfig(name = "Table orientation", info = "Orientation of the table.")
    public final Ѵo<NodeOrientation> orient = new Ѵo<>(GUI.table_orient);
    @IsConfig(name = "Zeropad numbers", info = "Adds 0s for number length consistency.")
    public final Ѵo<Boolean> zeropad = new Ѵo<>(GUI.table_zeropad);
    @IsConfig(name = "Search show original index", info = "Show unfiltered table item index when filter applied.")
    public final Ѵo<Boolean> orig_index = new Ѵo<>(GUI.table_orig_index);
    @IsConfig(name = "Show table header", info = "Show table header with columns.")
    public final Ѵo<Boolean> show_header = new Ѵo<>(GUI.table_show_header);
    @IsConfig(name = "Show table footer", info = "Show table controls at the bottom of the table. Displays menubar and table items information.")
    public final Ѵo<Boolean> show_footer = new Ѵo<>(GUI.table_show_footer);
    @IsConfig(name = "Field")
    public final VarEnum<Metadata.Field> fieldFilter = new VarEnum<>(CATEGORY, this::applyData,
        ()->filter(Metadata.Field.values(), Field::isTypeStringRepresentable)
    );
    
    private final ExecuteN runOnce = new ExecuteN(1);
    
    @Override
    public void init() {
        out_sel = outputs.create(widget.id,"Selected Group", MetadataGroup.class, null);
        out_sel_met = outputs.create(widget.id,"Selected", List.class, EMPTY_LIST);
        in_items = inputs.create("To display", List.class, EMPTY_LIST, this::setItems);
        
        // add table to scene graph
        root.getChildren().add(table.getRoot());
        setAnchors(table.getRoot(),0d);
        
        // table properties
        table.setFixedCellSize(GUI.font.getValue().getSize() + 5);
        table.getSelectionModel().setSelectionMode(MULTIPLE);
        table.searchSetColumn(VALUE);
        d(maintain(orient,table.nodeOrientationProperty()));
        d(maintain(zeropad,table.zeropadIndex));
        d(maintain(orig_index,table.showOriginalIndex));
        d(maintain(show_header,table.headerVisible));
        d(maintain(show_footer,table.footerVisible));
        
        
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
        table.getDefaultColumnInfo();
        
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
        d(Player.playingtem.onUpdate(m -> table.updateStyleRules()));
       
        
        // column context menu - add change field submenus
        Menu m = (Menu)table.columnVisibleMenu.getItems().stream().filter(i->i.getText().equals("Value")).findFirst().get();
        Stream.of(Field.values())
              .sorted(by(f -> f.name()))
              .map(f -> new SelectionMenuItem(f.toStringEnum(), false){{
                  this.setOnMouseClicked(() -> {
                        if(!selected.get()) {
                            // refresh menu
                            m.getItems().forEach(mi -> ((SelectionMenuItem)mi).selected.set(false));
                            selected.set(true);
                            // apply
                            fieldFilter.setNapplyValue(f);
                        }
                  });
              }})
              .forEach(m.getItems()::add);
            // refresh when menu opens
        table.columnVisibleMenu.addEventHandler(WINDOW_SHOWN, e -> m.getItems().forEach(mi -> ((SelectionMenuItem)mi).selected.set(fieldFilter.getValue().toStringEnum().equals(mi.getText()))));
        
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
                DragUtil.setItemList(filerListToSelectedNsort(),db,true);
            }
            e.consume();
        });
        
        // resizing
        table.setColumnResizePolicy(resize -> {
            FilteredTable<MetadataGroup,?> t = table;   // (FilteredTable) resize.getTable()
            boolean b = UNCONSTRAINED_RESIZE_POLICY.call(resize);
            // resize index column
            t.getColumn(ColumnField.INDEX).ifPresent(i->i.setPrefWidth(t.calculateIndexColumnWidth()));
            // resize main column to span remaining space
            t.getColumn(VALUE).ifPresent(c->{
                double Σcw = t.getColumns().stream().filter(TableColumn::isVisible).mapToDouble(TableColumn::getWidth).sum();
                double sw = t.getVScrollbarWidth();
                c.setPrefWidth(t.getWidth()-(sw+Σcw-c.getWidth()));
            });
            return b;
        });
        
        // maintain outputs
        table.getSelectionModel().selectedItemProperty().addListener((o,ov,nv) -> out_sel.setValue(nv));
        // forward on selection
        EventStreams.changesOf(table.getSelectedItems()).reduceSuccessions((a,b)->b, ofMillis(60)).subscribe(c -> {
            if(!sel_lock)
                out_sel_met.setValue(filerList(in_items.getValue(),true,false));
        });
        table.getSelectionModel().selectedItemProperty().addListener((o,ov,nv) -> {
            if(!sel_lock)
                sel_last = nv==null ? "null" : nv.getField().toS(nv.getValue(), "");
        });
        
        // prevent volume change
        table.setOnScroll(Event::consume);
    }
    
    @Override
    public void refresh() {
        runOnce.execute(() -> {
            String c = getWidget().properties.getS("columns");
            table.setColumnState(c==null ? table.getDefaultColumnInfo() : TableColumnInfo.fromString(c));
        });
        applyData(null);
    }

    @Override
    public Collection<Config<Object>> getFields() {
        // serialize column state when requested
        getWidget().properties.put("columns", table.getColumnState().toString());
        return super.getFields();
    }
    
    
/******************************** PRIVATE API *********************************/
    
    //applies lvl & fieldFilter
    private void applyData(Object o) {
        Metadata.Field f = fieldFilter.getValue();
        
        // rebuild value column
        find(table.getColumns(), c -> VALUE == c.getUserData()).ifPresent(c -> {
            TableColumn<MetadataGroup,?> t = table.getColumnFactory().call(VALUE);
            c.setText(t.getText());
            c.setCellFactory((Callback)t.getCellFactory());
            c.setCellValueFactory((Callback)t.getCellValueFactory());
        });
        // update filters
        table.filterPane.setPrefTypeSupplier(() -> tuple(VALUE.toString(f), VALUE.getType(f), VALUE));
        table.filterPane.setData(map(MetadataGroup.Field.values(), mgf->tuple(mgf.toString(f),mgf.getType(f),mgf)));
        
        setItems(in_items.getValue());
    }
    
    private final Histogram<Object, Metadata, TupleM6<Long,Set<String>,Double,Long,Double,Year>> h = new Histogram();
    
    /** populates metadata groups to table from metadata list */
    private void setItems(List<Metadata> list) {
        fut(fieldFilter.getValue())
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
                List<Metadata> fl = filerList(list,true,false);
                runLater(() -> {
                    if(!l.isEmpty()) {
                        selectionStore();
                        table.setItemsRaw(l);
                        selectionReStore();
                    }
                    out_sel_met.setValue(fl);
                });
            })
            .run();
    }
    
    private List<Metadata> filerList(List<Metadata> list, boolean orAll, boolean orEmpty) {
        if(list==null || list.isEmpty()) return EMPTY_LIST;
        
        // bug fix, without this line, which does exactly nothing,
        // mgs list contains nulls sometimes (no idea why)
        util.functional.Util.toS(table.getSelectedItems(),Objects::toString);
            
        List<MetadataGroup> mgs = orAll ? table.getSelectedOrAllItems() : table.getSelectedItems();

        // optimization : if empty, dont bother filtering
        if(mgs.isEmpty()) return orEmpty ? EMPTY_LIST : new ArrayList<>(list);
        
        // composed predicate, performs badly
        // Predicate<Metadata> p = mgs.parallelStream()
        //        .map(MetadataGroup::toMetadataPredicate)
        //        .reduce(Predicate::or)
        //        .orElse(NONE);
        
        Field f = fieldFilter.getValue();
        Predicate<Metadata> p;
        // optimization : if only 1, dont use list
        // optimization : dont use equals for primitive types
        if(mgs.size()==1) {
            boolean prim = f.getType().isPrimitive();
            Object v = mgs.get(0).getValue();
            p = prim ? m -> m.getField(f)==v : m -> v.equals(m.getField(f));
        } else {
            Set<Object> l = mgs.stream().map(mg->mg.getValue()).collect(toSet());
            boolean prim = f.getType().isPrimitive();
            p = prim ? m -> isInR(m.getField(f), l) : m -> l.contains(m.getField(f));
        }
        
        // optimization : use parallel stream
        return list.parallelStream().filter(p).collect(toList());
    }
    
    // get all items in grouped in the selected groups, sorts using library sort order \
    private List<Metadata> filerListToSelectedNsort() {
        List<Metadata> l = filerList(in_items.getValue(),false,true);
                       l.sort(DB.library_sorter.get());
        return l;
    }
    private void playSelected() {
        play(filerList(in_items.getValue(),false,true));
    }
    
/******************************* SELECTION RESTORE ****************************/
    
    // restoring selection if table items change, we want to preserve as many
    // selected items as possible - when selection changes, we select all items
    // (previously selected) that are still in the table 
    private boolean sel_lock = false;
    private Set sel_old;
    // restoring selection from previous session, we serialize string
    // representation and try to restre when application runs again
    // we restore only once
    @IsConfig(name = "Last selected", editable = false)
    private String sel_last = "null";
    private boolean sel_last_restored = false;
    
    private void selectionStore() {
        // remember selected
        sel_old = table.getSelectedItems().stream().map(MetadataGroup::getValue).collect(toSet());
        sel_lock = true;    // prevent forwarding items
    }
    
    private void selectionReStore() {
        if(table.getItems().isEmpty()) return;
        
        // restore last selected from previous session
        if(!sel_last_restored && !"null".equals(sel_last)) {
            forEachWithI(table.getItems(), (i,mg) -> {
                if(mg.getField().toS(mg.getValue(), "").equals(sel_last)) {
                    table.getSelectionModel().select(i);
                    sel_last_restored = true; // restore only once
                    return;
                }
            });
            
        // update selected - restore every available old one
        } else {
            forEachWithI(table.getItems(), (i,mg) -> {
                if(sel_old.contains(mg.getValue())) {
                    table.getSelectionModel().select(i);
                }
            });
        }
        // performance optimization - prevents refreshes of a lot of items
        if(table.getSelectionModel().isEmpty())
            table.getSelectionModel().select(0);
        
        sel_lock = false;   // enable forwarding items
    }
    
/******************************** CONTEXT MENU ********************************/
    
    private static Menu searchMenu;
    private static final TableContextMenuMⱤ<Metadata, LibraryViewController> contxt_menu = new TableContextMenuMⱤ<>(
        () -> {
            ImprovedContextMenu<List<Metadata>> m = new ImprovedContextMenu();
            MenuItem[] is = menuItems(App.plugins.getPlugins(HttpSearchQueryBuilder.class), 
                                      q -> "in " + Parser.toS(q),
                                      q -> Environment.browse(q.apply(m.getValue().get(0).getAlbum())));
            searchMenu = new Menu("Search album cover",null,is);
            m.getItems().addAll(menuItem("Play items", e -> play(m.getValue())),
                menuItem("Enqueue items", e -> PlaylistManager.use(p -> p.addItems(m.getValue()))),
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
        PlaylistManager.use(p -> p.setNplay(items.stream().sorted(DB.library_sorter.get())));
    }
    
}