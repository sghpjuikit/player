package sp.it.pl.ui.objects.window.stage;

import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableMap;
import javafx.geometry.Rectangle2D;
import javafx.scene.input.KeyCombination;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sp.it.pl.ui.objects.window.Resize;
import sp.it.util.access.WithSetterObservableValue;
import sp.it.util.math.P;
import static java.lang.Math.abs;
import static sp.it.pl.main.AppExtensionsKt.getEmScaled;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.pl.ui.objects.window.stage.WindowBase.Maximized.ALL;
import static sp.it.pl.ui.objects.window.stage.WindowBase.Maximized.NONE;
import static sp.it.util.access.PropertiesDelegatedKt.toWritable;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.reactive.UtilKt.syncC;

/**
 * Customized Stage, window of the application.
 * <p/>
 * Prior to calling these methods, correct screen to this window has to be set.
 * For example the window will maximize within its screen, so correct screen
 * needs to be set first. The responsibility of deciding, which window is correct
 * is left on the developer.
 */
public class WindowBase {

	final Stage s = new Stage();

	final DoubleProperty W = new SimpleDoubleProperty(100);
	final DoubleProperty H = new SimpleDoubleProperty(100);
	final DoubleProperty X = new SimpleDoubleProperty(0);
	final DoubleProperty Y = new SimpleDoubleProperty(0);
	final ReadOnlyObjectWrapper<Maximized> MaxProp = new ReadOnlyObjectWrapper<>(NONE);
	final ReadOnlyBooleanWrapper isMoving = new ReadOnlyBooleanWrapper(false);
	final ReadOnlyObjectWrapper<Resize> isResizing = new ReadOnlyObjectWrapper<>(Resize.NONE);
	final BooleanProperty FullProp = new SimpleBooleanProperty(s.isFullScreen());
	private double deMaxX = 0; // 0-1
	private double deMaxY = 0; // 0-1

	/** Whether the window has focus. Read-only. Use {@link #focus()} */
	public final @NotNull ReadOnlyBooleanProperty focused = s.focusedProperty();
	/** Whether this window is always on top {@link javafx.stage.Stage#alwaysOnTopProperty()} */
	public final @NotNull WithSetterObservableValue<@NotNull Boolean> alwaysOnTop = toWritable(s.alwaysOnTopProperty(), consumer(s::setAlwaysOnTop));
	/** Whether this window is fullscreen {@link javafx.stage.Stage#fullScreenProperty()} */
	public final @NotNull WithSetterObservableValue<@NotNull Boolean> fullscreen = toWritable(s.fullScreenProperty(), consumer(it -> { FullProp.set(it); s.setFullScreen(it); }));
	/** {@link javafx.stage.Stage#fullScreenExitHintProperty} */
	public final @NotNull ObjectProperty<@NotNull String> fullScreenExitHint = s.fullScreenExitHintProperty();
	/** {@link javafx.stage.Stage#fullScreenExitKeyProperty} */
	public final @NotNull ObjectProperty<@NotNull KeyCombination> fullScreenExitCombination = s.fullScreenExitKeyProperty();
	/** Whether this window is maximized */
	public final @NotNull WithSetterObservableValue<@NotNull Maximized> maximized = toWritable(MaxProp, consumer(it -> setMaximized(it)));
	/** Whether the window is being moved */
	public final @NotNull ReadOnlyBooleanProperty moving = isMoving.getReadOnlyProperty();
	/** Whether and how the window is being resized */
	public final @NotNull ReadOnlyObjectProperty<@NotNull Resize> resizing = isResizing.getReadOnlyProperty();
	/** Whether this window is resizable. Programmatically it is still possible to change the size of the Stage */
	public final @NotNull BooleanProperty resizable = s.resizableProperty();
	public final @NotNull DoubleProperty opacity = s.opacityProperty();
	public final @NotNull ReadOnlyBooleanProperty showing = s.showingProperty();
	public final @NotNull ObservableMap<@Nullable Object, @Nullable Object> properties = s.getProperties();


