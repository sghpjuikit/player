
package Layout.Areas;

import Configuration.Configurable;
import GUI.ContextManager;
import GUI.DragUtil;
import GUI.WidgetTransfer;
import GUI.Window;
import Layout.Component;
import Layout.Container;
import Layout.PolyContainer;
import Layout.Widgets.Widget;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import static java.util.Collections.EMPTY_LIST;
import java.util.List;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import static javafx.scene.input.MouseButton.PRIMARY;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import static utilities.Util.NotNULL;

/**
 * Implementation of PolyArea.
 */
public final class TabArea extends PolyArea {
    
    @FXML TabPane tabPane;
    @FXML AnchorPane content;
    
    private Component widget;  // active component, max one, null if none
    
    public TabArea(PolyContainer _container) {
        super(_container);
        
        // init properties
        container.properties.initProperty(Integer.class, "selected", -1);
        
        root.setMinSize(0,0);
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("TabbedArea.fxml"));
        fxmlLoader.setRoot(root);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException ex) {
            throw new RuntimeException(ex.getMessage());
        }
        
        tabPane.setUserData(this);
        content.getStyleClass().setAll(Area.bgr_STYLECLASS);
        // load controls
        controls = new AreaControls(this);
        root.getChildren().add(controls.root);
        AnchorPane.setBottomAnchor(controls.root, 0.0);
        AnchorPane.setTopAnchor(controls.root, 0.0);
        AnchorPane.setLeftAnchor(controls.root, 0.0);
        AnchorPane.setRightAnchor(controls.root, 0.0);
        
        // support drag from
        root.setOnDragDetected( e -> {
            // disallow in normal mode & primary button drag only
            if (controls.isShowing() && e.getButton()==PRIMARY) {
                ClipboardContent c = new ClipboardContent();
                // use first free index (0) id empty (widget==null)
                int i = widget==null ? 0 : container.indexOf(widget);
                c.put(DragUtil.widgetDF, new WidgetTransfer(i, container));
                Dragboard db = root.startDragAndDrop(TransferMode.ANY);
                          db.setContent(c);
                e.consume();
            }
        });
        // support drag onto
        root.setOnDragOver(DragUtil.componentDragAcceptHandler);
        // handle drag onto
        root.setOnDragDropped( e -> {
            Dragboard db = e.getDragboard();
            if (db.hasContent(DragUtil.widgetDF)) {
                WidgetTransfer wt = DragUtil.getWidgetTransfer(db);
                // use first free index (0) if empty (widget==null)
                int i1 = widget==null ? 0 : container.indexOf(widget);
                int i2 = wt.childIndex();
                container.swapChildren(wt.getContainer(), i1, i2);
                e.consume();
            }
        });
        

        hide();
    }
    
    /** @return active - currently displayed component */
    @Override
    public Component getActiveComponent() {
        Tab t = tabPane.getSelectionModel().getSelectedItem();
        return t == null ? null : (Component)t.getUserData();
    }
    
    /**
     * @return singleton list containing active - currently displayed component
     */
    @Override
    public List<Component> getActiveComponents() {
        Component c = getActiveComponent();
        return c==null ? EMPTY_LIST : Collections.singletonList(c);
    }
    
    
    @Override
    public void add(Component c) {
        addComponents(Collections.singleton(c));
    }
    
    public void addComponents(Collection<Component> cs) {
        if(cs.isEmpty()) return;
        
        int i = container.properties.getI("selected");
        // process components -> turn into tab, set behavior, load lazily
            // somehow null get through here, investigate, fix, document
        cs.stream().filter(NotNULL).forEach( c -> {
            DraggableTab t = new DraggableTab(c.getName());
            t.setDetachable(true);
            t.setTooltip(new Tooltip(c.getName()));
            t.setUserData(c);
            t.setOnClosed(e -> container.removeChild(c));
            t.setOnSelectionChanged( e -> {
                // prevent loading because of lock
                if(selectionLock) return;
                if(tabPane.getTabs().contains(t) && t.isSelected())
                    loadTab(t,(Component)t.getUserData());
            });
            tabPane.getTabs().add(t);
            container.getChildren().put(container.getChildren().size(), c);
        });
        
        // select correct tab
        if(!selectionLock) {
            if(i<0) i = 0;
            if(i==0) {
                Tab t = tabPane.getTabs().get(i);
                loadTab(t,(Component)t.getUserData());
            }
            selectComponent(i);
        }
    }
    
    @Override
    public void removeComponent(Component c) {
        tabPane.getTabs().stream()
                .filter(t->t.getUserData().equals(c))
                .findAny()
                .ifPresent(tabPane.getTabs()::remove);
    }
    
