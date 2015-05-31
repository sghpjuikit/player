
package GUI.objects.Table;

import AudioPlayer.Player;
import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.PlaylistItem;
import static AudioPlayer.playlist.PlaylistItem.Field.LENGTH;
import static AudioPlayer.playlist.PlaylistItem.Field.NAME;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.services.Database.DB;
import AudioPlayer.tagging.Metadata;
import GUI.GUI;
import GUI.objects.ContextMenu.ContentContextMenu;
import GUI.objects.ContextMenu.TableContextMenuInstance;
import GUI.objects.Table.TableColumnInfo.ColumnInfo;
import GUI.objects.TableRow.ImprovedTableRow;
import Layout.Widgets.Features.TaggingFeature;
import Layout.Widgets.WidgetManager;
import static Layout.Widgets.WidgetManager.WidgetSource.NOLAYOUT;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.*;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseEvent.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import main.App;
import org.reactfx.Subscription;
import util.File.Environment;
import util.Util;
import static util.Util.*;
import util.dev.TODO;
import static util.dev.TODO.Purpose.READABILITY;
import static util.functional.Util.by;
import util.graphics.drag.DragUtil;
import util.parsing.Parser;
import util.units.FormattedDuration;
import web.HttpSearchQueryBuilder;

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
    
    private final TableColumn<PlaylistItem,String> columnName;
    private final TableColumn<PlaylistItem,FormattedDuration> columnTime;
    
    // selection helper variables
    double last;
    ArrayList<Integer> selected_temp = new ArrayList<>();
    
    // dependencies
    private final Subscription d1;
    
    public PlaylistTable () {
        super(NAME);
        
        VBox.setVgrow(this, Priority.ALWAYS);
        
        // initialize table
        setFixedCellSize(GUI.font.getValue().getSize() + 5);
        getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        // initialize column factories
        setColumnStateFacory( f -> {
            boolean visible = f==NAME || f==LENGTH;
            return new ColumnInfo(f.toString(), f.ordinal(), visible, 60);
        });
        setColumnFactory( f -> {
            TableColumn<PlaylistItem,?> c = new TableColumn(f.toString());
            c.setCellValueFactory( f==NAME || f==LENGTH
                    ? new PropertyValueFactory(f.name())
                    : cf -> cf.getValue()== null ? null : new PojoV(cf.getValue().getField(f))
            );
            c.setCellFactory(cellFactoryAligned(f.getType(), ""));
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
                onLeftDoubleClick((r,e) -> PlaylistManager.playItem(getSourceIndex(r.getIndex())));
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
                styleRuleAdd("played", PlaylistManager::isItemPlaying);
                styleRuleAdd("corrupt", PlaylistItem::markedAsCorrupted);
            }
        });
        // maintain playing item css by refreshing index column
        d1 = Player.playingtem.subscribeToChanges(o->refreshColumn(columnIndex));
        
        // resizing
        setColumnResizePolicy( rf -> {
            // handle column resize (except index)
            if(rf!=null && rf.getColumn()!=null && rf.getColumn()!=columnIndex) {
                if(getColumns().contains(columnName))
                    columnName.setPrefWidth(columnName.getWidth()-rf.getDelta());
                rf.getColumn().setPrefWidth(rf.getColumn().getWidth()+rf.getDelta());
                return false;
            }
            // handle table resize or index column
            
            // table
            double W = getWidth();

            // column 1
            double W1 = calculateIndexColumnWidth();
            
            // column 3
            double mt = getItems().stream().mapToDouble(PlaylistItem::getTimeMs).max().orElse(6000);
            Text t2 = new Text(new FormattedDuration(mt).toString());
                 t2.setFont(GUI.font.getValue());
            double W3 = t2.getLayoutBounds().getWidth() + 5;
            
            // slider
            double H = getItems().size()*getFixedCellSize();
            double S = H > getHeight()-getTableHeaderHeight() ? 15 : 0;

            // gap to prevent horizontal slider to appear
            double G = 3;
            
            List<TableColumn> other = new ArrayList(getColumns());
                              other.remove(columnIndex);
                              other.remove(columnTime);
                              other.remove(columnName);
            double W4 = other.stream().mapToDouble(c->c.getWidth()).sum();
            
            columnIndex.setPrefWidth(W1);
            columnName.setPrefWidth(W-W1-W3-W4-S-G);
            columnTime.setPrefWidth(W3);
            return false;
        });
        
        // prevent selection change on right click
        addEventFilter(MOUSE_PRESSED, consumeOnSecondaryButton);
        // addEventFilter(MOUSE_RELEASED, consumeOnSecondaryButton);
        // prevent context menu changing selection despite the above
        // addEventFilter(ContextMenuEvent.ANY, Event::consume);
        
        // empty table left click -> add items
        addEventHandler(MOUSE_CLICKED, e -> {
            if (isTableHeaderVisible() && e.getY()<getTableHeaderHeight()) return;
            if (e.getButton()==PRIMARY && e.getClickCount()==1 && getItems().isEmpty())
                        PlaylistManager.addOrEnqueueFiles(true);
        });
        
        // move items on drag
        setOnMouseDragged( e -> {
            if (e.getButton()!=MouseButton.PRIMARY || !e.isControlDown()) return;
            
            double h = getFixedCellSize();
            double dist = e.getScreenY()- last;
            
            int by = (int) (dist/h);
            if (by >= 1 || by <= -1) {
                last = e.getScreenY();
                moveSelectedItems(by);
            }
        });
        
        // set key-induced actions
        setOnKeyReleased( e -> {
            if (e.getCode() == KeyCode.ENTER) {     // play first of the selected
                if(!getSelectedItems().isEmpty())
                    PlaylistManager.playItem(getSelectedItems().get(0));
            } else
            if (e.getCode() == KeyCode.DELETE) {    // delete selected
                PlaylistManager.removeItems(getSelectedItems());
                getSelectionModel().clearSelection();
            } else
            if (e.getCode() == KeyCode.ESCAPE) {    // deselect
                getSelectionModel().clearSelection();
            }
        });
        setOnKeyPressed( e -> {
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
                DragUtil.setPlaylist(new Playlist(getSelectedItems()),db);
            }
            e.consume();
        });
        // drag&drop to
        setOnDragOver_NoSelf(DragUtil.audioDragAccepthandler);
        // handle drag (empty table has no rows so row drag event handlers
        // will not work, events fall through on table and we handle it here
        setOnDragDropped( e -> dropDrag(e, 0));
        
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
    
