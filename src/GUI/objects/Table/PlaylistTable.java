
package gui.objects.table;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.control.*;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Callback;

import org.reactfx.Subscription;

import AudioPlayer.Item;
import AudioPlayer.Player;
import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.services.Database.DB;
import AudioPlayer.tagging.Metadata;
import Layout.widget.feature.SongReader;
import Layout.widget.feature.SongWriter;
import gui.GUI;
import gui.objects.ContextMenu.ImprovedContextMenu;
import gui.objects.ContextMenu.TableContextMenuR;
import gui.objects.TableRow.ImprovedTableRow;
import main.App;
import util.File.Environment;
import util.Util;
import util.access.V;
import util.dev.TODO;
import util.graphics.drag.DragUtil;
import util.parsing.Parser;
import util.units.FormattedDuration;
import web.HttpSearchQueryBuilder;

import static AudioPlayer.playlist.PlaylistItem.Field.*;
import static Layout.widget.WidgetManager.WidgetSource.NO_LAYOUT;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.PLAYLIST_PLUS;
import static java.util.Collections.EMPTY_LIST;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.MouseEvent.MOUSE_DRAGGED;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static main.App.APP;
import static util.Util.*;
import static util.dev.TODO.Purpose.READABILITY;
import static util.functional.Util.SAME;
import static util.functional.Util.filterMap;
import static util.functional.Util.list;
import static util.graphics.drag.DragUtil.installDrag;
import static util.reactive.Util.maintain;

/**
 * Playlist table GUI component.
 * <p>
 * Introduces two additional TableCell css pseudoclasses: {@link #playingRowCSS}
 * and {@link #corruptRowCSS} that style the cells containing played item and
 * corrupted item respectively.
 * <p>
 * Always call {@link #dispose()}
 *
 * @author uranium
 */
@TODO(purpose = READABILITY, note = "dragging duplicite code for empty table case")
public final class PlaylistTable extends FilteredTable<PlaylistItem,PlaylistItem.Field> {

    public final V<Boolean> scrollToPlaying = new V<>(true);
    private final TableColumn<PlaylistItem,String> columnName;
    private final TableColumn<PlaylistItem,FormattedDuration> columnTime;

    // selection helper variables
    double last;
    ArrayList<Integer> selected_temp = new ArrayList<>();

    // dependencies
    private final Subscription d1;

