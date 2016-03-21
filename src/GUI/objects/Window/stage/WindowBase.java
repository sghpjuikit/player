
package gui.objects.Window.stage;

import java.util.List;

import javafx.beans.property.*;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import gui.Gui;
import gui.objects.Window.Resize;
import util.access.CyclicEnum;
import util.dev.Dependency;
import util.graphics.Util;

import static gui.objects.Window.stage.WindowBase.Maximized.ALL;
import static gui.objects.Window.stage.WindowBase.Maximized.NONE;
import static java.lang.Math.abs;
import static javafx.stage.StageStyle.UNDECORATED;
import static main.App.APP;
import static util.async.Async.run;
import static util.async.Async.runLater;

/**
 * Customized Stage, window of the application.
 * <p>
 * <p>
 * Screen depended methods:
 * <br>
 * Several features are screen dependent (like maximization) as they behave
 * differently per screen. Their methods are annotated with {@link Dependency}
 * annotation set to value SCREEN.
 * <p>
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
    private double demax_x = 0; // 0-1
    private double demax_y = 0; // 0-1

    Stage s = new Stage();

    /** Indicates whether the window has focus. Read-only. Use {@link #focus()}. */
    public final ReadOnlyBooleanProperty focused = s.focusedProperty();
    /** Indicates whether this window is always on top. Window always on top
      * will not hide behind other windows. */
    public final ReadOnlyBooleanProperty alwaysOnTop = s.alwaysOnTopProperty();
    /** Indicates whether this window is in fullscreen. */
    public final ReadOnlyBooleanProperty fullscreen = s.fullScreenProperty();
    /** Indicates whether this window is maximized. */
    public final ReadOnlyObjectProperty<Maximized> maximized = MaxProp.getReadOnlyProperty();
    /** Indicates whether the window is being moved. */
    public final ReadOnlyBooleanProperty moving = isMoving.getReadOnlyProperty();
    /** Indicates whether and how the window is being resized. */
    public final ReadOnlyObjectProperty<Resize> resizing = isResizing.getReadOnlyProperty();
    /** Defines whether this window is resizable. Programatically it is still
      * possible to change the size of the Stage. */
    public final BooleanProperty resizable = s.resizableProperty();



    public WindowBase() {
        this(null, UNDECORATED);
    }

    public WindowBase(Stage owner, StageStyle style) {
        if(s!=null) s.initOwner(owner);
        if(style!=null) s.initStyle(style);
        s.setFullScreenExitHint("");
    }

    /**
     * Returns to last remembered state.
     * Needed during window initialization.
     * Doesnt affect content of the window.
     */
    public void update() {
        s.setWidth(W.get());
        s.setHeight(H.get());

        List<Screen> scrs = Screen.getScreens();
        Rectangle2D psb = Screen.getPrimary().getBounds();
        double sxmin = scrs.stream().mapToDouble(s->s.getBounds().getMinX()).min().orElse(psb.getMinX());
        double symin = scrs.stream().mapToDouble(s->s.getBounds().getMinY()).min().orElse(psb.getMinY());
        double sxmax = scrs.stream().mapToDouble(s->s.getBounds().getMaxX()).min().orElse(psb.getMaxX());
        double symax = scrs.stream().mapToDouble(s->s.getBounds().getMaxY()).min().orElse(psb.getMaxY());
        // prevents out of screen position
        if(Y.get()+H.get()<symin) Y.setValue(symin);
        if(Y.get()>symax) Y.setValue(symax);
        if(X.get()+W.get()<sxmin) X.setValue(sxmin);
        if(X.get()>sxmax) X.setValue(sxmax);

        s.setX(X.get());
        s.setY(Y.get());
        screen = Util.getScreen(getCenterXY()); // update screen
        demax_x = (s.getX()-screen.getBounds().getMinX())/screen.getBounds().getWidth();  // just in case
        demax_x = (s.getY()-screen.getBounds().getMinY())/screen.getBounds().getHeight(); // -||-

        // we need to refresh maximized value so set it to NONE and back
        Maximized m = MaxProp.get();
        setMaximized(Maximized.NONE);
        setMaximized(m);

        // setFullscreen(FullProp.get()) produces a bug probably because the
        // window is not yet ready. Delay execution. Avoid the whole process
        // when the value is not true
        if(FullProp.get()) run(322, ()->setFullscreen(true));
    }

    /**
     * WARNING: Dont use the stage for positioning, maximizing and other
     * functionalities already defined in this class!!
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
        return s.getX()+getWidth()/2;
    }

    public double getCenterY() {
        return s.getY()+getHeight()/2;
    }

    // cached, needs to be updated when size or position changes
    Screen screen = Screen.getPrimary();

    /** Sets screen to this window. It influences screen dependent features. */
    public void setScreen(Screen scr) {
        screen = scr;
    }

    /** Gets screen of this window. It influences screen dependent features. */
    public Screen getScreen() {
        return screen;
    }


    /**
     * The value of the property resizable
     * see {@link #setAlwaysOnTop(boolean)}
     * @return the value of the property resizable.
     */
    public boolean isAlwaysOnTop() {
        return s.isAlwaysOnTop();
    }

    /**
     * Sets the value of the property is AlwaysOnTop.
     * Property description:
     * Defines behavior where this window always stays on top of other windows.
     * @param val
     */
    public void setAlwaysOnTop(boolean val) {
        s.setAlwaysOnTop(val);
    }

    public void toggleAlwaysOnTOp() {
        s.setAlwaysOnTop(!s.isAlwaysOnTop());
    }

    /** Brings the window to front if it was behind some window on the desktop.*/
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
            APP.taskbarIcon.forbidIconify = true;
        if(s.getOwner()!=null && s.getOwner() instanceof Stage) {
            ((Stage)s.getOwner()).setIconified(val);
        } else {
            s.setIconified(val);
        }

