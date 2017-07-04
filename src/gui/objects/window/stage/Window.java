package gui.objects.window.stage;

import audio.playback.PLAYBACK;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import gui.Gui;
import gui.objects.icon.Icon;
import gui.objects.popover.PopOver;
import gui.objects.window.Resize;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import layout.Component;
import layout.container.layout.Layout;
import layout.container.switchcontainer.SwitchContainer;
import layout.container.switchcontainer.SwitchPane;
import main.AppBuilders;
import org.reactfx.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.lasfm.LastFM;
import util.access.V;
import util.action.Action;
import util.animation.Anim;
import util.animation.interpolator.ElasticInterpolator;
import util.conf.IsConfigurable;
import util.graphics.Util;
import util.graphics.drag.DragUtil;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CLOSE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.GAVEL;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.LASTFM;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.LOCK;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.*;
import static gui.objects.icon.Icon.createInfoIcon;
import static gui.objects.window.Resize.NONE;
import static java.lang.Math.*;
import static javafx.collections.FXCollections.observableArrayList;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyCombination.keyCombination;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.*;
import static javafx.scene.paint.Color.BLACK;
import static main.App.APP;
import static util.access.SequentialValue.next;
import static util.access.SequentialValue.previous;
import static util.animation.Anim.par;
import static util.async.Async.runLater;
import static util.dev.Util.noØ;
import static util.dev.Util.throwIfNot;
import static util.functional.Util.*;
import static util.graphics.Util.setAnchors;
import static util.graphics.Util.setScaleXY;
import static util.reactive.Util.maintain;

/**
 * Window for application.
 * <p/>
 * Use example:
 * <pre>{@code
 *     Window w = Window.create();
 *            w.setIsPopup(true);
 *            w.getStage().initOwner(App.getWindow().getStage());
 *            w.setTitle(title);
 *            w.setContent(content);
 *            w.show();
 *            w.setLocationCenter();
 * }</pre>
 */
@IsConfigurable
public class Window extends WindowBase {

	private static final Logger LOGGER = LoggerFactory.getLogger(Window.class);
	static final ObservableList<Window> WINDOWS = observableArrayList();

	/**
	 * Pseudoclass active when this window is focused. Applied on root as '.window'.
	 */
	public static final PseudoClass pcFocused = PseudoClass.getPseudoClass("focused");
	/**
	 * Pseudoclass active when this window is resized. Applied on root as '.window'.
	 */
	public static final PseudoClass pcResized = PseudoClass.getPseudoClass("resized");
	/**
	 * Pseudoclass active when this window is moved. Applied on root as '.window'.
	 */
	public static final PseudoClass pcMoved = PseudoClass.getPseudoClass("moved");
	/**
	 * Pseudoclass active when this window is fullscreen. Applied on root as '.window'.
	 */
	public static final PseudoClass pcFullscreen = PseudoClass.getPseudoClass("fullscreen");

	/**
	 * Scene root. Assigned '.window' styleclass.
	 */
	@FXML
	public AnchorPane root = new AnchorPane();
	/**
	 * Single child of the root.
	 */
	@FXML
	public StackPane subroot;
	@FXML
	public StackPane back, backImage;
	@FXML
	public AnchorPane bordersVisual;
	@FXML
	public AnchorPane front, content;
	@FXML
	HBox rightHeaderBox;

	/**
	 * Disposables ran when window closes. For example you may put here listeners.
	 */
	public final List<Subscription> disposables = new ArrayList<>();
	/**
	 * Denotes whether this window is main window. Main window has altered behavior:
	 * <ul>
	 * <li> Closing it causes application to close as well
	 * </ul>
	 */
	public final V<Boolean> isMain = new V<>(false);
	final Set<Subscription> isMainDisposables = set();

	Window(Stage owner, StageStyle style) {
		super(owner, style);
		s.getProperties().put("window", this);
	}