	public WindowBase(Stage owner, StageStyle style) {
		if (owner!=null) s.initOwner(owner);
		if (style!=null) s.initStyle(style);

		// window properties may change externally so let us take notice
		syncC(s.xProperty(), v -> {
			if (!fullscreen.getValue() && maximized.getValue()==NONE) {
				X.setValue(v);
				if (!isMoving.getValue()) updateScreen();
			}
		});
		syncC(s.yProperty(), v -> {
			if (!fullscreen.getValue() && maximized.getValue()==NONE) {
				Y.setValue(v);
				if (!isMoving.getValue()) updateScreen();
			}
		});
		syncC(s.widthProperty(), v ->  {
			if (!fullscreen.getValue() && maximized.getValue()==NONE) {
				W.setValue(v);
				if (!isMoving.getValue()) updateScreen();
			}
		});
		syncC(s.heightProperty(), v ->  {
			if (!fullscreen.getValue() && maximized.getValue()==NONE) {
				H.setValue(v);
				if (!isMoving.getValue()) updateScreen();
			}
		});
	}

	/**
	 * Returns to last remembered state.
	 * Needed during window initialization.
	 * Does not affect content of the window.
	 */
	@SuppressWarnings("SpellCheckingInspection")
	public void update() {
		s.setWidth(W.get());
		s.setHeight(H.get());

		List<Screen> screens = Screen.getScreens();
		Rectangle2D psb = Screen.getPrimary().getBounds();
		double sxmin = screens.stream().mapToDouble(s -> s.getBounds().getMinX()).min().orElse(psb.getMinX());
		double symin = screens.stream().mapToDouble(s -> s.getBounds().getMinY()).min().orElse(psb.getMinY());
		double sxmax = screens.stream().mapToDouble(s -> s.getBounds().getMaxX()).max().orElse(psb.getMaxX());
		double symax = screens.stream().mapToDouble(s -> s.getBounds().getMaxY()).max().orElse(psb.getMaxY());
		// prevents out of screen position
		if (Y.get() + H.get()<symin) Y.setValue(symin);
		if (Y.get()>symax) Y.setValue(symax-H.get());
		if (X.get() + W.get()<sxmin) X.setValue(sxmin);
		if (X.get()>sxmax) X.setValue(sxmax-W.get());

		s.setX(X.get());
		s.setY(Y.get());
		updateScreen();
		deMaxX = (s.getX() - screen.getBounds().getMinX())/screen.getBounds().getWidth();  // just in case
		deMaxY = (s.getY() - screen.getBounds().getMinY())/screen.getBounds().getHeight(); // -||-

		if (fullscreen.getValue()) s.setFullScreen(true);
		if (maximized.getValue()!=NONE) applyMaximized();
	}

	/**
	 * WARNING: Don't use the stage for positioning, maximizing and other
	 * functionalities already defined in this class!!
	 *
	 * @return stage or null if window's gui was not initialized yet.
	 */
	public Stage getStage() {
		return s;
	}

	public double getHeight() {
		return s.getHeight();
	}

	public double getWidth() {
		return s.getWidth();
	}

	public double getX() {
		return s.getX();
	}

	public double getY() {
		return s.getY();
	}

	public double getCenterX() {
		return s.getX() + getWidth()/2;
	}

	public double getCenterY() {
		return s.getY() + getHeight()/2;
	}

	// cached, needs to be updated when size or position changes
	protected Screen screen = Screen.getPrimary();

	private void updateScreen() {
		screen = sp.it.util.ui.UtilKt.getScreen(s);
	}

	/** Brings the window to front if it was behind some window on the desktop. */
	public void focus() {
		s.requestFocus();
	}

	/**
	 * @return the value of the property minimized.
	 */
	public boolean isMinimized() {
		return s.getOwner()!=null && s.getOwner() instanceof Stage ss
			? ss.isIconified()
			: s.isIconified();
	}

