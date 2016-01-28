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

import javafx.collections.FXCollections;
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

import AudioPlayer.services.Service;
import Configuration.Configurable;
import Layout.Component;
import Layout.container.Container;
import Layout.widget.Widget;
import Layout.widget.WidgetFactory;
import Layout.widget.WidgetManager.WidgetSource;
import Layout.widget.feature.ConfiguringFeature;
import Layout.widget.feature.Feature;
import gui.objects.ContextMenu.ImprovedContextMenu;
import gui.objects.Window.stage.Window;
import util.ClassName;
import util.File.Environment;
import util.File.FileUtil;

import static Configuration.Configurable.configsFromFxPropertiesOf;
import static Layout.widget.WidgetManager.WidgetSource.*;
import static gui.objects.tree.FileTree.createTreeItem;
import static java.util.stream.Collectors.toList;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static main.App.APP;
import static util.File.FileUtil.listFiles;
import static util.Util.*;
import static util.functional.Util.*;

/**
 *
 * @author Plutonium_
 */
public class TreeItems {

    public static TreeItem<? extends Object> tree(Object o) {
        if(o instanceof TreeItem)       return (TreeItem)o;
        if(o instanceof Widget)         return new WidgetItem((Widget)o);
        if(o instanceof WidgetFactory)  return new TreeItem<>(o);
        if(o instanceof Widget.Group)   return new STreeItem(o,()->APP.widgetManager.findAll(OPEN).filter(w->w.getInfo().group()==o).sorted(by(w -> w.getName())));
        if(o instanceof WidgetSource)   return new STreeItem(o,()->APP.widgetManager.findAll((WidgetSource)o).sorted(by(w -> w.getName())));
        if(o instanceof Feature)        return new STreeItem(((Feature)o).name(), () -> APP.widgetManager.getFactories().filter(f -> f.hasFeature(((Feature)o))).sorted(by(f -> f.nameGui())));
        if(o instanceof Container)      return new LayoutItem((Component)o);
        if(o instanceof File)           return new FileTreeItem((File)o);
        if(o instanceof Node)           return new NodeTreeItem((Node)o);
        if(o instanceof Window)         return new STreeItem(o,() -> stream(((Window)o).getStage().getScene().getRoot(),((Window)o).getLayout()));
        return new TreeItem<>(o);
    }

    public static TreeItem<Object> tree(Object v, Object... children) {
        TreeItem t = new TreeItem();
        t.setValue(v);
        t.getChildren().addAll(trees(children));
        return t;
    }

    public static TreeItem<Object> tree(Object v, List<? extends Object> cs) {
        return new SimpleTreeItem(v,cs.stream());
    }

    public static TreeItem<Object> tree(Object v, Supplier<Stream<? extends Object>> cs) {
        return new STreeItem(v, cs);
    }

    public static TreeItem<Object> treeApp() {
        TreeItem widgetT = tree("Widgets",
                     tree("Categories", (List)list(Widget.Group.values())),
                     tree("Types", () -> APP.widgetManager.getFactories().sorted(by(f -> f.nameGui()))),
                     tree("Open", (List)list(ANY,LAYOUT,STANDALONE)),
                     tree("Features", () -> APP.widgetManager.getFeatures().sorted(by(f -> f.name())))
                   );
        return tree("App",
                 tree("Behavior",
                   widgetT,
                   tree("Services", () -> APP.services.getAllServices().sorted(by(s -> ClassName.of(s.getClass()))))
                 ),
                 tree("UI",
                   widgetT,
                   tree("Windows", () -> APP.windowManager.windows.stream()),
                   tree("Layouts", () -> APP.widgetManager.getLayouts().sorted(by(l -> l.getName())))
                 ),
                 tree("Location", listFiles(APP.DIR_APP)),
                 tree("File system", map(File.listRoots(),FileTreeItem::new))
               );
    }

    public static List<TreeItem<? extends Object>> trees(Object... children) {
        return (stream(children).map(TreeItems::tree).collect(toList()));
    }

    public static List<TreeItem<? extends Object>> trees(Collection<Object> children) {
        return children.stream().map(TreeItems::tree).collect(toList());
    }

