/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.tree;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

import services.Service;
import layout.Component;
import layout.container.Container;
import layout.container.layout.Layout;
import layout.widget.Widget;
import layout.widget.WidgetFactory;
import layout.widget.WidgetManager.WidgetSource;
import layout.widget.feature.ConfiguringFeature;
import layout.widget.feature.Feature;
import gui.objects.ContextMenu.ImprovedContextMenu;
import gui.objects.window.stage.Window;
import util.ClassName;
import util.access.V;
import util.conf.Configurable;
import util.file.Environment;
import util.file.FileUtil;

import static layout.widget.WidgetManager.WidgetSource.*;
import static java.util.stream.Collectors.toList;
import static javafx.collections.FXCollections.observableArrayList;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static main.App.APP;
import static util.Util.*;
import static util.conf.Configurable.configsFromFxPropertiesOf;
import static util.file.FileUtil.listFiles;
import static util.functional.Util.*;

/**
 *
 * @author Plutonium_
 */
public class TreeItems {

    public static SimpleTreeItem<? extends Object> tree(Object o) {
        if(o instanceof TreeItem)       return (SimpleTreeItem)o;
        if(o instanceof Widget)         return new WidgetItem((Widget)o);
        if(o instanceof WidgetFactory)  return new SimpleTreeItem<>(o);
        if(o instanceof Widget.Group)   return new STreeItem(o,()->APP.widgetManager.findAll(OPEN).filter(w->w.getInfo().group()==o).sorted(by(w -> w.getName())));
        if(o instanceof WidgetSource)   return new STreeItem(o,()->APP.widgetManager.findAll((WidgetSource)o).sorted(by(w -> w.getName())));
        if(o instanceof Feature)        return new STreeItem(((Feature)o).name(), () -> APP.widgetManager.getFactories().filter(f -> f.hasFeature(((Feature)o))).sorted(by(f -> f.nameGui())));
        if(o instanceof Container)      return new LayoutItem((Component)o);
        if(o instanceof File)           return new FileTreeItem((File)o);
        if(o instanceof Node)           return new NodeTreeItem((Node)o);
        if(o instanceof Window)         return new STreeItem(o,() -> stream(((Window)o).getStage().getScene().getRoot(),((Window)o).getLayout()));
        return new SimpleTreeItem<>(o);
    }

    public static SimpleTreeItem<Object> tree(Object v, Object... children) {
        SimpleTreeItem<Object> t = new SimpleTreeItem<>(null);
        t.setValue(v);
        t.getChildren().addAll((List)trees(children));
        return t;
    }

    public static SimpleTreeItem<Object> tree(Object v, List<? extends Object> cs) {
        return new SimpleTreeItem(v,cs.stream());
    }

    public static SimpleTreeItem<Object> tree(Object v, Stream<? extends Object> cs) {
        return new STreeItem(v, () -> cs);
    }

    public static SimpleTreeItem<Object> tree(Object v, Supplier<Stream<? extends Object>> cs) {
        return new STreeItem(v, cs);
    }

    public static SimpleTreeItem<Object> treeApp() {
        TreeItem widgetT = tree("Widgets",
                     tree("Categories", (List)list(Widget.Group.values())),
                     tree("Types", () -> APP.widgetManager.getFactories().sorted(by(WidgetFactory::nameGui))),
                     tree("Open", (List)list(ANY,LAYOUT,STANDALONE)),
                     tree("Features", () -> APP.widgetManager.getFeatures().sorted(by(Feature::name)))
                   );
        return tree("App",
                 tree("Behavior",
                   widgetT,
                   tree("Services", () -> APP.services.getAllServices().sorted(by(s -> ClassName.of(s.getClass()))))
                 ),
                 tree("UI",
                   widgetT,
                   tree("Windows", () -> APP.windowManager.windows.stream()),
                   tree("Layouts", () -> APP.widgetManager.getLayouts().sorted(by(Layout::getName)))
                 ),
                 tree("Location", listFiles(APP.DIR_APP)),
                 tree("File system", map(File.listRoots(),FileTreeItem::new))
               );
    }

    public static List<SimpleTreeItem<? extends Object>> trees(Object... children) {
        return (stream(children).map(TreeItems::tree).collect(toList()));
    }

