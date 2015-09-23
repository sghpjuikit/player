package gui.objects.Window.stage;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.glass.ui.Robot;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.StreamException;

import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.services.lasfm.LastFM;
import Configuration.*;
import Layout.Component;
import Layout.container.layout.Layout;
import Layout.container.switchcontainer.SwitchContainer;
import Layout.container.switchcontainer.SwitchPane;
import action.Action;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import gui.GUI;
import gui.objects.PopOver.PopOver;
import gui.objects.Window.Resize;
import gui.objects.icon.Icon;
import gui.objects.spinner.Spinner;
import gui.pane.IOPane;
import main.App;
import util.access.Ѵ;
import util.animation.Anim;
import util.animation.interpolator.ElasticInterpolator;
import util.async.executor.FxTimer;
import util.dev.TODO;
import util.graphics.drag.DragUtil;
import util.graphics.fxml.ConventionFxmlLoader;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.FULLSCREEN;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.FULLSCREEN_EXIT;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.WINDOW_MAXIMIZE;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.WINDOW_MINIMIZE;
import static gui.objects.Window.Resize.*;
import static gui.objects.icon.Icon.createInfoIcon;
import static java.lang.Math.*;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.*;
import static javafx.scene.paint.Color.BLACK;
import static javafx.stage.StageStyle.UNDECORATED;
import static javafx.stage.WindowEvent.WINDOW_SHOWN;
import static main.App.APP;
import static util.animation.Anim.par;
import static util.dev.TODO.Purpose.BUG;
import static util.dev.Util.no;
import static util.dev.Util.yes;
import static util.functional.Util.find;
import static util.functional.Util.forEachIRStream;
import static util.functional.Util.forEachIStream;
import static util.functional.Util.list;
import static util.functional.Util.mapB;
import static util.graphics.Util.*;
import static util.reactive.Util.maintain;

/**
 Window for application.
 <p>
 Window with basic functionality.
 <p>
 Below is a code example creating and configuring custom window instance:
 <pre>
     Window w = Window.create();
            w.setIsPopup(true);
            w.getStage().initOwner(App.getWindow().getStage());
            w.setTitle(title);
            w.setContent(content);
            w.show();
            w.setLocationCenter();
 </pre>
 <p>
 @author plutonium
 */
@IsConfigurable
public class Window extends WindowBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(Window.class);
    public static final List<Window> WINDOWS = new ArrayList<>();
    /** Psududoclass active when this window is focused. Applied on root as '.window'. */
    public static final PseudoClass pcFocused = PseudoClass.getPseudoClass("focused");
    /** Psududoclass active when this window is resized. Applied on root as '.window'. */
    public static final PseudoClass pcResized = PseudoClass.getPseudoClass("resized");
    /** Psududoclass active when this window is moved. Applied on root as '.window'. */
    public static final PseudoClass pcMoved = PseudoClass.getPseudoClass("moved");
    /** Psududoclass active when this window is fullscreen. Applied on root as '.window'. */
    public static final PseudoClass pcFullscreen = PseudoClass.getPseudoClass("fullscreen");

    /**
     Get focused window. There is zero or one focused window in the application
     at any given time.
     <p>
     @see #getActive()
     @return focused window or null if none focused.
     */
    public static Window getFocused() {
	return find(WINDOWS, w->w.focused.get()).orElse(null);
    }

    /**
     Same as {@link #getFocused()} but when none focused returns main window
     instead of null.
     <p>
     Both methods are equivalent except for when the application itself has no
     focus - which is when no window has focus.
     <p>
     Use when null must absolutely be avoided and the main window substitute
     for focused window will not break expected behavior and when this method
     can get called when app has no focus (such as through global shortcut).
     <p>
     @return focused window or main window if none. Never null.
     */
    public static Window getActive() {
	return find(WINDOWS, w->w.focused.get()).orElse(App.getWindow());
    }

    private static double mouse_speed = 0;
    private static double mouse_x = 0;
    private static double mouse_y = 0;
    private static final Robot robot = com.sun.glass.ui.Application.GetApplication().createRobot();
    private static FxTimer mouse_pulse = new FxTimer(100, -1, () -> {
        double x = robot.getMouseX();
        double y = robot.getMouseY();
        mouse_speed = sqrt(pow(mouse_x-x,2)+pow(mouse_y-y,2));
        // System.out.println(mouse_speed);
        mouse_x = x;
        mouse_y = y;
    });

    static { mouse_pulse.start();}

