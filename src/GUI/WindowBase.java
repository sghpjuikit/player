
package GUI;

import Configuration.Configuration;
import GUI.objects.PopOver.PopOver;
import PseudoObjects.Maximized;
import java.util.ArrayList;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import main.App;

/**
 * Customized Stage, window of the application.
 */
public class WindowBase {
    
    final DoubleProperty WProp = new SimpleDoubleProperty(100);
    final DoubleProperty HProp = new SimpleDoubleProperty(100);
    final DoubleProperty XProp = new SimpleDoubleProperty(0);
    final DoubleProperty YProp = new SimpleDoubleProperty(0);
    final BooleanProperty ResProp = new SimpleBooleanProperty(true);
    final ObjectProperty<Maximized> MaxProp = new SimpleObjectProperty(Maximized.NONE);
    final BooleanProperty FullProp = new SimpleBooleanProperty(false);
    
    Stage s = new Stage();
    boolean main;
    
    public WindowBase() {
        this(false);
    }
    public WindowBase(boolean isMain) {
        main = isMain;
        s.initStyle(StageStyle.UNDECORATED);
        s.setFullScreenExitHint(Configuration.fullscreenExitHintText);
        
        if(main) {    
            WProp.addListener((observable,oldV,newV) -> 
                    Configuration.windowWidth = (double)newV);
            HProp.addListener((observable,oldV,newV) -> 
                    Configuration.windowHeight = (double)newV);
            XProp.addListener((observable,oldV,newV) -> 
                    Configuration.windowPosX = (double)newV);
            YProp.addListener((observable,oldV,newV) -> 
                    Configuration.windowPosY = (double)newV);
            s.iconifiedProperty().addListener((observable,oldV,newV) -> 
                    Configuration.windowMinimized = newV );
            MaxProp.addListener((observable,oldV,newV) -> 
                    Configuration.windowMaximized = newV);
            FullProp.addListener((observable,oldV,newV) -> 
                    Configuration.windowFullscreen = newV);
            ResProp.addListener((observable,oldV,newV) -> 
                    Configuration.windowResizable = newV);
            s.alwaysOnTopProperty().addListener((observable,oldV,newV) -> 
                    Configuration.windowAlwaysOnTop = newV);   
        }
    }
    
