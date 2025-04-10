package sp.it.pl.ui.objects.window.stage;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import sp.it.pl.core.InfoUi;
import sp.it.pl.core.NameUi;
import sp.it.pl.layout.Component;
import sp.it.pl.layout.ContainerSwitch;
import sp.it.pl.layout.ContainerSwitchUi;
import sp.it.pl.layout.ContainerUni;
import sp.it.pl.layout.Layout;
import sp.it.pl.main.AppEventLog;
import sp.it.pl.main.Df;
import sp.it.pl.ui.objects.icon.Icon;
import sp.it.pl.ui.objects.window.Resize;
import sp.it.util.access.OrV;
import sp.it.util.access.OrV.OrValue.Initial.Inherit;
import sp.it.util.access.V;
import sp.it.util.access.WithSetterObservableValue;
import sp.it.util.action.ActionRegistrar;
import sp.it.util.animation.Anim;
import sp.it.util.async.executor.EventReducer;
import sp.it.util.dev.DebugKt;
import sp.it.util.dev.DevKt;
import sp.it.util.math.P;
import sp.it.util.reactive.Disposer;
import sp.it.util.reactive.Subscribed;
import sp.it.util.reactive.Subscription;
import sp.it.util.system.Os;
import sp.it.util.ui.UiDelegateKt;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ANGLE_DOUBLE_UP;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ANGLE_UP;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CARET_LEFT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CARET_RIGHT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CIRCLE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.GAVEL;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.LOCK;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.SEND;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.TH;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.TH_LARGE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.UNLOCK;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.WARNING;
import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.signum;
import static java.lang.Math.sqrt;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toSet;
import static javafx.scene.input.KeyCode.A;
import static javafx.scene.input.KeyCode.ALT;
import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.F;
import static javafx.scene.input.KeyCode.F11;
import static javafx.scene.input.KeyCode.F12;
import static javafx.scene.input.KeyCode.G;
import static javafx.scene.input.KeyCode.LEFT;
import static javafx.scene.input.KeyCode.Q;
import static javafx.scene.input.KeyCode.RIGHT;
import static javafx.scene.input.KeyCode.UP;
import static javafx.scene.input.KeyCode.WINDOWS;
import static javafx.scene.input.KeyCombination.NO_MATCH;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.input.KeyEvent.KEY_RELEASED;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.DRAG_DETECTED;
import static javafx.scene.input.MouseEvent.MOUSE_DRAGGED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED_TARGET;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED_TARGET;
import static javafx.scene.input.MouseEvent.MOUSE_MOVED;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static javafx.scene.input.MouseEvent.MOUSE_RELEASED;
import static javafx.scene.paint.Color.rgb;
import static javafx.stage.WindowEvent.WINDOW_SHOWING;
import static javafx.util.Duration.millis;
import static sp.it.pl.main.AppBuildersKt.animShowNodes;
import static sp.it.pl.main.AppBuildersKt.appProgressIcon;
import static sp.it.pl.main.AppDragKt.contains;
import static sp.it.pl.main.AppDragKt.getAny;
import static sp.it.pl.main.AppDragKt.installDrag;
import static sp.it.pl.main.AppExtensionsKt.showAndDetect;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.pl.main.Ui.ICON_CLOSE;
import static sp.it.pl.ui.objects.window.Resize.ALL;
import static sp.it.pl.ui.objects.window.Resize.E;
import static sp.it.pl.ui.objects.window.Resize.N;
import static sp.it.pl.ui.objects.window.Resize.NE;
import static sp.it.pl.ui.objects.window.Resize.NONE;
import static sp.it.pl.ui.objects.window.Resize.NW;
import static sp.it.pl.ui.objects.window.Resize.S;
import static sp.it.pl.ui.objects.window.Resize.SE;
import static sp.it.pl.ui.objects.window.Resize.SW;
import static sp.it.pl.ui.objects.window.stage.WindowHelperKt.bestRec;
import static sp.it.pl.ui.objects.window.stage.WindowHelperKt.openWindowSettings;
import static sp.it.pl.ui.objects.window.stage.WindowUtilKt.buildWindowLayout;
import static sp.it.pl.ui.objects.window.stage.WindowUtilKt.installStartLayoutPlaceholder;
import static sp.it.pl.ui.objects.window.stage.WindowUtilKt.installWindowInteraction;
import static sp.it.pl.ui.objects.window.stage.WindowUtilKt.lookupId;
import static sp.it.pl.ui.objects.window.stage.WindowUtilKt.resizeTypeForCoordinates;
import static sp.it.util.access.PropertiesDelegatedKt.toWritable;
import static sp.it.util.access.PropertiesKt.toggle;
import static sp.it.util.access.PropertiesKt.toggleNext;
import static sp.it.util.access.Values.next;
import static sp.it.util.access.Values.previous;
import static sp.it.util.animation.Anim.anim;
import static sp.it.util.collections.UtilKt.setTo;
import static sp.it.util.dev.FailKt.failIf;
import static sp.it.util.functional.Util.filter;
import static sp.it.util.functional.Util.list;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.functional.UtilKt.runnable;
import static sp.it.util.reactive.UnsubscribableKt.on;
import static sp.it.util.reactive.UtilKt.attach;
import static sp.it.util.reactive.UtilKt.sync;
import static sp.it.util.reactive.UtilKt.syncC;
import static sp.it.util.reactive.UtilKt.syncTo;
import static sp.it.util.reactive.UtilKt.zip;
import static sp.it.util.text.StringExtensionsKt.keys;
import static sp.it.util.ui.NodeExtensionsKt.pseudoClassToggle;
import static sp.it.util.ui.UiDelegateKt.setUiDelegate;
import static sp.it.util.ui.Util.setAnchors;
import static sp.it.util.ui.UtilKt.getScreen;
import static sp.it.util.ui.UtilKt.getScreenXy;
import static sp.it.util.ui.UtilKt.getXy;
import static sp.it.util.ui.UtilKt.initClip;
import static sp.it.util.ui.UtilKt.initClipToPadding;
import static sp.it.util.ui.UtilKt.pseudoclass;

