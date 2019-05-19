package sp.it.pl.layout.widget;

import java.util.function.Consumer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.BoxBlur;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.TilePane;
import sp.it.pl.gui.objects.Text;
import sp.it.pl.gui.objects.icon.CheckIcon;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.pl.gui.objects.popover.PopOver;
import sp.it.pl.layout.container.BiContainer;
import sp.it.pl.layout.container.BiContainerUi;
import sp.it.pl.layout.container.FreeFormContainer;
import sp.it.pl.main.AppAnimator;
import sp.it.pl.main.Df;
import sp.it.util.access.ref.SingleR;
import sp.it.util.animation.Anim;
import sp.it.util.reactive.Subscribed;
import sp.it.util.reactive.Subscription;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.COGS;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.GAVEL;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.INFO;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.LINK;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.TIMES;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.UNLINK;
import static de.jensd.fx.glyphs.octicons.OctIcon.FOLD;
import static de.jensd.fx.glyphs.octicons.OctIcon.UNFOLD;
import static javafx.geometry.NodeOrientation.LEFT_TO_RIGHT;
import static javafx.scene.input.DragEvent.DRAG_DONE;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static javafx.scene.input.MouseEvent.MOUSE_MOVED;
import static javafx.stage.WindowEvent.WINDOW_HIDDEN;
import static sp.it.pl.layout.widget.WidgetUi.PSEUDOCLASS_DRAGGED;
import static sp.it.pl.layout.widget.Widget.LoadType.AUTOMATIC;
import static sp.it.pl.layout.widget.Widget.LoadType.MANUAL;
import static sp.it.pl.main.AppBuildersKt.helpPopOver;
import static sp.it.pl.main.AppDragKt.set;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.functional.UtilKt.runnable;
import static sp.it.util.reactive.EventsKt.onEventUp;
import static sp.it.util.reactive.UtilKt.syncC;
import static sp.it.util.ui.Util.layStack;
import static sp.it.util.ui.Util.setAnchor;
import static sp.it.util.ui.UtilKt.getCentre;
import static sp.it.util.ui.UtilKt.pseudoclass;

/**
 * Controls for a widget area.
 */
public final class WidgetUiControls {

    private static final double activatorW = 20;
    private static final double activatorH = 20;
    private static final String infobTEXT = "Help\n\n"
        + "Displays information about the widget, e.g., name, purpose or "
        + "how to use it.";
    private static final String absbTEXT = "Absolute size\n\n"
        + "Prevents widget from resizing proportionally to parent container's "
        + "size. Instead, the widget will keep the same size, if possible.";
    private static final String lockbTEXT = "Lock widget\n\n"
        + "Disallows layout mode when mouse enters top corner of the widget. \n"
        + "This can be applied separately on widgets, but also containers or "
        + "whole layout.";
    private static final String propbTEXT = "Settings\n\n"
        + "Displays widget properties.";
    private static final String actbTEXT = "Actions\n\n"
        + "Opens action chooser for this widget. Action chooser displays and "
        + "can run an action on some data, in this case this widget. Shows "
        + "non-layout operations.";
    private static final String closebTEXT = "Close widget\n\n"
        + "Closes widget and creates empty place in the container.";
    private static SingleR<PopOver<Text>,WidgetUiControls> helpP = new SingleR<>(
        () -> helpPopOver(""),
        (p, ac) -> {
            // set text
            p.contentNode.getValue().setText(ac.getInfo());
            // for some reason we need to put this every time, which
            // should not be the case, investigate
            p.contentNode.getValue().setWrappingWidth(400);
            // we need to handle hiding this WidgetUiControls when popup
            // closes and we are outside of the area (not implemented yet)
            p.addEventHandler(WINDOW_HIDDEN, we -> {
                if (ac.isShowingWeak) ac.hide();
            });
        }
    );

    private final WidgetUi area;
    public final AnchorPane root = new AnchorPane();
    public final Label title = new Label();
    public final Icon propB;
    public final TilePane header_buttons = new TilePane(4.0, 4.0);
    public final Icon infoB, absB, lockB;

    private boolean isShowingStrong = false;
    private boolean isShowingWeak = false;
    private Subscribed hiderWeak;
    private final Anim anim;

