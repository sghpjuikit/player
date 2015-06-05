
package Layout.Areas;

import gui.objects.Pickers.Picker;
import gui.objects.Pickers.WidgetPicker;
import util.graphics.drag.DragUtil;
import gui.GUI;
import static gui.GUI.*;
import Layout.BiContainerPure;
import Layout.Container;
import Layout.FreeFormContainer;
import Layout.PolyContainer;
import java.util.Objects;
import java.util.stream.Stream;
import javafx.animation.*;
import static javafx.animation.Interpolator.LINEAR;
import javafx.event.EventHandler;
import static javafx.geometry.Orientation.HORIZONTAL;
import static javafx.geometry.Orientation.VERTICAL;
import static javafx.scene.input.MouseButton.PRIMARY;
import javafx.scene.input.MouseEvent;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import main.App;
import util.Animation.Interpolators.CircularInterpolator;
import static util.Animation.Interpolators.EasingMode.EASE_OUT;
import static util.Util.setAnchors;

/**
 * @author uranium
 * 
 * @TODO make dynamic indexes work and this widget part of layout map. See
 * TO Do file API section
 */
@Layout.Widgets.Widget.Info
public final class Layouter implements ContainerNode {
    
    private final Container container;
    private final int index;
    
    public final Picker<String> cp = new Picker();
    public final AnchorPane root = new AnchorPane(cp.root);
    
    private final FadeTransition a1;
    private final ScaleTransition a2;
    private final EventHandler<MouseEvent> clickShowHider;
    private final EventHandler<MouseEvent> exitHider;
    
    public Layouter(Container con, int index) {
        Objects.requireNonNull(con);
        
        this.index = index;
        this.container = con;
        
        cp.onSelect = layout -> {
            switch(layout) {
                case "Split Vertical" : closeAndDo(cp.root, this::showSplitV);
                                        break;
                case "Split Horizontal" : closeAndDo(cp.root, this::showSplitH);
                                        break;
                case "Widget" : closeAndDo(cp.root, this::showWidgetArea);
                                break;
                case "Tabs"   : closeAndDo(cp.root, this::showTabs);
                                break;
                case "FreeForm" : closeAndDo(cp.root, this::showFreeform);
                                  break;
            }
        };
        cp.onCancel = this::hide;
        cp.textCoverter = text -> text;
        cp.itemSupply = () -> Stream.of("Split Vertical", "Split Horizontal",
                                        "Widget", "Tabs", "FreeForm");
        cp.buildContent();
        
        setAnchors(cp.root, 0);
        
        Interpolator i = new CircularInterpolator(EASE_OUT);
        a1 = new FadeTransition(ANIM_DUR, cp.root);
        a1.setInterpolator(LINEAR);
        a2 = new ScaleTransition(ANIM_DUR, cp.root);
        a2.setInterpolator(i);
        
        cp.root.setOpacity(0);
        cp.root.setScaleX(0);
        cp.root.setScaleY(0);
        
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
                if(cp.root.getOpacity()!=0) return;
                // avoid when under lock
                if(container.isUnderLock()) return;
                // rely on the public show() implementation, not internal one
                show();
                e.consume();
            }

        };
        exitHider =  e -> cp.onCancel.run();
//        exitHider =  e -> {
//            // rely on the public show() implementation, not internal one
//            cp.onCancel.run();
//            e.consume();
//        };
        
        // initialize mode
        setWeakMode(true); // this needs to be called in constructor
        // initialize show
        setShow(GUI.isLayoutMode());
    }

/****************************  functionality  *********************************/
    
    @Override
    public void show() {
        showControls(true);
//        openAndDo(cp.root, null);
    }

    @Override
    public void hide() {
        // prevent leaving layout mode when layout mode active
        if(GUI.isLayoutMode())return;
        showControls(false);
//        closeAndDo(cp.root, null);
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
        }
        a1.play();
        a2.play();
        
//        Interpolator in = new SineInterpolator();
////        forEachIndexedStream(cp.getCells(), (i,n) -> new Anim(millis(300), in, at -> { n.setScaleX(at); n.setScaleY(at); }).delay(millis(i*50)))
////            .toArray(Anim[]::new)
////        cp.getCells().stream().map(n->new Anim(millis(300), i, at -> { n.setScaleX(at); n.setScaleY(at); })).to
//        Transition t = new ParallelTransition(
//            forEachIndexedStream(cp.getCells(), (i,n) -> new Anim(millis(300), in, at -> { n.setScaleX(at); n.setScaleY(at); }).delay(millis(i*50)))
//            .toArray(Anim[]::new)
//        );
//        t.setDelay(millis(500));
//        t.play();
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
            root.addEventHandler(MOUSE_CLICKED,clickShowHider);
            root.removeEventHandler(MOUSE_ENTERED,clickShowHider);
        } else {
            root.addEventHandler(MOUSE_CLICKED,clickShowHider);
            root.removeEventHandler(MOUSE_ENTERED,clickShowHider);
        }
    }
    
    public void toggleWeakMode() {
        clickMode = !clickMode;
    }
    public boolean isWeakMode() {
        return clickMode;
    }
    

    private void showWidgetArea() {
        WidgetPicker w = new WidgetPicker();
        w.onSelect = factory -> {
            closeAndDo(w.root, () -> {
                root.getChildren().remove(w.root);
                root.setOnMouseExited(null);
                // this is the crucial part
                container.addChild(index, factory.create());
                if(GUI.isLayoutMode()) container.show();
                App.actionStream.push("New widget");
            });
        };
        w.onCancel = () -> closeAndDo(w.root, () -> {
            root.getChildren().remove(w.root);
            showControls(GUI.isLayoutMode());
        });
        w.buildContent();
        root.getChildren().add(w.root);
        setAnchors(w.root, 0);
        openAndDo(w.root, null);
    }
    private void showSplitV() {
        container.addChild(index, new BiContainerPure(HORIZONTAL));
        App.actionStream.push("Divide layout");
    }
    private void showSplitH() {
        container.addChild(index, new BiContainerPure(VERTICAL));
        App.actionStream.push("Divide layout");
    }
    private void showTabs() {
        container.addChild(index, new PolyContainer());
    }
    private void showFreeform() {
        container.addChild(index, new FreeFormContainer());
    }
    
    @Override
    public Pane getRoot() {
        return root;
    }
}