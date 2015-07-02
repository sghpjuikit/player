/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.tree;

import Configuration.Config;
import Configuration.Configurable;
import Configuration.ListConfigurable;
import Layout.Component;
import Layout.Container;
import Layout.LayoutManager;
import Layout.Widgets.feature.ConfiguringFeature;
import Layout.Widgets.Widget;
import Layout.Widgets.WidgetFactory;
import Layout.Widgets.WidgetManager;
import Layout.Widgets.WidgetManager.WidgetSource;
import static Layout.Widgets.WidgetManager.WidgetSource.*;
import gui.objects.ContextMenu.ImprovedContextMenu;
import gui.objects.Window.stage.Window;
import static gui.objects.tree.FileTree.createTreeItem;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import static java.util.stream.Collectors.toList;
import java.util.stream.Stream;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import main.App;
import util.ClassName;
import util.File.Environment;
import util.File.FileUtil;
import static util.Util.*;
import static util.functional.Util.*;

/**
 *
 * @author Plutonium_
 */
public class TreeItems {
    
    
    public static TreeItem<Object> tree(Object v, TreeItem<Object>... children) {
        TreeItem<Object> t = new TreeItem();
        t.setValue(v);
        t.getChildren().addAll(children);
        return t;
    }
    public static TreeItem<Object> tree(Object v, List<Object> cs) {
        return new SimpleTreeItem(v,cs.stream());
    }
    public static TreeItem<Object> tree(Object v, Supplier<Stream<? extends Object>> cs) {
        return new STreeItem(v, cs);
    }
    
    public static TreeItem<Object> treeApp() {
        return tree("App",
                 tree("Gui",
                   tree("Widgets",
                     tree("Categories", (List)list(Widget.Group.values())),
                     tree("Types", () -> WidgetManager.getFactories()),
                     tree("Open", (List)list(ANY,LAYOUT,STANDALONE))
                   ),
                   tree("Windows", () -> Window.windows.stream()),
                   tree("Layouts", () -> LayoutManager.getLayouts())
                 ),
                 tree("Location", () -> stream(App.getLocation().listFiles()).sorted((a,b) -> a.isFile() && b.isDirectory() ? 1 : a.isDirectory() && b.isFile() ? -1 : a.compareTo(b))),
                 tree("File system", () -> stream(File.listRoots()))
               );
    }
    
    public static TreeItem<Object> tree(Object o) {
        if(o instanceof Widget)         return new WidgetItem((Widget)o);
        if(o instanceof WidgetFactory)  return new STreeItem(o,()->Stream.empty());
        if(o instanceof Widget.Group)   return new STreeItem(o,()->WidgetManager.findAll(OPEN).filter(w->w.getInfo().group()==o));
        if(o instanceof WidgetSource)   return new STreeItem(o,()->WidgetManager.findAll((WidgetSource)o));
        if(o instanceof Container)      return new LayoutItem((Component)o);
        if(o instanceof File)           return (TreeItem) new FileTreeItem((File)o);
        if(o instanceof Node)           return (TreeItem) new NodeTreeItem((Node)o);
        if(o instanceof Window)         return new STreeItem(o,()->strm(((Window)o).getStage().getScene().getRoot(),((Window)o).getLayoutAggregator().getLayouts().values().stream()));
        return new TreeItem<>(o);
    }
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
                    else if(o instanceof WidgetFactory)  setText(((WidgetFactory)o).getName());
                    else if(isEnum(o.getClass()))   setText(util.Util.enumToHuman(o.toString()));
                    else if(o instanceof File)      setText(FileUtil.getNameFull((File)o));
                    else if(o instanceof Node)      setText(toS((Node)o));
                    else if(o instanceof Window)    setText("window"+Window.windows.indexOf(o));
                    else setText(o.toString());
                } else {
                    setGraphic(null);
                    setText(null);
                }
            }
        };
    }
    
    public static void doOnDoubleClick(Object o) {
        if(o instanceof Configurable) WidgetManager.use(ConfiguringFeature.class, ANY, w -> w.configure((Configurable)o));
        if(o instanceof Node) WidgetManager.use(ConfiguringFeature.class, ANY, w -> w.configure(toC(o)));
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
                Environment.browse(m.getValue(),true);
            })
        );
    }

    
    
    public static class SimpleTreeItem extends TreeItem<Object> {

        public SimpleTreeItem(Object v, Stream<Object> ch) {
            super(v);
            getChildren().addAll(ch.map(TreeItems::tree).collect(toList()));
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
                super.getChildren().setAll(s.get().map(TreeItems::tree).collect(toList()));
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
            File value = i.getValue();
            if (value != null && value.isDirectory()) {
                File[] all = value.listFiles();
                if (all != null) {
                    // we want to sort the items : directories first
                    List<TreeItem<File>> fils = new ArrayList();
                    List<TreeItem<File>> dirs = new ArrayList();
                    for (File f : all) {
                        if(!f.isDirectory()) dirs.add(createTreeItem(f));
                        else                 fils.add(createTreeItem(f));
                    }
                           fils.addAll(dirs);
                    return fils;
                }
            }

            return listRO();
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
        return emptifyString(n.getId())+":"+ClassName.get(n.getClass());
    }
    
    private static Map<ObservableValue,String> propertes(Object target) {
        Map<ObservableValue,String> out = new HashMap();
        for (final Method method : target.getClass().getMethods()) {
            if (method.getName().endsWith("Property")) {
                try {
                    final Class returnType = method.getReturnType();
                    if (ObservableValue.class.isAssignableFrom(returnType)) {
                        final String propertyName = method.getName().substring(0, method.getName().lastIndexOf("Property"));
                        method.setAccessible(true);
                        final ObservableValue property = (ObservableValue) method.invoke(target);
                        out.put(property, propertyName);
                    }
                } catch(Exception e) {}
            }
        }
        return out;
    }
    public static Configurable toC(Object o) {
        List<Config> cs = new ArrayList();
        propertes(o).forEach((p,name) -> {
            if(!(p instanceof WritableValue || p instanceof ReadOnlyProperty)) System.out.println(p.getClass());
            if((p instanceof WritableValue || p instanceof ReadOnlyProperty) && p.getValue()!=null)
                cs.add(Config.fromProperty(name,p));
        });
        ListConfigurable c = new ListConfigurable(cs);
        return c;
    }
}