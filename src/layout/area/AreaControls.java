/*
 * To change this license header, changeWidget License Headers in Project Properties.
 * To change this template file, changeWidget Tools | Templates
 * and open the template in the editor.
 */
package layout.area;

import java.util.function.Consumer;

import javafx.animation.FadeTransition;
import javafx.animation.Transition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.BoxBlur;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;

import org.reactfx.EventSource;

import gui.Gui;
import gui.objects.Text;
import gui.objects.icon.CheckIcon;
import gui.objects.icon.Icon;
import gui.objects.picker.WidgetPicker;
import gui.objects.popover.PopOver;
import layout.container.bicontainer.BiContainer;
import layout.widget.Widget;
import unused.SimpleConfigurator;
import util.SingleR;
import util.animation.Anim;
import util.graphics.drag.DragUtil;
import util.graphics.fxml.ConventionFxmlLoader;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;
import static de.jensd.fx.glyphs.octicons.OctIcon.FOLD;
import static de.jensd.fx.glyphs.octicons.OctIcon.UNFOLD;
import static gui.Gui.OpenStrategy.INSIDE;
import static gui.Gui.OpenStrategy.POPUP;
import static gui.Gui.closeAndDo;
import static gui.Gui.openAndDo;
import static javafx.geometry.NodeOrientation.LEFT_TO_RIGHT;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.*;
import static javafx.scene.input.ScrollEvent.SCROLL;
import static javafx.stage.WindowEvent.WINDOW_HIDDEN;
import static layout.area.Area.DRAGGED_PSEUDOCLASS;
import static layout.widget.Widget.LoadType.AUTOMATIC;
import static layout.widget.Widget.LoadType.MANUAL;
import static main.App.APP;
import static main.AppBuildersKt.helpPopOver;
import static util.functional.Util.mapB;
import static util.graphics.Util.layScrollVText;
import static util.graphics.Util.setAnchors;
import static util.reactive.Util.maintain;

/**
 * Controls for a widget area.
 */
public final class AreaControls {

    private static final double activatorW = 20;
    private static final double activatorH = 20;
    private static final String infobTEXT = "Help\n\n"
        + "Displays information about the widget, e.g., name, purpose or "
        + "how to use it.";
    private static final String dragbTEXT = "Drag widget\n\n"
        + "Drag the button with mouse to move and drop on different widget "
        + "to change their position in the layout.";
    private static final String absbTEXT = "Absolute size\n\n"
        + "Prevents widget from resizing proportionally to parent container's "
        + "size. Instead, the widget will keep the same size, if possible.";
    private static final String lockbTEXT = "Lock widget\n\n"
        + "Disallows layout mode when mouse enters top corner of the widget. \n"
        + "This can be applied separately on widgets, but also containers or "
        + "whole layout.";
    private static final String refbTEXT = "Refresh widget\n\n"
        + "Applies widget properties, layout or reloads widget content. Depends "
        + "on widget.";
    private static final String propbTEXT = "Settings\n\n"
        + "Displays widget properties.";
    private static final String actbTEXT = "Actions\n\n"
        + "Opens action chooser for this widget. Action chooser displays and "
        + "can run an action on some data, in this case this widget. Shows "
        + "non-layout operations.";
    private static final String detachbTEXT = "Detach widget\n\n"
        + "Moves widget out of the container and puts it to new layout in a new "
        + "window";
    private static final String changebTEXT = "Change widget\n\n"
        + "Choose new widget or container. If any is selected, old widget is "
        + "closed.";
    private static final String closebTEXT = "Close widget\n\n"
        + "Closes widget and creates empty place in the container.";
    private static SingleR<PopOver<Text>, AreaControls> helpP = new SingleR<>(
	() -> helpPopOver(""),
	(p, ac) -> {
            // set text
	    p.getContentNode().setText(ac.getInfo());
            // for some reason we need to put this every time, which
	    // should not be the case, investigate
	    p.getContentNode().setWrappingWidth(400);
            // we need to handle hiding this AreaControls when popup
	    // closes and we are outside of the area (not implemented yet)
	    p.addEventHandler(WINDOW_HIDDEN, we -> {
			if (ac.isShowingWeak) ac.hide();
	    });
	});


    @FXML public AnchorPane root = new AnchorPane();
    @FXML public Region deactivator;
    @FXML public Region deactivator2;
    @FXML public BorderPane header;
    @FXML public Label title;
    public Icon propB;
    public @FXML TilePane header_buttons;
    Icon infoB, absB, lockB;