    public static List<SimpleTreeItem<? extends Object>> trees(Collection<Object> children) {
        return children.stream().map(TreeItems::tree).collect(toList());
    }

    public static List<SimpleTreeItem<? extends Object>> trees(Stream<? extends Object> children) {
        return children.map(TreeItems::tree).collect(toList());
    }

/**************************************************************************************************/

    public static <T> TreeCell<T> buildTreeCell(TreeView<T> t) {
        return new TreeCell<>(){
            {
                setOnMouseClicked(e -> {
                    T o = getItem();
                    // context menu
                    if(o!=null && e.getButton()==SECONDARY && e.getClickCount()==1) {
                        if(!isSelected()) getTreeView().getSelectionModel().clearAndSelect(getIndex());
                        showMenu(o, getTreeView(), this, e);
                    }
                    // custom action
                    if(o!=null && e.getButton()==PRIMARY) {
                        if(e.getClickCount()==1) doOnSingleClick(o);
                        else if(e.getClickCount()==2) doOnDoubleClick(o);
                    }
                });
            }
            @Override
            protected void updateItem(T o, boolean empty) {
                super.updateItem(o, empty);
                if(!empty && o!=null) {
                    if(o instanceof Component)      setText(((Component)o).getName());
                    else if(o instanceof Service)   setText(((Service)o).getClass().getSimpleName());
                    else if(o instanceof WidgetFactory)  setText(((WidgetFactory)o).nameGui());
                    else if(isEnum(o.getClass()))   setText(util.Util.enumToHuman(o.toString()));
                    else if(o instanceof File)      setText(FileUtil.getNameFull((File)o));
                    else if(o instanceof Node)      setText(toS((Node)o));
                    else if(o instanceof Window)    setText(windowToName((Window)o));
                    else setText(o.toString());
                } else {
                    setGraphic(null);
                    setText(null);
                }
            }
        };
    }

    public static void doOnDoubleClick(Object o) {
        if(o instanceof Configurable) APP.widgetManager.use(ConfiguringFeature.class, ANY, w -> w.configure((Configurable)o));
        if(o instanceof Node) APP.widgetManager.use(ConfiguringFeature.class, ANY, w -> w.configure(configsFromFxPropertiesOf(o)));
        if(o instanceof File) {
            File f = (File)o;
            if (f.isFile() || Environment.isOpenableInApp(f)) Environment.openIn(f, true);
        }
    }

    public static void doOnSingleClick(Object o) {}

    public static <T> void showMenu(T o, TreeView<T> t, Node n, MouseEvent e) {
        if(o instanceof File) {
            List<File> files = filterMap(t.getSelectionModel().getSelectedItems(), c->c.getValue() instanceof File, c->(File)c.getValue());
            if(files.isEmpty()) {
                m.getItems().forEach(i -> i.setDisable(true));
            } else if(files.size()==1) {
                m.getItems().forEach(i -> i.setDisable(false));
                m.getItems().get(2).setDisable(!files.get(0).isFile());
            } else {
                m.getItems().forEach(i -> i.setDisable(false));
                m.getItems().get(0).setDisable(true);
                m.getItems().get(2).setDisable(true);
            }
            m.setValue(files);
            m.show(n, e);
        }
    }

    private static ImprovedContextMenu<List<File>> m = new ImprovedContextMenu<>(){{
        getItems().addAll(
            menuItem("Open", e -> Environment.open(getValue().get(0))),
            menuItem("Open in-app", e -> Environment.openIn(getValue(),true)),
            menuItem("Edit", e -> Environment.edit(getValue().get(0))),
            menuItem("Copy", e -> {
                ClipboardContent cc = new ClipboardContent();
                cc.put(DataFormat.FILES, getValue());
                Clipboard.getSystemClipboard().setContent(cc);
            }),
            menuItem("Explore in browser", e -> Environment.browse(getValue().stream()))
        );
    }};

    public static class SimpleTreeItem<T> extends TreeItem<T> {
        public final V<Boolean> showLeaves = new V<>(true);

        public SimpleTreeItem(T v) {
            this(v, stream());
        }

