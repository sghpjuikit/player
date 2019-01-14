package sp.it.pl.gui.objects.window.stage;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
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
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.reactfx.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sp.it.pl.audio.Player;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.pl.gui.objects.window.Resize;
import sp.it.pl.layout.Component;
import sp.it.pl.layout.container.layout.Layout;
import sp.it.pl.layout.container.switchcontainer.SwitchContainer;
import sp.it.pl.layout.container.switchcontainer.SwitchPane;
import sp.it.pl.main.AppProgress;
import sp.it.pl.util.access.V;
import sp.it.pl.util.action.Action;
import sp.it.pl.util.action.ActionManager;
import sp.it.pl.util.animation.Anim;
import sp.it.pl.util.animation.interpolator.ElasticInterpolator;
import sp.it.pl.util.async.executor.EventReducer;
import sp.it.pl.util.graphics.UtilKt;
import sp.it.pl.util.graphics.drag.DragUtil;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ANGLE_DOUBLE_UP;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ANGLE_UP;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CARET_LEFT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CARET_RIGHT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CLOSE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.GAVEL;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.GEARS;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.GRADUATION_CAP;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.LOCK;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.SQUARE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.SQUARE_ALT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.TH;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.TH_LARGE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.UNLOCK;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.FULLSCREEN;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.FULLSCREEN_EXIT;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.WINDOW_MAXIMIZE;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.WINDOW_MINIMIZE;
import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.signum;
import static javafx.collections.FXCollections.observableArrayList;
import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyCode.LEFT;
import static javafx.scene.input.KeyCode.RIGHT;
import static javafx.scene.input.KeyCode.TAB;
import static javafx.scene.input.KeyCode.UP;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.input.KeyEvent.KEY_RELEASED;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.DRAG_DETECTED;
import static javafx.scene.input.MouseEvent.MOUSE_DRAGGED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED_TARGET;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED_TARGET;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static javafx.scene.input.MouseEvent.MOUSE_RELEASED;
import static javafx.scene.layout.Region.USE_COMPUTED_SIZE;
import static javafx.scene.paint.Color.BLACK;
import static javafx.util.Duration.millis;
import static sp.it.pl.gui.objects.window.Resize.NONE;
import static sp.it.pl.main.AppBuildersKt.appProgressIndicator;
import static sp.it.pl.main.AppBuildersKt.createInfoIcon;
import static sp.it.pl.main.AppUtil.APP;
import static sp.it.pl.util.access.SequentialValue.next;
import static sp.it.pl.util.access.SequentialValue.previous;
import static sp.it.pl.util.animation.Anim.animPar;
import static sp.it.pl.util.async.AsyncKt.runLater;
import static sp.it.pl.util.dev.Util.throwIfNot;
import static sp.it.pl.util.functional.Util.forEachIRStream;
import static sp.it.pl.util.functional.Util.forEachIStream;
import static sp.it.pl.util.functional.Util.list;
import static sp.it.pl.util.functional.Util.mapB;
import static sp.it.pl.util.functional.Util.set;
import static sp.it.pl.util.functional.UtilKt.consumer;
import static sp.it.pl.util.graphics.Util.setAnchors;
import static sp.it.pl.util.graphics.UtilKt.initClip;
import static sp.it.pl.util.graphics.UtilKt.setScaleXY;
import static sp.it.pl.util.reactive.Util.maintain;

