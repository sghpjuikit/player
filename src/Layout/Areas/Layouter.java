
package Layout.Areas;

import GUI.DragUtil;
import GUI.GUI;
import GUI.objects.Pickers.WidgetPicker;
import Layout.BiContainerPure;
import Layout.Container;
import Layout.PolyContainer;
import java.io.IOException;
import java.util.Objects;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import static javafx.animation.Interpolator.LINEAR;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import static javafx.geometry.Orientation.HORIZONTAL;
import static javafx.geometry.Orientation.VERTICAL;
import javafx.scene.Node;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import javafx.scene.input.MouseEvent;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import main.App;
import util.Animation.Interpolators.CircularInterpolator;
import static util.Animation.Interpolators.EasingMode.EASE_OUT;
import static util.Util.setAPAnchors;

/**
 * @author uranium
 * 
 * @TODO make dynamic indexes work and this widget part of layout map. See
 * TO Do file API section
 */
@Layout.Widgets.Widget.Info
public final class Layouter implements ContainerNode {
    
    public static final Duration ANIM_DUR = Duration.millis(300);
    
    private @FXML BorderPane controls;
    private @FXML AnchorPane root = new AnchorPane();
    private @FXML AnchorPane content;
    private final Container container;
    private int index;              // hack (see to do API section, layouts)
    
    private final FadeTransition a1;
    private final ScaleTransition a2;
    private final EventHandler<MouseEvent> clickShowHider;
    private final EventHandler<MouseEvent> exitHider;
    
    public Layouter(Container con, int index) {
        Objects.requireNonNull(con);
        
        this.index = index;
        this.container = con;
        
        FXMLLoader fxmlLoader = new FXMLLoader(Layouter.class.getResource("Layouter.fxml"));
        fxmlLoader.setRoot(root);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        
        Interpolator i = new CircularInterpolator(EASE_OUT);
        a1 = new FadeTransition(ANIM_DUR, content);
        // anim.setInterpolator(i); // use the LINEAR by defaul instead
        a2 = new ScaleTransition(ANIM_DUR, content);
        a2.setInterpolator(i);
            // initialize state for animations
        content.setOpacity(0);
        content.setScaleX(0);
        content.setScaleY(0);
                
        // prevent action & allow passing mouse events when not fully visible
        content.mouseTransparentProperty().bind(content.opacityProperty().isNotEqualTo(1));
        
        // do not support drag from - no content
        // but accept drag onto
        root.setOnDragOver(DragUtil.componentDragAcceptHandler);
        // handle drag onto
        root.setOnDragDropped( e -> {
            if (DragUtil.hasComponent()) {
                container.swapChildren(index,DragUtil.getComponent());
                e.setDropCompleted(true);
                e.consume();
            }
        });
        
        clickShowHider =  e -> {
            if(e.getButton()==PRIMARY) {
                if(!content.isMouseTransparent()) return;
                // avoid when under lock
                if(container.isUnderLock()) return;
                // rely on the public show() implementation, not internal one
                show();
                e.consume();
            }
            if (e.getButton()==SECONDARY) {
                if(!GUI.isLayoutMode()){
                    hide();
                    e.consume();
                }
            }
        };
        exitHider =  e -> {
            // rely on the public show() implementation, not internal one
            hide();
            e.consume();
        };
        
        // initialize mode
        setWeakMode(true); // this needs to be called in constructor
        // initialize show
        setShow(GUI.isLayoutMode());
    }

/****************************  functionality  *********************************/
    
    @Override
    public void show() {
        showControls(true);
    }

    @Override
    public void hide() {
        // prevent leaving layout mode when layout mode active
        if(GUI.isLayoutMode())return;
        showControls(false);
    }
    
    private void showControls(boolean val) {
        a1.stop();
        a2.stop();
        if (val) {
            a1.setToValue(1);
            a2.setToX(1);
            a2.setToY(1);
        } else {
            a1.setToValue(0);
            a2.setToX(0);
            a2.setToY(0);
            if(content.getChildren().size()>1 && !end) {
                a2.setOnFinished( ae -> {
                    controls.setVisible(true);
                    content.getChildren().retainAll(controls);
                    a2.setOnFinished(null);
                });
            }
        }
        a1.play();
        a2.play();
    }
    
    private boolean clickMode = true;
    
    /**
     * In normal mode the controls are displayed on mouse click
     * In weak mode the controls are displayed on mouse hover
     * Default false.
     * @param val 
     */
    public void setWeakMode(boolean val) {
        clickMode = val;
        
        // always hide on mouse exit, initialize
        if (root.getOnMouseExited()==null)
            root.setOnMouseExited(exitHider);
        // swap handlers
        if(val) {
            root.addEventFilter(MOUSE_CLICKED,clickShowHider);
            root.removeEventFilter(MOUSE_ENTERED,clickShowHider);
        } else {
            root.removeEventFilter(MOUSE_CLICKED,clickShowHider);
            root.addEventFilter(MOUSE_ENTERED,clickShowHider);
        }
    }
    
    public void toggleWeakMode() {
        clickMode = !clickMode;
    }
    public boolean isWeakMode() {
        return clickMode;
    }
    


    // quick fix to prevent overwriting onFinished animation handler
    boolean end = false;
    @FXML
    private void showWidgetArea(MouseEvent e) {
        if(e.getButton()!=PRIMARY) return;
        
        WidgetPicker w = new WidgetPicker();
        w.setOnSelect(f -> {
            end = true;
            a2.setOnFinished( a -> {
                // actually not needed since layouter is removed when widget is
                // loaded in the container in its place
//                controls.setVisible(true);
//                content.getChildren().retainAll(controls);
//                animS.setOnFinished(null);
                // this is the crucial part
                container.addChild(index, f.create());
                if(GUI.isLayoutMode()) container.show();
                App.actionStream.push("New widget");
            });
            showControls(false);
        });
        
        a2.setOnFinished( ae -> {
            Node n = w.getNode();
            content.getChildren().add(n);
            setAPAnchors(n, 0);
            controls.setVisible(false);
            showControls(true);
            a2.setOnFinished(null);
        });
        showControls(false);
        
        e.consume();
    }
    @FXML
    private void showSplitV(MouseEvent e) {
        if(e.getButton()!=PRIMARY) return;
        
        closeAndDo(a -> {
            container.addChild(index, new BiContainerPure(HORIZONTAL));
            App.actionStream.push("Divide layout");
        });

        e.consume();
    }
    @FXML
    private void showSplitH(MouseEvent e) {
        if(e.getButton()!=PRIMARY) return;
        
        closeAndDo(a -> {
            container.addChild(index, new BiContainerPure(VERTICAL));
            App.actionStream.push("Divide layout");
        });
        
        e.consume();
    }
    @FXML
    private void showTabs(MouseEvent e) {
        if(e.getButton()!=PRIMARY) return;
        
        closeAndDo(a -> container.addChild(index, new PolyContainer()));
        
        e.consume();
    }
    
    private void closeAndDo(EventHandler<ActionEvent> action) {
        FadeTransition a1 = new FadeTransition(ANIM_DUR);
                       a1.setToValue(0);
                       a1.setInterpolator(LINEAR);
        ScaleTransition a2 = new ScaleTransition(ANIM_DUR);
                        a2.setInterpolator(new CircularInterpolator(EASE_OUT));
                        a2.setToX(0);
                        a2.setToY(0);
        ParallelTransition pt = new ParallelTransition(content, a1, a2);
        pt.setOnFinished(action);
        pt.play();
    }
    
    @Override
    public Pane getRoot() {
        return root;
    }
}


