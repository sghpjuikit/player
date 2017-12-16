/*
 * Implementation based on ControlsF:
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

package sp.it.pl.gui.objects.textfield.autocomplete;

import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Skin;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import sp.it.pl.gui.objects.textfield.autocomplete.AutoCompletePopup.SuggestionEvent;
import static javafx.beans.binding.Bindings.min;
import static javafx.beans.binding.Bindings.size;

public class AutoCompletePopupSkin<T> implements Skin<AutoCompletePopup<T>> {
	private final AutoCompletePopup<T> control;
	private final ListView<T> list;

	public AutoCompletePopupSkin(AutoCompletePopup<T> control) {
		this(control, 1);
	}

	public AutoCompletePopupSkin(AutoCompletePopup<T> control, int activationClickCount) {
		this.control = control;

		double reserve = 12; // removes vertical scrollbar
		list = new ListView<>(control.getSuggestions());
		list.setFixedCellSize(20);  // TODO: avoid hardcoded value
		list.prefHeightProperty().bind(
			min(control.visibleRowCountProperty(), size(list.getItems()))
				.multiply(list.fixedCellSizeProperty())
				.add(reserve));
		list.setCellFactory(this::buildListViewCellFactory);
		list.setOnMouseClicked(e -> {
			if (e.getButton()==MouseButton.PRIMARY && e.getClickCount()==activationClickCount)
				chooseSuggestion();
		});
		list.setOnKeyPressed(e -> {
			switch (e.getCode()) {
				case ENTER: chooseSuggestion();
					break;
				case ESCAPE: if (control.isHideOnEscape()) control.hide();
					break;
				default: break;
			}
			e.consume();
		});
	}

	@Override
	public Region getNode() {
		return list;
	}

	@Override
	public AutoCompletePopup<T> getSkinnable() {
		return control;
	}

	@Override
	public void dispose() {}

	private void chooseSuggestion() {
		onSuggestionChosen(list.getSelectionModel().getSelectedItem());
	}

	private void onSuggestionChosen(T suggestion) {
		if (suggestion!=null && getSkinnable().onSuggestion.get()!=null)
			getSkinnable().onSuggestion.get().handle(new SuggestionEvent<>(suggestion));
	}

	protected ListCell<T> buildListViewCellFactory(ListView<T> listView) {
		return TextFieldListCell.forListView(control.getConverter()).call(listView);
	}
}