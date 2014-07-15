/**
 * Copyright (c) 2013, ControlsFX
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of ControlsFX, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL CONTROLSFX BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package GUI.objects.PopOver;

import GUI.WindowBase;
import static GUI.objects.PopOver.PopOver.ScreenCentricPos.ScreenBottomLeft;
import static GUI.objects.PopOver.PopOver.ScreenCentricPos.ScreenBottomRight;
import static GUI.objects.PopOver.PopOver.ScreenCentricPos.ScreenCenter;
import static GUI.objects.PopOver.PopOver.ScreenCentricPos.ScreenTopLeft;
import static GUI.objects.PopOver.PopOver.ScreenCentricPos.ScreenTopRight;
import GUI.objects.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.animation.FadeTransition;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.PopupControl;
import javafx.scene.control.Skin;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import javafx.scene.input.MouseEvent;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static javafx.scene.input.MouseEvent.MOUSE_RELEASED;
import javafx.stage.Screen;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import static javafx.stage.WindowEvent.WINDOW_HIDING;
import javafx.util.Duration;
import main.App;

/**
 * The PopOver control provides detailed information about an owning node or 
 * simply displays content in a
 * popup window. The popup window has a very lightweight appearance (no default
 * window decorations) and an arrow pointing at the owner. Due to the nature of
 * popup windows the PopOver will move around with the parent window when the
 * user drags it. <br>
 * <center> <img src="popover.png"/> </center>
 * <br>
 * <center> <img src="popover-detached.png"/> </center> <br>
 * The following image shows a popover with an accordion content node. 
 * <br>
 * <center> <img src="popover-accordion.png"/> </center> <br>
 * PopOver controls are automatically resizing themselves when the content node
 * changes its size.
 * <br><p><pre>
 * The popover has two modes of behavior marked with detached property.
 * Detached popover:
 *  - has no arrow
 *  - can be dragged and moved
 *  - contains a header with title and some controls.
 *  - autohide off
 * </pre>
 * It is a draggable more standalone window compared to
 * normal mode which comes close to tooltip in terms of functionality.
 * <p>
 * The header can still be visible in normal mode. To be precise, the header is
 * visible when popover is detached or when at least one of the following is not
 * empty: header icons list, title text.
 * <p>
 * The close button however appears only in detached mode. This is because in
 * normal mode autohide is usually on. Therefore in normal mode, close button is
 * substituted with autohide icon which allows setting autohiding on/off. It
 * 'pins' the popover to the screen.
 * <br><p><p>
 * Popover can become
 * detached when dragged by mouse but only if it is detachable. Detaching is not
 * reversible, respectively it is only during the initial mouse drag that detached
 * the popover - if the popover is returned to the original position.
 * <br><p><p>
 * There is a notion of owner node and owner window. The popover tries to
 * act as a child to the Node and Window.
 * <p>
 * Popover will move with the owner as if locked onto its position. The relocation
 * takes place when Node owner changes location within its parent or when window
 * owner changes its location within the screen or its size.
 * The popover
 * will hide when window owner hides or when node owner is detached from the
 * scene graph (specifically when its Scene changes) or when node owner's turns
 * invisible.
 * <p>
 * Both are set automatically when any of the show() methods is called depending
 * on the parameter of the show method. If Node is specified, it will become an
 * owner node and the window the node is within will become owner window. If
 * only window is specified it becomes owner window and owner node is left empty.
 * <br><p><p>
 * There is a lot of use cases for popover. The content can vary greatly from
 * simple text or icon to a very complex components in which case it is easier 
 * to think of the popover as standalone window. 
 * <p>
 * Below are some of the expected use cases along with the recommended 
 * configurations:
 * <pre>
 * Informative popover with static content - more sophisticated tooltip
 *  - autohide : true
 *  - hide on click : true
 *  - move with owner : optional
 *  - tip: hide when owner receives any kind of event
 *  - hiding immediatelly or disabling animations might be considered
 *  - detachable : false
 *  - detached : false or true with no arrow
 * 
 * Variation tips:
 *  - autohide : optional but inverse with : close on click (see {@link #setHideOnClick(boolean)})
 *  - hide on click : see above or depending on content
 *  - move with owner : true
 * 
 * Small content reactive popover - an alternative to context menus or default popups
 *  - autohide : true
 *  - hide on click : false or false with custom implementation through popover's content's mouse click event handler
 *  - move with owner : true
 *  - detachable : true
 *  - detached : false
 * 
 * Sophisticated content - alternative to full fledged window
 *  - autohide : false
 *  - hide on click : false
 *  - move with owner : false
 *  - arrow visible : false or depending on context
 *  - detached : true or depending on context
 * </pre>
 * <br><p>
 * The popover supports nested instances - popovers opened from other 
 * popovers. However the owning window will be the owning window of the parent
 * popup, not the popup itself. Although Node inside a popup can be an owner node
 * for another popup.
 */
public class PopOver extends PopupControl {

    private static final String STYLE_CLASS = "popover";
    public static List<PopOver> active_popups = new ArrayList(); 

/******************************************************************************/

