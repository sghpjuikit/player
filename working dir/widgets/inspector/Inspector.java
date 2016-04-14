
package inspector;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import layout.widget.Widget;
import layout.widget.controller.ClassController;
import layout.widget.controller.io.Output;
import layout.widget.feature.FileExplorerFeature;
import gui.objects.tree.TreeItems;
import util.graphics.drag.DragUtil;

import static layout.widget.Widget.Group.APP;
import static javafx.css.PseudoClass.getPseudoClass;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import static util.graphics.Util.setAnchor;

@Widget.Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Inspector",
    description = "Inspects application as hierarchy. Displays windows, widgets,"
                + "file system and more. Allows editing if possible.",
    howto = "Available actions:\n"
          + "    Right click: Open context menu\n"
          + "    Double click file: Open file in native application\n"
          + "    Double click skin file: Apply skin on application\n"
          + "    Double click widget file: Open widget\n"
          + "    Drag & drop file: Explore file\n"
          + "    Drag & drop files: Explore files' first common parent directory\n",
    notes = "",
    version = "0.8",
    year = "2015",
    group = APP
)
public class Inspector extends ClassController implements FileExplorerFeature {

    private static final PseudoClass csPC = getPseudoClass("configselected");
    private Node sel_node = null;
    private TreeView<Object> tree = new TreeView<>();

    private Output<Object> out_sel;

    public Inspector() {
        setAnchor(this, tree,0d);

        tree.getSelectionModel().setSelectionMode(MULTIPLE);
        tree.setCellFactory(TreeItems::buildTreeCell);
        tree.setRoot(TreeItems.treeApp());
        (((TreeItems.SimpleTreeItem)tree.getRoot().getChildren().get(3))).showLeaves.set(false);
        tree.getSelectionModel().selectedItemProperty().addListener((o,ov,nv) -> {
            Object oi = ov==null ? null : ov.getValue();
            Object ni = nv==null ? null : nv.getValue();

            out_sel.setValue(ni);

            // selected node highlighting
            if(sel_node!=null) {
                sel_node.pseudoClassStateChanged(csPC, false);
                sel_node.setStyle("");
                sel_node = null;
            }
            if(ni instanceof Node) {
                Node n = (Node)ni;
                sel_node = n;
                n.pseudoClassStateChanged(csPC, true);
                n.setStyle("-fx-background-color: rgba(90,200,200,0.2);");
            }
        });

        setOnDragOver(DragUtil.fileDragAccepthandler);
        setOnDragDropped(e -> exploreFiles(DragUtil.getFiles(e)));

        // prevent scrolling event from propagating
        setOnScroll(Event::consume);
    }

    @Override
    public void init() {
        out_sel = outputs.create(widget.id,"Selected", Object.class, null);
    }

    @Override
    public void exploreFile(File f) {
        TreeItem<File> root = (TreeItem) tree.getRoot().getChildren().get(3);

        // expand up to root + partitions
        tree.getRoot().setExpanded(true);
        root.setExpanded(true);

        // expand the most possible
        Path p = f.toPath().getRoot();
        Optional<TreeItem<File>> item = root.getChildren().stream().filter(i -> i.getValue().toString().contains(f.toPath().getRoot().toString())).findFirst();
        item.ifPresent(e -> e.setExpanded(true));
        ObjectProperty<TreeItem<File>> it = new SimpleObjectProperty(item.orElse(null));

        f.getAbsoluteFile().toPath().forEach(pth -> {
            if(it.get()==null) return;
            else {
                it.get().setExpanded(true);
                it.set(it.get().getChildren().stream().filter(i -> i.getValue().toString().contains(pth.toString())).findFirst().orElse(null));
            }
        });

        // expand if last==directory
        if(it.get()!=null && it.get().getValue().isDirectory()) it.get().setExpanded(true);

    }

    @Override
    public void onClose() {
        if(sel_node!=null) unhighlightNode(sel_node);
    }



    private static void highlightNode(Node n) {
        n.pseudoClassStateChanged(csPC, true);
        n.setStyle("-fx-background-color: rgba(90,200,200,0.2);");
    }
    private static void unhighlightNode(Node n) {
        n.pseudoClassStateChanged(csPC, false);
        n.setStyle("");
    }
}
