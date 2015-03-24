
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
import GUI.DragUtil;
import GUI.GUI;
import GUI.objects.ContextMenu.ContentContextMenu;
import GUI.objects.ContextMenu.TableContextMenuInstance;
import GUI.objects.Table.TableColumnInfo.ColumnInfo;
import Layout.Widgets.Features.TaggingFeature;
import Layout.Widgets.WidgetManager;
import static Layout.Widgets.WidgetManager.WidgetSource.NOLAYOUT;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.event.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.*;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import static org.reactfx.EventStreams.changesOf;
import org.reactfx.Subscription;
import util.File.Enviroment;
import util.Util;
import static util.Util.*;
import util.dev.TODO;
import static util.dev.TODO.Purpose.READABILITY;
import static util.functional.Util.cmpareBy;
import util.units.FormattedDuration;

/**
 * Playlist table GUI component.
 * <p>
 * Introduces two additional TableCell css pseudoclasses: {@link #playingRowCSS}
 * and {@link #corruptRowCSS} that style the cells containing played item and
 * corrupted item respectively.
 * 
 * @author uranium
 */
@TODO(purpose = READABILITY, note = "dragging duplicite code for empty table case")
public final class PlaylistTable extends FilteredTable<PlaylistItem,PlaylistItem.Field> {
    
    // css styles for rows
    private static final PseudoClass playingRowCSS = PseudoClass.getPseudoClass("played");
    private static final PseudoClass corruptRowCSS = PseudoClass.getPseudoClass("corrupt");
    
    private final TableColumn<PlaylistItem,String> columnName;
    private final TableColumn<PlaylistItem,FormattedDuration> columnTime;
    
    // selection helper variables
    double last;
    ArrayList<Integer> selected_temp = new ArrayList<>();
    
    // dependencies
    Subscription d1 = Player.playingtem.subscribeToChanges(o->refreshColumn(columnIndex));
    Subscription d2;
    
    // invisible controls helping with resizing columns
    private Label tmp = new Label();
    private Label tmp2 = new Label();
    
    public PlaylistTable () {
        super(NAME);
            
        // stupid workaround for having to put the tmp,tmp2 labels somewhere on the scenegrapgh...
        AnchorPane a = new AnchorPane(this, tmp,tmp2);
        setAnchors(this, 0);
        root.getChildren().add(a);
        VBox.setVgrow(a, Priority.ALWAYS);
        
        // initialize table
        setTableMenuButtonVisible(false);
        setFixedCellSize(GUI.font.getValue().getSize() + 5);
        getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        // initialize column factories
        setColumnStateFacory( f -> {
            boolean visible = f==NAME || f==LENGTH;
            return new ColumnInfo(f.toString(), f.ordinal(), visible, 60);
        });
        setColumnFactory( f -> {
            TableColumn<PlaylistItem,?> c = new TableColumn(f.toString());
            if(f==NAME || f==LENGTH) {
                c.setCellValueFactory(new PropertyValueFactory(f.name()));
            } else {
                c.setCellValueFactory( cf -> {
                    if(cf.getValue()==null) return null;
                    return new ReadOnlyObjectWrapper(cf.getValue().getField(f));
                });
            }
            c.setCellFactory(cellFactoryAligned(f.getType(), ""));
            c.setUserData(f);
            c.setResizable(true);
            return c;
        });
        setColumnState(getDefaultColumnInfo());
        columnName = (TableColumn) getColumn(NAME).get();
        columnTime = (TableColumn) getColumn(LENGTH).get();
        
        // initialize row factories
        setRowFactory(t -> new TableRow<PlaylistItem>() {
            {
                setOnMousePressed( e -> {
                    // remember position for moving selected rows on mouse drag
                    last = e.getScreenY();
                });
                // clear table selection on mouse released if no item
                setOnMouseReleased( e -> {
                    if (getItem() == null)
                        selectNone();
                });
                // handle drag transfer
                setOnDragDropped( e -> 
                    onDragDropped(e, isEmpty() ? getItems().size() : getIndex())
                );
            }
            private void updatePseudoclassState(PlaylistItem item, boolean empty) {
                if (empty) return;
                // set pseudoclass
                // normal pseudoClassStateChanged(playingRowCSS, false); doesnt work here faithfully
                // since the content is within cells themselves - the pseudoclass has to be passed down
                // if we want the content (like text, not just the cell) to be styled correctly
                if (PlaylistManager.isItemPlaying(item))
                    getChildrenUnmodifiable().forEach(
                            c->c.pseudoClassStateChanged(playingRowCSS, true));
                else 
                    getChildrenUnmodifiable().forEach(
                            c->c.pseudoClassStateChanged(playingRowCSS, false));

                if (item.markedAsCorrupted())
                     getChildrenUnmodifiable().forEach(
                             c->c.pseudoClassStateChanged(corruptRowCSS, true));
                else 
                    getChildrenUnmodifiable().forEach(
                            c->c.pseudoClassStateChanged(corruptRowCSS, false));
            }
            @Override 
            protected void updateItem(PlaylistItem item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty) updatePseudoclassState(item, empty);
            }
            // this method is workaround for initialisation bug where the
            // pseudoclasses dont initialize properly
            // it should be handled better but this is good enough for now
            @Override protected void layoutChildren() {
                super.layoutChildren();
                updatePseudoclassState(getItem(), isEmpty());
            }
        });
        
