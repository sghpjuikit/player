/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Dirs;

import Configuration.IsConfig;
import GUI.DragType;
import Layout.Widgets.FXMLController;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import main.App;
import org.fxmisc.livedirs.LiveDirs;

/**
 * FXML Controller class
 *
 * @author Plutonium_
 */
public class DirsController extends FXMLController {
    
    @FXML AnchorPane root;
    @FXML TreeView<Path> tree;
    
    @IsConfig(name = "Root directory", info = "Root directory path for the directory tree to display.")
    public File rootDir = App.getAppLocation();
    @IsConfig(name = "Drag type", info = "Indicates type of dragging action.")
    public DragType dragType = DragType.COPY;
    
    LiveDirs<ChangeSource> liveDirs;
    
    @Override
    public void init() {
        try {
            // create LiveDirs to watch a directory
            liveDirs = new LiveDirs(ChangeSource.EXTERNAL);
            
            tree.setShowRoot(false);
            tree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            
            // set value factory
            tree.setCellFactory((TreeView<Path> param) -> {
                return new TreeCell<Path>(){
                    @Override
                    protected void updateItem(Path item, boolean empty) {
                        super.updateItem(item, empty);
                        if(empty || item== null) {this.
                            setText(null);
                            setGraphic(null);
                        } else {
                            setText(item.getFileName().toString()); // show filenames
                            setGraphic(makeIcon(item));             // denote type by icon
                        }
                    }
                };
            });
            
        } catch (IOException ex) {
            Logger.getLogger(DirsController.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // supprt drag from tree - add selected to clipboard/dragboard
        tree.setOnDragDetected( e -> {
            if (e.getButton()!=MouseButton.PRIMARY) return; // only left button
            if(tree.getSelectionModel().isEmpty()) return;  // if empty
            if(dragType==DragType.NONE) return;             // if no drag support
            
            Dragboard db = tree.startDragAndDrop(dragType.val());
            List<File> files = tree.getSelectionModel().getSelectedItems().stream()
                                            .map(tc->tc.getValue().toFile())
                                            .collect(Collectors.toList());
            ClipboardContent cont = new ClipboardContent();
                             cont.putFiles(files);
            db.setContent(cont);
            e.consume();
        });
    }
    
    @Override
    public void refresh() {
        Path dir = rootDir.getAbsoluteFile().toPath();
        liveDirs.addTopLevelDirectory(dir);
        tree.setRoot(liveDirs.model().getRoot());
    }
    
    @Override
    public void OnClosing() {
        Thread t = new Thread(()->liveDirs.dispose());
        t.setDaemon(true);
        t.start();
    }
    
    
    

    
    private static Node makeIcon(Path p) {
        if(p.toFile().isFile()) return new Circle(2.5, Color.CADETBLUE);
        else return new Rectangle(5, 5, Color.CADETBLUE);
    }
    
    private static enum ChangeSource {
        INTERNAL, // indicates a change made by this application
        EXTERNAL  // indicates an external change
    }    
}
