
package GUI.Components;

import Configuration.Config;
import Layout.AltState;
import Layout.BiContainer;
import Layout.Container;
import Layout.Controller;
import Layout.TabContainer;
import Layout.Widget;
import Layout.WidgetInfo;
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
 */
@WidgetInfo
public class Layouter extends Widget implements AltState, Controller {
    
    private static final FXMLLoader fxmlLoader = new FXMLLoader(Layouter.class.getResource("Layouter.fxml"));
    
    @FXML Pane controls;
    @FXML AnchorPane entireArea = new AnchorPane();
    private final Container container;
    private final FadeTransition anim;
    
    public Layouter(Container con) {
        
        container = con;
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
        int i = container.indexOf(this);
        container.addChild(1, Widget.EMPTY());
    }
    @FXML
    private void showSplitV() {
        int i = container.indexOf(this);
        container.addChild(1, new BiContainer(Orientation.VERTICAL));
    }
    @FXML
    private void showSplitH() {
        int i = container.indexOf(this);
        container.addChild(1, new BiContainer(Orientation.HORIZONTAL));
    }
    @FXML
    private void showTabs() {
        int i = container.indexOf(this);
        container.addChild(1, new TabContainer());
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