/** Window for application. */
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
	@FXML public AnchorPane root = new AnchorPane();
	/**
	 * Single child of the root.
	 */
	@FXML public StackPane subroot;
	@FXML public StackPane back, backImage;
	@FXML public AnchorPane bordersVisual;
	@FXML public AnchorPane front, content;
	@FXML HBox rightHeaderBox;
	private final double headerHeight = 30;

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
		headerContainer.setMouseTransparent(false);
		headerContainer.setOnMouseClicked(e -> {
			if (e.getButton()==PRIMARY)
				if (e.getClickCount()==2)
					toggleMaximize();
			if (e.getButton()==SECONDARY)
				if (e.getClickCount()==2)
					isHeaderVisible.set(!isHeaderVisible.get());
		});

		// show header by hovering over a thin activator
		header_activator.addEventFilter(MOUSE_ENTERED, e -> {
			if (!_headerVisible)
				applyHeaderVisible(true);
		});

		// hide header on long mouse exit if specified so
		EventReducer<Boolean> headerContainerMouseExited = EventReducer.toLast(300, v -> {
			if (!v) applyHeaderVisible(false);
		});
		headerContainer.addEventFilter(MOUSE_ENTERED_TARGET, e ->
			headerContainerMouseExited.push(true)
		);
		headerContainer.addEventFilter(MOUSE_EXITED_TARGET, e -> {
			if ((!isHeaderVisible.get() || isFullscreen()) && !moving.get() && resizing.get()==NONE && e.getSceneY()>20)    // TODO: 20?
				headerContainerMouseExited.push(false);
		});
		fullscreen.addListener((o,ov,nv) -> applyHeaderVisible(_headerVisible));

		titleL.setMinWidth(0);

		// change volume on scroll
		root.setOnScroll(e -> {
			if (e.getDeltaY()>0) Player.volumeInc();
			else if (e.getDeltaY()<0) Player.volumeDec();
		});

		// report focus changes
		getStage().getScene().focusOwnerProperty().addListener((o,ov,nv) -> APP.ui.getFocusChangedHandler().invoke(nv));
		root.addEventFilter(MOUSE_PRESSED, e -> {
			if (e.getButton()==PRIMARY)
				APP.ui.focusClickedWidget(e);
		});
		root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (e.getCode()==TAB && e.isShortcutDown()) {
				e.consume();

				if (e.isShiftDown()) APP.widgetManager.widgets.selectPreviousWidget(layout);
				else APP.widgetManager.widgets.selectNextWidget(layout);
			}
		});

		List<Maximized> maximizedValues = list(Maximized.LEFT, Maximized.NONE, Maximized.RIGHT);
		ActionManager.INSTANCE.getKeyManageWindow();
		root.addEventFilter(KeyEvent.ANY, e -> {
			// layout mode
			if (e.getCode().equals(ActionManager.INSTANCE.getKeyManageLayout())) {
				if (e.getEventType().equals(KEY_RELEASED)) APP.ui.setLayoutMode(false);
				if (e.getEventType().equals(KEY_PRESSED)) APP.ui.setLayoutMode(true);
				if (e.getEventType().equals(KEY_PRESSED) && getSwitchPane()!=null)
					runLater(() -> {
						getSwitchPane().widget_io.layout();
						getSwitchPane().widget_io.drawGraph();
					});
			}
			// toggle maximized
			if (e.getEventType().equals(KEY_PRESSED)) {
				if (e.isAltDown() && e.isShiftDown()) {

					if (e.getCode()==LEFT) {
						if (maximized.get()==Maximized.LEFT) screen = previous(Screen.getScreens(), screen);
						setMaximized(previous(maximizedValues, maximized.get()));
					}
					if (e.getCode()==RIGHT) {
						if (maximized.get()==Maximized.RIGHT) screen = next(Screen.getScreens(), screen);
						setMaximized(next(maximizedValues, maximized.get()));
					}
					if (e.getCode()==UP) {
						setMaximized(Maximized.ALL);
					}
					if (e.getCode()==DOWN) {
						if (maximized.get()==Maximized.ALL) setMaximized(Maximized.NONE);
						else minimize();
					}
				}
			}
		});

		setTitle("");
		double is = 15;
		Icon propB = new Icon(GEARS, is, Action.get("Open settings"));
		Icon runB = new Icon(GAVEL, is, Action.get("Open app actions"));
		Icon lockB = new Icon(null, is, "Lock layout\n\nRestricts certain layout operations to "
			+ "prevent accidents and configuration getting in the way. Widgets, containers and "
			+ "layouts can also be locked individually.", () -> APP.ui.toggleLayoutLocked());
		maintain(APP.ui.getLockedLayout(), mapB(LOCK, UNLOCK), lockB::icon);
		Icon lmB = new Icon(null, is, Action.get("Layout zoom overlay in/out"));
		Icon ltB = new Icon(CARET_LEFT, is, Action.get("Layout move left"));
		Icon rtB = new Icon(CARET_RIGHT, is, Action.get("Layout move right"));
		maintain(APP.ui.getLayoutMode(), mapB(TH, TH_LARGE), lmB::icon);
		Icon guideB = new Icon(GRADUATION_CAP, is, Action.get("Open guide"));
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
			+ "\tContent right drag : drag tabs.").size(is);
		Icon progB = new Icon(FontAwesomeIcon.CIRCLE, is).scale(0.4).onClick(e -> AppProgress.INSTANCE.showTasks((Node) e.getTarget())).tooltip("Progress & Tasks");

		leftHeaderBox.getChildren().addAll(
			propB, runB, new Label(" "),
			ltB, lockB, lmB, rtB, new Label(" "),
			guideB, helpB, progB
		);
		leftHeaderBox.setTranslateY(-4);
		initClip(leftHeaderBox, new Insets(4, 0, 4, 0));


		Icon miniB = new Icon(null, is, Action.get("Mini mode"));
		maintain(miniB.hoverProperty(), mapB(ANGLE_DOUBLE_UP, ANGLE_UP), miniB::icon);
		Icon onTopB = new Icon(null, is, "Always on top\n\nForbid hiding this window behind other "
			+ "application windows", this::toggleAlwaysOnTop);
		maintain(alwaysOnTop, mapB(SQUARE, SQUARE_ALT), onTopB::icon);
		Icon fullsB = new Icon(null, is, "Fullscreen\n\nExpand window to span whole screen and "
			+ "put it on top", this::toggleFullscreen).scale(1.3);
		maintain(fullscreen, mapB(FULLSCREEN_EXIT, FULLSCREEN), fullsB::icon);
		Icon minB = new Icon(WINDOW_MINIMIZE, is, "Minimize application", this::toggleMinimize);
		Icon maxB = new Icon(WINDOW_MAXIMIZE, is, "Maximize\n\nExpand window to span whole screen",
			this::toggleMaximize);