//        if(val && Window.WINDOWS.stream().allMatch(w -> w.isMinimized())) APP.taskbarIcon.iconify(val);
        if(val) {
            APP.taskbarIcon.iconify(val);
        }

        // focus when deminimizing
        if(!val) focus();
            runLater(() -> APP.taskbarIcon.forbidIconify = false);
    }

    /**
     * Minimize/deminimize main application window. Switches between ALL and
     * NONE maximize states.
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
     * @param val
     */
    public void setMaximized(Maximized val) {
        // no-op if fullscreen
        if(isFullscreen()) return;

        // prevent pointless change
        Maximized old = isMaximized();
        if(old==val) return;

        // remember window state if entering from non-maximized state
        if(old==Maximized.NONE) {
            // this must not execute when val==NONE but that will not happen here
            W.set(s.getWidth());
            H.set(s.getHeight());
            X.set(s.getX());
            Y.set(s.getY());
            demax_x = (s.getX()-screen.getBounds().getMinX())/screen.getBounds().getWidth();
            demax_y = (s.getY()-screen.getBounds().getMinY())/screen.getBounds().getHeight();
        }
        // remember state
        MaxProp.set(val);

        // apply
        switch (val) {
            case ALL:           maximizeAll();          break;
            case LEFT:          maximizeLeft();         break;
            case RIGHT:         maximizeRight();        break;
            case LEFT_TOP:      maximizeLeftTop();      break;
            case RIGHT_TOP:     maximizeRightTop();     break;
            case LEFT_BOTTOM:   maximizeLeftBottom();   break;
            case RIGHT_BOTTOM:  maximizeRightBottom();  break;
            case NONE:          demaximize();           break;
        }
    }
    private void maximizeRightBottom() {
        stageresizeRelocate(
            screen.getBounds().getMinX() + screen.getBounds().getWidth()/2,
            screen.getBounds().getMinY() + screen.getBounds().getHeight()/2,
            screen.getBounds().getWidth()/2,
            screen.getBounds().getHeight()/2
        );
    }
    private void maximizeAll() {
        stageresizeRelocate(
            screen.getBounds().getMinX(),
            screen.getBounds().getMinY(),
            screen.getBounds().getWidth(),
            screen.getBounds().getHeight()
        );
    }
    private void maximizeRight() {
        stageresizeRelocate(
            screen.getBounds().getMinX() + screen.getBounds().getWidth()/2,
            screen.getBounds().getMinY(),
            screen.getBounds().getWidth()/2,
            screen.getBounds().getHeight()
        );
    }
    private void maximizeLeft() {
        stageresizeRelocate(
            screen.getBounds().getMinX(),
            screen.getBounds().getMinY(),
            screen.getBounds().getWidth()/2,
            screen.getBounds().getHeight()
        );
    }
    private void maximizeLeftTop() {
        stageresizeRelocate(
            screen.getBounds().getMinX(),
            screen.getBounds().getMinY(),
            screen.getBounds().getWidth()/2,
            screen.getBounds().getHeight()/2
        );
    }
    private void maximizeRightTop() {
        stageresizeRelocate(
            screen.getBounds().getMinX() + screen.getBounds().getWidth()/2,
            screen.getBounds().getMinY(),
            screen.getBounds().getWidth()/2,
            screen.getBounds().getHeight()/2
        );
    }
    private void maximizeLeftBottom() {
        stageresizeRelocate(
            screen.getBounds().getMinX(),
            screen.getBounds().getMinY() + screen.getBounds().getHeight()/2,
            screen.getBounds().getWidth()/2,
            screen.getBounds().getHeight()/2
        );
    }
    private void stageresizeRelocate(double x, double y, double w, double h) {
        s.setX(x);
        s.setY(y);
        s.setWidth(w);
        s.setHeight(h);
    }
    private void demaximize() {
        MaxProp.set(NONE);
        // Normally we would use last position, but it is possible that demaximization happens
        // not to the same screen as it was maximized on (screen can manually be changed, which
        // is valid behavior in multi-screen maximization cycling (on Windows WIN+LEFT/RIGHT)).
        // Because stage coordinates are absolute to all screens (eg only leftmost screen contains
        // coordinate 0, demazimized window could be on the wrong (original) screen. Hence we
        // remember screen-relative position (offset) and relatively so (in 0-1 fraction of screen
        // width (otherwise we again risk outside of screen position))
        // and calculate new one: screen+offset setXY(X.get(),Y.get());
        stageresizeRelocate(
            demax_x*screen.getBounds().getWidth() + screen.getBounds().getMinX(),
            demax_y*screen.getBounds().getHeight() + screen.getBounds().getMinY(),
            W.get(),
            H.get()
        );
    }
   /**
    * Maximize/demaximize main application window. Switches between ALL and
    * NONE maximize states.
    */
    public void toggleMaximize() {
        setMaximized(isMaximized()==ALL ? NONE : ALL);
    }

    /** @return value of property fulscreen of this window */
    public boolean isFullscreen() {
        return s.isFullScreen();
    }

    /**
     * Sets setFullscreen mode on (true) and off (false)
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
     * @param text
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

    /** @see #setX(double, boolean)  */
    public void setX(double x) {
        setXY(x, getY(), true);
    }

    /** @see #setX(double, double, boolean)  */
    public void setX(double x, boolean snap) {
        setXY(x, getY(), snap);
    }

    /** @see #setX(double, boolean)  */
    public void setY(double y) {
        setXY(getX(), y, true);
    }

    /** @see #setX(double, double, boolean)  */
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

    /** Centers this window on ita screen. */
    public void setXYScreenCenter() {
        double x = screen.getBounds().getWidth()/2 - getWidth()/2;
        double y = screen.getBounds().getHeight()/2 - getHeight()/2;
        setXY(x, y);
    }

    /** Returns screen x,y of the center of this window. */
    public Point2D getCenterXY() {
        return new Point2D(getCenterX(), getCenterY());
    }

    /**
     * Snaps window to edge of the screen or other window.
     * <p>
     * Executes snapping. Window will snap if snapping is allowed and if the
     * preconditions of window's state require snapping to be done.
     * <p>
     * Because convenience methods are provided that auto-snap on position
     * change, there is little use for calling this method externally.
     */
    public void snap() {
        // avoid snapping while isResizing. It leads to unwanted behavior
        // avoid when not desired
        if(!Gui.snapping.get() || resizing.get()!=Resize.NONE) return;

        double S = Gui.snapDistance.get();

        // snap to screen edges (x and y separately)
        double SWm = screen.getBounds().getMinX();
        double SHm = screen.getBounds().getMinY();
        double SW = screen.getBounds().getMaxX();
        double SH = screen.getBounds().getMaxY();
        // x
        if (abs(s.getX() - SWm) < S)
            snapLeft();
        else if (abs(s.getX()+s.getWidth() - SW) < S)
            snapRight();
        // y
        if (abs(s.getY() - SHm) < S)
            snapUp();
        else if (abs(s.getY()+s.getHeight() - SH) < S)
            snapDown();


        // snap to other window edges
        for(Window w: Window.WINDOWS) {
            double WXS = w.getX()+w.getWidth();
            double WXE = w.getX();
            double WYS = w.getY()+w.getHeight();
            double WYE = w.getY();

            // x
            if (Math.abs(WXS - s.getX())<S)
                s.setX(WXS);
            else if (Math.abs(s.getX()+s.getWidth() - WXE) < S)
                s.setX(WXE - s.getWidth());
            // y
            if (Math.abs(WYS - s.getY())<S)
                s.setY(WYS);
            else if (Math.abs(s.getY()+s.getHeight() - WYE) < S)
                s.setY(WYE - s.getHeight());
        }
    }

    /**
     * Sets position of the window on the screen.
     * <p>
     * Note: Always use methods provided in this class for isResizing and never
     * those in the Stage of this window.
     * <p>
     * If the window is in full screen mode, this method is no-op.
     *
     * @param x x coordinate for left upper corner
     * @param y y coordinate for left upper corner
     * @param snap flag for snapping to screen edge and other windows. Snapping
     * will be executed only if the window id not being resized.
     */
    @Dependency("must update screen")
    public void setXY(double x,double y, boolean snap) {
        if (isFullscreen()) return;
        MaxProp.set(Maximized.NONE);
        s.setX(x);
        s.setY(y);
        X.set(x);
        Y.set(y);
        screen = Util.getScreen(getCenterXY()); // update screen
        if(snap) snap();
    }

    /**
     * Sets size of the window.
     * Always use this over setWidth(), setHeight(). Not using this method will
     * result in improper behavior during isResizing - more specifically - the new
     * size will not be remembered and the window will revert back to previous
     * size during certain graphical operations like reposition.
     *
     * Its not recommended to use this method for maximizing. Use maximize(),
     * maximizeLeft(), maximizeRight() instead.
     *
     * This method is weak solution to inability to override setWidth(),
     * setHeight() methods of Stage.
     *
     * If the window is in full screen mode or !isResizable(), this method is no-op.
     * @param width
     * @param height
     */
    @Dependency("must update screen")
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
        // if(snap) snap();
        W.set(s.getWidth());
        H.set(s.getHeight());
        screen = Util.getScreen(getCenterXY()); // update screen
    }

    @Dependency("must update screen")
    public void setSize(double width, double height) {
        if (isFullscreen()) return;
        s.setWidth(width);
        s.setHeight(height);
        W.set(s.getWidth());
        H.set(s.getHeight());
        screen = Util.getScreen(getCenterXY()); // update screen
    }

    /**
     * Sets initial size and location by invoking the {@link #setSize} and
     * {@link #setLocation()} method. The initial size values are primary screen
     * size divided by half and the location will be set so the window is
     * center aligned on the primary screen.
     */
    public void setXYSizeInitial() {
        double w = screen.getBounds().getWidth()/2;
        double h = screen.getBounds().getHeight()/2;
        setSize(w,h);
        setXY(w/2,h/2);
    }

    /** Sets the window visible and focuses it. */
    public void show() {
        s.show();
        focus();
    }

    public void hide() {
        s.hide();
    }

    public boolean isShowing() {
        return s.isShowing();
    }

    public void close() {
       s.close();
    }


    /**
     * Checks whether the GUI has completed its initialization.
     * If true is returned, the GUI - the Stage and Scene will never be
     * null. Otherwise, some
     * operations might be unupported and Stage or Scene will be null. Window and Stage
     * will never be null, but their state might not be optimal for carrying out
     * operations - this method guarantees that optimality.
     * It is recommended to run this check before executing operations operating
     * on Window, Stage and Scene objects of he application and handle the case,
     * when the initialization has not been completed differently.
     * This method helps avoid exceptions resulting from uninitialized GUI state.
     * @return
     */
    public boolean isInitialized() {
        return (!(getStage() == null ||
                  getStage().getScene() == null ||
                  getStage().getScene().getRoot() == null));
    }

    /** State of window maximization. */
    public static enum Maximized implements CyclicEnum<Maximized> {
        ALL,
        LEFT,
        RIGHT,
        LEFT_TOP,
        RIGHT_TOP,
        LEFT_BOTTOM,
        RIGHT_BOTTOM,
        NONE;
    }

}