    public PlaylistTable (Playlist playlist) {
        super(NAME,playlist);
        playlist.setTransformation(getItems());

        VBox.setVgrow(this, Priority.ALWAYS);

        // initialize table
        setFixedCellSize(GUI.font.getValue().getSize() + 5);
        getSelectionModel().setSelectionMode(MULTIPLE);

        // initialize column factories
        setColumnFactory( f -> {
            TableColumn<PlaylistItem,Object> c = new TableColumn<>(f.toString());
//            c.setCellValueFactory(f==NAME || f==LENGTH
//                    ? new PropertyValueFactory(f.name().toLowerCase())
//                    : cf -> cf.getValue()== null ? null : new PojoV<>(cf.getValue().getField(f))
//            );
            c.setCellValueFactory(f==NAME || f==LENGTH
                    ? new PropertyValueFactory(f.name().toLowerCase())
                    : new Callback<CellDataFeatures<PlaylistItem,Object>, ObservableValue<Object>>() {
                        @Override
                        public ObservableValue<Object> call(CellDataFeatures<PlaylistItem,Object> cf) {
                            return cf.getValue()== null ? null : new PojoV<>(cf.getValue().getField(f));
                        }
                    }
            );
            c.setCellFactory((Callback)col -> buildDefaultCell(f));
            c.setResizable(true);
            return c;
        });
        setColumnState(getDefaultColumnInfo());
        columnName = (TableColumn) getColumn(NAME).get();
        columnTime = (TableColumn) getColumn(LENGTH).get();

        // initialize row factories
        setRowFactory(t -> new ImprovedTableRow<PlaylistItem>() {
            {
                // remember position for moving selected rows on mouse drag
                setOnMousePressed( e -> last = e.getScreenY());
                // clear table selection on mouse released if no item
                setOnMouseReleased( e -> {
                    if (getItem() == null)
                        selectNone();
                });
                // left doubleckick -> play
                onLeftDoubleClick((r,e) -> getPlaylist().playItem(r.getItem()));
                // right click -> show context menu
                onRightSingleClick((r,e) -> {
                    // prep selection for context menu
                    if(!isSelected())
                        getSelectionModel().clearAndSelect(getIndex());
                    // show context menu
                    contxt_menu.show(PlaylistTable.this, e);
                });
                // handle drag transfer
                setOnDragDropped( e -> dropDrag(e, isEmpty() ? getItems().size() : getIndex()));

                // additional css styleclasses
                styleRuleAdd("played", p -> p==getPlaylist().getPlaying());
                styleRuleAdd("corrupt", PlaylistItem::isCorruptCached);
            }
        });
        // maintain playing item css by refreshing index column
        d1 = Player.playingtem.onChange(o -> refreshColumn(columnIndex));

        // resizing
        setColumnResizePolicy(resize -> {
            // handle column resize (except index)
            if(resize!=null && resize.getColumn()!=null && resize.getColumn()!=columnIndex) {
                if(getColumns().contains(columnName))
                    columnName.setPrefWidth(columnName.getWidth()-resize.getDelta());
                resize.getColumn().setPrefWidth(resize.getColumn().getWidth()+resize.getDelta());

                // dont return - after resizing the resized column, we go resize
                // the rest to always fill the table width
                // true means the delta is reset and wont accumulate
                // return true;
            }
            // handle table resize or index column

            double tw = resize.getTable().getWidth();
            double sw = getVScrollbarWidth();
            double g = 3;               // gap to prevent horizontal slider to appear

            // column index
            double W1 = calculateIndexColumnWidth();

            // column time
            double mt = getItems().stream().mapToDouble(PlaylistItem::getTimeMs).max().orElse(6000);
            Text t2 = new Text(new FormattedDuration(mt).toString());
                 t2.setFont(GUI.font.getValue());
            double W3 = t2.getLayoutBounds().getWidth() + 5;

            columnIndex.setPrefWidth(W1);
            columnTime.setPrefWidth(W3);

            List<TableColumn> cs = new ArrayList(resize.getTable().getColumns());
            TableColumn mc = isColumnVisible(NAME) ? columnName : getColumn(TITLE).orElse(null);
            if(mc!=null) {
                cs.remove(mc);
                double Σcw = cs.stream().mapToDouble(c->c.getWidth()).sum();
                mc.setPrefWidth(tw-Σcw-sw-g);
            }
            return true; // false/true, doesnt matter
        });

        // prevent selection change on right click
        addEventFilter(MOUSE_PRESSED, consumeOnSecondaryButton);

        // empty table left click -> add items
        addEventHandler(MOUSE_CLICKED, e -> {
            if (headerVisible.get() && e.getY()<getTableHeaderHeight()) return;
            if (e.getButton()==PRIMARY && e.getClickCount()==1 && getItems().isEmpty())
                        getPlaylist().addOrEnqueueFiles(true);
        });

        // move items on drag
        // setOnMouseDragged(e -> { // handler !work since java 9
        // addEventFilter(MOUSE_DRAGGED, e -> { // same here
        addEventFilter(MOUSE_DRAGGED, e -> {
            if (e.getButton()!=MouseButton.PRIMARY || !e.isControlDown()) return;
            // we cant move ites when fiter on & we cant cancel filter, user would freak out
            //  if(itemsPredicate.get()!=null) return; // unreliable as non null predicates may have no effect
            if(getItems().size()!=getItemsRaw().size()) return;

            // transform any sort (if in effect) to actual table items, we cant change order on
            // items out of natural order
            // note this is only called the 1st time (or not at all), not repeatedly
            if(itemsComparator.get()!=SAME || !getSortOrder().isEmpty()) {
                movingitems = true;
                List l = list(getItems());
                List sl = list(getSelectionModel().getSelectedIndices());
                setItemsRaw(EMPTY_LIST);    // clear items
                getSortOrder().clear();     // clear sort order
                setItemsRaw(l);             // set items back, now any sort is part of their order
                selectRows(sl,getSelectionModel()); // set selection back
                movingitems = false;
            }

            double h = getFixedCellSize();
            double dist = e.getScreenY()- last;

            int by = (int) (dist/h);
            if (by >= 1 || by <= -1) {
                last = e.getScreenY();
                moveSelectedItems(by);
            }
        });

        // set key-induced actions
        setOnKeyReleased(e -> {
            if (e.getCode() == KeyCode.ENTER) {     // play first of the selected
                if(!getSelectedItems().isEmpty())
                    getPlaylist().playItem(getSelectedItems().get(0));
            } else
            if (e.getCode() == KeyCode.DELETE) {    // delete selected
                List<PlaylistItem> p = getSelectedItemsCopy();
                getSelectionModel().clearSelection();
                getPlaylist().removeAll(p);
            } else
            if (e.getCode() == KeyCode.ESCAPE) {    // deselect
                getSelectionModel().clearSelection();
            }
        });
        setOnKeyPressed(e -> {
            if(e.isControlDown()) {
                if(e.getCode()==KeyCode.UP) {
//                    table.getFocusModel().focus(-1);
                    moveSelectedItems(-1);
                } else
                if(e.getCode()==KeyCode.DOWN) {
//                    table.getFocusModel().focus(-1);
                    moveSelectedItems(1);
                }
            }
        });

        // drag&drop from
        setOnDragDetected(e -> {
            if (e.getButton() == PRIMARY  && !e.isControlDown() && !getSelectedItems().isEmpty()
                        && isRowFull(getRowS(e.getSceneX(), e.getSceneY()))) {

                Dragboard db = startDragAndDrop(TransferMode.COPY);
                DragUtil.setItemList(getSelectedItemsCopy(),db,true);
            }
            e.consume();
        });

        // drag&drop to
        // handle drag (empty table has no rows so row drag event handlers
        // will not work, events fall through on table and we handle it here
        installDrag(
            this,PLAYLIST_PLUS,"Add to playlist after row",
            DragUtil::hasAudio,
            e -> e.getGestureSource()==this,
            e -> dropDrag(e, 0)
        );

        // scroll to playing item
        maintain(scrollToPlaying, v -> {
            if(v) {
                scrollToCenter(getItems().indexOf(playlist.getPlaying()));
            }
        });
        playlist.playingI.addListener((o,ov,nv) -> {
            if(scrollToPlaying.getValue())
                scrollToCenter(getItems().indexOf(playlist.getPlaying()));
        });

        // reflect selection for whole application
        getSelectionModel().selectedItemProperty().addListener(selItemListener);
        getSelectionModel().getSelectedItems().addListener(selItemsListener);

        // set up a nice placeholder
        setPlaceholder(new Label("Click or drag & drop files"));
    }