    /**
     * Creates simple help popover designed as a tooltip for help buttons.
     * <p>
     * The content of the popover is specified text and title: "Help". 
     * <pre>
     *  For example: "Left click : Executes action\n
     *                Help button click : Shows hint on using this feature"
     * </pre>
     * The created popover is not detached, not detachable, hide on click is true,
     * autohide true, rest default.
     * <p>
     * Tip: make use of {@link #setChildPopup(GUI.objects.PopOver.PopOver)}
     * <p>
     * Tip: Associate help popovers with buttons marked with question mark or
     * similar icon.
     * <p>
     * Tip: Help popover is intended to help user with familiarizing with the
     * functionalities of the application and as such is not expected to be
     * showed frequently.
     * It is recommended to have only single instance per help button, meaning 
     * that if the popover is requested multiple times, it will only be constructed
     * once. Another recommended practice is to create the popover lazily - on
     * demand when requested, leaving the initial value as null.
     * For example lets have a global reference to the popup and lets initialize
     * it to null. When the popover is requested do a null check before the show()
     * method is called and construct the popover if it was null. Or construct
     * a method getHelpPopover() for this. Lazy singleton pattern.
     * <p>
     * Note: following the above advice and assuming the request for the popover
     * comes from button click (more precisely an event handler) it is likely
     * that the event handler will be written as lambda. In that case the 
     * reference to the popup will have to be global as the object referenced
     * from lambda must be effectively final and can not be reinitialized inside.
     * @param text
     * @return 
     */
    public static PopOver createHelpPopOver(String text) {
        PopOver p = new PopOver(new Text(text));
                p.setTitle("Help");
                p.setAutoHide(true);
                p.setHideOnClick(true);
                p.setDetachable(false);
        return p;
    }
    
    /**
     * Creates a pop over with a label as the content node.
     * Sets autoFix and consumeAutoHidingEvents to false.
     */
    public PopOver() {
        super();
        getStyleClass().add(STYLE_CLASS);
        setConsumeAutoHidingEvents(false);
        setAutoFix(false);
        
        
        // not working as advertized, disable for now...
//        ChangeListener<Object> repositionListener = (o, oldV, newV) -> {
//            if (isShowing() && !isDetached()) {
//                System.out.println("reposition listener fired");
//                show(ownerNode, getX(), getY());
//                adjustWindowLocation();
//            }
//        };
//        arrowSize.addListener(repositionListener);
//        cornerRadius.addListener(repositionListener);
//        arrowLocation.addListener(repositionListener);
//        arrowIndent.addListener(repositionListener);
    }

    /**
     * Creates a pop over with the given node as the content node.
     * Sets autoFix and consumeAutoHidingEvents to false.
     * @param content shown by the pop over
     */
    public PopOver(Node content) {
        this();
        setContentNode(content);
    }
    
    /**
     * Creates a pop over with the given node as the content node and provided
     * title text.
     * Sets autoFix and consumeAutoHidingEvents to false.
     * @param content shown by the pop over
     */
    public PopOver(String title, Node content) {
        this();
        setTitle(title);
        setContentNode(content);
    }
    
    @Override
    protected Skin<PopOver> createDefaultSkin() {
        return new PopOverSkin(this);
    }
    
    private final ObjectProperty<Node> contentNode = new SimpleObjectProperty<Node>(this, "contentNode") {
        @Override
        public void setValue(Node node) {
            Objects.requireNonNull(node, "content node can not be null");
            super.setValue(node);
        }
    };

    /**
     * Returns the content shown by the pop over.
     * @return the content node property
     */
    public final ObjectProperty<Node> contentNodeProperty() {
        return contentNode;
    }

    /**
     * Returns the content of this popover as previously set with 
     * {@link #setContentNode(javafx.scene.Node)}.
     * @return the content node
     * @see #contentProperty()
     */
    public final Node getContentNode() {
        return contentNodeProperty().get();
    }

    /**
     * Sets the content. There can only be one content. Old content will be 
     * removed.
     * @param content the new content node value
     * @see #contentProperty()
     * @throws  NullPointerException if param null.
     */
    public final void setContentNode(Node content) {
        Objects.requireNonNull(content);
        contentNodeProperty().set(content);
    }
    
/******************************************************************************/

    /** 
     * Hides this popup if it is not detached. Otherwise does nothing.
     * Equivalent to: if(!isDetached()) hide();
     */
    @Override
    public void hide() {
        if (!isDetached()) {
            if (isAnimated()) fadeOut();
            else hideImmediatelly();
        }
    }
    
    /** 
     * Hides this popup. If is animated, the hiding animation will be set off,
     * otherwise happens immediatelly. Detached property is ignored.
     * The hiding can be prevented during the animation time (by calling
     * any of the show methods) as the hiding takes place only when animation
     * finishes.
     */
    public void hideStrong() {
        if (isAnimated()) fadeOut();
        else hideImmediatelly();
    }
    
    /** 
     * Hides this popup immediatelly.
     * Use when the hiding animation is not desired (regardless the set value)
     * or could cause problems such as delaying application exit.
     * <p>
     * Developer note: this is the default hide implementation overriding the
     * one in the super class. It should not cause any delays and keep changes
     * to hiding mechanism to minimum as otherwise it could cause problems. For 
     * example casting javafx.stage.Window to PopOver (when it in fact is an
     * instance of this class) and calling modified hide() method has been
     * observed to cause serious problems.
     */
    public void hideImmediatelly() {
        active_popups.remove(this);
        uninstallMoveWith();
        super.hide();
    }    
    
/******************************************************************************/
    
