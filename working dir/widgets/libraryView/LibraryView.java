package libraryView;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.layout.AnchorPane;
import javafx.util.Callback;

import audio.Player;
import audio.playlist.PlaylistManager;
import audio.tagging.Metadata;
import audio.tagging.Metadata.Field;
import audio.tagging.MetadataGroup;
import gui.Gui;
import gui.itemnode.FieldedPredicateItemNode.PredicateData;
import gui.objects.contextmenu.ImprovedContextMenu;
import gui.objects.contextmenu.SelectionMenuItem;
import gui.objects.contextmenu.TableContextMenuMR;
import gui.objects.table.FilteredTable;
import gui.objects.table.ImprovedTable.PojoV;
import gui.objects.table.TableColumnInfo;
import gui.objects.tablecell.NumberRatingCellFactory;
import gui.objects.tablerow.ImprovedTableRow;
import layout.widget.Widget.Info;
import layout.widget.controller.FXMLController;
import layout.widget.controller.io.Input;
import layout.widget.controller.io.Output;
import layout.widget.feature.SongReader;
import layout.widget.feature.SongWriter;
import main.App;
import services.database.Db;
import util.access.VarEnum;
import util.access.Vo;
import util.async.executor.ExecuteN;
import util.conf.Config;
import util.conf.IsConfig;
import util.file.Environment;
import util.graphics.drag.DragUtil;
import util.parsing.Parser;
import web.SearchUriBuilder;

import static audio.tagging.Metadata.Field.ALBUM;
import static audio.tagging.Metadata.Field.CATEGORY;
import static audio.tagging.MetadataGroup.Field.*;
import static audio.tagging.MetadataGroup.degroup;
import static gui.objects.contextmenu.SelectionMenuItem.buildSingleSelectionMenu;
import static java.time.Duration.ofMillis;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.geometry.Pos.CENTER_RIGHT;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import static javafx.scene.control.TableView.UNCONSTRAINED_RESIZE_POLICY;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.TransferMode.COPY;
import static javafx.stage.WindowEvent.WINDOW_SHOWN;
import static layout.widget.Widget.Group.LIBRARY;
import static layout.widget.WidgetManager.WidgetSource.NO_LAYOUT;
import static main.App.APP;
import static org.reactfx.EventStreams.changesOf;
import static util.async.Async.runLater;
import static util.async.future.Fut.fut;
import static util.collections.Tuples.tuple;
import static util.functional.Util.*;
import static util.graphics.Util.*;
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
public class LibraryView extends FXMLController {

    private @FXML AnchorPane root;
    private final FilteredTable<MetadataGroup,MetadataGroup.Field> table = new FilteredTable<>(VALUE);

    // input/output
    private Output<MetadataGroup> out_sel;
    private Output<List<Metadata>> out_sel_met;
    private Input<List<Metadata>> in_items;

    // configurables
    @IsConfig(name = "Table orientation", info = "Orientation of the table.")
    public final Vo<NodeOrientation> orient = new Vo<>(Gui.table_orient);
    @IsConfig(name = "Zeropad numbers", info = "Adds 0s for number length consistency.")
    public final Vo<Boolean> zeropad = new Vo<>(Gui.table_zeropad);
    @IsConfig(name = "Search show original index", info = "Show unfiltered table item index when filter applied.")
    public final Vo<Boolean> orig_index = new Vo<>(Gui.table_orig_index);
    @IsConfig(name = "Show table header", info = "Show table header with columns.")
    public final Vo<Boolean> show_header = new Vo<>(Gui.table_show_header);
    @IsConfig(name = "Show table footer", info = "Show table controls at the bottom of the table. Displays menu bar and table items information.")
    public final Vo<Boolean> show_footer = new Vo<>(Gui.table_show_footer);
    @IsConfig(name = "Field")
    public final VarEnum<Metadata.Field> fieldFilter = new VarEnum<Metadata.Field>(CATEGORY,
        () -> filter(Metadata.Field.values(), Field::isTypeStringRepresentable),
        this::applyData
    );

    private final ExecuteN runOnce = new ExecuteN(1);