    /** Clears resources. Do not use this table after calling this method. */
    public void dispose() {
        d1.unsubscribe();
        getSelectionModel().selectedItemProperty().removeListener(selItemListener);
        getSelectionModel().getSelectedItems().removeListener(selItemsListener);
    }

    public Playlist getPlaylist() {
        return (Playlist) getItemsRaw();
    }

/********************************** SELECTION *********************************/

    public boolean movingitems = false;
    ChangeListener<PlaylistItem> selItemListener = (o,ov,nv) -> {
        if(movingitems) return;
        PlaylistManager.selectedItemES.push(nv);
    };
    ListChangeListener<PlaylistItem> selItemsListener = (ListChangeListener.Change<? extends PlaylistItem> c) -> {
        if(movingitems) return;
        while(c.next()) {
            PlaylistManager.selectedItemsES.push(getSelectionModel().getSelectedItems());
        }
    };

    /**
     * Moves/shifts all selected items by specified distance.
     * Selected items retain their relative positions. Items stop moving when
     * any of them hits end/start of the playlist - tems wont rotate in the playlist.
     * Selection doesnt change.
     * @param by distance to move items by. Negative moves back. Zero does nothing.
     */
    public void moveSelectedItems(int by) {
        movingitems =  true;    // lock to avoid firing selectedChange event (important)

        // get selected
        // construct new list (oldS), must not be observable (like indices)
        List<Integer> oldS = new ArrayList();
                      oldS.addAll(getSelectionModel().getSelectedIndices());
        // move in playlist
        List<Integer> newS = getPlaylist().moveItemsBy(oldS, by);
        // select back
        Util.selectRows(newS, getSelectionModel());

        movingitems = false;    // release lock
    }

/****************************** DRAG AND DROP *********************************/

