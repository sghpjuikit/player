package Bookmarker;


import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.Playlist;
import Configuration.IsConfig;
import GUI.ContextManager;
import GUI.DragUtil;
import Layout.Widgets.FXMLController;
import Library.BookmarkItem;
import Library.BookmarkManager;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import static javafx.scene.input.KeyCode.LEFT;
import static javafx.scene.input.KeyCode.RIGHT;
import javafx.scene.input.KeyEvent;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static javafx.scene.input.MouseEvent.MOUSE_RELEASED;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TransferMode;
import javafx.util.Callback;
import utilities.FileUtil;
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
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
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
                if (e.getButton()==MouseButton.PRIMARY) {
                    Dragboard db = table.startDragAndDrop(TransferMode.ANY);
                    DragUtil.setContent(db ,table.getSelectionModel().getSelectedItems());
                    e.consume();
                }
            });
            // show contextmenu on right click
            row.setOnMouseClicked( e -> {
                if (!row.isEmpty() && e.getButton().equals(MouseButton.SECONDARY)) {  
                    if (!table.getSelectionModel().getSelectedItems().isEmpty())
                        ContextManager.showMenu(ContextManager.bookmarkMenu,table.getSelectionModel().getSelectedItems());
                }
            });
            
            return row;
        });
        // support drag transfer
        table.setOnDragOver( t -> {
            if (t.getGestureSource() == table) return;
            Dragboard db = t.getDragboard();
            if (db.hasFiles() || db.hasUrl() || db.hasContent(DragUtil.items) ||
                db.hasContent(DragUtil.playlist))
                if (t.getSource().equals(table))
                t.acceptTransferModes(TransferMode.ANY);
            t.consume();
        });
        
        // handle drag drop
        table.setOnDragDropped((DragEvent t) -> {
            Dragboard db = t.getDragboard();

            if (db.hasFiles()) {    // add files and folders
                List<File> files = FileUtil.getAudioFiles(db.getFiles(), 1);
                BookmarkManager.addBookmarksAsFiles(files);
            } else                  // add url
            if (db.hasUrl()) {
                String url = db.getUrl();
                BookmarkManager.addBookmarksAsURI(Collections.singletonList(URI.create(url)));
            } else                  // add playlist
            if (db.hasContent(DragUtil.playlist)) {
                Playlist pl = DragUtil.getPlaylist(db);
                BookmarkManager.addBookmarks(pl.getItems());
            } else
            if (db.hasContent(DragUtil.items)) {
                List<Item> i = DragUtil.getItems(db);
                BookmarkManager.addBookmarks(i);
            }
            
            t.setDropCompleted(true);
            t.consume();
        });
        
        // scroll vertically when holding shift
        table.addEventFilter(ScrollEvent.SCROLL, e -> {
            if(e.isShiftDown()) {
                if(e.getDeltaY()<0) {   // scroll to the right
                    KeyEvent ev = new KeyEvent(KEY_PRESSED, RIGHT.getName(),
                            RIGHT.getName(), KeyCode.RIGHT, false,false,false,false);
                    Event.fireEvent(table, ev);
                } else {                // scroll to the left
                    KeyEvent ev = new KeyEvent(KEY_PRESSED, LEFT.getName(),
                            LEFT.getName(), KeyCode.LEFT, false,false,false,false);
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
            col.widthProperty().addListener((o,oldV,newV) -> {
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
            c.visibleProperty().addListener((o,oldV,newV)-> colVis.put(c.getText(), newV));
        });
    }
    
    @IsConfig(visible = false)
    public HashMap<String,Boolean> colVis = new HashMap();
    @IsConfig(visible = false)
    public HashMap<String,Double> colWid = new HashMap();
    @IsConfig(visible = false)
    public HashMap<String,Integer> colPos = new HashMap();
    
    private boolean column_resize_lock = false; // true - maintenance allowed
    // refresh correct column widths
    private final  ChangeListener<? super Number> colWidthWriter = (o,oldV,newV) -> {
        table.getColumns().forEach(col -> {
            // get width
            Double width = colWid.get(col.getText());                           // System.out.println("READ " + col.getText() + " " + width +" for " + newV);
            if (width != null)
                col.setPrefWidth(newV.doubleValue()*width);
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
    
    
}