	/** Minimizes window. */
	public void minimize() {
		setMinimized(true);
	}

	/** Sets the value of the property minimized. */
	public void setMinimized(boolean val) {
		if (s.getOwner()!=null && s.getOwner() instanceof Stage ss) {
			ss.setIconified(val);
		} else {
			s.setIconified(val);
		}
	}

	/**
	 * Minimize/de-minimize this window. Switches between ALL and NONE maximize states.
	 */
	public void toggleMinimize() {
		setMinimized(!isMinimized());
	}

	/**
	 * Sets maximized state of this window to provided state. If the window is
	 * in full screen mode this method is a no-op.
	 */
	private void setMaximized(Maximized val) {
		// no-op if fullscreen
		if (fullscreen.getValue()) return;

		// prevent pointless change
		Maximized old = maximized.getValue();
		if (old==val) return;

		// remember window state if entering from non-maximized state
		if (old==Maximized.NONE) {
			// this must not execute when val==NONE but that will not happen here
			W.set(s.getWidth());
			H.set(s.getHeight());
			X.set(s.getX());
			Y.set(s.getY());
			deMaxX = (s.getX() - screen.getBounds().getMinX())/screen.getBounds().getWidth();
			deMaxY = (s.getY() - screen.getBounds().getMinY())/screen.getBounds().getHeight();
		}

		MaxProp.setValue(val);
		applyMaximized();
	}

	private void applyMaximized() {
		switch (maximized.getValue()) {
			case ALL -> maximizeAll();
			case LEFT -> maximizeLeft();
			case RIGHT -> maximizeRight();
			case LEFT_TOP -> maximizeLeftTop();
			case RIGHT_TOP -> maximizeRightTop();
			case LEFT_BOTTOM -> maximizeLeftBottom();
			case RIGHT_BOTTOM -> maximizeRightBottom();
			case NONE -> deMaximize();
		}
	}

	private void maximizeRightBottom() {
		stageResizeRelocate(
			screen.getBounds().getMinX() + screen.getBounds().getWidth()/2,
			screen.getBounds().getMinY() + screen.getBounds().getHeight()/2,
			screen.getBounds().getWidth()/2,
			screen.getBounds().getHeight()/2
		);
	}

	private void maximizeAll() {
		stageResizeRelocate(
			screen.getBounds().getMinX(),
			screen.getBounds().getMinY(),
			screen.getBounds().getWidth(),
			screen.getBounds().getHeight()
		);
	}

	private void maximizeRight() {
		stageResizeRelocate(
			screen.getBounds().getMinX() + screen.getBounds().getWidth()/2,
			screen.getBounds().getMinY(),
			screen.getBounds().getWidth()/2,
			screen.getBounds().getHeight()
		);
	}

	private void maximizeLeft() {
		stageResizeRelocate(
			screen.getBounds().getMinX(),
			screen.getBounds().getMinY(),
			screen.getBounds().getWidth()/2,
			screen.getBounds().getHeight()
		);
	}

	private void maximizeLeftTop() {
		stageResizeRelocate(
			screen.getBounds().getMinX(),
			screen.getBounds().getMinY(),
			screen.getBounds().getWidth()/2,
			screen.getBounds().getHeight()/2
		);
	}

	private void maximizeRightTop() {
		stageResizeRelocate(
			screen.getBounds().getMinX() + screen.getBounds().getWidth()/2,
			screen.getBounds().getMinY(),
			screen.getBounds().getWidth()/2,
			screen.getBounds().getHeight()/2
		);
	}

	private void maximizeLeftBottom() {
		stageResizeRelocate(
			screen.getBounds().getMinX(),
			screen.getBounds().getMinY() + screen.getBounds().getHeight()/2,
			screen.getBounds().getWidth()/2,
			screen.getBounds().getHeight()/2
		);
	}