/******************************** Configs *************************************/

    @IsConfig(name = "Opacity", info = "Window opacity.", min = 0, max = 1)
    public static final Ѵ<Double> windowOpacity = new Ѵ<>(1d, v -> WINDOWS.forEach(w -> w.getStage().setOpacity(v)));

    @IsConfig(name = "Borderless", info = "Hides borders.")
    public static final Ѵ<Boolean> window_borderless = new Ѵ<>(false, v -> WINDOWS.forEach(w -> w.setBorderless(v)));

    @IsConfig(name = "Headerless", info = "Hides header.")
    public static final Ѵ<Boolean> window_headerless = new Ѵ<>(false, v -> WINDOWS.forEach(w -> w.setHeaderVisible(!v)));

/******************************************************************************/

    /**
     @return new window or null if error occurs during initialization.
     */
    public static Window create() {
        Window w = new Window();

        w.getStage().initOwner(App.getWindowOwner().getStage());
        // load fxml part
        new ConventionFxmlLoader(Window.class, w.root, w).loadNoEx();

//        System.out.println(windows.);
        if(WINDOWS.isEmpty()) w.setAsMain();
        // add to list of active windows
        WINDOWS.add(w);

        w.initialize();
        return w;
    }

    public static Window createWindowOwner() {
	Window w = new Window();
//               w.getStage().initStyle(WindowManager.show_taskbar_icon ? TRANSPARENT : UTILITY);
               w.getStage().initStyle(UNDECORATED);
               w.s.setOpacity(0);
               w.s.setScene(new Scene(new Region()));
               ((Region)w.s.getScene().getRoot()).setBackground(null);
               w.s.getScene().setFill(null);
               w.setSize(20, 20);
	return w;
    }

    /**
     ***************************************************************************
     */

    boolean main = false;

    // root is assigned '.window' styleclass
    @FXML public AnchorPane root = new AnchorPane();
    @FXML public AnchorPane back;
    @FXML public AnchorPane front;
    @FXML public AnchorPane borders;
    @FXML public AnchorPane content;
    @FXML private HBox rightHeaderBox;

    private Window() {
	super();
        s.getProperties().put("window", this);
    }

    /**
     * Initializes the controller class.
     */
    private void initialize() {
	getStage().setScene(new Scene(root));
	getStage().getScene().setFill(Color.rgb(0, 0, 0, 0.01));
	getStage().setOpacity(windowOpacity.getValue());

	// clip the content to its bounds to prevent leaking out
	Rectangle mask = new Rectangle(1, 1, BLACK);
	mask.widthProperty().bind(content.widthProperty());
	mask.heightProperty().bind(content.heightProperty());
	content.setClip(mask);

        // normally we would bind bgr size, but we will bind it more dynamically later
	// bgrImgLayer.prefWidthProperty().bind(root.widthProperty());

	// avoid some instances of not closing properly
	s.setOnCloseRequest(e -> close());

        // disable default exit fullscreen on ESC key
	// it doesnt exit fullscreen properly for this class, because it calls
	// stage.setFullscreen() directly
	s.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
	// implement the functionality properly
	s.addEventHandler(KEY_PRESSED, e -> {
	    if (e.getCode() == ESCAPE && isFullscreen()) {
                setFullscreen(false);
                e.consume();
            }
	});

        // drag&drop
        DragUtil.installDrag(
            root, GAVEL,
            "Display possible actions\n\nMoving the drag elsewhere may offer other actions",
            e -> !DragUtil.hasWidgetOutput(e),
            e -> APP.actionPane.show(DragUtil.getAnyFut(e))
        );

	// maintain custom pseudoclasses for .window styleclass
	focused.addListener((o,ov,nv) -> root.pseudoClassStateChanged(pcFocused, nv));
	resizing.addListener((o,ov,nv) -> root.pseudoClassStateChanged(pcResized, nv!=NONE));
	moving.addListener((o,ov,nv) -> root.pseudoClassStateChanged(pcMoved, nv));
	fullscreen.addListener((o,ov,nv) -> root.pseudoClassStateChanged(pcFullscreen, nv));

	// set local shortcuts
        Action.getActions().stream().filter(a -> !a.isGlobal() && a.hasKeysAssigned())
              .forEach(a -> a.registerInScene(s.getScene()));

	// update context manager
	root.addEventFilter(MOUSE_PRESSED, e -> UiContext.setPressedXY(e.getSceneX(),e.getSceneY()));
	root.addEventFilter(MOUSE_CLICKED, e -> UiContext.fireAppMouseClickEvent(this, e));

	// app dragging
	header.addEventHandler(DRAG_DETECTED, this::moveStart);
	header.addEventHandler(MOUSE_DRAGGED, this::moveDo);
	header.addEventHandler(MOUSE_RELEASED, this::moveEnd);

	// header double click maximize, show header on/off
        header.setMouseTransparent(false);
	header.setOnMouseClicked(e -> {
	    if (e.getButton() == PRIMARY)
		if (e.getClickCount() == 2)
		    toggleMaximize();
	    if (e.getButton() == SECONDARY)
		if (e.getClickCount() == 2)
		    setHeaderVisible(!headerVisible);
	});

        // header open/close
        header_activator.addEventFilter(MOUSE_ENTERED, e -> {
            if(!headerVisible)
                applyHeaderVisible(true);
        });
        header.addEventFilter(MOUSE_EXITED_TARGET, e -> {
            if(!headerVisible && !moving.get() && resizing.get()==NONE && e.getSceneY()>20)
                applyHeaderVisible(false);
        });

        titleL.setMinWidth(0);

        // change volume on scroll
	// if some component has its own onScroll behavior, it should consume
	// the event so this will not fire
	root.setOnScroll(e -> {
	    if (e.getDeltaY() > 0) PLAYBACK.volumeInc();
	    else if (e.getDeltaY() < 0) PLAYBACK.volumeDec();
	});

        // layout mode on key press/release
	root.addEventFilter(KeyEvent.ANY, e -> {
	    if (e.getCode().equals(Action.Shortcut_ALTERNATE)) {
		GUI.setLayoutMode(e.getEventType().equals(KEY_PRESSED));
                if(e.getEventType().equals(KEY_PRESSED)) IOPane.drawWidgetIO();
	    }
	});

	Icon propB = new Icon(GEARS, 13, Action.get("Open settings"));
	Icon runB = new Icon(GAVEL, 13, Action.get("Open app actions"));
	Icon layB = new Icon(COLUMNS, 13, Action.get("Open layout manager"));
	Icon lastFMB = new Icon<>(null, 13, "LastFM\n\nEnable/configure last fm with left/right "
                + "click. Currently, lastFM support is disabled.", e -> {
                    Node b = (Node) e.getSource();
                    if (e.getButton() == PRIMARY)
                        if (LastFM.getScrobblingEnabled())
                            LastFM.toggleScrobbling();
                        else
                            if (LastFM.isLoginSuccess())
                                LastFM.toggleScrobbling();
                            else
                                new PopOver("LastFM login", LastFM.getLastFMconfig()).show(b);
                    else if (e.getButton() == SECONDARY)
                        new PopOver("LastFM login", LastFM.getLastFMconfig()).show(b);
        });
        maintain(LastFM.scrobblingEnabledProperty(), mapB(LASTFM_SQUARE,LASTFM), lastFMB::icon);
        lastFMB.setDisable(true);
	Icon lockB = new Icon(null, 13, "Lock layout\n\nRestricts certain layout operations to "
                + "prevent accidents and configuration getting in the way. Widgets, containers and "
                + "layouts can also be locked individually.", GUI::toggleLayoutLocked);
        maintain(GUI.layoutLockedProperty(), mapB(LOCK,UNLOCK), lockB::icon);
	Icon lmB = new Icon(null, 13, Action.get("Manage Layout & Zoom"));
	Icon ltB = new Icon(CARET_LEFT, 13, "Previous layout\n\nSwitch to next layout",
                () -> ((SwitchPane)getSwitchPane()).alignLeftTab());
	Icon rtB = new Icon(CARET_RIGHT, 13, "Next layout\n\nSwitch to next layout",
                () -> ((SwitchPane)getSwitchPane()).alignRightTab());
        maintain(GUI.layout_mode, mapB(TH,TH_LARGE), lmB::icon);
	Icon guideB = new Icon(GRADUATION_CAP, 13, Action.get("Open guide"));
	Icon helpB = createInfoIcon("Available actions:\n"
            + "\tHeader icons : Providing custom functionalities. See tooltips.\n"
            + "\tHeader buttons : Providing window contorl. See tooltips.\n"
            + "\tMouse drag : Move window. Windows snap to screen or to other windows.\n"
            + "\tMouse drag to screen edge : Activates one of 7 maximized modes.\n"
            + "\tMouse drag edge : Resizes window.\n"
            + "\tDouble left click : Toggle meximized mode on/off.\n"
            + "\tDouble right click : Toggle hide header on/off.\n"
            + "\tPress ALT : Show hidden header temporarily.\n"
            + "\tPress ALT : Activate layout mode.\n"
            + "\tContent right drag : drag tabs.");

        // left header
	leftHeaderBox.getChildren().addAll(
//            gitB, cssB, dirB, iconsB, new Label(" "),
            new Label(" "),
            layB, propB, runB, lastFMB, new Label(" "),
            ltB, lockB, lmB, rtB, new Label(" "), guideB, helpB
        );

	Icon miniB = new Icon(null, 13, Action.get("Mini mode"));
        maintain(miniB.hoverProperty(), mapB(ANGLE_DOUBLE_UP,ANGLE_UP), miniB::icon);
        Icon ontopB = new Icon(null, 13, "Always on top\n\nForbid hiding this window behind other "
                + "application windows", this::toggleAlwaysOnTOp);
        maintain(alwaysOnTop, mapB(SQUARE,SQUARE_ALT), ontopB::icon);
        Icon fullscrB = new Icon(null, 17, "Fullscreen\n\nExpand window to span whole screen and "
                + "put it on top", this::toggleFullscreen);
        maintain(fullscreen, mapB(FULLSCREEN_EXIT,FULLSCREEN), fullscrB::icon);
        Icon minimB = new Icon(WINDOW_MINIMIZE, 13, "Minimize application", this::toggleMinimize);
//        maintain(minimB.hoverProperty(), mapB(MINUS_SQUARE,MINUS_SQUARE_ALT), minimB::icon);
        Icon maximB = new Icon(WINDOW_MAXIMIZE, 13, "Maximize\n\nExpand window to span whole screen",
                this::toggleMaximize);
//        maintain(maximB.hoverProperty(), mapB(PLUS_SQUARE,PLUS_SQUARE_ALT), maximB::icon);
        Icon closeB = new Icon(CLOSE, 13, "Close\n\nCloses window. If the window is main, "
                + "application closes as well.", this::close);
//        maintain(maximB.hoverProperty(), mapB(PLUS_SQUARE,PLUS_SQUARE_ALT), maximB::icon);

        // right header
	rightHeaderBox.getChildren().addAll(miniB, ontopB, fullscrB, minimB, maximB, closeB);
    }

    private void setAsMain() {
        no(App.getWindow()!=null, "Only one window can be main");

	APP.window = this;
	main = true;

        // move the window owner to screen of this window, which
        // moves taskbar icon to respective screen's taskbar
        moving.addListener((o,ov,nv) -> {
            if(ov && !nv)
                App.getWindowOwner().setX(getCenterX());
        });
        add1timeEventHandler(s, WINDOW_SHOWN, e -> App.getWindowOwner().setX(getCenterX()));

	setIcon(App.getIcon());
	setTitle(null);
	// setTitlePosition(Pos.CENTER_LEFT);

        Icon mainw_i = new Icon(FontAwesomeIcon.CIRCLE,5)
                .tooltip("Main window\n\nThis window is main app window\nClosing it will "
                       + "close application.");
        rightHeaderBox.getChildren().add(0, new Label(""));
        rightHeaderBox.getChildren().add(0,mainw_i);
    }

