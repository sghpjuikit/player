
package GUI.objects;

import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import GUI.ContextManager;
import GUI.DragUtil;
import GUI.GUI;
import PseudoObjects.FormattedDuration;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.util.Callback;
import utilities.AudioFileFormat;
import utilities.FileUtil;
import utilities.MathUtil;
import utilities.TODO;
import utilities.TableUtil;

/**
 * Playlist table GUI component.
 * <p>
 * Supports filtering by predicate and sorting by comparator and also the bindings
 * involved.
 * <p>
 * Introduces two additional TableCell css pseudoclasses: {@link #playingRowCSS}
 * and {@link #corruptRowCSS} that style the cells containing played item and
 * corrupted item respectively.
 * <p>
 * 
 * @author uranium
 */
@TODO("dragging duplicite code for empty table case")
public final class PlaylistTable {
    // css styles for rows
    private static final PseudoClass playingRowCSS = PseudoClass.getPseudoClass("played");
    private static final PseudoClass corruptRowCSS = PseudoClass.getPseudoClass("corrupt");
    
    private final TableView<PlaylistItem> table = new TableView();
    private final TableColumn<PlaylistItem, String> columnIndex = new TableColumn<>("#");
    private final TableColumn<PlaylistItem, String> columnName = new TableColumn<>("name");
    private final TableColumn<PlaylistItem, FormattedDuration> columnTime = new TableColumn<>("time");
    
    private final Callback<TableColumn<PlaylistItem,String>, TableCell<PlaylistItem,String>> indexCellFactory;
    private final Callback<TableColumn<PlaylistItem,String>, TableCell<PlaylistItem,String>> nameCellFactory;
    private final Callback<TableColumn<PlaylistItem,FormattedDuration>, TableCell<PlaylistItem,FormattedDuration>> timeCellFactory;
    private final Callback<TableView<PlaylistItem>,TableRow<PlaylistItem>> rowFactory;
    
    private FilteredList<PlaylistItem> itemsF = new FilteredList(FXCollections.emptyObservableList());
    private SortedList<PlaylistItem> itemsS = new SortedList(itemsF);
    
    
    // properties
    boolean zero_pad = true;
    private boolean show_original_index; // when filter in effect
    Pos cell_align = Pos.CENTER_LEFT;
    
    // selection helper variables
    double last;
    ArrayList<Integer> selected_temp = new ArrayList<>();
    int clicked_row = -1;
    
    // playing item observation listeners
    private final InvalidationListener pIL = (o) -> refresh();
    private final WeakInvalidationListener playingListener = new WeakInvalidationListener(pIL);
    
    // invisible controls helping with resizing columns
    private Label tmp = new Label();
    private Label tmp2 = new Label();
    
