/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.Table;

import AudioPlayer.playlist.NamedPlaylist;
import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import GUI.DragUtil;
import AudioPlayer.tagging.FormattedDuration;
import java.util.List;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import utilities.Util;

/**
 * @author uranium
 * 
 * Graphical component. Table for generic playlist. Displays items of Playlist object.
 * To use this class, simply create its instance and use the setPlaylist() method to bind
 * the Playlist object. 
 */
public class PlaylistTableSimple extends AnchorPane {
    NamedPlaylist playlist;
    TableView<PlaylistItem> table;
    
    public PlaylistTableSimple() {
        initialize();
        initializeObservables();
    }
    
    private void initialize() {        
        // initialize table gui
        table = new TableView();
        getChildren().add(table);
        Util.setAPAnchors(table, 0);
        
        //initiaize table columns
        final TableColumn<PlaylistItem, String> columnIndex = new TableColumn<>("item");
        columnIndex.setCellValueFactory(new PropertyValueFactory<>("index"));
        columnIndex.setCellFactory((TableColumn<PlaylistItem, String> p) -> {
            TableCell<PlaylistItem, String> cell = new TableCell() {
                @Override protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) setText("");
                    else {
                        setText(String.valueOf(1 + getIndex()) + ".");
                        if(table.getItems().size() < 10) {
                            columnIndex.setPrefWidth(16);
                        } else                            
                        if(table.getItems().size() < 100) {
                            columnIndex.setPrefWidth(20);
                        } else
                        if(table.getItems().size() < 1000) {
                            columnIndex.setPrefWidth(24);
                        }
                    }
                }
            };
            cell.setAlignment(Pos.CENTER_RIGHT);
            return cell;
        });
        TableColumn<PlaylistItem, String> columnName = new TableColumn<>("name");
        columnName.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<PlaylistItem, FormattedDuration> columnTime = new TableColumn<>("time");
        columnTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        columnTime.setCellFactory((TableColumn<PlaylistItem, FormattedDuration> param) -> {
            TableCell<PlaylistItem, FormattedDuration> cell = new TableCell(){
                    @Override protected void updateItem(Object item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) setText("");
                        else {
                            setText(item.toString());
                        }                        
                    }
            };
            cell.setAlignment(Pos.CENTER_RIGHT);
            return cell;
        });
       
        
        // initialize table data
        table.getColumns().setAll(columnIndex, columnName, columnTime);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        //resize columns
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        columnIndex.setMinWidth(12);
        columnIndex.setPrefWidth(12);
        columnIndex.setMaxWidth(12);
        columnIndex.setResizable(false);
        columnTime.setMinWidth(40);
        columnTime.setPrefWidth(40);
        columnTime.setMaxWidth(40);
        columnTime.setResizable(false);
        columnName.setResizable(true);
        columnName.prefWidthProperty().bind(table.widthProperty().subtract(12+40+15)); // +15 to eliminate scrollbar)       
    }
    
    private void initializeObservables() {
        // hide the table header
        table.widthProperty().addListener((ObservableValue<? extends Number> ov, Number t, Number t1) -> {
            Pane header = (Pane) lookup("TableHeaderRow");
            if (header != null && header.isVisible()) {
                header.setMaxHeight(0);
                header.setMinHeight(0);
                header.setPrefHeight(0);
                header.setVisible(false);
                header.setManaged(false);
            }
        });
        
        // play content on doubleclick
        table.setOnMouseClicked((MouseEvent event) -> {
            if (event.getClickCount() == 2) {
                // the if solves the problem of clicling on an empty row
                if (table.getSelectionModel().getSelectedIndex() != -1) {
                    PlaylistManager.playPlaylistFrom(playlist, table.getSelectionModel().getSelectedIndex());
                }
            }
            event.consume();
        });
        
        // play content on Enter, remove items on Delete
        table.setOnKeyPressed((KeyEvent event) -> {
            if (event.getCode() == KeyCode.ENTER) {
                PlaylistManager.playPlaylistFrom(playlist, table.getSelectionModel().getSelectedIndex());
            }
            if (event.getCode() == KeyCode.DELETE) {
                if ( table.getItems().isEmpty()) { return; }
                List<Integer> items = table.getSelectionModel().getSelectedIndices();
                playlist.removeItem(items);
                table.getItems().clear();
                table.getItems().addAll(playlist.getItems());
                table.getSelectionModel().clearSelection();
            }
        });
        // support dragging from table
        table.setOnDragDetected((MouseEvent event) -> {
            Dragboard db = table.startDragAndDrop(TransferMode.ANY);
            DragUtil.setPlaylist(new Playlist(table.getSelectionModel().getSelectedItems()), db);
            event.consume();
        });
    }
    
    /**
     * Binds the Playlist object as the underlaying data structure for this table.
     * @param _playlist 
     */
    public void setPlaylist(NamedPlaylist _playlist) {
        playlist = _playlist;
        table.getItems().setAll(_playlist.getItems());
    }
    
    private void refreshTable() {
        //most prob doesnt work
//        for (TableColumn c: table.getColumns()) {
//            c.setVisible(false);
//            c.setVisible(true);
//        }
    }
}