    public WidgetUiControls(WidgetUi area) {
        this.area = area;

        root.setId("widget-ui-controls");
        root.getStyleClass().add("widget-ui-controls");

        setAnchor(root, layStack(title, Pos.CENTER), 0.0);
        title.getParent().setMouseTransparent(true);

        // build header buttons
        Icon closeB = new Icon(TIMES, -1, closebTEXT, this::close).styleclass("header-icon");
        Icon actB = new Icon(GAVEL, -1, actbTEXT, () -> APP.actionPane.show(Widget.class, area.getWidget())).styleclass("header-icon");
        propB = new Icon(COGS, -1, propbTEXT, this::settings).styleclass("header-icon");
        lockB = new Icon(null, -1, lockbTEXT, () -> {
            toggleLocked();
            APP.actionStream.invoke("Widget layout lock");
        }).styleclass("header-icon");
//		maintain(area.container.locked, mapB(LOCK,UNLOCK),lockB::icon);
        absB = new Icon(LINK, -1, absbTEXT, e -> {
            toggleAbsSize();
            updateAbsB();
        }).styleclass("header-icon");
        CheckIcon loadB = new CheckIcon();
        loadB.styleclass("header-icon");
        loadB.tooltip("Switch between automatic or manual widget loading.");
        syncC(area.getWidget().loadType, it -> loadB.selected.setValue(it==AUTOMATIC));
        syncC(loadB.selected, it -> loadB.icon(it ? UNFOLD : FOLD));
        loadB.selected.addListener((o,ov,nv) -> area.getWidget().loadType.set(nv ? AUTOMATIC : MANUAL));
        // ^ technically we've got ourselves bidirectional binding and risk stackoverflow. We know
        // the value changes fire only when value is different, so we ara safe

        infoB = new Icon(INFO, -1, infobTEXT, this::showInfo).styleclass("header-icon"); // consistent with Icon.infoIcon()

        // build header
        header_buttons.setPrefRows(1);
        header_buttons.setPrefColumns(10);
        header_buttons.setNodeOrientation(LEFT_TO_RIGHT);
        header_buttons.setAlignment(Pos.CENTER_RIGHT);
        header_buttons.getChildren().addAll(infoB, loadB, absB, lockB, propB, actB, closeB);
        setAnchor(root, header_buttons, 0.0, 0.0, null, null);

        // build animations
        BoxBlur blur = new BoxBlur(0, 0, 1);
        anim = new Anim(it -> {
                root.setOpacity(it);
                root.setVisible(it!=0.0);
                root.setMouseTransparent(it!=1.0);

                blur.setWidth(it*APP.ui.getBlurLM());
                blur.setHeight(it*APP.ui.getBlurLM());
                area.getContent().setEffect(APP.ui.getBlurLayoutMode().getValue() ? blur : null);
                area.getContent().setOpacity(APP.ui.getOpacityLayoutMode().getValue() ? 1-APP.ui.getOpacityLM()*it : 1);
                area.getContent().setMouseTransparent(it==1.0);
            }).dur(APP.ui.getDurationLM());

        // weak mode and strong mode - strong mode is show/hide called from external code
        // - weak mode is show/hide by mouse enter/exit events in the corner
        // - the weak behavior must not work in strong mode
        // weak show - activator behavior
        BooleanProperty inside = new SimpleBooleanProperty(false);
        Pane p = area.contentRoot;
        var showS = (Consumer<MouseEvent>) e -> {
            // ignore when already showing, under lock or in strong mode
            if (!isShowingWeak && !area.isUnderLock() && !isShowingStrong) {
                // transform into IN/OUT boolean
                var in = p.getWidth() - activatorW*APP.ui.getFont().getValue().getSize()/12.0 < e.getX() && activatorH*APP.ui.getFont().getValue().getSize()/12.0 > e.getY();
                // ignore when no change
                if (in != inside.getValue()) {
                    inside.setValue(in);
                    if (in) showWeak();
                }
            }
        };
        p.addEventFilter(MOUSE_MOVED, showS::accept);
        p.addEventFilter(MOUSE_ENTERED, showS::accept);
        p.addEventFilter(MOUSE_EXITED, e -> inside.set(false));

        hiderWeak = new Subscribed(feature -> {
            var eh = consumer((MouseEvent e) -> {
                if (
                    isShowingWeak &&    // ignore when not showing
                    !isShowingStrong &&     // ignore in strong mode
                    (helpP.isØ() || !helpP.get().isShowing()) &&    // keep visible when popup is shown
                    !root.getPseudoClassStates().contains(PSEUDOCLASS_DRAGGED) &&   // keep visible when dragging
                    (
                        !root.localToScene(root.getLayoutBounds()).contains(e.getSceneX(), e.getSceneY()) ||
                        header_buttons.getChildren().stream().allMatch(it -> getCentre(it.localToScene(it.getLayoutBounds())).distance(e.getSceneX(), e.getSceneY())>100)
                    )
                ) {
                    hideWeak();
                }
            });
            return Subscription.Companion.invoke(
                onEventUp(root, MOUSE_MOVED, eh),
                onEventUp(root, MOUSE_EXITED, eh),
                onEventUp(p, MOUSE_EXITED, eh),
                onEventUp(root, DRAG_DONE, consumer(e -> eh.invoke(new MouseEvent(MOUSE_MOVED, e.getX(), e.getY(), e.getScreenX(), e.getScreenY(), MouseButton.NONE, 0 , false, false, false, false, false, false, false, false, false, true, null))))
            );
        });

        // dragging
        root.setOnDragDetected(e -> {
            if (e.getButton()==PRIMARY) {
                if (e.isShortcutDown()) {
                    area.detach();
                    e.consume();
                } else if (!(area.container instanceof FreeFormContainer)) {
                    Dragboard db = root.startDragAndDrop(TransferMode.ANY);
                    set(db, Df.COMPONENT, area.getWidget());
                    root.pseudoClassStateChanged(PSEUDOCLASS_DRAGGED, true);
                    e.consume();
                }
            }
        });
        root.setOnDragDone(e -> root.pseudoClassStateChanged(PSEUDOCLASS_DRAGGED, false));
    }