    /**
     * Constructor.
     * Creates the table and fits it to specified parent.
     * Initializes everything needed to fully function.
     * @param parent AnchorPane container wrapping this table. Anchors will be set to 0.
     */
    public PlaylistTable (AnchorPane parent) {
        
        parent.getChildren().add(table);
        AnchorPane.setBottomAnchor(table, 0.0);
        AnchorPane.setLeftAnchor(table, 0.0);
        AnchorPane.setRightAnchor(table, 0.0);
        AnchorPane.setTopAnchor(table, 0.0);
        parent.getChildren().add(tmp);parent.getChildren().add(tmp2);
        
        // initialize table gui
        table.setTableMenuButtonVisible(true);
        table.setFixedCellSize(GUI.font.getSize() + 4);
        
        // initialize table factories
        indexCellFactory = ( column -> {
            TableCell cell = new TableCell<PlaylistItem, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (!empty) {
                        int index = show_original_index ? itemsF.getSourceIndex(getIndex()) : getIndex();
                            index++;
                        if (zero_pad)
                            setText(MathUtil.zeroPad(index, table.getItems().size()) + ".");
                        else
                            setText(String.valueOf(index) + ".");
                    } 
                    else setText("");
                }
            };
            cell.setAlignment(reverse(cell_align));
            return cell;
        });
        nameCellFactory = ( column -> {
            final TableCell cell = new TableCell<PlaylistItem,String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? "" : item);
                }
            };
            cell.setAlignment(cell_align);
            return cell;
        });
        timeCellFactory = ( column -> {
            TableCell cell = new TableCell<PlaylistItem, FormattedDuration>() {
                @Override
                protected void updateItem(FormattedDuration item, boolean empty) {
                    super.updateItem(item, empty);getStyleClass().forEach(System.out::println);
                    setText(empty ? "" : item.toString());
                }
            };
            cell.setAlignment(reverse(cell_align));
            return cell;
        });
        rowFactory = p_table -> {
            TableRow<PlaylistItem> row = new TableRow<PlaylistItem>() {
                @Override 
                protected void updateItem(PlaylistItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (!empty) {
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
                }
                // this method is workaround for initialisation bug where the
                // pseudoclasses dont initialize properly
                // it should be handled better but this is good enough for now
                @Override protected void layoutChildren() {
                    super.layoutChildren();
                    if(getItem()!=null){
                        if (PlaylistManager.isItemPlaying(getItem()))
                            getChildrenUnmodifiable().forEach(
                                    c->c.pseudoClassStateChanged(playingRowCSS, true));
                        else 
                            getChildrenUnmodifiable().forEach(
                                    c->c.pseudoClassStateChanged(playingRowCSS, false));

                        if (getItem().markedAsCorrupted())
                             getChildrenUnmodifiable().forEach(
                                     c->c.pseudoClassStateChanged(corruptRowCSS, true));
                        else 
                            getChildrenUnmodifiable().forEach(
                                    c->c.pseudoClassStateChanged(corruptRowCSS, false));
                    }
                }
            };
            
            
            // remember index of row that was clicked on
            row.setOnMousePressed( e -> {
//            if (e.getButton()!=PRIMARY) return;
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
//            if (e.getButton()!=PRIMARY) return;
                if (row.getItem() == null)
                    selectNone();
            });
            
            // handle drag from
            row.setOnDragDetected( e -> {
            if (e.getButton()!=PRIMARY) return;
                if(row.isEmpty()) return;
                if (e.isControlDown() && e.getButton() == PRIMARY) {
                    Dragboard db = table.startDragAndDrop(TransferMode.ANY);
                    DragUtil.setContent(db, new Playlist(getSelectedItems()));
                    e.consume();
                }
            });
            //support drag over transfer
            row.setOnDragOver(dragOverHandler);
            // handle drag transfer
            row.setOnDragDropped( e -> {
                int index = row.isEmpty() ? table.getItems().size() : row.getIndex();
                onDragDropped(e, index);
            });
            // handle click
            row.setOnMouseClicked( e -> {
                if(row.isEmpty()) return;
                if (e.getButton() ==  PRIMARY) {    // play item on doubleclick
                    if (e.getClickCount() == 2) {
                        PlaylistManager.playItem(row.getItem());
                    }
                    e.consume();            
                } else
                if (e.getButton() == SECONDARY) {  // show contextmenu
                    if (!PlaylistManager.isEmpty())
                        ContextManager.showMenu(ContextManager.playlistMenu, getSelectedItems());
                }
            });
            return row;
        };
        
         // set table factories
        columnIndex.setSortable(false);
        columnIndex.setCellValueFactory(new PropertyValueFactory<>("name"));
        columnIndex.setCellFactory(indexCellFactory);
        
        columnName.setCellValueFactory(new PropertyValueFactory<>("name"));
        columnName.setCellFactory(nameCellFactory);
        
        columnTime.setCellValueFactory(new PropertyValueFactory<>("time"));
//        columnTime.setCellFactory(timeCellFactory);
        
        table.setRowFactory(rowFactory);
        
        // initialize table data
        table.getColumns().setAll(columnIndex, columnName, columnTime);
        table.getColumns().forEach( t -> t.setResizable(false));
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setItems(itemsS);
        
        // resizing
        tmp.setVisible(false);tmp2.setVisible(false);
        table.setColumnResizePolicy( resize -> {
            // table
            double W = table.getWidth();

            // column 1
            // need this weird method to get 9s as their are wide (font isnt always proportional)
            int i = MathUtil.DecMin1(PlaylistManager.getItems().size());    
            tmp.setText(""); // set empty to make sure the label resizes
            tmp.setText(String.valueOf(i)+".");
            double W1 = tmp.getWidth()+5;
            
            // column 3
            tmp2.setText("");
            tmp2.setText("00:00");
            double W3 = tmp2.getWidth()+5;
            
            // slider
            double H = table.getItems().size()*table.getFixedCellSize();
            double W4 = H > table.getHeight() ? 15 : 0;
            
            // gap to prevent horizontal slider to appear
            double G = 3;
            
            columnIndex.setPrefWidth(W1);
            columnName.setPrefWidth(W-W1-W3-W4-G);
            columnTime.setPrefWidth(W3);
            return true;
        });
        // resize index column on items.size change, use weak listener
        table.getItems().addListener( (ListChangeListener.Change<? extends PlaylistItem> change) -> {
            while(change.next()) {
                if(change.wasAdded() || change.wasRemoved())
                    table.getColumnResizePolicy().call(new TableView.ResizeFeatures(table, columnIndex, 0.0));
            }
        });
        
        // handle selection
        table.setOnMousePressed( e -> {
            if (e.getButton()!=MouseButton.PRIMARY) return;
            // reselect items from remembered state
            // this overrides default behavior where mousePressed deselects all but
            // the item that was clicked on
            if (selected_temp.contains(clicked_row)) {
                TableUtil.selectRows(selected_temp, table.getSelectionModel());
            }
            
            e.consume();
        });
        // handle selection
        table.setOnMouseReleased( e -> {
            if (e.getButton()!=MouseButton.PRIMARY) return;
            // remember the indexes of selected rows
            // clone (! not copy), copying would mean that change to selected items
            // would change the remembered indexes too
            selected_temp.clear();
            for(Integer i: table.getSelectionModel().getSelectedIndices()) {
                selected_temp.add(i);
            }
            e.consume();
        });
        
        // move items on drag
        table.setOnMouseDragged( e -> {
            if (e.getButton()!=MouseButton.PRIMARY) return;
            
            double ROW = table.getFixedCellSize();
            double diff = e.getScreenY()- last; // note: use getSceneY() instead getY()
            
            int by = (int) (diff/ROW);
            if (by >= 1 || by <= -1) {
                last = e.getScreenY();
                moveSelectedItems(by);
            }
        });
        
        // set key-induced actions
        table.setOnKeyReleased( e -> {
            if (e.getCode() == KeyCode.ENTER) {     // play first of the selected
                if(!getSelectedItems().isEmpty())
                    PlaylistManager.playItem(getSelectedItems().get(0));
            } else
            if (e.getCode() == KeyCode.DELETE) {    // delete selected
                PlaylistManager.removeItems(getSelectedItems());
                table.getSelectionModel().clearSelection();
            } else
            if (e.getCode() == KeyCode.ESCAPE) {    // deselect
                table.getSelectionModel().clearSelection();
            } else
            if (e.getCode() == KeyCode.UP) {
//                moveSelectedItems(-1);
            } else
            if (e.getCode() == KeyCode.DOWN) {
//                moveSelectedItems(1);
            }
        });
        table.setOnKeyPressed( e -> {
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
        
        // dragging behavior (for empty table - it does not have any rows so
        // drag event handlers registered on rows in row factory will not work)
        table.setOnDragOver(dragOverHandler);
        // handle drag (for empty table - it does not have any rows so
        // drag event handlers registered on rows in row factory will not work)
        table.setOnDragDropped( e -> {
            if (table.getItems().isEmpty())
                onDragDropped(e, 0);
        });
        
        // reflect selection for whole application
        bindSelection();
        
        // observe and show playing item
        PlaylistManager.playingItemProperty().addListener(playingListener); // set listener
        playingListener.invalidated(PlaylistManager.playingItemProperty()); // init value
        
//        table.setSortPolicy( t -> {
//            SortedList itemsList = (SortedList) t.getItems();
//            FXCollections.sort(itemsList, itemsS.getComparator());
//            return true;
//        });
//        columnIndex.s
        
        refresh();
        
    }
    
    public TableView<PlaylistItem> getTable() {
        return table;
    }
    
    public void setVisible(boolean val) {
        table.setVisible(val);
    }
    
    /** Set visibility of button controlling columns. Default true. */
    public void setMenuButtonVisible(boolean val) {
        table.setTableMenuButtonVisible(val);
    }
    
    /** Set visibility of columns header. Default true. */
    public void setHeaderVisible(boolean val) {
        if(val) table.getStylesheets().remove(getClass().getResource("PlaylistTable.css").toExternalForm());
        else    table.getStylesheets().add(getClass().getResource("PlaylistTable.css").toExternalForm());
    }
    
    /**
     * Sets left-to-right or right-to-left or inherited (text) flow orientation.
     * Default INHERIT.
     * @param orient 
     */
    public void setNodeOrientation(NodeOrientation orient) {
        table.setNodeOrientation(orient);
    }
    /**
     * Sets alignment of content within cells
     * Default CENTER_LEFT.
     * @param val 
     */
    public void setCellAlign(Pos val) {
        cell_align = val;
        refresh();
    }
    /**
     * @param placeholder This Node is shown to the user when the table has no
     * content to show. This may be the case because the table model has no data
     * in the first place, that a filter has been applied to the table model,
     * resulting in there being nothing to show the user, or that there are no
     * currently visible columns.
     */
    public void setPlaceholder(Node placeholder) {
        table.setPlaceholder(placeholder);
    }
    
    /**
     * Will add zeros to index numbers to maintain length consistency.
     * @param val 
     */
    public void zeropadIndex(boolean val) {
        zero_pad = val;
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
        table.setRowFactory(null);
        table.setRowFactory(rowFactory);
    }
    
    /** Clears resources like listeners for this table object. */
    public void clearResources() {
        PlaylistManager.playingItemProperty().removeListener(playingListener);
        unbindSelection();
    }
    
    private Pos reverse(Pos val) {
        switch(val) {
            case BASELINE_LEFT: return Pos.BASELINE_RIGHT;
            case BOTTOM_LEFT: return Pos.BOTTOM_RIGHT;
            case CENTER_LEFT: return Pos.CENTER_RIGHT;
            case TOP_LEFT: return Pos.TOP_RIGHT;

            case BASELINE_RIGHT: return Pos.BASELINE_LEFT;
            case BOTTOM_RIGHT: return Pos.BOTTOM_LEFT;
            case CENTER_RIGHT: return Pos.CENTER_LEFT;
            case TOP_RIGHT: return Pos.TOP_LEFT;
                
            case BASELINE_CENTER:
            case BOTTOM_CENTER:
            case CENTER:
            case TOP_CENTER: return val;
            default: return val;
        }
    }
/************************************* DATA ***********************************/
    
    /**
     * Sets items for this table. The list will be observed and its changes 
     * reflected.
     * Removes any previous items. This method changes the observable list
     * backing the data of this table and might render previously returned value
     * from {@link #getItemsF} and {@link #getItemsS} outdated. 
     * @param items list of items to set as a backing data for this table.
     * If the provided list is not FilteredList it will be wrapped into one. The
     * same goes for SortedList. The final result of this method is {@link SortedList}.
     * The parameter therefore also takes Filtered or Sorted List as a parameter.
     * The proper transformation is: ObservableList wrapped by FilteredList
     * wrapped by SortedList.
     */
    public void setItems(ObservableList<PlaylistItem> items) {
        if(items instanceof SortedList) {
            itemsS = (SortedList<PlaylistItem>)items;
        }
        else {
            if(items instanceof FilteredList) {
                itemsF = (FilteredList<PlaylistItem>) items;
            }
            else itemsF = new FilteredList(items);
            itemsS = new SortedList<>(itemsF);
        }
        table.setItems(itemsS);
        itemsS.comparatorProperty().bind(table.comparatorProperty());
        refresh();
    }
    
    /** @return list of items of this table. Supports filtering. */
    public FilteredList<PlaylistItem> getItemsF() {
        return itemsF;
    }
    /** @return list of items of this table. Supports sorting. */
    public SortedList<PlaylistItem> getItemsS() {
        return itemsS;
    }
    
    /** 
     * @param true shows item's index in the observable list - source of its
     * data. False will display index within filtered list. In other words false
     * will cause items to always be indexed from 1 to items.size. This has only
     * effect when filtering the table. 
     */
    public void setShowOriginalIndex(boolean val) {
        show_original_index = val;
    }
    
    public boolean isShowOriginalIndex() {
        return show_original_index;
    }
    
    public void sortByName() {
//        itemsS.comparatorProperty().unbind();
//        table.setItems(FXCollections.emptyObservableList());
//        
//        itemsS.setComparator(PlaylistItem.getComparatorName());
////        FXCollections.sort(itemsS, PlaylistItem.getComparatorName());
//        itemsS.sort(PlaylistItem.getComparatorName());
//        
//        table.setItems(itemsS);
//        itemsS.comparatorProperty().bind(table.comparatorProperty());
        columnName.setComparator(new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                return PlaylistItem.getComparatorArtist().compare(new PlaylistItem(null, o1, last), new PlaylistItem(null, o2, last));
            }
        });
        columnName.setSortType(TableColumn.SortType.ASCENDING);
        table.sort();
        
//        FXCollections.sort(itemsF, PlaylistItem.getComparatorName());
    }
    public void sortByLength() {        
//        itemsS.comparatorProperty().unbind();
//        itemsS.setComparator(PlaylistItem.getComparatorTime());
//        itemsS.comparatorProperty().bind(table.comparatorProperty());
        
//        FXCollections.sort(itemsS, PlaylistItem.getComparatorTime());
    }
    public void sortByLocation() {
//        itemsS.comparatorProperty().unbind();
        itemsS.setComparator(PlaylistItem.getComparatorURI());
//        itemsS.comparatorProperty().bind(table.comparatorProperty());
        
//        FXCollections.sort(itemsS, PlaylistItem.getComparatorURI());
    }
    public void sortByArtist() {
//        itemsS.comparatorProperty().unbind();
        itemsS.setComparator(PlaylistItem.getComparatorArtist());
//        itemsS.comparatorProperty().bind(table.comparatorProperty());
        
//        FXCollections.sort(itemsS, PlaylistItem.getComparatorArtist());
    }
    public void sortByTitle() {
//        itemsS.comparatorProperty().unbind();
        itemsS.setComparator(PlaylistItem.getComparatorTitle());
//        itemsS.comparatorProperty().bind(table.comparatorProperty());
//        
//        FXCollections.sort(((SortableList<PlaylistItem>)table.getItems()), PlaylistItem.getComparatorTitle());
    }
    