	/**
	 * Initializes the controller class.
	 */
	void initialize() {
		getStage().setScene(new Scene(root));
		getStage().getScene().setFill(Color.rgb(0, 0, 0, 0.01));

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
		// it does not exit fullscreen properly for this class, because it calls
		// stage.setFullscreen() directly
		s.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
		// implement the functionality properly
		s.addEventHandler(KEY_PRESSED, e -> {
			if (e.getCode()==ESCAPE && isFullscreen()) {
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
		focused.addListener((o, ov, nv) -> root.pseudoClassStateChanged(pcFocused, nv));
		resizing.addListener((o, ov, nv) -> root.pseudoClassStateChanged(pcResized, nv!=NONE));
		moving.addListener((o, ov, nv) -> root.pseudoClassStateChanged(pcMoved, nv));
		fullscreen.addListener((o, ov, nv) -> root.pseudoClassStateChanged(pcFullscreen, nv));

		// disable borders when not resizable
		// makes gui consistent & prevents potential bugs
		//
		// note the poor impl. only borders must be Regions!
		maintain(resizable, v -> !v, v -> front.getChildren().stream().filter(c -> c.getClass().equals(Region.class)).forEach(c -> c.setMouseTransparent(v)));

		// app dragging
		header.addEventHandler(DRAG_DETECTED, this::moveStart);
		header.addEventHandler(MOUSE_DRAGGED, this::moveDo);
		header.addEventHandler(MOUSE_RELEASED, this::moveEnd);

		// app dragging (anywhere on ALT)
		root.addEventFilter(MOUSE_PRESSED, e -> {
			if (e.getButton()==PRIMARY && e.isAltDown() && e.isShiftDown()) {
				isMovingAlt = true;
				subroot.setMouseTransparent(true);
				moveStart(e);
				e.consume();
			}
		});
		root.addEventFilter(MOUSE_DRAGGED, e -> {
			if (isMovingAlt) {
				moveDo(e);
				e.consume();
			}
		});
		root.addEventFilter(MOUSE_RELEASED, e -> {
			if (e.getButton()==PRIMARY && isMovingAlt) {
				isMovingAlt = false;
				subroot.setMouseTransparent(false);
				moveEnd(e);
				e.consume();
			}
		});
		root.addEventFilter(MouseEvent.ANY, e -> {
			if (isMovingAlt) e.consume();
		});

		// header double click maximize, show header on/off
		header.setMouseTransparent(false);
		header.setOnMouseClicked(e -> {
			if (e.getButton()==PRIMARY)
				if (e.getClickCount()==2)
					toggleMaximize();
			if (e.getButton()==SECONDARY)
				if (e.getClickCount()==2)
					setHeaderVisible(!headerVisible);
		});

		// header open/close
		header_activator.addEventFilter(MOUSE_ENTERED, e -> {
			if (!headerVisible)
				applyHeaderVisible(true);
		});
		header.addEventFilter(MOUSE_EXITED_TARGET, e -> {
			if (!headerVisible && !moving.get() && resizing.get()==NONE && e.getSceneY()>20)
				applyHeaderVisible(false);
		});

		titleL.setMinWidth(0);

		// change volume on scroll
		// if some component has its own onScroll behavior, it should consume
		// the event so this will not fire
		root.setOnScroll(e -> {
			if (e.getDeltaY()>0) PLAYBACK.volumeInc();
			else if (e.getDeltaY()<0) PLAYBACK.volumeDec();
		});

		List<Maximized> maximizedValues = list(Maximized.LEFT, Maximized.NONE, Maximized.RIGHT);
		List<Screen> screens = Screen.getScreens();
		KeyCombination cycleSMLeft = keyCombination("Alt+Left");
		KeyCombination cycleSMRight = keyCombination("Alt+Right");
		KeyCombination maximize = keyCombination("Alt+Up");
		KeyCombination minimize = keyCombination("Alt+Down");

		// layout mode on key press/release
		root.addEventFilter(KeyEvent.ANY, e -> {
			if (e.getCode().equals(Action.Shortcut_ALTERNATE)) {
				Gui.setLayoutMode(e.getEventType().equals(KEY_PRESSED));
				if (e.getEventType().equals(KEY_PRESSED) && getSwitchPane()!=null)
					runLater(() -> {
						getSwitchPane().widget_io.layout();
						getSwitchPane().widget_io.drawGraph();
					});
			}
			if (e.getEventType().equals(KEY_PRESSED)) {
				if (cycleSMLeft.match(e)) {
					if (maximized.get()==Maximized.LEFT) screen = previous(screens, screen);
					setMaximized(previous(maximizedValues, maximized.get()));
				}
				if (cycleSMRight.match(e)) {
					if (maximized.get()==Maximized.RIGHT) screen = next(screens, screen);
					setMaximized(next(maximizedValues, maximized.get()));
				}
				if (maximize.match(e)) {
					setMaximized(Maximized.ALL);
				}
				if (minimize.match(e)) {
					if (maximized.get()==Maximized.ALL) setMaximized(Maximized.NONE);
					else minimize();
				}
			}
		});

		setTitle("");
		Icon propB = new Icon(GEARS, 13, Action.get("Open settings"));
		Icon runB = new Icon(GAVEL, 13, Action.get("Open app actions"));
		Icon layB = new Icon(COLUMNS, 13, Action.get("Open layout manager"));
		Icon lastFMB = new Icon<>(null, 13, "LastFM\n\nEnable/configure last fm with left/right "
			+ "click. Currently, lastFM support is disabled.", e -> {
			Node b = (Node) e.getSource();
			if (e.getButton()==PRIMARY)
				if (LastFM.getScrobblingEnabled())
					LastFM.toggleScrobbling();
				else if (LastFM.isLoginSuccess())
					LastFM.toggleScrobbling();
				else
					new PopOver<>("LastFM login", LastFM.getLastFMconfig()).show(b);
			else if (e.getButton()==SECONDARY)
				new PopOver<>("LastFM login", LastFM.getLastFMconfig()).show(b);
		});
		maintain(LastFM.scrobblingEnabledProperty(), mapB(LASTFM_SQUARE, LASTFM), lastFMB::icon);
		lastFMB.setDisable(true);
		Icon lockB = new Icon(null, 13, "Lock layout\n\nRestricts certain layout operations to "
			+ "prevent accidents and configuration getting in the way. Widgets, containers and "
			+ "layouts can also be locked individually.", Gui::toggleLayoutLocked);
		maintain(Gui.layoutLockedProperty(), mapB(LOCK, UNLOCK), lockB::icon);
		Icon lmB = new Icon(null, 13, Action.get("Manage Layout & Zoom"));
		Icon ltB = new Icon(CARET_LEFT, 13, "Previous layout\n\nSwitch to next layout", () -> getSwitchPane().alignLeftTab());
		Icon rtB = new Icon(CARET_RIGHT, 13, "Next layout\n\nSwitch to next layout", () -> getSwitchPane().alignRightTab());
		maintain(Gui.layout_mode, mapB(TH, TH_LARGE), lmB::icon);
		Icon guideB = new Icon(GRADUATION_CAP, 13, Action.get("Open guide"));
		Icon helpB = createInfoIcon("Available actions:\n"
			+ "\tHeader icons : Providing custom functionalities. See tooltips.\n"
			+ "\tHeader buttons : Providing window control. See tooltips.\n"
			+ "\tMouse drag : Move window. Windows snap to screen or to other windows.\n"
			+ "\tMouse drag to screen edge : Activates one of 7 maximized modes.\n"
			+ "\tMouse drag edge : Resize window.\n"
			+ "\tDouble left click : Toggle maximized mode on/off.\n"
			+ "\tDouble right click : Toggle hide header on/off.\n"
			+ "\tPress ALT : Show hidden header temporarily.\n"
			+ "\tPress ALT : Activate layout mode.\n"
			+ "\tContent right drag : drag tabs.").size(13);

		// left header
		leftHeaderBox.getChildren().addAll(
			layB, propB, runB, lastFMB, new Label(" "),
			ltB, lockB, lmB, rtB, new Label(" "),
			guideB, helpB
		);

		Icon miniB = new Icon(null, 13, Action.get("Mini mode"));
		maintain(miniB.hoverProperty(), mapB(ANGLE_DOUBLE_UP, ANGLE_UP), miniB::icon);
		Icon onTopB = new Icon(null, 13, "Always on top\n\nForbid hiding this window behind other "
			+ "application windows", this::toggleAlwaysOnTOp);
		maintain(alwaysOnTop, mapB(SQUARE, SQUARE_ALT), onTopB::icon);
		Icon fullsB = new Icon(null, 13, "Fullscreen\n\nExpand window to span whole screen and "
			+ "put it on top", this::toggleFullscreen).scale(1.3);
		maintain(fullscreen, mapB(FULLSCREEN_EXIT, FULLSCREEN), fullsB::icon);
		Icon minB = new Icon(WINDOW_MINIMIZE, 13, "Minimize application", this::toggleMinimize);
		Icon maxB = new Icon(WINDOW_MAXIMIZE, 13, "Maximize\n\nExpand window to span whole screen",
			this::toggleMaximize);
//        maintain(maxB.hoverProperty(), mapB(PLUS_SQUARE,PLUS_SQUARE_ALT), maxB::icon);
		Icon closeB = new Icon(CLOSE, 13, "Close\n\nCloses window. If the window is is main, "
			+ "application closes as well.", this::close);
		Icon mainB = new Icon(FontAwesomeIcon.CIRCLE, 13).scale(0.4)
			.onClick(() -> APP.windowManager.setAsMain(this));
		maintain(isMain, v -> mainB.setOpacity(v ? 1.0 : 0.4));
		maintain(isMain, v -> mainB.tooltip(v
			? "Main window\n\nThis window is main app window\nClosing it will close application."
			: "Main window\n\nThis window is not main app window\nClosing it will not close application."));

		// right header
		rightHeaderBox.getChildren().addAll(mainB, new Label(""), miniB, onTopB, fullsB, new Label(""), minB, maxB, closeB);
	}

/* ---------- CONTENT ----------------------------------------------------------------------------------------------- */

	private Layout layout;
	private SwitchContainer topContainer;
//    private SwitchPane switchPane;

	public Layout getLayout() {
		return layout;
	}

	public void setContent(Node n) {
		throwIfNot(layout==null, "Layout already initialized");
		content.getChildren().clear();
		content.getChildren().add(n);
		setAnchors(n, 0d);
	}

//    public Node getContent() {
//        return content.getChildren().isEmpty() ? null : content.getChildren().get(0);
//    }

	public void setContent(Component c) {
		if (c!=null)
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
		throwIfNot(layout==null, "Layout already initialized");
		layout = l;
		content.getChildren().clear();
		layout.load(content);
		topContainer = (SwitchContainer) l.getChild();
		topContainer.load();    // if loaded no-op, otherwise initializes
//        switchPane = new SwitchPane();

		double scaleFactor = 1.25; // to prevent running out of bgr when isMoving gui
		backImage.translateXProperty().unbind();
		backImage.setTranslateX(0);
		backImage.setScaleX(scaleFactor);
		backImage.setScaleY(scaleFactor);
		// scroll bgr along with the tabs
		// using: (|x|/x)*AMPLITUDE*(1-1/(1+SCALE*|x|))
		// try at: http://www.mathe-fa.de
		topContainer.ui.translateProperty().addListener((o, ov, nv) -> {
			double x = nv.doubleValue();
			double space = backImage.getWidth()*((scaleFactor - 1)/2d);
			double dir = signum(x);
			x = abs(x);
			backImage.setTranslateX(dir*space*(1 - (1/(1 + 0.0005*x))));
		});
		topContainer.ui.zoomProperty().addListener((o, ov, nv) -> {
			double x = nv.doubleValue();
			x = 1 - (1 - x)/5;
			backImage.setScaleX(scaleFactor*pow(x, 0.25));
			backImage.setScaleY(scaleFactor*pow(x, 0.25));
		});
	}

	/**
	 * Returns layout aggregator of this window.
	 *
	 * @return layout aggregator, never null.
	 */
	public SwitchPane getSwitchPane() {
		return topContainer==null ? null : topContainer.ui;
	}

	public SwitchContainer getTopContainer() {
		return (SwitchContainer) layout.getChild();
	}

	/**
	 * Blocks mouse input to content, but not to root.
	 * <p/>
	 * Use when any input to content is not desirable, for example during
	 * window manipulation like animations.
	 * <p/>
	 * Sometimes content could consume or interfere with the input directed
	 * towards the window (root), in such situations this method will help.
	 *
	 * @param val true to block mouse input
	 */
	public void setContentMouseTransparent(boolean val) {
		content.setMouseTransparent(val);
	}

/* ---------- HEADER & BORDER --------------------------------------------------------------------------------------- */

	@FXML
	public BorderPane header;
	@FXML
	private Pane header_activator;
	@FXML
	private Region lBorder;
	@FXML
	private Region rBorder;
	@FXML
	private Label titleL;
	@FXML
	private HBox leftHeaderBox;
	private boolean headerVisible = true;
	private boolean headerAllowed = true;
	private boolean borderless = false;

	/**
	 * Sets visibility of the window header, including its buttons for control
	 * of the window (close, etc).
	 */
	public void setHeaderVisible(boolean val) {
		headerVisible = val;
		applyHeaderVisible(val);
	}

	public boolean isHeaderVisible() {
		return headerVisible;
	}

	private void applyHeaderVisible(boolean val) {
		if (!headerAllowed&val) return;
		if (rightHeaderBox.isVisible()==val) return;
		rightHeaderBox.setVisible(val);
		leftHeaderBox.setVisible(val);
		if (val) {
			header.setPrefHeight(25);
			AnchorPane.setTopAnchor(content, 25d);
			AnchorPane.setTopAnchor(lBorder, 25d);
			AnchorPane.setTopAnchor(rBorder, 25d);

			Anim.par(
				par(
					forEachIStream(leftHeaderBox.getChildren(), (i, icon) ->
						new Anim(at -> setScaleXY(icon, at*at)).dur(500).intpl(new ElasticInterpolator()).delay(i*45))
				),
				par(
					forEachIRStream(rightHeaderBox.getChildren(), (i, icon) ->
						new Anim(at -> setScaleXY(icon, at*at)).dur(500).intpl(new ElasticInterpolator()).delay(i*45))
				)
			).play();
		} else {
			header.setPrefHeight(isBorderlessApplied() ? 0 : 5);
			AnchorPane.setTopAnchor(content, 5d);
			AnchorPane.setTopAnchor(lBorder, 5d);
			AnchorPane.setTopAnchor(rBorder, 5d);
		}
	}

	/** Set false to never show header. */
	public void setHeaderAllowed(boolean val) {
		setHeaderVisible(val && headerVisible);
		headerAllowed = val;
	}

	public boolean isHeaderVisibleApplied() {
		return AnchorPane.getTopAnchor(content)==25;
	}

	/**
	 * Set title for this window shown in the header.
	 */
	public void setTitle(String text) {
		if (text==null || text.isEmpty()) {
			leftHeaderBox.getChildren().remove(titleL);
		} else {
			leftHeaderBox.getChildren().remove(titleL);
			leftHeaderBox.getChildren().add(0, titleL);
			titleL.setPadding(new Insets(5, 0, 0, 5));
		}
		titleL.setText(text);
	}

	/** Set title alignment. */
	public void setTitlePosition(Pos align) {
		BorderPane.setAlignment(titleL, align);
	}

	/**
	 * Creates new progress indicator in this window's header, and returns it.
	 * Bind or set its progress value to show ongoing task's progress.
	 * <ul>
	 * <li> Set the the indicator's progress to -1, to indicate the task has
	 * started. This will display the indicator.
	 * <li> Stop the indicator by setting progress to 1, when your task finishes.
	 * </ul>
	 * Always do both on FX application thread.
	 * <p/>
	 * Multiple indicators are supported. Never use the same one for more than
	 * one task/work.
	 * <p/>
	 * Indicator is disposed of automatically when progress is set to 1. Be sure
	 * that the task finishes at some point!
	 *
	 * @return indicator
	 */
	public ProgressIndicator taskAdd() {
		return AppBuilders.appProgressIndicator(
			pi -> leftHeaderBox.getChildren().add(pi),      // add indicator to header on start
			pi -> leftHeaderBox.getChildren().remove(pi)    // remove indicator from header on end
		);
	}

/* ---------- WINDOW MECHANICS -------------------------------------------------------------------------------------- */

	@Override
	public void close() {
		LOGGER.info("Closing{} window. {} windows currently open.", isMain.get() ? " main" : "", WINDOWS.size());
		if (isMain.get()) {
			APP.close();
		} else {
			if (layout!=null) layout.close(); // close layout to release resources
			disposables.forEach(Subscription::unsubscribe);
			WINDOWS.remove(this);   // remove from window list
			super.close();  // in the end close itself
		}
	}

	@Override
	public void setFullscreen(boolean v) {
		super.setFullscreen(v);
		applyBorderless(v || borderless);
		applyHeaderVisible(!v && headerVisible);
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
		util.graphics.Util.setAnchors(content, tp, p, p, p);
		header.setPrefHeight(tp);
		bordersVisual.setVisible(!v);
	}

	public boolean isBorderlessApplied() {
		return AnchorPane.getBottomAnchor(content)==0;
	}

/* ---------- MOVING ------------------------------------------------------------------------------------------------ */

	private double appX;
	private double appY;
	private Subscription mouseMonitor = null;
	private double mouseSpeed = 0;
	private boolean isMovingAlt = false;

	private void moveStart(MouseEvent e) {
		// disable when being resized, resize starts at mouse pressed so
		// it can not consume drag detected event and prevent dragging
		// should be fixed
		if (e.getButton()!=PRIMARY || resizing.get()!=Resize.NONE) return;
//        if (header.contains(new Point2D(e.getSceneX(), e.getSceneY())));

		mouseMonitor = APP.mouseCapture.observeMouseVelocity(speed -> mouseSpeed = speed);
		isMoving.set(true);
		appX = e.getSceneX();
		appY = e.getSceneY();
	}

	private void moveDo(MouseEvent e) {
		// We do not want to check button onMove. Right click could interfere (possibly stop)
		// the movement, but we want to simply ignore that. The movement begins and ends only
		// with PRIMARY button, which is satisfactory condition to begin with.
		// if (!isMoving.get() || e.getButton() != PRIMARY) return;
		if (!isMoving.get()) return;

		double X = e.getScreenX();
		double Y = e.getScreenY();
		Screen screen = Util.getScreen(X, Y);

		double SWm = screen.getBounds().getMinX();
		double SHm = screen.getBounds().getMinY();
		double SW = screen.getBounds().getMaxX();
		double SH = screen.getBounds().getMaxY();
		double SW5 = screen.getBounds().getWidth()/5;
		double SH5 = screen.getBounds().getHeight()/5;

		if (isMaximized()==Maximized.NONE)
			setXY(X - appX, Y - appY);

		// (imitate Windows Aero Snap)
		Maximized to;

		//left screen edge
		if (X<=SWm + 10)
			if (Y<SHm + SH5) to = Maximized.LEFT_TOP;
			else if (Y<SH - SH5) to = Maximized.LEFT;
			else to = Maximized.LEFT_BOTTOM; // left screen part
		else if (X<SWm + SW5)
			if (Y<=SHm) to = Maximized.LEFT_TOP;
			else if (Y<SH - 1) to = Maximized.NONE;
			else to = Maximized.LEFT_BOTTOM; // middle screen
		else if (X<SW - SW5)
			if (Y<=SHm) to = Maximized.ALL;
			else if (Y<SH - 1) to = Maximized.NONE;
			else to = Maximized.NONE; // right screen part
		else if (X<SW - 10)
			if (Y<=SHm) to = Maximized.RIGHT_TOP;
			else if (Y<SH - 1) to = Maximized.NONE;
			else to = Maximized.RIGHT_BOTTOM; // right screen edge
		else if (Y<SHm + SH5) to = Maximized.RIGHT_TOP;
		else if (Y<SH - SH5) to = Maximized.RIGHT;
		else to = Maximized.RIGHT_BOTTOM;

		setScreen(screen);
		setMaximized(mouseSpeed<100 ? to : isMaximized());
	}

	@SuppressWarnings("unused")
	private void moveEnd(MouseEvent e) {
		isMoving.set(false);
		if (mouseMonitor!=null) mouseMonitor.unsubscribe();
	}

	/*******************************    RESIZING  *********************************/

	@FXML
	private void border_onDragStart(MouseEvent e) {
		if (resizable.get()) {
			double X = e.getSceneX();
			double Y = e.getSceneY();
			double WW = getWidth();
			double WH = getHeight();
			double L = 18; // corner threshold

			Resize r = NONE;
			if ((X>WW - L) && (Y>WH - L)) r = Resize.SE;
			else if ((X<L) && (Y>WH - L)) r = Resize.SW;
			else if ((X<L) && (Y<L)) r = Resize.NW;
			else if ((X>WW - L) && (Y<L)) r = Resize.NE;
			else if ((X>WW - L)) r = Resize.E;
			else if ((Y>WH - L)) r = Resize.S;
			else if ((X<L)) r = Resize.W;
			else if ((Y<L)) r = Resize.N;
			isResizing.set(r);
		}
		e.consume();
	}

	@FXML
	private void border_onDragEnd(MouseEvent e) {
		if (isResizing.get()!=NONE) isResizing.set(NONE);
		e.consume();
	}

	@FXML
	private void border_onDragged(MouseEvent e) {
		if (resizable.get()) {
			Resize r = isResizing.get();
			if (r==Resize.SE)
				setSize(e.getScreenX() - getX(), e.getScreenY() - getY());
			else if (r==Resize.S)
				setSize(getWidth(), e.getScreenY() - getY());
			else if (r==Resize.E)
				setSize(e.getScreenX() - getX(), getHeight());
			else if (r==Resize.SW) {
				setSize(getX() + getWidth() - e.getScreenX(), e.getScreenY() - getY());
				setXY(e.getScreenX(), getY());
			} else if (r==Resize.W) {
				setSize(getX() + getWidth() - e.getScreenX(), getHeight());
				setXY(e.getScreenX(), getY());
			} else if (r==Resize.NW) {
				setSize(getX() + getWidth() - e.getScreenX(), getY() + getHeight() - e.getScreenY());
				setXY(e.getScreenX(), e.getScreenY());
			} else if (r==Resize.N) {
				setSize(getWidth(), getY() + getHeight() - e.getScreenY());
				setXY(getX(), e.getScreenY());
			} else if (r==Resize.NE) {
				setSize(e.getScreenX() - getX(), getY() + getHeight() - e.getScreenY());
				setXY(getX(), e.getScreenY());
			}
		}
		e.consume();
	}

	/******************************************************************************/

	/**
	 * Sets font, overriding css style.
	 *
	 * @param font non null font
	 */
	public void setFont(Font font) {
		noØ(font);
		String tmp = font.getStyle().toLowerCase();
		FontPosture style = tmp.contains("italic") ? FontPosture.ITALIC : FontPosture.REGULAR;
		FontWeight weight = tmp.contains("bold") ? FontWeight.BOLD : FontWeight.NORMAL;
		// for some reason javaFX and css values are quite different...
		String styleS = style==FontPosture.ITALIC ? "italic" : "normal";
		String weightS = weight==FontWeight.BOLD ? "bold" : "normal";
		getStage().getScene().getRoot().setStyle(
				"-fx-font-family: \"" + font.getFamily() + "\";" +
				"-fx-font-style: " + styleS + ";" +
				"-fx-font-weight: " + weightS + ";" +
				"-fx-font-size: " + font.getSize() + ";"
		);
	}

	@FXML
	private void consumeMouseEvent(MouseEvent e) {
		e.consume();
	}

}