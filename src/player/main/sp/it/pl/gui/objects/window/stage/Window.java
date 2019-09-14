package sp.it.pl.gui.objects.window.stage;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import java.util.List;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.pl.gui.objects.window.Resize;
import sp.it.pl.layout.Component;
import sp.it.pl.layout.container.Layout;
import sp.it.pl.layout.container.SwitchContainer;
import sp.it.pl.layout.container.SwitchContainerUi;
import sp.it.pl.main.AppErrors;
import sp.it.pl.main.AppProgress;
import sp.it.pl.main.Df;
import sp.it.util.access.V;
import sp.it.util.action.ActionManager;
import sp.it.util.action.ActionRegistrar;
import sp.it.util.animation.Anim;
import sp.it.util.animation.interpolator.ElasticInterpolator;
import sp.it.util.async.executor.EventReducer;
import sp.it.util.reactive.Disposer;
import sp.it.util.reactive.Subscription;
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
import static javafx.scene.input.KeyCode.ALT_GRAPH;
import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.LEFT;
import static javafx.scene.input.KeyCode.RIGHT;
import static javafx.scene.input.KeyCode.UP;
import static javafx.scene.input.KeyCombination.NO_MATCH;
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
import static javafx.scene.paint.Color.rgb;
import static javafx.util.Duration.millis;
import static sp.it.pl.gui.objects.window.Resize.NONE;
import static sp.it.pl.gui.objects.window.stage.WindowUtilKt.installStartLayoutPlaceholder;
import static sp.it.pl.main.AppBuildersKt.appProgressIndicator;
import static sp.it.pl.main.AppBuildersKt.infoIcon;
import static sp.it.pl.main.AppDragKt.contains;
import static sp.it.pl.main.AppDragKt.getAnyFut;
import static sp.it.pl.main.AppDragKt.installDrag;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.util.access.PropertiesKt.toggle;
import static sp.it.util.access.Values.next;
import static sp.it.util.access.Values.previous;
import static sp.it.util.animation.Anim.animPar;
import static sp.it.util.dev.FailKt.failIf;
import static sp.it.util.functional.Util.filter;
import static sp.it.util.functional.Util.forEachIRStream;
import static sp.it.util.functional.Util.forEachIStream;
import static sp.it.util.functional.Util.list;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.functional.UtilKt.runnable;
import static sp.it.util.reactive.SubscriptionKt.on;
import static sp.it.util.reactive.UtilKt.onChangeAndNow;
import static sp.it.util.reactive.UtilKt.syncC;
import static sp.it.util.text.UtilKt.getNamePretty;
import static sp.it.util.ui.Util.setAnchors;
import static sp.it.util.ui.UtilKt.getScreen;
import static sp.it.util.ui.UtilKt.initClip;
import static sp.it.util.ui.UtilKt.pseudoclass;
import static sp.it.util.ui.UtilKt.setScaleXY;

