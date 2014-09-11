package Bookmarker;


import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.PlaylistManager;
import Configuration.IsConfig;
import GUI.DragUtil;
import GUI.objects.ContextMenu.ContentContextMenu;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Features.TaggingFeature;
import Layout.Widgets.WidgetManager;
import static Layout.Widgets.WidgetManager.WidgetSource.NOLAYOUT;
import Library.BookmarkItem;
import Library.BookmarkManager;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import static javafx.scene.input.KeyCode.LEFT;
import static javafx.scene.input.KeyCode.RIGHT;
import javafx.scene.input.KeyEvent;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import javafx.scene.input.MouseEvent;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static javafx.scene.input.MouseEvent.MOUSE_RELEASED;
import static javafx.scene.input.ScrollEvent.SCROLL;
import javafx.scene.input.TransferMode;
import javafx.util.Callback;
import utilities.Enviroment;
import utilities.Util;


/**
 * FXML Controller class
 */
public class BookmarkerController extends FXMLController {
    
    @FXML TableView<BookmarkItem> table;
    @FXML TableColumn nameColumn;
    @FXML TableColumn<BookmarkItem,String> pathColumn;
    
    // properties
    @IsConfig(name = "Read only", info = "Forbid all changes to bookmarks through this widget.")
    public boolean read_only = false;
    @IsConfig(name = "Editable", info = "Allow editing of the bookmarks directly in table.")
    public boolean editable = false;
    