    private void showThis(Node ownerNode, Window ownerWindow) {
        Objects.requireNonNull(ownerWindow);
        
        // we must set the owners so moving with owner behavior knows which
        // mode should be done
        if(ownerNode!=null) {
            this.ownerNode = ownerNode;
            this.ownerWindow = ownerNode.getScene().getWindow();
            System.out.println(this.ownerWindow instanceof PopOver);
            if(this.ownerWindow instanceof PopOver) {
                setParentPopup((PopOver)this.ownerWindow);
            }
        }
        else this.ownerWindow = ownerWindow;
        
        setDetached(false);
        
        // show the popup
        super.show(ownerWindow,0,0);
        active_popups.add(this);
        getSkin().getNode().setOpacity(isAnimated() ? opacityOldVal : 1);
        
        // initialize moving with owner behavior to respect set value
        initializeMovingBehavior(move_with_owner);
        
        // fixes 'broken' default hide on ESC functionality. Because hideOnESC
        // calls hide() method by default, there is no effect in detached mode
        // we fix this here, because we need default hide() implementation to
        // not close detached popup
        getScene().addEventHandler(KEY_PRESSED, e -> {
            if(e.getCode()==ESCAPE && isHideOnEscape() && isDetached()) 
                hideStrong();
        });
    }
    
    private void position(double x, double y) {
        setX(x);
        setY(y);
        
        // causes popup position itself on the screen with the most favourable
        // position (the furthest from any border)
        // do it after the popup is shown, after the desired cordinates are
        // set - those are needed, before adjusting location
        adjustArrowLocation();
        
        // move popover so the arrow points to the desired coordinates
        // avoid when no arrow
        if(getArrowSize()>0) adjustWindowLocation();
        
        // solves a small bug where the followng variables dont get initialized
        // and binding is currently impossible
        deltaThisX = getX();
        deltaThisY = getY();
        
        if (isAnimated()) fadeIn();
    }
    
    /**
     * Makes the pop over visible at the give location and associates it with
     * the given owner node. The x and y coordinate will be the target location
     * of the arrow of the pop over and not the location of the window.     * 
     * @param owner the owning node
     * @param x the x coordinate for the pop over arrow tip
     * @param y the y coordinate for the pop over arrow tip
     * @throws NullPointerException if owner param null or is not residing
     * within any {@link Window} - (its {@link getScene().getWindow()}) must not
     * return null
     */
    @Override
    public final void show(Node owner, double x, double y) {
        showThis(owner, owner.getScene().getWindow());
        Point2D a = owner.localToScreen(x,y);
        double X = a.getX() + owner.getBoundsInParent().getWidth()/2;
        double Y = a.getY() + owner.getBoundsInParent().getHeight()/2;
        position(X,Y);
    }
    
    /**
     * Shows popup. Equivalent to: show(owner,0,0);
     * @param owner
     */
    public final void show(Node owner) {
        show(owner,0,0);
    }
    
    /** Display at specified designated position relative to node. */
    public void show(Node owner, NodeCentricPos pos) {
        showThis(owner, owner.getScene().getWindow());
        double X = pos.calcX(owner, this)+owner.getBoundsInParent().getWidth()/2;
        double Y = pos.calcY(owner, this)+owner.getBoundsInParent().getHeight()/2;
        position(X,Y);
    }
    
    /** Display at specified screen coordinates */
    @Override
    public void show(Window window, double x, double y) {
        showThis(null, window);
        position(x, y);
    }
    
    /** Display at specified designated screen position */
    public void show(ScreenCentricPos pos) {
        showThis(null, App.getWindow().getStage());
        position(pos.calcX(this), pos.calcY(this));
        
        if(pos==ScreenBottomLeft || pos==ScreenBottomRight || pos==ScreenCenter 
                || pos==ScreenTopLeft || pos==ScreenTopRight)
            uninstallMoveWith();
        
    }

    @Override
    public void show(Window window) {
        showThis(null, window);
        uninstallMoveWith();
    }
    
/**************************** MOVE WITH OWNER *********************************/
    
    private boolean move_with_owner = true;
    
    /**
     * Sets moving with owner behavior on or off. Default on (true).
     * <p>
     * This functionality is characterized by the popup moving with the owning
     * Window or Node - its positioning relative to its owner becomes absolute.
     * <p>
     * There are two 'modes' decided automatically depending on the way this
     * popover was shown. The position can be absolute relative to the Node or
     * relative to the Window (or none). The first will reposition this popup
     * in order for it to stay with the Node (even when the Node only changes
     * its position within the Window that remained static). Second simply 
     * follows the Window and the third does nothing.
     * <p>
     * The 'mode' is decided based on the parameters of the show method that was
     * called.
     * Must never be called before the popover is shown.
     * @param val 
     */
    public void setMoveWithOwner(boolean val) {
        // avoid wasteful operation, continue only if values differ
        if(val != move_with_owner) {
            move_with_owner = val;
            
            // if is showing the behavior (one way or the other) was already initialized
            // and we need to change it. Otherwise, it needs to be applied from
            // show method as the initialisation can not precede show method
            // The behavior is uninitialized on hide so it will not be started twice
            if(isShowing()) {
                initializeMovingBehavior(val);
            }
        }
    }
    
    public boolean isMoveWithOwner() {
        return move_with_owner;
    }
    
