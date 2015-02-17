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

package GUI.objects.Rater;

import com.sun.javafx.Utils;
import com.sun.javafx.scene.control.skin.BehaviorSkinBase;
import de.jensd.fx.fontawesome.AwesomeIcon;
import static java.lang.Math.ceil;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import static javafx.geometry.Orientation.VERTICAL;
import javafx.geometry.Point2D;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.control.Label;
import static javafx.scene.input.MouseButton.SECONDARY;
import javafx.scene.input.MouseEvent;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import static util.Util.createIcon;

/**
 *
 */
public class RatingSkin extends BehaviorSkinBase<Rating, RatingBehavior> {
    
    public static final String SELECTED = "strong";
    public static final PseudoClass max = PseudoClass.getPseudoClass("max");
    public static final PseudoClass min = PseudoClass.getPseudoClass("min");
    
    // the container for the traditional rating control. If updateOnHover and
    // partialClipping are disabled, this will show a combination of strong
    // and non-strong graphics, depending on the current rating value
    private Pane backgroundContainer;
    // the container for the strong graphics which may be partially clipped.
    // Note that this only exists if updateOnHover or partialClipping is enabled.
    private Pane foregroundContainer;
    // clip mask
    private Rectangle forgroundClipRect;
    
    private double rating = 0;
    // tmp value for restoring old rating after hovering ends
    private double old_rating;
    
    private final EventHandler<MouseEvent> mouseMoveHandler = e -> {
        if (!getSkinnable().isUpdateOnHover() || !getSkinnable().isEditable())
            return;
        
        double newRating = calculateRating(e.getSceneX(), e.getSceneY());
        updateRating(newRating);
    };
    
    private final EventHandler<MouseEvent> mouseClickHandler = e-> {
        if (!getSkinnable().isEditable() || e.getButton()==SECONDARY) 
            return;
        
        double newRating = calculateRating(e.getSceneX(), e.getSceneY());
        updateRating(newRating);
        old_rating = newRating;
        
        // fire rating changed event
        if (getSkinnable().ratingChanged != null)
            getSkinnable().ratingChanged.accept(newRating/getSkinnable().getMax());
    };
    

    

    
    public RatingSkin(Rating control) {
        super(control, new RatingBehavior(control));
        
        // init
        recreateButtons();
        updateRating(getSkinnable().getRating());
        
        registerChangeListener(control.ratingProperty(), "RATING");
        registerChangeListener(control.maxProperty(), "MAX");
        registerChangeListener(control.orientationProperty(), "ORIENTATION");
        registerChangeListener(control.updateOnHoverProperty(), "UPDATE_ON_HOVER");
        registerChangeListener(control.partialRatingProperty(), "PARTIAL_RATING");
        
        // remember rating and return to old after mouse hover ends
        getSkinnable().addEventHandler(MOUSE_ENTERED, e -> {
            if (getSkinnable().isUpdateOnHover())
                old_rating = getSkinnable().getRating();
        });
        getSkinnable().addEventHandler(MOUSE_EXITED, e -> {
            if (getSkinnable().isUpdateOnHover())
                updateRating(old_rating);
        });
        getSkinnable().ratingProperty().addListener((o,ov,nv)->updateRating(nv.doubleValue()));
    }
    
    @Override protected void handleControlPropertyChanged(String p) {
        super.handleControlPropertyChanged(p);
        
        if (p == "RATING" || p == "PARTIAL_RATING" || p == "UPDATE_ON_HOVER") ;
        else if (p == "MAX" || p == "ORIENTATION") recreateButtons();
        
        updateRating(getSkinnable().getRating());
    }
    