/******************************* CONTENT **************************************/

    private Layout layout;
    private SwitchContainer topContainer;
//    private SwitchPane switchPane;

    public Layout getLayout() {
        return layout;
    }

    public void setContent(Node n) {
        yes(layout==null, "Layout already initialized");
	content.getChildren().clear();
	content.getChildren().add(n);
	setAnchors(n, 0d);
    }

    public void setContent(Component c) {
        if(c!=null)
            topContainer.addChild(topContainer.getEmptySpot(), c);
    }

    public void initLayout() {
        topContainer = new SwitchContainer();
        Layout l = new Layout();
	content.getChildren().clear();
               l.load(content);
        l.setChild(topContainer);
        initLayout(l);
    }

    public void initLayout(Layout l) {
        yes(layout==null, "Layout already initialized");
        layout = l;
	content.getChildren().clear();
        layout.load(content);
        topContainer = (SwitchContainer) l.getChild();
        topContainer.load();    // if loaded no-op, otherwise initializes
//        switchPane = new SwitchPane();

        double scaleFactor = 1.25; // to prevent running out of bgr when isMoving gui
        back.translateXProperty().unbind();
        back.setTranslateX(0);
        back.setScaleX(scaleFactor);
        back.setScaleY(scaleFactor);
        // scroll bgr along with the tabs
        // using: (|x|/x)*AMPLITUDE*(1-1/(1+SCALE*|x|))
        // -try at: http://www.mathe-fa.de
        topContainer.ui.translateProperty().addListener((o, oldx, newV) -> {
            double x = newV.doubleValue();
            double space = back.getWidth() * ((scaleFactor - 1) / 2d);
            double dir = signum(x);
            x = abs(x);
            back.setTranslateX(dir * space * (1 - (1 / (1 + 0.0005 * x))));
        });
        topContainer.ui.zoomProperty().addListener((o, oldx, newV) -> {
            double x = newV.doubleValue();
            x = 1 - (1 - x) / 5;
            back.setScaleX(scaleFactor * pow(x, 0.25));
            back.setScaleY(scaleFactor * pow(x, 0.25));
        });
    }

    /**
     Returns layout aggregator of this window.
     <p>
     @return layout aggregator, never null.
     */
    public SwitchPane getSwitchPane() {
	return topContainer==null ? null : topContainer.ui;
    }

    public SwitchContainer getTopContainer() {
        return (SwitchContainer) layout.getChild();
    }

    /**
     Blocks input to content, but not to root.
     <p>
     Use when any input to content is not desirable, for example during
     window manipulation like animations.
     <p>
     Sometimes content could consume or interfere with the input directed
     towards the window (root), in such situations this method will help.
     <p>
     @param val
     */
    public void setContentMouseTransparent(boolean val) {
	content.setMouseTransparent(val);
    }

