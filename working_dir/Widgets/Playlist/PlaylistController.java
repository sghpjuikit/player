package Playlist;


import AudioPlayer.playlist.PlaylistManager;
import Configuration.IsConfig;
import GUI.objects.PlaylistTable;
import Layout.Widgets.SupportsTagging;
import Layout.Widgets.WidgetController;
import Layout.Widgets.WidgetManager;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.Observable;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import utilities.AudioFileFormat;
import utilities.FileUtil;
import utilities.Util;



/**
 * Playlist FXML Controller class
 * Controls behavior of the Playlist FXML graphics.
 */
public class PlaylistController extends WidgetController {

    @FXML
    AnchorPane root;
    @FXML
    TextField searchBox;
    @FXML
    Label duration;
    @FXML
    AnchorPane tablePane;
    private  PlaylistTable table;
    
    // properties
    @IsConfig(name = "Table orientation", info = "Orientation of table.")
    public NodeOrientation table_orient = NodeOrientation.INHERIT;
    @IsConfig(name = "Table text orientation", info = "Orientation of text within table cells.")
    public Pos cell_align = Pos.CENTER_LEFT;
    @IsConfig(name = "Zeropad numbers", info = "Adds 0 to uphold number length consistency.")
    public boolean zeropad = false;
    @IsConfig(name = "Show table menu button", info = "Show table menu button for controlling columns.")
    public boolean show_menu_button = true;
    @IsConfig(name = "Show table header", info = "Show table header with columns.")
    public boolean show_header = true;
    @IsConfig(name = "Default open location", info = "Opens this location for file dialogs.")
    public String browse = "";
    @IsConfig(name = "File search depth", info = "Depth for recursive file search for folders.")
    public int folder_depth = 1;
    
    @Override
    public void initialize() {
        searchBox.setPromptText("search playlist");
        table = new PlaylistTable(tablePane);

        PlaylistManager.lengthProperty().addListener( o -> {
            duration.setText(Util.formatDuration(PlaylistManager.getLength()));
        }); // must be initialized manually though
        duration.setText(Util.formatDuration(PlaylistManager.getLength()));
    }
    
    @Override
    public void refresh() {
        table.zeropadIndex(zeropad);
        table.setNodeOrientation(table_orient);
        table.setCellAlign(cell_align);
        table.setMenuButtonVisible(show_menu_button);
        table.setHeaderVisible(show_header);
        table.refresh();
    }
    
    @FXML
    public void chooseFiles() {
        File init = new File(browse);
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose Audio Files");
        if (FileUtil.isValidDirectory(init))
            fc.setInitialDirectory(init);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "supported audio", AudioFileFormat.extensions()));
        List<File> files = fc.showOpenMultipleDialog(root.getScene().getWindow());
        if (files != null) {
            browse =  files.get(0).getParent();
            List<URI> queue = new ArrayList<>();
            files.forEach(f -> queue.add(f.toURI()));
            table.enqueueItems(queue);
        }
    }
    
    @FXML
    public void chooseFolder() {
        File init = new File(browse);
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Choose Audio Files From Directory Tree");
        if (FileUtil.isValidDirectory(init)) 
            dc.setInitialDirectory(init);
        File dir = dc.showDialog(root.getScene().getWindow());
        if (dir != null) {
            browse = dir.getPath();
            List<URI> queue = new ArrayList<>();
            List<File> files = FileUtil.getAudioFiles(dir, folder_depth);
            files.forEach(f -> queue.add(f.toURI()));
            table.enqueueItems(queue);
        }
    }
    
    @FXML
    public void chooseUrl() {
        // to implement
    }
    
    @FXML
    public void removeSelectedItems() {
        PlaylistManager.removeSelectedItems();
    }
    @FXML
    public void removeUnselectedItems() {
        PlaylistManager.removeUnselectedItems();
    }
    @FXML
    public void removeUnplayableItems() {
        PlaylistManager.removeCorrupt();
    }
    @FXML
    public void removeDuplicateItems() {
        PlaylistManager.removeDuplicates();
    }
    @FXML
    public void removeAllItems() {
        PlaylistManager.removeAllItems();
    }
    @FXML
    public void duplicateSelectedItemsAsGroup() {
        PlaylistManager.duplicateItemsAsGroup(PlaylistManager.getSelectedItems());
    }
    @FXML
    public void duplicateSelectedItemsByOne() {
        PlaylistManager.duplicateItemsByOne(PlaylistManager.getSelectedItems());
    }
    @FXML
    public void SelectAll() {
        table.selectAll();
    }
    @FXML
    public void SelectInverse() {
        table.selectInverse();
    }
    @FXML
    public void SelectNone() {
        table.selectNone();
    }
    @FXML
    public void sortByTitle() {
        table.sortByTitle();
    }
    @FXML
    public void sortByLength() {
        table.sortByLength();
    }
    @FXML
    public void sortByFilename() {
        table.sortByFilename();
    }
    @FXML
    public void reverseOrder() {
        PlaylistManager.reversePlaylist();
    }
    @FXML
    public void randomOrder() {
        PlaylistManager.randomizePlaylist();
    }
    @FXML
    public void tagEditSelected() {
        SupportsTagging t = WidgetManager.getTaggerOrCreate();
        if (t!=null) t.read(PlaylistManager.getSelectedItems());
    }
    @FXML
    public void savePlaylist() {
        PlaylistManager.saveActivePlaylist();
    }
    @FXML
    public void saveSelectedAsPlaylist() {
        // to implement
    }
}