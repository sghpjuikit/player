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

import GUI.objects.PopOver.PopOver.ArrowLocation;
import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import static de.jensd.fx.fontawesome.AwesomeIcon.DOT_CIRCLE_ALT;
import static de.jensd.fx.fontawesome.AwesomeIcon.THUMB_TACK;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import static javafx.scene.control.ContentDisplay.CENTER;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.HLineTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.VLineTo;
import javafx.stage.Window;


public class PopOverSkin implements Skin<PopOver> {

    private static final PseudoClass DETACHED = PseudoClass.getPseudoClass("detached");
    private static final PseudoClass FOCUSED = PseudoClass.getPseudoClass("focused");

    private double xOffset;
    private double yOffset;

    private boolean tornOff;
    
    private final StackPane root;
    private Path path;
    private BorderPane content;
    
    private final Label title;
    private final Label closeIcon;
    private final Label pinIcon;
    private BorderPane header;
    private HBox headerControls;

    private Point2D dragStartLocation;

    private final PopOver<?> popOver;

    public PopOverSkin(final PopOver<?> popOver) {

        this.popOver = popOver;

        root = new StackPane();
        root.setPickOnBounds(false);
        root.getStyleClass().add("popover");

        /*
         * The min width and height equal 2 * corner radius + 2 * arrow indent +
         * 2 * arrow size.
         */
        root.minWidthProperty().bind(
                Bindings.add(Bindings.multiply(2, popOver.arrowSizeProperty()),
                        Bindings.add(
                                Bindings.multiply(2,
                                        popOver.cornerRadiusProperty()),
                                Bindings.multiply(2,
                                        popOver.arrowIndentProperty()))));

        root.minHeightProperty().bind(root.minWidthProperty());

        // create header & its content
        title = new Label();
        title.textProperty().bind(popOver.titleProperty());
        title.getStyleClass().add("title");

        closeIcon = AwesomeDude.createIconLabel(AwesomeIcon.TIMES_CIRCLE, "11");
        closeIcon.setOnMouseClicked( e -> popOver.hideStrong());
        closeIcon.setTooltip(new Tooltip("Close"));
        closeIcon.getStyleClass().add("popover-closebutton");
        
        pinIcon = AwesomeDude.createIconLabel(popOver.isAutoHide() ? THUMB_TACK : DOT_CIRCLE_ALT, "","11","11",CENTER);
        pinIcon.setOnMouseClicked( e -> {
            popOver.setAutoHide(!popOver.isAutoHide());
            e.consume();
        });
        pinIcon.setTooltip(new Tooltip("Autohide"));
        pinIcon.getStyleClass().add("popover-closebutton");
        // maintain proper pinIcon icon
        popOver.autoHideProperty().addListener((o,ov,nv) -> {
            AwesomeDude.setIcon(pinIcon, nv ? THUMB_TACK : DOT_CIRCLE_ALT, "11");
        });
        
        headerControls = new HBox(closeIcon);
        headerControls.setSpacing(5);
        headerControls.setAlignment(Pos.CENTER_RIGHT);
        headerControls.getStyleClass().add("header-buttons");
        // initialize proper header content
                headerControls.getChildren().setAll(popOver.getHeaderIcons());
//                headerControls.getChildren().add(headerControls.getChildren().size(),
//                        popOver.isDetached() ? closeIcon : pinIcon);
                headerControls.getChildren().addAll(pinIcon,closeIcon);
        // maintain proper header content
        popOver.getHeaderIcons().addListener((ListChangeListener.Change<? extends Node> e) -> {
            while(e.next()) {
                headerControls.getChildren().setAll(popOver.getHeaderIcons());
//                headerControls.getChildren().add(headerControls.getChildren().size(),
//                        popOver.isDetached() ? closeIcon : pinIcon);
                headerControls.getChildren().addAll(pinIcon,closeIcon);
            }
        });
        // switch pin & close icons when on detach change
        // we do this because detached/not d. modes differ in autohide
        // we want to customize autohiding arbitrarily in undetached mode
        // we want to close manually in detached mode with autohide off
        popOver.detachedProperty().addListener((o,ov,nv)->{
//            headerControls.getChildren().set(headerControls.getChildren().size()-1,
//                        nv ? closeIcon : pinIcon);
            headerControls.getChildren().addAll(pinIcon,closeIcon);
        });
        
        header = new BorderPane();
        header.setLeft(title);
        header.setRight(headerControls);
        header.getStyleClass().add("popover-header");
        
        // create content
        content = new BorderPane();
        content.setCenter(popOver.getContentNode());
        content.setTop(shouldHeaderBeVisible() ? header : null);    // show header only if title text not empty
        content.getStyleClass().add("content");
        
        // show new content when changes
        popOver.contentNodeProperty().addListener((o,ov,nv) -> content.setCenter(nv));
        
        // show header only if title text not empty
        popOver.titleProperty().addListener((o,ov,nv)->
                content.setTop(shouldHeaderBeVisible() ? header : null));

        InvalidationListener updatePathListener = o -> updatePath();

        popOver.getScene().getWindow().xProperty().addListener(updatePathListener);
        popOver.getScene().getWindow().yProperty().addListener(updatePathListener);

        popOver.arrowLocationProperty().addListener(updatePathListener);

        // this block must be done before the next one
        path = new Path();
        path.getStyleClass().add("bgr");
        path.setManaged(false);
        createPathElements();
        updatePath();
        
        // react on detached state change and initialize
        popOver.detachedProperty().addListener((o,ov,nv) -> {
            updatePath();
            content.pseudoClassStateChanged(DETACHED, nv);
            path.pseudoClassStateChanged(DETACHED, nv);
            content.setTop(header); // always show header in detached mode
        });
        boolean detached = popOver.isDetached();
        content.setTop(shouldHeaderBeVisible() ? header : null);
        content.pseudoClassStateChanged(DETACHED, detached);
        path.pseudoClassStateChanged(DETACHED, detached);        

        final EventHandler<MouseEvent> mousePressedHandler = e -> {
            if (popOver.isDetachable()) {
                tornOff = false;
                
                xOffset = e.getScreenX();
                yOffset = e.getScreenY();
                
                dragStartLocation = new Point2D(xOffset, yOffset);
            }
        };

        final EventHandler<MouseEvent> mouseReleasedHandler = e -> {
            if (tornOff && !popOver.isDetached()) {
                tornOff = false;
                popOver.detach();
            }
        };

        final EventHandler<MouseEvent> mouseDragHandler = e -> {
            if (popOver.isDetachable()) {
                double deltaX = e.getScreenX() - xOffset;
                double deltaY = e.getScreenY() - yOffset;
                
                Window window = popOver.getScene().getWindow();
                
                window.setX(window.getX() + deltaX);
                window.setY(window.getY() + deltaY);
                
                xOffset = e.getScreenX();
                yOffset = e.getScreenY();
                
                if (dragStartLocation.distance(xOffset, yOffset) > 20) {
                    tornOff = true;
                    updatePath();
                } else if (tornOff) {
                    tornOff = false;
                    updatePath();
                }
            }
        };

        root.setOnMousePressed(mousePressedHandler);
        root.setOnMouseDragged(mouseDragHandler);
        root.setOnMouseReleased(mouseReleasedHandler);

        root.getChildren().add(path);
        root.getChildren().add(content);
    }