    private void initializeMovingBehavior(boolean val) {
        // throw exception if illegal state
        Objects.requireNonNull(ownerWindow, "Error, popup window owner null"
                + " while isShowing. Cant apply moving with owner behavior");
        // install/uninstall correct 'mode'
        if(val) {
            if(ownerNode!=null) installMoveWithNode(ownerNode); // node mode
            else installMoveWithWindow(ownerWindow);            // window mode
        } else {
            uninstallMoveWith();    // uninstalls both
        }
    }

    // move with handling -------
    // owners
    private Node ownerNode = null;
    private Window ownerWindow = null;
    
    // variables for dragging behavior
    private double deltaX = 0;
    private double deltaY = 0;
    private double deltaThisX = 0;
    private double deltaThisY = 0;
    private boolean deltaThisLock = false;
    
    // listeners for relocating
    private final ChangeListener<Number> xListener = (o,oldX,newX) -> {
        if(ownerNode!=null)
            setX(deltaThisX+ownerNode.localToScreen(0, 0).getX()-deltaX);
    };
    private final ChangeListener<Number> yListener = (o,oldY,newY) -> {
        if(ownerNode!=null)
            setY(deltaThisY+ownerNode.localToScreen(0, 0).getY()-deltaY);
    };
    private final ChangeListener<Number> winXListener = (o,oldX,newX) -> {
        if(ownerWindow!=null)
            setX(deltaThisX+ownerWindow.getX()-deltaX);
    };
    private final ChangeListener<Number> winYListener = (o,oldY,newY) -> {
        if(ownerWindow!=null)
            setY(deltaThisY+ownerWindow.getY()-deltaY);
    };
    
    private void installMoveWithWindow(Window owner) {
        ownerNode = null;
        ownerWindow = owner;
        ownerWindow.xProperty().addListener(winXListener);
        ownerWindow.yProperty().addListener(winYListener);
        ownerWindow.widthProperty().addListener(winXListener);
        ownerWindow.heightProperty().addListener(winYListener);
        // uninstall just before window gets hidden to prevent possible illegal states
        ownerWindow.addEventFilter(WINDOW_HIDING, e -> visibilityListener.changed(null,null,null));
        // remember owner's position to monitor its position change
        deltaX = owner.getX();
        deltaY = owner.getY();
        installMonitoring();
    }
    private void installMoveWithNode(Node owner) {
        ownerNode = owner;
        ownerWindow = ownerNode.getScene().getWindow();
        ownerWindow.xProperty().addListener(xListener);
        ownerWindow.yProperty().addListener(yListener);
        ownerWindow.widthProperty().addListener(xListener);
        ownerWindow.heightProperty().addListener(yListener);
        ownerNode.layoutXProperty().addListener(xListener);
        ownerNode.layoutYProperty().addListener(yListener);
        // uninstall when Node is disconnected from scene graph to prevent possible illegal states
        ownerNode.sceneProperty().addListener(visibilityListener);
        ownerNode.visibleProperty().addListener(visibilityListener2);
        // remember owner's position to monitor its position change
        deltaX = ownerNode.localToScreen(0,0).getX();
        deltaY = ownerNode.localToScreen(0,0).getY();
        installMonitoring();
    }
    private void uninstallMoveWith() {
        if(ownerWindow!=null) {
            ownerWindow.xProperty().removeListener(winXListener);
            ownerWindow.yProperty().removeListener(winYListener);
            ownerWindow.widthProperty().removeListener(winXListener);
            ownerWindow.heightProperty().removeListener(winYListener);
        }
        if(ownerNode!=null) {
            ownerNode.layoutXProperty().removeListener(xListener);
            ownerNode.layoutYProperty().removeListener(yListener);
            ownerWindow.xProperty().removeListener(xListener);
            ownerWindow.yProperty().removeListener(yListener);
            ownerWindow.widthProperty().removeListener(xListener);
            ownerWindow.heightProperty().removeListener(yListener);
            ownerNode.sceneProperty().removeListener(visibilityListener);
            ownerNode.visibleProperty().removeListener(visibilityListener2);
        }
        ownerNode = null;
        ownerWindow = null;
        uninstallMonitoring();
    }
    
    // monitoring ----------
    // turn lock on/off to prevent dragging to change state
    EventHandler<MouseEvent> lockOnHandler = e -> deltaThisLock=true;
    EventHandler<MouseEvent> lockOffHandler = e -> deltaThisLock=false;
    // remember position for dragging functionality
    InvalidationListener deltaXListener = o->{ if(deltaThisLock) deltaThisX=getX(); };
    InvalidationListener deltaYListener = o->{ if(deltaThisLock) deltaThisY=getY(); };
    // hide on scene change
    ChangeListener<Scene> visibilityListener = (o,oldV,newV) -> { 
        if(newV != oldV) {
            uninstallMoveWith();
            hideStrong();
        } 
    };
    // hide on node owoner setVisible(false)
    ChangeListener<Boolean> visibilityListener2 = (o,oldV,newV) -> { 
        if(!newV) {
            uninstallMoveWith();
            hideStrong();
        } 
    };
    
