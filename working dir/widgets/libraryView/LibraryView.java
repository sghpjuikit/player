package libraryView;

import sp.it.pl.audio.Player;
import sp.it.pl.audio.playlist.PlaylistManager;
import sp.it.pl.audio.tagging.Metadata;
import sp.it.pl.audio.tagging.Metadata.Field;
import sp.it.pl.audio.tagging.MetadataGroup;
import sp.it.pl.gui.Gui;
import sp.it.pl.gui.itemnode.FieldedPredicateItemNode.PredicateData;
import sp.it.pl.gui.objects.contextmenu.SelectionMenuItem;
import sp.it.pl.gui.objects.contextmenu.TableContextMenuR;
import sp.it.pl.gui.objects.table.FilteredTable;
import sp.it.pl.gui.objects.table.ImprovedTable.PojoV;
import sp.it.pl.gui.objects.table.TableColumnInfo;
import sp.it.pl.gui.objects.tablecell.NumberRatingCellFactory;
import sp.it.pl.gui.objects.tablerow.ImprovedTableRow;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.control.Menu;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.input.Dragboard;
import javafx.scene.layout.AnchorPane;
import javafx.util.Callback;
import sp.it.pl.layout.widget.Widget.Info;
import sp.it.pl.layout.widget.controller.FXMLController;
import sp.it.pl.layout.widget.controller.io.Input;
import sp.it.pl.layout.widget.controller.io.Output;
import sp.it.pl.util.access.VarEnum;
import sp.it.pl.util.access.Vo;
import sp.it.pl.util.access.fieldvalue.ColumnField;
import sp.it.pl.util.async.executor.ExecuteN;
import sp.it.pl.util.conf.Config;
import sp.it.pl.util.conf.IsConfig;
import sp.it.pl.util.conf.IsConfig.EditMode;
import sp.it.pl.util.graphics.drag.DragUtil;
import static sp.it.pl.audio.tagging.Metadata.Field.CATEGORY;
import static sp.it.pl.audio.tagging.MetadataGroup.Field.*;
import static sp.it.pl.audio.tagging.MetadataGroup.ungroup;
import static sp.it.pl.gui.objects.contextmenu.SelectionMenuItem.buildSingleSelectionMenu;
import static java.time.Duration.ofMillis;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.geometry.Pos.CENTER_RIGHT;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import static javafx.scene.control.TableView.UNCONSTRAINED_RESIZE_POLICY;
import static javafx.scene.input.KeyCode.DELETE;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.TransferMode.COPY;
import static javafx.stage.WindowEvent.WINDOW_SHOWN;
import static sp.it.pl.layout.widget.Widget.Group.LIBRARY;
import static sp.it.pl.main.AppUtil.APP;
import static org.reactfx.EventStreams.changesOf;
import static sp.it.pl.util.async.AsyncKt.runLater;
import static sp.it.pl.util.async.future.Fut.fut;
import static sp.it.pl.util.functional.Util.*;
import static sp.it.pl.util.graphics.Util.menuItem;
import static sp.it.pl.util.graphics.Util.setAnchors;
import static sp.it.pl.util.reactive.Util.maintain;