    /**
     * Initializes the last remembered state of the window represented by this
     * application's settings.
     * Mostly needed during window initialization. This method applies last
     * remembered state. Can be also used as a refresh.
     * Doesnt affect content of the window.
     */
    public void update() {
        s.setFullScreenExitHint(Configuration.fullscreenExitHintText);
        s.setOpacity(Window.windowOpacity);
        
        if(main) {
            WProp.set(Configuration.windowWidth);
            HProp.set(Configuration.windowHeight);
            XProp.set(Configuration.windowPosX);
            YProp.set(Configuration.windowPosY);
            ResProp.set(Configuration.windowResizable);
            MaxProp.set(Configuration.windowMaximized);
            FullProp.set(Configuration.windowFullscreen);
            
        }
        
        // the order is importantif (main) setMinimized(Configuration.windowAlwaysOnTop);
        s.setWidth(WProp.get());
        s.setHeight(HProp.get());
        s.setX(XProp.get());
        s.setY(YProp.get());
        setResizable(ResProp.get());
        if (main) setMinimized(Configuration.windowMinimized);
        setMaximized(MaxProp.get());
        setFullscreen(FullProp.get());
        if (main) setAlwaysOnTop(Configuration.windowAlwaysOnTop);
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
    
    public boolean isVisible() {
        return s.isShowing();
    }
    public void setVisible(boolean val) {
        if(val) s.show();
        else s.hide();
    }
    /**
     * Property description:
     * Defines whether the Stage is resizable or not by the user. Programatically
     * you may still change the size of the Stage. This is a hint which allows
     * the implementation to optionally make the Stage resizable by the user. 
     * @return the value of the property resizable.
     */
    public boolean isResizable() {
        return s.isResizable();
    }
    /**
     * Sets the value of the property resizable.
     * Property description:
     * Defines whether the Stage is resizable or not by the user. Programatically
     * you may still change the size of the Stage. This is a hint which allows
     * the implementation to optionally make the Stage resizable by the user. 
     * @param val 
     */
    public void setResizable(boolean val) {
        ResProp.set(val);
        s.setResizable(val);
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
        // but preserve onTopProperty
        boolean aOt = s.isAlwaysOnTop();
        s.setAlwaysOnTop(true);
        Platform.runLater(()->s.setAlwaysOnTop(aOt));       // is runLater a good idea here?
    }
    /**
     * @return the value of the property minimized.
     */
    public boolean isMinimized() {
        return App.getWindowOwner().s.isIconified();   // act as main window
//        return s.isIconified();
    }
    /** Minimizes window. */
    public void minimize() {
        setMinimized(true);
    }
    /** Sets the value of the property minimized. */
    public void setMinimized(boolean val) {
        // this would minimize window normally but we dont want that since it
        // is not supported when windows have the same owner
//        s.setIconified(val);
        Configuration.windowMinimized = val;
        App.getWindowOwner().s.setIconified(val);    // minimize app
        // focus when deminimizing
        if(!val) focus();
    }
    /** 
     * Minimize/deminimize main application window. Switches between ALL and
     * NONE maximize states. 
     */
    public void toggleMinimize() {
        setMinimized(!isMinimized());
    }
    /** @return the value of the property maximized. */
    public Maximized isMaximised() {
        return MaxProp.get();
    }
    /**
     * Sets maximized state of this window to provided state. If the window is
     * in full screen mode it will revert to non full screen.
     * @param state 
     */
    public void setMaximized(Maximized state) {        
        if(isFullscreen()) return;
        MaxProp.set(state);
        switch (state) {
            case ALL:
            case LEFT:
            case RIGHT:
            case LEFT_TOP:
            case RIGHT_TOP:
            case LEFT_BOTTOM:
            case RIGHT_BOTTOM:
                            WProp.set(s.getWidth());
                            HProp.set(s.getHeight());
                            XProp.set(s.getX());
                            YProp.set(s.getY());
                            break;
            case NONE:      break;
            default:
        }
        switch (state) {
            case ALL:           maximizeAll();          break;
            case LEFT:          maximizeLeft();         break;
            case RIGHT:         maximizeRight();        break;
            case LEFT_TOP:      maximizeLeftTop();      break;
            case RIGHT_TOP:     maximizeRightTop();     break;
            case LEFT_BOTTOM:   maximizeLeftBottom();   break;
            case RIGHT_BOTTOM:  maximizeRightBottom();  break;
            case NONE:          demaximize();           break;
            default:
        }
    }
    private void maximizeAll() {
        s.setX(Screen.getPrimary().getVisualBounds().getMinX());
        s.setY(Screen.getPrimary().getVisualBounds().getMinY());
        s.setWidth(Screen.getPrimary().getVisualBounds().getWidth());
        s.setHeight(Screen.getPrimary().getVisualBounds().getHeight());
    }
    private void maximizeRight() {
        s.setX(Screen.getPrimary().getVisualBounds().getMinX() + Screen.getPrimary().getVisualBounds().getWidth()/2);
        s.setY(Screen.getPrimary().getVisualBounds().getMinY());
        s.setWidth(Screen.getPrimary().getVisualBounds().getWidth()/2);
        s.setHeight(Screen.getPrimary().getVisualBounds().getHeight());
    }
    private void maximizeLeft() {
        s.setX(Screen.getPrimary().getVisualBounds().getMinX());
        s.setY(Screen.getPrimary().getVisualBounds().getMinY());
        s.setWidth(Screen.getPrimary().getVisualBounds().getWidth()/2);
        s.setHeight(Screen.getPrimary().getVisualBounds().getHeight());
    }
    private void maximizeLeftTop() {
        s.setX(Screen.getPrimary().getVisualBounds().getMinX());
        s.setY(Screen.getPrimary().getVisualBounds().getMinY());
        s.setWidth(Screen.getPrimary().getVisualBounds().getWidth()/2);
        s.setHeight(Screen.getPrimary().getVisualBounds().getHeight()/2);
    }
    private void maximizeRightTop() {
        s.setX(Screen.getPrimary().getVisualBounds().getMinX() + Screen.getPrimary().getVisualBounds().getWidth()/2);
        s.setY(Screen.getPrimary().getVisualBounds().getMinY());
        s.setWidth(Screen.getPrimary().getVisualBounds().getWidth()/2);
        s.setHeight(Screen.getPrimary().getVisualBounds().getHeight()/2);
    }
    private void maximizeLeftBottom() {
        s.setX(Screen.getPrimary().getVisualBounds().getMinX());
        s.setY(Screen.getPrimary().getVisualBounds().getMinY() + Screen.getPrimary().getVisualBounds().getHeight()/2);
        s.setWidth(Screen.getPrimary().getVisualBounds().getWidth()/2);
        s.setHeight(Screen.getPrimary().getVisualBounds().getHeight()/2);
    }
    private void maximizeRightBottom() {
        s.setX(Screen.getPrimary().getVisualBounds().getMinX() + Screen.getPrimary().getVisualBounds().getWidth()/2);
        s.setY(Screen.getPrimary().getVisualBounds().getMinY() + Screen.getPrimary().getVisualBounds().getHeight()/2);
        s.setWidth(Screen.getPrimary().getVisualBounds().getWidth()/2);
        s.setHeight(Screen.getPrimary().getVisualBounds().getHeight()/2);
    }
    
    private void demaximize() {
        MaxProp.set(Maximized.NONE);
        setSize(WProp.get(),HProp.get());
        setLocation(XProp.get(),YProp.get());
    }
   /** 
    * Maximize/demaximize main application window. Switches between ALL and
    * NONE maximize states.
    */    
    public void toggleMaximize() {
        if (isMaximised() == Maximized.ALL)
            setMaximized(Maximized.NONE);
        else
            setMaximized(Maximized.ALL);
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
        Configuration.fullscreenExitHintText = text;
    }
    
    private void snapUp() {
        s.setY(0);
    }
    private void snapDown() {
        s.setY(Screen.getPrimary().getVisualBounds().getHeight() - s.getHeight());
    }
    private void snapLeft() {
        s.setX(0);
    }    
    private void snapRight() {
        s.setX(Screen.getPrimary().getVisualBounds().getWidth() - s.getWidth());
    }
    
    /**
     * Sets position of the window on the screen.
     * WARNING: Dont use getStage().setX() and similar !
     * This method is weak solution to inability to override setX(), setY()
     * methods of Stage. Not using this may cause the window not revert to its
     * remembered previous state during demaximization.
     * If the window is in full screen mode, this method is no-op.
     * @param X
     * @param Y
     */
    public void setLocation(double X, double Y) {
        if (isFullscreen()) return;
        MaxProp.set(Maximized.NONE);
        XProp.set(s.getX());
        YProp.set(s.getY());
        s.setX(X);
        s.setY(Y);
        
        
        double SW = Screen.getPrimary().getVisualBounds().getWidth();
        double SH = Screen.getPrimary().getVisualBounds().getHeight();
        double S = GUI.snapDistance;
        
//        // snap to screen edges (x and y separately)
        if (GUI.snapping) {  
            // x snapping
            if (Math.abs(s.getX())<S)
                snapLeft();
            else if (Math.abs(s.getX()+s.getWidth() - SW) < S)
                snapRight();
            // y snapping
            if (Math.abs(s.getY())<S)
                snapUp();
            else if (Math.abs(s.getY()+s.getHeight() - SH) < S)
                snapDown();
        }
        
        // snap to other window edges
        for(Window w: ContextManager.windows) {
            double WXS = w.getX()+w.getWidth();
            double WXE = w.getX();
            double WYS = w.getY()+w.getHeight();
            double WYE = w.getY();
        
            // snap to edges (x and y separately)
            if (GUI.snapping) {  
                // x snapping
                if (Math.abs(WXS - s.getX())<S)
                    s.setX(WXS);
                else if (Math.abs(s.getX()+s.getWidth() - WXE) < S)
                    s.setX(WXE - s.getWidth());
                // y snapping
                if (Math.abs(WYS - s.getY())<S)
                    s.setY(WYS);
                else if (Math.abs(s.getY()+s.getHeight() - WYE) < S)
                    s.setY(WYE - s.getHeight());    
            }
        }
    }
    
    public void setLocationCenter() {
        double x = Screen.getPrimary().getVisualBounds().getWidth() / 2 - getWidth() / 2;
        double y = Screen.getPrimary().getVisualBounds().getHeight() / 2 - getHeight() / 2;
        setLocation(x, y);
    }
    
    /**
     * Sets size of the window.
     * Always use this over setWidth(), setHeight(). Not using this method will
     * result in improper behavior during resizing - more specifically - the new
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
    public void setSize( double width, double height) {
        if (isFullscreen() || !isResizable()) return;
        s.setWidth(width);
        s.setHeight(height);
        WProp.set(s.getWidth());
        HProp.set(s.getHeight());
    }
    
    /** Sets the window visible and focuses it. */
    public void show() {
        s.show();
        // we need to focus the window
        // we need to defer the focusing for later or we risk setting setOnTop
        // to true permanently
        Platform.runLater(()->focus());     // cannot be done better? investigate
    }
    
    public void close() {
        if(main) {
            // close all pop overs first (or we risk an exception and not closing app
            // properly - PopOver bug
            new ArrayList<>(PopOver.active_popups).forEach(PopOver::hideImmediatelly);
            // act as main window and close whole app
            App.getWindowOwner().close();
        }  
        else s.close();
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
    
    
    
}