/****************************** HEADER & BORDER **********************************/

    @FXML public BorderPane header;
    @FXML private Pane header_activator;
    @FXML private Region lBorder;
    @FXML private Region rBorder;
    @FXML private ImageView iconI;
    @FXML private Label titleL;
    @FXML private HBox leftHeaderBox;
    private boolean headerVisible = true;
    private boolean headerAllowed = true;
    private boolean borderless = false;

    /**
     Sets visibility of the window header, including its buttons for control
     of the window (close, etc).
     */
    public void setHeaderVisible(boolean val) {
	headerVisible = val;
	applyHeaderVisible(val);
    }

    public boolean isHeaderVisible() {
	return headerVisible;
    }

    private void applyHeaderVisible(boolean val) {
        if (!headerAllowed & val) return;
        if(rightHeaderBox.isVisible()==val) return;
	rightHeaderBox.setVisible(val);
	leftHeaderBox.setVisible(val);
	if (val) {
	    header.setPrefHeight(25);
	    AnchorPane.setTopAnchor(content, 25d);
	    AnchorPane.setTopAnchor(lBorder, 25d);
	    AnchorPane.setTopAnchor(rBorder, 25d);

            Anim.par(
                par(
                    forEachIStream(leftHeaderBox.getChildren(),(i,icon)->
                        new Anim(at->setScaleXY(icon,at*at)).dur(500).intpl(new ElasticInterpolator()).delay(i*45))
                ),
                par(
                    forEachIRStream(rightHeaderBox.getChildren(),(i,icon)->
                        new Anim(at->setScaleXY(icon,at*at)).dur(500).intpl(new ElasticInterpolator()).delay(i*45))
                )
            ).play();


	} else {
	    header.setPrefHeight(isBorderlessApplied()? 0 : 5);
	    AnchorPane.setTopAnchor(content, 5d);
	    AnchorPane.setTopAnchor(lBorder, 5d);
	    AnchorPane.setTopAnchor(rBorder, 5d);
	}
    }

    /** Set false to never show header. */
    public void setHeaderAllowed(boolean val) {
	setHeaderVisible(val ? headerVisible : false);
	headerAllowed = val;
    }

    public boolean isHeaderVisibleApplied() {
	return AnchorPane.getTopAnchor(content)==25;
    }

    /**
     * Set title for this window shown in the header.
     */
    public void setTitle(String text) {
	titleL.setText(text);
    }

    /** Set title alignment. */
    public void setTitlePosition(Pos align) {
	BorderPane.setAlignment(titleL, align);
    }

    /**
     Set icon. Null clears.
     */
    public void setIcon(Image img) {
	iconI.setImage(img);
	leftHeaderBox.getChildren().remove(iconI);
    }

    /** Creates new progress indicator in this window's header, and returns it.
     * Bind or set its progress value to show ongoing task's progress.
     * <ul>
     * <li> Set the the indicator's progress to -1, to indicate the task has
     * started. This will display the indicator.
     * <li> Stop the indicator by setting progress to 1, when your task finishes.
     * </ul>
     * Always do both on FX application thread.
     * <p>
     * Multiple indicators are supported. Never use the same one for more than
     * one task/work.
     * <p>
     * Indicator is disposed of automatically when progress is set to 1. Be sure
     * that the task finishes at some point!
     *
     * @return indicator
     */
    public ProgressIndicator taskAdd() {
        Spinner p = new Spinner();
        Anim a = new Anim(at->setScaleXY(p,at*at)).dur(500).intpl(new ElasticInterpolator());
        p.progressProperty().addListener((o,ov,nv) -> {
            if(nv.doubleValue()==-1) {
                // add indicator to header
                leftHeaderBox.getChildren().add(p);
                a.then(null)
                 .play();
            }
            if(nv.doubleValue()==1) {
                // remove indicator from header
                a.then(() -> leftHeaderBox.getChildren().remove(p))
                 .playClose();
            }
        });
        return p;
    }
    boolean a(){
        return true;
    }