    @Override
    public void init() {
        
        //initiaize table columns
        nameColumn.setCellValueFactory(new PropertyValueFactory("name"));
        nameColumn.setCellFactory( column -> {
            TableCell<BookmarkItem,String> cell = new TableCell<BookmarkItem,String>() {
                private TextField textField;
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                       setText(null);
                       setGraphic(null);
                    } else {
                       if (isEditing()) {
                           if (textField != null)
                               textField.setText(getString());
                           setText(null);
                           setGraphic(textField);
                       } else {
                           setText(item);
                           setGraphic(null);
                       }
                   }
                }
                @Override
                public void startEdit() {
                    if (isEmpty() || editable) return;
                    super.startEdit();
                    createTextField();

                    setText(null);
                    setGraphic(textField);
                    textField.selectAll();
                    textField.requestFocus();
                }
                @Override
                public void cancelEdit() {
                    super.cancelEdit();
                    setText(getItem());
                    setGraphic(null);
                }
                @Override
                public void commitEdit(String t) {
                    //super.commitEdit(t);
                    table.getItems().get(getIndex()).setName(t);
                    cancelEdit();
                }
                private void createTextField() {
                    textField = new TextField(getString());
                    textField.setMinWidth(this.getWidth() - this.getGraphicTextGap()* 2);
//                        textField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean arg1, Boolean arg2) -> {
//                            if (!arg2)
//                                cancelEdit();
//                        });
                    textField.setOnKeyReleased( t -> {
                        if (t.getCode() == KeyCode.ENTER)
                            commitEdit(textField.getText());
                        if (t.getCode() == KeyCode.ESCAPE)
                            cancelEdit();
                    });
                }
                private String getString() {
                    return getItem() == null ? "" : getItem().toString();
                }
            };
            cell.selectedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean arg1, Boolean arg2) -> {
                if(!cell.isSelected())
                    cell.cancelEdit();
            });
            cell.setOnMouseClicked((MouseEvent t) -> {
                if(t.getClickCount()==2)
                    cell.startEdit();
            });
            return cell;
        });
        pathColumn.setCellValueFactory(new PropertyValueFactory<>("uri"));
        pathColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<BookmarkItem, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<BookmarkItem, String> param) {
                return new SimpleObjectProperty<String>(param.getValue().getName());
            }
        });
        pathColumn.setCellFactory( column -> {
            TableCell<BookmarkItem,String> cell = new TableCell<BookmarkItem, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    try {
                        BookmarkItem i = table.getItems().get(getIndex());
                        setText(i.getPath());
                    } catch (IndexOutOfBoundsException | NullPointerException e) {
                        setText("");
                    }
                }
            };
            return cell;
        });
        
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        
        // remove selected items on delete
        table.setOnKeyPressed( t -> {
            if (t.getCode() == KeyCode.DELETE && !read_only) {
                // must construct separate list or all items will not get deleted
                // because list change -> clear selection
                List l = new ArrayList<>(table.getSelectionModel().getSelectedItems());
                BookmarkManager.removeBookmarks(l);
                table.getSelectionModel().clearSelection();
            }
        });
        
        // deselect on list change (avoid broken selection behavior on delete)
        table.itemsProperty().get().addListener((ListChangeListener.Change<? extends BookmarkItem> change) -> {
            while (change.next())
                if (change.wasRemoved())
                    table.getSelectionModel().clearSelection();
        });
        
        
        table.setRowFactory( t -> {
            TableRow<BookmarkItem> row = new TableRow<>();
            // support dragging from table
            row.setOnDragDetected( e -> {
                if (e.getButton()==PRIMARY) {
                    Dragboard db = table.startDragAndDrop(TransferMode.ANY);
                    DragUtil.setItemList(table.getSelectionModel().getSelectedItems(), db);
                    e.consume();
                }
            });
            // show contextmenu on right click
            row.setOnMouseClicked( e -> {
                if (!row.isEmpty() && e.getButton().equals(SECONDARY)) {  
                    if (!table.getSelectionModel().getSelectedItems().isEmpty())
                        getCM(t).show(row, e.getScreenX(), e.getScreenY());
                }
            });
            
            return row;
        });
        // support drag transfer
        table.setOnDragOver(DragUtil.audioDragAccepthandler);
        // handle drag drop
        table.setOnDragDropped( e -> {
            if(DragUtil.hasAudio(e.getDragboard())) {
                List<Item> items = DragUtil.getAudioItems(e);
                BookmarkManager.addBookmarks(items);
                e.setDropCompleted(true);
                e.consume();
            }
        });
        
        // scroll vertically when holding shift
        table.addEventFilter(SCROLL, e -> {
            if(e.isShiftDown()) {
                if(e.getDeltaY()<0) {   // scroll to the right
                    KeyEvent ev = new KeyEvent(KEY_PRESSED, RIGHT.getName(),
                            RIGHT.getName(), RIGHT, false,false,false,false);
                    Event.fireEvent(table, ev);
                } else {                // scroll to the left
                    KeyEvent ev = new KeyEvent(KEY_PRESSED, LEFT.getName(),
                            LEFT.getName(), LEFT, false,false,false,false);
                    Event.fireEvent(table, ev);
                }
                e.consume(); // consume so table wont scroll vertically
            }
        });
        
        // maintain lock on column maintenance
        table.addEventFilter(MOUSE_PRESSED,e->column_resize_lock=true);
        table.addEventFilter(MOUSE_RELEASED,e->column_resize_lock=false);
        
        // keep remembering column positions
        table.getColumns().addListener(colPosReader);
        
        // keep remembering column widths
        table.getColumns().forEach(col->{            
            col.widthProperty().addListener((o,ov,nv) -> {
                if(column_resize_lock) {
                    if(table.getWidth()==0) return; // avoid pre-init input
                    double p_width = col.getWidth()/table.getWidth();           // System.out.println("WRITE " + col.getText() + " " + p_width);
                    colWid.put(col.getText(), p_width);
                }
            });
        });
        // maintain remembered column widths 
        table.widthProperty().addListener(colWidthWriter);
        
        // keep remembering column visibility
        table.getColumns().forEach(c -> {
            c.visibleProperty().addListener((o,ov,nv)-> colVis.put(c.getText(),nv));
        });
    }
    
    @IsConfig(editable = false)
    public HashMap<String,Boolean> colVis = new HashMap();
    @IsConfig(editable = false)
    public HashMap<String,Double> colWid = new HashMap();
    @IsConfig(editable = false)
    public HashMap<String,Integer> colPos = new HashMap();
    
    private boolean column_resize_lock = false; // true - maintenance allowed
    // refresh correct column widths
    private final  ChangeListener<? super Number> colWidthWriter = (o,ov,nv) -> {
        table.getColumns().forEach(col -> {
            // get width
            Double width = colWid.get(col.getText());                           // System.out.println("READ " + col.getText() + " " + width +" for " + newV);
            if (width != null)
                col.setPrefWidth(nv.doubleValue()*width);
            else {      
                // set width to 1/n if n/a
                double total = table.getColumns().size();
                colWid.putIfAbsent(col.getText(),1/total);
            }
        });
    };
    // store correct column order
    private final ListChangeListener<TableColumn> colPosReader = (ListChangeListener.Change<? extends TableColumn> l) -> {
        while(l.next()) {
            if(table.getWidth()==0) return; // avoid pre-init input
            table.getColumns().forEach(col->{
                colPos.put(col.getText(), table.getColumns().indexOf(col));
            });
        }
    };
    // refresh column order
    private void colPosWrite() {
        if(colPos.size()<2) {
            table.getColumns().forEach(col->
                colPos.put(col.getText(), table.getColumns().indexOf(col))
            );
        }
        
        List<TableColumn> cols = new ArrayList(table.getColumns());
        table.getColumns().clear();
        colPos.entrySet().stream()
                .sorted(Util.cmpareBy(e->e.getValue()))
                .forEach( e -> {
                    TableColumn c = cols.stream().filter(cc->cc.getText().equals(e.getKey())).findAny().get();
                    if(c!=null)
                        table.getColumns().add(c);
                
        });
    }    
    
    
    @Override
    public void refresh() {
        table.setEditable(editable);

        // start column maintenance
        column_resize_lock=true;
        // init column visibility
        colVis.forEach( (name,visibility) -> {
            TableColumn c = table.getColumns().stream().filter(cc->cc.getText().equals(name)).findAny().get();
            if(c!=null) {
//                c.setVisible(visibility);
                table.getColumns().remove(c);
            }
        });
        
        // refresh correct column order
        colPosWrite();
        // init column widths
        colWidthWriter.changed(table.widthProperty(), 0, table.getWidth());  // might be unnecessary, but just in case
        // end column maintenance
        column_resize_lock=false;
        
        table.setItems(BookmarkManager.getBookmarks()); 
    }
    
