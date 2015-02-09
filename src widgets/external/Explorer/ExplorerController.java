/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Explorer;

import Configuration.IsConfig;
import GUI.GUI;
import GUI.objects.ContextMenu.ContentContextMenu;
import GUI.objects.ContextMenu.TreeContextMenuInstance;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Widget;
import Layout.Widgets.Widget.Info;
import de.jensd.fx.fontawesome.AwesomeDude;
import static de.jensd.fx.fontawesome.AwesomeIcon.CSS3;
import static de.jensd.fx.fontawesome.AwesomeIcon.GE;
import static de.jensd.fx.fontawesome.AwesomeIcon.PAINT_BRUSH;
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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import javafx.scene.input.TransferMode;
import static javafx.scene.input.TransferMode.COPY;
import static javafx.scene.input.TransferMode.MOVE;
import javafx.scene.layout.AnchorPane;
import static javafx.scene.paint.Color.CADETBLUE;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import main.App;
import util.File.Enviroment;
import util.File.FileUtil;
import static util.Util.createmenuItem;
import util.access.Accessor;

/**
 * FXML Controller class
 *
 * @author Plutonium_
 */
@Info (
    author = "Martin Polakovic",
    description = "Simple file system browser with drag and copy support.",
    howto = "Available actions:\n" +
            "    Double click file: Open file in native application\n" +
            "    Double click skin file: Apply skin on application\n" +
            "    Double click widget file: Open widget\n" +
            "    Right click: Open context menu\n" +
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
    
    // non-applied configurables
    @IsConfig(name = "Open files in application if possible", info = "Open files by this"
            + " application if possible on doubleclick, rather than always use native OS application.")
    public boolean openInApp = true;
    
    @Override
    public void init() {
        tree.setFixedCellSize(GUI.font.getValue().getSize() + 5);
        tree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        
        // set value factory
        tree.setCellFactory( treeView -> new TreeCell<File>() {
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
        
        // context menu & open file
        tree.setOnMouseClicked( e -> {
            if (e.getButton()==PRIMARY) {
                if(e.getClickCount()==2) {
                    File f = tree.getSelectionModel().getSelectedItem().getValue();
                    if(f!=null && (f.isFile() || Enviroment.isOpenableInApp(f)))
                        Enviroment.openIn(f, openInApp);
                }
            } else
            if(e.getButton()==SECONDARY) {
                contxt_menu.show(tree, e);
            }
        });
                
        // support drag from tree - add selected to clipboard/dragboard
        tree.setOnDragDetected( e -> {
            if (e.getButton()!=MouseButton.PRIMARY) return;
            if(tree.getSelectionModel().isEmpty()) return;
            
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
    public void close() {
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
        File f = p.toFile();
        if(p.toString().endsWith(".css"))
            return AwesomeDude.createIconLabel(CSS3,"11");
        if((f.isDirectory() && App.SKIN_FOLDER().equals(p.toFile().getParentFile())) || FileUtil.isValidSkinFile(p.toFile()))
            return AwesomeDude.createIconLabel(PAINT_BRUSH,"11");
        if((f.isDirectory() && App.WIDGET_FOLDER().equals(p.toFile().getParentFile())) || FileUtil.isValidWidgetFile(p.toFile()))
            return AwesomeDude.createIconLabel(GE,"11");
        
        if(f.isFile()) return new Circle(2.5, CADETBLUE);
        else return new Rectangle(5, 5, CADETBLUE);
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
/****************************** CONTEXT MENU **********************************/
    
    private static final TreeContextMenuInstance<File> contxt_menu = new TreeContextMenuInstance<File>(
        () -> {
            ContentContextMenu<List<File>> m = new ContentContextMenu<>();
            m.getItems().addAll(
                createmenuItem("Open", e -> {
                    Enviroment.open(m.getValue().get(0));
                }),
                createmenuItem("Open in-app", e -> {
                    Enviroment.openIn(m.getValue(),true);
                }),
                createmenuItem("Edit", e -> {
                    Enviroment.edit(m.getValue().get(0));
                }),
                createmenuItem("Copy", e -> {
                    ClipboardContent cc = new ClipboardContent();
                    cc.put(DataFormat.FILES, m.getValue());
                    Clipboard.getSystemClipboard().setContent(cc);
                }),
                createmenuItem("Explore in browser", e -> {
                    Enviroment.browse(m.getValue(),true);
                })
            );
            return m;
        },
        (menu,tree) -> {
            List<File> files = tree.getSelectionModel().getSelectedItems().stream()
                    .map(t->t.getValue())
                    .collect(Collectors.toList());
            menu.setValue(files);
            
            if(files.isEmpty()) {
                menu.getItems().forEach(i -> i.setDisable(true));
            } else if(files.size()==1) {
                menu.getItems().forEach(i -> i.setDisable(false));
                menu.getItems().get(2).setDisable(!files.get(0).isFile());
            } else {
                menu.getItems().forEach(i -> i.setDisable(false));
                menu.getItems().get(0).setDisable(true);
                menu.getItems().get(2).setDisable(true);
            }
        }
    );
    
}