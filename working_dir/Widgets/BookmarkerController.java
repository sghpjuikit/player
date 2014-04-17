
import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.Playlist;
import Configuration.IsConfig;
import GUI.ContextManager;
import GUI.DragUtil;
import Layout.WidgetController;
import Library.BookmarkItem;
import Library.BookmarkManager;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
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
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.util.Callback;
import utilities.FileUtil;


/**
 * FXML Controller class
 */
public class BookmarkerController extends WidgetController {
    @FXML
    TableView<BookmarkItem> table;
    @FXML
    TableColumn nameColumn;
    @FXML
    TableColumn<BookmarkItem,String> pathColumn;
    
    // properties
    @IsConfig(name = "Read only", info = "Forbid all changes to bookmarks through this widget.")
    public boolean read_only = false;
    @IsConfig(name = "Editable", info = "Allow editing of the bookmarks directly in table.")
    public boolean editable = false;
    @IsConfig(name = "Column1 width", info = "Forbid all changes to bookmarks through this widget.",editable = false)
    public double c1_w = 0;
    
    @Override
    public void initialize() {
        
        //initiaize table columns
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setCellFactory(new Callback<TableColumn, TableCell>() {
            @Override
            public TableCell call(TableColumn p) {
                TableCell cell = new TableCell<BookmarkItem, String>() {
                    private TextField textField;
                    @Override
                    protected void updateItem(String t, boolean bln) {
                        super.updateItem(t, bln);
                        if (bln) {
                           setText(null);
                           setGraphic(null);
                        } else {
                           if (isEditing()) {
                               if (textField != null)
                                   textField.setText(getString());
                               setText(null);
                               setGraphic(textField);
                           } else {
                               setText(getString());
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
                        textField.setOnKeyReleased((KeyEvent t) -> {
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
            }
        });
        pathColumn.setCellValueFactory(new PropertyValueFactory<>("uri"));
        pathColumn.setCellFactory(new Callback<TableColumn<BookmarkItem, String>, TableCell<BookmarkItem,String>>() {
            @Override
            public TableCell<BookmarkItem, String> call(TableColumn<BookmarkItem, String> p) {
                TableCell<BookmarkItem,String> cell = new TableCell<BookmarkItem, String>() {
                    @Override
                    protected void updateItem(String t, boolean bln) {
                        super.updateItem(t, bln);
                        try {
                            BookmarkItem i = table.getItems().get(getIndex());
                            setText(i.getPath());
                        } catch (IndexOutOfBoundsException | NullPointerException e) {
                            setText("");
                        }
                    }
                };
                return cell;
            }
            
        });
        
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setItems(BookmarkManager.getBookmarks());        

        
        // remove selected items on delete
        table.setOnKeyPressed((KeyEvent t) -> {
            if (t.getCode() == KeyCode.DELETE && !read_only) {
                // must construct separate list or all items will not get deleted
                // because list change -> clear selection
                List l = new ArrayList<>(table.getSelectionModel().getSelectedItems());
                BookmarkManager.removeBookmarks(l);
            }
        });
        
        // remember column sizes
        nameColumn.widthProperty().addListener((ObservableValue<? extends Number> ov, Number t, Number t1) -> {
            c1_w = nameColumn.getWidth();
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

    }
    
    @Override
    public void refresh() {
        nameColumn.prefWidthProperty().setValue(c1_w); // why the hell is this not working!!
        table.setEditable(editable);
    }
    
}