package gui.itemnode.textfield;

import gui.objects.textfield.DecoratedTextField;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import javafx.scene.control.TextField;
import javafx.util.Callback;
import util.access.AccessibleValue;
import static util.dev.Util.noØ;

/**
 * Customized {@link TextField} that displays a nullable object value. Normally a non-editable text
 * field that brings up a popup picker for its item type. Useful as property editor with value selection feature.
 * <p/>
 * Text field displays the value by having a to String converter {@link #valueFactory}.
 * <p/>
 * In addition there is a dialog button calling implementation dependant item
 * chooser expected in form of a pop-up.
 *
 * @param <T> type of the value
 * @author Martin Polakovic
 */
public abstract class TextFieldItemNode<T> extends DecoratedTextField implements AccessibleValue<T> {

	/**
	 * Returns style class as text field.
	 * Should be: text-input, text-field.
	 */
	public static List<String> STYLE_CLASS() {
		// debug (prints: text-input, text-field)
		// new TextField().getStyleClass().forEach(System.out::println);

		// manually
		List<String> out = new ArrayList<>();
		out.add("text-input");
		out.add("text-field");
		return out;
	}

	T v;
	private BiConsumer<T,T> onValueChange;
	private final Callback<T,String> valueFactory;
	private final String nullText = "<none>";

	/**
	 * Constructor. Creates instance of the item text field utilizing parser of the provided type.
	 *
	 * @param valueFactory nonnull item to string converter that will never receive null
	 * @throws RuntimeException if any param null
	 */
	public TextFieldItemNode(Callback<T,String> valueFactory) {
		this.valueFactory = noØ(valueFactory);
		setEditable(false);
		getStyleClass().setAll(STYLE_CLASS());    //set same css style as TextField
		setText(nullText);
		setPromptText(nullText);

		// set the button to the right & action
		setRight(new ArrowDialogButton());
		getRight().setOnMouseClicked(e -> onDialogAction());
	}

	/** Behavior to be executed on dialog button click. Should cause an execution of an {@link #setValue(Object)}. */
	abstract void onDialogAction();

	/**
	 * Sets item for this text field. Sets text and prompt text according to provided implementation.<br/>
	 * The item change event is fired unless the value is the same according to object reference ({@literal ==}) or
	 * {@link Object#equals(Object)}.
	 *
	 * @param value nullable value
	 */
	@Override
	public void setValue(T value) {
		T ov = v;
		if (ov==value || (ov!=null && value!=null && ov.equals(value))) return;

		v = value;
		String text = value==null ? nullText : valueFactory.call(value);
		setText(text);
		setPromptText(text);
		if (onValueChange!=null) onValueChange.accept(ov, value);
	}

	/** @return current value displayed in this text field. */
	@Override
	public T getValue() {
		return v;
	}

	/**
	 * Sets behavior to execute when item changes. Value change conditions are explained in {@link #setValue(Object)}.
	 *
	 * @param onValueChange nullable change handler
	 */
	public void setOnItemChange(BiConsumer<T,T> onValueChange) {
		this.onValueChange = onValueChange;
	}

}