        // resizing
        tmp.setVisible(false);
        tmp2.setVisible(false);
        setColumnResizePolicy( rf -> {
            // handle column resize
            if(rf!=null && rf.getColumn()!=null) {
                if(getColumns().contains(columnName))
                    columnName.setPrefWidth(columnName.getWidth()-rf.getDelta());
                rf.getColumn().setPrefWidth(rf.getColumn().getWidth()+rf.getDelta());
                return true;
            }
            // handle table resize
            
            // table
            double W = getWidth();

            // column 1
            // need this weird method to get 9s as their are wide chars
            // (font isnt always proportional)
            int s = getLastIndex();
            int i = Util.decMin1(s);
            tmp.setText(i + ".");
            double W1 = tmp.getWidth() + 5; // 4 is enough
            // column 3
            tmp2.setText("00:00");
            double W3 = tmp2.getWidth() + 5; // 4 is enough
            
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
            return true;
        });
        
        // prevent selection change on right click
        addEventFilter(MOUSE_PRESSED, consumeOnSecondaryButton);
        addEventFilter(MOUSE_RELEASED, consumeOnSecondaryButton);
        // prevent context menu changing selection despite the above
        addEventFilter(ContextMenuEvent.ANY, Event::consume);
        
        // handle click
        addEventHandler(MOUSE_CLICKED, e -> {
            if (isTableHeaderVisible() && e.getY()<getTableHeaderHeight()) return;
            if (e.getButton() == PRIMARY) { 
                // add new items if empty
                if (e.getClickCount() == 1) {
                    if(getItems().isEmpty())
                        PlaylistManager.addOrEnqueueFiles(true);
                }
                // play item on doubleclick
                if (e.getClickCount() == 2) {
                    if(!getSelectionModel().isEmpty()) {
                        int i = getSelectionModel().getSelectedIndex();
                        int j = getItemsFiltered().getSourceIndex(i);
                        PlaylistManager.playItem(j);
                    }
                }           
            } else
            if(e.getButton()==SECONDARY) {
                // prepare selection for context menu
                int i = getRow(e.getY());
                if(!getSelectionModel().isSelected(i))
                    getSelectionModel().clearAndSelect(i);
                // show context menu
                contxt_menu.show(this, e);
                e.consume();
            }
            e.consume();
        });
        
        // move items on drag
        setOnMouseDragged( e -> {
            if (e.getButton()!=MouseButton.PRIMARY) return;
            
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
            if (e.isControlDown() && e.getButton() == PRIMARY 
                    && !getSelectedItems().isEmpty()
                        && isRowFull(getRowS(e.getSceneX(), e.getSceneY()))) {
                
                Dragboard db = startDragAndDrop(TransferMode.COPY);
                DragUtil.setPlaylist(new Playlist(getSelectedItems()),db);
            }
            e.consume();
        });
        // drag&drop to
        setOnDragOver(e -> {
            if(e.getGestureSource()!= this)
                DragUtil.audioDragAccepthandler.handle(e);
        });
        // handle drag (for empty table - it does not have any rows so
        // drag event handlers registered on rows in row factory will not work)
        // in case table is not empty. row's respective handler will handle this
        setOnDragDropped( e -> {
            if (getItems().isEmpty())
                onDragDropped(e, 0);
        });
        
        // reflect selection for whole application
        getSelectionModel().selectedItemProperty().addListener(selItemListener);
        getSelectionModel().getSelectedItems().addListener(selItemsListener);
        
        // set up a nice placeholder
        setPlaceholder(new Label("Click or drag & drop files"));
        
        refreshColumn(columnIndex);
        
        // maintain columns width
        d2 = changesOf(getItems()).subscribe(c -> getColumnResizePolicy().call(null));
    }

    /** Clears resources like listeners for this table object. */
    public void clearResources() {
        d1.unsubscribe();
        d2.unsubscribe();
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
        PlaylistManager.getItems().sort(cmpareBy(p -> (Comparable) p.getField(f)));
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
    
    private void onDragDropped(DragEvent e, int index) {
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
            m.getItems().addAll(
                createmenuItem("Play items", e -> {
                    PlaylistManager.playItem(m.getValue().get(0));
                }),
                createmenuItem("Remove items", e -> {
                    PlaylistManager.removeItems(m.getValue());
                }),
                createmenuItem("Edit the item/s in tag editor", e -> {
                    WidgetManager.use(TaggingFeature.class,NOLAYOUT, w->w.read(m.getValue()));
                }),
                createmenuItem("Crop items", e -> {
                    PlaylistManager.retainItems(m.getValue());
                }),
                createmenuItem("Duplicate items as group", e -> {
                    PlaylistManager.duplicateItemsAsGroup(m.getValue());
                }),
                createmenuItem("Duplicate items individually", e -> {
                    PlaylistManager.duplicateItemsByOne(m.getValue());
                }),
                createmenuItem("Explore items's directory", e -> {
                    List<File> files = m.getValue().stream()
                            .filter(Item::isFileBased)
                            .map(Item::getLocation)
                            .collect(Collectors.toList());
                    Enviroment.browse(files,true);
                }),
                createmenuItem("Add items to library", e -> {
                    List<Metadata> items = m.getValue().stream()
                            .map(Item::toMeta)
                            .collect(Collectors.toList());
                    DB.addItems(items);
                })
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