/**************************** WINDOW MECHANICS ********************************/

    @Override
    public void close() {
        LOGGER.info("Closing window. {} windows currently open.", WINDOWS.size());
	if (main) {
            LOGGER.info("Window is main. App will be closed.", WINDOWS.size());
            // javaFX bug fix - close all pop overs first
	    // new list avoids ConcurrentModificationError
	    list(PopOver.active_popups).forEach(PopOver::hideImmediatelly);
	    // act as main window and close whole app
	    App.getWindowOwner().close();
	} else {
            if(layout!=null) layout.close(); // close layout to release resources
            WINDOWS.remove(this);   // remove from window list
            super.close();  // in the end close itself
        }
    }

    @Override
    public void setFullscreen(boolean v) {
	super.setFullscreen(v);                         // fullscreen
        applyBorderless(v ? true : borderless);         // borderless
	applyHeaderVisible(v ? false : headerVisible);  // headerless
    }

    public boolean isBorderless() {
	return borderless;
    }

    public void setBorderless(boolean v) {
        borderless = v;
        applyBorderless(v);
    }

    private void applyBorderless(boolean v) {
        double tp = isHeaderVisibleApplied() ? 25 : v ? 0 : 5;
        double p = v ? 0 : 5;
        util.graphics.Util.setAnchors(content, tp,p,p,p);
        header.setPrefHeight(tp);
	borders.setVisible(!v);
    }
    public boolean isBorderlessApplied() {
	return AnchorPane.getBottomAnchor(content) == 0;
    }

 /*********************************   MOVING   ********************************/

    private double appX;
    private double appY;

    private void moveStart(MouseEvent e) {
        // disable when being resized, resize starts at mouse pressed so
	// it can not consume drag detected event and prevent dragging
	// should be fixed
	if (e.getButton() != PRIMARY || resizing.get()!=Resize.NONE) return;
//        if(header.contains(new Point2D(e.getSceneX(), e.getSceneY())));

        isMoving.set(true);
	appX = e.getSceneX();
	appY = e.getSceneY();
    }

    private void moveDo(MouseEvent e) {
	if (!isMoving.get() || e.getButton() != PRIMARY) return;

	double X = e.getScreenX();
	double Y = e.getScreenY();

	// get screen
	Screen screen = getScreen(X, Y);

	double SWm = screen.getBounds().getMinX(); //screen_wbegin
	double SHm = screen.getBounds().getMinY(); //screen_wbegin
	double SW = screen.getBounds().getMaxX(); //screen_width
	double SH = screen.getBounds().getMaxY(); //screen_height
	double SW5 = screen.getBounds().getWidth() / 5;
	double SH5 = screen.getBounds().getHeight() / 5;

	if (isMaximized() == Maximized.NONE)
	    setXY(X - appX, Y - appY);

	// (imitate Aero Snap)
	Maximized to;

	//left screen edge
	if (X <= SWm + 10)
	    if (Y < SHm + SH5) to = Maximized.LEFT_TOP;
	    else if (Y < SH - SH5) to = Maximized.LEFT;
	    else to = Maximized.LEFT_BOTTOM; // left screen part
	else if (X < SWm + SW5)
	    if (Y <= SHm) to = Maximized.LEFT_TOP;
	    else if (Y < SH - 1) to = Maximized.NONE;
	    else to = Maximized.LEFT_BOTTOM; // middle screen
	else if (X < SW - SW5)
	    if (Y <= SHm) to = Maximized.ALL;
	    else if (Y < SH - 1) to = Maximized.NONE;
	    else to = Maximized.NONE; // right screen part
	else if (X < SW - 10)
	    if (Y <= SHm) to = Maximized.RIGHT_TOP;
	    else if (Y < SH - 1) to = Maximized.NONE;
	    else to = Maximized.RIGHT_BOTTOM; // right screen edge
	else
	    if (Y < SHm + SH5) to = Maximized.RIGHT_TOP;
	    else if (Y < SH - SH5) to = Maximized.RIGHT;
	    else to = Maximized.RIGHT_BOTTOM;

	setScreen(screen);
	setMaximized(mouse_speed<10 ? to : isMaximized());
    }

    private void moveEnd(MouseEvent e) {
        isMoving.set(false);
    }