    @Override
    public Node getNode() {
        return root;
    }

    @Override
    public PopOver getSkinnable() {
        return popOver;
    }
    
    /**
     * Sets padding of content within popover. Overrides and defaults to css.
     * @param i 
     */
    public void setContentPadding(Insets i) {
        // set padding from borders
        content.setPadding(i);
        // set header - content gap
        header.setPadding(new Insets(0, 0, i.getTop(), 0));
    }
    
    /**
     * Returns padding of content within popover. Default is css value.
     * @return 
     */
    public Insets getContentPadding() {
        return content.getPadding();
    }
    
    @Override
    public void dispose() {
    }

    private boolean shouldHeaderBeVisible() {
        return popOver.isDetached() || !title.getText().isEmpty() 
                    || !popOver.getHeaderIcons().isEmpty();
    }

    private MoveTo moveTo;

    private QuadCurveTo topCurveTo, rightCurveTo, bottomCurveTo, leftCurveTo;

    private HLineTo lineBTop, lineETop, lineHTop, lineKTop;
    private LineTo lineCTop, lineDTop, lineFTop, lineGTop, lineITop, lineJTop;

    private VLineTo lineBRight, lineERight, lineHRight, lineKRight;
    private LineTo lineCRight, lineDRight, lineFRight, lineGRight, lineIRight,
            lineJRight;

