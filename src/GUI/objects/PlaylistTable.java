
package GUI.objects;

import AudioPlayer.Player;
import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.services.Database.DB;
import AudioPlayer.tagging.Metadata;
import GUI.DragUtil;
import GUI.GUI;
import GUI.objects.ContextMenu.ContentContextMenu;
import Layout.Widgets.Features.TaggingFeature;
import Layout.Widgets.WidgetManager;
import static Layout.Widgets.WidgetManager.Widget_Source.FACTORY;
import PseudoObjects.FormattedDuration;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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
import javafx.util.Duration;
import org.reactfx.Subscription;
import utilities.Enviroment;
import utilities.FxTimer;
import utilities.SingleInstance;
import utilities.TODO;
import utilities.TableUtil;
import utilities.Util;

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
    private final Callback<TableView<PlaylistItem>,TableRow<PlaylistItem>> rowFactory;
    
    private FilteredList<PlaylistItem> itemsF = new FilteredList(FXCollections.emptyObservableList());
    private SortedList<PlaylistItem> itemsS = new SortedList(itemsF);
    
    
    // properties
    boolean zero_pad = true;
    private boolean show_original_index; // when filter in effect
    
    // selection helper variables
    double last;
    ArrayList<Integer> selected_temp = new ArrayList<>();
    int clicked_row = -1;
    
    // playing item observation listeners
    Subscription playintItemMonitor = Player.playingtem.subscribeToChanges(o->refresh());
    
    // invisible controls helping with resizing columns
    private Label tmp = new Label();
    private Label tmp2 = new Label();
    
    public PlaylistTable (AnchorPane parent) {
        this();
        setRoot(parent);
    }
    
    public void setRoot(AnchorPane parent) {
        parent.getChildren().add(table);
        AnchorPane.setBottomAnchor(table, 0d);
        AnchorPane.setLeftAnchor(table, 0d);
        AnchorPane.setRightAnchor(table, 0d);
        AnchorPane.setTopAnchor(table, 0d);
        parent.getChildren().add(tmp);
        parent.getChildren().add(tmp2);        
    }
    
    /**
     * Constructor.
     * Creates the table and fits it to specified parent.
     * Initializes everything needed to fully function.
     * @param parent AnchorPane container wrapping this table. Anchors will be set to 0.
     */
    public PlaylistTable () {
        
        // initialize table gui
        table.setTableMenuButtonVisible(true);
        table.setFixedCellSize(GUI.font.getSize() + 5);
        
        // initialize table factories
        indexCellFactory = ( column -> new TableCell<PlaylistItem, String>() {
            {
                setAlignment(Pos.CENTER_RIGHT);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty) {
                    int index = show_original_index ? itemsF.getSourceIndex(getIndex()) : getIndex();
                        index++;
                    setText(Util.zeroPad(index, table.getItems().size(), zero_pad ? '0' : ' ') + ".");
                } 
                else setText("");
            }
        });
        
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
            // handle drag transfer
            row.setOnDragDropped( e -> 
                onDragDropped(e, row.isEmpty() ? table.getItems().size() : row.getIndex())
            );
            return row;
        };
        
         // set table factories
        columnIndex.setSortable(false);
        columnIndex.setCellFactory(indexCellFactory);
        columnName.setCellValueFactory(new PropertyValueFactory("name"));
        columnTime.setCellValueFactory(new PropertyValueFactory("time"));
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
            // need this weird method to get 9s as their are wide chars
            // (font isnt always proportional)
            int i = Util.DecMin1(table.getItems().size());    
            tmp.setText(String.valueOf(i)+".");
            tmp.setVisible(true);
            double W1 = tmp.getWidth();
            tmp.setVisible(false);
            // column 3
            tmp2.setText("00:00");
            double W3 = tmp2.getWidth() + 4;
            
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
        
        // handle selection
        table.setOnMousePressed( e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
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
            if (e.getButton() != MouseButton.PRIMARY) return;
            // remember the indexes of selected rows
            // clone (! not copy), copying would mean that change to selected items
            // would change the remembered indexes too
            selected_temp.clear();
            for(Integer i: table.getSelectionModel().getSelectedIndices()) {
                selected_temp.add(i);
            }
            e.consume();
        });
        // handle click
        table.setOnMouseClicked( e -> {
            if (e.getButton() == PRIMARY) {     // play item on doubleclick
                if (e.getClickCount() == 2) {
                    int i = table.getSelectionModel().getSelectedIndex();
                    int real_i = itemsF.getSourceIndex(i);
                    PlaylistManager.playItem(real_i);
                    e.consume();
                }           
            } else
            if (e.getButton() == SECONDARY)     // show contextmenu
                if (!PlaylistManager.isEmpty()) {
                    contxt_menu.get(table).show(table, e.getScreenX(), e.getScreenY());
                    e.consume();
                }
        });
        
        // move items on drag
        table.setOnMouseDragged( e -> {
            if (e.getButton()!=MouseButton.PRIMARY) return;
            
            double ROW = table.getFixedCellSize();
            double diff = e.getScreenY()- last;
            
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
        // handle drag from - copy selected items
        table.setOnDragDetected( e -> {
            if (e.isControlDown() && e.getButton() == PRIMARY) {
                Dragboard db = table.startDragAndDrop(TransferMode.COPY);
                DragUtil.setPlaylist(new Playlist(getSelectedItems()),db);
                e.consume();
            }
        });
        //support drag over transfer - paste items
        table.setOnDragOver(dragOverHandler);
        // handle drag (for empty table - it does not have any rows so
        // drag event handlers registered on rows in row factory will not work)
        // in case table is not empty. row's respective handler will handle this
        table.setOnDragDropped( e -> {
            if (table.getItems().isEmpty())
                onDragDropped(e, 0);
        });
        
        // reflect selection for whole application
        table.getSelectionModel().selectedItemProperty().addListener(selItemListener);
        table.getSelectionModel().getSelectedItems().addListener(selItemsListener);
        
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
        if(val) table.getStylesheets().remove(PlaylistTable.class.getResource("PlaylistTable.css").toExternalForm());
        else    table.getStylesheets().add(PlaylistTable.class.getResource("PlaylistTable.css").toExternalForm());
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
        refresh(); // in order to apply change
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
        table.getColumnResizePolicy().call(new TableView.ResizeFeatures(table, columnIndex, 0.0));
    }
    
    /** Clears resources like listeners for this table object. */
    public void clearResources() {
        playintItemMonitor.unsubscribe();
        table.getSelectionModel().selectedItemProperty().removeListener(selItemListener);
        table.getSelectionModel().getSelectedItems().removeListener(selItemsListener);
    }
    
/************************************* DATA ***********************************/
    
     // we will set this to table.getItems, but since the list can change, we
    // have to carry it over to the new list
    ListChangeListener<PlaylistItem> resizer = o -> {
        // unfortunately this doesnt work, it requires delay
        // table.getColumnResizePolicy().call(new TableView.ResizeFeatures(table, columnIndex, 0d));
        FxTimer.run(Duration.millis(100), () -> {
                table.getColumnResizePolicy().call(new TableView.ResizeFeatures(table, columnIndex, 0d));
        });
    };
    
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
        } else {
            if(items instanceof FilteredList) {
                itemsF = (FilteredList<PlaylistItem>) items;
            }
            else itemsF = new FilteredList(items);
            itemsS = new SortedList<>(itemsF);
        }
        if (table.getItems() != null) table.getItems().removeListener(resizer); // remove listener
        table.setItems(itemsS);
        table.getItems().addListener(resizer);  // add listener
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
        refresh(); // in order to apply value
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
        columnName.setComparator((o1,o2) -> 
                PlaylistItem.getComparatorArtist().compare(
                        new PlaylistItem(null, o1, last), 
                        new PlaylistItem(null, o2, last)));
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
    ChangeListener<PlaylistItem> selItemListener = (o,ov,nv) -> {
        if(movingitems) return; 
        PlaylistManager.selectedItemES.push(nv);
    };
    ListChangeListener<PlaylistItem> selItemsListener = (ListChangeListener.Change<? extends PlaylistItem> c) -> {
        if(movingitems) return;
        while(c.next()) {
            PlaylistManager.selectedItemsES.push(table.getSelectionModel().getSelectedItems());
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
    
    private final EventHandler<DragEvent> dragOverHandler =  e -> {
        // avoid illegal operation on drag&drop from self to self
        if(e.getGestureSource() != table)
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

    private static final SingleInstance<ContentContextMenu<List<PlaylistItem>>,TableView<PlaylistItem>> contxt_menu = new SingleInstance<>(
        () -> {
            ContentContextMenu<List<PlaylistItem>> contextMenu = new ContentContextMenu();
            contextMenu.getItems().addAll(
                Util.createmenuItem("Play items", e -> {
                    List<PlaylistItem> items = contextMenu.getItem();
                    PlaylistManager.playItem(items.get(0));
                }),
                Util.createmenuItem("Remove items", e -> {
                    List<PlaylistItem> items = contextMenu.getItem();
                    PlaylistManager.removeItems(items);
                }),
                Util.createmenuItem("Edit the item/s in tag editor", e -> {
                    List<PlaylistItem> items = contextMenu.getItem();
                    TaggingFeature tf = WidgetManager.getWidget(TaggingFeature.class,FACTORY);
                    if (tf!=null) tf.read(items);
                }),
                Util.createmenuItem("Crop items", e -> {
                    List<PlaylistItem> items = contextMenu.getItem();
                    PlaylistManager.retainItems(items);
                }),
                Util.createmenuItem("Duplicate items as group", e -> {
                    List<PlaylistItem> items = contextMenu.getItem();
                    PlaylistManager.duplicateItemsAsGroup(items);
                }),
                Util.createmenuItem("Duplicate items individually", e -> {
                    List<PlaylistItem> items = contextMenu.getItem();
                    PlaylistManager.duplicateItemsByOne(items);
                }),
                Util.createmenuItem("Explore items's directory", e -> {
                    List<PlaylistItem> items = contextMenu.getItem();
                    List<File> files = items.stream()
                            .filter(Item::isFileBased)
                            .map(Item::getLocation)
                            .collect(Collectors.toList());
                    Enviroment.browse(files,true);
                }),
                Util.createmenuItem("Add items to library", e -> {
                    List<Metadata> items = contextMenu.getItem().stream()
                            .map(Item::toMetadata)
                            .collect(Collectors.toList());
                    DB.addItems(items);
                })
            );
            contextMenu.setConsumeAutoHidingEvents(false);
            return contextMenu;
        },
        (menu,table) -> menu.setItem(Util.copySelectedItems(table))
    );
}