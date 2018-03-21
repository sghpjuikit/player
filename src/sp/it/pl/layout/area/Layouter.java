
package sp.it.pl.layout.area;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import sp.it.pl.gui.Gui;
import sp.it.pl.gui.objects.picker.Picker;
import sp.it.pl.gui.objects.picker.WidgetPicker;
import sp.it.pl.layout.container.Container;
import sp.it.pl.layout.container.bicontainer.BiContainer;
import sp.it.pl.layout.container.freeformcontainer.FreeFormContainer;
import sp.it.pl.main.AppAnimator;
import sp.it.pl.main.AppBuildersKt;
import sp.it.pl.util.animation.interpolator.CircularInterpolator;
import sp.it.pl.util.collections.Tuple3;
import sp.it.pl.util.graphics.drag.DragUtil;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.EXCHANGE;
import static javafx.animation.Interpolator.LINEAR;
import static javafx.geometry.Orientation.HORIZONTAL;
import static javafx.geometry.Orientation.VERTICAL;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static sp.it.pl.main.AppUtil.APP;
import static sp.it.pl.util.animation.interpolator.EasingMode.EASE_OUT;
import static sp.it.pl.util.collections.Tuples.tuple;
import static sp.it.pl.util.dev.Util.noNull;
import static sp.it.pl.util.functional.Util.stream;
import static sp.it.pl.util.graphics.Util.setAnchor;
import static sp.it.pl.util.graphics.Util.setAnchors;
import static sp.it.pl.util.graphics.drag.DragUtil.installDrag;

/**
 * Graphics for when components of a {@link Container}} is null at certain index. It shows a
 * component chooser for user to choose container or widget to put into parent container at the
 * index - it allows creation of layouts.
 * <p/>
 * Uses two nested {@link Picker} - 1 for components and 2 for widgets.
 * The component picker is top level picker offering component type choices (all container
 * implementations or widget). If user picks container it will be constructed. If user picks widget
 * option, widget picker will be displayed, constructing widgets.
 */
public final class Layouter implements ContainerNode {

    private final Container<?> container;
    private final int index;

    public final AnchorPane root = new AnchorPane();
    private final Picker<Tuple3<String,Runnable,String>> cp = new Picker<>();
    private WidgetPicker wp = new WidgetPicker();

    /** Component picker on cancel action. */
    public Runnable onCpCancel = null;
    private boolean isCancelPlaying = false; // avoids calling onCancel twice
    private boolean weakMode = true;

    private final FadeTransition a1;
    private final ScaleTransition a2;
    private final EventHandler<MouseEvent> clickShowHider;
    private final EventHandler<MouseEvent> exitHider;

    public Layouter(Container<?> c, int index) {
        noNull(c);
        this.index = index;
        this.container = c;

        cp.itemSupply = () -> stream(
            tuple("Split Vertically", this::showSplitV, "Splits space to left and right component."),
            tuple("Split Horizontally", this::showSplitH, "Splits space to top and bottom component."),
            tuple("Widget", this::showWidgetArea, "Choose a widget using a widget chooser."),
            tuple("FreeForm", this::showFreeform, "Container for unlimited number of widgets. Widgets become 'window-like' - allow "
                    + "manual resizing and reposition. Widgets may overlap. You can make use of autolayout mechanism.")
        );
        cp.textConverter = layout_action -> layout_action._1;
        cp.infoConverter = layout_action -> layout_action._3;
        cp.onSelect = layout_action -> AppAnimator.INSTANCE.closeAndDo(cp.root, layout_action._2);
        cp.onCancel = () -> {
            isCancelPlaying = true;
            hide();
        };
        cp.consumeCancelClick = false; // we need right click to close container
        cp.buildContent();
        setAnchor(root, cp.root,0d);

        Duration dur = AppBuildersKt.nodeAnimation(new Rectangle()).getCycleDuration();
        a1 = new FadeTransition(dur, cp.root);
        a1.setInterpolator(LINEAR);
        a2 = new ScaleTransition(dur, cp.root);
        a2.setInterpolator(new CircularInterpolator(EASE_OUT));

        cp.root.setOpacity(0);
        cp.root.setScaleX(0);
        cp.root.setScaleY(0);

        // drag&drop
        installDrag(
            root, EXCHANGE, "Switch components",
            DragUtil::hasComponent,
            e -> container == DragUtil.getComponent(e),
            e -> DragUtil.getComponent(e).swapWith(container,index)
        );

        clickShowHider =  e -> {
            if (e.getButton()==PRIMARY) {
                if (cp.root.getOpacity()!=0) return;
                // avoid when under lock
                if (container.lockedUnder.get()) return;
                // rely on the public show() implementation, not internal one
                show();
                e.consume();
            }
        };
//        exitHider =  e -> cp.onCancel.run();
        exitHider = e -> {
            if (!isCancelPlaying) {
                // rely on the public show() implementation, not internal one
                cp.onCancel.run();
                e.consume();
            }
        };

        setWeakMode(true); // this needs to be called in constructor
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
        if (Gui.isLayoutMode()) return;
        showControls(false);
//        closeAndDo(cp.root, null);
    }

