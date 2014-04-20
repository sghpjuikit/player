
package GUI.objects;

import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import GUI.ContextManager;
import GUI.DragUtil;
import GUI.GUI;
import PseudoObjects.FormattedDuration;
import PseudoObjects.TODO;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
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
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.util.Callback;
import utilities.FileUtil;
import utilities.MathUtil;
import utilities.TableUtil;

/**
 * Fully standalone playlist table GUI component.
 * Automatically binds itself to player's data.
 * 
 * @author uranium
 */
@TODO("dragging duplicite code for empty table case" +
      "applying played,currupt css pseudo classes in row factory instead of column factory") // table-row-cell css not working or sth
public final class PlaylistTable {
    // css styles for rows
    private static final PseudoClass playingRowCSS = PseudoClass.getPseudoClass("played");
    private static final PseudoClass corruptRowCSS = PseudoClass.getPseudoClass("corrupt");
    
    private final TableView<PlaylistItem> table;
    private final TableColumn<PlaylistItem, String> columnIndex = new TableColumn<>("#");
    private final TableColumn<PlaylistItem, String> columnName = new TableColumn<>("name");
    private final TableColumn<PlaylistItem, FormattedDuration> columnTime = new TableColumn<>("time");
    
    private final Callback<TableColumn<PlaylistItem,String>, TableCell<PlaylistItem,String>>indexCellFactory;
    private final Callback<TableColumn<PlaylistItem,String>, TableCell<PlaylistItem,String>> nameCellFactory;
    private final Callback<TableColumn<PlaylistItem,FormattedDuration>, TableCell<PlaylistItem,FormattedDuration>> timeCellFactory;
    
    // properties
    boolean zero_pad = true;
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
    
    private void setCellStyle(TableCell cell) {
        if (cell.getIndex() == PlaylistManager.indexOfPlaying())
             cell.pseudoClassStateChanged(playingRowCSS, true);
        else cell.pseudoClassStateChanged(playingRowCSS, false);
        
        if (table.getItems().get(cell.getIndex()).markedAsCorrupted())
             cell.pseudoClassStateChanged(corruptRowCSS, true);
        else cell.pseudoClassStateChanged(corruptRowCSS, false);
    }
    
