package sp.it.pl.gui.objects.window.stage;

import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import sp.it.pl.gui.objects.window.Resize;
import sp.it.util.math.P;
import static java.lang.Math.abs;
import static sp.it.pl.gui.objects.window.stage.WindowBase.Maximized.ALL;
import static sp.it.pl.gui.objects.window.stage.WindowBase.Maximized.NONE;
import static sp.it.pl.gui.objects.window.stage.WindowUtilKt.fixJavaFxNonDecoratedMinimization;
import static sp.it.pl.main.AppKt.APP;
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

	final DoubleProperty W = new SimpleDoubleProperty(100);
	final DoubleProperty H = new SimpleDoubleProperty(100);
	final DoubleProperty X = new SimpleDoubleProperty(0);
	final DoubleProperty Y = new SimpleDoubleProperty(0);
	final ReadOnlyObjectWrapper<Maximized> MaxProp = new ReadOnlyObjectWrapper<>(NONE);
	final ReadOnlyBooleanWrapper isMoving = new ReadOnlyBooleanWrapper(false);
	final ReadOnlyObjectWrapper<Resize> isResizing = new ReadOnlyObjectWrapper<>(Resize.NONE);
	final BooleanProperty FullProp = new SimpleBooleanProperty(false);
	private double deMaxX = 0; // 0-1
	private double deMaxY = 0; // 0-1

	Stage s = new Stage();

	/**
	 * Indicates whether the window has focus. Read-only. Use {@link #focus()}.
	 */
	public final ReadOnlyBooleanProperty focused = s.focusedProperty();
	/**
	 * Indicates whether this window is always on top. Window always on top
	 * will not hide behind other windows.
	 */
	public final ReadOnlyBooleanProperty alwaysOnTop = s.alwaysOnTopProperty();
	/**
	 * Indicates whether this window is in fullscreen.
	 */
	public final ReadOnlyBooleanProperty fullscreen = s.fullScreenProperty();
	/**
	 * Indicates whether this window is maximized.
	 */
	public final ReadOnlyObjectProperty<Maximized> maximized = MaxProp.getReadOnlyProperty();
	/**
	 * Indicates whether the window is being moved.
	 */
	public final ReadOnlyBooleanProperty moving = isMoving.getReadOnlyProperty();
	/**
	 * Indicates whether and how the window is being resized.
	 */
	public final ReadOnlyObjectProperty<Resize> resizing = isResizing.getReadOnlyProperty();
	/**
	 * Defines whether this window is resizable. Programmatically it is still
	 * possible to change the size of the Stage.
	 */
	public final BooleanProperty resizable = s.resizableProperty();

	public WindowBase(Stage owner, StageStyle style) {
		if (owner!=null) s.initOwner(owner);
		if (style!=null) s.initStyle(style);

		fixJavaFxNonDecoratedMinimization(s);

		// window properties may change externally so let us take notice
		syncC(s.xProperty(), v -> {
			if (!isFullscreen() && isMaximized()==NONE) {
				X.setValue(v);
				updateScreen();
			}
		});
		syncC(s.yProperty(), v -> {
			if (!isFullscreen() && isMaximized()==NONE) {
				Y.setValue(v);
				updateScreen();
			}
		});
		syncC(s.widthProperty(), v ->  {
			if (!isFullscreen() && isMaximized()==NONE) {
				W.setValue(v);
				updateScreen();
			}
		});
		syncC(s.heightProperty(), v ->  {
			if (!isFullscreen() && isMaximized()==NONE) {
				H.setValue(v);
				updateScreen();
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

		if (FullProp.get()) s.setFullScreen(true);
		if (MaxProp.get()!=NONE) applyMaximized();
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
		screen = sp.it.util.ui.UtilKt.getScreen(getCenter());
	}

	/**
	 * The value of the property resizable
	 * see {@link #setAlwaysOnTop(boolean)}
	 *
	 * @return the value of the property resizable.
	 */
	public boolean isAlwaysOnTop() {
		return s.isAlwaysOnTop();
	}

	/**
	 * Sets the value of the property is AlwaysOnTop.
	 * Property description:
	 * Defines behavior where this window always stays on top of other windows.
	 */
	public void setAlwaysOnTop(boolean val) {
		s.setAlwaysOnTop(val);
	}

	public void toggleAlwaysOnTop() {
		s.setAlwaysOnTop(!s.isAlwaysOnTop());
	}

	/** Brings the window to front if it was behind some window on the desktop. */
	public void focus() {
		s.requestFocus();
	}

	/**
	 * @return the value of the property minimized.
	 */
	public boolean isMinimized() {
		return s.getOwner()!=null && s.getOwner() instanceof Stage
			? ((Stage) s.getOwner()).isIconified()
			: s.isIconified();
	}

	/** Minimizes window. */
	public void minimize() {
		setMinimized(true);
	}

	/** Sets the value of the property minimized. */
	public void setMinimized(boolean val) {
		if (s.getOwner()!=null && s.getOwner() instanceof Stage) {
			((Stage) s.getOwner()).setIconified(val);
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

	/** @return the value of the property maximized. */
	public Maximized isMaximized() {
		return MaxProp.get();
	}

	/**
	 * Sets maximized state of this window to provided state. If the window is
	 * in full screen mode this method is a no-op.
	 */
	public void setMaximized(Maximized val) {
		// no-op if fullscreen
		if (isFullscreen()) return;

		// prevent pointless change
		Maximized old = isMaximized();
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
		switch (MaxProp.getValue()) {
			case ALL: maximizeAll(); break;
			case LEFT: maximizeLeft(); break;
			case RIGHT: maximizeRight(); break;
			case LEFT_TOP: maximizeLeftTop(); break;
			case RIGHT_TOP: maximizeRightTop(); break;
			case LEFT_BOTTOM: maximizeLeftBottom(); break;
			case RIGHT_BOTTOM: maximizeRightBottom(); break;
			case NONE: deMaximize(); break;
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
		// is valid behavior in multi-screen maximization cycling (on Windows WIN+LEFT/RIGHT)).
		// Because stage coordinates are absolute to all screens (eg only leftmost screen contains
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
		setMaximized(isMaximized()==ALL ? NONE : ALL);
	}

	/** @return value of property fullscreen of this window */
	public boolean isFullscreen() {
		return s.isFullScreen();
	}

	/**
	 * Sets setFullscreen mode on (true) and off (false)
	 *
	 * @param val - true to go setFullscreen, - false to go out of setFullscreen
	 */
	public void setFullscreen(boolean val) {
		FullProp.set(val);
		s.setFullScreen(val);
	}

	/** Change between on/off setFullscreen state */
	public void toggleFullscreen() {
		setFullscreen(!isFullscreen());
	}

	/**
	 * Specifies the text to show when a user enters full screen mode, usually
	 * used to indicate the way a user should go about exiting out of setFullscreen
	 * mode. A value of null will result in the default per-locale message being
	 * displayed. If set to the empty string, then no message will be displayed.
	 * If an application does not have the proper permissions, this setting will
	 * be ignored.
	 */
	public void setFullScreenExitHint(String text) {
		s.setFullScreenExitHint(text);
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
		if (!APP.ui.getSnapping().get() || resizing.get()!=Resize.NONE) return;

		double S = APP.ui.getSnapDistance().get();

		// snap to screen edges (x and y separately)
		double SWm = screen.getBounds().getMinX();
		double SHm = screen.getBounds().getMinY();
		double SW = screen.getBounds().getMaxX();
		double SH = screen.getBounds().getMaxY();
		// x
		if (abs(s.getX() - SWm)<S)
			snapLeft();
		else if (abs(s.getX() + s.getWidth() - SW)<S)
			snapRight();
		// y
		if (abs(s.getY() - SHm)<S)
			snapUp();
		else if (abs(s.getY() + s.getHeight() - SH)<S)
			snapDown();

		// snap to other window edges
		for (javafx.stage.Window w : Stage.getWindows()) {
			if (!w.getProperties().containsKey(Window.keyWindowAppWindow)) continue;

			double WXS = w.getX() + w.getWidth();
			double WXE = w.getX();
			double WYS = w.getY() + w.getHeight();
			double WYE = w.getY();

			// x
			if (Math.abs(WXS - s.getX())<S)
				s.setX(WXS);
			else if (Math.abs(s.getX() + s.getWidth() - WXE)<S)
				s.setX(WXE - s.getWidth());
			// y
			if (Math.abs(WYS - s.getY())<S)
				s.setY(WYS);
			else if (Math.abs(s.getY() + s.getHeight() - WYE)<S)
				s.setY(WYE - s.getHeight());
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
		if (isFullscreen()) return;
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
	 * size will not be remembered and the window will revert back to previous
	 * size during certain graphical operations like reposition.
	 * <p>
	 * Its not recommended to use this method for maximizing. Use maximize(),
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
	public void setXYSize(double x, double y, double width, double height) {
		if (isFullscreen()) return;
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
	}

	/**
	 * @param width horizontal size of the window
	 * @param height vertical size of the window
	 */
	public void setSize(double width, double height) {
		if (isFullscreen()) return;
		s.setWidth(width);
		s.setHeight(height);
		W.set(s.getWidth());
		H.set(s.getHeight());
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
		setSize(w, h);
		setXY(w/2, h/2);
	}

	/** Sets the window visible and focuses it. */
	public void show() {
		s.show();
		focus();
	}

	/** Sets the window invisible and focuses it. */
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