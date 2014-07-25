
package Layout.Areas;

import Configuration.Configurable;
import GUI.ContextManager;
import GUI.DragUtil;
import GUI.WidgetTransfer;
import GUI.Window;
import Layout.Component;
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
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;

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
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("TabbedWidgetArea.fxml"));
        fxmlLoader.setRoot(root);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException ex) {
            throw new RuntimeException(ex.getMessage());
        }
        // load controls
        controls = new AreaControls(this);
        root.getChildren().add(controls.root);
        AnchorPane.setBottomAnchor(controls.root, 0.0);
        AnchorPane.setTopAnchor(controls.root, 0.0);
        AnchorPane.setLeftAnchor(controls.root, 0.0);
        AnchorPane.setRightAnchor(controls.root, 0.0);
        
        // support drag from
        root.setOnDragDetected( e -> {
            if (!controls.isShowing()) return;              // disallow in normal mode
            if (e.getButton() == MouseButton.PRIMARY) {     // primary button drag only
                ClipboardContent cc = new ClipboardContent();
                cc.put(DragUtil.widgetDF, new WidgetTransfer(container, container.getParent()));
                Dragboard db = root.startDragAndDrop(TransferMode.ANY);
                          db.setContent(cc);
                e.consume();
            }
        });
        // support drag onto
        root.setOnDragOver( e -> {
            Dragboard db = e.getDragboard();
            if (db.hasContent(DragUtil.widgetDF)) {
                e.acceptTransferModes(TransferMode.ANY);
                e.consume();
            }
        });
        root.setOnDragDropped( e -> {
            Dragboard db = e.getDragboard();
            if (db.hasContent(DragUtil.widgetDF)) {
                WidgetTransfer wt = (WidgetTransfer) db.getContent(DragUtil.widgetDF);
                container.swapChildren(container, wt.getContainer(),wt.getWidget());
            }
            e.consume();
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
        cs.stream().forEach(c->{
            DraggableTab t = new DraggableTab(c.getName());
//            t.setDetachable(false);
            t.setTooltip(new Tooltip(c.getName()));
            t.setUserData(c);
            t.setOnClosed(e -> removeComponent((Component)t.getUserData()));
            tabPane.getTabs().add(t);
            t.setOnSelectionChanged( e -> {
                if(tabPane.getTabs().contains(t) && t.isSelected())
                    loadTab(t,(Component)t.getUserData());
            });
            loadTab(t, c);
        });
        
        
        
        // select 1st if none selected
//        if(tabPane.getTabs().size()==cs.size()) {
//            int i = container.properties.getI("selected");
            if(i<0) i = 0;
            if(i==0) {
                Tab t = tabPane.getTabs().get(i);
                loadTab(t,(Component)t.getUserData());
            }
            selectComponent(i);
//        }
    }
    
    @Override
    public void removeComponent(Component c) {
        container.removeChild(c);
        tabPane.getTabs().stream().filter(t->t.getUserData().equals(c)).findAny()
               .ifPresent(t->tabPane.getTabs().remove(t));
    }
    
    private void selectComponent(Integer i) {System.out.println("showigng tab "+i);
        int tabs = tabPane.getTabs().size();
        if(i==null) {
            if(tabPane.getSelectionModel().isEmpty())
                tabPane.getSelectionModel().select(0);
            System.out.println("selecting 0");
        }
        if(i!=null){
//            if(tabs>i) i = tabs;    // prevent out of bounds
//            if(i<1) i = 1;          // prevent no selection
//            if(tabs>0 && tabPane.getSelectionModel().getSelectedIndex() != i)
                tabPane.getSelectionModel().select(i);
        }
//        
//        // prevent no selection
//        if(!tabPane.getTabs().isEmpty() && tabPane.getSelectionModel().isEmpty())
//            tabPane.getSelectionModel().select(0);
        
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
        container.properties.set("selected", ii);
    }
    
    /** Refreshes the active widget */
    @Override
    public void refresh() {
        getActiveComponent().load();
    }
    @Override
    public void detach() {
        // create new window with empty widget as content to initialize layouts
        Window w = ContextManager.showWindow(Widget.EMPTY());
               // set size to that of a source
               w.setSize(root.getWidth(), root.getHeight());
        // change content
        container.getParent().swapChildren(container, 
                w.getLayoutAggregator().getActive(), 
                w.getLayoutAggregator().getActive().getChild());
    }
    
    @Override
    public AnchorPane getContent() {
        return content;
    }
    
}