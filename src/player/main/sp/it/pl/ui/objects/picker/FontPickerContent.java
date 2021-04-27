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

package sp.it.pl.ui.objects.picker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import org.jetbrains.annotations.NotNull;
import static javafx.collections.FXCollections.observableArrayList;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.layout.Priority.NEVER;
import static javafx.scene.layout.Priority.SOMETIMES;
import static sp.it.pl.main.AppExtensionsKt.getEmScaled;
import static sp.it.util.reactive.EventsKt.propagateESCAPE;

/** Content for picking {@link javafx.scene.text.Font} */
public class FontPickerContent extends GridPane {
	private static final double HGAP = 10;
	private static final double VGAP = 5;

	private static final Predicate<Object> MATCH_ALL = t -> true;
	private static final Double[] fontSizes = new Double[]{8d, 9d, 11d, 12d, 14d, 16d, 18d, 20d, 22d, 24d, 26d, 28d, 36d, 48d, 72d};

	private static List<FontStyle> getFontStyles(String fontFamily) {
		Set<FontStyle> set = new HashSet<>();
		for (String f : Font.getFontNames(fontFamily)) {
			set.add(new FontStyle(f.replace(fontFamily, "")));
		}

		List<FontStyle> result = new ArrayList<>(set);
		Collections.sort(result);
		return result;
	}

	private final ObservableList<String> filteredFontList = observableArrayList(Font.getFamilies());
	private final ObservableList<FontStyle> filteredStyleList = observableArrayList();
	private final ObservableList<Double> filteredSizeList = observableArrayList(fontSizes);

	private final ListView<String> fontListView = new ListView<>(filteredFontList);
	private final ListView<FontStyle> styleListView = new ListView<>(filteredStyleList);
	private final ListView<Double> sizeListView = new ListView<>(filteredSizeList);
	private final TextArea sampleArea = new TextArea("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.");

	public FontPickerContent() {
		setHgap(HGAP);
		setVgap(VGAP);
		setMinSize(getEmScaled(500), getEmScaled(300));
		setPrefSize(getEmScaled(600), getEmScaled(400));

		ColumnConstraints c0 = new ColumnConstraints();
		c0.setPercentWidth(60);
		ColumnConstraints c1 = new ColumnConstraints();
		c1.setPercentWidth(25);
		ColumnConstraints c2 = new ColumnConstraints();
		c2.setPercentWidth(15);
		getColumnConstraints().addAll(c0, c1, c2);

		RowConstraints r0 = new RowConstraints();
		r0.setVgrow(NEVER);
		RowConstraints r1 = new RowConstraints();
		r1.setVgrow(SOMETIMES);
		RowConstraints r2 = new RowConstraints();
		r2.setFillHeight(true);
		r2.setVgrow(NEVER);
		RowConstraints r3 = new RowConstraints();
		r3.setPrefHeight(getEmScaled(250.0));
		r3.setVgrow(NEVER);
		getRowConstraints().addAll(r0, r1, r2, r3);

		add(new Label("Font"), 0, 0);
		add(fontListView, 0, 1);
		propagateESCAPE(fontListView);
		fontListView.setCellFactory(stringListView -> new ListCell<>() {
			@Override
			public void updateItem(String family, boolean empty) {
				super.updateItem(family, empty);
				if (empty) {
					setText(null);
				} else {
					setFont(Font.font(family, getEmScaled(12.0)));
					setText(family);
				}
			}
		});

		ChangeListener<Object> sampleRefreshListener = (o, ov, nv) -> refreshSample();

		fontListView.selectionModelProperty().get().selectedItemProperty().addListener((o, ov, nv) -> {
			String fontFamily = selectedOr(fontListView, null);
			styleListView.setItems(observableArrayList(getFontStyles(fontFamily)));
			styleListView.getSelectionModel().select(styleListView.getItems().stream().findFirst().orElse(null));

			refreshSample();
		});
		fontListView.addEventHandler(KEY_PRESSED, e -> {
			if (e.getCode().isLetterKey()) {
				filteredFontList.stream().filter(it -> it.toLowerCase().startsWith(e.getText().toLowerCase())).findFirst().ifPresent(it -> {
					fontListView.getSelectionModel().select(it);
					fontListView.scrollTo(it);
				});
				e.consume();
			}
		});

		add(new Label("Style"), 1, 0);
		add(styleListView, 1, 1);
		propagateESCAPE(styleListView);
		styleListView.selectionModelProperty().get().selectedItemProperty().addListener(sampleRefreshListener);

		add(new Label("Label"), 2, 0);
		add(sizeListView, 2, 1);
		propagateESCAPE(sizeListView);
		sizeListView.selectionModelProperty().get().selectedItemProperty().addListener(sampleRefreshListener);

		sampleArea.setWrapText(true);
		StackPane sampleStack = new StackPane(sampleArea);
		sampleStack.setPadding(new Insets(getEmScaled(24.0), 0.0, 0.0, 0.0));
		add(sampleStack, 0, 3, 3, 3);
	}

