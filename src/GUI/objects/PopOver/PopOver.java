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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.animation.FadeTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.PopupControl;
import javafx.scene.control.Skin;
import javafx.scene.input.MouseEvent;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static javafx.scene.input.MouseEvent.MOUSE_RELEASED;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import main.App;

/**
 * The PopOver control provides detailed information about an owning node in a
 * popup window. The popup window has a very lightweight appearance (no default
 * window decorations) and an arrow pointing at the owner. Due to the nature of
 * popup windows the PopOver will move around with the parent window when the
 * user drags it. <br>
 * <center> <img src="popover.png"/> </center> <br>
 * The PopOver can be detached from the owning node by dragging it away from the
 * owner. It stops displaying an arrow and starts displaying a title and a close
 * icon. <br>
 * <br>
 * <center> <img src="popover-detached.png"/> </center> <br>
 * The following image shows a popover with an accordion content node. PopOver
 * controls are automatically resizing themselves when the content node changes
 * its size.<br>
 * <br>
 * <center> <img src="popover-accordion.png"/> </center> <br>
 */
public class PopOver extends PopupControl {

    private static final String STYLE_CLASS = "popover";
    public static List<PopOver> active_popups = new ArrayList(); 
    
    public static void autoCloseFire() {
        // copy list to avoid concurrent modification on hide removal from list
        // active_popups.stream() wont help here either, why?
        List<PopOver> popups = new ArrayList<>(active_popups);
        popups.forEach(p->{
            if(p.isAutoHide()) p.hide();
        });
    }
    public static void escapeCloseFire() {
        PopOver pop=null;
        for(PopOver p: active_popups)
            if(p.isHideOnEscape()){
                pop=p;
                break;
            }
        if(pop!=null) pop.hideStrong();
    }

/******************************************************************************/

