/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.WidgetImpl;

import AudioPlayer.playlist.NamedPlaylist;
import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.PlaylistCategorizer;
import GUI.DragUtil;
import GUI.objects.Table.PlaylistTableSimple;
import PseudoObjects.Category;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.Observable;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Accordion;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import util.dev.Log;

/**
 * FXML Controller class
 *
 * @author uranium
 */
public class PlaylistManagerComponent extends AnchorPane {
    
    @FXML
    TreeView<String> categoryTree;
    @FXML
    Accordion tables;

    public PlaylistManagerComponent() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("PlaylistManager.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            Log.err("TaggerComponent source data coudlnt be read.");
        }        
    }
    
    public void initialize() {        
        TreeItem<String> root = new TreeItem<>("Playlists");
        root.setExpanded(true);
        categoryTree.setRoot(root);
        categoryTree.setShowRoot(false);
        
        // refresh playlists and categories
        PlaylistCategorizer.findCategories();
        
        // poulate all playlists tree
        TreeItem<String> all = new TreeItem<>("All playlists ");
            for(Playlist p: PlaylistCategorizer.getPlaylists()) {
                TreeItem pi = new TreeItem(p);
                all.getChildren().add(pi);
            }
        // populate all categories tree
        TreeItem<String> allc = new TreeItem<>("All Categories");
            for(Category c: PlaylistCategorizer.getCategories()) {
                TreeItem i = new TreeItem(c);
                allc.getChildren().add(i);
                for (Playlist p: c.getPlaylists()) {
                    TreeItem pi = new TreeItem(p);
                    i.getChildren().add(pi);     
                }
            }
        // populate hierarchical categories tree
        TreeItem<String> hier = new TreeItem<>("Hierarchy");
        for(Category c: PlaylistCategorizer.getHierarchy()) {
            buildBranch(hier, c);
        }
        // populate no category playlists tree
        TreeItem<String> no = new TreeItem<>("No category ");
            for(Playlist p: PlaylistCategorizer.getPlaylists(null)) {
                TreeItem pi = new TreeItem(p);
                no.getChildren().add(pi);
            }
        // add branches
        root.getChildren().add(all);
        root.getChildren().add(allc);
        root.getChildren().add(hier);
        root.getChildren().add(no);
        
        // allow multiple selections
        categoryTree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // show playlists if clicked
        categoryTree.getSelectionModel().getSelectedItems().addListener((Observable o) -> {
            tables.getPanes().clear();
            for(final NamedPlaylist p: getAllSelectedPlaylists()) {
                final TitledPane t = new TitledPane();
                // set playlist name as title
                t.setText(p.getName());
                // set action - if opened set up the table and its data
                t.expandedProperty().addListener((Observable ob) -> {
                    if (t.isExpanded() == true)
                        setTable(t, p);
                });
                // allow dragging of the playlist from TitledPane object
                t.setOnDragDetected(e -> {
                    Dragboard db = t.startDragAndDrop(TransferMode.ANY);
                    DragUtil.setPlaylist(p, db);
                    e.consume();
                });
                // add to accordion tables
                tables.getPanes().add(t);
            }
        });
    }
    
    /**
     * Sets table to concrete titled pane and adds Playlist as its data source 
     */
    private void setTable(TitledPane pane, NamedPlaylist p) {
        // titledPane Y=0 is wrong as its behind title!
        double paddingUp = 25;
        // stylistic choice to make the accordion more readable
        double paddingLeft = 25;
        // set table
        PlaylistTableSimple table1 = new PlaylistTableSimple();
        table1.setVisible(true);
        table1.minWidthProperty().bind(pane.widthProperty().subtract(paddingLeft)); 
        table1.minHeightProperty().bind(pane.heightProperty().subtract(paddingUp));
        table1.maxWidthProperty().bind(pane.widthProperty().subtract(paddingLeft));
        table1.maxHeightProperty().bind(pane.heightProperty().subtract(paddingUp));
        table1.prefHeightProperty().bind(pane.widthProperty().subtract(paddingLeft));
        table1.prefWidthProperty().bind(pane.heightProperty().subtract(paddingUp));
        table1.setLayoutX(0+paddingLeft);
        table1.setLayoutY(0+paddingUp);
        table1.setPlaylist(p);
        pane.setContent(table1);
    }
    
    /**
     * Returns all playlists that are selected or are child of any of the selected
     * TreeItem
     */
    private List<NamedPlaylist> getAllSelectedPlaylists() {
        List<NamedPlaylist> playlists = new ArrayList<>();
        for(Object o: categoryTree.getSelectionModel().getSelectedItems()) {
            getAllPlaylists((TreeItem<?>)o, playlists);
        }
        return playlists;
    }
    
    /**
     * Recursively finds all playlists of specified TreeItem and adds them to
     * specified list.
     * @param item
     * @param list 
     */
    private void getAllPlaylists(TreeItem<?> item, List<NamedPlaylist> list) {
        if ( item.getValue() instanceof Playlist ) {
            NamedPlaylist p = (NamedPlaylist)item.getValue();
            if (!list.contains(p)) {
                list.add(p);
            }
        }
        for(TreeItem<?> i: item.getChildren()) {
            getAllPlaylists(i, list);
        }
    }
    
    private void buildBranch(TreeItem<?> item, Category category) {
        for (Category c: category.getChildren()) {
            TreeItem i = new TreeItem(c.getName());
            item.getChildren().add(i);
            buildBranch(i, c);
        }
    }
}