    private HLineTo lineBBottom, lineEBottom, lineHBottom, lineKBottom;
    private LineTo lineCBottom, lineDBottom, lineFBottom, lineGBottom,
            lineIBottom, lineJBottom;

    private VLineTo lineBLeft, lineELeft, lineHLeft, lineKLeft;
    private LineTo lineCLeft, lineDLeft, lineFLeft, lineGLeft, lineILeft,
            lineJLeft;

    private void createPathElements() {
        DoubleProperty centerYProperty = new SimpleDoubleProperty();
        DoubleProperty centerXProperty = new SimpleDoubleProperty();

        DoubleProperty leftEdgeProperty = new SimpleDoubleProperty();
        DoubleProperty leftEdgePlusRadiusProperty = new SimpleDoubleProperty();

        DoubleProperty topEdgeProperty = new SimpleDoubleProperty();
        DoubleProperty topEdgePlusRadiusProperty = new SimpleDoubleProperty();

        DoubleProperty rightEdgeProperty = new SimpleDoubleProperty();
        DoubleProperty rightEdgeMinusRadiusProperty = new SimpleDoubleProperty();

        DoubleProperty bottomEdgeProperty = new SimpleDoubleProperty();
        DoubleProperty bottomEdgeMinusRadiusProperty = new SimpleDoubleProperty();

        DoubleProperty cornerProperty = popOver.cornerRadiusProperty();

        DoubleProperty arrowSizeProperty = popOver.arrowSizeProperty();
        DoubleProperty arrowIndentProperty = popOver
                .arrowIndentProperty();

        centerYProperty.bind(Bindings.divide(root.heightProperty(), 2));
        centerXProperty.bind(Bindings.divide(root.widthProperty(), 2));

        leftEdgePlusRadiusProperty.bind(Bindings.add(leftEdgeProperty,
                popOver.cornerRadiusProperty()));

        topEdgePlusRadiusProperty.bind(Bindings.add(topEdgeProperty,
                popOver.cornerRadiusProperty()));

        rightEdgeProperty.bind(root.widthProperty());
        rightEdgeMinusRadiusProperty.bind(Bindings.subtract(rightEdgeProperty,
                popOver.cornerRadiusProperty()));

        bottomEdgeProperty.bind(root.heightProperty());
        bottomEdgeMinusRadiusProperty.bind(Bindings.subtract(
                bottomEdgeProperty, popOver.cornerRadiusProperty()));

        // INIT
        moveTo = new MoveTo();
        moveTo.xProperty().bind(leftEdgePlusRadiusProperty);
        moveTo.yProperty().bind(topEdgeProperty);

        //
        // TOP EDGE
        //
        lineBTop = new HLineTo();
        lineBTop.xProperty().bind(
                Bindings.add(leftEdgePlusRadiusProperty, arrowIndentProperty));

        lineCTop = new LineTo();
        lineCTop.xProperty().bind(
                Bindings.add(lineBTop.xProperty(), arrowSizeProperty));
        lineCTop.yProperty().bind(
                Bindings.subtract(topEdgeProperty, arrowSizeProperty));

        lineDTop = new LineTo();
        lineDTop.xProperty().bind(
                Bindings.add(lineCTop.xProperty(), arrowSizeProperty));
        lineDTop.yProperty().bind(topEdgeProperty);

        lineETop = new HLineTo();
        lineETop.xProperty().bind(
                Bindings.subtract(centerXProperty, arrowSizeProperty));

        lineFTop = new LineTo();
        lineFTop.xProperty().bind(centerXProperty);
        lineFTop.yProperty().bind(
                Bindings.subtract(topEdgeProperty, arrowSizeProperty));

        lineGTop = new LineTo();
        lineGTop.xProperty().bind(
                Bindings.add(centerXProperty, arrowSizeProperty));
        lineGTop.yProperty().bind(topEdgeProperty);

        lineHTop = new HLineTo();
        lineHTop.xProperty().bind(
                Bindings.subtract(Bindings.subtract(
                        rightEdgeMinusRadiusProperty, arrowIndentProperty),
                        Bindings.multiply(arrowSizeProperty, 2)));

        lineITop = new LineTo();
        lineITop.xProperty().bind(
                Bindings.subtract(Bindings.subtract(
                        rightEdgeMinusRadiusProperty, arrowIndentProperty),
                        arrowSizeProperty));
        lineITop.yProperty().bind(
                Bindings.subtract(topEdgeProperty, arrowSizeProperty));

        lineJTop = new LineTo();
        lineJTop.xProperty().bind(
                Bindings.subtract(rightEdgeMinusRadiusProperty,
                        arrowIndentProperty));
        lineJTop.yProperty().bind(topEdgeProperty);

        lineKTop = new HLineTo();
        lineKTop.xProperty().bind(rightEdgeMinusRadiusProperty);

        //
        // RIGHT EDGE
        //
        rightCurveTo = new QuadCurveTo();
        rightCurveTo.xProperty().bind(rightEdgeProperty);
        rightCurveTo.yProperty().bind(
                Bindings.add(topEdgeProperty, cornerProperty));
        rightCurveTo.controlXProperty().bind(rightEdgeProperty);
        rightCurveTo.controlYProperty().bind(topEdgeProperty);

        lineBRight = new VLineTo();
        lineBRight.yProperty().bind(
                Bindings.add(topEdgePlusRadiusProperty, arrowIndentProperty));

        lineCRight = new LineTo();
        lineCRight.xProperty().bind(
                Bindings.add(rightEdgeProperty, arrowSizeProperty));
        lineCRight.yProperty().bind(
                Bindings.add(lineBRight.yProperty(), arrowSizeProperty));

        lineDRight = new LineTo();
        lineDRight.xProperty().bind(rightEdgeProperty);
        lineDRight.yProperty().bind(
                Bindings.add(lineCRight.yProperty(), arrowSizeProperty));

        lineERight = new VLineTo();
        lineERight.yProperty().bind(
                Bindings.subtract(centerYProperty, arrowSizeProperty));

        lineFRight = new LineTo();
        lineFRight.xProperty().bind(
                Bindings.add(rightEdgeProperty, arrowSizeProperty));
        lineFRight.yProperty().bind(centerYProperty);

        lineGRight = new LineTo();
        lineGRight.xProperty().bind(rightEdgeProperty);
        lineGRight.yProperty().bind(
                Bindings.add(centerYProperty, arrowSizeProperty));

        lineHRight = new VLineTo();
        lineHRight.yProperty().bind(
                Bindings.subtract(Bindings.subtract(
                        bottomEdgeMinusRadiusProperty, arrowIndentProperty),
                        Bindings.multiply(arrowSizeProperty, 2)));

        lineIRight = new LineTo();
        lineIRight.xProperty().bind(
                Bindings.add(rightEdgeProperty, arrowSizeProperty));
        lineIRight.yProperty().bind(
                Bindings.subtract(Bindings.subtract(
                        bottomEdgeMinusRadiusProperty, arrowIndentProperty),
                        arrowSizeProperty));

        lineJRight = new LineTo();
        lineJRight.xProperty().bind(rightEdgeProperty);
        lineJRight.yProperty().bind(
                Bindings.subtract(bottomEdgeMinusRadiusProperty,
                        arrowIndentProperty));

        lineKRight = new VLineTo();
        lineKRight.yProperty().bind(bottomEdgeMinusRadiusProperty);

        //
        // BOTTOM EDGE
        //

        bottomCurveTo = new QuadCurveTo();
        bottomCurveTo.xProperty().bind(rightEdgeMinusRadiusProperty);
        bottomCurveTo.yProperty().bind(bottomEdgeProperty);
        bottomCurveTo.controlXProperty().bind(rightEdgeProperty);
        bottomCurveTo.controlYProperty().bind(bottomEdgeProperty);

        lineBBottom = new HLineTo();
        lineBBottom.xProperty().bind(
                Bindings.subtract(rightEdgeMinusRadiusProperty,
                        arrowIndentProperty));

        lineCBottom = new LineTo();
        lineCBottom.xProperty().bind(
                Bindings.subtract(lineBBottom.xProperty(), arrowSizeProperty));
        lineCBottom.yProperty().bind(
                Bindings.add(bottomEdgeProperty, arrowSizeProperty));

        lineDBottom = new LineTo();
        lineDBottom.xProperty().bind(
                Bindings.subtract(lineCBottom.xProperty(), arrowSizeProperty));
        lineDBottom.yProperty().bind(bottomEdgeProperty);

        lineEBottom = new HLineTo();
        lineEBottom.xProperty().bind(
                Bindings.add(centerXProperty, arrowSizeProperty));

        lineFBottom = new LineTo();
        lineFBottom.xProperty().bind(centerXProperty);
        lineFBottom.yProperty().bind(
                Bindings.add(bottomEdgeProperty, arrowSizeProperty));

        lineGBottom = new LineTo();
        lineGBottom.xProperty().bind(
                Bindings.subtract(centerXProperty, arrowSizeProperty));
        lineGBottom.yProperty().bind(bottomEdgeProperty);

        lineHBottom = new HLineTo();
        lineHBottom.xProperty().bind(
                Bindings.add(Bindings.add(leftEdgePlusRadiusProperty,
                        arrowIndentProperty), Bindings.multiply(
                        arrowSizeProperty, 2)));

        lineIBottom = new LineTo();
        lineIBottom.xProperty().bind(
                Bindings.add(Bindings.add(leftEdgePlusRadiusProperty,
                        arrowIndentProperty), arrowSizeProperty));
        lineIBottom.yProperty().bind(
                Bindings.add(bottomEdgeProperty, arrowSizeProperty));

        lineJBottom = new LineTo();
        lineJBottom.xProperty().bind(
                Bindings.add(leftEdgePlusRadiusProperty, arrowIndentProperty));
        lineJBottom.yProperty().bind(bottomEdgeProperty);

        lineKBottom = new HLineTo();
        lineKBottom.xProperty().bind(leftEdgePlusRadiusProperty);

        //
        // LEFT EDGE
        //
        leftCurveTo = new QuadCurveTo();
        leftCurveTo.xProperty().bind(leftEdgeProperty);
        leftCurveTo.yProperty().bind(
                Bindings.subtract(bottomEdgeProperty, cornerProperty));
        leftCurveTo.controlXProperty().bind(leftEdgeProperty);
        leftCurveTo.controlYProperty().bind(bottomEdgeProperty);

        lineBLeft = new VLineTo();
        lineBLeft.yProperty().bind(
                Bindings.subtract(bottomEdgeMinusRadiusProperty,
                        arrowIndentProperty));

        lineCLeft = new LineTo();
        lineCLeft.xProperty().bind(
                Bindings.subtract(leftEdgeProperty, arrowSizeProperty));
        lineCLeft.yProperty().bind(
                Bindings.subtract(lineBLeft.yProperty(), arrowSizeProperty));

        lineDLeft = new LineTo();
        lineDLeft.xProperty().bind(leftEdgeProperty);
        lineDLeft.yProperty().bind(
                Bindings.subtract(lineCLeft.yProperty(), arrowSizeProperty));

        lineELeft = new VLineTo();
        lineELeft.yProperty().bind(
                Bindings.add(centerYProperty, arrowSizeProperty));

        lineFLeft = new LineTo();
        lineFLeft.xProperty().bind(
                Bindings.subtract(leftEdgeProperty, arrowSizeProperty));
        lineFLeft.yProperty().bind(centerYProperty);

        lineGLeft = new LineTo();
        lineGLeft.xProperty().bind(leftEdgeProperty);
        lineGLeft.yProperty().bind(
                Bindings.subtract(centerYProperty, arrowSizeProperty));

        lineHLeft = new VLineTo();
        lineHLeft.yProperty().bind(
                Bindings.add(Bindings.add(topEdgePlusRadiusProperty,
                        arrowIndentProperty), Bindings.multiply(
                        arrowSizeProperty, 2)));

        lineILeft = new LineTo();
        lineILeft.xProperty().bind(
                Bindings.subtract(leftEdgeProperty, arrowSizeProperty));
        lineILeft.yProperty().bind(
                Bindings.add(Bindings.add(topEdgePlusRadiusProperty,
                        arrowIndentProperty), arrowSizeProperty));

        lineJLeft = new LineTo();
        lineJLeft.xProperty().bind(leftEdgeProperty);
        lineJLeft.yProperty().bind(
                Bindings.add(topEdgePlusRadiusProperty, arrowIndentProperty));

        lineKLeft = new VLineTo();
        lineKLeft.yProperty().bind(topEdgePlusRadiusProperty);

        topCurveTo = new QuadCurveTo();
        topCurveTo.xProperty().bind(leftEdgePlusRadiusProperty);
        topCurveTo.yProperty().bind(topEdgeProperty);
        topCurveTo.controlXProperty().bind(leftEdgeProperty);
        topCurveTo.controlYProperty().bind(topEdgeProperty);
    }