    private void installMonitoring() {
        // this should have been handled like this:
        //     deltaThisX.bind(Bindings.when(deltaThisLock).then(xProperty()).otherwise(deltaThisX));
        // but the binding doesn seem to allow binding to itself or simply 'not
        // do' anything in the 'otherwise' part - there we need to maintain the
        // current value but it doesnt seem possible - it would really clean
        // this up a bit (way too many listeners for my taste)
        
        // maintain lock to prevent illegal position monitoring
        getScene().addEventHandler(MOUSE_PRESSED, lockOnHandler);
        getScene().addEventHandler(MOUSE_RELEASED, lockOffHandler);
        // monitor this' position for change if lock allows
        xProperty().addListener(deltaXListener);
        yProperty().addListener(deltaYListener);
        // fire listeners to initialize the values (listeners dont get fired when added)
        deltaXListener.invalidated(xProperty());
        deltaYListener.invalidated(yProperty());
        deltaThisX = getX();
        deltaThisY = getY();
    }
    private void uninstallMonitoring() {
        getScene().removeEventHandler(MOUSE_PRESSED, lockOnHandler);
        getScene().removeEventHandler(MOUSE_RELEASED, lockOffHandler);
        xProperty().removeListener(deltaXListener);
        yProperty().removeListener(deltaYListener);
    }
    
    
/******************************************************************************/
    
    private void adjustArrowLocation() {
        Bounds bounds = PopOver.this.getSkin().getNode().getBoundsInParent();
        
        double SW = Screen.getPrimary().getBounds().getWidth();
        double SH = Screen.getPrimary().getBounds().getHeight();
        double W = getWidth();
        double H = getHeight();
        double X = getX();
        double Y = getY();
        double right = SW-X;
        double left = X;
        double down = SH-Y;
        double up = Y;
        double vdiff = right-left;
        double hdiff = down-up;
        double h_content_screen_span = W/SW;
        double v_content_screen_span = H/SH;
        
        
        if(right>left) {
            if(down>up) {
                setArrowLocation(ArrowLocation.LEFT_TOP);
            } else {
                setArrowLocation(ArrowLocation.LEFT_BOTTOM);
            }
        } else {
            if(down>up) {
                setArrowLocation(ArrowLocation.RIGHT_TOP);
            } else {
                setArrowLocation(ArrowLocation.RIGHT_BOTTOM);
            }
        }
//        setArrowLocation(ArrowLocation.LEFT_TOP);  
    }
   /*
    * Move the window so that the arrow will end up pointing at the
    * target coordinates.
    */
    private void adjustWindowLocation() {
        Bounds bounds = PopOver.this.getSkin().getNode().getBoundsInParent();
        double X = getX();
        
        switch (getArrowLocation()) {
        case TOP_CENTER:
        case TOP_LEFT:
        case TOP_RIGHT:
            setX(X + bounds.getMinX() - computeXOffset());
            setY(getY() + bounds.getMinY() + getArrowSize());
            break;
        case LEFT_TOP:
        case LEFT_CENTER:
        case LEFT_BOTTOM:
            setX(X + bounds.getMinX() + getArrowSize());
            setY(getY() + bounds.getMinY() - computeYOffset());
            break;
        case BOTTOM_CENTER:
        case BOTTOM_LEFT:
        case BOTTOM_RIGHT:
            setX(X + bounds.getMinX() - computeXOffset());
            setY(getY() - bounds.getMinY() - bounds.getMaxY() - 1);
            break;
        case RIGHT_TOP:
        case RIGHT_BOTTOM:
        case RIGHT_CENTER:
            setX(X - bounds.getMinX() - bounds.getMaxX() - 1);
            setY(getY() + bounds.getMinY() - computeYOffset());
            break;
        }
    }
    
    private double computeXOffset() {        
        switch (getArrowLocation()) {
        case TOP_LEFT:
        case BOTTOM_LEFT:
            return getCornerRadius() + getArrowIndent() + getArrowSize();
        case TOP_CENTER:
        case BOTTOM_CENTER:
            return getContentNode().prefWidth(-1) / 2;
        case TOP_RIGHT:
        case BOTTOM_RIGHT:
            return getContentNode().prefWidth(-1) - getArrowIndent()
                    - getCornerRadius() - getArrowSize();
        default:
            return 0;
        }
    }

    private double computeYOffset() {
        switch (getArrowLocation()) {
        case LEFT_TOP:
        case RIGHT_TOP:
            return getCornerRadius() + getArrowIndent() + getArrowSize();
        case LEFT_CENTER:
        case RIGHT_CENTER:
            return getContentNode().prefHeight(-1) / 2;
        case LEFT_BOTTOM:
        case RIGHT_BOTTOM:
            return getContentNode().prefHeight(-1) - getCornerRadius()
                    - getArrowIndent() - getArrowSize();
        default:
            return 0;
        }
    }
    
/******************************************************************************/
    
    // TODO: add css support for the gap value
    // gap between screen border and the popover
    // note that the practical value is (GAP-padding)/2 so if padding is 4
    // then real GAP will be 3
    private static double GAP = 9;
    
    public enum NodeCentricPos {
        Center,
        UpLeft,
        UpCenter,
        UpRight,
        DownLeft,
        DownCenter,
        DownRight,
        RightUp,
        RightCenter,
        RightDown,
        LeftUp,
        LeftCenter,
        LeftDown;
        