/** Window for application. */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Window extends WindowBase {

	private static final Logger logger = LoggerFactory.getLogger(Window.class);
	/** Styleclass for window. Applied on {@link #root}. */
	public static final String scWindow = "window";
	public static final String keyWindowAppWindow = "window";
	public static final String keyWindowLayout = "layout";
	/** Pseudoclass active when this window is focused. Applied on {@link #scWindow}. */
	public static final PseudoClass pcFocused = pseudoclass("focused");
	/** Pseudoclass active when this window is resized. Applied on {@link #scWindow}. */
	public static final PseudoClass pcResized = pseudoclass("resized");
	/** Pseudoclass active when this window is moved. Applied on {@link #scWindow}. */
	public static final PseudoClass pcMoved = pseudoclass("moved");
	/** Pseudoclass active when this window is fullscreen. Applied on {@link #scWindow}. */
	public static final PseudoClass pcFullscreen = pseudoclass("fullscreen");

	/** Scene root. Assigned {@link #scWindow} styleclass. */
	@FXML public StackPane root = new StackPane();
	@FXML public StackPane back, backImage;
	@FXML public AnchorPane front, content;
	@FXML HBox rightHeaderBox;

	final ReadOnlyBooleanWrapper isMainImpl = new ReadOnlyBooleanWrapper(false);
	/** Denotes whether this is main window. Closing main window closes the application. Only one window can be main. */
	public final ReadOnlyBooleanProperty isMain = isMainImpl.getReadOnlyProperty();
	/** Invoked just before this window closes, after layout closes. */
	public final Disposer onClose = new Disposer();

	Window(Stage owner, StageStyle style) {
		super(owner, style);
		s.getProperties().put(keyWindowAppWindow, this);
	}

	void initialize() {
		getStage().setScene(new Scene(root));
		getStage().getScene().setFill(rgb(0, 0, 0, 0.01));

		initClip(content);

		// normally we would bind bgr size, but we will bind it more dynamically later
		// bgrImgLayer.prefWidthProperty().bind(root.widthProperty());

		s.setOnCloseRequest(e -> close()); // avoid window not closing properly sometimes (like when OS requests closing the window)
		s.setFullScreenExitHint("");
		s.setFullScreenExitKeyCombination(NO_MATCH);

		// drag&drop
		installDrag(
			root, GAVEL,
			"Display possible actions\n\nMoving the drag elsewhere may offer other options",
			e -> !contains(e.getDragboard(), Df.WIDGET_OUTPUT),
			consumer(e -> APP.ui.getActionPane().getOrBuild().show(getAnyFut(e.getDragboard())))
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
		syncC(resizable, it -> front.getChildren().stream().filter(c -> c.getClass().equals(Region.class)).forEach(c -> c.setMouseTransparent(!it)));

		// app dragging
		header.addEventHandler(DRAG_DETECTED, this::moveStart);
		header.addEventHandler(MOUSE_DRAGGED, this::moveDo);
		header.addEventHandler(MOUSE_RELEASED, this::moveEnd);

		// app dragging (anywhere on ALT)
		root.addEventFilter(MOUSE_PRESSED, e -> {
			if (e.getButton()==PRIMARY && e.isAltDown() && e.isShiftDown()) {
				isMovingAlt = true;
				root.setMouseTransparent(true);
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
				root.setMouseTransparent(false);
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
			if (e.getDeltaY()>0) APP.audio.volumeInc();
			else if (e.getDeltaY()<0) APP.audio.volumeDec();
		});

		List<Maximized> maximizedValues = list(Maximized.LEFT, Maximized.NONE, Maximized.RIGHT);
		root.addEventFilter(KeyEvent.ANY, e -> {
			// layout mode
			if (e.getCode().equals(ActionManager.INSTANCE.getKeyManageLayout())) {
				if (e.getEventType().equals(KEY_RELEASED)) APP.ui.setLayoutMode(false);
				if (e.getEventType().equals(KEY_PRESSED)) APP.ui.setLayoutMode(true);
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
		Icon propB = new Icon(GEARS, -1, ActionRegistrar.get("Open settings")).styleclass("header-icon");
		Icon runB = new Icon(GAVEL, -1, ActionRegistrar.get("Open app actions")).styleclass("header-icon");
		Icon lockB = new Icon(null, -1, "Lock layout\n\nRestricts certain layout operations to "
			+ "prevent accidents and configuration getting in the way. Widgets, containers and "
			+ "layouts can also be locked individually.", () -> APP.ui.toggleLayoutLocked()).styleclass("header-icon");
		on(syncC(APP.ui.getLockedLayout(), it -> lockB.icon(it ? LOCK : UNLOCK)), onClose);
		Icon lmB = new Icon(null, -1, ActionRegistrar.get("Layout zoom overlay in/out")).styleclass("header-icon");
		Icon ltB = new Icon(CARET_LEFT, -1, ActionRegistrar.get("Layout move left")).styleclass("header-icon");
		Icon rtB = new Icon(CARET_RIGHT, -1, ActionRegistrar.get("Layout move right")).styleclass("header-icon");
		on(syncC(APP.ui.getLayoutMode(), it -> lmB.icon(it ? TH : TH_LARGE)), onClose);
		Icon guideB = new Icon(GRADUATION_CAP, -1, ActionRegistrar.get("Open guide")).styleclass("header-icon");
		Icon helpB = infoIcon("Available actions:"
			+ "\n\tHeader icons : Providing custom functionalities. See tooltips."
			+ "\n\tHeader buttons : Providing window control. See tooltips."
			+ "\n\tMouse drag : Move window. Windows snap to screen or to other windows."
			+ "\n\tMouse drag to screen edge : Activates one of 7 maximized modes."
			+ "\n\tMouse drag edge : Resize window."
			+ "\n\tDouble header left click : Toggle maximized mode on/off."
			+ "\n\tDouble header right click : Toggle hide header on/off."
			+ "\n\t" + getNamePretty(ALT_GRAPH) + " : Toggle layout mode"
			+ "\n\tContent right drag : drag tabs."
		).styleclass("header-icon");
		Icon progB = new Icon(FontAwesomeIcon.CIRCLE, -1).styleclass("header-icon").scale(0.4).onClick(e -> AppProgress.INSTANCE.showTasks((Node) e.getTarget())).tooltip("Progress & Tasks");
		Icon errorB = new Icon(FontAwesomeIcon.WARNING, -1).styleclass("header-icon")
			.onClick(e -> AppErrors.INSTANCE.showDetailForLastError())
			.tooltip("Errors");
		onChangeAndNow(AppErrors.INSTANCE.getHistory(), runnable(() -> errorB.setVisible(!AppErrors.INSTANCE.getHistory().isEmpty())));

		leftHeaderBox.getChildren().addAll(
			propB, runB, new Label(" "),
			ltB, lockB, lmB, rtB, new Label(" "),
			guideB, helpB, progB, errorB
		);
		leftHeaderBox.setTranslateY(-4);
		initClip(leftHeaderBox, new Insets(4, 0, 4, 0));


		Icon miniB = new Icon(null, -1, "Toggle dock", () -> toggle(APP.windowManager.getDockShow())).styleclass("header-icon");
		syncC(miniB.hoverProperty(), it -> miniB.icon(it ? ANGLE_DOUBLE_UP : ANGLE_UP));
		Icon onTopB = new Icon(null, -1, "Always on top\n\nForbid hiding this window behind other application windows", this::toggleAlwaysOnTop).styleclass("header-icon");
		syncC(alwaysOnTop, it -> onTopB.icon(it ? SQUARE : SQUARE_ALT));
		Icon fullsB = new Icon(null, -1, "Fullscreen\n\nExpand window to span whole screen and put it on top", this::toggleFullscreen).scale(1.3).styleclass("header-icon");
		syncC(fullscreen, it -> fullsB.icon(it ? FULLSCREEN_EXIT : FULLSCREEN));
		Icon minB = new Icon(WINDOW_MINIMIZE, -1, "Minimize application", this::toggleMinimize).styleclass("header-icon");
		Icon maxB = new Icon(WINDOW_MAXIMIZE, -1, "Maximize\n\nExpand window to span whole screen", this::toggleMaximize).styleclass("header-icon");
//        maintain(maxB.hoverProperty(), mapB(PLUS_SQUARE,PLUS_SQUARE_ALT), maxB::icon);
		Icon closeB = new Icon(CLOSE, -1, "Close\n\nCloses window. If the window is main, application closes as well.", this::close).styleclass("header-icon");
		Icon mainB = new Icon(FontAwesomeIcon.CIRCLE, -1).styleclass("header-icon").scale(0.4)
			.onClick(() -> APP.windowManager.setAsMain(this));
		syncC(isMain, v -> mainB.setOpacity(v ? 1.0 : 0.4));
		syncC(isMain, v -> mainB.tooltip(v
			? "Main window\n\nThis window is main app window\nClosing it will close application."
			: "Main window\n\nThis window is not main app window\nClosing it will not close application."));
		mainB.setVisible(APP.isUiApp());

		rightHeaderBox.getChildren().addAll(mainB, new Label(""), miniB, onTopB, fullsB, new Label(""), minB, maxB, closeB);
		rightHeaderBox.setTranslateY(-4);
		initClip(rightHeaderBox, new Insets(4, 0, 4, 0));



		ProgressIndicator x = appProgressIndicator();
		x.progressProperty().bind(AppProgress.INSTANCE.getProgress());
		leftHeaderBox.getChildren().add(x);

		installStartLayoutPlaceholder(this);
	}

/* ---------- CONTENT ----------------------------------------------------------------------------------------------- */

	private Layout layout;
	private SwitchContainer topContainer;

	public Layout getLayout() {
		return layout;
	}

	public void setContent(Node n) {
		failIf(layout!=null, () -> "Layout already initialized");

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
		Layout l = new Layout();
		content.getChildren().clear();
		l.load(content);
		l.setChild(topContainer);
		initLayout(l);
	}

	public void initLayout(Layout l) {
		failIf(layout!=null, () -> "Layout already initialized");

		layout = l;
		s.getProperties().put(Window.keyWindowLayout, l);
		content.getChildren().clear();
		layout.load(content);
		topContainer = (SwitchContainer) l.getChild();
		topContainer.load();    // if loaded no-op, otherwise initializes

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
	public SwitchContainerUi getSwitchPane() {
		return topContainer==null ? null : topContainer.ui;
	}

	public SwitchContainer getTopContainer() {
		return layout==null ? null : (SwitchContainer) layout.getChild();
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
	@FXML private StackPane header;
	@FXML private Pane header_activator;
	@FXML private Label titleL;
	@FXML private HBox leftHeaderBox;
	private boolean _headerVisible = true;

	/** Whether header can be ever visible. Default true. */
	public final V<Boolean> isHeaderAllowed = new V<>(true).initAttachC(v -> applyHeaderVisible(_headerVisible));
	/** Visibility of the window header, including its buttons for control of the window (close, etc). Default true. */
	public final V<Boolean> isHeaderVisible = new V<>(true).initAttachC(v -> applyHeaderVisible(v && !isFullscreen()));

	private void applyHeaderVisible(boolean headerOn) {
		boolean hOn = headerOn && isHeaderAllowed.get();

		headerContainer.setMinHeight(hOn ? USE_COMPUTED_SIZE : 0);
		headerContainer.setPrefHeight(hOn ? USE_COMPUTED_SIZE : 0);
		headerContainer.setMaxHeight(hOn ? USE_COMPUTED_SIZE : 0);

		if (hOn != _headerVisible) {
			_headerVisible = headerOn;
			header.setVisible(hOn);
			if (hOn) {
				animPar(
					animPar(
						forEachIStream(filter(leftHeaderBox.getChildren(), it -> it instanceof Icon), (i, icon) ->
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
			consumer(pi -> leftHeaderBox.getChildren().add(pi)),
			consumer(pi -> leftHeaderBox.getChildren().remove(pi))
		);
	}

/* ---------- WINDOW MECHANICS -------------------------------------------------------------------------------------- */

	@Override
	public void close() {
		logger.info("Closing{} window. {} windows remain open.", isMain.getValue() ? " main" : "", APP.windowManager.windows.size()-1);

		if (!isMain.getValue()) {
			if (layout!=null) layout.close();
			onClose.invoke();
		}
		getStage().hide();
		super.close();
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
		screen = getScreen(X, Y);
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
			double L = 20; // corner threshold

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