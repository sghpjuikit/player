
package Layout.WidgetImpl;

import Configuration.Config;
import Layout.AltState;
import Layout.BiContainerPure;
import Layout.Container;
import Layout.PolyContainer;
import Layout.Widgets.Controller;
import Layout.Widgets.Widget;
import Layout.Widgets.WidgetInfo;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import static javafx.scene.input.MouseButton.PRIMARY;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import static utilities.Animation.Interpolators.EasingMode.EASE_OUT;
import utilities.Animation.Interpolators.ElasticInterpolator;

/**
 * @author uranium
 * 
 * @TODO make dynamic indexes work and this widget part of layout map. See
 * TO Do file API section
 */
@WidgetInfo
public class Layouter extends Widget implements AltState, Controller<Widget> {
    
    private int index;              // hack (see to do API section, layouts)
    
    @FXML Pane controls;
    @FXML AnchorPane entireArea = new AnchorPane();
    private final Container container;
    private final FadeTransition anim;
    private final ScaleTransition animS;
    
    public Layouter(Container con, int index) {
        super("Layouter");
        this.index = index;
        this.container = con;
        
        FXMLLoader fxmlLoader = new FXMLLoader(Layouter.class.getResource("Layouter.fxml"));
        fxmlLoader.setRoot(entireArea);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        
        Interpolator i = new ElasticInterpolator(EASE_OUT);
        anim = new FadeTransition(Duration.millis(350), controls);
//        anim.setInterpolator(i);
        animS = new ScaleTransition(Duration.millis(350), controls);
        animS.setInterpolator(i);
        controls.setOpacity(0);
        entireArea.setOnMouseEntered( e -> showControls(true));
        entireArea.setOnMouseExited( e -> showControls(false));
        entireArea.getStyleClass().setAll("darker");
    }
    
    private void showControls(boolean val) {
        anim.stop();
        animS.stop();
        if (val) {
            anim.setToValue(1);
            animS.setToX(1);
            animS.setToY(1);
        } else {
            anim.setToValue(0);
            animS.setToX(0);
            animS.setToY(0);
        }
        anim.play();
        animS.play();
    }
    
    @FXML
    private void showWidgetArea(MouseEvent e) {
        if(e.getButton()!=PRIMARY) return;
        Integer i = container.indexOf(this);            // cant use here because it returns null
                                                        // because Layouter is not part of Layout map
        container.addChild(index, Widget.EMPTY());
    }
    @FXML
    private void showSplitV(MouseEvent e) {
        if(e.getButton()!=PRIMARY) return;
        Integer i = container.indexOf(this);
        container.addChild(index, new BiContainerPure(Orientation.HORIZONTAL));
    }
    @FXML
    private void showSplitH(MouseEvent e) {
        if(e.getButton()!=PRIMARY) return;
        Integer i = container.indexOf(this);
        container.addChild(index, new BiContainerPure(Orientation.VERTICAL));
    }
    @FXML
    private void showTabs(MouseEvent e) {
        if(e.getButton()!=PRIMARY) return;
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

/****************************  as controller  *********************************/
    
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

/******************************   as widget  **********************************/
    
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

    @Override
    public boolean isEmpty() {
        return true;
    }
    
    
}