/************************************* SORT ***********************************/
    
    /**
     * Sorts directly the playlist data, rather than just this table.
     * <p>
     * {@inheritDoc} */
    @TODO(purpose = TODO.Purpose.ILL_DEPENDENCY, note = "Normally, we want to just"
            + "sort getItemsRaw(), but then we get desynced with playlist, which"
            + "can cause problems for index dependent operations."
            + "Thats because filteredTable does not take observable list as input"
            + "and has its own, thus separating it from PlaylistManager' data"
            + "We need to decouple playlist tables (all are 'linked' now because"
            + "of this) and remove PlaylistManager alltogether - modularize it"
            + "into playlist tables only.")
    @Override
    public void sortBy(PlaylistItem.Field f) {
        getSortOrder().clear();
        PlaylistManager.getItems().sort(by(p -> (Comparable) p.getField(f)));
    }
    
/********************************** SELECTION *********************************/
    
    private boolean movingitems = false;
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
        List<Integer> newS = PlaylistManager.moveItemsBy(oldS, by);
        // select back
        Util.selectRows(newS, getSelectionModel());
        
        movingitems = false;    // release lock
    }
    
    /**
     * Selects all playlist items.
     */
    public void selectAll() {
        getSelectionModel().selectAll();
    }
    /**
     * Inverses the selection of the playlist table. 
     */
    public void selectInverse() {
        List<Integer> selected = getSelectionModel().getSelectedIndices();
        int size = getItems().size();
        List<Integer> inverse = new ArrayList<>();
        for(int i=0; i<size; i++)
            if(!selected.contains(i))
                inverse.add(i);
        
        Util.selectRows(inverse, getSelectionModel());
    }
    /**
     * Deselects all selected items in the playlist table.
     */
    public void selectNone() {
        getSelectionModel().clearSelection();
    }
    
/****************************** DRAG AND DROP *********************************/
    
    private void dropDrag(DragEvent e, int index) {
        if (DragUtil.hasAudio(e.getDragboard())) {
            List<Item> items = DragUtil.getAudioItems(e);
            PlaylistManager.addItems(items, index);
            //end drag transfer
            e.setDropCompleted(true);
            e.consume();
        }
    }
    
/****************************** CONTEXT MENU **********************************/
    
    private static final TableContextMenuInstance<PlaylistItem> contxt_menu = new TableContextMenuInstance<> (
        () -> {
            ContentContextMenu<List<PlaylistItem>> m = new ContentContextMenu();
            m.getItems().addAll(menuItem("Play items", e -> {
                    PlaylistManager.playItem(m.getValue().get(0));
                }),
                menuItem("Remove items", e -> {
                    PlaylistManager.removeItems(m.getValue());
                }),
                menuItem("Edit the item/s in tag editor", e -> {
                    WidgetManager.use(TaggingFeature.class,NOLAYOUT, w->w.read(m.getValue()));
                }),
                menuItem("Crop items", e -> {
                    PlaylistManager.retainItems(m.getValue());
                }),
                menuItem("Duplicate items as group", e -> {
                    PlaylistManager.duplicateItemsAsGroup(m.getValue());
                }),
                menuItem("Duplicate items individually", e -> {
                    PlaylistManager.duplicateItemsByOne(m.getValue());
                }),
                menuItem("Explore items's directory", e -> {
                    List<File> files = m.getValue().stream()
                            .filter(Item::isFileBased)
                            .map(Item::getLocation)
                            .collect(Collectors.toList());
                    Environment.browse(files,true);
                }),
                menuItem("Add items to library", e -> {
                    List<Metadata> items = m.getValue().stream()
                            .map(Item::toMeta)
                            .collect(Collectors.toList());
                    DB.addItems(items);
                }),
                new Menu("Search album cover",null,
                    menuItems(App.plugins.getPlugins(HttpSearchQueryBuilder.class), 
                            q -> "in " + Parser.toS(q),
                            q -> Environment.browse(q.apply(m.getValue().get(0).getMetadata().getAlbum())))
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