    @Override
    public void init() {
        out_sel = outputs.create(widget.id,"Selected Group", MetadataGroup.class, null);
        out_sel_met = outputs.create(widget.id,"Selected", List.class, listRO());
        in_items = inputs.create("To display", List.class, listRO(), this::setItems);


//        out_sel_met = outputs.create(widget.id,"Selected Songs", new TypeToken<List<Metadata>>(){}, listRO());
//        in_items = inputs.create("To display", new TypeToken<List<Metadata>>(){}.getRawType(), listRO(), this::setItems);

        // add table to scene graph
        root.getChildren().add(table.getRoot());
        setAnchors(table.getRoot(),0d);

        // table properties
        table.setFixedCellSize(Gui.font.getValue().getSize() + 5);
        table.getSelectionModel().setSelectionMode(MULTIPLE);
        table.searchSetColumn(VALUE);
        d(maintain(orient,table.nodeOrientationProperty()));
        d(maintain(zeropad,table.zeropadIndex));
        d(maintain(orig_index,table.showOriginalIndex));
        d(maintain(show_header,table.headerVisible));
        d(maintain(show_footer,table.footerVisible));


        // set up table columns
        table.setkeyNameColMapper(name-> "#".equals(name) ? name : MetadataGroup.Field.valueOfEnumString(name).toString());
        table.setColumnFactory(f -> {
            Metadata.Field mf = fieldFilter.getValue();
            TableColumn<MetadataGroup,?> c = new TableColumn(f.toString(mf));
            c.setCellValueFactory( cf -> cf.getValue()==null ? null : new PojoV(f.getOf(cf.getValue())));
            Pos a = f.getType(mf)==String.class ? CENTER_LEFT : CENTER_RIGHT;
            c.setCellFactory(f==AVG_RATING
                ? (Callback) APP.ratingCell.getValue()
                : f==W_RATING
                ? (Callback) new NumberRatingCellFactory()
                : (Callback) col -> { TableCell cel = table.buildDefaultCell(f); cel.setAlignment(a); return cel;}
            );
            return c;
        });
        // maintain rating column cell style
        APP.ratingCell.addListener((o,ov,nv) -> table.getColumn(AVG_RATING).ifPresent(c->c.setCellFactory((Callback)nv)));
        table.getDefaultColumnInfo();

        // rows
        table.setRowFactory(tbl -> new ImprovedTableRow<MetadataGroup>()
                // additional css styleclasses
                .styleRuleAdd("played", MetadataGroup::isPlaying)
                // add behavior
                .onLeftDoubleClick((row,e) -> playSelected())
                .onRightSingleClick((row,e) -> {
                    // prep selection for context menu
                    if (!row.isSelected())
                        tbl.getSelectionModel().clearAndSelect(row.getIndex());
                    // show context menu
                    contextMenu.show(this, (TableView)table, e);
                })
        );
        // maintain playing item css by refreshing column
        d(Player.playingItem.onUpdate(m -> table.updateStyleRules()));


        // column context menu - add change field submenus
        Menu m = (Menu)table.columnVisibleMenu.getItems().stream().filter(i->i.getText().equals("Value")).findFirst().get();
        m.getItems().addAll(
            buildSingleSelectionMenu(
                list(Metadata.Field.values()),
                null,
                Metadata.Field::name,
                fieldFilter::setNapplyValue
            )
        );
        // refresh when menu opens
        table.columnVisibleMenu.addEventHandler(WINDOW_SHOWN, e -> m.getItems().forEach(mi -> ((SelectionMenuItem)mi).selected.setValue(fieldFilter.getValue().toStringEnum().equals(mi.getText()))));
        // add menu items
        table.menuRemove.getItems().addAll(
            menuItem("Remove selected groups from library", () -> Db.removeItems(degroup(table.getSelectedItems()))),
            menuItem("Remove playing group from library", () -> Db.removeItems(degroup(table.getItems().stream().filter(mg -> mg.isPlaying())))),
            menuItem("Remove all groups from library", () -> Db.removeItems(degroup(table.getItems())))
        );

        // key actions
        table.setOnKeyPressed(e -> {
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
        d(changesOf(table.getSelectedItems())
          .reduceSuccessions((a,b) -> b, ofMillis(100)).subscribe(c -> {

                if (!sel_ignore) if (fieldFilter.get()==CATEGORY) {
                    System.out.println("output set " + filterList(in_items.getValue(),true,false).size());
                }
                if (!sel_ignore)
                    out_sel_met.setValue(filterList(in_items.getValue(),true,false));
                if (sel_ignore_canturnback) {
                    sel_ignore_canturnback = false;
                    sel_ignore = false;
                }
        }));
        d(changesOf(table.getSelectionModel().selectedItemProperty())
          .reduceSuccessions((a,b) -> b, ofMillis(100)).subscribe(s -> {
                MetadataGroup nv = s.getNewValue();
                if (!sel_ignore)
                    sel_last = nv==null ? "null" : VALUE.toS(nv,nv.getValue(), "");
        }));

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

    // applies lvl & fieldFilter
    @SuppressWarnings("unchecked")
    private void applyData(Object o) {
        Metadata.Field f = fieldFilter.getValue();

        // rebuild value column
        // TODO: this should be fully type safe
        table.getColumn(VALUE).ifPresent(c -> {
            TableColumn<MetadataGroup,?> t = table.getColumnFactory().call(VALUE);
            c.setText(t.getText());
            c.setCellValueFactory((Callback)t.getCellValueFactory());
            c.setCellFactory((Callback)t.getCellFactory());
            table.refreshColumn(c);
        });
        // update filters
        table.filterPane.setPrefTypeSupplier(() -> PredicateData.ofField(VALUE));
        table.filterPane.setData(map(MetadataGroup.Field.values(), mgf -> new PredicateData(mgf.toString(f),mgf.getType(f),mgf)));

        setItems(in_items.getValue());
    }

    /** populates metadata groups to table from metadata list */
    private void setItems(List<Metadata> list) {
        if (list==null) return;
        fut(fieldFilter.getValue())
            .use(f -> {
                List<MetadataGroup> mgs = stream(
                    MetadataGroup.groupOf(f,list),
                    MetadataGroup.groupsOf(f,list)
                ).collect(toList());
                List<Metadata> fl = filterList(list,true,false);
                runLater(() -> {
                    if (!mgs.isEmpty()) {
                        selectionStore();
                        table.setItemsRaw(mgs);
                        selectionReStore();
                        out_sel_met.setValue(fl);
                    }
                });
            })
            .run();
    }

    private List<Metadata> filterList(List<Metadata> list, boolean orAll, boolean orEmpty) {
        if (list==null || list.isEmpty()) return listRO();

        List<MetadataGroup> mgs = orAll ? table.getSelectedOrAllItems() : table.getSelectedItems();

        // handle special "All" row, selecting it is equivalent to selecting all rows
        if (mgs.stream().anyMatch(mg -> mg.isAll())) return list;
        else return mgs.stream().flatMap(mg -> mg.getGrouped().stream()).collect(toList());
    }

    // get all items in grouped in the selected groups, sorts using library sort order \
    private List<Metadata> filerListToSelectedNsort() {
        List<Metadata> l = filterList(in_items.getValue(),false,true);
                       l.sort(Db.library_sorter.get());
        return l;
    }

    private void playSelected() {
        play(filterList(in_items.getValue(),false,true));
    }

    private List<Metadata> getSelected() {
        return filterList(in_items.getValue(),false,true);
    }

/******************************* SELECTION RESTORE ****************************/

    // restoring selection if table items change, we want to preserve as many
    // selected items as possible - when selection changes, we select all items
    // (previously selected) that are still in the table
    private boolean sel_ignore = false;
    private boolean sel_ignore_canturnback = true;
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
        sel_ignore = true;
        sel_ignore_canturnback = false;
    }

    private void selectionReStore() {
        if (table.getItems().isEmpty()) return;

        // restore last selected from previous session
        if (!sel_last_restored && !"null".equals(sel_last)) {
            forEachWithI(table.getItems(), (i,mg) -> {
                if (VALUE.toS(mg,mg.getValue(), "").equals(sel_last)) {
                    table.getSelectionModel().select(i);
                    sel_last_restored = true; // restore only once
                    return;
                }
            });

        // update selected - restore every available old one
        } else {
            forEachWithI(table.getItems(), (i,mg) -> {
                if (sel_old.contains(mg.getValue())) {
                    table.getSelectionModel().select(i);
                }
            });
        }
        // performance optimization - prevents refreshes of a lot of items
        if (table.getSelectionModel().isEmpty())
            table.getSelectionModel().select(0);

//        sel_ignore = false;
        sel_ignore_canturnback = true;
    }

/******************************** CONTEXT MENU ********************************/

    private static Menu searchMenu;
    private static final TableContextMenuMR<Metadata, LibraryView> contextMenu = new TableContextMenuMR<>(
        () -> {
            ImprovedContextMenu<List<Metadata>> m = new ImprovedContextMenu<>();
            MenuItem[] is = menuItems(APP.plugins.getPlugins(SearchUriBuilder.class),
                                      q -> "in " + Parser.DEFAULT.toS(q),
                                      q -> Environment.browse(q.apply(m.getValue().get(0).getAlbum())));
            searchMenu = new Menu("Search album cover",null,is);
            m.getItems().addAll(menuItem("Play items", e -> play(m.getValue())),
                menuItem("Enqueue items", e -> PlaylistManager.use(p -> p.addItems(m.getValue()))),
                menuItem("Update from file", e -> App.refreshItemsFromFileJob(m.getValue())),
                menuItem("Remove from library", e -> Db.removeItems(m.getValue())),
                new Menu("Show in",null,
                    menuItems(filterMap(APP.widgetManager.getFactories(), f->f.hasFeature(SongReader.class), f -> f.nameGui()),
                        f -> f,
                        f -> APP.widgetManager.use(f,NO_LAYOUT,c->((SongReader)c.getController()).read(m.getValue()))
                    )
                ),
                new Menu("Edit tags in",null,
                    menuItems(filterMap(APP.widgetManager.getFactories(), f->f.hasFeature(SongWriter.class), f -> f.nameGui()),
                        f -> f,
                        f -> APP.widgetManager.use(f,NO_LAYOUT,c->((SongWriter)c.getController()).read(m.getValue()))
                    )
                ),
                searchMenu
            );
            return m;
        }, (menu, w) -> {
            menu.setValue(w.filerListToSelectedNsort());
            if (w.fieldFilter.getValue()==ALBUM && menu.getItems().size()==5)
                menu.getItems().add(searchMenu);
            if (w.fieldFilter.getValue()!=ALBUM && menu.getItems().size()==6)
                menu.getItems().remove(searchMenu);
        });

    private static void play(List<Metadata> items) {
        if (items.isEmpty()) return;
        PlaylistManager.use(p -> p.setNplay(items.stream().sorted(Db.library_sorter.get())));
    }

}