//        maintain(maxB.hoverProperty(), mapB(PLUS_SQUARE,PLUS_SQUARE_ALT), maxB::icon);
		Icon closeB = new Icon(CLOSE, is, "Close\n\nCloses window. If the window is main, "
			+ "application closes as well.", this::close);
		Icon mainB = new Icon(FontAwesomeIcon.CIRCLE, is).scale(0.4)
			.onClick(() -> APP.windowManager.setAsMain(this));
		maintain(isMain, v -> mainB.setOpacity(v ? 1.0 : 0.4));
		maintain(isMain, v -> mainB.tooltip(v
			? "Main window\n\nThis window is main app window\nClosing it will close application."
			: "Main window\n\nThis window is not main app window\nClosing it will not close application."));

		rightHeaderBox.getChildren().addAll(mainB, new Label(""), miniB, onTopB, fullsB, new Label(""), minB, maxB, closeB);
		rightHeaderBox.setTranslateY(-4);
		initClip(rightHeaderBox, new Insets(4, 0, 4, 0));



		ProgressIndicator x = appProgressIndicator();
		x.progressProperty().bind(AppProgress.INSTANCE.getProgress());
		leftHeaderBox.getChildren().add(x);
	}

/* ---------- CONTENT ----------------------------------------------------------------------------------------------- */

	private Layout layout;
	private SwitchContainer topContainer;