    public static List<TreeItem<? extends Object>> trees(Stream<? extends Object> children) {
        return children.map(TreeItems::tree).collect(toList());
    }

/**************************************************************************************************/

    public static TreeCell<Object> buildTreeCell(TreeView<Object> t) {
        return new TreeCell<Object>(){
            {
                setOnMouseClicked(e -> {
                    Object o = getItem();
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
            protected void updateItem(Object o, boolean empty) {
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

    public static void doOnSingleClick(Object o) {
    }

    public static void showMenu(Object o, TreeView<Object> t, Node n, MouseEvent e) {
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

    private static ImprovedContextMenu<List<File>> m = new ImprovedContextMenu();

    static{
        m.getItems().addAll(
            menuItem("Open", e -> {
                Environment.open(m.getValue().get(0));
            }),
            menuItem("Open in-app", e -> {
                Environment.openIn(m.getValue(),true);
            }),
            menuItem("Edit", e -> {
                Environment.edit(m.getValue().get(0));
            }),
            menuItem("Copy", e -> {
                ClipboardContent cc = new ClipboardContent();
                cc.put(DataFormat.FILES, m.getValue());
                Clipboard.getSystemClipboard().setContent(cc);
            }),
            menuItem("Explore in browser", e -> {
                Environment.browse(m.getValue().stream());
            })
        );
    }



    public static class SimpleTreeItem extends TreeItem<Object> {

        public SimpleTreeItem(Object v, Stream<? extends Object> children) {
            super(v);
            getChildren().addAll((List)trees(children));
        }

        @Override
        public boolean isLeaf() {
            return getChildren().isEmpty();
        }
    }
    public static class STreeItem extends TreeItem<Object> {
        Supplier<Stream<? extends Object>> s;
        private boolean isFirstTimeChildren = true;
        private boolean isFirstTimeLeaf = true;
        private boolean isLeaf; // cache

        public STreeItem(Object v, Supplier<Stream<? extends Object>> cs) {
            super(v);
            s = cs;
        }

        @Override
        public ObservableList<TreeItem<Object>> getChildren() {
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
    public static class WidgetItem extends STreeItem {

        public WidgetItem(Widget v) {
            super(v, () -> Stream.empty());
        }

    }
    public static class LayoutItem extends STreeItem {

        public LayoutItem(Component v) {
            super(v, v instanceof Container ? () -> ((Container)v).getChildren().values().stream() : () -> Stream.empty());
        }

    }
    public static class FileTreeItem extends TreeItem<File> {

        private boolean isLeaf;
        private boolean isFirstTimeChildren = true;
        private boolean isFirstTimeLeaf = true;

        public FileTreeItem(File value) {
            super(value);
        }

        @Override public ObservableList<TreeItem<File>> getChildren() {
            ObservableList<TreeItem<File>> c = super.getChildren();
            if (isFirstTimeChildren) {
                c.setAll(buildChildren(this));
                isFirstTimeChildren = false;
            }
            return c;
        }

        @Override public boolean isLeaf() {
            if (isFirstTimeLeaf) {
                isFirstTimeLeaf = false;
                isLeaf = getValue().isFile();
            }

            return isLeaf;
        }

        private List<TreeItem<File>> buildChildren(TreeItem<File> i) {
            // we want to sort the items : directories first
            // we make use of the fact that listFiles() gives us already
            // sorted list
            List<TreeItem<File>> dirs = new ArrayList<>();
            List<TreeItem<File>> fils = new ArrayList<>();
            listFiles(i.getValue()).forEach(f -> {
                if(!f.isDirectory()) dirs.add(createTreeItem(f));
                else                 fils.add(createTreeItem(f));
            });
                   dirs.addAll(fils);
            return dirs;
        }
    }
    public static class NodeTreeItem extends TreeItem<Node> {

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
                ObservableList<TreeItem<Node>> out = FXCollections.observableArrayList();
            Node value = i.getValue();
            if (value != null) {
                if(value instanceof Region) {
                    new ArrayList<>(((Region)value).getChildrenUnmodifiable()).forEach(n -> {
                        out.add(new NodeTreeItem(n));
                    });
                }
            }

            return out;
        }
    }

    public static Stream strm(Object o, Stream t) {
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