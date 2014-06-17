package ExtendedInfo;


import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.tagging.Chapter;
import AudioPlayer.tagging.CommentExtended;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataReader;
import Configuration.IsConfig;
import GUI.GUI;
import Layout.Widgets.FXMLController;
import PseudoObjects.ReadMode;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import utilities.Util;

/**
 * FXML Controller class
 *
 * @author uranium
 */
public class ExtendedInfoController extends FXMLController {
    @FXML TableView<CommentExtended> infosT;
    @FXML TableView<Chapter> chaptersT;
    @FXML TextField text;
    @FXML TextField type;
    @FXML AnchorPane entireArea;
    @FXML Label sourceL;
        
    // data
    PlaylistItem item;
    Metadata source;
    Metadata new_meta;
    private final ObservableList<CommentExtended> infos = FXCollections.observableArrayList();
    private final ObservableList<Chapter> chapters = FXCollections.observableArrayList();
    private final ObjectProperty<Chapter> playingChapter = new SimpleObjectProperty<>();
    
        
    // properties
    @IsConfig(name = "Read Mode", info = "Source of data for the widget.")
    public ReadMode readMode = ReadMode.PLAYING;
    @IsConfig(name = "Show Zero/End chapters", info = "Automatically add chapters at start and end of the song.")
    public boolean includeStartEndChapter = true;
    
    // listeners
    ChangeListener<PlaylistItem> selItemListener = (o,oldV,newV) -> {
        if (readMode == ReadMode.PLAYLIST_SELECTED)
            refresh();
    };
    ChangeListener<PlaylistItem> plaItemListener = (o,oldV,newV) -> {
        if (readMode == ReadMode.PLAYING)
            refresh();
    };
    
    @Override
    public void init() {
        
        // initialize tables
        TableColumn<CommentExtended, String> columnKey = new TableColumn<>("key");
        columnKey.setCellValueFactory(new PropertyValueFactory<>("key"));
        columnKey.setCellFactory( column -> {
            TableCell<CommentExtended, String> cell = new TableCell() {
                @Override protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) setText("");
                    else setText(item.toString());
                }
            };
            cell.setAlignment(Pos.CENTER_RIGHT);
            return cell; 
        });
        columnKey.setPrefWidth(50);
        columnKey.setResizable(false);
        
        TableColumn<CommentExtended, String> columnIValue = new TableColumn<>("value");
        columnIValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        columnIValue.setResizable(true);
