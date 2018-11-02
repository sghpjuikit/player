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
import sp.it.pl.gui.objects.tree.TreeItemsKt;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.controller.SimpleController;
import sp.it.pl.layout.widget.controller.io.Output;
import sp.it.pl.layout.widget.feature.FileExplorerFeature;
import sp.it.pl.layout.widget.feature.Opener;
import sp.it.pl.main.Widgets;
import sp.it.pl.util.graphics.drag.DragUtil;
import static javafx.css.PseudoClass.getPseudoClass;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import static sp.it.pl.layout.widget.Widget.Group.APP;
import static sp.it.pl.util.graphics.Util.setAnchor;
import static sp.it.pl.util.graphics.UtilKt.expandAndSelect;
import static sp.it.pl.util.graphics.UtilKt.propagateESCAPE;

@Widget.Info(
    author = "Martin Polakovic",
    name = Widgets.INSPECTOR,
    description = "Inspects application as hierarchy. Displays windows, widgets,"
                + "file system and more. Allows editing if possible.",
    howto = "Available actions:\n"
          + "    Right click: Open context menu\n"
          + "    Double click file: Open file in native application\n"
          + "    Double click skin file: Apply skin on application\n"
          + "    Double click widget file: Open widget\n"
          + "    Drag & drop file: Explore file\n"
          + "    Drag & drop files: Explore files' first common parent directory\n",
    version = "0.8",
    year = "2015",
    group = APP
)
public class Inspector extends SimpleController implements FileExplorerFeature, Opener {

    private static final PseudoClass selectedPC = getPseudoClass("selected");
    private Node sel_node = null;
    private TreeView<Object> tree = TreeItemsKt.buildTreeView();
    private Output<Object> out_sel;

    public Inspector(Widget<?> widget) {
        super(widget);

        setAnchor(this, tree,0d);

        tree.getSelectionModel().setSelectionMode(MULTIPLE);
        tree.setCellFactory(TreeItemsKt::buildTreeCell);
        tree.setRoot(TreeItemsKt.treeApp());
        tree.getSelectionModel().selectedItemProperty().addListener((o,ov,nv) -> {
            Object oi = ov==null ? null : ov.getValue();
            Object ni = nv==null ? null : nv.getValue();

            out_sel.setValue(ni);

            // selected node highlighting
            if (sel_node!=null) {
                sel_node.pseudoClassStateChanged(selectedPC, false);
                sel_node.setStyle("");
                sel_node = null;
            }
            if (ni instanceof Node) {
                sel_node = (Node) ni;
                highlightNode(sel_node);
            }
        });
	    propagateESCAPE(tree);

        setOnDragOver(DragUtil.fileDragAcceptHandler);
        setOnDragDropped(e -> exploreFiles(DragUtil.getFiles(e)));

        // prevent scrolling event from propagating
        setOnScroll(Event::consume);
    }

    @Override
    public void init() {
        out_sel = outputs.create(widget.id,"Selected", Object.class, null);
        onClose.plusAssign(() -> unhighlightNode(sel_node));
    }

    @SuppressWarnings("unchecked")
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
        ObjectProperty<TreeItem<File>> it = new SimpleObjectProperty<>(item.orElse(null));

        f.getAbsoluteFile().toPath().forEach(pth -> {
            if (it.get()!=null) {
                it.get().setExpanded(true);
                it.set(it.get().getChildren().stream().filter(i -> i.getValue().toString().contains(pth.toString())).findFirst().orElse(null));
            }
        });

        // expand if last==directory
        if (it.get()!=null && it.get().getValue().isDirectory()) it.get().setExpanded(true);

    }

    @Override
    public void open(Object data) {
        TreeItem<Object> item = TreeItemsKt.tree(data);
        tree.getRoot().getChildren().add(item);
        expandAndSelect(tree, item);
    }

    private static void highlightNode(Node n) {
        if (n==null) return;
        n.pseudoClassStateChanged(selectedPC, true);
        n.setStyle("-fx-background-color: rgba(90,200,200,0.2);");
    }

    private static void unhighlightNode(Node n) {
        if (n==null) return;
        n.pseudoClassStateChanged(selectedPC, false);
        n.setStyle("");
    }
}