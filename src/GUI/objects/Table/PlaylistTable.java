
package GUI.objects.Table;

import AudioPlayer.Player;
import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.PlaylistItem;
import static AudioPlayer.playlist.PlaylistItem.Field.NAME;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.services.Database.DB;
import AudioPlayer.tagging.FormattedDuration;
import AudioPlayer.tagging.Metadata;
import GUI.DragUtil;
import GUI.GUI;
import GUI.objects.ContextMenu.ContentContextMenu;
import GUI.objects.ContextMenu.TableContextMenuInstance;
import Layout.Widgets.Features.TaggingFeature;
import Layout.Widgets.WidgetManager;
import static Layout.Widgets.WidgetManager.WidgetSource.NOLAYOUT;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.ResizeFeatures;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.reactfx.Subscription;
import util.Parser.File.Enviroment;
import util.TODO;
import static util.TODO.Purpose.READABILITY;
import util.Util;
import static util.Util.createmenuItem;
import static util.Util.selectRows;

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
    
    private final TableColumn<PlaylistItem,String> columnName = new TableColumn("name");
    private final TableColumn<PlaylistItem,FormattedDuration> columnTime = new TableColumn("time");
    
    private final Callback<TableView<PlaylistItem>,TableRow<PlaylistItem>> rowFactory;
    
    // selection helper variables
    double last;
    ArrayList<Integer> selected_temp = new ArrayList<>();
    int clicked_row = -1;
    
    // playing item observation listeners
    Subscription playintItemMonitor = Player.playingtem.subscribeToChanges(o->refresh());
    
    // invisible controls helping with resizing columns
    private Label tmp = new Label();
    private Label tmp2 = new Label();
    
    public PlaylistTable () {
        super(NAME);
        
        // stupid workaround for having to put the tmp,tmp2 labels somewhere on the scenegrapgh...
        AnchorPane a = new AnchorPane(this, tmp,tmp2);
        Util.setAPAnchors(this, 0);
        root.getChildren().add(a);
        VBox.setVgrow(a, Priority.ALWAYS);
        
        // initialize table gui
        setTableMenuButtonVisible(false);
        setFixedCellSize(GUI.font.getValue().getSize() + 5);
        
        // initialize factories
        rowFactory = p_table -> {
            TableRow<PlaylistItem> row = new TableRow<PlaylistItem>() {
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
            };
            
            
            // remember index of row that was clicked on
            row.setOnMousePressed( e -> {
                // remember position for moving selected rows on mouse drag
                last = e.getScreenY();
            
                if (row.getItem() == null)
//                    selectNone();
                    clicked_row = -1;
                else
                    clicked_row = row.getIndex();
            });
            // clear table selection on mouse released if no item
            row.setOnMouseReleased( e -> {
                if (row.getItem() == null)
                    selectNone();
            });
            // handle drag transfer
            row.setOnDragDropped( e -> 
                onDragDropped(e, row.isEmpty() ? getItems().size() : row.getIndex())
            );
            return row;
        };
        
         // set table factories
        columnName.setCellValueFactory(new PropertyValueFactory("name"));
        columnTime.setCellValueFactory(new PropertyValueFactory("time"));
        setRowFactory(rowFactory);
        
        // initialize table data
        getColumns().setAll(columnIndex, columnName, columnTime);
        getColumns().forEach( t -> t.setResizable(false));
        getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        // resizing
        tmp.setVisible(false);tmp2.setVisible(false);
        setColumnResizePolicy( resize -> {
            // table
            double W = getWidth();

            // column 1
            // need this weird method to get 9s as their are wide chars
            // (font isnt always proportional)
            int s = isShowOriginalIndex() ? getItemsRaw().size() : getItems().size();
            int i = Util.DecMin1(s);
            tmp.setText(String.valueOf(i)+".");
            tmp.setVisible(true);
            double W1 = tmp.getWidth();
            tmp.setVisible(false);
            // column 3
            tmp2.setText("00:00");
            double W3 = tmp2.getWidth() + 4;
            
            // slider
            double H = getItems().size()*getFixedCellSize();
            double W4 = H > getHeight() ? 15 : 0;

            // gap to prevent horizontal slider to appear
            double G = 3;

            columnIndex.setPrefWidth(W1);
            columnName.setPrefWidth(W-W1-W3-W4-G);
            columnTime.setPrefWidth(W3);
            return true;
        });
        
        // handle selection
        setOnMousePressed( e -> {
            if (e.getY()<getFixedCellSize()) return;
            if (e.getButton() != PRIMARY) return;
            // reselect items from remembered state
            // this overrides default behavior where mousePressed deselects all but
            // the item that was clicked on
            if (selected_temp.contains(clicked_row)) {
                selectRows(selected_temp, getSelectionModel());
            }
            e.consume();
        });
        // handle selection
        setOnMouseReleased( e -> {
            if (e.getY()<getFixedCellSize()) return;
            if (e.getButton() != PRIMARY) return;
            // remember the indexes of selected rows
            // clone (! not copy), copying would mean that change to selected items
            // would change the remembered indexes too
            selected_temp.clear();
            for(Integer i: getSelectionModel().getSelectedIndices()) {
                selected_temp.add(i);
            }
            e.consume();
        });
        // handle click
        setOnMouseClicked( e -> {
            if (e.getY()<getFixedCellSize()) return;
            // play item on doubleclick
            if (e.getButton() == PRIMARY) { 
                if (e.getClickCount() == 1) {
                    if(getItems().isEmpty())
                        PlaylistManager.addOrEnqueueFiles(true);
                }
                if (e.getClickCount() == 2) {
                    int i = getSelectionModel().getSelectedIndex();
                    int real_i = getItemsFiltered().getSourceIndex(i);
                    PlaylistManager.playItem(real_i);
                }           
            } else
            // show contextmenu
            if (e.getButton() == SECONDARY)
                contxt_menu.show(this, e);
            
            e.consume();
        });
        
        // move items on drag
        setOnMouseDragged( e -> {
            if (e.getButton()!=MouseButton.PRIMARY) return;
            
            double ROW = getFixedCellSize();
            double diff = e.getScreenY()- last;
            
            int by = (int) (diff/ROW);
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
        // handle drag from - copy selected items
        setOnDragDetected( e -> {
            if (e.isControlDown() && e.getButton() == PRIMARY) {
                Dragboard db = startDragAndDrop(TransferMode.COPY);
                DragUtil.setPlaylist(new Playlist(getSelectedItems()),db);
                e.consume();
            }
        });
        //support drag over transfer - paste items
        setOnDragOver(dragOverHandler);
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
        
        refresh();
    }
    

    
/******************************************************************************/     
    
    public void enqueueItem(String url) {
        PlaylistManager.addUrl(url);
    }
    public void enqueueItem(String url, int at) {
        PlaylistManager.addUrl(url, at);
    }
    public void enqueueItems(List<URI> uris) {
        PlaylistManager.addUris(uris); 
    }
    public void enqueueItems(List<URI> uris, int at) {
        PlaylistManager.addUris(uris, at); 
    }
    
    /**
     * Refreshes the table.
     * This method should be got rid of, but...
     * Current implementation should have absolutely no negative performance impact
     */
    public void refresh() {
        // force 'refresh'
        setRowFactory(null);
        setRowFactory(rowFactory);
        getColumnResizePolicy().call(new ResizeFeatures(this, columnIndex, 0.0));
    }
    
    /** Clears resources like listeners for this table object. */
    public void clearResources() {
        playintItemMonitor.unsubscribe();
        getSelectionModel().selectedItemProperty().removeListener(selItemListener);
        getSelectionModel().getSelectedItems().removeListener(selItemsListener);
    }
    
/************************************* DATA ***********************************/
        
    public void sortByName() {
        getItemsSorted().comparatorProperty().unbind();
        getItemsSorted().setComparator(PlaylistItem.getComparatorName());
    }
    public void sortByLength() {        
        getItemsSorted().comparatorProperty().unbind();
        getItemsSorted().setComparator(PlaylistItem.getComparatorTime());
//        itemsS.comparatorProperty().bind(table.comparatorProperty());
    }
    public void sortByLocation() {
        getItemsSorted().comparatorProperty().unbind();
        getItemsSorted().setComparator(PlaylistItem.getComparatorURI());
//        itemsS.comparatorProperty().bind(table.comparatorProperty());
    }
    public void sortByArtist() {
        getItemsSorted().comparatorProperty().unbind();
        getItemsSorted().setComparator(PlaylistItem.getComparatorArtist());
//        itemsS.comparatorProperty().bind(table.comparatorProperty());
    }
    public void sortByTitle() {
        getItemsSorted().comparatorProperty().unbind();
        getItemsSorted().setComparator(PlaylistItem.getComparatorTitle());
//        itemsS.comparatorProperty().bind(table.comparatorProperty());
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
    
    private final EventHandler<DragEvent> dragOverHandler =  e -> {
        // avoid illegal operation on drag&drop from self to self
        if(e.getGestureSource() != this)
            // rest of the logic is common, leave to existing implementation
            DragUtil.audioDragAccepthandler.handle(e);
    };
    
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
                            .map(Item::toMetadata)
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