@Info(
    author = "Martin Polakovic",
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

    private static final TableContextMenuR<MetadataGroup> contextMenu = new TableContextMenuR<>();

    private @FXML AnchorPane root;
    private final FilteredTable<MetadataGroup> table = new FilteredTable<>(MetadataGroup.class, VALUE);

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
    public final VarEnum<Metadata.Field<?>> fieldFilter = new VarEnum<>(CATEGORY,
        filter(Metadata.Field.FIELDS, Field::isTypeStringRepresentable),
        this::applyData
    );

    private final ExecuteN runOnce = new ExecuteN(1);

    @Override
    public void init() {
        out_sel = outputs.create(widget.id,"Selected Group", MetadataGroup.class, null);
        out_sel_met = outputs.create(widget.id,"Selected", List.class, listRO());
        in_items = inputs.create("To display", List.class, listRO(), this::setItems);

        // add table to scene graph
        root.getChildren().add(table.getRoot());
        setAnchors(table.getRoot(),0d);

        // table properties
        table.getSelectionModel().setSelectionMode(MULTIPLE);
        table.search.setColumn(VALUE);
        d(maintain(Gui.font, f -> f.getSize()+5, table.fixedCellSizeProperty()));
        d(maintain(orient,table.nodeOrientationProperty()));
        d(maintain(zeropad,table.zeropadIndex));
        d(maintain(orig_index,table.showOriginalIndex));
        d(maintain(show_header,table.headerVisible));
        d(maintain(show_footer,table.footerVisible));

        // set up table columns
        table.setKeyNameColMapper(name-> ColumnField.INDEX.name().equals(name) ? name : MetadataGroup.Field.valueOf(name).toString());
//        table.setKeyNameColMapper(name-> Field.FIELD_NAMES.contains(name) ? MetadataGroup.Field.VALUE.name() : name);
        table.setColumnFactory(f -> {
            if (f instanceof MetadataGroup.Field) {
                MetadataGroup.Field<?> mgf = (MetadataGroup.Field) f;
                Metadata.Field mf = fieldFilter.getValue();
                TableColumn<MetadataGroup,Object> c = new TableColumn<>(mgf.toString(mf));
                c.setCellValueFactory(cf -> cf.getValue()==null ? null : new PojoV<>(mgf.getOf(cf.getValue())));
                Pos a = mgf.getType(mf)==String.class ? CENTER_LEFT : CENTER_RIGHT;
                c.setCellFactory(mgf==AVG_RATING
                        ? (Callback) APP.ratingCell.getValue()
                        : mgf==W_RATING
                                ? (Callback) NumberRatingCellFactory.INSTANCE
                                : col -> {
                                    TableCell cel = table.buildDefaultCell(mgf);
                                    cel.setAlignment(a);
                                    return cel;
                                }
                );
                return c;
            } else {
                TableColumn<MetadataGroup,Object> c = new TableColumn<>(f.toString());
                c.setCellValueFactory(cf -> cf.getValue()==null ? null : new PojoV<>(f.getOf(cf.getValue())));
                c.setCellFactory(column -> table.buildDefaultCell(f));
                return c;
            }
        });
        // maintain rating column cell style
        d(APP.ratingCell.maintain(cf -> table.getColumn(AVG_RATING).ifPresent(c -> c.setCellFactory((Callback)cf))));

        table.getDefaultColumnInfo();

        // rows
        table.setRowFactory(tbl -> new ImprovedTableRow<MetadataGroup>()
                // additional css style classes
                .styleRuleAdd("played", MetadataGroup::isPlaying)
                // add behavior
                .onLeftDoubleClick((row,e) -> playSelected())
                .onRightSingleClick((row,e) -> {
                    // prep selection for context menu
                    if (!row.isSelected())
                        tbl.getSelectionModel().clearAndSelect(row.getIndex());

                    contextMenu.show(MetadataGroup.groupOfUnrelated(filerListToSelectedNsort()), table, e);
                })
        );
        // maintain playing item css by refreshing column
        d(Player.playingItem.onUpdate(m -> table.updateStyleRules()));


        // column context menu - add change VALUE column menus
        Menu m = (Menu)table.columnVisibleMenu.getItems().stream().filter(i->i.getText().equals("Value")).findFirst().get();
        m.getItems().addAll(
            buildSingleSelectionMenu(
                list(Metadata.Field.FIELDS),
                null,
                Metadata.Field::name,
                fieldFilter::setNapplyValue
            )
        );
        // refresh when menu opens
        table.columnMenu.addEventHandler(WINDOW_SHOWN, e -> m.getItems().forEach(mi -> ((SelectionMenuItem)mi).selected.setValue(fieldFilter.getValue().toStringEnum().equals(mi.getText()))));
        // add menu items
        table.menuRemove.getItems().addAll(
            menuItem("Remove selected groups from library", e -> APP.db.removeItems(MetadataGroup.ungroup(table.getSelectedItems()))),
            menuItem("Remove playing group from library", e -> APP.db.removeItems(ungroup(table.getItems().stream().filter(MetadataGroup::isPlaying)))),
            menuItem("Remove all groups from library", e -> APP.db.removeItems(MetadataGroup.ungroup(table.getItems())))
        );

        // key actions
        table.setOnKeyPressed(e -> {
            // play selected
            if (e.getCode() == ENTER) {
                playSelected();
            }
            // delete selected
            if (e.getCode() == DELETE) {
                APP.db.removeItems(table.getSelectedItems().stream().flatMap(mg -> mg.getGrouped().stream()).collect(toList()));
            }
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
            FilteredTable<MetadataGroup> t = table;   // (FilteredTable) resize.getTable()
            boolean b = UNCONSTRAINED_RESIZE_POLICY.call(resize);
            // resize index column
            t.getColumn(ColumnField.INDEX).ifPresent(i -> i.setPrefWidth(t.computeIndexColumnWidth()));
            // resize main column to span remaining space
            t.getColumn(VALUE).ifPresent(c -> {
                double sumW = t.getColumns().stream().filter(TableColumn::isVisible).mapToDouble(TableColumn::getWidth).sum();
                double sbW = t.getVScrollbarWidth();
                c.setPrefWidth(t.getWidth()-(sbW+sumW-c.getWidth()));
            });
            return b;
        });

        // maintain outputs
        table.getSelectionModel().selectedItemProperty().addListener((o,ov,nv) -> out_sel.setValue(nv));
        // forward on selection
        d(changesOf(table.getSelectedItems())
          .reduceSuccessions((a,b) -> b, ofMillis(100)).subscribe(c -> {
                if (!sel_ignore)
                    out_sel_met.setValue(filterList(in_items.getValue(),true));
                if (sel_ignore_canturnback) {
                    sel_ignore_canturnback = false;
                    sel_ignore = false;
                }
        }));
        d(changesOf(table.getSelectionModel().selectedItemProperty())
          .reduceSuccessions((a,b) -> b, ofMillis(100)).subscribe(s -> {
                MetadataGroup nv = s.getNewValue();
                if (!sel_ignore)
                    sel_last = nv==null ? "null" : nv.getValueS("");
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

    @Override
    public void onClose() {
        super.onClose();
    }

    /******************************** PRIVATE API *********************************/

    // applies lvl & fieldFilter
    @SuppressWarnings({"unchecked", "unused"})
    private void applyData(Object o) {
        // rebuild value column
        table.getColumn(VALUE).ifPresent(c -> {
            TableColumn<MetadataGroup,Object> t = table.getColumnFactory().call(VALUE);
            c.setText(t.getText());
            c.setCellValueFactory(t.getCellValueFactory());
            c.setCellFactory(t.getCellFactory());
            table.refreshColumn(c);
        });

        // update filters
        Metadata.Field f = fieldFilter.getValue();
        table.filterPane.inconsistent_state = true;
        table.filterPane.setPrefTypeSupplier(() -> PredicateData.ofField(VALUE));
        table.filterPane.setData(map(MetadataGroup.Field.FIELDS, mgf -> new PredicateData(mgf.toString(f),mgf.getType(f),mgf)));
        table.filterPane.shrinkTo(0);
        table.filterPane.growTo1();
        table.filterPane.clear();

        setItems(in_items.getValue());
    }

    /** Populates metadata groups to table from metadata list. */
    private void setItems(List<Metadata> list) {
        if (list==null) return;
        fut(fieldFilter.getValue())
            .use(f -> {
                List<MetadataGroup> mgs = stream(MetadataGroup.groupOf(f,list), MetadataGroup.groupsOf(f,list)).collect(toList());
                List<Metadata> fl = filterList(list,true);
                runLater(() -> {
                    if (!mgs.isEmpty()) {
                        selectionStore();
                        table.setItemsRaw(mgs);
                        selectionReStore();
                        out_sel_met.setValue(fl);
                    }
                });
            });
    }

    private List<Metadata> filterList(List<Metadata> list, boolean orAll) {
        if (list==null || list.isEmpty()) return listRO();

        List<MetadataGroup> mgs = table.getSelectedOrAllItems(orAll).collect(toList());

        // handle special "All" row, selecting it is equivalent to selecting all rows
        return mgs.stream().anyMatch(MetadataGroup::isAll)
            ? list
            : mgs.stream().flatMap(mg -> mg.getGrouped().stream()).collect(toList());
    }

    /**
     *  Get all items in grouped in the selected groups, sorts using library sort order.
     */
    private List<Metadata> filerListToSelectedNsort() {
        List<Metadata> l = filterList(in_items.getValue(),false);
                       l.sort(APP.db.getLibraryComparator().get());
        return l;
    }

    private void playSelected() {
        play(filerListToSelectedNsort());
    }

    private void play(List<Metadata> items) {
        if (items.isEmpty()) return;
        PlaylistManager.use(p -> p.setNplay(items));
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
    @IsConfig(name = "Last selected", editable = EditMode.APP)
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
                if (mg.getValueS("").equals(sel_last)) {
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

}