    // animations // dont setParentRec here or make final
    private final FadeTransition contrAnim;
    private final FadeTransition contAnim;
    private final Transition blurAnim;

    WidgetArea area;

    public AreaControls(WidgetArea area) {
	this.area = area;

        // load fxml
        new ConventionFxmlLoader(AreaControls.class, root, this).loadNoEx();

        root.getStyleClass().add(Area.WIDGET_AREA_CONTROLS_STYLECLASS);

		// avoid clashing of title and control buttons for small root size
		header_buttons.maxWidthProperty()
		    .bind(root.widthProperty().subtract(title.widthProperty())
			.divide(2).subtract(15));
		header_buttons.setMinWidth(15);
		header_buttons.setHgap(8);
		header_buttons.setVgap(8);

		// build header buttons
		Icon closeB = new Icon(TIMES, 12, closebTEXT, this::close);
		Icon changeB = new Icon(TH_LARGE, 12, changebTEXT, this::changeWidget);
		Icon detachB = new Icon(CLONE, 12, detachbTEXT, area::detach);
		Icon actB = new Icon(GAVEL, 12, actbTEXT, () -> APP.actionPane.show(Widget.class, area.getWidget()));
		propB = new Icon(COGS, 12, propbTEXT, this::settings);
		Icon refreshB = new Icon(REFRESH, 12, refbTEXT, this::refreshWidget);
		lockB = new Icon(null, 12, lockbTEXT, () -> {
	        toggleLocked();
		    APP.actionStream.push("Widget layout lock");
        });
//		maintain(area.container.locked, mapB(LOCK,UNLOCK),lockB::icon);
		absB = new Icon(LINK, 12, absbTEXT, e -> {
		    toggleAbsSize();
		    updateAbsB();
		});
        Icon dragB = new Icon(MAIL_REPLY, 12, dragbTEXT);
        CheckIcon loadB = new CheckIcon();
        maintain(area.getWidget().loadType, lt -> lt==AUTOMATIC, loadB.selected);
        maintain(loadB.selected, mapB(UNFOLD,FOLD), loadB::icon);
        loadB.selected.addListener((o,ov,nv) -> area.getWidget().loadType.set(nv ? AUTOMATIC : MANUAL));
        // ^ technically we've got ourselves bidirectional binding and risk stackoverflow. We know
        // the value changes fire only when value is different, so we ara safe

        // dragging
        EventHandler<MouseEvent> dh = e -> {
            if (e.getButton()==PRIMARY) {   // primary button drag only
                Dragboard db = root.startDragAndDrop(TransferMode.ANY);
                DragUtil.setComponent(area.getWidget(),db);
                // signal dragging graphically with css
                root.pseudoClassStateChanged(DRAGGED_PSEUDOCLASS, true);
                e.consume();
            }
        };
        dragB.setOnDragDetected(dh);
        root.setOnDragDetected(dh);
        // return graphics to normal
        root.setOnDragDone(e -> root.pseudoClassStateChanged(DRAGGED_PSEUDOCLASS, false));


		infoB = new Icon(INFO, 12, infobTEXT, this::showInfo); // consistent with Icon.createInfoIcon()

		// build header
		header_buttons.setNodeOrientation(LEFT_TO_RIGHT);
		header_buttons.setAlignment(Pos.CENTER_RIGHT);
		header_buttons.getChildren().addAll(infoB, loadB, dragB, absB, lockB, refreshB, propB, actB, detachB, changeB, closeB);

		// build animations
		contrAnim = new FadeTransition(Gui.duration_LM, root);
		contAnim = new FadeTransition(Gui.duration_LM, area.getContent());
		BoxBlur blur = new BoxBlur(0, 0, 1);
		area.getContent().setEffect(blur);
		blurAnim = new Anim(at -> {
	            blur.setWidth(at* Gui.blur_LM);
	            blur.setHeight(at* Gui.blur_LM);
	        }).dur(Gui.duration_LM);

	        // weak mode and strong mode - strong mode is show/hide called from external code
		// - weak mode is show/hide by mouse enter/exit events in the corner (activator/deactivator)
		// - the weak behavior must not work in strong mode
		// weak show - activator behavior
		BooleanProperty inside = new SimpleBooleanProperty(false);
		// monitor mouse movement (as filter)
		EventSource<MouseEvent> showS = new EventSource<>();
	        Pane p = area.content_padding;
		p.addEventFilter(MOUSE_MOVED, showS::push);
		p.addEventFilter(MOUSE_ENTERED, showS::push);
		p.addEventFilter(MOUSE_EXITED, e -> inside.set(false));
	        // and check activator.mouse_enter events
		// ignore when already showing, under lock or in strong mode
		showS.filter(e -> !isShowingWeak && !area.isUnderLock() && !isShowingStrong)
		    // transform into IN/OUT boolean
		    .map(e -> p.getWidth() - activatorW < e.getX() && activatorH > e.getY())
		    // ignore when no change
		    .filter(in -> in != inside.get())
		    // or store new state on change
		    .hook(inside::set)
		    // ignore when not inside
		    .filter(in -> in)
		    // activate weak layout mode
		    .subscribe(activating -> showWeak());

		// weak hide - deactivator behavior
	        Consumer<MouseEvent> hideWeakTry = e -> {
	            if (
	                // ignore when already not showing or in strong mode
	                isShowingWeak && !isShowingStrong &&
	                // mouse entering the popup qualifies as root.mouseExited which we need
	                // to avoid (now we need to handle hiding when popup closes)
	                (helpP.isØ() || !helpP.get().isShowing()) &&
	                // only when the deactivators are !'hovered'
	                // (Node.isHover() !work here) & we need to transform coords into scene-relative
	                !deactivator.localToScene(deactivator.getBoundsInLocal()).contains(e.getSceneX(), e.getSceneY()) &&
	                !deactivator2.localToScene(deactivator2.getBoundsInLocal()).contains(e.getSceneX(), e.getSceneY())
	            ) {
	                hideWeak();
	            }
	        };
		deactivator.addEventFilter(MOUSE_EXITED, hideWeakTry::accept);
		deactivator2.addEventFilter(MOUSE_EXITED, hideWeakTry::accept);
		header_buttons.addEventFilter(MOUSE_EXITED, hideWeakTry::accept);
		p.addEventFilter(MOUSE_EXITED, hideWeakTry::accept);

	        // hide on mouse exit from area
		// sometimes mouse exited deactivator does not fire in fast movement
		// same thing as above - need to take care of popup...
		p.addEventFilter(MOUSE_EXITED, e -> {
		    if (isShowingWeak && !isShowingStrong && (helpP.isØ() || !helpP.get().isShowing()))
			hide();
		});

	        // enlarge deactivator that resizes with control buttons to give more
		// room for mouse movement
		deactivator.setScaleX(1.2);
		deactivator.setScaleY(1.2);
    }