	private void stageResizeRelocate(double x, double y, double w, double h) {
		s.setX(x);
		s.setY(y);
		s.setWidth(w);
		s.setHeight(h);
	}

	private void deMaximize() {
		MaxProp.set(NONE);
		// Normally we would use last position, but it is possible that de-maximization happens
		// not to the same screen as it was maximized on (screen can manually be changed, which
		// is valid behavior in multiscreen maximization cycling (on Windows WIN+LEFT/RIGHT)).
		// Because stage coordinates are absolute to all screens. E.g. only leftmost screen contains
		// coordinate 0, de-maximized window could be on the wrong (original) screen. Hence we
		// remember screen-relative position (offset) and relatively so (in 0-1 fraction of screen
		// width (otherwise we again risk outside of screen position))
		// and calculate new one: screen+offset setXY(X.get(),Y.get());
		stageResizeRelocate(
			deMaxX*screen.getBounds().getWidth() + screen.getBounds().getMinX(),
			deMaxY*screen.getBounds().getHeight() + screen.getBounds().getMinY(),
			W.get(),
			H.get()
		);
	}

	/**
	 * Maximize/de-maximize this window. Switches between ALL and NONE maximize states.
	 */
	public void toggleMaximize() {
		setMaximized(maximized.getValue()==ALL ? NONE : ALL);
	}

	/** Snaps this window to the top edge of this window's screen. */
	public void snapUp() {
		s.setY(screen.getBounds().getMinY());
	}

	/** Snaps this window to the bottom edge of this window's screen. */
	private void snapDown() {
		s.setY(screen.getBounds().getMaxY() - s.getHeight());
	}

	/** Snaps this window to the left edge of this window's screen. */
	private void snapLeft() {
		s.setX(screen.getBounds().getMinX());
	}

	/** Snaps this window to the right edge of this window's screen. */
	private void snapRight() {
		s.setX(screen.getBounds().getMaxX() - s.getWidth());
	}

	/** @see #setX(double, boolean) */
	public void setX(double x) {
		setXY(x, getY(), true);
	}

	/** @see #setX(double, boolean) */
	public void setX(double x, boolean snap) {
		setXY(x, getY(), snap);
	}

	/** @see #setX(double, boolean) */
	public void setY(double y) {
		setXY(getX(), y, true);
	}

	/** @see #setY(double, boolean) */
	public void setY(double y, boolean snap) {
		setXY(getX(), y, snap);
	}

	/**
	 * Calls {@link #setXY(double, double, boolean)} with provided parameters
	 * and true for snap.
	 */
	public void setXY(double x, double y) {
		setXY(x, y, true);
	}

	public void setXY(P xy) {
		setXY(xy.getX(), xy.getY(), true);
	}

	public void setXY(P xy, boolean snap) {
		setXY(xy.getX(), xy.getY(), snap);
	}

	/** Centers this window on its screen. */
	public void setXYToCenter() {
		setXYToCenter(screen);
	}

	/** Centers this window on the specified screen. */
	public void setXYToCenter(Screen s) {
		Rectangle2D b = s.getBounds();
		double x = (b.getMinX() + b.getMaxX())/2 - getWidth()/2;
		double y = (b.getMinY() + b.getMaxY())/2 - getHeight()/2;
		setXY(x, y);
	}

	/** Returns screen x,y of the center of this window. */
	public P getCenter() {
		return new P(getCenterX(), getCenterY());
	}