//        columnIValue.prefWidthProperty().bind(infosT.widthProperty()
//                .subtract(columnKey.widthProperty())
//                .subtract(5)); //(key column) + 5 (additional space to eliminate scrollbar
        columnIValue.setCellFactory( column -> {
            TableCell<CommentExtended, String> cell = new TableCell<CommentExtended, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) setText("");
                    else setText(item);
                }
            };
            cell.setAlignment(Pos.CENTER_LEFT);
            return cell;
        });
        infosT.getColumns().setAll(columnKey, columnIValue);
        
        
        TableColumn<Chapter, String> columnPlaying = new TableColumn<>("");
        columnPlaying.setCellValueFactory(new PropertyValueFactory<>("info"));
        columnPlaying.setCellFactory( column -> {
            TableCell<Chapter, String> cell = new TableCell<Chapter, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) setText("");
                    else {
                        if (getIndex() == chaptersT.getItems().indexOf(playingChapter.get())) {
                            setText("O");
                        } else {
                            setText("");
                        }
                    }
                }
            };
            cell.setAlignment(Pos.CENTER_LEFT);
            return cell;
        });
        columnPlaying.setPrefWidth(13);
        columnPlaying.setResizable(false);
        TableColumn<Chapter, Duration> columnTime = new TableColumn<>("time");
        columnTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        columnTime.setCellFactory( column -> {
            TableCell<Chapter, Duration>  cell = new TableCell<Chapter, Duration>() {
                @Override
                protected void updateItem(Duration item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) setText("");
                    else setText(Util.formatDuration(item));
                }
            };
            cell.setAlignment(Pos.CENTER_RIGHT);
            return cell;
        });
        columnTime.setPrefWidth(37);
        columnTime.setResizable(false);        
        final TableColumn<Chapter, String> columnCValue = new TableColumn<>("info");
        columnCValue.setCellValueFactory(new PropertyValueFactory<>("info"));
        columnCValue.setCellFactory( column -> {
            //final Text txt = new Text();
            //text.wrappingWidthProperty().bind(columnCValue.widthProperty().subtract(15)); //-15: scrollbar
            TableCell<Chapter, String> cell = new TableCell<Chapter, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) setText("");
                    else {
                        // set font style  -  THIS IS NOT GOOD SOLUTION fix .css !! - font is dark instead of light color when initialized
//                            text.setFont(getFont());
//                            text.setFill(getTextFill());
//                            text.setStyle(getFont().getStyle());

                        //txt.setText(item.toString());
                        //setGraphic(txt);
                        setText(item);
                    }
                    // clear infoT selection on click
                    // clear this table selection on mouse released if no item
                    setOnMouseReleased((MouseEvent t) -> {
                        infosT.getSelectionModel().clearSelection();
                        if (item == null) {
                            chaptersT.getSelectionModel().clearSelection();
                        }
                    });
                }
            };
            cell.setAlignment(Pos.CENTER_LEFT);
            cell.setWrapText(true);
            return cell;
        });
        columnCValue.setPrefWidth(TableView.USE_COMPUTED_SIZE);
        columnCValue.setMaxWidth(TableView.USE_COMPUTED_SIZE);
        columnCValue.setMinWidth(TableView.USE_COMPUTED_SIZE);
        columnCValue.setResizable(true);
//        columnCValue.prefWidthProperty().bind(chaptersT.widthProperty()
//                .subtract(columnTime.widthProperty())
//                .subtract(columnPlaying.widthProperty())
//                .subtract(5)); //5 = additional space to eliminate scrollbar
        chaptersT.getColumns().addAll(columnPlaying, columnTime, columnCValue);       
        
        infosT.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        chaptersT.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        infosT.setItems(infos);
        chaptersT.setItems(chapters);
        infosT.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        chaptersT.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        // bind table height