/****************************** CONTEXT MENU **********************************/
    
    private static ContentContextMenu<List<BookmarkItem>> cm;
    
    private static ContentContextMenu getCM(TableView<BookmarkItem> t) {
        if(cm==null) cm = buildCM();
        // note: we need to create a copy of the list to avoid modification
        cm.setItem(new ArrayList(t.getSelectionModel().getSelectedItems()));
        return cm;
    }
    
    private static ContentContextMenu buildCM() {
        final ContentContextMenu<List<BookmarkItem>> contextMenu = new ContentContextMenu();
        
        MenuItem item1 = new MenuItem("Add items to playlist");        
                 item1.setOnAction(e -> {
                     List<BookmarkItem> items = contextMenu.getItem();
                     PlaylistManager.addItems(items);
                 });
        MenuItem item2 = new MenuItem("Add items to playlist & play");        
                 item2.setOnAction(e -> {
                     List<BookmarkItem> items = contextMenu.getItem();
                     if(items.isEmpty()) return;
                     int i = PlaylistManager.getSize();
                     PlaylistManager.addItems(items);
                     PlaylistManager.playItem(i);
                 });
        MenuItem item3 = new MenuItem("Unbookmark items");        
                 item3.setOnAction(e -> {
                     List<BookmarkItem> items = contextMenu.getItem();
                     BookmarkManager.removeBookmarks(items);
                 });
        MenuItem item4 = new MenuItem("Edit the item/s in tag editor");        
                 item4.setOnAction(e -> {
                     List<BookmarkItem> items = contextMenu.getItem();
                     WidgetManager.getWidget(TaggingFeature.class,NOLAYOUT,w->w.read(items));
                 });
        MenuItem item5 = new MenuItem("Explore items's directory");        
                 item5.setOnAction(e -> {
                     List<BookmarkItem> items = contextMenu.getItem();
                     List<File> files = items.stream()
                             .filter(Item::isFileBased)
                             .map(Item::getLocation)
                             .collect(Collectors.toList());
                     Enviroment.browse(files,true);
                 });                 
                 
        contextMenu.getItems().addAll(item1, item2, item3, item4, item5);
        contextMenu.setConsumeAutoHidingEvents(false);
        return contextMenu;
    }
}