        public NodeCentricPos reverse() {
            switch(this) {
                case Center:        return Center;
                case UpLeft:        return DownRight;
                case UpCenter:      return DownCenter;
                case UpRight:       return DownLeft;
                case DownLeft:      return UpRight;
                case DownCenter:    return UpCenter;
                case DownRight:     return UpLeft;
                case RightUp:       return LeftDown;
                case RightCenter:   return LeftCenter;
                case RightDown:     return LeftUp;
                case LeftUp:        return RightDown;
                case LeftCenter:    return RightCenter;
                case LeftDown:      return RightUp;
                default: return this;
            }
        }
        public double calcX(Node n, PopOver popup) {
            double W = popup.getContentNode().getBoundsInParent().getWidth();
            double X = n.localToScreen(0, 0).getX();
            switch(this) {
                case Center:
                case DownCenter:
                case UpCenter:  return X + n.getBoundsInParent().getWidth()/2 - W/2;
                case LeftCenter:
                case LeftUp:
                case LeftDown:  return X - W;
                case RightCenter:
                case RightUp:
                case RightDown: return X + n.getBoundsInParent().getWidth();
                case UpLeft:
                case DownLeft:  return X;
                case UpRight:
                case DownRight: return X + n.getBoundsInParent().getWidth() - W;
                default: return 0;
            }
        }
        public double calcY(Node n, PopOver popup) {
            double H = popup.getContentNode().getBoundsInParent().getHeight();
            double Y = n.localToScreen(0, 0).getY();
            switch(this) {
                case UpRight:
                case UpCenter:
                case UpLeft:    return Y - H;
                case DownCenter:
                case DownLeft:
                case DownRight: return Y + n.getBoundsInParent().getHeight();
                case LeftUp:
                case RightUp:   return Y;
                case Center:
                case LeftCenter:
                case RightCenter:return Y + n.getBoundsInParent().getHeight()/2-H/2;
                case LeftDown:
                case RightDown: return Y + n.getBoundsInParent().getHeight()-H;
                default: return 0;
            }
        }
    }
    public enum ScreenCentricPos {
        ScreenTopRight,
        ScreenTopLeft,
        ScreenCenter,
        ScreenBottomRight,
        ScreenBottomLeft,
        AppTopRight,
        AppTopLeft,
        AppCenter,
        AppBottomRight,
        AppBottomLeft;
        
        public double calcX(PopOver popup) {
            double W = popup.getContentNode().layoutBoundsProperty().get().getWidth() + GAP;
            Rectangle2D screen = Screen.getPrimary().getBounds();
            WindowBase app = App.getWindow();
            switch(this) {
                case AppTopLeft:
                case AppBottomLeft:     return app.getX();
                case AppTopRight:
                case AppBottomRight:    return app.getX()+app.getWidth()-W;
                case AppCenter:         return app.getX()+app.getWidth()/2-W/2;
                case ScreenTopLeft:
                case ScreenBottomLeft:  return 0;
                case ScreenTopRight:
                case ScreenBottomRight: return screen.getWidth()-W;
                case ScreenCenter:      return screen.getWidth()/2-W/2;
                default:                return screen.getWidth()/2-W/2;
            }
        }
        public double calcY(PopOver popup) {
            double H = popup.getContentNode().layoutBoundsProperty().get().getHeight() + GAP;
            Rectangle2D screen = Screen.getPrimary().getBounds();
            WindowBase app = App.getWindow();
            switch(this) {
                case AppBottomLeft:
                case AppBottomRight:    return app.getY()+app.getHeight()-H;
                case AppCenter:         return app.getY()+app.getHeight()/2-H/2;
                case AppTopLeft:
                case AppTopRight:       return app.getY();
                case ScreenBottomLeft:
                case ScreenBottomRight: return screen.getHeight()-H;
                case ScreenCenter:      return screen.getHeight()/2-H/2;
                case ScreenTopLeft:
                case ScreenTopRight:    return 0;
                default:                return screen.getHeight()/2-H/2;
            }
        }
    }
    
/******************************************************************************/

    // arrow size support
    // TODO: make styleable
    private final DoubleProperty arrowSize = new SimpleDoubleProperty(this,
            "arrowSize", 9);

    /**
     * Controls the size of the arrow. Default value is 12.
     * Set to arbitrary positive value or 0 to disable arrow.
     * <p>
     * Disabling arrow
     * has an effect of not positioning the popover so the arrow points to the
     * specific point. Rather the popover's upper left corner will.
     * 
     * @return the arrow size property
     */
    public final DoubleProperty arrowSizeProperty() {
        return arrowSize;
    }

    /**
     * Returns the value of the arrow size property.
     * 
     * @return the arrow size property value
     * @see #arrowSizeProperty()
     */
    public final double getArrowSize() {
        return arrowSizeProperty().get();
    }

    /**
     * Sets the value of the arrow size property.
     * <p>
     * Set to arbitrary positive value or 0 to disable arrow.
     * 
     * @param size arrow size
     * @see #arrowSizeProperty()
     */
    public final void setArrowSize(double size) {
        arrowSizeProperty().set(size);
    }

    // arrow indent support
    // TODO: make styleable
    private final DoubleProperty arrowIndent = new SimpleDoubleProperty(this,
            "arrowIndent", 12);

    /**
     * Controls the distance between the arrow and the corners of the pop over.
     * The default value is 12.
     * 
     * @return the arrow indent property
     */
    public final DoubleProperty arrowIndentProperty() {
        return arrowIndent;
    }

