package sp.it.pl.ui.objects.combobox;

import java.util.function.Function;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.input.KeyCode;
import sp.it.pl.ui.objects.search.Search;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static sp.it.util.dev.FailKt.noNull;

/**
 * ComboBox with added functionalities.
 * <ul>
 * <li> String converter
 * <li> Default text for empty value
 * <li> Better keyboard UX (SPACE key press shows popup)
 * <li> Searching & scrolling when typing
 * </ul>
 */
public class ImprovedComboBox<T> extends ComboBox<T> {

	/** Text for when no value is selected. Default {@code "<none>"} */
	public final String emptyText;
	/** String converter for cell value factory. Default is Object::toString */
	public final Function<T,String> toStringConverter;
	/** String converter for cell value factory. Default is Object::toString */
	public final javafx.util.StringConverter<T> toStringConverterFx;
	/**
	 * Item search. Has no graphics.
	 */
	protected final Search search = new Search() {
		@Override
		public void doSearch(String query) {
			@SuppressWarnings("unchecked")
			ListView<T> items = (ListView) ((ComboBoxListViewSkin) getSkin()).getPopupContent();
			// scroll to match
			if (!getItems().isEmpty()) {
				for (int i = 0; i<getItems().size(); i++) {
					T e = getItems().get(i);
					String es = toStringConverterFx.toString(e);
					if (isMatchNth(es, query)) {
						items.scrollTo(i);
						// items.getSelectionModel().select(i); // TODO: make this work reasonably well
						break;
					}
				}
			}
		}

		@Override
		public boolean isMatch(String text, String query) {
			return text.toLowerCase().contains(query.toLowerCase());
		}
	};

	/** Equivalent to {@code this(Object::toString)}. */
	public ImprovedComboBox() {
		this(Object::toString);
	}

	/** Equivalent to {@code this(Object::toString, "<none>")}. */
	public ImprovedComboBox(Function<T,String> toS) {
		this(toS, "<none>");
	}

	/**
	 * @param toS to string converter (it will never receive null)
	 * @param empty_text text to use for null value
	 */
	public ImprovedComboBox(Function<T,String> toS, String empty_text) {
		noNull(toS, () -> empty_text);


		this.emptyText = empty_text;
		this.toStringConverter = toS;
		this.toStringConverterFx = new javafx.util.StringConverter<>() {
			@Override public String toString(T object) {
				return object==null ? empty_text : toStringConverter.apply(object);
			}
			@Override public T fromString(String string) {
				return null;
			}
		};

		// we need to set the converter specifically or the combobox cell wont get updated sometimes
		setConverter(toStringConverterFx);
		setCellFactory(view -> new ImprovedComboBoxListCell<>(this));
		setButtonCell(getCellFactory().call(null));
		setValue(null);

		// search
		search.installOn(this);

		// improved keyboard UX
		addEventHandler(KEY_PRESSED, e -> {
			if (!e.isConsumed() && e.getCode()==KeyCode.SPACE && !isShowing() && !getItems().isEmpty()) {
				show();
				@SuppressWarnings("unchecked")
				ListView<T> items = (ListView) ((ComboBoxListViewSkin) getSkin()).getPopupContent();
				items.requestFocus();
			}
		});
	}

	public static class ImprovedComboBoxListCell<T> extends ListCell<T> {   // do not extend ComboBoxListCell! causes problems!
		public final javafx.util.StringConverter<T> converter;

		public ImprovedComboBoxListCell(ImprovedComboBox<T> comboBox) {
			this.converter = comboBox.toStringConverterFx;
		}

		@Override
		protected void updateItem(T item, boolean empty) {
			super.updateItem(item, empty);
			setText(converter.toString(empty ? null : item));
		}
	}
}