    /**
     * Constructor.
     * Creates the table and fits it to specified parent.
     * Initializes everything needed to fully function.
     * @param parent AnchorPane container wrapping this table. Anchors will be set to 0.
     */
    public PlaylistTable (AnchorPane parent) {
        table = new TableView();
        parent.getChildren().add(table);
        AnchorPane.setBottomAnchor(table, 0.0);
        AnchorPane.setLeftAnchor(table, 0.0);
        AnchorPane.setRightAnchor(table, 0.0);
        AnchorPane.setTopAnchor(table, 0.0);
        parent.getChildren().add(tmp);parent.getChildren().add(tmp2);
        
        // initialize table gui
        table.setTableMenuButtonVisible(true);
        table.setFixedCellSize(GUI.font.getSize() + 4);
        
        //initiaize factories
        indexCellFactory = new Callback<TableColumn<PlaylistItem, String>, TableCell<PlaylistItem, String>>() {
            @Override
            public TableCell<PlaylistItem, String> call(TableColumn<PlaylistItem, String> p) {
                TableCell<PlaylistItem, String> cell = new TableCell() {
                    @Override
                    protected void updateItem(Object item, boolean empty) {
                        super.updateItem(item, empty);
                        if (!empty) {
                            if (zero_pad)
                                setText(MathUtil.zeroPad(getIndex()+1, table.getItems().size()) + ".");
                            else
                                setText(String.valueOf(getIndex()+1) + ".");
                            
                            setCellStyle(this);
                        } else
                            setText("");
                    }
                };
                cell.setAlignment(reverse(cell_align));
                return cell;
            }
        };
        nameCellFactory = new Callback<TableColumn<PlaylistItem, String>, TableCell<PlaylistItem, String>>() {
            @Override
            public TableCell<PlaylistItem, String> call(TableColumn<PlaylistItem, String> p) {
                final TableCell<PlaylistItem, String>  cell = new TableCell() {
                    @Override
                    protected void updateItem(Object item, boolean empty) {
                        super.updateItem(item, empty);
                        if (!empty) {
                            setText(item.toString());
                            setCellStyle(this);
                        } else
                            setText("");
                    }
                };
                cell.setAlignment(cell_align);
                return cell;
            }
        };
        timeCellFactory = new Callback<TableColumn<PlaylistItem, FormattedDuration>, TableCell<PlaylistItem, FormattedDuration>>() {
            @Override
            public TableCell<PlaylistItem, FormattedDuration> call(TableColumn<PlaylistItem, FormattedDuration> p) {
                TableCell<PlaylistItem, FormattedDuration> cell = new TableCell() {
                    @Override
                    protected void updateItem(Object item, boolean empty) {
                        super.updateItem(item, empty);
                        if (!empty) {
                            setText(item.toString());
                            setCellStyle(this);
                        } else
                            setText("");
                    }
                };
                cell.setAlignment(reverse(cell_align));
                return cell;
            }
        };
        
         //initiaize table columns
        columnIndex.setSortable(false);
        columnIndex.setCellValueFactory(new PropertyValueFactory<>("name"));
        columnIndex.setCellFactory(indexCellFactory);
        
        columnName.setCellValueFactory(new PropertyValueFactory<>("name"));
        columnName.setCellFactory(nameCellFactory);
        
        columnTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        columnTime.setCellFactory(timeCellFactory);
        
        // initialize table data
        table.getColumns().setAll(columnIndex, columnName, columnTime);
        table.getColumns().forEach( t -> t.setResizable(false));
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setItems(PlaylistManager.getItems());
        
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
            double W1 = tmp.getWidth()+4;
            
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
            return false;
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
            // remember position for moving selected rows on mouse drag
            last = e.getSceneY();
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
            updateSelected();
            e.consume();
        });
        
        // move items on drag
        table.setOnMouseDragged( e -> {
            if (e.getButton()!=MouseButton.PRIMARY) return;
            double ROW = table.getFixedCellSize();
            double diff = e.getSceneY() - last;// note: use getSceneY() instead getY()
            int by = (int) (diff/ROW);
            // 'by' taking on values > 1 is very effecting catching
            // increases performance from very poor to very good
            if (by >= 1 || by <= -1) {
                last = e.getSceneY();
                moveSelectedItems((int)Math.signum(by));
//                moveSelectedItems(by); // fast like hell, but bugged (changes order of unselected)
//                                       // disabled until moving really supports by>|1|
            }
        });
        
        // set key-induced actions
        table.setOnKeyReleased( e -> {
            if (e.getCode() == KeyCode.ENTER) {  // play first of the selected
                PlaylistManager.playItem(PlaylistManager.getSelectedItems().get(0));
            } else
            if (e.getCode() == KeyCode.DELETE) { // delete selected
                PlaylistManager.removeSelectedItems();
            } else
            if (e.getCode() == KeyCode.ESCAPE) { // deselect
                selectNone();
            } else
            if (e.getCode() == KeyCode.A && e.isControlDown()) {    // select all
                updateSelected();
            } else
            if (e.getCode() == KeyCode.UP) {
                updateSelected();
            } else
            if (e.getCode() == KeyCode.DOWN) {
                updateSelected();
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
        

        // dragging behavior (for nonempty table)
        table.setRowFactory( pt -> {
            TableRow<PlaylistItem> row = new TableRow<PlaylistItem>() {
                // WHY IS THIS NOT WORKING ?
//                @Override
//                protected void updateItem(PlaylistItem item, boolean empty) {
//                    super.updateItem(item, empty);
//                    if (!empty) {
//                        if (getIndex()==PlaylistManager.indexOfPlaying())
//                            pseudoClassStateChanged(playingRowCSS, true);
//                        if (item.isCorrupt())
//                            pseudoClassStateChanged(corruptRowCSS, true);
//                    }
//                }                
            };
            
            // remember index of row that was clicked on
            row.setOnMousePressed( e -> {
            if (e.getButton()!=MouseButton.PRIMARY) return;
                if (row.getItem() == null)
                    selectNone();
                else
                    clicked_row = row.getIndex();
            });
            // clear table selection on mouse released if no item
            row.setOnMouseReleased( e -> {
            if (e.getButton()!=MouseButton.PRIMARY) return;
                if (row.getItem() == null)
                    selectNone();
            });
            
            // handle drag
            row.setOnDragDetected( e -> {
            if (e.getButton()!=MouseButton.PRIMARY) return;
                if(row.isEmpty()) return;
                if (!e.isControlDown() && e.getButton() != MouseButton.SECONDARY) return;
                Dragboard db = table.startDragAndDrop(TransferMode.ANY);
                DragUtil.setContent(db, new Playlist(table.getSelectionModel().getSelectedItems()));
                e.consume();
            });
            //support drag transfer
            row.setOnDragOver( t -> {
                if(row.isEmpty()) return;
                if (t.getGestureSource() == table) return;
                Dragboard db = t.getDragboard();
                if (db.hasFiles() || db.hasUrl() || db.hasContent(DragUtil.items) ||
                        db.hasContent(DragUtil.playlist))
                    t.acceptTransferModes(TransferMode.ANY);
                t.consume();
            });
            // handle drag transfer
            row.setOnDragDropped( t -> {
                if(row.isEmpty()) return;
                Dragboard db = t.getDragboard();
                if (db.hasFiles()) {
                    List<URI> uris = new ArrayList<>();
                    FileUtil.getAudioFiles(db.getFiles(), 1).stream().map(File::toURI).forEach(uris::add);
                    enqueueItems(uris, row.getIndex());
                } else if (db.hasUrl()) {
                    String url = db.getUrl();
                    enqueueItem(url, row.getIndex());
                } else if (db.hasContent(DragUtil.playlist)) {
                    Playlist pl = DragUtil.getPlaylist(db);
                    PlaylistManager.addItems(pl, row.getIndex());
                } else if (db.hasContent(DragUtil.items)) {
                    List<Item> pl = DragUtil.getItems(db);
                    PlaylistManager.addItems(pl, row.getIndex());
                }
                t.consume();
            });
            // handle click
            row.setOnMouseClicked( e -> {
                if(row.isEmpty()) return;
                if (e.getButton() ==  MouseButton.PRIMARY) {    // play content on doubleclick
                    if (e.getClickCount() == 2) {
                        PlaylistManager.playSelectedItem();
                    }
                    e.consume();            
                } else
                if (e.getButton() == MouseButton.SECONDARY) {  // show contextmenu
                    if (!PlaylistManager.isEmpty())
                        ContextManager.showMenu(ContextManager.playlistMenu,PlaylistManager.getSelectedItems());
                }
            });
            return row;
        });
        
        // dragging behavior (for empty table)
        table.setOnDragOver( e -> {
            if (e.getGestureSource() == table) return;
            if (table.getItems().isEmpty()) {
                Dragboard db = e.getDragboard();
                if (db.hasFiles() || db.hasUrl() || db.hasContent(DragUtil.items) ||
                        db.hasContent(DragUtil.playlist))
                    e.acceptTransferModes(TransferMode.ANY);
            }
        });
        // handle drag (for empty table)
        table.setOnDragDropped( t -> {
            if (table.getItems().isEmpty()) {
                Dragboard db = t.getDragboard();
                if (db.hasFiles()) {    // add files and folders
                    List<URI> uris = new ArrayList<>();
                    FileUtil.getAudioFiles(db.getFiles(), 1).stream().map(File::toURI).forEach(uris::add);
                    enqueueItems(uris);
                } else                  // add url
                if (db.hasUrl()) {                  
                    String url = db.getUrl();
                    enqueueItem(url);
                } else                  // add playlist items
                if (db.hasContent(DragUtil.playlist)) {
                    Playlist p = DragUtil.getPlaylist(db);
                    PlaylistManager.addItems(p,0); // 0 cause empty table, 0 is last item
                } else if(db.hasContent(DragUtil.items)) {
                    List<Item> p = DragUtil.getItems(db);
                    PlaylistManager.addItems(p);
                }
                
                t.setDropCompleted(true);
                t.consume();
            }
        });
        
        // bind selection (needs to be got rid of, instead bind last seleced item)
        if(PlaylistManager.selectionModelProperty().isBound())
            table.selectionModelProperty().bindBidirectional(PlaylistManager.selectionModelProperty());
        else
            PlaylistManager.selectionModelProperty().bindBidirectional(table.selectionModelProperty());
        
        // observe and show playing item
        PlaylistManager.playingItemProperty().addListener(playingListener);
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
    public void sortByTitle() {
        FXCollections.sort(table.getItems());
    }
    public void sortByLength() {
        table.getItems().sort(PlaylistItem.getComparatorTime());
    }
    public void sortByFilename() {
        table.getItems().sort(PlaylistItem.getComparatorURI());
    }
    /**
     * Selects all playlist items.
     */
    public void selectAll() {
        table.getSelectionModel().selectAll();
        updateSelected();
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
        updateSelected();
    }
    /**
     * Deselects all selected items in the playlist table.
     */
    public void selectNone() {
        table.getSelectionModel().clearSelection();
        updateSelected();
    }
    
    /**
     * Refreshes the table.
     * This method should be got rid of, but...
     * Current implementation should have absolutely no negative performance impact
     */
    public void refresh() {
        // one column 'refresh' is all thats needed
        columnIndex.setCellFactory(null);
        columnIndex.setCellFactory(indexCellFactory);
    }
    
    /**
     * Moves/shifts all selected items by specified distance.
     * Selected items retain their relative positions. Items stop moving when
     * any of them hits end/start of the playlist. Items wont rotate in the playlist.
     * Selection doesnt change.
     * @note If this method requires real time response (for example reacting on mouse
     * drag in table), it is important to 'cache' the behavior and allow values
     * >1 && <-1 so the moved items dont lag behind.
     * @param by distance to move items by. Negative moves back. Zero does nothing.
     */
    private void moveSelectedItems(int by) {
//        try {
//            // get selected, remove duplicates
//            List<Object> oldSelected = Arrays.asList((new TreeSet<>(table.getSelectionModel().getSelectedIndices())).toArray());
//            // new selected
//            List<Integer> indexes = new ArrayList<>();
//            // bypass table to prevent refresh
//            List<PlaylistItem> p = new ArrayList<>();
//                               p.addAll(PlaylistManager.getItems());
//
//            if(by > 0) {
//                for (int i = oldSelected.size()-1; i >= 0; i--) {
//                    int ii = (Integer)oldSelected.get(i);
//                    Collections.swap(p, ii, ii+by);
//                    indexes.add(ii+by);
//                }
//
//            } else if ( by < 0) {
//                for (int i = 0; i < oldSelected.size(); i++) {
//                    int ii = (Integer)oldSelected.get(i);
//                    Collections.swap(p, ii, ii+by);
//                    indexes.add(ii+by);
//                }
//            }
//            PlaylistManager.getItems().setAll(p);
//            Util.selectRows(indexes, table.getSelectionModel());
//        } catch ( IndexOutOfBoundsException ex) {
//            // ex is thrown if moved block hits start or end of the playlist
//            // this gets rid of enormously complicated if statement
//            return;
//        }
            // get selected, remove duplicates
        List<Object> oldSelected = Arrays.asList((new TreeSet<>(table.getSelectionModel().getSelectedIndices())).toArray());
        List<Integer> newSelected = PlaylistManager.moveItemsBy(oldSelected, by);
        TableUtil.selectRows(newSelected, table.getSelectionModel());
    }
    /**
     * Reflect the selection change in playlist manager for whole app to see
     */
    private void updateSelected() {
        // clear selection if empty selection ( fixes mibehavior )
        if (table.getSelectionModel().getSelectedItems().isEmpty())
            PlaylistManager.setSelectedItems(Collections.EMPTY_LIST);
        else {
            Playlist p = new Playlist(table.getSelectionModel().getSelectedItems());
//                     p.removeDuplicates();
            PlaylistManager.setSelectedItems(p.list());
        }
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

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        System.out.println("FINALIZING PLAYLIST TABLE");
        PlaylistManager.playingItemProperty().removeListener(playingListener);
    }
    
    
}