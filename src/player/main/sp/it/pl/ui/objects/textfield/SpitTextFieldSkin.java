/*
 * Implementation based on ControlsFX
 *
 * Copyright (c) 2013, 2015 ControlsFX
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

package sp.it.pl.ui.objects.textfield;

import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.skin.TextFieldSkin;
import javafx.scene.layout.HBox;
import javafx.scene.text.HitInfo;
import sp.it.util.reactive.Disposer;
import static sp.it.util.functional.UtilKt.runnable;
import static sp.it.util.reactive.UnsubscribableKt.on;
import static sp.it.util.reactive.UtilKt.onChange;
import static sp.it.util.reactive.UtilKt.syncC;
import static sp.it.util.ui.UtilKt.pseudoclass;

public class SpitTextFieldSkin extends TextFieldSkin {

	private static final PseudoClass HAS_NO_SIDE_NODE = pseudoclass("no-visible");
	private static final PseudoClass HAS_LEFT_NODE = pseudoclass("left-visible");
	private static final PseudoClass HAS_RIGHT_NODE = pseudoclass("right-visible");

	private HBox leftPane;
	private HBox rightPane;
	private final Disposer onDispose = new Disposer();

	public SpitTextFieldSkin(SpitTextField control) {
		super(control);
	}

	@Override
	public void install() {
		super.install();

		var control = (SpitTextField) getSkinnable();
		on(onChange(control.getLeft(), runnable(() -> updateChildren())), onDispose);
		on(onChange(control.getRight(), runnable(() -> updateChildren())), onDispose);
		on(syncC(control.focusedProperty(), e -> updateChildren()), onDispose);
		updateChildren();
	}

	@Override
	public void dispose() {
		onDispose.invoke();
		super.dispose();
	}

	private void updateChildren() {
		var newLeft = ((SpitTextField) getSkinnable()).getLeft();
		if (!newLeft.isEmpty()) {
			getChildren().remove(leftPane);
			leftPane = new HBox(newLeft.toArray(Node[]::new));
			leftPane.setManaged(false);
			leftPane.getStyleClass().add("left-pane");
			getChildren().add(leftPane);
		} else {
			if (leftPane!=null) getChildren().remove(leftPane);
			leftPane = null;
		}

		var newRight = ((SpitTextField) getSkinnable()).getRight();
		if (!newRight.isEmpty()) {
			getChildren().remove(rightPane);
			rightPane = new HBox(newRight.toArray(Node[]::new));
			rightPane.setManaged(false);
			rightPane.getStyleClass().add("right-pane");
			getChildren().add(rightPane);
		} else {
			if (rightPane!=null) getChildren().remove(rightPane);
			rightPane = null;
		}

		getSkinnable().pseudoClassStateChanged(HAS_LEFT_NODE, !newLeft.isEmpty());
		getSkinnable().pseudoClassStateChanged(HAS_RIGHT_NODE, !newRight.isEmpty());
		getSkinnable().pseudoClassStateChanged(HAS_NO_SIDE_NODE, !newLeft.isEmpty() && !newRight.isEmpty());
	}

	@Override
	protected void layoutChildren(double x, double y, double w, double h) {
		final double fullHeight = h + snappedTopInset() + snappedBottomInset();

		final double leftWidth = leftPane==null ? 0.0 : snapSizeX(leftPane.prefWidth(fullHeight));
		final double rightWidth = rightPane==null ? 0.0 : snapSizeX(rightPane.prefWidth(fullHeight));

		final double textFieldStartX = snapPositionX(x) + snapSizeX(leftWidth);
		final double textFieldWidth = w - snapSizeX(leftWidth) - snapSizeX(rightWidth);

		super.layoutChildren(textFieldStartX, 0, textFieldWidth, fullHeight);

		if (leftPane!=null) {
			final double leftStartX = 0;
			leftPane.resizeRelocate(leftStartX, 0, leftWidth, fullHeight);
		}

		if (rightPane!=null) {
			final double rightStartX = w - rightWidth + snappedLeftInset();
			rightPane.resizeRelocate(rightStartX, 0, rightWidth, fullHeight);
		}
	}

	@Override
	public HitInfo getIndex(double x, double y) {
		// This resolves https://bitbucket.org/controlsfx/controlsfx/issue/476
		// when we have a left Node and the click point is badly returned
		// because we weren't considering the shift induced by the leftPane.
		final double leftWidth = leftPane==null ? 0.0 : snapSizeX(leftPane.prefWidth(getSkinnable().getHeight()));
		return super.getIndex(x - leftWidth, y);
	}

	@Override
	protected double computePrefWidth(double h, double topInset, double rightInset, double bottomInset, double leftInset) {
		final double pw = super.computePrefWidth(h, topInset, rightInset, bottomInset, leftInset);
		final double leftWidth = leftPane==null ? 0.0 : snapSizeX(leftPane.prefWidth(h));
		final double rightWidth = rightPane==null ? 0.0 : snapSizeX(rightPane.prefWidth(h));

		return pw + leftWidth + rightWidth;
	}

	@Override
	protected double computePrefHeight(double w, double topInset, double rightInset, double bottomInset, double leftInset) {
		final double ph = super.computePrefHeight(w, topInset, rightInset, bottomInset, leftInset);
		final double leftHeight = leftPane==null ? 0.0 : snapSizeY(leftPane.prefHeight(-1));
		final double rightHeight = rightPane==null ? 0.0 : snapSizeY(rightPane.prefHeight(-1));

		return Math.max(ph, Math.max(leftHeight, rightHeight));
	}

}