    void refreshWidget() {
	area.refresh();
    }

    void toggleLocked() {
	area.toggleLocked();
    }

    void settings() {
		if (area.getActiveWidgets().isEmpty()) return;
        Widget w = area.getActiveWidgets().get(0);

        if (Gui.open_strategy==POPUP) {
	        APP.windowManager.showSettings(w,propB);
        } else
        if (Gui.open_strategy==INSIDE) {
            closeAndDo(area.content_root, () -> {
	            // TODO: decide whether we use SimpleConfigurator or Configurator widget
                // Configurator sc = new Configurator(true);
                //              sc.configure(w);
	            SimpleConfigurator sc = new SimpleConfigurator(w);

	            sc.getStyleClass().addAll("block", "area", "widget-area");// imitate area looks
	            sc.setOnMouseClicked(me->{ if (me.getButton()==SECONDARY) closeAndDo(sc, () -> openAndDo(area.content_root, null)); });
	            area.root.getChildren().add(sc);
	            setAnchors(sc, 0d);
	            openAndDo(sc, null);
            });
        }
    }

    void changeWidget() {
        if (Gui.open_strategy==POPUP) {
            WidgetPicker w = new WidgetPicker();
            PopOver p = new PopOver(w.getNode());
                    p.title.set("Change widget");
                    p.setArrowSize(0); // autofix breaks the arrow position, turn off - sux
                    p.setAutoFix(true); // we need autofix here, because the popup can getM rather big
                    p.setAutoHide(true);
                    p.show(propB);
            w.onCancel = p::hide;
            w.onSelect = factory -> {
                closeAndDo(w.getNode(), () -> {
                    area.root.getChildren().remove(w.getNode());
                    // load widget
                    area.add(factory.create());

                    openAndDo(area.content_root,null);
                });
            };
        } else
        if (Gui.open_strategy==INSIDE) {
            closeAndDo(area.content_root, () -> {
                WidgetPicker w = new WidgetPicker();
                w.onCancel = () -> {
                    closeAndDo(w.root, () -> {
                        area.root.getChildren().remove(w.root);
                        Layouter l = new Layouter(area.container, area.index);
                        area.root.getChildren().add(l.root);
                        setAnchors(l.root, 0d);
//                        openAndDo(l.getRoot(), null);
                        l.show();
                        l.onCpCancel = () -> {
                            area.root.getChildren().remove(l.root);
                            openAndDo(area.content_root,null);
                        };
                    });
                };
                w.onSelect = factory -> {
                    closeAndDo(w.root, () -> {
                        area.root.getChildren().remove(w.root);
                        // load widget
                        area.add(factory.create());

//                        openAndDo(area.content_root,null);
                    });
                };
                area.root.getChildren().add(w.getNode());
                setAnchors(w.getNode(), 0d);

                openAndDo(w.getNode(), null);
            });
        }
    }