/********************************** SELECTION *********************************/
    
    private boolean movingitems = false;
    ChangeListener<PlaylistItem> selItemListener = (o,oldV,newV)->{
        if(movingitems) return; 
        PlaylistManager.selectedItemProperty().set(newV);
    };
    ListChangeListener<PlaylistItem> selItemsListener = (ListChangeListener.Change<? extends PlaylistItem> c) -> {
        if(movingitems) return;
        while(c.next()) {
            PlaylistManager.getSelectedItems().setAll(c.getList());
        }
    };
    
    private void bindSelection() {
        table.getSelectionModel().selectedItemProperty().addListener(selItemListener);
        table.getSelectionModel().getSelectedItems().addListener(selItemsListener);
    }
    
    private void unbindSelection() {
        table.getSelectionModel().selectedItemProperty().removeListener(selItemListener);
        table.getSelectionModel().getSelectedItems().removeListener(selItemsListener);
    }
    
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
                      oldS.addAll(table.getSelectionModel().getSelectedIndices());
        // move in playlist
        List<Integer> newS = PlaylistManager.moveItemsBy(oldS, by);
        // select back
        TableUtil.selectRows(newS, table.getSelectionModel());
        
        movingitems = false;    // release lock
    }
    
    
    public List<PlaylistItem> getSelectedItems() {
        return table.getSelectionModel().getSelectedItems();
    }
    
    /**
     * Selects all playlist items.
     */
    public void selectAll() {
        table.getSelectionModel().selectAll();
    }
    /**
     * Inverses the selection of the playlist table. 
     */
    public void selectInverse() {
        List<Integer> selected = table.getSelectionModel().getSelectedIndices();
        int size = table.getItems().size();
        List<Integer> inverse = new ArrayList<>();
        for(int i=0; i<size; i++)
            if(!selected.contains(i))
                inverse.add(i);
        
        TableUtil.selectRows(inverse, table.getSelectionModel());
    }
    /**
     * Deselects all selected items in the playlist table.
     */
    public void selectNone() {
        table.getSelectionModel().clearSelection();
    }
    