/******************************** selection ***********************************/
    
    private static boolean selectionLock = false;
    
    public void selectComponentPreventLoad(boolean val) {
        selectionLock = val;
    }
    
    public void selectComponent(Integer i) {
        // release lock on manual change
        selectionLock = false;
        
        int tabs = tabPane.getTabs().size();
        if(i==null) {
            if(tabPane.getSelectionModel().isEmpty())
                tabPane.getSelectionModel().select(0);
        } else {
//            if(tabs>i) i = tabs;    // prevent out of bounds
//            if(i<1) i = 1;          // prevent no selection
//            if(tabs>0 && tabPane.getSelectionModel().getSelectedIndex() != i)
                tabPane.getSelectionModel().select(i);
        }
        
        // remember new state
        int ii = tabPane.getSelectionModel().getSelectedIndex();
        container.properties.set("selected", ii);
    }
    
    /** Purges all tabs. */
    @Override
    public void removeAllComponents() {
        tabPane.getTabs().clear();
    }
    
    private void loadTab(Tab t, Component c) {
        if (c == null || t == null) return;
        widget = c;
        Node w = c.load();
        t.setContent(w);
        t.getContent().setLayoutX(0);
        t.getContent().setLayoutY(0);
        t.getContent().minHeight(0);
        t.getContent().minWidth(0);
        controls.title.setText(c.getName());
        controls.propB.setDisable(false);
        if(c instanceof Configurable)
            controls.propB.setDisable(((Configurable)c).getFields().isEmpty());
        
        int ii = tabPane.getTabs().indexOf(t);
        if(!selectionLock) container.properties.set("selected", ii);
    }
    
    /** Refreshes the active component */
    @Override
    public void refresh() {
        Component c = getActiveComponent();
        if(c instanceof Widget) Widget.class.cast(c).getController().refresh();
        else if (c instanceof Container) Container.class.cast(c).load();
    }
    
    @Override
    public void detach() {
        // create new window with no content (not even empty widget)
        Window w = ContextManager.showWindow(null);
               // set size to that of a source (also add header & border space)
               w.setSize(root.getWidth()+10, root.getHeight()+30);
        // change content
        Container c2 = w.getLayoutAggregator().getActive();
        int i1 = container.getParent().indexOf(container);
        int i2 = c2.indexOf(w.getLayoutAggregator().getActive().getChild());
        container.getParent().swapChildren(c2,i1,i2);
    }

    public void detachComponent(int i) {
        // grab component
        Component c = (Component)tabPane.getTabs().get(i).getUserData();
        // remove from layout graph
        container.getChildren().put(i, null);
        // remove from scene graph (it is done automatically, since at the
        // moment when the component loads it is removed from old location
        // but the tab remains and we need to clean it too
        tabPane.getTabs().remove(i);
        // create new window with the component as its content
        Window w = ContextManager.showWindow((Widget)c);
               // set size to that of a source (also add header & border space)
               w.setSize(root.getWidth()+10, root.getHeight()+30);
    }
    
    @Override
    public AnchorPane getContent() {
        return content;
    }
    
}