    private void showControls(boolean val) {
        a1.stop();
        a2.stop();
        if (val) {
            a1.setOnFinished(null);
            a1.setToValue(1);
            a2.setToX(1);
            a2.setToY(1);
        } else {
            a1.setOnFinished(isCancelPlaying ? e -> {
                isCancelPlaying = false;
                if (onCpCancel!=null) onCpCancel.run();
            } : e -> isCancelPlaying=false);
            a1.setToValue(0);
            a2.setToX(0);
            a2.setToY(0);
            isCancelPlaying = true;
        }
        a1.play();
        a2.play();
    }

    /**
     * In normal mode the controls are displayed on mouse click
     * In weak mode the controls are displayed on mouse hover
     * Default false.
     * @param val
     */
    public void setWeakMode(boolean val) {
        weakMode = val;

        // always hide on mouse exit, setParentRec
        if (root.getOnMouseExited()==null)
            root.setOnMouseExited(exitHider);
        // swap handlers
        if (val) {
            root.addEventHandler(MOUSE_CLICKED,clickShowHider);
            root.removeEventHandler(MOUSE_ENTERED,clickShowHider);
        } else {
            root.addEventHandler(MOUSE_CLICKED,clickShowHider);
            root.removeEventHandler(MOUSE_ENTERED,clickShowHider);
        }
    }

    public void toggleWeakMode() {
        weakMode = !weakMode;
    }
    public boolean isWeakMode() {
        return weakMode;
    }

    private void showWidgetArea() {
        wp = new WidgetPicker();
        wp.onSelect = factory -> {
            AppAnimator.INSTANCE.closeAndDo(wp.root, () -> {
                root.getChildren().remove(wp.root);
                root.setOnMouseExited(null);
                // this is the crucial part
                container.addChild(index, factory.create());
                if (Gui.isLayoutMode()) container.show();
                APP.actionStream.push("New widget");
            });
        };
        wp.onCancel = () -> AppAnimator.INSTANCE.closeAndDo(wp.root, () -> {
            root.getChildren().remove(wp.root);
            showControls(true);
        });
        wp.consumeCancelClick = true; // we need right click to not close container
        wp.root.addEventHandler(MOUSE_CLICKED, Event::consume); // also left click to not open container chooser
        wp.buildContent();
        root.getChildren().add(wp.root);
        setAnchors(wp.root, 0d);
        AppAnimator.INSTANCE.openAndDo(wp.root, null);
    }
    private void showSplitV() {
        container.addChild(index, new BiContainer(HORIZONTAL));
        APP.actionStream.push("Divide layout");
    }
    private void showSplitH() {
        container.addChild(index, new BiContainer(VERTICAL));
        APP.actionStream.push("Divide layout");
    }
    private void showFreeform() {
        container.addChild(index, new FreeFormContainer());
    }

    @Override
    public Pane getRoot() {
        return root;
    }
}