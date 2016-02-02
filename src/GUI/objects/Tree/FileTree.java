/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.tree;

import de.jensd.fx.glyphs.GlyphsDude;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;

import static javafx.scene.input.TransferMode.COPY;
import static javafx.scene.input.TransferMode.MOVE;
import static javafx.scene.paint.Color.CADETBLUE;

import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import util.file.FileUtil;

import static main.App.APP;
import static util.file.FileUtil.listFiles;

/**
 <p>
 @author Plutonium_
 */
public class FileTree extends TreeView<File>{

    public FileTree() {
        from(this);
    }


    public static void from(TreeView<File> tree) {
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
    }


    private static Node makeIcon(Path p) {
        File f = p.toFile();
        if(p.toString().endsWith(".css"))
            return GlyphsDude.createIcon(CSS3,"11");
        if((f.isDirectory() && APP.DIR_SKINS.equals(p.toFile().getParentFile())) || FileUtil.isValidSkinFile(p.toFile()))
            return GlyphsDude.createIcon(PAINT_BRUSH,"11");
        if((f.isDirectory() && APP.DIR_WIDGETS.equals(p.toFile().getParentFile())) || FileUtil.isValidWidgetFile(p.toFile()))
            return GlyphsDude.createIcon(GE,"11");

        if(f.isFile()) return new Circle(2.5, CADETBLUE);
        else return new Rectangle(5, 5, CADETBLUE);
    }

    // This method creates a TreeItem to represent the given File. It does this
    // by overriding the TreeItem.getChildren() and TreeItem.isLeaf() methods
    // anonymously, but this could be better abstracted by creating a
    // 'FileTreeItem' subclass of TreeItem. However, this is left as an exercise
    // for the reader.
    public static TreeItem<File> createTreeItem(final File f) {
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

            private ObservableList<TreeItem<File>> buildChildren(TreeItem<File> i) {
                // we want to sort the items : directories first
                // we make use of the fact that listFiles() gives us already
                // sorted list
                ObservableList<TreeItem<File>> dirs = FXCollections.observableArrayList();
                List<TreeItem<File>> fils = new ArrayList();
                listFiles(i.getValue()).forEach(f -> {
                    if(!f.isDirectory()) dirs.add(createTreeItem(f));
                    else                 fils.add(createTreeItem(f));
                });
                       dirs.addAll(fils);
                return dirs;
            }
        };
    }
}
