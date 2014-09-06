/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Explorer;

import Configuration.IsConfig;
import GUI.GUI;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Features.ImageDisplayFeature;
import Layout.Widgets.Widget;
import Layout.Widgets.WidgetInfo;
import Layout.Widgets.WidgetManager;
import de.jensd.fx.fontawesome.AwesomeDude;
import static de.jensd.fx.fontawesome.AwesomeIcon.CSS3;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import static javafx.scene.input.MouseButton.PRIMARY;
import javafx.scene.input.TransferMode;
import static javafx.scene.input.TransferMode.COPY;
import static javafx.scene.input.TransferMode.MOVE;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import main.App;
import utilities.Enviroment;
import utilities.FileUtil;
import utilities.ImageFileFormat;
import utilities.access.Accessor;

/**
 * FXML Controller class
 *
 * @author Plutonium_
 */
@WidgetInfo (
    author = "Martin Polakovic",
    description = "Simple file system browser with drag and copy support.",
    howto = "Available actions:\n" +
            "    Double click file: Open file in native application\n" +
            "    Double click skin file: Apply skin on application\n" +
            "    Drag file from list : Starts drag & drop or copy\n" +
            "    Drag file from list + SHIFT : Starts drag & drop or move\n",
    notes = "",
    version = "0.7",
    year = "2014",
    group = Widget.Group.OTHER
)
public class ExplorerController extends FXMLController {
    
    // gui
    @FXML AnchorPane root;
    @FXML TreeView<File> tree;
    private TreeItem<File> customLocationItem = createTreeItem(App.getLocation());
    
    // auto applied configurables
    @IsConfig(name = "Custom location", info = "Custom location for the directory tree to display.")
    public final Accessor<File> rootDir = new Accessor<>(App.getLocation(), v -> {
        // remove old
        tree.getRoot().getChildren().remove(customLocationItem);
        customLocationItem.getChildren().clear();
        // create new
        customLocationItem = createTreeItem(v.getAbsoluteFile());
        tree.getRoot().getChildren().add(customLocationItem);
    });

    
    @Override
    public void init() {
        tree.setFixedCellSize(GUI.font.getSize() + 5);
        tree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // set value factory
        tree.setCellFactory( treeView -> new TreeCell<File>(){
            {
                setOnMouseClicked( e -> {
                    if(e.getClickCount()==2 && e.getButton()==PRIMARY) {
                        File f = getItem();
                        // handle files
                        if(f.isFile()) {
                            // open skin
                            if(FileUtil.isValidSkinFile(f))
                                GUI.setSkin(FileUtil.getName(f));
                            else if (ImageFileFormat.isSupported(f)) {
                                ImageDisplayFeature idf = WidgetManager.getWidget(ImageDisplayFeature.class, WidgetManager.Widget_Source.FACTORY);
                                if(idf != null) idf.showImage(f);
                            }
                            // open file in native application
                            else
                                Enviroment.open(f);
                        }
                    }
                });
            }
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if(empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Path p = item.toPath().getFileName();
                    String s = p==null ? item.toPath().toString() : p.toString();
                    setText(s);                             // show filenames
                    setGraphic(makeIcon(item.toPath()));    // denote type by icon
                }
            }
        });
        
        // support drag from tree - add selected to clipboard/dragboard
        tree.setOnDragDetected( e -> {
            if (e.getButton()!=MouseButton.PRIMARY) return; // only left button
            if(tree.getSelectionModel().isEmpty()) return;  // if empty
            
            TransferMode tm = e.isShiftDown() ? MOVE : COPY;
            Dragboard db = tree.startDragAndDrop(tm);
            List<File> files = tree.getSelectionModel().getSelectedItems().stream()
                                            .map(tc->tc.getValue())
                                            .collect(Collectors.toList());
            ClipboardContent cont = new ClipboardContent();
                             cont.putFiles(files);
            db.setContent(cont);
            e.consume();
        });
        
        // create invisible root
        tree.setShowRoot(false);
        tree.setRoot(new TreeItem<>(new File("")));
        
        // discover and set particions as roots
        File[] drives = File.listRoots();
        for(int i=0; i<drives.length; i++)
            tree.getRoot().getChildren().add(createTreeItem(drives[i]));
        
        // add custom location
        tree.getRoot().getChildren().add(customLocationItem);
        
        // prevent scrolling event from propagating
        root.setOnScroll(Event::consume);
    }
    
    @Override
    public void OnClosing() {
        tree.getRoot().getChildren().clear();
        tree.setRoot(null);
        customLocationItem.getChildren().clear();
    }
    
/******************************** PUBLIC API **********************************/
    
    @Override
    public void refresh() {
        rootDir.applyValue();
    }
     
/******************************* HELPER METHODS *******************************/
    
    private static Node makeIcon(Path p) {
        if(p.toString().endsWith(".css")) return AwesomeDude.createIconLabel(CSS3,"11");
        if(p.toFile().isFile()) return new Circle(2.5, Color.CADETBLUE);
        else return new Rectangle(5, 5, Color.CADETBLUE);
    }
    
    // This method creates a TreeItem to represent the given File. It does this
    // by overriding the TreeItem.getChildren() and TreeItem.isLeaf() methods 
    // anonymously, but this could be better abstracted by creating a 
    // 'FileTreeItem' subclass of TreeItem. However, this is left as an exercise
    // for the reader.
    private TreeItem<File> createTreeItem(final File f) {
        return new TreeItem<File>(f) {
            // We cache whether the File is a leaf or not. A File is a leaf if
            // it is not a directory and does not have any files contained within
            // it. We cache this as isLeaf() is called often, and doing the 
            // actual check on File is expensive.
            private boolean isLeaf;
            // We do the children and leaf testing only once, and then set these
            // booleans to false so that we do not check again during this
            // run. A more complete implementation may need to handle more 
            // dynamic file system situations (such as where a folder has files
            // added after the TreeView is shown). Again, this is left as an
            // exercise for the reader.
            private boolean isFirstTimeChildren = true;
            private boolean isFirstTimeLeaf = true;

            @Override public ObservableList<TreeItem<File>> getChildren() {
                if (isFirstTimeChildren) {
                    isFirstTimeChildren = false;

                    // First getChildren() call, so we actually go off and 
                    // determine the children of the File contained in this TreeItem.
                    super.getChildren().setAll(buildChildren(this));
                }
                return super.getChildren();
            }

            @Override public boolean isLeaf() {
                if (isFirstTimeLeaf) {
                    isFirstTimeLeaf = false;
                    isLeaf = getValue().isFile();
                }

                return isLeaf;
            }

            private ObservableList<TreeItem<File>> buildChildren(TreeItem<File> TreeItem) {
                File value = TreeItem.getValue();
                if (value != null && value.isDirectory()) {
                    File[] all = value.listFiles();
                    if (all != null) {
                        // we want to sort the items : directories first
                        ObservableList<TreeItem<File>> directories = FXCollections.observableArrayList();
                        List<TreeItem<File>> files = new ArrayList();
                        for (File f : all) {
                            if(f.isFile()) files.add(createTreeItem(f));
                            else directories.add(createTreeItem(f));
                        }
                        directories.addAll(files);
                        return directories;
                    }
                }

                return FXCollections.emptyObservableList();
            }
        };
    }

}