/*******************************    RESIZING  *********************************/

    @FXML
    private void border_onDragStart(MouseEvent e) {
        // start resize if allowed
        if(resizable.get()) {
            double X = e.getSceneX();
            double Y = e.getSceneY();
            double WW = getWidth();
            double WH = getHeight();
            double L = 18; // corner treshold

            Resize r = NONE;
            if ((X > WW - L) && (Y > WH - L))   r = SE;
            else if ((X < L) && (Y > WH - L))   r = SW;
            else if ((X < L) && (Y < L))        r = NW;
            else if ((X > WW - L) && (Y < L))   r = NE;
            else if ((X > WW - L))              r = Resize.E;
            else if ((Y > WH - L))              r = S;
            else if ((X < L))                   r = W;
            else if ((Y < L))                   r = N;
            isResizing.set(r);
        }
	e.consume();
    }

    @FXML
    private void border_onDragEnd(MouseEvent e) {
        // end resizing if active
	if(isResizing.get()!=NONE) isResizing.set(NONE);
	e.consume();
    }

    @FXML
    private void border_onDragged(MouseEvent e) {
        if(resizable.get()) {
            Resize r = isResizing.get();
            if (r == SE)
                setSize(e.getScreenX() - getX(), e.getScreenY() - getY());
            else if (r == S)
                setSize(getWidth(), e.getScreenY() - getY());
            else if (r == Resize.E)
                setSize(e.getScreenX() - getX(), getHeight());
            else if (r == SW) {
                setSize(getX() + getWidth() - e.getScreenX(), e.getScreenY() - getY());
                setXY(e.getScreenX(), getY());
            } else if (r == W) {
                setSize(getX() + getWidth() - e.getScreenX(), getHeight());
                setXY(e.getScreenX(), getY());
            } else if (r == NW) {
                setSize(getX() + getWidth() - e.getScreenX(), getY() + getHeight() - e.getScreenY());
                setXY(e.getScreenX(), e.getScreenY());
            } else if (r == N) {
                setSize(getWidth(), getY() + getHeight() - e.getScreenY());
                setXY(getX(), e.getScreenY());
            } else if (r == NE) {
                setSize(e.getScreenX() - getX(), getY() + getHeight() - e.getScreenY());
                setXY(getX(), e.getScreenY());
            }
        }
	e.consume();
    }

