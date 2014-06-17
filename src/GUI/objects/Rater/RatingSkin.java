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
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

/**
 *
 */
public class RatingSkin extends BehaviorSkinBase<Rating, RatingBehavior> {
    
    /***************************************************************************
     * 
     * Private fields
     * 
     **************************************************************************/
    
    private static final String STRONG = "strong";
    
    private boolean updateOnHover;
    private boolean partialRating;
    
    // the container for the traditional rating control. If updateOnHover and
    // partialClipping are disabled, this will show a combination of strong
    // and non-strong graphics, depending on the current rating value
    private Pane backgroundContainer;
    
    // the container for the strong graphics which may be partially clipped.
    // Note that this only exists if updateOnHover or partialClipping is enabled.
    private Pane foregroundContainer;
    
    private double rating = 0;

    private Rectangle forgroundClipRect;
    
    private Point2D lastMouseLocation = new Point2D(0, 0);
    
    private final EventHandler<MouseEvent> mouseMoveHandler = (MouseEvent event) -> {
        if (!updateOnHover || !getSkinnable().isEditable()) return;
        
        lastMouseLocation = new Point2D(event.getSceneX(), event.getSceneY());
        double newRating = calculateRating();
        updateClip();
        updateRating(newRating);
    };
    
    private final EventHandler<MouseEvent> mouseClickHandler = (MouseEvent event) -> {
        if (!getSkinnable().isEditable() || event.getButton()==MouseButton.SECONDARY) return;
        
        lastMouseLocation = new Point2D(event.getSceneX(), event.getSceneY());
        double newRating = calculateRating();
        updateClip();
        updateRating(newRating);
        old_rating = newRating;
        
        // fire rating changed event
        if (getSkinnable().ratingChanged != null)
            getSkinnable().ratingChanged.handle(event);
    };
    
    // tmp value for restoring old rating after hovering ends
    private double old_rating;
    

    /***************************************************************************
     * 
     * Constructors
     * 
     **************************************************************************/
    
    public RatingSkin(Rating control) {
        super(control, new RatingBehavior(control));
        
        this.updateOnHover = control.isUpdateOnHover();
        this.partialRating = control.isPartialRating();
        
        // init
        recreateButtons();
        updateRating(getSkinnable().getRating());
        
        registerChangeListener(control.ratingProperty(), "RATING");
        registerChangeListener(control.maxProperty(), "MAX");
        registerChangeListener(control.orientationProperty(), "ORIENTATION");
        registerChangeListener(control.updateOnHoverProperty(), "UPDATE_ON_HOVER");
        registerChangeListener(control.partialRatingProperty(), "PARTIAL_RATING");
        
        // remember rating and return to old after mouse hover ends
        getSkinnable().addEventHandler(MouseEvent.MOUSE_ENTERED, (Event t) -> {
            if (updateOnHover)
                old_rating = getSkinnable().getRating();
        });
        getSkinnable().addEventHandler(MouseEvent.MOUSE_EXITED, (Event t) -> {
            if (updateOnHover)
                updateRating(old_rating);
        });
        getSkinnable().ratingProperty().addListener(l->updateRating(getSkinnable().getRating()));
    }

    
    
    /***************************************************************************
     * 
     * Implementation
     * 
     **************************************************************************/
    
    @Override protected void handleControlPropertyChanged(String p) {
        super.handleControlPropertyChanged(p);
        
        if (p == "RATING") {
            updateRating(getSkinnable().getRating());
        } else if (p == "MAX") {
            recreateButtons();
        } else if (p == "ORIENTATION") {
            recreateButtons();
        } else if (p == "PARTIAL_RATING") {
            this.partialRating = getSkinnable().isPartialRating();
            recreateButtons();
        } else if (p == "UPDATE_ON_HOVER") {
            this.updateOnHover = getSkinnable().isUpdateOnHover();
            recreateButtons();
        }
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
            Node backgroundNode = createButton();
            
            if (index > 0) {
                if (isVertical()) {
                    backgroundContainer.getChildren().add(0,backgroundNode);
                } else {
                    backgroundContainer.getChildren().add(backgroundNode);
                }

                Node foregroundNode = createButton();
                foregroundNode.getStyleClass().add(STRONG);
                foregroundNode.setMouseTransparent(true);

                if (isVertical()) {
                    foregroundContainer.getChildren().add(0,foregroundNode);
                } else {
                    foregroundContainer.getChildren().add(foregroundNode);
                }
            }
        }
        
        updateRating(getSkinnable().getRating());
    }
    
    private double calculateRating() {
        final Rating control = getSkinnable();
        final Point2D b = backgroundContainer.sceneToLocal(lastMouseLocation.getX(), lastMouseLocation.getY());
        final double x = b.getX();
        final double y = b.getY();
        final double w = control.getWidth() - (snappedLeftInset() + snappedRightInset());
        final double h = control.getHeight() - (snappedTopInset() + snappedBottomInset());
        final int max = control.getMax();
        
        double newRating = (isVertical()) ? ((h-y)/h)*max : (x/w)*max;
        
        if (! partialRating)
            newRating = Utils.clamp(1, Math.ceil(newRating), max);
        
        return newRating;
    }
    
    private void updateRating(double newRating) {
        if (rating == newRating) return;
        rating = newRating;
        getSkinnable().setRating(newRating);
        updateClip();
    }
    
    private void updateClip() {
        final Rating control = getSkinnable();
        
        final double w = control.getWidth() - (snappedLeftInset() + snappedRightInset());
        final double h = control.getHeight() - (snappedTopInset() + snappedBottomInset());
        
        double x = w/control.getMax()*rating;
        double y = h/control.getMax()*rating;
        
//        System.out.println(x + " " + y + " " + w + " " + h);                   //DEBUG
        
        if (isVertical()) {
            forgroundClipRect.relocate(0, h - y);
            forgroundClipRect.setWidth(control.getWidth());
            forgroundClipRect.setHeight(y);
        } else {
            forgroundClipRect.setWidth(x);
            forgroundClipRect.setHeight(control.getHeight());
        }
    }
        
    private Node createButton() {
        Region btn = new Region();
        btn.getStyleClass().add("button");
        
        btn.setOnMouseMoved(mouseMoveHandler);
        btn.setOnMouseClicked(mouseClickHandler);
        return btn;
    }
    
    private boolean isVertical() {
        return getSkinnable().getOrientation() == Orientation.VERTICAL;
    }
    
    @Override protected double computeMaxWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset);
    }
}