    private void dropDrag(DragEvent e, int index) {
        if (DragUtil.hasAudio(e)) {
            List<Item> items = DragUtil.getAudioItems(e);
            getPlaylist().addItems(items, index);
            e.setDropCompleted(true);
            e.consume();
        }
    }

/****************************** CONTEXT MENU **********************************/

    private static final TableContextMenuR<PlaylistItem> contxt_menu = new TableContextMenuR<> (
        () -> {
            ImprovedContextMenu<List<PlaylistItem>> m = new ImprovedContextMenu();
            m.getItems().addAll(menuItem("Play items", e -> {
                    PlaylistManager.use(p -> p.playItem(m.getValue().get(0)));
                }),
                menuItem("Remove items", e -> {
                    PlaylistManager.use(p -> p.removeAll(m.getValue()));
                }),
                new Menu("Show in",null,
                    menuItems(filterMap(APP.widgetManager.getFactories(),f->f.hasFeature(SongReader.class),f->f.nameGui()),
                        f -> f,
                        f -> APP.widgetManager.use(f,NO_LAYOUT,c->((SongReader)c.getController()).read(m.getValue()))
                    )
                ),
                new Menu("Edit tags in",null,
                    menuItems(filterMap(APP.widgetManager.getFactories(),f->f.hasFeature(SongWriter.class),f->f.nameGui()),
                        f -> f,
                        f -> APP.widgetManager.use(f,NO_LAYOUT,c->((SongWriter)c.getController()).read(m.getValue()))
                    )
                ),
                menuItem("Crop items", e -> {
                    PlaylistManager.use(p -> p.retainAll(m.getValue()));
                }),
                menuItem("Duplicate items as group", e -> {
                    PlaylistManager.use(p -> p.duplicateItemsAsGroup(m.getValue()));
                }),
                menuItem("Duplicate items individually", e -> {
                    PlaylistManager.use(p -> p.duplicateItemsByOne(m.getValue()));
                }),
                menuItem("Explore items's directory", e -> {
                    Environment.browse(m.getValue().stream().filter(Item::isFileBased).map(Item::getFile));
                }),
                menuItem("Add items to library", e -> {
                    List<Metadata> items = m.getValue().stream()
                            .map(Item::toMeta)
                            .collect(Collectors.toList());
                    DB.addItems(items);
                }),
                new Menu("Search album cover",null,
                    menuItems(APP.plugins.getPlugins(HttpSearchQueryBuilder.class),
                            q -> "in " + Parser.toS(q),
                            q -> App.itemToMeta(m.getValue().get(0), i -> Environment.browse(q.apply(i.getAlbum()))))
                )
            );
            return m;
        },
        (menu,table) -> {
            List<PlaylistItem> items = ImprovedTable.class.cast(table).getSelectedItemsCopy();
            menu.setValue(items);
            if(items.isEmpty()) {
                menu.getItems().forEach(i->i.setDisable(true));
            } else {
                menu.getItems().forEach(i->i.setDisable(false));
            }
        }
    );
}