/******************************************************************************/

    @FXML
    private void consumeMouseEvent(MouseEvent e) {
	e.consume();
    }

/**************************** SERIALIZATION ***********************************/

    private static final XStream X = App.APP.serialization.x;

    public void serialize(File f) {
        try {
            X.toXML(this, new BufferedWriter(new FileWriter(f)));
        } catch (IOException e) {
            LOGGER.error("Window serialization failed into file {}", f,e);
        }
    }

    public static Window deserialize(File f) {
	try {
	    return (Window) X.fromXML(f);
	} catch (ClassCastException | StreamException e) {
            LOGGER.error("Unable to load window from the file {}",e);
	    return null;
	}
    }

    public static final class WindowConverter implements Converter {

	@Override
	public boolean canConvert(Class type) {
	    return Window.class.equals(type);
	}

        @TODO(purpose = BUG, note = "fullscreen deserialization bug in WindowBase")
	@Override
	public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
	    Window w = (Window) value;
	    writer.startNode("W");
	    writer.setValue(w.WProp.getValue().toString());
	    writer.endNode();
	    writer.startNode("H");
	    writer.setValue(w.HProp.getValue().toString());
	    writer.endNode();
	    writer.startNode("X");
	    writer.setValue(w.XProp.getValue().toString());
	    writer.endNode();
	    writer.startNode("Y");
	    writer.setValue(w.YProp.getValue().toString());
	    writer.endNode();
	    writer.startNode("minimized");
	    writer.setValue(w.s.iconifiedProperty().getValue().toString());
	    writer.endNode();
	    writer.startNode("maximized");
	    writer.setValue(w.MaxProp.getValue().toString());
	    writer.endNode();
	    writer.startNode("fullscreen");
//	    writer.setValue(w.FullProp.getValue().toString());
	    writer.setValue(Boolean.FALSE.toString());
	    writer.endNode();
	    writer.startNode("resizable");
	    writer.setValue(w.resizable.getValue().toString());
	    writer.endNode();
	    writer.startNode("alwaysOnTop");
	    writer.setValue(w.s.alwaysOnTopProperty().getValue().toString());
	    writer.endNode();
	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
	    Window w = Window.create();
	    if (w == null) return null;

	    reader.moveDown();
	    w.WProp.set(Double.parseDouble(reader.getValue()));
	    reader.moveUp();
	    reader.moveDown();
	    w.HProp.set(Double.parseDouble(reader.getValue()));
	    reader.moveUp();
	    reader.moveDown();
	    w.XProp.set(Double.parseDouble(reader.getValue()));
	    reader.moveUp();
	    reader.moveDown();
	    w.YProp.set(Double.parseDouble(reader.getValue()));
	    reader.moveUp();
	    reader.moveDown();
	    w.s.setIconified(Boolean.parseBoolean(reader.getValue()));
	    reader.moveUp();
	    reader.moveDown();
	    w.MaxProp.set(Maximized.valueOf(reader.getValue()));
	    reader.moveUp();
	    reader.moveDown();
	    w.FullProp.set(Boolean.parseBoolean(reader.getValue()));
	    reader.moveUp();
	    reader.moveDown();
	    w.resizable.set(Boolean.parseBoolean(reader.getValue()));
	    reader.moveUp();
	    reader.moveDown();
	    w.setAlwaysOnTop(Boolean.parseBoolean(reader.getValue()));
	    reader.moveUp();
	    return w;
	}
    }

}