    private void recreateButtons() {
        backgroundContainer = null;
        foregroundContainer = null;
        
        backgroundContainer = isVertical() ? new VBox() : new HBox();
        backgroundContainer.getStyleClass().add("container");
        getChildren().setAll(backgroundContainer);
        
        foregroundContainer = isVertical() ? new VBox() : new HBox();
        foregroundContainer.getStyleClass().add("container");
        foregroundContainer.setMouseTransparent(true);
        getChildren().add(foregroundContainer);

        forgroundClipRect = new Rectangle();
        foregroundContainer.setClip(forgroundClipRect);
        
        for (int index = 0; index <= getSkinnable().getMax(); index++) {
            Node backgroundNode = createButton(AwesomeIcon.STAR_ALT);
            
            if (index > 0) {
                if (isVertical()) {
                    backgroundContainer.getChildren().add(0,backgroundNode);
                } else {
                    backgroundContainer.getChildren().add(backgroundNode);
                }

                Node foregroundNode = createButton(AwesomeIcon.STAR);
                     foregroundNode.getStyleClass().add(SELECTED);
                     foregroundNode.setMouseTransparent(true);

                if (isVertical()) {
                    foregroundContainer.getChildren().add(0,foregroundNode);
                } else {
                    foregroundContainer.getChildren().add(foregroundNode);
                }
            }
        }
    }
    
    // returns rating based on scene relative mouse position
    private double calculateRating(double sceneX, double sceneY) {
        final Rating control = getSkinnable();
        final Point2D b = backgroundContainer.sceneToLocal(sceneX,sceneY);
        double leftpad = backgroundContainer.getPadding().getLeft();
        double toppad = backgroundContainer.getPadding().getRight();
        final int max = control.getMax();
        double w = control.getWidth() - leftpad - toppad;
        double h = control.getHeight() - leftpad - toppad;
        double x = b.getX()-leftpad;
        double y = b.getY()-toppad;        
               x = Utils.clamp(0, x, w);
               y = Utils.clamp(0, y, h);
               
        // calculate the new value
        double nv = (isVertical()) ? ((h-y)/h)*max : (x/w)*max;
               // need to make absolutely sure we dont get out of legal value bounds
               nv = Utils.clamp(0,nv,max);
               
        // ceil double to int if needed
        if (!getSkinnable().isPartialRating()) nv = ceil(nv);
        
        return nv;
    }
    
    // sets rating to thespecfied one and updates both skin & skinnable
    private void updateRating(double newRating) {
        // prevents recursive call, updating skinnable would eventually invoke this method again
        if (rating == newRating) return;
        
        // set value
        rating = newRating;
        getSkinnable().setRating(newRating);
        updateClip();
    }
    
    // updates the skin to the current values
    private void updateClip() {
        final Rating control = getSkinnable();
        
        final double w = control.getWidth() - (snappedLeftInset() + snappedRightInset());
        final double h = control.getHeight() - (snappedTopInset() + snappedBottomInset());
        
        double x = w/control.getMax()*rating;
        double y = h/control.getMax()*rating;
        
        if (isVertical()) {
            forgroundClipRect.relocate(0, h - y);
            forgroundClipRect.setWidth(control.getWidth());
            forgroundClipRect.setHeight(y);
        } else {
            forgroundClipRect.setWidth(x);
            forgroundClipRect.setHeight(control.getHeight());
        }
        
        boolean isMaxed = rating==1*getSkinnable().getMax();
        foregroundContainer.getChildren().forEach(n->n.pseudoClassStateChanged(max,isMaxed));
        boolean is0 = rating==0;
        backgroundContainer.getChildren().forEach(n->n.pseudoClassStateChanged(min,is0));
    }
        
    private Node createButton(AwesomeIcon icon) {
        Label l = createIcon(icon, 10, null, null);
        l.setCache(true);
        l.setCacheShape(true);
        l.setCacheHint(CacheHint.SPEED);
//        Label l = new Label();
               l.getStyleClass().add("rating-button");
               l.setOnMouseMoved(mouseMoveHandler);
               l.setOnMouseClicked(mouseClickHandler);
        return l;
    }
    
    private boolean isVertical() {
        return getSkinnable().getOrientation() == VERTICAL;
    }
    
    @Override protected double computeMaxWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset);
    }
}