        public SimpleTreeItem(T v, Stream<T> children) {
            super(v);
            super.getChildren().addAll((Collection)trees(children));
            showLeaves.addListener((o,ov,nv) -> {
                if (nv) {
                    throw new UnsupportedOperationException("Can not repopulate leaves yet");
                } else {
                    super.getChildren().removeIf(TreeItem::isLeaf);
                }
                stream(super.getChildren()).select(SimpleTreeItem.class)
                        .forEach(i -> i.showLeaves.set(nv));
            });
        }

        @Override
        public boolean isLeaf() {
            return getChildren().isEmpty();
        }
    }
    public static class STreeItem<T> extends SimpleTreeItem<T> {
        Supplier<Stream<T>> s;
        private boolean isFirstTimeChildren = true;
        private boolean isFirstTimeLeaf = true;
        private boolean isLeaf; // cache

        public STreeItem(T v, Supplier<Stream<T>> cs) {
            super(v);
            s = cs;
        }

        @Override
        public ObservableList<TreeItem<T>> getChildren() {
            if (isFirstTimeChildren) {
                isFirstTimeChildren = false;
                super.getChildren().setAll((List)trees(s.get()));
            }
            return super.getChildren();
        }

        @Override
        public boolean isLeaf() {
//            if (isFirstTimeLeaf) {
//                isFirstTimeLeaf = false;
//                isLeaf = getValue().isFile();
//            }
//
//            return isLeaf;
            return false;
        }
    }
    public static class WidgetItem extends STreeItem<Widget> {

        public WidgetItem(Widget v) {
            super(v, () -> stream());
        }

    }
    public static class LayoutItem extends STreeItem<Component> {

        public LayoutItem(Component v) {
            super(v, v instanceof Container ? () -> ((Container)v).getChildren().values().stream() : () -> stream());
        }

    }
    public static class FileTreeItem extends SimpleTreeItem<File> {

        private final boolean isLeaf;
        private boolean isFirstTimeChildren = true;


        public FileTreeItem(File value, boolean isFile) {
            super(value);
            isLeaf = isFile; // cache, but now we must forbid value change
            valueProperty().addListener((o,ov,nv) -> {
                throw new RuntimeException("FileTreeItem value must never change");
            });
        }

        private FileTreeItem(File value) {
            this(value, value.isFile());
        }

        @Override
        public ObservableList<TreeItem<File>> getChildren() {
            ObservableList<TreeItem<File>> c = super.getChildren();
            if (isFirstTimeChildren) {
                c.setAll(buildChildren(this));
                isFirstTimeChildren = false;
            }
            return c;
        }

        @Override
        public boolean isLeaf() {
            return isLeaf;
        }

        private List<? extends TreeItem<File>> buildChildren(TreeItem<File> i) {
            List<FileTreeItem> dirs = new ArrayList<>();
            List<FileTreeItem> fils = new ArrayList<>();
            listFiles(i.getValue()).forEach(f -> {
                boolean isFile = f.isFile();
                (isFile ? fils : dirs).add(new FileTreeItem(f,isFile));
            });
            if(showLeaves.get()) dirs.addAll(fils);
            dirs.forEach(item -> item.showLeaves.set(showLeaves.get()));
            return dirs;
        }
    }
    public static class NodeTreeItem extends SimpleTreeItem<Node> {

        public NodeTreeItem(Node value) {
            super(value);
        }

        private boolean isLeaf;
        private boolean isFirstTimeChildren = true;
        private boolean isFirstTimeLeaf = true;

        @Override public ObservableList<TreeItem<Node>> getChildren() {
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
                isLeaf = getChildren().isEmpty();
            }
            return isLeaf;
        }

        private ObservableList<TreeItem<Node>> buildChildren(TreeItem<Node> i) {
            ObservableList<TreeItem<Node>> out = observableArrayList();
            Node value = i.getValue();
            if(value instanceof Region)
                ((Region)value).getChildrenUnmodifiable().forEach(n -> out.add(new NodeTreeItem(n)));

            return out;
        }
    }

    private static Stream strm(Object o, Stream t) {
        return Stream.concat(Stream.of(o), t);
    }
    private static String toS(Node n) {
        return emptifyString(n.getId()) + ":" + APP.className.get(n.getClass());
    }
    private static String windowToName(Window w) {
        String n = "window " + list(APP.windowManager.windows).indexOf(w);
        if(w==APP.window) n += " (main)";
        if(w==APP.windowManager.miniWindow) n += " (mini-docked)";
        return n;
    }
}