	public void setFont(@NotNull Font font) {
		selectInList(fontListView, font.getFamily());
		selectInList(styleListView, new FontStyle(font));
		selectInList(sizeListView, font.getSize());
	}

	public Font getFont() {
		FontStyle style = selectedOr(styleListView, null);
		if (style==null) {
			return Font.font(selectedOr(fontListView, null), selectedOr(sizeListView, 12.0));

		} else {
			return Font.font(selectedOr(fontListView, null), style.getWeight(), style.getPosture(), selectedOr(sizeListView, 12.0));
		}
	}

	private void refreshSample() {
		sampleArea.setFont(getFont());
	}

	private <T> void selectInList(ListView<T> listView, T selection) {
		listView.getSelectionModel().select(selection);
		listView.scrollTo(selection);
	}

	private <T> T selectedOr(ListView<T> listView, T or) {
		var t = listView.getSelectionModel().getSelectedItem();
		return t!=null ? t : or;
	}

	/**
	 * Font style as combination of font weight and font posture.
	 * Weight does not have to be there (represented by null)
	 * Posture is required, null posture is converted to REGULAR
	 */
	private static class FontStyle implements Comparable<FontStyle> {

		private FontPosture posture;
		private FontWeight weight;

		public FontStyle(FontWeight weight, FontPosture posture) {
			this.posture = posture==null ? FontPosture.REGULAR : posture;
			this.weight = weight;
		}

		public FontStyle() {
			this(null, null);
		}

		public FontStyle(String styles) {
			this();
			String[] fontStyles = (styles==null ? "" : styles.trim().toUpperCase()).split(" ");
			for (String style : fontStyles) {
				FontWeight w = FontWeight.findByName(style);
				if (w!=null) {
					weight = w;
				} else {
					FontPosture p = FontPosture.findByName(style);
					if (p!=null) posture = p;
				}
			}
		}

		public FontStyle(Font font) {
			this(font.getStyle());
		}

		public FontPosture getPosture() {
			return posture;
		}

		public FontWeight getWeight() {
			return weight;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime*result + ((posture==null) ? 0 : posture.hashCode());
			result = prime*result + ((weight==null) ? 0 : weight.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object that) {
			if (this==that) return true;
			if (that==null) return false;
			if (getClass()!=that.getClass()) return false;
			FontStyle other = (FontStyle) that;
			if (posture!=other.posture) return false;
			return weight==other.weight;
		}

		private static String makePretty(Object o) {
			String s = o==null ? "" : o.toString();
			if (!s.isEmpty()) {
				s = s.replace("_", " ");
				s = s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
			}
			return s;
		}

		@Override
		public String toString() {
			return String.format("%s %s", makePretty(weight), makePretty(posture)).trim();
		}

		private <T extends Enum<T>> int compareEnums(T e1, T e2) {
			if (e1==e2) return 0;
			if (e1==null) return -1;
			if (e2==null) return 1;
			return e1.compareTo(e2);
		}

		@Override
		public int compareTo(FontStyle fs) {
			int result = compareEnums(weight, fs.weight);
			return (result!=0) ? result : compareEnums(posture, fs.posture);
		}

	}
}