//        infosT.maxHeightProperty().bind(entireArea.heightProperty().divide(2));
//        chaptersT.prefHeightProperty().bind(entireArea.heightProperty()
//                                                .subtract(infosT.heightProperty())
//                                                    .subtract(30)); // 30 = bottom anchor set up in .fxml
        // behavior
        infosT.selectionModelProperty().get().selectedItemProperty().addListener((ObservableValue<? extends CommentExtended> ov, CommentExtended t, CommentExtended t1) -> {
            CommentExtended sel = infosT.getSelectionModel().getSelectedItem();
            if (!infosT.getSelectionModel().isEmpty()) {
                type.setText(sel.getKey());
                text.setText(sel.getValue());
            }
        });
        chaptersT.selectionModelProperty().get().selectedItemProperty().addListener((ObservableValue<? extends Chapter> ov, Chapter t, Chapter t1) -> {
            Chapter sel = chaptersT.getSelectionModel().getSelectedItem();
            if (!chaptersT.getSelectionModel().isEmpty()) {
                type.setText("");
                text.setText(sel.getInfo());
            }
        });
        infosT.setOnKeyPressed((KeyEvent t) -> {
            if (t.getCode() == KeyCode.DELETE) {
                removeInfo();
            }
        });
        chaptersT.setOnKeyPressed((KeyEvent t) -> {
            if (t.getCode() == KeyCode.ENTER) {
                playSelectedChapter();
            }
            if (t.getCode() == KeyCode.DELETE) {
                removeChapter();
            }
        });
        chaptersT.setOnMouseClicked((MouseEvent t) -> {
            if (t.getClickCount() == 2) {
                playSelectedChapter();
            }
            t.consume();
        });
        type.setOnKeyPressed((KeyEvent t) -> {
            if (t.getCode() == KeyCode.ENTER) {
                if( !chaptersT.getSelectionModel().isEmpty() || !infosT.getSelectionModel().isEmpty()) {
                    editElement();
                } else
                    addElement();
            }
        });
        text.setOnKeyPressed((KeyEvent t) -> {
            if (t.getCode() == KeyCode.ENTER) {
                if( !chaptersT.getSelectionModel().isEmpty() || !infosT.getSelectionModel().isEmpty()) {
                    editElement();
                } else
                    addElement();
            }
        });
        
        // look for data changes
        PlaylistManager.selectedItemProperty().addListener(selItemListener);
        PlaylistManager.playingItemProperty().addListener(plaItemListener);
        
        // determine playing chapter
        PLAYBACK.currentTimeProperty().addListener( o -> {
            if (source == null || !source.same(PlaylistManager.getPlayingItem()))
                return;
            
            for (Chapter ch: chapters) {
                double curr = PLAYBACK.getCurrentTime().toMillis();
                double start = ch.getTime().toMillis();
                double end;
                if (chapters.size()-1 == chapters.indexOf(ch)) {
                    end = source.getLength().toMillis(); }
                else {
                    end = chapters.get(1+chapters.indexOf(ch)).getTime().toMillis();
                }
                if (curr>start && curr<=end && !ch.equals(playingChapter.get())) {
                    playingChapter.set(ch);
                    refreshChapters();
                }
            }
        });
        
        entireArea.heightProperty().addListener((Observable o) -> {
            reposition();
        });
        
        // hide the table headers
        infosT.widthProperty().addListener((ObservableValue<? extends Number> ov, Number t, Number t1) -> {
            Pane header = (Pane) infosT.lookup("TableHeaderRow");
            if (header != null && header.isVisible()) {
                header.setMaxHeight(0);
                header.setMinHeight(0);
                header.setPrefHeight(0);
                header.setVisible(false);
                header.setManaged(false);
            }
        });
        chaptersT.widthProperty().addListener((ObservableValue<? extends Number> ov, Number t, Number t1) -> {
            Pane header = (Pane) chaptersT.lookup("TableHeaderRow");
            if (header != null && header.isVisible()) {
                header.setMaxHeight(0);
                header.setMinHeight(0);
                header.setPrefHeight(0);
                header.setVisible(false);
                header.setManaged(false);
            }
        });
    }
    
    @Override
    public void refresh() {
       getSource();
       reposition();
    }

    @Override
    public void OnClosing() {
        PlaylistManager.selectedItemProperty().removeListener(selItemListener);
        PlaylistManager.playingItemProperty().removeListener(plaItemListener);
    }
    
