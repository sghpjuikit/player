
package Layout.WidgetImpl;

import Configuration.Config;
import GUI.objects.Pickers.WidgetPicker;
import GUI.objects.PopOver.ContextPopOver;
import GUI.objects.PopOver.PopOver;
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
import javafx.beans.binding.Bindings;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import static javafx.geometry.Orientation.HORIZONTAL;
import static javafx.geometry.Orientation.VERTICAL;
import javafx.scene.Node;
import static javafx.scene.input.MouseButton.PRIMARY;
import javafx.scene.input.MouseEvent;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
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
public final class Layouter extends Widget implements AltState, Controller<Widget> {
    
    private int index;              // hack (see to do API section, layouts)
    
    @FXML Pane controls;
    @FXML AnchorPane entireArea = new AnchorPane();
    private final Container container;
    private final FadeTransition anim;
    private final ScaleTransition animS;
    private boolean enabled = false;
    
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
        // anim.setInterpolator(i); // use the LINEAR by defaul instead
        animS = new ScaleTransition(Duration.millis(350), controls);
        animS.setInterpolator(i);
        controls.setOpacity(0);
        
        setWeakMode(false);
        
        entireArea.getStyleClass().setAll("darker");
        
        // dont show when opacity 0, this has a MouseTransparent=true effect
        // which we need to prevent clicking on controls when they are 'hidden'
        controls.visibleProperty().bind(Bindings.notEqual(controls.opacityProperty(),0));
    }

/****************************  functionality  *********************************/
    
    @Override
    public void show() {
        showControls(true);
    }

    @Override
    public void hide() {
        showControls(false);
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
    
    private boolean weakMode = false;
    
    /**
     * In normal mode the controls are displayed on mouse click
     * In weak mode the controls are displayed on mouse hover
     * Default false.
     * @param val 
     */
    public void setWeakMode(boolean val) {
        weakMode = val;
        
        // always hide on mouse exit, but make sure it is initialized
        if (entireArea.getOnMouseExited()==null)
            entireArea.setOnMouseExited(controlsHider);
        // swap handlers
        if(val) {
            entireArea.setOnMouseClicked(null);
            entireArea.setOnMouseEntered(controlsShower);
        } else {
            entireArea.setOnMouseClicked(controlsShower);
            entireArea.setOnMouseEntered(null);
        }
    }
    
    public void toggleWeakMode() {
        weakMode = !weakMode;
    }
    public boolean isWeakMode() {
        return weakMode;
    }
    
    private final EventHandler<MouseEvent> controlsShower =  e -> {
        showControls(true);
        e.consume();
    };
    private final EventHandler<MouseEvent> controlsHider =  e -> {
        if(e.getEventType().equals(MOUSE_CLICKED) && e.getButton()!=PRIMARY) return;
        showControls(false);
        e.consume();
    };

    
    @FXML
    private void showWidgetArea(MouseEvent e) {
        if(e.getButton()!=PRIMARY) return;
        // cant use here because it returns null
        // because Layouter is not part of Layout map
        // same goes for below methods
         Integer i = container.indexOf(this);
         
        WidgetPicker w = new WidgetPicker();
        ContextPopOver p = new ContextPopOver(w.getNode());
                       p.show((Node)e.getSource(), PopOver.NodeCentricPos.UpLeft);
        w.setOnSelect(f -> {
            container.addChild(index, f.create());
            p.hide();
        });
    }
    @FXML
    private void showSplitV(MouseEvent e) {
        if(e.getButton()!=PRIMARY) return;
        
        Integer i = container.indexOf(this);
        container.addChild(index, new BiContainerPure(HORIZONTAL));
    }
    @FXML
    private void showSplitH(MouseEvent e) {
        if(e.getButton()!=PRIMARY) return;
        
        Integer i = container.indexOf(this);
        container.addChild(index, new BiContainerPure(VERTICAL));
    }
    @FXML
    private void showTabs(MouseEvent e) {
        if(e.getButton()!=PRIMARY) return;
        
        Integer i = container.indexOf(this);
        container.addChild(index, new PolyContainer());
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