    /**
     * Creates a pop over with a label as the content node.
     */
    public PopOver() {
        super();
        getStyleClass().add(STYLE_CLASS);
        
        
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
     * @param content shown by the pop over
     */
    public PopOver(Node content) {
        this();
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
     * Returns the value of the content property.
     * @return the content node
     * @see #contentProperty()
     */
    public final Node getContentNode() {
        return contentNodeProperty().get();
    }

    /**
     * Sets the value of the content property.
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
    
    private void showThis(Node owner,Window ownerW) {
        
        setDetached(false);
        if(owner!=null) {
           installMoveWithWindowPre(owner.getScene().getWindow());
           installMoveWithNodePre(owner);
        } else {
            if(ownerW!=null) installMoveWithWindowPre(ownerW);
        }
        
        // show the popup
        super.show(App.getWindowOwner().getStage(),0,0);
        active_popups.add(this);
        if(isAnimated())getSkin().getNode().setOpacity(opacityOldVal);
    }
    private void position(Node owner,Window ownerW, double x, double y) {
        setX(x);
        setY(y);
        adjustWindowLocation();
        
        if (isAnimated()) fadeIn();
        
        // now after the popup is visible and constructed
        if(owner!=null) installMoveWithNodePos(owner);
//        if(owner==null && ownerW!=null) installMoveWithWindowePos(ownerW);
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
        showThis(owner, null);
        Point2D a = owner.localToScreen(x,y);
        double X = a.getX() + owner.getBoundsInParent().getWidth()/2;
        double Y = a.getY() + owner.getBoundsInParent().getHeight()/2;
        position(owner,null,X,Y);
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
        showThis(owner, null);
        double X = pos.calcX(owner, this)+owner.getBoundsInParent().getWidth()/2;
        double Y = pos.calcY(owner, this)+owner.getBoundsInParent().getHeight()/2;
        position(owner,null,X,Y);
    }
    
    /** Display at specified screen coordinates */
    @Override
    public void show(Window window, double x, double y) {
        showThis(null, window);
        position(ownerNode, window, x, y);
    }
    
    /** Display at specified designated screen position */
    public void show(ScreenCentricPos pos) {
        showThis(null, App.getWindow().getStage());
        position(null,  App.getWindow().getStage(), pos.calcX(this), pos.calcY(this));
        
//        super.show(App.getWindowOwner().getStage());
//        active_popups.add(this);
//        setX(pos.calcX(this));
//        setY(pos.calcY(this));
//        
//        if (isAnimated()) fadeIn();
    }

    @Override
    public void show(Window window) {
        super.show(window);
        active_popups.add(this);
        
        if (isAnimated()) fadeIn();
    }
    
/******************************************************************************/

    private Node ownerNode;
    private Stage ownerWindow;
    
    private double deltaX = 0;
    private double deltaY = 0;
    private double deltaThisX = 0;
    private double deltaThisY = 0;
    private boolean deltaThisLock = false;
    
    private final ChangeListener<Number> strongXListener = (o, oldX, newX) -> {
        if(ownerNode!=null)
            setX(deltaThisX+ownerNode.localToScreen(0, 0).getX()-deltaX);
    };
    private final ChangeListener<Number> strongYListener = (o, oldY, newY) -> {
        if(ownerNode!=null)
            setY(deltaThisY+ownerNode.localToScreen(0, 0).getY()-deltaY);
    };
    
    private final WeakChangeListener<Number> xListener = new WeakChangeListener(strongXListener);
    private final WeakChangeListener<Number> yListener = new WeakChangeListener(strongYListener);
    
    private void installMoveWithWindowPre(Window owner) {
        ownerWindow = (Stage)owner;
        ownerWindow.xProperty().addListener(xListener);
        ownerWindow.yProperty().addListener(yListener);
        ownerWindow.widthProperty().addListener(xListener);
        ownerWindow.heightProperty().addListener(yListener);
    }
//    private void installMoveWithWindowePos(Window owner) {
//        // monitor this' position for change if lock allows
//        xProperty().addListener(o->{ if (deltaThisLock) deltaThisX=getX(); });
//        yProperty().addListener(o->{ if (deltaThisLock) deltaThisY=getY(); });
//        // remember owner's position to monitor change
//        deltaX = owner.getX();
//        deltaY = owner.getY();
//    }
    private void installMoveWithNodePre(Node owner) {
        ownerNode = owner;
        owner.layoutXProperty().addListener(xListener);
        owner.layoutYProperty().addListener(yListener);
    }
    private void installMoveWithNodePos(Node owner) {
        // remember owner's position to monitor change
        deltaX = owner.localToScreen(0,0).getX();
        deltaY = owner.localToScreen(0,0).getY();
        // maintain lock to prevent illegal position monitoring
        getScene().addEventHandler(MOUSE_PRESSED, e-> deltaThisLock=true );
        getScene().addEventHandler(MOUSE_RELEASED, e-> deltaThisLock=false );
        // monitor this' position for change if lock allows
        xProperty().addListener(o->{ if (deltaThisLock) deltaThisX=getX(); });
        yProperty().addListener(o->{ if (deltaThisLock) deltaThisY=getY(); });
        // remember this' position to monitor - listeners wont initialize it
        deltaThisX = this.getX();
        deltaThisY = this.getY();
    }
    private void uninstallMoveWith() {
        if(ownerWindow!=null) {
            ownerWindow.xProperty().removeListener(xListener);
            ownerWindow.yProperty().removeListener(yListener);
            ownerWindow.widthProperty().removeListener(xListener);
            ownerWindow.heightProperty().removeListener(yListener);
            ownerWindow = null;
        }
        if(ownerNode!=null) {
            ownerNode.layoutXProperty().removeListener(xListener);
            ownerNode.layoutYProperty().removeListener(yListener);
            ownerNode = null;
        }
    }
    
/******************************************************************************/
    
   /*
    * Move the window so that the arrow will end up pointing at the
    * target coordinates.
    */
    private void adjustWindowLocation() {
        Bounds bounds = PopOver.this.getSkin().getNode().getBoundsInParent();

        switch (getArrowLocation()) {
        case TOP_CENTER:
        case TOP_LEFT:
        case TOP_RIGHT:
            setX(getX() + bounds.getMinX() - computeXOffset());
            setY(getY() + bounds.getMinY() + getArrowSize());
            break;
        case LEFT_TOP:
        case LEFT_CENTER:
        case LEFT_BOTTOM:
            setX(getX() + bounds.getMinX() + getArrowSize());
            setY(getY() + bounds.getMinY() - computeYOffset());
            break;
        case BOTTOM_CENTER:
        case BOTTOM_LEFT:
        case BOTTOM_RIGHT:
            setX(getX() + bounds.getMinX() - computeXOffset());
            setY(getY() - bounds.getMinY() - bounds.getMaxY() - 1);
            break;
        case RIGHT_TOP:
        case RIGHT_BOTTOM:
        case RIGHT_CENTER:
            setX(getX() - bounds.getMinX() - bounds.getMaxX() - 1);
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
            double W = popup.getContentNode().layoutBoundsProperty().get().getWidth();
            Rectangle2D screen = Screen.getPrimary().getVisualBounds();
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
            double H = popup.getContentNode().layoutBoundsProperty().get().getHeight();
            Rectangle2D screen = Screen.getPrimary().getVisualBounds();
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
     * 
     * @see #arrowSizeProperty()
     */
    public final double getArrowSize() {
        return arrowSizeProperty().get();
    }

    /**
     * Sets the value of the arrow size property.
     * 
     * @param size
     *            the new value of the arrow size property
     * 
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
     * Sets closing behavior when receives click event on/off. The detached
     * value is ignored as well. Default false (off).
     * Tip: It should only be used when content is static and does not react
     * on mouse events - for example in case of informative popups.
     * Also, it is recommended to use this in conjunction with {@link #setAutoHide(boolean)}
     * but with inverted value. One might want to have a popup with reactive
     * content auto-closing when clicked anywhere but on it, or popup displaying
     * some information hovering above its owner, allowing for it to be used
     * while remaining open until it itself is clicked on.---
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
    
    /**
     * Title position property. The title position value can be null as well. In
     * that case it falls back to value set by the css. Default value is null.
     * <p>
     * Once the value is set to non null value, the css value will never be used
     * again and setting this value to null again will have no effect.
     * <p>
     * Intended as set and forget type of value that is only set once.
     * @return the detached title property
     */
    public final ObjectProperty<Pos> titlePosProperty() {
        return titlePos;
    }

    /**
     * Returns the title position property.
     * @see #titlePosProperty()
     */
    public final Pos getTitlePos() {
        return titlePos.get();
    }

    /**
     * Sets the title position property.
     * @see #titlePosProperty()
     */
    public final void setTitlePos(Pos titlePos) {
        this.titlePos.set(titlePos);
    }
}