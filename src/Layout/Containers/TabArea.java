
package Layout.Containers;

import Configuration.Configurable;
import GUI.ContextManager;
import GUI.DragUtil;
import GUI.GUI;
import GUI.WidgetTransfer;
import GUI.Window;
import Layout.Component;
import Layout.PolyContainer;
import Layout.Widgets.Widget;
import java.io.IOException;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.BoxBlur;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import utilities.Log;

/**
 * Implementation of PolyArea.
 */
public final class TabArea extends PolyArea {
    
    @FXML TabPane tabPane;
    @FXML Label nameL;
    @FXML Button menuB;
    @FXML Button propB;
    @FXML AnchorPane controls;
    @FXML Region activator;
    @FXML Region deactivator;
    
//    private DraggableTab addTab;
//    private final List<DraggableTab> tabs = new ArrayList();
    private Component widget;  // active component, max one, null if none
    private boolean forbid_index_update = false;
    
    // animations (initializing here will cause them malfunction)
    private FadeTransition contrAnim;
    private FadeTransition contAnim;
    private TranslateTransition blurAnim;
    
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
            Log.err("Problem loading tabbed container. Resources couldnt be read.");
        }
        
//        addTab = new DraggableTab("+");
//        addTab.setDetachable(false);
//        addTab.setClosable(false);
//        addTab.setOnSelectionChanged( e -> {
////            container.a
//            if(addTab.isSelected())
//                ContextManager.showMenu(ContextManager.widgetsMenu, menuB, container);
//        });
//        tabPane.getTabs().add(addTab);
        
        tabPane.getSelectionModel().selectedIndexProperty().addListener((o,oldV,newV)->{
            if(!forbid_index_update && container.properties.getI("selected")!=newV.intValue()) {
                container.properties.set("selected", newV.intValue());
                System.out.println("BGBGBG "+newV);
            }
        });
        
        contrAnim = new FadeTransition(Duration.millis(GUI.duration_LM), controls);
        contAnim = new FadeTransition(Duration.millis(GUI.duration_LM), tabPane);
        blurAnim = new TranslateTransition(Duration.millis(GUI.duration_LM), tabPane);
        
        BoxBlur blur = new BoxBlur(0, 0, 1);
        blur.widthProperty().bind(tabPane.translateZProperty());
        blur.heightProperty().bind(tabPane.translateZProperty());
        tabPane.translateZProperty().addListener( o -> {
            tabPane.setEffect(blur);
        });

        menuB.setOnMouseClicked( e -> {
            ContextManager.showMenu(ContextManager.widgetsMenu, menuB, container);
        });  
        propB.setDisable(true);
        propB.setOnMouseClicked( e -> 
            ContextManager.openFloatingSettingsWindow((Widget)widget)
        );
        activator.setOnMouseEntered(e -> {
            if (!layoutMode.isAlt() && !layoutMode.isLocked())
                layoutMode.in();
        });
        controls.setOnMouseExited( e -> {
            if (!layoutMode.isAlt())
                layoutMode.out();
        });
        root.setOnMouseExited( e -> {
            if (!layoutMode.isAlt())
                layoutMode.out();
        });
        
        // support drag from
        root.setOnDragDetected( e -> {
            if (!layoutMode.isAlt()) return;              // disallow in normal mode
            if (e.getButton() == MouseButton.PRIMARY) {     // primary button drag only
                ClipboardContent cc = new ClipboardContent();
                cc.put(DragUtil.widgetDataFormat, new WidgetTransfer(container, container.getParent()));
                Dragboard db = root.startDragAndDrop(TransferMode.ANY);
                          db.setContent(cc);
                e.consume();
            }
        });
        // support drag onto
        root.setOnDragOver( e -> {
            Dragboard db = e.getDragboard();
            if (db.hasContent(DragUtil.widgetDataFormat))
                e.acceptTransferModes(TransferMode.ANY);
            e.consume();
        });
        root.setOnDragDropped( e -> {
            Dragboard db = e.getDragboard();
            if (db.hasContent(DragUtil.widgetDataFormat)) {
                WidgetTransfer wt = (WidgetTransfer) db.getContent(DragUtil.widgetDataFormat);
                container.swapChildren(container, wt.getContainer(),wt.getWidget());
            }
            e.consume();
        });
        

        hide();
    }
    
    /** @return active - currently displayed component */
    public Component getActiveComponent() {
        return (Component)tabPane.getSelectionModel().getSelectedItem().getUserData();
    }
    
    
    @Override
    public void addComponent(Component c) {
        DraggableTab t = new DraggableTab(c.getName());
//            t.setDetachable(false);
            t.setTooltip(new Tooltip(c.getName()));
            t.setUserData(c);
            t.setOnClosed(e -> removeComponent((Component)t.getUserData()));
            t.setOnSelectionChanged( e -> {
                if(t.isSelected())
                    loadTab(t,(Component)t.getUserData());
            });
//        tabs.add(t);
//        tabPane.getTabs().add(tabPane.getTabs().size()-1,t);
            forbid_index_update = true;
        tabPane.getTabs().add(t);
        forbid_index_update = false;
    }
    
    @Override
    public void removeComponent(Component c) {
        container.removeChild(c);
        tabPane.getTabs().stream().filter(t->t.getUserData().equals(c)).findAny()
               .ifPresent(t->tabPane.getTabs().remove(t));
//        tabs.stream().filter(t->t.getUserData().equals(c)).findAny()
//            .ifPresent(t->tabPane.getTabs().remove(t));
    }
    
    public void showComponentAt(int i) {System.out.println("SETTING SHOW "+i);
        tabPane.getSelectionModel().select(i);
// not needed - updates automatically on selectionModel.change (see constructor)        
//        if(container.properties.getI("selected")!=i)
//            container.properties.set("selected", i);
    }
    
    /** Purges all tabs. */
    @Override
    public void removeAllComponents() {
        tabPane.getTabs().clear();
//        tabPane.getTabs().removeAll(tabs);
    }
    
    private void loadTab(Tab t, Component c) {
        if (c == null || t == null) return;
        widget = c;
        AnchorPane w = (AnchorPane) c.load();
        t.setContent(w);
        t.getContent().setLayoutX(0);
        t.getContent().setLayoutY(0);
        t.getContent().minHeight(0);
        t.getContent().minWidth(0);
        nameL.setText(c.getName());
        propB.setDisable(false);
        if(c instanceof Widget)
            propB.setDisable(((Configurable)c).getFields().isEmpty());
    }
    
    /** Refreshes the active widget */
    @Override
    public void refreshWidget() {
        getActiveComponent().load();
    }
    public void detach() {
        Window w = ContextManager.openFloatingWindow(Widget.EMPTY());
        container.getParent().swapChildren(container, w.getLayout(), w.getLayout().getChild());
    }
    
    
    @Override
    protected void showControls() {
        contrAnim.stop();
        contAnim.stop();
        blurAnim.stop();
        contrAnim.setToValue(1);
        contAnim.setToValue(1-GUI.opacity_LM);
        blurAnim.setToZ(GUI.blur_LM);
        contrAnim.play();
        if(GUI.opacity_layoutMode)
            contAnim.play();
        if(GUI.blur_layoutMode)
            blurAnim.play();
        activator.toBack();
        tabPane.setMouseTransparent(true);
        deactivator.setMouseTransparent(false);
    }
    @Override
    protected void hideControls() {
        contrAnim.stop();
        contAnim.stop();
        blurAnim.stop();
        contrAnim.setToValue(0);
        contAnim.setToValue(1);
        blurAnim.setToZ(0);
        contrAnim.play();
        if(GUI.opacity_layoutMode)
            contAnim.play();
        if(GUI.blur_layoutMode)
            blurAnim.play();
        activator.toFront();
        tabPane.setMouseTransparent(false);
        deactivator.setMouseTransparent(true);
    }
    
}