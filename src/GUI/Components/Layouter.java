
package GUI.Components;

import Configuration.Config;
import Layout.AltState;
import Layout.BiContainerPure;
import Layout.Container;
import Layout.Widgets.Controller;
import Layout.PolyContainer;
import Layout.Widgets.Widget;
import Layout.Widgets.WidgetInfo;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

/**
 * @author uranium
 * 
 * @TODO make dynamic indexes work and this widget part of layout map. See
 * TO Do file API section
 */
@WidgetInfo
public class Layouter extends Widget implements AltState, Controller {
    
    private static final FXMLLoader fxmlLoader = new FXMLLoader(Layouter.class.getResource("Layouter.fxml"));
    private int index;              // hack (see to do API section, layouts)
    
    @FXML Pane controls;
    @FXML AnchorPane entireArea = new AnchorPane();
    private final Container container;
    private final FadeTransition anim;
    
    public Layouter(Container con, int index) {
        this.index = index;
        this.container = con;
        
        fxmlLoader.setRoot(entireArea);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        
        anim = new FadeTransition(Duration.millis(350), controls);
        controls.setOpacity(0);
        entireArea.setOnMouseEntered( e -> showControls(true));
        entireArea.setOnMouseExited( e -> showControls(false));
    }
    
    private void showControls(boolean val) {System.out.println(val);
        anim.stop();
        if (val) anim.setToValue(1);
            else anim.setToValue(0);
        anim.play();
    }
    
    @FXML
    private void showWidgetArea() {
        Integer i = container.indexOf(this);            // cant use here because it returns null
                                                        // because Layouter is not part of Layout map
        container.addChild(index, Widget.EMPTY());
    }
    @FXML
    private void showSplitV() {
        Integer i = container.indexOf(this);
        container.addChild(index, new BiContainerPure(Orientation.HORIZONTAL));
    }
    @FXML
    private void showSplitH() {
        Integer i = container.indexOf(this);
        container.addChild(index, new BiContainerPure(Orientation.VERTICAL));
    }
    @FXML
    private void showTabs() {
        Integer i = container.indexOf(this);
        container.addChild(index, new PolyContainer());
    }    

    @Override
    public void show() {
        showControls(true);
    }

    @Override
    public void hide() {
        showControls(false);
    }

/******************************************************************************/
    
    private Widget widget;

    @Override public String getName() {
        return "Layouter";
    }
    
    @Override public void refresh() { }

    @Override public void setWidget(Widget w) {
        widget = w;
    }

    @Override public Widget getWidget() {
        return widget;
    }

    @Override public Node load() {
        return entireArea;
    }

    @Override public Controller getController() {
        return this;
    }
    
    @Override public WidgetInfo getInfo() {
        return getClass().getAnnotation(WidgetInfo.class);
    }

    @Override public List<Config> getFields() {
        return Collections.EMPTY_LIST;
    }    
}