    /**
     * Returns the value of the arrow indent property.
     * 
     * @return the arrow indent value
     * 
     * @see #arrowIndentProperty()
     */
    public final double getArrowIndent() {
        return arrowIndentProperty().get();
    }

    /**
     * Sets the value of the arrow indent property.
     * 
     * @param size
     *            the arrow indent value
     * 
     * @see #arrowIndentProperty()
     */
    public final void setArrowIndent(double size) {
        arrowIndentProperty().set(size);
    }

    // radius support

    // TODO: make styleable

    private final DoubleProperty cornerRadius = new SimpleDoubleProperty(this,
            "cornerRadius", 6);

    /**
     * Returns the corner radius property for the pop over.
     * 
     * @return the corner radius property (default is 6)
     */
    public final DoubleProperty cornerRadiusProperty() {
        return cornerRadius;
    }

    /**
     * Returns the value of the corner radius property.
     * 
     * @return the corner radius
     * 
     * @see #cornerRadiusProperty()
     */
    public final double getCornerRadius() {
        return cornerRadiusProperty().get();
    }

    /**
     * Sets the value of the corner radius property.
     * 
     * @param radius
     *            the corner radius
     * 
     * @see #cornerRadiusProperty()
     */
    public final void setCornerRadius(double radius) {
        cornerRadiusProperty().set(radius);
    }

    private final ObjectProperty<PopOver.ArrowLocation> arrowLocation = new SimpleObjectProperty(
            this, "arrowLocation", PopOver.ArrowLocation.LEFT_TOP);

    /**
     * Stores the preferred arrow location. This might not be the actual
     * location of the arrow if auto fix is enabled.
     * @see #setAutoFix(boolean)
     * @return the arrow location property
     */
    public final ObjectProperty<PopOver.ArrowLocation> arrowLocationProperty() {
        return arrowLocation;
    }

    /**
     * Sets the value of the arrow location property.
     * @see #arrowLocationProperty()
     * @param location the requested location
     */
    public final void setArrowLocation(PopOver.ArrowLocation location) {
        arrowLocationProperty().set(location);
    }

    /**
     * Returns the value of the arrow location property.
     * @see #arrowLocationProperty()
     * @return the preferred arrow location
     */
    public final PopOver.ArrowLocation getArrowLocation() {
        return arrowLocationProperty().get();
    }

    /** All possible arrow locations. */
    public enum ArrowLocation {
        LEFT_TOP, LEFT_CENTER, LEFT_BOTTOM, RIGHT_TOP, RIGHT_CENTER, RIGHT_BOTTOM,
        TOP_LEFT, TOP_CENTER, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT;
    }
    
/**************************** IN/OUT ANIMATIONS *******************************/
    
    private static final Duration DEFAULT_FADE_IN_DURATION = Duration.seconds(.5);
    private static boolean animated = true;
    
    // Lazily initialized, might be null, use getter
    private Duration fadeDuration;
    private FadeTransition fadeInAnim;
    private FadeTransition fadeOutAnim;
    private double opacityOldVal = 0;   // for restoring from previous session
    
    /**
     * Return whether is animated. Uses fade in and out animations on show/hide.
     */
    public boolean isAnimated() {
        return animated;
    }
    
    /**
     * Sets whether is animated. Uses fade in and out animations on show/hide.
     */
    public void setAnimated(boolean val) {
        animated = val;
    }
    
    public void setAnimDuration(Duration val) {
        fadeDuration = val;
    }
    
    public Duration getAnimDuration() {
        return (fadeDuration != null) ? fadeDuration : DEFAULT_FADE_IN_DURATION;
    }

    private void fadeIn() {//getSkin().getNode().setOpacity(0);
        // lazy initialize
        if (fadeInAnim==null) {
            fadeInAnim = new FadeTransition();
            fadeInAnim.setFromValue(0);
            fadeInAnim.setToValue(1);
        }
        // if running stop
        if (fadeOutAnim!=null) {
            fadeOutAnim.setOnFinished(null);
            fadeOutAnim.stop();
            opacityOldVal = getSkin().getNode().getOpacity();
        }// else opacityOldVal = 0;
        // set & play
        fadeInAnim.setOnFinished(e -> opacityOldVal=1 );
        fadeInAnim.setNode(getSkin().getNode());
        fadeInAnim.setFromValue(opacityOldVal);
        fadeInAnim.setDuration(getAnimDuration());
//        fadeInAnim.playFrom(getAnimDuration().multiply(opacityOldVal));
        fadeInAnim.play();
    }
    private void fadeOut() {
        // lazy initialize
        if (fadeOutAnim==null) {
            fadeOutAnim = new FadeTransition();
            fadeOutAnim.setToValue(0);
        }
        // if running stop
        if (fadeInAnim!=null )
            fadeInAnim.stop();
        // set & play
        fadeOutAnim.setNode(getSkin().getNode());
        fadeOutAnim.setDuration(getAnimDuration());
        fadeOutAnim.setOnFinished( e -> {
            opacityOldVal=0;//System.out.println("setting 0");
            hideImmediatelly();
            fadeOutAnim.setOnFinished(null);
        });
        fadeOutAnim.playFromStart();
    }
    
/******************************************************************************/
    