    private boolean showArrow(ArrowLocation loc) {
        ArrowLocation arrowLocation = popOver.getArrowLocation();
        return loc.equals(arrowLocation) && !popOver.isDetached() && !tornOff;
    }

    private void updatePath() {
        // Point2D targetPoint = new Point2D(popOver.getTargetX(),
        // popOver.getTargetY());
        //
        // Point2D windowPoint = new Point2D(getPopupWindow().getX(),
        // getPopupWindow().getY());

        List<PathElement> elements = new ArrayList<>();
        elements.add(moveTo);

        if (showArrow(ArrowLocation.TOP_LEFT)) {
            elements.add(lineBTop);
            elements.add(lineCTop);
            elements.add(lineDTop);
        }
        if (showArrow(ArrowLocation.TOP_CENTER)) {
            elements.add(lineETop);
            elements.add(lineFTop);
            elements.add(lineGTop);
        }
        if (showArrow(ArrowLocation.TOP_RIGHT)) {
            elements.add(lineHTop);
            elements.add(lineITop);
            elements.add(lineJTop);
        }
        elements.add(lineKTop);
        elements.add(rightCurveTo);

        if (showArrow(ArrowLocation.RIGHT_TOP)) {
            elements.add(lineBRight);
            elements.add(lineCRight);
            elements.add(lineDRight);
        }
        if (showArrow(ArrowLocation.RIGHT_CENTER)) {
            elements.add(lineERight);
            elements.add(lineFRight);
            elements.add(lineGRight);
        }
        if (showArrow(ArrowLocation.RIGHT_BOTTOM)) {
            elements.add(lineHRight);
            elements.add(lineIRight);
            elements.add(lineJRight);
        }
        elements.add(lineKRight);
        elements.add(bottomCurveTo);

        if (showArrow(ArrowLocation.BOTTOM_RIGHT)) {
            elements.add(lineBBottom);
            elements.add(lineCBottom);
            elements.add(lineDBottom);
        }
        if (showArrow(ArrowLocation.BOTTOM_CENTER)) {
            elements.add(lineEBottom);
            elements.add(lineFBottom);
            elements.add(lineGBottom);
        }
        if (showArrow(ArrowLocation.BOTTOM_LEFT)) {
            elements.add(lineHBottom);
            elements.add(lineIBottom);
            elements.add(lineJBottom);
        }
        elements.add(lineKBottom);
        elements.add(leftCurveTo);

        if (showArrow(ArrowLocation.LEFT_BOTTOM)) {
            elements.add(lineBLeft);
            elements.add(lineCLeft);
            elements.add(lineDLeft);
        }
        if (showArrow(ArrowLocation.LEFT_CENTER)) {
            elements.add(lineELeft);
            elements.add(lineFLeft);
            elements.add(lineGLeft);
        }
        if (showArrow(ArrowLocation.LEFT_TOP)) {
            elements.add(lineHLeft);
            elements.add(lineILeft);
            elements.add(lineJLeft);
        }
        elements.add(lineKLeft);
        elements.add(topCurveTo);

        path.getElements().setAll(elements);
    }
}