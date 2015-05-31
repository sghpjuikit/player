/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.Tree;

import Configuration.Config;
import Configuration.Configurable;
import Configuration.ListConfigurable;
import Layout.Widgets.Features.ConfiguringFeature;
import Layout.Widgets.WidgetManager;
import static Layout.Widgets.WidgetManager.WidgetSource.ANY;
import java.lang.reflect.Method;
import java.util.*;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import static javafx.css.PseudoClass.getPseudoClass;
import javafx.scene.Node;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Region;
import static util.Util.emptifyString;
import static util.functional.Util.stream;

/**
 *
 * @author Plutonium_
 */
public class PropertyTree extends TreeView<Node> {
    
    public PropertyTree() {
        // set value factory
        setCellFactory( tv -> new TreeCell<Node>(){
            @Override
            protected void updateItem(Node item, boolean empty) {
                super.updateItem(item, empty);
                if(empty || item==null) {
                    setText(null);
                } else {
                    String s = item.getClass().getSimpleName().isEmpty() ? item.getClass().toString() : item.getClass().getSimpleName();
                    setText(emptifyString(item.getId())+":"+s);
                }
            }
        });
        getSelectionModel().selectedItemProperty().addListener((o,ov,nv) -> {
            if(ov!=null && ov.getValue()!=null) {
                ov.getValue().pseudoClassStateChanged(csPC, false);
                ov.getValue().setStyle("");
            }
            if(nv!=null && nv.getValue()!=null) {
                nv.getValue().pseudoClassStateChanged(csPC, true);
                nv.getValue().setStyle("-fx-background-color: rgba(90,200,200,0.2);");
                WidgetManager.use(ConfiguringFeature.class, ANY, w -> w.configure(toC(nv.getValue())));
            }
        });
    }
    
    public void expand(Node f) {
        ObjectProperty<TreeItem<Node>> it = new SimpleObjectProperty(getRoot());
        stream(f.getParent(),p->p.getParent()!=null,p -> p.getParent())
            .skip(1)
            .forEach(pth -> {
                if(it.get()!=null) {
                    it.get().setExpanded(true);
                    it.set(it.get().getChildren().stream().filter(i -> i.getValue().equals(pth)).findFirst().orElse(null));
                }
            });
    }
    
    
    private static final PseudoClass csPC = getPseudoClass("configselected");
    
    
    public static TreeItem<Node> createTreeItem(Node f) {
        return new TreeItem<Node>(f) {
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
                            out.add(createTreeItem(n));
                        });
                    }
                }

                return out;
            }
        };
    }
    
    
    private static Map<ObservableValue,String> setTarget(Object target) {
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
        setTarget(o).forEach((p,name) -> {
            if(!(p instanceof WritableValue || p instanceof ReadOnlyProperty)) System.out.println(p.getClass());
            if((p instanceof WritableValue || p instanceof ReadOnlyProperty) && p.getValue()!=null)
                cs.add(Config.fromProperty(name,p));
        });
        ListConfigurable c = new ListConfigurable(cs);
        return c;
        
    }
}