	/**
	 * Snaps window to edge of the screen or other window.
	 * <p/>
	 * Executes snapping. Window will snap if snapping is allowed and if the
	 * preconditions of window's state require snapping to be done.
	 * <p/>
	 * Because convenience methods are provided that auto-snap on position
	 * change, there is little use for calling this method externally.
	 */
	public void snap() {
		// avoid snapping while isResizing. It leads to unwanted behavior
		// avoid when not desired
		if (!APP.ui.getSnapping().get()) return;

		var r = isResizing.getValue();
		var w = sp.it.util.ui.UtilKt.getBounds(s);
		var wc = new P(w.getMinX() + w.getWidth()/2.0, w.getMinY() + w.getHeight()/2.0);
		var S = getEmScaled(APP.ui.getSnapDistance().getValue());

		var SWm = screen.getBounds().getMinX();
		var SHm = screen.getBounds().getMinY();
		var SW = screen.getBounds().getMaxX();
		var SH = screen.getBounds().getMaxY();
		var SC = new P(screen.getBounds().getMinX() + screen.getBounds().getWidth()/2.0, screen.getBounds().getMinY() + screen.getBounds().getWidth()/2.0);

		if (r==Resize.NONE) {

			// snap to screen edges (x and y separately)
			if (abs(w.getMinX() - SWm)<S) snapLeft();
			else if (abs(w.getMaxX() - SW)<S) snapRight();
			else if (abs(wc.getX() - SC.getX())<S) s.setX(SC.getX()-w.getWidth()/2.0);
			if (abs(w.getMinY() - SHm)<S) snapUp();
			else if (abs(w.getMaxY() - SH)<S) snapDown();
			else if (abs(wc.getY() - SC.getY())<S) s.setY(SC.getY()-w.getHeight()/2.0);

			// snap to other window edges
			for (javafx.stage.Window W : Stage.getWindows()) {
				if (!W.getProperties().containsKey(Window.keyWindowAppWindow)) continue;

				var WXS = W.getX() + W.getWidth();
				var WXE = W.getX();
				var WYS = W.getY() + W.getHeight();
				var WYE = W.getY();
				var WC = new P(W.getX() + W.getWidth()/2.0, W.getY() + W.getHeight()/2.0);

				if (abs(w.getMinX() - WXS)<S) s.setX(WXS);
				else if (abs(w.getMaxX() - WXE)<S) s.setX(WXE - w.getWidth());
				if (abs(w.getMinY() - WYS)<S) s.setY(WYS);
				else if (abs(w.getMaxY() - WYE)<S) s.setY(WYE - w.getHeight());
				if (abs(wc.getX() - WC.getX())<S) s.setX(WC.getX() - w.getWidth()/2.0);
				if (abs(wc.getY() - WC.getY())<S) s.setY(WC.getY() - w.getHeight()/2.0);
			}
		}
		if (r==Resize.W || r==Resize.SW || r==Resize.NW || r==Resize.ALL) {
			if (abs(w.getMinX() - SWm)<S) {
				setSize(w.getWidth() + (w.getMinX() - SWm), w.getHeight(), false);
				snapLeft();
				w = sp.it.util.ui.UtilKt.getBounds(s);
			}
		}
		if (r==Resize.E || r==Resize.SE || r==Resize.NE || r==Resize.ALL) {
			if (abs(w.getMaxX() - SW)<S) {
				setSize(w.getWidth() - (w.getMaxX() - SW), w.getHeight(), false);
				w = sp.it.util.ui.UtilKt.getBounds(s);
			}
		}
		if (r==Resize.N || r==Resize.NW || r==Resize.NE || r==Resize.ALL) {
			if (abs(w.getMinY() - SHm)<S) {
				setSize(w.getWidth(), w.getHeight() + (w.getMinY() - SHm), false);
				snapUp();
				w = sp.it.util.ui.UtilKt.getBounds(s);
			}
		}
		if (r==Resize.S || r==Resize.SW || r==Resize.SE || r==Resize.ALL) {
			if (abs(w.getMaxY() - SH)<S) {
				setSize(w.getWidth(), w.getHeight() - (w.getMaxY() - SH), false);
			}
		}
	}

	/** @return window-relative position of the centre of this window */
	public P getCentre() {
		return sp.it.util.ui.UtilKt.getCentre(s);
	}

	/** @return window-relative x position of the centre of this window */
	public double getCentreX() {
		return sp.it.util.ui.UtilKt.getCentreX(s);
	}