/******************************************************************************/
    
    private void getSource() {
        // get playlist item source
        new_meta = null;
        item = null;
        switch (readMode) {
            case PLAYLIST_SELECTED: item = PlaylistManager.getSelectedItem();
                                    break;
            case PLAYING:  item = PlaylistManager.getPlayingItem();
                                    break;
            case LIBRARY_SELECTED:  item = PlaylistManager.getSelectedItem();
                                    //not yet implemented           
                                    break;
            default:
        }
        if (item == null) {
            new_meta = null;
            populateGUI();
            return;
        }
        Task<Metadata> t = MetadataReader.create(item,
            result -> {
                new_meta = result;
                populateGUI();
            }, 
            ()-> {
                new_meta = null;
                populateGUI();
            }
        );

    }
    
    private void populateGUI() {
        // do nothing if source same as before (avoid pointless update)
        if (new_meta != null && new_meta.same(source)) { return; }
        // assign new data
        source = new_meta;
        
         // update GUI, empty if no source
        if (source == null) {
            sourceL.setText(" No item.");
            infos.clear();
            chapters.clear();
        } else {
            sourceL.setText(item.getName());
            getInfoData();
            getChapterData();
            refresh();
        }       
    }
    
    private void getInfoData() {
        infos.clear();
        infos.setAll(source.getExtended().getComments());
    }
    private void getChapterData() {
        chapters.clear();
        chapters.setAll(source.getExtended().getChapters());
        // include start and end chapters as 1st and last chapter
        // any modifications to these will not be remembered
        if (includeStartEndChapter) {
            chapters.add(0, new Chapter(Duration.ZERO, "Start"));
            chapters.add(new Chapter(source.getLength(), "End"));
        }        
    }

    private void reposition() {
        // resizes 1st table to height=rows*row_height
        int min = 0; // if rows=0 set min_rows allowed so the 'empty table' text shows up
        int infosSize = (infos.size() > min) ? infos.size() : min;
        infosT.setPrefHeight((GUI.font.getSize()+13)*infosSize + 5); // +5 to avoid scrollbar
        //chaptersT.setLayoutY(Configuration.TABLE_ROW_HEIGHT*infosSize + 5);
    }

    private void refreshChapters() {
        for (TableColumn c: chaptersT.getColumns()) {
            c.setVisible(false);
            c.setVisible(true);
        }
    }
    private void refreshInfos() {
        for (TableColumn c: infosT.getColumns()) {
            c.setVisible(false);
            c.setVisible(true);
        }
    }
    
 /************************** FUNCTIONAL METHODS *******************************/
    
    // the following 3 methods are generic chapter/info methods implementing logic
    // behind which type of element is going to be operated with
    @FXML
    public void addElement() {
        if (type.getText().isEmpty()) {
            addChapter();
        } else {
            addInfo();
        }
    }
    @FXML
    public void removeElement() {
        removeChapter();
        removeInfo();
    }
    @FXML
    public void editElement() {
        if (!chaptersT.getSelectionModel().isEmpty())
            editChapter();
        else if (!infosT.getSelectionModel().isEmpty())
            editInfo();
    }
    
    // the next 6 methods folow the concept of: 
    //      1 - change the very metadata itself (causing immediate update of physical data)
    //      2 - update GUI (automatically, thx to ObservableList)
    // also, changing upper table raises the need to reposition() bottom table...
    private void addInfo() {
        source.getExtended().addComment(type.getText(), text.getText());
        getInfoData();
        reposition();
    }
    private void removeInfo() {
        source.getExtended().removeComments(infosT.getSelectionModel().getSelectedItems());
        getInfoData();
        reposition();
    }
    private void addChapter() {
        source.getExtended().addChapter(PLAYBACK.getCurrentTime(), text.getText());
        getChapterData();
    }
    private void removeChapter() {
        source.getExtended().removeChapters(chaptersT.getSelectionModel().getSelectedItems());
        getChapterData();
    }
    private void editChapter() {
        Chapter ch = chaptersT.getSelectionModel().getSelectedItem();
        source.getExtended().editChapter(ch, ch.getTime(), text.getText());
        getChapterData();
        refreshChapters(); // table wont refresh, needs manual refresh
    }
    private void editInfo() {
        CommentExtended c = infosT.getSelectionModel().getSelectedItem();
        source.getExtended().editComment(c, type.getText(), text.getText());
        getInfoData();
        refreshInfos(); // table wont refresh, needs manual refresh
    }
    
/******************************************************************************/
    
    public void playChapter(int index) {
        if (index > chapters.size()-1 || index < 0) { return; }
        
        if (PlaylistManager.getPlayingItem().same(source)) {
            Duration time = chaptersT.getItems().get(index).getTime();
            PLAYBACK.seek(time);
        } else {
            PlaylistManager.addUri(source.getURI());
            PlaylistManager.playLastItem();
            Duration time = chaptersT.getItems().get(index).getTime();
            PLAYBACK.seek(time);           
        }       
    }
    
    @FXML public void playSelectedChapter() {
        int index = chaptersT.getSelectionModel().getSelectedIndex();
        playChapter(index);
    }
    
    @FXML public void playPreviousChapter() {
        int index = chaptersT.getSelectionModel().getSelectedIndex()-1;
        playChapter(index);
    }
    
    @FXML public void playNextChapter() {
        int index = chaptersT.getSelectionModel().getSelectedIndex()+1;
        playChapter(index);
    }
    
}