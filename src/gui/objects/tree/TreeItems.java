/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.tree;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.MouseEvent;

import gui.objects.contextmenu.ImprovedContextMenu;
import gui.objects.window.stage.Window;
import layout.Component;
import layout.container.Container;
import layout.container.layout.Layout;
import layout.widget.Widget;
import layout.widget.WidgetFactory;
import layout.widget.WidgetManager.WidgetSource;
import layout.widget.feature.ConfiguringFeature;
import layout.widget.feature.Feature;
import services.Service;
import util.HierarchicalBase;
import util.access.V;
import util.conf.Config;
import util.conf.Configurable;
import util.file.Environment;
import util.file.Util;
import util.type.ClassName;

import static java.util.stream.Collectors.toList;
import static javafx.collections.FXCollections.observableArrayList;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static layout.widget.WidgetManager.WidgetSource.*;
import static main.App.APP;
import static util.Util.emptyOr;
import static util.conf.Configurable.configsFromFxPropertiesOf;
import static util.dev.Util.log;
import static util.file.Util.listFiles;
import static util.functional.Util.*;
import static util.graphics.Util.menuItem;

/**
 *
 * @author Martin Polakovic
 */
public class TreeItems {

    public static <T> SimpleTreeItem<T> tree(T o) {
        if (o instanceof TreeItem)          return (SimpleTreeItem) o;  // purposefully, this will crash if TreeItem is not SimpleTreeItem, we don't want to wrap TreeItems
        if (o instanceof Widget)            return (SimpleTreeItem) new WidgetItem((Widget)o);
        if (o instanceof WidgetFactory)     return (SimpleTreeItem) new SimpleTreeItem<>(o);
        if (o instanceof Widget.Group)      return (SimpleTreeItem) new STreeItem(o,()->APP.widgetManager.findAll(OPEN).filter(w->w.getInfo().group()==o).sorted(by(w -> w.getName())));
        if (o instanceof WidgetSource)      return (SimpleTreeItem) new STreeItem(o,()->APP.widgetManager.findAll((WidgetSource)o).sorted(by(w -> w.getName())));
        if (o instanceof Feature)           return (SimpleTreeItem) new STreeItem(((Feature)o).name(), () -> APP.widgetManager.getFactories().filter(f -> f.hasFeature(((Feature)o))).sorted(by(f -> f.nameGui())));
        if (o instanceof Container)         return (SimpleTreeItem) new LayoutItem((Component)o);
        if (o instanceof File)              return (SimpleTreeItem) new FileTreeItem((File)o);
        if (o instanceof Node)              return (SimpleTreeItem) new NodeTreeItem((Node)o);
        if (o instanceof Window)            return (SimpleTreeItem) new STreeItem(o,() -> stream(((Window)o).getStage().getScene().getRoot(),((Window)o).getLayout()));
        if (o instanceof Name)              return (SimpleTreeItem) new STreeItem(o, () -> ((HierarchicalBase)o).getHChildren().stream(), () -> ((Name)o).getHChildren().isEmpty());
        if (o instanceof HierarchicalBase)  return (SimpleTreeItem) new STreeItem(o, () -> ((HierarchicalBase)o).getHChildren().stream(), () -> true);
        return new SimpleTreeItem<>(o);
    }

    public static SimpleTreeItem<Object> tree(Object v, Object... children) {
        SimpleTreeItem<Object> t = new SimpleTreeItem<>(null);
        t.setValue(v);
        t.getChildren().addAll((List)trees(children));
        return t;
    }

    public static SimpleTreeItem<Object> tree(Object v, List<?> cs) {
        return new SimpleTreeItem(v,cs.stream());
    }

    public static SimpleTreeItem<Object> tree(Object v, Stream<?> cs) {
        return new STreeItem(v, () -> cs);
    }

