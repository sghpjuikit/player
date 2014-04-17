
package GUI.Containers;

import Configuration.Configurable;
import GUI.ContextManager;
import GUI.DragUtil;
import GUI.GUI;
import GUI.WidgetTransfer;
import GUI.Window;
import Layout.AltState;
import Layout.AltStateHandler;
import Layout.Component;
import Layout.Container;
import Layout.TabContainer;
import Layout.Widget;
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
 *
 */
public final class TabbedWidgetArea extends AnchorPane implements AltState {
    
    @FXML TabPane tabPane;
    @FXML Label nameL;
    @FXML Button menuB;
    @FXML Button propB;
    @FXML AnchorPane controls;
    @FXML Region activator;
    @FXML Region deactivator;
    
    private Component widget;  // active component, max one, null if none
    TabContainer container;
    
    // animations (initializing here will cause them malfunction)
    private FadeTransition contrAnim;
    private FadeTransition contAnim;
    private TranslateTransition blurAnim;
    
    private final AltStateHandler stateHandler = new AltStateHandler(){
        @Override public void in() { showControls(); }
        @Override public void out() { hideControls(); }
    };
    
    public TabbedWidgetArea(TabContainer c) {
        setMinSize(0, 0);
        container = c;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("TabbedWidgetArea.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException ex) {
            Log.err("Problem loading tabbed container. Resources couldnt be read.");
        }
        
        contrAnim = new FadeTransition(Duration.millis(GUI.duration_LM), controls);
        contAnim = new FadeTransition(Duration.millis(GUI.duration_LM), tabPane);
        blurAnim = new TranslateTransition(Duration.millis(GUI.duration_LM), tabPane);
        
        BoxBlur blur = new BoxBlur(0, 0, 1);
        tabPane.translateZProperty().addListener( o -> {
            if(GUI.blur_layoutMode) {
                blur.setWidth(tabPane.getTranslateZ());
                blur.setHeight(tabPane.getTranslateZ());
                tabPane.setEffect(blur);
            }
        });

        menuB.setOnMouseClicked( e -> {
            ContextManager.showMenu(ContextManager.widgetsMenu, menuB, container);
        });  
        propB.setDisable(true);
        propB.setOnMouseClicked( e -> 
            ContextManager.openFloatingSettingsWindow((Widget)widget)
        );
        activator.setOnMouseEntered(e -> {
            if (!stateHandler.isAlt() && !stateHandler.isLocked())
                stateHandler.in();
        });
        controls.setOnMouseExited( e -> {
            if (!stateHandler.isAlt())
                stateHandler.out();
        });
        this.setOnMouseExited( e -> {
            if (!stateHandler.isAlt())
                stateHandler.out();
        });
        
        // support drag from
        this.setOnDragDetected( e -> {
            if (!stateHandler.isAlt()) return;              // disallow in normal mode
            if (e.getButton() == MouseButton.PRIMARY) {     // primary button drag only
                ClipboardContent cc = new ClipboardContent();
                cc.put(DragUtil.widgetDataFormat, new WidgetTransfer(container, container.getParent()));
                Dragboard db = this.startDragAndDrop(TransferMode.ANY);
                          db.setContent(cc);
                e.consume();
            }
        });
        // support drag onto
        this.setOnDragOver( e -> {
            Dragboard db = e.getDragboard();
            if (db.hasContent(DragUtil.widgetDataFormat))
                e.acceptTransferModes(TransferMode.ANY);
            e.consume();
        });
        this.setOnDragDropped( e -> {
            Dragboard db = e.getDragboard();
            if (db.hasContent(DragUtil.widgetDataFormat)) {
                WidgetTransfer wt = (WidgetTransfer) db.getContent(DragUtil.widgetDataFormat);
                container.swapChildren(container, wt.getContainer(),wt.getWidget());
            }
            e.consume();
        });
        

        hide();
    }
    
    public Container getContainer() {
        return container;
    }
    /** @return active - currently displayed component */
    public Component getActiveComponent() {
        return (Component)tabPane.getSelectionModel().getSelectedItem().getUserData();
    }
    
    
    /** Purges all tabs. */
    public void clearTabs() {
        tabPane.getTabs().clear();
    }
    
    public void loadWidget(Component c) {
        DraggableTab t = new DraggableTab(c.getName());
//            t.setDetachable(false);
            t.setTooltip(new Tooltip(c.getName()));
            t.setUserData(c);
            t.setOnClosed(e -> removeWidget((Component)t.getUserData()));
        t.setOnSelectionChanged( e -> loadTab(t,(Component)t.getUserData()));
        tabPane.getTabs().add(t);
    }
    
    public void removeWidget(Component c) {
        container.removeChild(c);
        tabPane.getTabs().stream().filter(t->t.getUserData().equals(c)).findAny()
               .ifPresent(t->tabPane.getTabs().remove(t));
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
    public void refreshWidget() {
        getActiveComponent().load();
    }
    public void detach() {
        if (container!=null) {        
            Window w = ContextManager.openFloatingWindow(Widget.EMPTY());
            container.getParent().swapChildren(container, w.getLayout(), w.getLayout().getChild());
        }
    }
    public void close() {
        container.close();
    }
        
/******************************************************************************/
    @FXML
    @Override
    public void show() {
        stateHandler.in();
        stateHandler.setAlt(true);
    }
    @FXML
    @Override
    public void hide() {
        stateHandler.out();
        stateHandler.setAlt(false);
    }
    
    public void setLocked(boolean val) {
        stateHandler.setLocked(val);
    }
    @FXML
    public void toggleLocked() {
        if (stateHandler.isLocked())
            stateHandler.setLocked(false);
        else
            stateHandler.setLocked(true);
    }
    
    private void showControls() {
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
    private void hideControls() {
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