/****************************** DRAG AND DROP *********************************/
    
    private final EventHandler<DragEvent> dragOverHandler =  t -> {
        // avoid illegal operation on drag&drop from self to self
        if(t.getGestureSource() == table) return;

        Dragboard d = t.getDragboard();
        // accept if contains at least 1 audio file, audio url, playlist or items
        if ( (d.hasFiles() && d.getFiles().stream().anyMatch(AudioFileFormat::isSupported)) ||
             (d.hasUrl() && AudioFileFormat.isSupported(d.getUrl())) || 
                d.hasContent(DragUtil.items) ||
                d.hasContent(DragUtil.playlist)) {
            t.acceptTransferModes(TransferMode.ANY);
            t.consume();
        }
    };
    
    private void onDragDropped(DragEvent e, int index) {
        Dragboard db = e.getDragboard();
        if (db.hasFiles()) {    // add files and folders
            List<URI> uris = new ArrayList<>();
            FileUtil.getAudioFiles(db.getFiles(), 1).stream().map(File::toURI).forEach(uris::add);
            PlaylistManager.addUris(uris);
        } else                  // add url
        if (db.hasUrl()) {                  
            String url = db.getUrl();
            PlaylistManager.addUrl(url,0);
        } else                  // add playlist items
        if (db.hasContent(DragUtil.playlist)) {
            Playlist p = DragUtil.getPlaylist(db);
            PlaylistManager.addItems(p,0); // 0 cause empty table, 0 is last item
        } else if(db.hasContent(DragUtil.items)) {
            List<Item> p = DragUtil.getItems(db);
            PlaylistManager.addItems(p);
        }

        e.setDropCompleted(true);
        e.consume();
    }
    
}