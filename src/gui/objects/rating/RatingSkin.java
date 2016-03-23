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

package gui.objects.rating;

import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.control.SkinBase;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import de.jensd.fx.glyphs.GlyphIcons;
import util.graphics.Icons;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.STAR;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.STAR_ALT;
import static java.lang.Math.ceil;
import static javafx.application.Platform.runLater;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static util.Util.clip;

/** Skin for {@link Rating}. */
public class RatingSkin extends SkinBase<Rating> {

    public static final String SELECTED = "strong";
    public static final PseudoClass max = PseudoClass.getPseudoClass("max");
    public static final PseudoClass min = PseudoClass.getPseudoClass("min");

    // the container for the traditional rating control. If updateOnHover and
    // partialClipping are disabled, this will show a combination of strong
    // and non-strong graphics, depending on the current rating value
    private HBox backgroundContainer = new HBox();
    private HBox foregroundContainer = new HBox();  // the container for the strong graphics, has mask
    private Rectangle forgroundClipRect; // mask
    private double old_rating;
    private final EventHandler<MouseEvent> mouseMoveHandler = e -> {
        if (!getSkinnable().updateOnHover.get() || !getSkinnable().editable.get())
            return;

        double v = calculateRating(e.getSceneX(), e.getSceneY());
        updateRating(v);

        e.consume();
    };
    private final EventHandler<MouseEvent> mouseClickHandler = e-> {
        if (!getSkinnable().editable.get() || e.getButton()==SECONDARY)
            return;

        double v = calculateRating(e.getSceneX(), e.getSceneY());
        updateRating(v);
        old_rating = v;

        // fire rating changed event
        if (getSkinnable().ratingChanged != null)
            getSkinnable().ratingChanged.accept(v);

        e.consume();
    };

    public RatingSkin(Rating r) {
        super(r);

        recreateButtons();

        registerChangeListener(r.rating, e -> updateRating(r.rating.get()));
        registerChangeListener(r.icons, e -> recreateButtons());
        registerChangeListener(r.updateOnHover, e -> updateRating(r.rating.get()));
        registerChangeListener(r.partialRating, e -> updateRating(r.rating.get()));

        // remember rating and return to old after mouse hover ends
        r.addEventHandler(MOUSE_ENTERED, e -> {
            e.consume();
            if (r.updateOnHover.get())
                old_rating = r.rating.get();
        });
        r.addEventHandler(MOUSE_EXITED, e -> {
            e.consume();
            if (r.updateOnHover.get())
                updateRating(old_rating);
        });
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

        updateRating(getSkinnable().rating.get());
    }

    // returns rating based on scene relative mouse position
    private double calculateRating(double sceneX, double sceneY) {
        // get 0-1 position value
        final Rating r = getSkinnable();
        final Point2D b = backgroundContainer.sceneToLocal(sceneX,sceneY);
        double leftP = backgroundContainer.getPadding().getLeft();
        double rightP = backgroundContainer.getPadding().getRight();
        double w = r.getWidth() - leftP - rightP;
        double x = b.getX()-leftP;
               x = clip(0, x, w);
        // make 2px space for min & max value
        double extra = 2/w;
               x = x*(1+2*extra)-extra;
        // calculate the rating value
        double nv = clip(0,x/w,1);
        // ceil to int if needed
        double icons = r.icons.get();
        if (!r.partialRating.get()) nv = ceil(nv*icons)/icons;

        return nv;
    }

    // sets rating to thespecfied one and updates both skin & skinnable
    private void updateRating(double v) {
        // wont update sometimes without runlater
        runLater(() -> updateClip(v));
        updateClip(v);
    }

    // updates the skin to the current values
    private void updateClip(double v) {
        final Rating r =  getSkinnable();
        double w = r.getWidth() - (snappedLeftInset() + snappedRightInset());
        double x = w*v;

        forgroundClipRect.setWidth(x);
        forgroundClipRect.setHeight(r.getHeight());

        boolean is1 = v==1;
        foregroundContainer.getChildren().forEach(n->n.pseudoClassStateChanged(max,is1));
        boolean is0 = v==0;
        backgroundContainer.getChildren().forEach(n->n.pseudoClassStateChanged(min,is0));
    }

    private Node createButton(GlyphIcons icon) {
        Text l = Icons.createIcon(icon, getSkinnable().icons.get(), 10);
             l.setCache(true);
             l.setCacheHint(CacheHint.SPEED);
             l.getStyleClass().setAll("rating-button");
             l.setOnMouseMoved(mouseMoveHandler);
             l.setOnMouseClicked(mouseClickHandler);
        return l;
    }
}