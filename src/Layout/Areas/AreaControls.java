/*
 * To change this license header, changeWidget License Headers in Project Properties.
 * To change this template file, changeWidget Tools | Templates
 * and open the template in the editor.
 */
package Layout.Areas;

import GUI.DragUtil;
import GUI.GUI;
import static GUI.GUI.OpenStrategy.INSIDE;
import static GUI.GUI.OpenStrategy.POPUP;
import static GUI.GUI.closeAndDo;
import static GUI.GUI.openAndDo;
import GUI.objects.Icon;
import GUI.objects.Pickers.WidgetPicker;
import GUI.objects.PopOver.PopOver;
import GUI.objects.SimpleConfigurator;
import GUI.objects.Text;
import static Layout.Areas.Area.draggedPSEUDOCLASS;
import Layout.BiContainer;
import Layout.Container;
import Layout.Widgets.Features.Feature;
import Layout.Widgets.Widget;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.*;
import java.util.List;
import javafx.animation.FadeTransition;
import javafx.animation.Transition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import static javafx.geometry.NodeOrientation.RIGHT_TO_LEFT;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.BoxBlur;
import javafx.scene.input.Dragboard;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import javafx.scene.input.MouseEvent;
import static javafx.scene.input.MouseEvent.*;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import static javafx.stage.WindowEvent.WINDOW_HIDDEN;
import static javafx.util.Duration.millis;
import main.App;
import org.reactfx.EventSource;
import util.Animation.Anim;
import util.SingleInstance;
import static util.Util.setAnchors;
import static util.functional.Util.mapB;
import static util.functional.Util.toS;
import util.graphics.fxml.ConventionFxmlLoader;
import static util.reactive.Util.maintain;

/**
 FXML Controller class
 <p>
 @author Plutonium_
 */
public final class AreaControls {

    private static final double activatorW = 20;
    private static final double activatorH = 20;

