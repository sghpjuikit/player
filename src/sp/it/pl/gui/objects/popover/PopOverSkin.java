/*
 * Implementation based on ControlsFX
 *
 * Copyright (c) 2014, 2015, ControlsFX
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

package sp.it.pl.gui.objects.popover;

import java.util.ArrayList;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ListChangeListener.Change;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.Skin;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.HLineTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.VLineTo;
import javafx.stage.Window;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.pl.util.math.P;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.TIMES_CIRCLE;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.PIN;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.PIN_OFF;
import static javafx.beans.binding.Bindings.add;
import static javafx.beans.binding.Bindings.multiply;
import static javafx.beans.binding.Bindings.subtract;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static sp.it.pl.main.AppBuildersKt.resizeButton;
import static sp.it.pl.util.functional.Util.mapB;
import static sp.it.pl.util.functional.UtilKt.consumer;
import static sp.it.pl.util.graphics.MouseDragKt.initMouseDrag;
import static sp.it.pl.util.graphics.UtilKt.initClip;
import static sp.it.pl.util.graphics.UtilKt.pseudoclass;
import static sp.it.pl.util.graphics.UtilKt.setMinPrefMaxSize;
import static sp.it.pl.util.reactive.UtilKt.maintain;
import static sp.it.pl.util.reactive.UtilKt.syncTo;

public class PopOverSkin implements Skin<PopOver> {

	private static final PseudoClass DETACHED = pseudoclass("detached");
	private static final PseudoClass FOCUSED = pseudoclass("focused");
	private static final String ROOT_STYLECLASS = "popover-root";
	private static final String CONTENT_STYLECLASS = "content";
	private static final String HEADER_STYLECLASS = "popover-header";
	private static final String TITLE_STYLECLASS = "title";
	private static final String SHAPE_STYLECLASS = "bgr";

	private PopOver<? extends Node> p;
	private StackPane root;
	private final Path path;
	private final BorderPane content;
	private final Label title;
	private final BorderPane header;
	private boolean tornOff;

	public PopOverSkin(PopOver<? extends Node> popover) {
		p = popover;

		root = new StackPane() {
			@Override
			protected void layoutChildren() {
				popover.setPrefSize(getWidth(), getHeight());
				super.layoutChildren();

				path.relocate(p.computeArrowMarginX(), p.computeArrowMarginY());
				// path.resize(getWidth()+Math.abs(p.computeArrowMarginX()), getHeight()+Math.abs(p.computeArrowMarginY()));
			}
		};
		root.setPickOnBounds(false);
		root.getStyleClass().add(ROOT_STYLECLASS);
		setMinPrefMaxSize(root, Pane.USE_COMPUTED_SIZE, Pane.USE_COMPUTED_SIZE);

		//  min width and height equal 2 * corner radius + 2*arrow indent + 2*arrow size
		root.minHeightProperty().bind(root.minWidthProperty());
		root.minWidthProperty().bind(add(multiply(2, p.arrowSize), add(multiply(2, p.cornerRadius), multiply(2, p.arrowIndent))));

		// header content
		title = new Label();
		title.textProperty().bind(p.title);
		title.getStyleClass().add(TITLE_STYLECLASS);
		String closeBt = "Close\n\nClose this popup and its content.";
		Icon closeB = new Icon(TIMES_CIRCLE, 11, closeBt, p::hideStrong).styleclass("popover-close-button");
		String pinBt = "Pin\n\nWhen disabled, this popup will close on mouse click outside of this popup.";
		Icon pinB = new Icon(null, 11, pinBt, () -> {
			p.setAutoHide(!p.isAutoHide());

		});
		maintain(p.autoHideProperty(), mapB(PIN_OFF, PIN), pinB::icon);

		HBox headerControls = new HBox();
		headerControls.setSpacing(5);
		headerControls.setAlignment(Pos.CENTER_RIGHT);
		headerControls.getStyleClass().add("header-buttons");
		// initialize proper header content
		headerControls.getChildren().setAll(p.getHeaderIcons());
		headerControls.getChildren().addAll(pinB);
		// maintain proper header content
		Runnable updateHeaderButtons = () -> {
			List<Node> bs = new ArrayList<>();
			bs.addAll(p.getHeaderIcons());
			bs.add(pinB);
			if (!p.isAutoHide()) bs.add(closeB);

			headerControls.getChildren().setAll(bs);
		};
		p.getHeaderIcons().addListener((Change<?> e) -> { while (e.next()) updateHeaderButtons.run(); });
		p.autoHideProperty().addListener((o,ov,nv) -> updateHeaderButtons.run());
		updateHeaderButtons.run();

		// content
		content = new BorderPane();
		content.getStyleClass().add(CONTENT_STYLECLASS);
		setMinPrefMaxSize(content, Pane.USE_COMPUTED_SIZE, Pane.USE_COMPUTED_SIZE);
		maintain(p.contentNode, content::setCenter);
		initClip(content);

		// header
		header = new BorderPane();
		header.setLeft(title);
		header.setRight(headerControls);
		header.getStyleClass().add(HEADER_STYLECLASS);
		BorderPane.setAlignment(title, Pos.CENTER_LEFT);
		BorderPane.setAlignment(headerControls, Pos.CENTER_RIGHT);
		// header visibility
		maintain(p.headerVisible, b -> b ? header : null, content.topProperty());

		// footer
		Icon resizeB = resizeButton();
		resizeB.setPadding(new Insets(15));
		syncTo(p.userResizable, resizeB.visibleProperty());
		initMouseDrag(
			resizeB,
			new P(),
			consumer(drag -> drag.data.setXY(content.getWidth(), content.getHeight())),
			consumer(drag -> content.setPrefSize(drag.data.getX() + drag.diff.getX(), drag.data.getY() + drag.diff.getY()))
		);

		path = new Path();
		path.getStyleClass().add(SHAPE_STYLECLASS);
		path.setManaged(false);
		createPathElements();
		p.arrowLocation.addListener((o,ov,nv) -> updatePath());
		updatePath();

		// react on detached state change and initialize
		maintain(p.detached, d -> {
			updatePath();
			p.pseudoClassStateChanged(DETACHED, d);
			content.setTop(header); // always show header in detached mode
		});
		// maintain focus style
		maintain(p.focusedProperty(), v -> p.pseudoClassStateChanged(FOCUSED, v));

		// detaching
		P dragStartWLocation = new P();
		P dragStartLocation = new P();
		P dragOffset = new P();
		root.setOnMousePressed(e -> {
			if (p.detachable.get()) {
				tornOff = false;
				dragStartWLocation.setXY(p.getScene().getWindow().getX(), p.getScene().getWindow().getY());
				dragStartLocation.setXY(e.getScreenX(), e.getScreenY());
			}
		});
		root.setOnMouseDragged(e -> {
			if (p.detachable.get()) {
				Window window = p.getScene().getWindow();
				dragOffset.setX(e.getScreenX() - dragStartLocation.getX());
				dragOffset.setY(e.getScreenY() - dragStartLocation.getY());
				window.setX(dragStartWLocation.getX() + dragOffset.getX());
				window.setY(dragStartWLocation.getY() + dragOffset.getY());
				if (dragOffset.distance()>20) {
					if (!tornOff) {
						tornOff = true;
						updatePath();
					}
				} else {
					if (tornOff) {
						tornOff = false;
						updatePath();
					}
				}
			}
		});
		root.setOnMouseReleased(e -> {
			if (tornOff && !p.detached.get()) {
				tornOff = false;
				p.detached.set(true);
			}
		});

		// bug fix
		// When owning window is not focused, interacting with the popover is impossible (its focus
		// seems to be tied in with its owner window's focus). Unfortunately clicking on the popover
		// when its owner does not have focus does not focus it, which is really annoying.
		// Whether intended default behavior or javafx/popover bug, the below fixes it.
		//
		// We use filter to always execute the behavior & do not consume to not break any either.
		root.addEventFilter(MOUSE_PRESSED, e -> {
			if (!p.getOwnerWindow().isFocused())
				p.getOwnerWindow().requestFocus();
		});

		root.getChildren().add(path);
		root.getChildren().add(content);
		root.getChildren().add(resizeB);
		StackPane.setAlignment(resizeB, Pos.BOTTOM_RIGHT);
	}

	@Override
	public StackPane getNode() {
		return root;
	}

	public Pane getContent() {
		return content;
	}

	@Override
	public PopOver getSkinnable() {
		return p;
	}

	public void setTitleAsOnlyHeaderContent(boolean right) {
		header.getChildren().clear();
		if (right) header.setRight(title);
		else header.setLeft(title);
	}

	// TODO: use css instead
	/** Sets padding of content within popover. Overrides and defaults to css. */
	public void setContentPadding(Insets i) {
		// set padding from borders
		content.setPadding(i);
		// set header - content gap
		header.setPadding(new Insets(0, 0, i.getTop(), 0));
	}

	/** @return padding of content within popover. Default is css value. */
	public Insets getContentPadding() {
		return content.getPadding();
	}

	/** @return title label */
	public Labeled getTitle() {
		return title;
	}

	@Override
	public void dispose() {
		p = null;
		root = null;
	}

	private MoveTo moveTo;
	private QuadCurveTo topCurveTo, rightCurveTo, bottomCurveTo, leftCurveTo;
	private HLineTo lineBTop, lineETop, lineHTop, lineKTop;
	private LineTo lineCTop, lineDTop, lineFTop, lineGTop, lineITop, lineJTop;
	private VLineTo lineBRight, lineERight, lineHRight, lineKRight;
	private LineTo lineCRight, lineDRight, lineFRight, lineGRight, lineIRight, lineJRight;
	private HLineTo lineBBottom, lineEBottom, lineHBottom, lineKBottom;
	private LineTo lineCBottom, lineDBottom, lineFBottom, lineGBottom, lineIBottom, lineJBottom;
	private VLineTo lineBLeft, lineELeft, lineHLeft, lineKLeft;
	private LineTo lineCLeft, lineDLeft, lineFLeft, lineGLeft, lineILeft, lineJLeft;

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
		DoubleProperty cornerProperty = p.cornerRadius;
		DoubleProperty arrowSizeProperty = p.arrowSize;
		DoubleProperty arrowIndentProperty = p.arrowIndent;

		centerYProperty.bind(Bindings.divide(root.heightProperty(), 2));
		centerXProperty.bind(Bindings.divide(root.widthProperty(), 2));

		leftEdgePlusRadiusProperty.bind(add(leftEdgeProperty, p.cornerRadius));
		topEdgePlusRadiusProperty.bind(add(topEdgeProperty, p.cornerRadius));

		rightEdgeProperty.bind(root.widthProperty());
		rightEdgeMinusRadiusProperty.bind(subtract(rightEdgeProperty, p.cornerRadius));

		bottomEdgeProperty.bind(root.heightProperty());
		bottomEdgeMinusRadiusProperty.bind(subtract(bottomEdgeProperty, p.cornerRadius));

		// INIT
		moveTo = new MoveTo();
		moveTo.xProperty().bind(leftEdgePlusRadiusProperty);
		moveTo.yProperty().bind(topEdgeProperty);

		//
		// TOP EDGE
		//
		lineBTop = new HLineTo();
		lineBTop.xProperty().bind(
			add(leftEdgePlusRadiusProperty, arrowIndentProperty));

		lineCTop = new LineTo();
		lineCTop.xProperty().bind(
			add(lineBTop.xProperty(), arrowSizeProperty));
		lineCTop.yProperty().bind(
			subtract(topEdgeProperty, arrowSizeProperty));

		lineDTop = new LineTo();
		lineDTop.xProperty().bind(
			add(lineCTop.xProperty(), arrowSizeProperty));
		lineDTop.yProperty().bind(topEdgeProperty);

		lineETop = new HLineTo();
		lineETop.xProperty().bind(
			subtract(centerXProperty, arrowSizeProperty));

		lineFTop = new LineTo();
		lineFTop.xProperty().bind(centerXProperty);
		lineFTop.yProperty().bind(
			subtract(topEdgeProperty, arrowSizeProperty));

		lineGTop = new LineTo();
		lineGTop.xProperty().bind(
			add(centerXProperty, arrowSizeProperty));
		lineGTop.yProperty().bind(topEdgeProperty);

		lineHTop = new HLineTo();
		lineHTop.xProperty().bind(
			subtract(subtract(
				rightEdgeMinusRadiusProperty, arrowIndentProperty),
				multiply(arrowSizeProperty, 2)));

		lineITop = new LineTo();
		lineITop.xProperty().bind(
			subtract(subtract(
				rightEdgeMinusRadiusProperty, arrowIndentProperty),
				arrowSizeProperty));
		lineITop.yProperty().bind(
			subtract(topEdgeProperty, arrowSizeProperty));

		lineJTop = new LineTo();
		lineJTop.xProperty().bind(
			subtract(rightEdgeMinusRadiusProperty,
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
			add(topEdgeProperty, cornerProperty));
		rightCurveTo.controlXProperty().bind(rightEdgeProperty);
		rightCurveTo.controlYProperty().bind(topEdgeProperty);

		lineBRight = new VLineTo();
		lineBRight.yProperty().bind(
			add(topEdgePlusRadiusProperty, arrowIndentProperty));

		lineCRight = new LineTo();
		lineCRight.xProperty().bind(
			add(rightEdgeProperty, arrowSizeProperty));
		lineCRight.yProperty().bind(
			add(lineBRight.yProperty(), arrowSizeProperty));

		lineDRight = new LineTo();
		lineDRight.xProperty().bind(rightEdgeProperty);
		lineDRight.yProperty().bind(
			add(lineCRight.yProperty(), arrowSizeProperty));

		lineERight = new VLineTo();
		lineERight.yProperty().bind(
			subtract(centerYProperty, arrowSizeProperty));

		lineFRight = new LineTo();
		lineFRight.xProperty().bind(
			add(rightEdgeProperty, arrowSizeProperty));
		lineFRight.yProperty().bind(centerYProperty);

		lineGRight = new LineTo();
		lineGRight.xProperty().bind(rightEdgeProperty);
		lineGRight.yProperty().bind(
			add(centerYProperty, arrowSizeProperty));

		lineHRight = new VLineTo();
		lineHRight.yProperty().bind(
			subtract(subtract(
				bottomEdgeMinusRadiusProperty, arrowIndentProperty),
				multiply(arrowSizeProperty, 2)));

		lineIRight = new LineTo();
		lineIRight.xProperty().bind(
			add(rightEdgeProperty, arrowSizeProperty));
		lineIRight.yProperty().bind(
			subtract(subtract(
				bottomEdgeMinusRadiusProperty, arrowIndentProperty),
				arrowSizeProperty));

		lineJRight = new LineTo();
		lineJRight.xProperty().bind(rightEdgeProperty);
		lineJRight.yProperty().bind(
			subtract(bottomEdgeMinusRadiusProperty,
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
			subtract(rightEdgeMinusRadiusProperty,
				arrowIndentProperty));

		lineCBottom = new LineTo();
		lineCBottom.xProperty().bind(
			subtract(lineBBottom.xProperty(), arrowSizeProperty));
		lineCBottom.yProperty().bind(
			add(bottomEdgeProperty, arrowSizeProperty));

		lineDBottom = new LineTo();
		lineDBottom.xProperty().bind(
			subtract(lineCBottom.xProperty(), arrowSizeProperty));
		lineDBottom.yProperty().bind(bottomEdgeProperty);

		lineEBottom = new HLineTo();
		lineEBottom.xProperty().bind(
			add(centerXProperty, arrowSizeProperty));

		lineFBottom = new LineTo();
		lineFBottom.xProperty().bind(centerXProperty);
		lineFBottom.yProperty().bind(
			add(bottomEdgeProperty, arrowSizeProperty));

		lineGBottom = new LineTo();
		lineGBottom.xProperty().bind(
			subtract(centerXProperty, arrowSizeProperty));
		lineGBottom.yProperty().bind(bottomEdgeProperty);

		lineHBottom = new HLineTo();
		lineHBottom.xProperty().bind(
			add(add(leftEdgePlusRadiusProperty,
				arrowIndentProperty), multiply(
				arrowSizeProperty, 2)));

		lineIBottom = new LineTo();
		lineIBottom.xProperty().bind(
			add(add(leftEdgePlusRadiusProperty,
				arrowIndentProperty), arrowSizeProperty));
		lineIBottom.yProperty().bind(
			add(bottomEdgeProperty, arrowSizeProperty));

		lineJBottom = new LineTo();
		lineJBottom.xProperty().bind(
			add(leftEdgePlusRadiusProperty, arrowIndentProperty));
		lineJBottom.yProperty().bind(bottomEdgeProperty);

		lineKBottom = new HLineTo();
		lineKBottom.xProperty().bind(leftEdgePlusRadiusProperty);

		//
		// LEFT EDGE
		//
		leftCurveTo = new QuadCurveTo();
		leftCurveTo.xProperty().bind(leftEdgeProperty);
		leftCurveTo.yProperty().bind(
			subtract(bottomEdgeProperty, cornerProperty));
		leftCurveTo.controlXProperty().bind(leftEdgeProperty);
		leftCurveTo.controlYProperty().bind(bottomEdgeProperty);

		lineBLeft = new VLineTo();
		lineBLeft.yProperty().bind(
			subtract(bottomEdgeMinusRadiusProperty,
				arrowIndentProperty));

		lineCLeft = new LineTo();
		lineCLeft.xProperty().bind(
			subtract(leftEdgeProperty, arrowSizeProperty));
		lineCLeft.yProperty().bind(
			subtract(lineBLeft.yProperty(), arrowSizeProperty));

		lineDLeft = new LineTo();
		lineDLeft.xProperty().bind(leftEdgeProperty);
		lineDLeft.yProperty().bind(
			subtract(lineCLeft.yProperty(), arrowSizeProperty));

		lineELeft = new VLineTo();
		lineELeft.yProperty().bind(
			add(centerYProperty, arrowSizeProperty));

		lineFLeft = new LineTo();
		lineFLeft.xProperty().bind(
			subtract(leftEdgeProperty, arrowSizeProperty));
		lineFLeft.yProperty().bind(centerYProperty);

		lineGLeft = new LineTo();
		lineGLeft.xProperty().bind(leftEdgeProperty);
		lineGLeft.yProperty().bind(
			subtract(centerYProperty, arrowSizeProperty));

		lineHLeft = new VLineTo();
		lineHLeft.yProperty().bind(
			add(add(topEdgePlusRadiusProperty,
				arrowIndentProperty), multiply(
				arrowSizeProperty, 2)));

		lineILeft = new LineTo();
		lineILeft.xProperty().bind(
			subtract(leftEdgeProperty, arrowSizeProperty));
		lineILeft.yProperty().bind(
			add(add(topEdgePlusRadiusProperty,
				arrowIndentProperty), arrowSizeProperty));

		lineJLeft = new LineTo();
		lineJLeft.xProperty().bind(leftEdgeProperty);
		lineJLeft.yProperty().bind(
			add(topEdgePlusRadiusProperty, arrowIndentProperty));

		lineKLeft = new VLineTo();
		lineKLeft.yProperty().bind(topEdgePlusRadiusProperty);

		topCurveTo = new QuadCurveTo();
		topCurveTo.xProperty().bind(leftEdgePlusRadiusProperty);
		topCurveTo.yProperty().bind(topEdgeProperty);
		topCurveTo.controlXProperty().bind(leftEdgeProperty);
		topCurveTo.controlYProperty().bind(topEdgeProperty);
	}

	private boolean showArrow(ArrowLocation loc) {
		ArrowLocation arrowLocation = p.arrowLocation.get();
		return loc.equals(arrowLocation) && !p.detached.get() && !tornOff;
	}

	public void updatePath() {
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