    public static SimpleTreeItem<Object> tree(Object v, Supplier<Stream<?>> cs) {
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
                 tree("File system", map(File.listRoots(),FileTreeItem::new)),
	             tree(Name.treeOfPaths("Settings", stream(APP.configuration.getFields()).map(Config::getGroup).toList()))
               );
    }

    public static List<SimpleTreeItem<?>> trees(Object... children) {
        return (stream(children).map(TreeItems::tree).collect(toList()));
    }

    public static List<SimpleTreeItem<?>> trees(Collection<Object> children) {
        return children.stream().map(TreeItems::tree).collect(toList());
    }

    public static List<SimpleTreeItem<?>> trees(Stream<? extends Object> children) {
        return children.map(TreeItems::tree).collect(toList());
    }

/**************************************************************************************************/

    public static <T> TreeCell<T> buildTreeCell(TreeView<T> t) {
        return new TreeCell<>() {
            {
                setOnMouseClicked(e -> {
                    T o = getItem();
                    // context menu
                    if (o!=null && e.getButton()==SECONDARY && e.getClickCount()==1) {
                        if (!isSelected()) getTreeView().getSelectionModel().clearAndSelect(getIndex());
                        showMenu(o, getTreeView(), this, e);
                    }
                    // custom action
                    if (o!=null && e.getButton()==PRIMARY) {
                        if (e.getClickCount()==1) doOnSingleClick(o);
                        else if (e.getClickCount()==2) doOnDoubleClick(o);
                    }
                });
            }
            @Override
            protected void updateItem(T o, boolean empty) {
                super.updateItem(o, empty);
                if (!empty && o!=null) {
                    if (o instanceof Component)      setText(((Component)o).getName());
                    else if (o instanceof Service)   setText(((Service)o).getClass().getSimpleName());
                    else if (o instanceof WidgetFactory)  setText(((WidgetFactory)o).nameGui());
                    else if (util.type.Util.isEnum(o.getClass()))   setText(util.Util.enumToHuman(o.toString()));
                    else if (o instanceof File)      setText(Util.getNameFull((File)o));
                    else if (o instanceof Node)      setText(toS((Node)o));
                    else if (o instanceof Window)    setText(windowToName((Window)o));
                    else if (o instanceof Name)    setText(((Name)o).val);
                    else if (o instanceof HierarchicalBase)    setText(Objects.toString(((HierarchicalBase)o).val));
                    else setText(o.toString());
                } else {
                    setGraphic(null);
                    setText(null);
                }
            }
        };
    }

    public static void doOnDoubleClick(Object o) {
        if (o instanceof Node) APP.widgetManager.use(ConfiguringFeature.class, ANY, w -> w.configure(configsFromFxPropertiesOf(o)));
        else if (o instanceof Window) APP.widgetManager.use(ConfiguringFeature.class, ANY, w -> w.configure(configsFromFxPropertiesOf(((Window)o).getStage())));
        else if (o instanceof File) {
            File f = (File)o;
            if (f.isFile() || Environment.isOpenableInApp(f)) Environment.openIn(f, true);
        }
        else if (o instanceof Configurable) APP.widgetManager.use(ConfiguringFeature.class, ANY, w -> w.configure((Configurable)o));
        else if (o instanceof Name) APP.widgetManager.use(ConfiguringFeature.class, ANY, w -> w.configure(stream(APP.configuration.getFields()).filter(f -> f.getGroup().equals(((Name)o).pathUp)).toList()));
        else if (o instanceof HierarchicalBase) doOnDoubleClick(((HierarchicalBase) o).val);
    }

    public static void doOnSingleClick(Object o) {
	    if (o instanceof HierarchicalBase) doOnSingleClick(((HierarchicalBase) o).val);
    }

    // TODO: implement properly
    public static <T> void showMenu(T o, TreeView<T> t, Node n, MouseEvent e) {
        if (o instanceof File) {
            List<File> files = filterMap(t.getSelectionModel().getSelectedItems(), c -> c.getValue() instanceof File, c -> (File)c.getValue());
            if (files.isEmpty()) {
                m.getItems().forEach(i -> i.setDisable(true));
            } else if (files.size()==1) {
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
        else if (o instanceof Window) {
		    windowMenu.setValue((Window) o);
		    windowMenu.show(n, e);
	    }

//	    if (o instanceof HierarchicalBase) showMenu(((HierarchicalBase)o).val, t, n, e); // requires mapping Hierarchical -> T
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

	private static ImprovedContextMenu<Window> windowMenu = new ImprovedContextMenu<>(){{
		util.type.Util.getAllMethods(Window.class).stream()
			.filter(m -> Modifier.isPublic(m.getModifiers()))
			.filter(m -> m.getParameterCount() == 0)
			.filter(m -> m.getReturnType() == void.class)
			.map(m -> menuItem(m.getName(), e -> {
					try {
						m.invoke(getValue());
					} catch (IllegalAccessException | InvocationTargetException x) {
						log(TreeItems.class).error("Could not invoke method {} on object {}", m, getValue(), x);
					}
				}
			))
			.forEach(getItems()::add);
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
        private final Supplier<Stream<T>> childrenLazy;
        private final Supplier<Boolean> isLeafLazy;
        private boolean isFirstTimeChildren = true;
        private Boolean isLeaf;

        public STreeItem(T v, Supplier<Stream<T>> childrenLazy) {
            this(v, childrenLazy, null);
        }

        public STreeItem(T v, Supplier<Stream<T>> childrenLazy, Supplier<Boolean> isLeafLazy) {
            super(v);
            this.isLeafLazy = isLeafLazy;
            this.childrenLazy = childrenLazy;
        }

        @Override
        public ObservableList<TreeItem<T>> getChildren() {
            if (isFirstTimeChildren) {
                isFirstTimeChildren = false;
                super.getChildren().setAll((List)trees(childrenLazy.get()));
            }
            return super.getChildren();
        }

        @Override
        public boolean isLeaf() {
	        return isLeafLazy==null ? false : isLeafLazy.get();
        }
    }
    public static class WidgetItem extends STreeItem<Object> {

        public WidgetItem(Widget v) {
            super(v, () -> stream(v.areaTemp.getRoot()), () -> true);
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
            if (showLeaves.get()) dirs.addAll(fils);
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
	        if (value instanceof Parent)
		        ((Parent)value).getChildrenUnmodifiable().forEach(n -> out.add(new NodeTreeItem(n)));

            return out;
        }
    }

    private static String toS(Node n) {
        return emptyOr(n.getId()) + ":" + APP.className.get(n.getClass());
    }
    private static String windowToName(Window w) {
        String n = "window " + list(APP.windowManager.windows).indexOf(w);
        if (APP.windowManager.getMain().filter(mw -> mw==w).isPresent()) n += " (main)";
        if (w==APP.windowManager.miniWindow) n += " (mini-docked)";
        return n;
    }

	public static class Name extends HierarchicalBase<String,Name> {
		private static final char DELIMITER = '.';

		public static Name treeOfPaths(String rootName, Collection<String> paths) {
			Name root = new Name(rootName, "", null);
			paths.stream().distinct().forEach(root::addPath);
			return root;
		}

		public final String pathUp;
		private List<Name> children;

		private Name(String value, Name parent) {
			this(value, parent==null || parent.pathUp.isEmpty() ? value : parent.pathUp + DELIMITER + value, parent);
		}

		private Name(String value, String pathUp, Name parent) {
			super(value, parent);
			this.pathUp = pathUp;
		}

		public void addPath(String path) {
			int i=path.indexOf(DELIMITER);
			boolean isLeaf = i<0;

			if (isLeaf) {
				stream(getHChildren()).findFirst(name -> path.equals(name.val)).ifPresentOrElse(
					name -> {},
					() -> getHChildren().add(new Name(path, this))
				);
			} else {
				String prefix = path.substring(0,i);
				String suffix = path.substring(i+1);

				stream(getHChildren()).findFirst(name -> prefix.equals(name.val))
					.ifPresentOrElse(
						name -> name.addPath(suffix),
						() -> {
							Name name = new Name(prefix, this);
							getHChildren().add(name);
							name.addPath(suffix);
						}
					);
			}
		}

		@Override
		public List<Name> getHChildren() {
			if (children==null) children = new ArrayList<>();
			return children;
		}
	}
}