    // we only need one instance for all areas
    private static final SingleInstance<PopOver<Text>, AreaControls> helpP = new SingleInstance<>(
	() -> PopOver.createHelpPopOver(""),
	(p, ac) -> {
            // set text
	    p.getContentNode().setText(ac.getWindgetInfo());
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
    Icon infoB, absB;

    // animations // dont initialize here or make final
    private final FadeTransition contrAnim;
    private final FadeTransition contAnim;
    private final Transition blurAnim;

    Area area;

    public AreaControls(Area area) {
	this.area = area;
        
        // load fxml part
        new ConventionFxmlLoader(AreaControls.class, root, this).loadNoEx();

	// avoid clashing of title and control buttons for small root size
	header_buttons.maxWidthProperty()
	    .bind(root.widthProperty().subtract(title.widthProperty())
		.divide(2).subtract(15));
	header_buttons.setMinWidth(15);
	header_buttons.setHgap(8);
	header_buttons.setVgap(8);

	// build header buttons
	infoB = new Icon(INFO, 12, "Help", this::showInfo);
	Icon closeB = new Icon(TIMES, 12, "Close widget", e -> {
	    close();
	    App.actionStream.push("Close widget");
	});
	Icon detachB = new Icon(EXTERNAL_LINK_SQUARE, 12, "Detach widget to own window", this::detach);
	Icon changeB = new Icon(TH_LARGE, 12, "Change widget", this::changeWidget);
	propB = new Icon(COGS, 12, "Settings", this::settings);
	Icon lockB = new Icon(null, 12, "Lock widget layout");
        maintain(area.container.locked, mapB(LOCK,UNLOCK),lockB.icon);
	lockB.setOnMouseClicked(e -> {
	    toggleLocked();
	    App.actionStream.push("Widget layout lock");
	});
	Icon refreshB = new Icon(REFRESH, 12, "Refresh widget", this::refreshWidget);
	absB = new Icon(LINK, 12, "Resize widget proportionally", e -> {
	    toggleAbsSize();
	    updateAbsB();
	});
        Icon dragB = new Icon(MAIL_REPLY, 12, "Move widget by dragging");
        dragB.setOnDragDetected( e -> {
            if (e.getButton()==PRIMARY) {   // primary button drag only
                Dragboard db = root.startDragAndDrop(TransferMode.ANY);
                DragUtil.setComponent(area.container,area.getActiveWidget(),db);
                // signal dragging graphically with css
                area.getContent().pseudoClassStateChanged(draggedPSEUDOCLASS, true);
                e.consume();
            }
        });
        root.setOnDragDetected( e -> {
            if (GUI.isLayoutMode() && e.getButton()==PRIMARY) {   // primary button drag only
                Dragboard db = root.startDragAndDrop(TransferMode.ANY);
                DragUtil.setComponent(area.container,area.getActiveWidget(),db);
                // signal dragging graphically with css
                area.getContent().pseudoClassStateChanged(draggedPSEUDOCLASS, true);
                e.consume();
            }
        });
        
	// build header
	header_buttons.setNodeOrientation(RIGHT_TO_LEFT);
	header_buttons.setAlignment(Pos.CENTER_LEFT);
	header_buttons.getChildren().addAll(closeB, detachB, changeB, propB, refreshB, lockB, absB, dragB, infoB);

	// build animations
	contrAnim = new FadeTransition(millis(GUI.duration_LM), root);
	contAnim = new FadeTransition(millis(GUI.duration_LM), area.getContent());
	BoxBlur blur = new BoxBlur(0, 0, 1);
	area.getContent().setEffect(blur);
	blurAnim = new Anim(at -> {
            blur.setWidth(at*GUI.blur_LM);
            blur.setHeight(at*GUI.blur_LM);
        }).dur(GUI.duration_LM);

        // weak mode and strong mode - strong mode is show/hide called from external code
	// - weak mode is show/hide by mouse enter/exit events in the corner (activator/deactivator)
	// - the weak behavior must not work in strong mode
	// weak show - activator behavior
	BooleanProperty inside = new SimpleBooleanProperty(false);
	// monitor mouse movement (as filter)
	EventSource<MouseEvent> showS = new EventSource();
        Pane p = area instanceof WidgetArea ? ((WidgetArea)area).content_padding : area.root;
	p.addEventFilter(MOUSE_MOVED, showS::push);
	p.addEventFilter(MOUSE_ENTERED, showS::push);
	p.addEventFilter(MOUSE_EXITED, e -> inside.set(false));
        // and check activator.mouse_enter events
	// ignore when already showing, under lock or in strong mode
	showS.filter(e -> !isShowingWeak && !area.isUnderLock() && !isShowingStrong)
	    // transform into IN/OUT boolean
	    .map(e -> root.getWidth() - activatorW < e.getX() && activatorH > e.getY())
	    // ignore when no change
	    .filter(in -> in != inside.get())
	    // or store new state on change
	    .hook(inside::set)
	    // ignore when not inside
	    .filter(in -> in)
	    // activate weak layout mode
	    .subscribe(activating -> showWeak());

	// weak hide - deactivator behavior
	EventSource<MouseEvent> hideS = new EventSource();
	deactivator.addEventFilter(MOUSE_EXITED, hideS::push);
	deactivator2.addEventFilter(MOUSE_EXITED, hideS::push);
	p.addEventFilter(MOUSE_EXITED, hideS::push);

        // hide when no longer hovered and in weak mode
	// ignore when alreadt not showing, under lock or in strong mode
	hideS.filter(e -> isShowingWeak && !area.isUnderLock() && !isShowingStrong)
	    // but not when helpPoOver is visible
	    // mouse entering the popup qualifies as root.mouseExited which we need
	    // to avoid (now we need to handle hiding when popup closes)
	    .filter(e -> helpP.isNull() || !helpP.get().isShowing())
	    // only when the deactivators are !'hovered' 
	    // (Node.isHover() !work here) & we need to transform coords into scene-relative
	    .filter(e -> !deactivator.localToScene(deactivator.getBoundsInLocal()).contains(e.getSceneX(), e.getSceneY())
		&& !deactivator2.localToScene(deactivator2.getBoundsInLocal()).contains(e.getSceneX(), e.getSceneY()))
	    .subscribe(e -> hideWeak());

        // hide on mouse exit from area
	// sometimes mouse exited deactivator does not fire in fast movement
	// same thing as above - need to take care of popup...
	p.addEventFilter(MOUSE_EXITED, e -> {
	    if (isShowingWeak && !isShowingStrong && (helpP.isNull() || !helpP.get().isShowing()))
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
        
        if(GUI.open_strategy==POPUP) {
            Widget w = (Widget) area.getActiveWidgets().get(0);
            SimpleConfigurator sc = new SimpleConfigurator(w);
            PopOver p = new PopOver(sc);
                    p.title.set(w.getName() + " Settings");
                    p.setArrowSize(0); // autofix breaks the arrow position, turn off - sux
                    p.setAutoFix(true); // we need autofix here, because the popup can get rather big
                    p.setAutoHide(true);
                    p.show(propB);
            sc.onOK = c -> p.hide();
        } else 
        if (GUI.open_strategy==INSIDE) {
            closeAndDo(area.content_root, () -> {
                Widget w = (Widget) area.getActiveWidgets().get(0);
                SimpleConfigurator sc = new SimpleConfigurator(w);
                sc.getStyleClass().addAll("block", "area", "widget-area");// imitate area looks
                sc.setOnMouseClicked(me->{ if(me.getButton()==SECONDARY) sc.ok(); });
                sc.setOkButtonVisible(false);
                sc.onOK = c -> closeAndDo(sc, () -> openAndDo(area.content_root, null));
                area.root.getChildren().add(sc);
                setAnchors(sc, 0);
                openAndDo(sc, null);
            });
        }
    }

    void changeWidget() {
        if(GUI.open_strategy==POPUP) {
            WidgetPicker w = new WidgetPicker();
            PopOver p = new PopOver(w.getNode());
                    p.title.set("Change widget");
                    p.setArrowSize(0); // autofix breaks the arrow position, turn off - sux
                    p.setAutoFix(true); // we need autofix here, because the popup can get rather big
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
        if (GUI.open_strategy==INSIDE) {
            closeAndDo(area.content_root, () -> {
                WidgetPicker w = new WidgetPicker();
                w.onCancel = () -> {
                    closeAndDo(w.root, () -> {
                        area.root.getChildren().remove(w.root);
                        Layouter l = new Layouter(area.container, area.index);
                        area.root.getChildren().add(l.root);
                        setAnchors(l.root, 0);
//                        openAndDo(l.getRoot(), null);
                        l.show();
                        l.cp.onCancel = () -> {
                            closeAndDo(l.root, () -> {
                                area.root.getChildren().remove(l.root);
                                openAndDo(area.content_root,null);
                            });
                        
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
                setAnchors(w.getNode(), 0);

                openAndDo(w.getNode(), null);
            });
        }
    }
    
    void showInfo() {
        if(GUI.open_strategy==POPUP) {
            helpP.get(this).show(infoB);
        } else 
        if (GUI.open_strategy==INSIDE) {
            closeAndDo(area.content_root, ()->{
                Text t = new Text(getWindgetInfo());
                     t.setMouseTransparent(true);
                ScrollPane s = new ScrollPane(t);
                s.setPadding(new Insets(15));
                s.getStyleClass().addAll(Area.bgr_STYLECLASS);
                s.addEventFilter(MOUSE_PRESSED, Event::consume);
                s.addEventFilter(MOUSE_RELEASED, Event::consume);
                s.addEventFilter(MOUSE_CLICKED, e -> {
                    if(e.getButton()==SECONDARY)
                        closeAndDo(s, () -> openAndDo(area.content_root, null));
                });
                area.root.getChildren().add(s);
                setAnchors(s, 0);
                openAndDo(s, null);
            });
        }
        App.actionStream.push("Widget info");
    }
    
    void detach() {
	area.detach();
    }

    void close() {
        // if area belongs to the container, close container
        if (area.index==null) 
            closeAndDo(area.container.getGraphics().getRoot(), area.container::close);
        // if area belongs to the child, close child only
        else 
            closeAndDo(area.content_root, () -> area.container.removeChild(area.index));
    }

    private void toggleAbsSize() {
	Container c = area.container.getParent();
	if (c != null && c instanceof BiContainer) {
	    Splitter s = BiContainer.class.cast(c).getGraphics();
	    s.toggleAbsoluteSizeFor(area.container.indexInParent());
	}
    }

    void updateAbsB() {
	Container c = area.container.getParent();
	if (c != null && c instanceof BiContainer) {
	    boolean l = c.properties.getI("abs_size") == area.container.indexInParent();
            absB.icon.setValue(l ? UNLINK : LINK);
	    if (!header_buttons.getChildren().contains(absB))
		header_buttons.getChildren().add(6, absB);
	} else
	    header_buttons.getChildren().remove(absB);
    }

    private void showWeak() {
        Node n = area.getActiveWidget().getController().getActivityNode();
        if(n!=null && !root.getChildren().contains(n)) {
            root.getChildren().add(n);
            setAnchors(n, 0);
            n.toBack();
        }
        //set state
	isShowingWeak = true;
	// stop animations if active
	contrAnim.stop();
	contAnim.stop();
	blurAnim.stop();
	// put new values
	contrAnim.setToValue(1);
	contAnim.setToValue(1 - GUI.opacity_LM);
	blurAnim.setRate(1);
	// play
	contrAnim.play();
	if (GUI.opacity_layoutMode) contAnim.play();
	if (GUI.blur_layoutMode) blurAnim.play();
	// handle graphics
	area.getContent().setMouseTransparent(true);
	root.setMouseTransparent(false);
	updateAbsB();
    }

    private void hideWeak() {
        Node n = area.getActiveWidget().getController().getActivityNode();
        if(n!=null) root.getChildren().remove(n);
	isShowingWeak = false;
	contrAnim.stop();
	contAnim.stop();
	blurAnim.stop();
	contrAnim.setToValue(0);
	contAnim.setToValue(1);
	blurAnim.setRate(-1);
	contrAnim.play();
	if (GUI.opacity_layoutMode) contAnim.play();
	if (GUI.blur_layoutMode) blurAnim.play();
	area.getContent().setMouseTransparent(false);
	root.setMouseTransparent(true);
	// hide help popup if open
	if (!helpP.isNull() && helpP.get().isShowing()) helpP.get().hide();
    }

    public void show() {
	//set state
	isShowingStrong = true;
	showWeak();
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

    public String getWindgetInfo() {
        Widget w = area.getActiveWidget();
        String info = "";
        if (w != null) {
            info += "\n\nWidget: " + w.name();
            if (!w.description().isEmpty())
                info += "\n\n" + w.description();
            if (!w.notes().isEmpty()) info += "\n\n" + w.notes();
            if (!w.howto().isEmpty()) info += "\n\n" + w.howto();
            
            List<Feature> ff = w.getController().getFeatures();
            String s = ff.isEmpty() ? "none" : "\n" + toS(ff, f -> "    " + f.name() + ": " + f.description() + "\n");
            info += "\n\nFeatures: " + s;
        }

        return "Available actions:\n"
             + "    Close : Closes the widget\n"
             + "    Detach : Opens the widget in new window\n"
             + "    Change : Opens widget chooser to pick new widget\n"
             + "    Settings : Opens settings for the widget if available\n"
             + "    Refresh : Refreshes the widget\n"
             + "    Lock : Forbids entering layout mode on mouse hover\n"
             + "    Press ALT : Toggles layout mode\n"
             + "    Drag & Drop header : Drags widget to other area\n"
             + "\n"
             + "Available actions in layout mode:\n"
             + "    Drag & Drop : Drags widget to other area\n"
             + "    Sroll : Changes widget area size\n"
             + "    Middle click : Set widget area size to max\n"
             + info;
    }
    
}