//    private SwitchPane switchPane;

	public Layout getLayout() {
		return layout;
	}

	public void setContent(Node n) {
		throwIfNot(layout==null, () -> "Layout already initialized");
		content.getChildren().clear();
		content.getChildren().add(n);
		setAnchors(n, 0d);
	}

	public void setContent(Component c) {
		if (c!=null)
			topContainer.addChild(topContainer.getEmptySpot(), c);
	}

	public void initLayout() {
		topContainer = new SwitchContainer();
		Layout l = new Layout("Layout");
		content.getChildren().clear();
		l.load(content);
		l.setChild(topContainer);
		initLayout(l);
	}

	public void initLayout(Layout l) {
		throwIfNot(layout==null, () -> "Layout already initialized");
		layout = l;
		if (layout.getName()==null) layout.setName("Layout");
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

	@FXML private StackPane headerContainer;
	@FXML private BorderPane header;
	@FXML private Pane header_activator;
	@FXML private Region lBorder;
	@FXML private Region rBorder;
	@FXML private Label titleL;
	@FXML private HBox leftHeaderBox;
	private boolean _headerVisible = true;

	/** Whether window borders are displayed (when {@link #isFullscreen()} is true, they never are). Default true. */
	public final V<Boolean> isBorderless = new V<>(false).initAttachC(v -> applyHeaderVisible(_headerVisible));
	/** Whether header can be ever visible. Default true. */
	public final V<Boolean> isHeaderAllowed = new V<>(true).initAttachC(v -> applyHeaderVisible(_headerVisible));
	/** Visibility of the window header, including its buttons for control of the window (close, etc). Default true. */
	public final V<Boolean> isHeaderVisible = new V<>(true).initAttachC(v -> applyHeaderVisible(v && !isFullscreen()));

	private void applyHeaderVisible(boolean headerOn) {
		boolean bOn = !isBorderless.get() && !isFullscreen();
		boolean hOn = headerOn && isHeaderAllowed.get();

		bordersVisual.setVisible(bOn);
		content.setPadding(bOn ? new Insets(0.0, 5.0, 5.0, 5.0) : Insets.EMPTY);
		headerContainer.setMinHeight(hOn ? USE_COMPUTED_SIZE : bOn ? 5 : 0);
		headerContainer.setPrefHeight(hOn ? USE_COMPUTED_SIZE : bOn ? 5 : 0);
		headerContainer.setMaxHeight(hOn ? USE_COMPUTED_SIZE : bOn ? 5 : 0);

		if (hOn != _headerVisible) {
			_headerVisible = headerOn;
			header.setVisible(hOn);
			if (hOn) {
				animPar(
					animPar(
						forEachIStream(leftHeaderBox.getChildren(), (i, icon) ->
							new Anim(at -> setScaleXY(icon, at*at)).dur(millis(500)).intpl(new ElasticInterpolator()).delay(millis(i*45)))
					),
					animPar(
						forEachIRStream(rightHeaderBox.getChildren(), (i, icon) ->
							new Anim(at -> setScaleXY(icon, at*at)).dur(millis(500)).intpl(new ElasticInterpolator()).delay(millis(i*45)))
					)
				).play();
			}
		}
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
		return appProgressIndicator(
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
		applyHeaderVisible(!v && _headerVisible);
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

		mouseMonitor = APP.mouse.observeMouseVelocity(consumer(speed -> mouseSpeed = speed));
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
		Screen screen = UtilKt.getScreen(X, Y);
		double SWm = screen.getBounds().getMinX();
		double SHm = screen.getBounds().getMinY();
		double SW = screen.getBounds().getMaxX();
		double SH = screen.getBounds().getMaxY();
		double SW5 = screen.getBounds().getWidth()/5;
		double SH5 = screen.getBounds().getHeight()/5;
		double SDw = 20; // horizontal snap activation distance
		double SDh = 20; // vertical snap activation distance

		if (isMaximized()==Maximized.NONE)
			setXY(X - appX, Y - appY);

		// (imitate Windows Aero Snap)
		Maximized to;

		if (X<=SWm + SDw) {
			if (Y<=SHm + SH5) to = Maximized.LEFT_TOP;
			else if (Y<SH - SH5) to = Maximized.LEFT;
			else to = Maximized.LEFT_BOTTOM;
		} else if (X<SWm + SW5) {
			if (Y<=SHm + SDh) to = Maximized.LEFT_TOP;
			else if (Y<SH - 1) to = Maximized.NONE;
			else to = Maximized.LEFT_BOTTOM;
		} else if (X<SW - SW5) {
			if (Y<=SHm + SDh) to = Maximized.ALL;
			else if (Y<SH - 1) to = Maximized.NONE;
			else to = Maximized.NONE;
		} else if (X<SW - SDw) {
			if (Y<=SHm+SDh) to = Maximized.RIGHT_TOP;
			else if (Y<SH - 1) to = Maximized.NONE;
			else to = Maximized.RIGHT_BOTTOM;
		} else {
			if (Y<SHm + SH5) to = Maximized.RIGHT_TOP;
			else if (Y<SH - SH5) to = Maximized.RIGHT;
			else to = Maximized.RIGHT_BOTTOM;
		}

		boolean isDeMaximize = to==Maximized.NONE && (mouseSpeed==0 || mouseSpeed>70);
		boolean isMaximize = to!=Maximized.NONE && (isMaximized()!=Maximized.NONE || mouseSpeed<70);
		if (isDeMaximize || isMaximize) {
			setScreen(screen);
			setMaximized(to);
		}
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

	@FXML
	private void consumeMouseEvent(MouseEvent e) {
		e.consume();
	}

}