    void toggleLocked() {
        area.toggleLocked();
    }

    void settings() {
        APP.windowManager.showSettings(area.getWidget(), propB);
    }

    void showInfo() {
        helpP.getM(this).showInCenterOf(infoB);
    }

    void close() {
        AppAnimator.INSTANCE.closeAndDo(area.contentRoot, runnable(() -> area.container.removeChild(area.index)));
    }

    private void toggleAbsSize() {
        if (area.container instanceof BiContainer) {
            BiContainerUi s = ((BiContainer) area.container).ui;
            s.toggleAbsoluteSizeFor(area.index);
        }
    }

    public void updateAbsB() {
        if (area.container instanceof BiContainer) {
            boolean l = area.container.properties.getI("abs_size") == area.index;
            absB.icon(l ? UNLINK : LINK);
            if (!header_buttons.getChildren().contains(absB))
            header_buttons.getChildren().add(6, absB);
        } else {
            header_buttons.getChildren().remove(absB);
        }
    }

    private void showWeak() {
        isShowingWeak = true;
        hiderWeak.subscribe(true);
        anim.playOpen();

        updateAbsB();
    }

    private void hideWeak() {
        hiderWeak.subscribe(false);
        isShowingWeak = false;
        anim.playClose();

        // hide help popup if open
        if (!helpP.isØ() && helpP.get().isShowing()) helpP.get().hide();
    }

    public void show() {
        isShowingStrong = true;
        area.contentRoot.pseudoClassStateChanged(pseudoclass("layout-mode"), true);
        showWeak();
        APP.actionStream.invoke("Widget control");
    }

    public void hide() {
        isShowingStrong = false;
        area.contentRoot.pseudoClassStateChanged(pseudoclass("layout-mode"), false);
        hideWeak();
    }

    public boolean isShowing() {
        return isShowingStrong;
    }

    public boolean isShowingWeak() {
        return isShowingWeak;
    }

    public String getInfo() {
        Widget w = area.getWidget();
        return ""
            + "Controls for managing user interface (ui). UI is comprised of "
            + "widgets, containers and layouts. Widgets provide the functionality "
            + "and behavior and can be configured. Containers are invisible "
            + "boxes to lay out widgets. Containers contain widgets, but also "
            + "other containers - creating a nested hierarchy. Layouts are "
            + "containers at the top of the hierarchy.\n"
            + "\n"
            + "Available actions:\n"
            + "    Right click : Go to parent container\n"
            + "    Left click : Go to child containers/widgets\n"
            + "    Close : Closes the widget\n"
            + "    Detach : Opens the widget in new window\n"
            + "    Change : Opens widget chooser to pick new widget\n"
            + "    Settings : Opens settings for the widget if available\n"
            + "    Refresh : Refreshes the widget\n"
            + "    Lock : Forbids entering layout mode on mouse hover\n"
            + "    Press ALT : Toggles layout mode\n"
            + "    Drag & Drop header : Drags widget to other area\n"
            + (w==null ? "" : "\n\n" + w.getInfo().toStr());
    }

}