
package Layout.Areas;

import Configuration.Configurable;
import GUI.ContextManager;
import GUI.DragUtil;
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
import java.util.Objects;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Dragboard;
import static javafx.scene.input.MouseButton.PRIMARY;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import utilities.Log;
import utilities.Util;
import static utilities.Util.NotNULL;

/**
 * Implementation of PolyArea.
 */
public final class TabArea extends PolyArea {
    
    private @FXML TabPane tabPane;
    private @FXML AnchorPane content;
    
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
        
        register();
        
        // load controls
        controls = new AreaControls(this);
        root.getChildren().add(controls.root);
        Util.setAPAnchors(controls.root, 0);
        
        // support drag from
        root.setOnDragDetected( e -> {
            // disallow in normal mode & primary button drag only
            if (controls.isShowingWeak() && e.getButton()==PRIMARY) {
                Dragboard db = root.startDragAndDrop(TransferMode.MOVE);
                DragUtil.setComponent(container.getParent(),container,db);
                // signal dragging graphically with css
                content.pseudoClassStateChanged(draggedPSEUDOCLASS, true);
                e.consume();
            }
        });
        // support drag onto
        root.setOnDragOver(DragUtil.componentDragAcceptHandler);
        // handle drag onto
        root.setOnDragDropped( e -> {
            if (DragUtil.hasComponent()) {
                int i = container.getParent().indexOf(container);
                container.swapChildren(i, DragUtil.getComponent());
                e.setDropCompleted(true);
                e.consume();
            }
        });
        // return graphics to normal
        root.setOnDragDone( e -> content.pseudoClassStateChanged(draggedPSEUDOCLASS, false));
        
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
        cs.stream().filter(NotNULL).forEach(c -> {
            Tab t = buildTab(c,tabPane.getTabs().size());
            container.getChildren().put(container.getChildren().size(), c);
            tabPane.getTabs().add(t);
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
        Objects.requireNonNull(c);
        tabPane.getTabs().stream()
                .filter(t -> t.getUserData() == c)
                .findAny()
                .ifPresent( t -> {
                    t.setUserData(null);
                    tabPane.getTabs().remove(t);
                });
    }
    
/******************************** selection ***********************************/
    
    private static boolean selectionLock = false;
    
    public void selectComponentPreventLoad(boolean val) {
        selectionLock = val;
    }
    
    public void selectComponent(Integer i) {
        // release lock on manual change
        selectionLock = false;
        // select
        tabPane.getSelectionModel().select(i==null ? 0 : i);
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
        Component w2 = w.getLayoutAggregator().getActive().getChild();
        int i1 = container.getParent().indexOf(container);
        container.getParent().swapChildren(c2,i1,w2);
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
    
    public void moveTab(int from, int to) {
        Log.deb("Moving component tab " + from + " " + to);
        
        // prevent selection change
        selectComponentPreventLoad(true);
        
        // pointless, order would remain the same, return
        if(from==to || from+1==to) return;
        
        Tab t = tabPane.getTabs().get(from);
        
        Tab newT = buildTab((Component) t.getUserData(), to);
        tabPane.getTabs().add(to,newT);
        tabPane.getTabs().remove(t);
        
        // because there are two indexes per child (left & right) if we move
        // to the right we must decrement by one
        // for n tabs there is n+1 positions between them.
        // this miraculously fixes the resulting problem of selection
        if(from<to) to--;
        // synchronize selection
        int oldSel = container.properties.getI("selected");
        int newSel;
        if (oldSel==from) newSel = to;
        else if (oldSel>from && oldSel<=to) newSel = oldSel+1;
        else if (to>oldSel && oldSel<=from) newSel = oldSel-1;
        else newSel = oldSel;
        selectComponent(newSel);
    }
    
    @Override
    public AnchorPane getContent() {
        return content;
    }
    
    
    // the tab must be added to tabPane after this
    private DraggableTab buildTab(Component c, int to) {
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
        return t;
    }
    
/******************************************************************************/
    
    public void register() {
        DraggableTab.tabPanes.add(tabPane); // register to support drag
    }
    
    @Override
    public void close() {
        tabPane.getTabs().forEach(t -> t.setUserData(null));
        DraggableTab.tabPanes.remove(tabPane); // unregister from drag
    }
    
}