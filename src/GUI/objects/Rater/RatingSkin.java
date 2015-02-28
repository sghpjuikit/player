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

import com.sun.javafx.scene.control.skin.BehaviorSkinBase;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.STAR;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.STAR_ALT;
import static java.lang.Math.ceil;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import static javafx.scene.input.MouseButton.SECONDARY;
import javafx.scene.input.MouseEvent;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import static util.Util.clip;
import util.graphics.Icons;

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
    private HBox backgroundContainer = new HBox();
    // the container for the strong graphics which may be partially clipped
    private HBox foregroundContainer = new HBox();
    // clip mask
    private Rectangle forgroundClipRect;
    
    private double rating = 0;
    private double old_rating;
    
    private final EventHandler<MouseEvent> mouseMoveHandler = e -> {
        e.consume();
        if (!getSkinnable().updateOnHover.get() || !getSkinnable().isEditable())
            return;
        
        double newRating = calculateRating(e.getSceneX(), e.getSceneY());
        updateRating(newRating);
    };
    
    private final EventHandler<MouseEvent> mouseClickHandler = e-> {
        e.consume();
        if (!getSkinnable().isEditable() || e.getButton()==SECONDARY) 
            return;
        
        double newRating = calculateRating(e.getSceneX(), e.getSceneY());
        updateRating(newRating);
        old_rating = newRating;
        
        // fire rating changed event
        if (getSkinnable().ratingChanged != null)
            getSkinnable().ratingChanged.accept(newRating/getSkinnable().max.get());
    };
    

    
    public RatingSkin(Rating control) {
        super(control, new RatingBehavior(control));
        
        // init
        recreateButtons();
        updateRating(getSkinnable().getRating());
        
        registerChangeListener(control.ratingProperty(), "RATING");
        registerChangeListener(control.max, "MAX");
        registerChangeListener(control.updateOnHover, "UPDATE_ON_HOVER");
        registerChangeListener(control.partialRating, "PARTIAL_RATING");
        
        // remember rating and return to old after mouse hover ends
        getSkinnable().addEventHandler(MOUSE_ENTERED, e -> {
            e.consume();
            if (getSkinnable().updateOnHover.get())
                old_rating = getSkinnable().getRating();
        });
        getSkinnable().addEventHandler(MOUSE_EXITED, e -> {
            e.consume();
            if (getSkinnable().updateOnHover.get())
                updateRating(old_rating);
        });
        getSkinnable().ratingProperty().addListener((o,ov,nv)->updateRating(nv.doubleValue()));
    }
    
    @Override 
    protected void handleControlPropertyChanged(String p) {
        super.handleControlPropertyChanged(p);
        
        if (p == "RATING" || p == "PARTIAL_RATING" || p == "UPDATE_ON_HOVER") ;
        else if (p == "MAX") recreateButtons();
        
        updateRating(getSkinnable().getRating());
    }
    
    private void recreateButtons() {
        backgroundContainer = new HBox();
        backgroundContainer.setAlignment(Pos.CENTER);
        foregroundContainer = new HBox();
        foregroundContainer.setAlignment(Pos.CENTER);
        foregroundContainer.setMouseTransparent(true);
        getChildren().setAll(backgroundContainer, foregroundContainer);
        
        forgroundClipRect = new Rectangle();
        foregroundContainer.setClip(forgroundClipRect);
        Node b = createButton(STAR_ALT);
        Node f = createButton(STAR);
        f.getStyleClass().add(SELECTED);
        f.setMouseTransparent(true);
        foregroundContainer.getChildren().add(f);
        backgroundContainer.getChildren().add(b);
    }
    
    // returns rating based on scene relative mouse position
    private double calculateRating(double sceneX, double sceneY) {
        final Rating control = getSkinnable();
        final Point2D b = backgroundContainer.sceneToLocal(sceneX,sceneY);
        double leftpad = backgroundContainer.getPadding().getLeft();
        double toppad = backgroundContainer.getPadding().getRight();
        final int max = control.max.get();
        double w = control.getWidth() - leftpad - toppad;
        double h = control.getHeight() - leftpad - toppad;
        double x = b.getX()-leftpad;
        double y = b.getY()-toppad;        
               x = clip(0, x, w);
               y = clip(0, y, h);
               
        // calculate the new value
        double nv = (x/w)*max;
               // need to make absolutely sure we dont get out of legal value bounds
               nv = clip(0,nv,max);
               
        // ceil double to int if needed
        if (!getSkinnable().partialRating.get()) nv = ceil(nv);
        
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
        
        double w = control.getWidth() - (snappedLeftInset() + snappedRightInset());
        double x = w/control.max.get()*rating;
        
        forgroundClipRect.setWidth(x);
        forgroundClipRect.setHeight(control.getHeight());
        
        boolean isMaxed = rating==1*getSkinnable().max.get();
        foregroundContainer.getChildren().forEach(n->n.pseudoClassStateChanged(max,isMaxed));
        boolean is0 = rating==0;
        backgroundContainer.getChildren().forEach(n->n.pseudoClassStateChanged(min,is0));
    }
        
    private Node createButton(FontAwesomeIconName icon) {
        Text l = Icons.createIcon(icon, getSkinnable().max.get(), 10);
             l.setCache(true);
             l.setCacheHint(CacheHint.SPEED);
             l.getStyleClass().add("rating-button");
             l.setOnMouseMoved(mouseMoveHandler);
             l.setOnMouseClicked(mouseClickHandler);
        return l;
    }
    
    @Override protected double computeMaxWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset);
    }
}