    /**
     * Sets closing behavior when receives click event. The detached value is 
     * ignored - popup will always hide. Default false (off).
     * <p>
     * Even if true, this behavior can be prevented when the content of the
     * popup consumes the MOUSE_CLICKED event.
     * <p>
     * Tip: It should only be used when content is static and does not react
     * on mouse events - for example in case of informative popups.
     * Also, it is recommended to use this in conjunction with {@link #setAutoHide(boolean)}
     * but with inverted value. One might want to have a popup with reactive
     * content auto-closing when clicked anywhere but on it, or static popup 
     * displaying information hovering above its owner, while the owner can be
     * used with the popup open but conveniently closing on mouse click.
     * <p>
     * Of course any combination of the values is possible. Completely custom 
     * implementation can be used too using mouse click event handler on the
     * content.
     * 
     * @param val 
     */
    public void setHideOnClick(boolean val) {
        if(val) {
            // construct the handler lazily on demand
            if(hideOnClick==null) {
                hideOnClick = e -> {
                    // close only if the popup was not dragged since mouse press
                    if(e.isStillSincePress()) hideStrong();
                };
            }
            // set the behavior
            getScene().addEventHandler(MOUSE_CLICKED, hideOnClick);
        } else {
            // remove the behavior but watch out for lazy initialization
            if(hideOnClick != null) {
                getScene().removeEventHandler(MOUSE_CLICKED, hideOnClick);
                hideOnClick = null;
            }
        }
    }
    
    /**
     * Returns whether hiding on click behavior is set true.
     */
    public boolean isHideOnClick() {
        return hideOnClick!=null;
    }
    
    private EventHandler<MouseEvent> hideOnClick;
    
/******************************************************************************/
    public void setParentPopup(PopOver popover) {
        popover.addEventFilter(WindowEvent.WINDOW_HIDING, e -> {
            if(this != null && this.isShowing())
                // same bug as with 'open popups preventing app
                // closing properly' due to owner being closed before the child
                // we neeed to close immediatelly
                this.hideImmediatelly();
        });
    }
/******************************** DETACHING ***********************************/
    
    /**
     * Detaches the pop over from the owning node. The pop over will no longer
     * display an arrow pointing at the owner node.
     */
    public final void detach() {
        if (isDetachable()) setDetached(true);
    }

    private final BooleanProperty detachable = new SimpleBooleanProperty(this,"detachable", true);
    private final BooleanProperty detached = new SimpleBooleanProperty(this,"detached", false);
    
    /** Determines if the pop over is detachable at all. */
    public final BooleanProperty detachableProperty() {
        return detachable;
    }

    /**
     * Sets the value of the detachable property.
     * @param detachable if true then the user can detach / tear off the pop over
     * @see #detachableProperty()
     */
    public final void setDetachable(boolean detachable) {
        detachableProperty().set(detachable);
    }

    /**
     * Returns the value of the detachable property.
     * @return true if the user is allowed to detach / tear off the pop over
     * @see #detachableProperty()
     */
    public final boolean isDetachable() {
        return detachableProperty().get();
    }
    
    /**
     * Determines whether the pop over is detached from the owning node or not.
     * A detached pop over no longer shows an arrow pointing at the owner and
     * features its own title bar.
     * @return the detached property
     */
    public final BooleanProperty detachedProperty() {
        return detached;
    }

    /**
     * Sets the value of the detached property.
     * @param detached if true the pop over will change its apperance to "detached"
     * mode
     * @see #detachedProperty();
     */
    public final void setDetached(boolean detached) {
        detachedProperty().set(detached);
    }

    /**
     * Returns the value of the detached property.
     * @return true if the pop over is currently detached.
     * @see #detachedProperty();
     */
    public final boolean isDetached() {
        return detachedProperty().get();
    }

    private final StringProperty title = new SimpleStringProperty(this,"detachedTitle", "");
    private final ObjectProperty<Pos> titlePos = new SimpleObjectProperty(this,"titlePos",null);
        
    /**
     * Stores the title text. Default "".
     * @return the detached title property
     */
    public final StringProperty titleProperty() {
        return title;
    }

    /**
     * @see #titleProperty()
     * @return the title text
     */
    public final String getTitle() {
        return titleProperty().get();
    }

    /**
     * Sets the title text.
     * @see #titleProperty()
     */
    public final void setTitle(String title) {
        titleProperty().set(title==null ? "" : title);
    }    
    
    private ObservableList<Node> headerContent = null;
    
    /**
     * Modifiable list of children of the header. The elements are Nodes displayed
     * in the header. The list is modifiable and any changes will be immediatelly
     * reflected visually.
     * <p>
     * Use to customize header for example by adding icon or buttons for controlling
     * the content of the popup.
     * <p>
     * Note that the order of the items matters. The nodes will be displayed in
     * the header in the same order as they are in the list. The ascending order
     * goes from left to right.
     * <p>
     * Tip: add node to specific position by add(int i, element e) and replace
     * the node with set(int i, element e); called on the returned list.
     * <p>
     * The added node will probably have assigned a mouse click event handler 
     * that executes a click behavior. In such case it is recommended to always
     * consume the event to prevent popover autohiding features to occur by 
     * stopping the event propagation.
     * 
     * @return all custom children of the header
     */
    public ObservableList<Node> getHeaderIcons() {
        if(headerContent==null) headerContent = FXCollections.observableArrayList();
        return headerContent;
    }
}