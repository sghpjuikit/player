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
import GUI.objects.FileTree;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Widget;
import Layout.Widgets.Widget.Info;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.*;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import javafx.scene.layout.AnchorPane;
import main.App;
import util.File.Enviroment;
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
    private TreeItem<File> customLocationItem = FileTree.createTreeItem(App.getLocation());
    
    // auto applied configurables
    @IsConfig(name = "Custom location", info = "Custom location for the directory tree to display.")
    public final Accessor<File> rootDir = new Accessor<>(App.getLocation(), v -> {
        // remove old
        tree.getRoot().getChildren().remove(customLocationItem);
        customLocationItem.getChildren().clear();
        // create new
        customLocationItem = FileTree.createTreeItem(v.getAbsoluteFile());
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
        FileTree.from(tree);
        
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

        
        // create invisible root
        tree.setShowRoot(false);
        tree.setRoot(new TreeItem<>(new File("")));
        
        // discover and set particions as roots
        File[] drives = File.listRoots();
        for(int i=0; i<drives.length; i++)
            tree.getRoot().getChildren().add(FileTree.createTreeItem(drives[i]));
        
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