    void showInfo() {
        if (Gui.open_strategy==POPUP) {
            helpP.getM(this).show(infoB);
        } else
        if (Gui.open_strategy==INSIDE) {
            closeAndDo(area.content_root, ()->{
                Text t = new Text(getInfo());
                     t.setMouseTransparent(true);
                ScrollPane s = layScrollVText(t);
                           s .addEventFilter(SCROLL, Event::consume);
                           s.setMaxWidth(500);
                StackPane sa = new StackPane(s);
                sa.setPadding(new Insets(20));
                sa.getStyleClass().addAll(Area.bgr_STYLECLASS);
                sa.addEventFilter(MOUSE_PRESSED, Event::consume);
                sa.addEventFilter(MOUSE_RELEASED, Event::consume);
                sa.addEventFilter(MOUSE_CLICKED, e -> {
                    if (e.getButton()==SECONDARY)
                        closeAndDo(sa, () -> openAndDo(area.content_root, null));
                });
                area.root.getChildren().add(sa);
                setAnchors(sa, 0d);
                openAndDo(sa, null);
            });
        }
    }

    void close() {
        // if area belongs to the container, setØ container
        if (area.index==null)
            closeAndDo(area.container.ui.getRoot(), () -> area.container.close());
        // if area belongs to the child, setØ child only
        else
            closeAndDo(area.content_root, () -> area.container.removeChild(area.index));
    }

    private void toggleAbsSize() {
		if (area.container instanceof BiContainer) {
		    Splitter s = BiContainer.class.cast(area.container).ui;
		    s.toggleAbsoluteSizeFor(area.index);
		}
    }

    void updateAbsB() {
	if (area.container instanceof BiContainer) {
	    boolean l = area.container.properties.getI("abs_size") == area.index;
            absB.icon(l ? UNLINK : LINK);
	    if (!header_buttons.getChildren().contains(absB))
		header_buttons.getChildren().add(6, absB);
	} else
	    header_buttons.getChildren().remove(absB);
    }

    private void showWeak() {
        //set state
	isShowingWeak = true;
	// stop animations if active
	contrAnim.stop();
	contAnim.stop();
	blurAnim.stop();
	// put new values
	contrAnim.setToValue(1);
	contAnim.setToValue(1 - Gui.opacity_LM);
	blurAnim.setRate(1);
	// play
	contrAnim.play();
	if (Gui.opacity_layoutMode) contAnim.play();
	if (Gui.blur_layoutMode) blurAnim.play();
	// handle graphics
	area.getContent().setMouseTransparent(true);
	root.setMouseTransparent(false);
	updateAbsB();
    }

    private void hideWeak() {
	isShowingWeak = false;
	contrAnim.stop();
	contAnim.stop();
	blurAnim.stop();
	contrAnim.setToValue(0);
	contAnim.setToValue(1);
	blurAnim.setRate(-1);
	contrAnim.play();
	if (Gui.opacity_layoutMode) contAnim.play();
	if (Gui.blur_layoutMode) blurAnim.play();
	area.getContent().setMouseTransparent(false);
	root.setMouseTransparent(true);
	// hide help popup if open
	if (!helpP.isØ() && helpP.get().isShowing()) helpP.get().hide();
    }

    public void show() {
	//set state
	isShowingStrong = true;
	showWeak();
        APP.actionStream.push("Widget control");
    }

    public void hide() {
	//set state
	isShowingStrong = false;
	hideWeak();
    }

    private boolean isShowingStrong = false;
    private boolean isShowingWeak = false;

    public boolean isShowing() {
	return isShowingStrong;
    }

    public boolean isShowingWeak() {
	return isShowingWeak;
    }


/******************************************************************************/

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