	/** @return window-relative y position of the centre of this window */
	public double getCentreY() {
		return sp.it.util.ui.UtilKt.getCentreY(s);
	}

	/**
	 * Sets position of the window on the screen.
	 * <p/>
	 * Note: Always use methods provided in this class for isResizing and never
	 * those in the Stage of this window.
	 * <p/>
	 * If the window is in full screen mode, this method is no-op.
	 *
	 * @param x horizontal location of the top left corner
	 * @param y vertical location of the top left corner
	 * @param snap flag for snapping to screen edge and other windows. Snapping will be executed only if the window id
	 * not being resized.
	 */
	public void setXY(double x, double y, boolean snap) {
		if (fullscreen.getValue()) return;
		MaxProp.set(Maximized.NONE);
		s.setX(x);
		s.setY(y);
		X.set(x);
		Y.set(y);
		if (snap) snap();
	}

	/**
	 * Sets size of the window.
	 * Always use this over setWidth(), setHeight(). Not using this method will
	 * result in improper behavior during isResizing - more specifically - the new
	 * size will not be remembered and the window will revert to previous
	 * size during certain graphical operations like reposition.
	 * <p>
	 * It is not recommended using this method for maximizing. Use maximize(),
	 * maximizeLeft(), maximizeRight() instead.
	 * <p>
	 * This method is weak solution to inability to override setWidth(),
	 * setHeight() methods of Stage.
	 * <p>
	 * If the window is in full screen mode or !isResizable(), this method is no-op.
	 *
	 * @param x horizontal location of the top left corner
	 * @param y vertical location of the top left corner
	 * @param width horizontal size of the window
	 * @param height vertical size of the window
	 */
	public void setXYSize(double x, double y, double width, double height, boolean snap) {
		if (fullscreen.getValue()) return;
		MaxProp.set(Maximized.NONE);
		s.setX(x);
		s.setY(y);
		X.set(x);
		Y.set(y);
		s.setWidth(width);
		s.setHeight(height);
		// should snap
		// if (snap) snap();
		W.set(s.getWidth());
		H.set(s.getHeight());
		if (snap) snap();
	}

	public void setXYSize(double x, double y, double width, double height) {
		setXYSize(x, y, width, height, true);
	}

	public void setSize(double width, double height, boolean snap) {
		if (fullscreen.getValue()) return;
		s.setWidth(width);
		s.setHeight(height);
		W.set(s.getWidth());
		H.set(s.getHeight());
		if (snap) snap();
	}

	public void setSize(double width, double height) {
		setSize(width, height, true);
	}

	public void setSize(P size, boolean snap) {
		setSize(size.getX(), size.getY(), snap);
	}

	public void setSize(P size) {
		setSize(size.getX(), size.getY());
	}

	public P getSize() {
		return new P(s.getWidth(), s.getHeight());
	}

	/**
	 * Sets initial size and location by invoking the {@link #setSize} and
	 * {@link #setXY(double, double)} method. The initial size values are primary screen
	 * size divided by half and the location will be set so the window is
	 * center aligned on the primary screen.
	 */
	public void setXYSizeInitial() {
		double w = screen.getBounds().getWidth()/2;
		double h = screen.getBounds().getHeight()/2;
		setXYSize(w/2, h/2, w, h, false);
	}

	/** Sets the window visible and focuses it. */
	public void show() {
		s.show();
		focus();
	}

	/** Sets the window invisible. */
	public void hide() {
		s.hide();
	}

	public boolean isShowing() {
		return s.isShowing();
	}

	/** Closes this window as to never show it visible again. */
	public void close() {
		s.close();
	}

	/** State of window maximization. */
	public enum Maximized {
		ALL,
		LEFT,
		RIGHT,
		LEFT_TOP,
		RIGHT_TOP,
		LEFT_BOTTOM,
		RIGHT_BOTTOM,
		NONE
	}

}