/** Window for application. */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Window extends WindowBase {

	private static final Logger logger = LoggerFactory.getLogger(Window.class);
	/** Styleclass for window. Applied on {@link #root}. */
	public static final String scWindow = "window";
	public static final String keyWindowLayout = "layout";
	/** Pseudoclass active when this window is focused. Applied on {@link #scWindow}. */
	public static final PseudoClass pcFocused = pseudoclass("focused");
	/** Pseudoclass active when this window is resized. Applied on {@link #scWindow}. */
	public static final PseudoClass pcResized = pseudoclass("resized");
	/** Pseudoclass active when this window is moved. Applied on {@link #scWindow}. */
	public static final PseudoClass pcMoved = pseudoclass("moved");
	/** Pseudoclass active when this window is fullscreen. Applied on {@link #scWindow}. */
	public static final PseudoClass pcFullscreen = pseudoclass("fullscreen");
	/** Pseudoclass active when this window is maximized. Applied on {@link #scWindow}. */
	public static final PseudoClass pcMaximized = pseudoclass("maximized");
	/** Pseudoclass active when this window {@link javafx.stage.StageStyle} is {@link javafx.stage.StageStyle#TRANSPARENT} and {@link #effect} is {@link sp.it.pl.ui.objects.window.stage.Window.BgrEffect#OFF}. */
	public static final String pcTransparentOn = "transparent-on";
	/** Pseudoclass active when this window {@link javafx.stage.StageStyle} is {@link javafx.stage.StageStyle#TRANSPARENT} and {@link #effect} is not {@link sp.it.pl.ui.objects.window.stage.Window.BgrEffect#OFF}. */
	public static final String pcTransparentBlur = "transparent-blur";
	/** Pseudoclass active when this window {@link #transparency} is {@link sp.it.pl.ui.objects.window.stage.Window.Transparency#ON}. */
	public static final String pcTransparent = "transparent";
	/** Pseudoclass active when this window {@link #transparency} is {@link sp.it.pl.ui.objects.window.stage.Window.Transparency#ON_CLICK_THROUGH}. */
	public static final String pcTransparentCt = "transparent-ct";

	/** Scene root. Assigned {@link #scWindow} styleclass. */
	public final StackPane root = buildWindowLayout(consumer(this::borderDragStart), consumer(this::borderDragged), consumer(this::borderDragEnd));
	public final StackPane back = lookupId(root, "back", StackPane.class);
	public final StackPane backImage = lookupId(root, "backImage", StackPane.class);
	public final AnchorPane front = lookupId(root, "front", AnchorPane.class);
	public final AnchorPane content = lookupId(root, "content", AnchorPane.class);
	public final HBox rightHeaderBox = lookupId(root, "rightHeaderBox", HBox.class);
	private final VBox frontContent = lookupId(root, "frontContent", VBox.class);

	final @NotNull ReadOnlyBooleanWrapper isMainImpl = new ReadOnlyBooleanWrapper(false);
	/** Whether this is main window. Closing main window closes the application. Only one window can be main. */
	public final @NotNull WithSetterObservableValue<@NotNull Boolean> isMain = toWritable(isMainImpl, consumer(it -> { if (it) APP.windowManager.setAsMain(this); }));
	/** Whether this window is resizable/movable with mouse when left ALT is held. Default true on non Linux platform. */
	public final @NotNull V<Boolean> isInteractiveOnLeftAlt = new V<>(!Os.UNIX.isCurrent());
	/** Whether {@link #backImage} translates and scales with content to provide a depth effect. A non-uniform bgr needs to be set for the effect to be visible. Default false. */
	public final @NotNull V<@NotNull Boolean> transformBgrWithContent = new V<>(false);
	/** Whether window content has transparent decoration. Default off. */
	public final @NotNull OrV<@NotNull Transparency> transparency = new OrV<>(APP.windowManager.getWindowTransparency(), new Inherit<>());
	/** Whether window content has transparent decoration. Default off. */
	public final @NotNull OrV<@NotNull BgrEffect> effect = new OrV<>(APP.windowManager.getWindowEffect(), new Inherit<>());
	/** Window opacity, backed by {@link javafx.stage.Stage#opacityProperty()}. Default 1.0. */
	public final @NotNull OrV<@NotNull Double> opacity = new OrV<>(APP.windowManager.getWindowOpacity(), new Inherit<>());
	/** Whether window shows on OS taskbar. Can only be set before window is shown. Default true. */
	public final @NotNull V<Boolean> isTaskbarVisible = new V<>(true);
	/** Invoked just before this window closes, after layout closes. */
	public final Disposer onClose = new Disposer();

	Window(Stage owner, StageStyle style) {
		super(owner, style);
		setUiDelegate(s, this);
	}

	void initialize() {
		getStage().setScene(new Scene(root));
		getStage().getScene().setFill(rgb(0, 0, 0, 0.01));

		// normally we would bind bgr size, but we will bind it more dynamically later
		// bgrImgLayer.prefWidthProperty().bind(root.widthProperty());

		// avoid window not closing properly sometimes (like when OS requests closing the window)
		s.setOnCloseRequest(e -> {
			closeWithAnim();  // close() as it would disrupt animation, since this ALT+F4 calls this AND key handler both
			e.consume();  // disable default behavior
		});

		// fullscreen
		fullScreenExitHint.setValue("");
		fullScreenExitCombination.setValue(NO_MATCH);
		attach(fullscreen, consumer(v -> applyHeaderVisible(!v && _headerVisible)));

		// transparent
		if (getStage().getStyle()==StageStyle.TRANSPARENT) {
			sync(zip(effect, transparency), consumer(it -> {
				var e = it.getFirst();
				var t = it.getSecond();

				pseudoClassToggle(root, pcTransparentOn, e==BgrEffect.OFF && t==Transparency.OFF);
				pseudoClassToggle(root, pcTransparentBlur, e!=BgrEffect.OFF && t==Transparency.OFF);
				pseudoClassToggle(root, pcTransparent, t==Transparency.ON);
				pseudoClassToggle(root, pcTransparentCt, t==Transparency.ON_CLICK_THROUGH);
			}));
		}

		syncTo(opacity, getStage().opacityProperty());

		// drag&drop
		installDrag(
			content, GAVEL,
			"Display possible actions\n\nMoving the drag elsewhere may offer other options",
			e -> !contains(e.getDragboard(), Df.WIDGET_OUTPUT),
			consumer(e -> showAndDetect(APP.ui.getActionPane().getOrBuild(), getAny(e.getDragboard()), e))
		);

		// maintain custom pseudoclasses for .window styleclass
		focused.addListener((o, ov, nv) -> root.pseudoClassStateChanged(pcFocused, nv));
		resizing.addListener((o, ov, nv) -> root.pseudoClassStateChanged(pcResized, nv!=NONE));
		moving.addListener((o, ov, nv) -> root.pseudoClassStateChanged(pcMoved, nv));
		fullscreen.addListener((o, ov, nv) -> root.pseudoClassStateChanged(pcFullscreen, nv));
		maximized.addListener((o, ov, nv) -> root.pseudoClassStateChanged(pcMaximized, nv==Maximized.ALL));

		// disable borders when not resizable
		// makes gui consistent & prevents potential bugs
		//
		// note the poor impl. only borders must be Regions!
		syncC(resizable, it -> front.getChildren().stream().filter(c -> c.getClass().equals(Region.class)).forEach(c -> c.setMouseTransparent(!it)));

		// window move hint cursor
		root.addEventHandler(KEY_PRESSED, e -> {
			if (e.getCode() == ALT && isInteractiveOnLeftAlt.getValue())
				root.setCursor(Cursor.HAND);
		});
		root.addEventFilter(KEY_RELEASED, e -> {
			if (e.getCode() == ALT)
				root.setCursor(Cursor.DEFAULT);
		});
		root.addEventFilter(MOUSE_MOVED, e ->
				root.setCursor(e.isAltDown() && isInteractiveOnLeftAlt.getValue() ? Cursor.HAND : Cursor.DEFAULT)
		);
		root.addEventFilter(MOUSE_RELEASED, e ->
				root.setCursor(e.isAltDown() && isInteractiveOnLeftAlt.getValue() ? Cursor.HAND : Cursor.DEFAULT)
		);

		// app dragging
		header.addEventHandler(DRAG_DETECTED, this::moveStart);
		header.addEventHandler(MOUSE_DRAGGED, this::moveDo);
		header.addEventHandler(MOUSE_RELEASED, this::moveEnd);

		// app dragging (anywhere on ALT)
		root.addEventFilter(MouseEvent.ANY, e -> {
			if (!APP.ui.isLayoutMode())
				if ((isInteractiveOnLeftAlt.getValue() && e.isAltDown()) || isMovingAlt || isResizingAlt)
					e.consume();
		});
		root.addEventFilter(DragEvent.ANY, e -> {
			if (!APP.ui.isLayoutMode())
				if (isMovingAlt || isResizingAlt)
					e.consume();
		});
		root.addEventFilter(MOUSE_PRESSED, e -> {
			if (isMovingAlt && e.getButton()==SECONDARY) {
				isMovingAltMaximized = maximized.getValue()==Maximized.NONE;

				if (e.isShiftDown()) {
					var appWindowsWhitelist = APP.windowManager.windows.stream()
						// ignore self
						.filter(w -> w!=this)
						// ignore collapsed dock
						.filter(w -> APP.windowManager.dockWindow==null || !APP.windowManager.dockWindow.isShowing() || w!=APP.windowManager.dockWindow.getWindow())
						.map(i -> new Rectangle((int) i.getX(), (int) i.getY(), (int) i.getWidth(), (int) i.getHeight()))
						.collect(toSet());
					var windowTitleBlacklist = Set.of("", "Program Manager");
					var processId = APP.getProcess().pid();
					var topWindow = new SystemInfo().getOperatingSystem()
						.getDesktopWindows(true)
						.stream()
						.filter(w -> !windowTitleBlacklist.contains(w.getTitle()))
						.filter(w -> w.getOwningProcessId()!=processId || appWindowsWhitelist.contains(w.getLocAndSize()))
						.max(comparingInt(r -> r.getOrder()))
						.map(w -> w.getLocAndSize())
						.orElse(null);

					if (topWindow!=null) {
						var b = bestRec(screen.getBounds(), new P(e.getScreenX(), e.getScreenY()), list(topWindow));
						setXYSize(b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight());
					} else {
						toggleMaximize();
					}
				} else {
					toggleMaximize();
				}

				e.consume();
			}
			if (isInteractiveOnLeftAlt.getValue() && e.isAltDown()) {
				if (e.getButton()==PRIMARY) {
					isMovingAlt = true;
					root.setMouseTransparent(true);
					moveStart(e);
					e.consume();
				}
				if (e.getButton()==SECONDARY && resizable.getValue()) {
					isResizingAlt = true;
					root.setMouseTransparent(true);
					resizeStart(e);
					e.consume();
				}
			}
		});
		root.addEventFilter(MOUSE_DRAGGED, e -> {
			if (isMovingAlt) {
				moveDo(e);
				e.consume();
			}
			if (isResizingAlt) {
				resizeDo(e);
				e.consume();
			}
		});
		root.addEventFilter(MOUSE_RELEASED, e -> {
			if (isMovingAlt && e.getButton()==PRIMARY) {
				isMovingAlt = false;
				isMovingAltMaximized = false;
				root.setMouseTransparent(false);
				moveEnd(e);
				e.consume();
			}
			if (isResizingAlt && e.getButton()==SECONDARY) {
				isResizingAlt = false;
				root.setMouseTransparent(false);
				resizeEnd(e);
				e.consume();
			}
		});

		initClipToPadding(root);
		installWindowInteraction(s);

		// header double click maximize, show header on/off
		headerContainer.setMouseTransparent(false);
		headerContainer.setOnMouseClicked(e -> {
			if (e.getButton()==PRIMARY)
				if (e.getClickCount()==2)
					toggleMaximize();
			if (e.getButton()==SECONDARY)
				if (e.getClickCount()==2)
					toggle(isHeaderVisible);
		});

		// show header by hovering over a thin activator
		headerActivator.addEventFilter(MOUSE_ENTERED, e -> {
			if (!_headerVisible)
				applyHeaderVisible(true);
		});

		// hide header on long mouse exit if specified so
		var headerContainerMouseExited = EventReducer.<Boolean>toLast(1000, consumer(v -> { if (!v) applyHeaderVisible(false); }));
		root.addEventFilter(MOUSE_EXITED, e -> { if (!isHeaderVisible.get()) headerContainerMouseExited.push(false); } );
		headerContainer.addEventFilter(MOUSE_ENTERED_TARGET, e -> headerContainerMouseExited.push(true) );
		headerContainer.addEventFilter(MOUSE_EXITED_TARGET, e -> { if ((!isHeaderVisible.get() || fullscreen.getValue()) && !moving.get() && resizing.get()==NONE && (!root.isHover() || e.getY()>=20)) headerContainerMouseExited.push(false); });
		fullscreen.addListener((o,ov,nv) -> applyHeaderVisible(_headerVisible));
		s.addEventHandler(WINDOW_SHOWING, e -> applyHeaderVisible(_headerVisible));
		header.heightProperty().addListener((o,ov,nv) -> { if (!_headerVisible) headerVisibleAnim.applyAt(1.0); });

		titleL.setMinWidth(0);

		// window control
		var maximizedValues = list(Maximized.LEFT, Maximized.NONE, Maximized.RIGHT);
		s.getScene().addEventFilter(KeyEvent.ANY, e -> {
			if (!e.isAltDown() && !e.isControlDown() && !e.isShortcutDown() && !e.isMetaDown()) {
				if (e.getCode()==F11 || e.getCode()==F12) {
					if (e.getEventType()==KEY_RELEASED) {
						toggle(fullscreen);
					}
					e.consume();
				}
			}
			if (e.isMetaDown()) {
				if (e.getCode()==A) {
					if (e.getEventType()==KEY_RELEASED) {
						toggle(alwaysOnTop);
					}
					e.consume();
				}
				if (e.getCode()==F) {
					if (e.getEventType()==KEY_RELEASED) {
						if (e.isShiftDown()) toggleNext(maximized);
						else toggleMaximize();
					}
					e.consume();
				}
				if (e.getCode()==G) {
					if (e.getEventType()==KEY_RELEASED) {
						toggleMinimize();
					}
					e.consume();
				}
				if (e.getCode()==Q) {
					if (e.getEventType()==KEY_RELEASED) {
						closeWithAnim();
					}
					e.consume();
				}
				if (e.getCode()==F11 || e.getCode()==F12) {
					if (e.getEventType()==KEY_RELEASED) {
						toggle(fullscreen);
					}
					e.consume();
				}
				if (e.getCode()==LEFT) {
					if (e.getEventType()==KEY_RELEASED) {
						if (maximized.getValue()==Maximized.LEFT) screen = previous(Screen.getScreens(), screen);
						maximized.setValue(previous(maximizedValues, maximized.getValue()));
					}
					e.consume();
				}
				if (e.getCode()==RIGHT) {
					if (e.getEventType()==KEY_RELEASED) {
						if (maximized.getValue()==Maximized.RIGHT) screen = next(Screen.getScreens(), screen);
						maximized.setValue(next(maximizedValues, maximized.getValue()));
					}
					e.consume();
				}
				if (e.getCode()==UP) {
					if (e.getEventType()==KEY_RELEASED) {
						maximized.setValue(Maximized.ALL);
					}
					e.consume();
				}
				if (e.getCode()==DOWN) {
					if (e.getEventType()==KEY_RELEASED) {
						if (maximized.getValue()==Maximized.ALL) maximized.setValue(Maximized.NONE);
						else minimize();
					}
					e.consume();
				}
			}
		});

		setTitle("");

		installHeaderReactiveContent();

		installStartLayoutPlaceholder(this);
	}
	private final ObservableValue<Boolean> headerIsLong = getStage().widthProperty().map(it -> it.doubleValue()>600.0);
	private final ArrayList<Node> headerChildrenLeftLong = new ArrayList<>();
	private final ArrayList<Node> headerChildrenLeftShort = new ArrayList<>();
	private final ArrayList<Node> headerChildrenRightLong = new ArrayList<>();
	private final ArrayList<Node> headerChildrenRightShort = new ArrayList<>();

	public Subscription addLeftHeaderIcon(Node icon) {
		icon.setFocusTraversable(false);
		if (icon instanceof Icon i) i.size(null).styleclass("header-icon");

		headerChildrenLeftLong.add(headerChildrenLeftLong.size()-1, icon);
		headerUpdateContent();
		return Subscription.Companion.invoke(runnable(() -> {
			headerChildrenLeftLong.remove(icon);
			headerUpdateContent();
		}));
	}

	public Subscription addRightHeaderIcon(Node icon) {
		icon.setFocusTraversable(false);
		if (icon instanceof Icon i) i.size(null).styleclass("header-icon");

		headerChildrenRightLong.add(2, icon);
		headerUpdateContent();
		return Subscription.Companion.invoke(runnable(() -> {
			headerChildrenRightLong.remove(icon);
			headerUpdateContent();
		}));
	}

	private void headerUpdateContent() {
		var it = headerIsLong.getValue();
		setTo(leftHeaderBox.getChildren(), it ? headerChildrenLeftLong : headerChildrenLeftShort);
		setTo(rightHeaderBox.getChildren(), it ? headerChildrenRightLong : headerChildrenRightShort);
	}

	private void installHeaderReactiveContent() {
		Icon leftMenuB = WindowUtilKt.leftHeaderMenuIcon(this);
		Icon lockB = new Icon(null, -1, ActionRegistrar.get("Toggle layout lock")).styleclass("header-icon");
			on(syncC(APP.ui.getLayoutLocked(), it -> lockB.icon(it, LOCK, UNLOCK)), onClose);
		Icon lmB = new Icon(null, -1, ActionRegistrar.get("Layout zoom overlay in/out")).styleclass("header-icon");
		Icon ltB = new Icon(CARET_LEFT, -1, ActionRegistrar.get("Layout move left")).styleclass("header-icon");
		Icon rtB = new Icon(CARET_RIGHT, -1, ActionRegistrar.get("Layout move right")).styleclass("header-icon");
			on(syncC(APP.ui.getLayoutMode(), it -> lmB.icon(it, TH, TH_LARGE)), onClose);
		Icon errorB = new Icon(WARNING, -1).styleclass("header-icon").action(() -> APP.getActionsLog().showDetailForLastError() ).tooltip("Event Log");
			syncC(AppEventLog.INSTANCE.getHasErrors(), it -> errorB.icon(it, WARNING, SEND));

		Icon miniB = new Icon(null, -1, "Toggle dock").styleclass("header-icon")
			.onClickDo(consumer(it -> toggle(APP.windowManager.getDockShow())));
		    syncC(miniB.hoverProperty(), it -> miniB.icon(it, ANGLE_DOUBLE_UP, ANGLE_UP));
		Icon rightMenuB = WindowUtilKt.rightHeaderMenuIcon(this);
		Icon closeB = new Icon(ICON_CLOSE, -1, "Close (" + keys(WINDOWS, Q) +")\n\nCloses window. If the ui.window is main, application closes as well.").styleclass("header-icon")
			.onClickDo(consumer(it -> close()));
		Icon mainB = new Icon(CIRCLE, -1).styleclass("header-icon").scale(0.4);
			 mainB.action(() -> openWindowSettings(this, mainB));
			syncC(isMain, v -> mainB.setOpacity(v ? 1.0 : 0.4));
			syncC(isMain, v -> mainB.tooltip(v
				? "Main window\n\nThis window is main app window\nClosing it will close application."
				: "Main window\n\nThis window is not main app window\nClosing it will not close application."));


		leftHeaderBox.setTranslateY(-4);
		initClip(leftHeaderBox, new Insets(4, 0, 4, 0));
		headerChildrenLeftLong.addAll(List.of(leftMenuB, new Label(" "), ltB, lockB, lmB, rtB, new Label(" "), errorB, appProgressIcon(onClose)));
		headerChildrenLeftShort.add(leftMenuB);

		rightHeaderBox.setTranslateY(-4);
		initClip(rightHeaderBox, new Insets(4, 0, 4, 0));
		headerChildrenRightShort.add(rightMenuB);
		headerChildrenRightLong.addAll(List.of(mainB, new Label(""), miniB, new Label(""), rightMenuB, closeB));

		syncC(headerIsLong, it -> headerUpdateContent());
	}

/* ---------- CONTENT ----------------------------------------------------------------------------------------------- */

	private @Nullable Layout layout;

	public @Nullable Layout getLayout() {
		return layout;
	}

	/** Can only be called if {@link #getLayout()} is null */
	public void setContent(Node n) {
		failIf(layout!=null, () -> "Layout already initialized");

		content.getChildren().clear();
		content.getChildren().add(n);
		setAnchors(n, 0d);
		n.requestFocus();
	}

	/** Can only be called if {@link #getLayout()} is null */
	public void initLayoutWithContainerUniWith(@NotNull Component c) {
		initLayout(new Layout());
		var cs = new ContainerUni();
		layout.setChild(cs);
		cs.addChild(cs.getEmptySpot(), c);
		c.focus();
	}

	/** Can only be called if {@link #getLayout()} is null */
	public void initLayoutWithContainerSwitchWith(@NotNull Component c) {
		var cs = initLayoutWithContainerSwitch();
			cs.addChild(cs.getEmptySpot(), c);
		c.focus();
	}

	/** Can only be called if {@link #getLayout()} is null */
	public ContainerSwitch initLayoutWithContainerSwitch() {
		initLayout(new Layout());
		var cs = new ContainerSwitch();
		layout.setChild(cs);
		return cs;
	}

	/** Can only be called if {@link #getLayout()} is null */
	public void initLayout(Layout l) {
		failIf(layout!=null, () -> "Layout already initialized");

		layout = l;
		s.getProperties().put(Window.keyWindowLayout, l);
		content.getChildren().clear();
		layout.load(content);

		if (l.getChild() instanceof ContainerSwitch cs) {
			var transformBgrWithContentSub = new Subscribed(it -> {
				double scaleFactor = 1.25; // prevent running out of bgr when translating
				backImage.translateXProperty().unbind();
				backImage.setTranslateX(0);
				backImage.setScaleX(scaleFactor);
				backImage.setScaleY(scaleFactor);
				var s1 = sync(cs.ui.translateProperty(), consumer(nv -> {
					double x = nv.doubleValue();
					double space = backImage.getWidth()*((scaleFactor - 1)/2d);
					double dir = signum(x);
					x = abs(x);
					backImage.setTranslateX(dir*space*(1 - (1/(1 + 0.0005*x))));  // (|x|/x)*AMPLITUDE*(1-1/(1+SCALE*|x|))  try at: http://www.mathe-fa.de
				}));
				var s2 = sync(cs.ui.zoomProperty(), consumer(nv -> {
					double x = nv.doubleValue();
					x = 1 - (1 - x)/5;
					backImage.setScaleX(scaleFactor*pow(x, 0.25));
					backImage.setScaleY(scaleFactor*pow(x, 0.25));
				}));

				return Subscription.Companion.invoke(
					s1,
					s2,
					Subscription.Companion.invoke(runnable(() -> {
						backImage.setTranslateX(0);
						backImage.setScaleX(1.0);
						backImage.setScaleY(1.0);
					}))
				);
			});
			sync(transformBgrWithContent, consumer(transformBgrWithContentSub::subscribe));
		}
	}

	// TODO: dispose of zoom effect
	public Layout detachLayout() {
		var l = layout;
		layout = null;
		return l;
	}

	/**
	 * Returns layout aggregator of this window.
	 *
	 * @return layout aggregator, never null.
	 */
	public @Nullable ContainerSwitchUi getSwitchPane() {
		if (layout!=null && layout.getChild() instanceof ContainerSwitch cs) return cs.ui;
		else return null;
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

	private final StackPane headerContainer = lookupId(root, "headerContainer", StackPane.class);
	private final StackPane header = lookupId(root, "header", StackPane.class);
	private final StackPane headerActivator = lookupId(root, "headerActivator", StackPane.class);
	private final Label titleL = lookupId(root, "titleL", Label.class);
	private final HBox leftHeaderBox = lookupId(root, "leftHeaderBox", HBox.class);
	private boolean _headerVisible = true;
	private boolean _headerInitialized = false;
	private final Anim headerVisibleAnim = anim(millis(250), consumer(it ->
		frontContent.setPadding(new Insets(-header.getHeight()*it, 0.0, 0.0, 0.0))
	));

	/** Whether header can be ever visible. Default true. */
	public final @NotNull V<@NotNull Boolean> isHeaderAllowed = new V<>(true).initAttachC(v -> applyHeaderVisible(_headerVisible));
	/** Visibility of the window header, including its buttons for control of the window (close, etc.). Default true. */
	public final @NotNull V<@NotNull Boolean> isHeaderVisible = new V<>(true).initAttachC(v -> applyHeaderVisible(v && !fullscreen.getValue()));

	private void applyHeaderVisible(boolean headerOn) {
		var hOn = headerOn && isHeaderAllowed.get();
		if (!_headerInitialized || hOn != _headerVisible) {
			headerVisibleAnim.intpl(hOn ? it -> it*it*it : it -> sqrt(sqrt(it)));
			headerVisibleAnim.getPlayAgainIfFinished();
			_headerInitialized = true;
			_headerVisible = headerOn;
			headerVisibleAnim.playFromDir(!hOn);
			if (hOn) {
				animShowNodes(filter(leftHeaderBox.getChildren(), it -> it instanceof Icon)).play();
				animShowNodes(filter(rightHeaderBox.getChildren(), it -> it instanceof Icon)).play();
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
			leftHeaderBox.getChildren().addFirst(titleL);
			titleL.setPadding(new Insets(5, 0, 0, 5));
		}
		titleL.setText(text);
	}

	/** Set title alignment. */
	public void setTitlePosition(Pos align) {
		BorderPane.setAlignment(titleL, align);
	}

/* ---------- WINDOW MECHANICS -------------------------------------------------------------------------------------- */

	@Override
	public void close() {
		logger.info("Closing {} window. {} windows remain open.", isMain.getValue() ? " main" : "", APP.windowManager.windows.size()-1);

		if (!isMain.getValue()) {
			if (layout!=null) layout.close();
			onClose.invoke();
		}
		super.close();
	}

	/** Prevents {@link #closeWithAnim} from being called multiple times */
	private boolean closing = false;

	/** Implements delay or animation for {@link #closeWithAnim}. Can be changed. By default calls {@link #close}. */
	public Runnable closeWithAnim = () -> close();

	/**
	 * Same as {@link #close}, but potentially with delay or animation.
	 * Calls {@link #closeWithAnim} (which eventually calls {@link #close}).
	 * Only takes effect 1st time, subsequent calls do nothing.
	 */
	public void closeWithAnim() {
		if (closing) return;
		closing = true;
		closeWithAnim.run();
	}

/* ---------- MOVING & RESIZING ------------------------------------------------------------------------------------- */

	private double appX;
	private double appY;
	private Subscription mouseMonitor = null;
	private double mouseSpeed = 0;
	private boolean isMovingAlt = false;
	private boolean isMovingAltMaximized = false;

	private void moveStart(MouseEvent e) {
		// disable when being resized, resize starts at mouse pressed, so
		// it can not consume drag detected event and prevent dragging
		// should be fixed
		if (e.getButton()!=PRIMARY || resizing.get()!=NONE) return;

		mouseMonitor = APP.getMouse().observeMouseVelocity(consumer(m -> mouseSpeed = m.getSpeed()));
		isMoving.set(true);
		appX = e.getSceneX();
		appY = e.getSceneY();
	}

	private void moveDo(MouseEvent e) {
		if (isMovingAltMaximized) return;
		// We do not want to check button onMove. Right click could interfere (possibly stop)
		// the movement, but we want to simply ignore that. The movement begins and ends only
		// with PRIMARY button, which is satisfactory condition to begin with.
		// if (!isMoving.get() || e.getButton() != PRIMARY) return;
		if (!isMoving.get()) return;

		var snap = !e.isShortcutDown();
		var X = e.getScreenX();
		var Y = e.getScreenY();
		screen = getScreen(X, Y);
		var SWm = screen.getBounds().getMinX();
		var SHm = screen.getBounds().getMinY();
		var SW = screen.getBounds().getMaxX();
		var SH = screen.getBounds().getMaxY();
		var SW5 = screen.getBounds().getWidth()/5;
		var SH5 = screen.getBounds().getHeight()/5;
		var SDw = 20; // horizontal snap activation distance
		var SDh = 20; // vertical snap activation distance

		if (maximized.getValue()==Maximized.NONE)
			setXY(X - appX, Y - appY, snap);

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
		boolean isMaximize = to!=Maximized.NONE && (maximized.getValue()!=Maximized.NONE || mouseSpeed<70);
		if (isDeMaximize || isMaximize) {
			maximized.setValue(to);
		}
	}

	private void moveEnd(MouseEvent e) {
		isMoving.set(false);
		if (mouseMonitor!=null) mouseMonitor.unsubscribe();
	}

	private P resizeAltMouseStart;
	private P resizeAltAppPos;
	private P resizeAltAppSize;
	private double resizeAltAppY;
	private boolean isResizingAlt = false;

	private void resizeStart(MouseEvent e) {
		resizeAltMouseStart = getScreenXy(e);
		resizeAltAppPos = getXy(s);
		resizeAltAppSize = getSize();
		isResizing.setValue(resizeTypeForCoordinates(s, new P(e.getScreenX(), e.getScreenY())));
		root.setCursor(isResizing.getValue().getCursor());
	}

	private void resizeDo(MouseEvent e) {
		if (isMovingAltMaximized) return;
		var snap = !e.isShortcutDown();
		var diff = getScreenXy(e).minus(resizeAltMouseStart);
		Resize r = isResizing.getValue();
		if (r==SE) {
			setSize(resizeAltAppSize.plus(diff), snap);
		} else if (r==S) {
			setSize(resizeAltAppSize.plus(diff.times(new P(0.0, 1.0))), snap);
		} else if (r==E) {
			setSize(resizeAltAppSize.plus(diff.times(new P(1.0, 0.0))), snap);
		} else if (r==SW) {
			setSize(resizeAltAppSize.minus(diff.times(new P(1.0, -1.0))), snap);
			setXY(resizeAltAppPos.plus(diff.times(new P(1.0, 0.0))), snap);
		} else if (r==Resize.W) {
			setSize(resizeAltAppSize.minus(diff.times(new P(1.0, 0.0))), snap);
			setXY(resizeAltAppPos.plus(diff.times(new P(1.0, 0.0))), snap);
		} else if (r==NW) {
			setSize(resizeAltAppSize.minus(diff), snap);
			setXY(resizeAltAppPos.plus(diff), snap);
		} else if (r==N) {
			setSize(resizeAltAppSize.minus(diff.times(new P(0.0, 1.0))), snap);
			setXY(resizeAltAppPos.plus(diff.times(new P(0.0, 1.0))), snap);
		} else if (r==NE) {
			setSize(resizeAltAppSize.plus(diff.times(new P(1.0, -1.0))), snap);
			setXY(resizeAltAppPos.plus(diff.times(new P(0.0, 1.0))), snap);
		} else if (r==ALL) {
			setSize(resizeAltAppSize.plus(diff.times(2.0)), snap);
			setXY(resizeAltAppPos.minus(diff), snap);
		}
	}

	private void resizeEnd(MouseEvent e) {
		root.setCursor(null);
		isResizing.setValue(NONE);
	}

	private void borderDragStart(MouseEvent e) {
		if (resizable.getValue()) {
			double X = e.getSceneX();
			double Y = e.getSceneY();
			double WW = getWidth();
			double WH = getHeight();
			double L = 20; // corner threshold

			Resize r = NONE;
			if ((X>WW - L) && (Y>WH - L)) r = SE;
			else if ((X<L) && (Y>WH - L)) r = SW;
			else if ((X<L) && (Y<L)) r = NW;
			else if ((X>WW - L) && (Y<L)) r = NE;
			else if ((X>WW - L)) r = E;
			else if ((Y>WH - L)) r = S;
			else if ((X<L)) r = Resize.W;
			else if ((Y<L)) r = N;
			isResizing.setValue(r);
		}
		e.consume();
	}

	private void borderDragged(MouseEvent e) {
		if (resizable.getValue()) {
			var snap = !e.isShortcutDown();
			var r = isResizing.get();
			if (r==SE)
				setSize(e.getScreenX() - getX(), e.getScreenY() - getY(), snap);
			else if (r==S)
				setSize(getWidth(), e.getScreenY() - getY(), snap);
			else if (r==E)
				setSize(e.getScreenX() - getX(), getHeight(), snap);
			else if (r==SW) {
				setSize(getX() + getWidth() - e.getScreenX(), e.getScreenY() - getY(), snap);
				setXY(e.getScreenX(), getY(), snap);
			} else if (r==Resize.W) {
				setSize(getX() + getWidth() - e.getScreenX(), getHeight(), snap);
				setXY(e.getScreenX(), getY(), snap);
			} else if (r==NW) {
				setSize(getX() + getWidth() - e.getScreenX(), getY() + getHeight() - e.getScreenY(), snap);
				setXY(e.getScreenX(), e.getScreenY(), snap);
			} else if (r==N) {
				setSize(getWidth(), getY() + getHeight() - e.getScreenY(), snap);
				setXY(getX(), e.getScreenY(), snap);
			} else if (r==NE) {
				setSize(e.getScreenX() - getX(), getY() + getHeight() - e.getScreenY(), snap);
				setXY(getX(), e.getScreenY(), snap);
			}
		}
		e.consume();
	}

	private void borderDragEnd(MouseEvent e) {
		isResizing.setValue(NONE);
		e.consume();
	}

	public enum Transparency implements NameUi, InfoUi {
		OFF(
			"None",
			"No forced window decoration transparency"
		),
		ON(
			"Transparent",
			"Forces transparent window decoration. Mouse behavior remains normal."
		),
		ON_CLICK_THROUGH(
			"Transparent (click-through)",
			"Forces transparent window decoration. Mouse clicks through transparent content."
		);

		Transparency(String nameUi, String infoUi) {
			this.nameUi = nameUi;
			this.infoUi = infoUi;
		}

		public final String nameUi;
		public final String infoUi;

		@NotNull @Override public String getNameUi() { return nameUi; }
		@NotNull @Override public String getInfoUi() { return infoUi; }
	}
	public enum BgrEffect implements NameUi, InfoUi {
		OFF(
			"None",
			"No background effect"
		),
		BLUR(
			"Blur",
			"Blur background effect. Only supported on Windows OS. Reduces performance."
		),
		ACRYLIC(
			"Acrylic blur",
			"Strong blur background effect. Only supported on Windows OS. Reduces performance."
		);

		BgrEffect(String nameUi, String infoUi) {
			this.nameUi = nameUi;
			this.infoUi = infoUi;
		}

		public final String nameUi;
		public final String infoUi;

		@NotNull @Override public String getNameUi() { return nameUi; }
		@NotNull @Override public String getInfoUi() { return infoUi; }
	}
}