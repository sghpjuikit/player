/*
 * Impl based on ControlsFX
 *
 * Copyright (c) 2013, 2015, ControlsFX
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

package gui.objects.textfield;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Skin;
import javafx.scene.control.TextField;

/**
 * {@link TextField}, which can be decorated with nodes inside on the left an right.
 */
public class DecoratedTextField extends TextField {

    public final ObjectProperty<Node> left = new SimpleObjectProperty<>(this, "left");
    public final ObjectProperty<Node> right = new SimpleObjectProperty<>(this, "right");

    /**
     * Instantiates a default CustomTextField.
     */
    public DecoratedTextField() {
        getStyleClass().add("custom-text-field");
        setMinWidth(TextField.USE_PREF_SIZE);
        setMaxWidth(TextField.USE_PREF_SIZE);
    }

	/**
	 * @see #left
	 */
    public final ObjectProperty<Node> leftProperty() {
        return left;
    }

	/**
	 * @see #left
	 */
    public final Node getLeft() {
        return left.get();
    }

	/**
	 * @see #left
	 */
    public final void setLeft(Node value) {
        left.set(value);
    }

    /**
     * @see #right
     */
    public final ObjectProperty<Node> rightProperty() {
        return right;
    }

	/**
	 * @see #right
	 */
    public final Node getRight() {
        return right.get();
    }

	/**
	 * @see #right
	 */
    public final void setRight(Node value) {
        right.set(value);
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new DecoratedTextFieldSkin(this);
    }
}