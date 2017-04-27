package gui.itemnode;

import gui.itemnode.ItemNode.ValueNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import util.dev.TODO;
import util.functional.Functors;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.layout.Priority.ALWAYS;
import static util.dev.TODO.Purpose.FUNCTIONALITY;
import static util.dev.TODO.Severity.SEVERE;
import static util.functional.Util.*;

// TODO: manual text edits should be part of transformation chain. It
// is doable like this:
// - all subsequent edits until another function is used are condensed into single 'function'
//   which simply returns the edited text. There could be some trouble with syncing the output
//   and getValue(), but its doable
// - this will lock all preceding transformations as our edit function can not react on changes
//   in previous transformations
/**
 * List editor with transformation ability. Editable area with function editor
 * displaying the list contents.
 * <p/>
 * Allows:
 * <ul>
 * <li> displaying the elements of the list as strings in the text area
 * <li> manual editing of the text in the text area
 * <li> creating and applying arbitrary function chain transformation on the list
 * elements producing list of different type
 * </ul>
 * <p/>
 * The input list is retained and all (if any) transformations are applied on it
 * every time a change in the transformation chain occurs. Every time the text is
 * updated, all manual changes of the text are lost.
 * <p/>
 * The input can be set:
 * <ul>
 * <li> as string which will be split by lines to list of strings. {@link #setData(java.lang.String)}
 * <li> list of objects. The list must be homogeneous. {@link #setData(java.util.List)}
 * </ul>
 * but is not necessary. The area can be used without input. Transformation chain
 * starts a Void.class which still allows functions that supply value, which in
 * return again allows scaling the transformation chain without limitation.
 * <p/>
 * The result can be accessed as:
 * <ul>
 * <li> text at any time equal to the visible text of the text area. {@link #getValueAsS()}
 * <li> list of strings. Each string element represents a single line in the text
 * area. List can be empty, but not null. {@link #getValue() }
 * </ul>
 */
public class ListAreaNode extends ValueNode<List<String>> {

	protected final TextArea textarea = new TextArea();
	private final VBox root = new VBox();
	private final List<Object> input = new ArrayList<>();
	public final FChainItemNode transforms = new FChainItemNode(Functors.pool::getI);
	/**
	 * Output list, i.e., input list after transformation of each element.
	 * The text of this area shows string representation of this list.
	 * <p/>
	 * Note that the area is editable, but the changes
	 * will (and possibly could) only be reflected in this list if its type is {@link String}, i.e.,
	 * if the last transformation transforms into String. This is apparent from the fact that
	 * this object is considered a String transformer (see {@link $getValue()}).
	 * When user manually edits the text, then if:
	 * <ul>
	 * <li>output type is String: it is considered a transformation of that text and it will be
	 * reflected in this list, i.e., {@link #getValue() )} and this will contain equal elements
	 * <li?output type is not Stirng: it is considered arbitrary user change of the text representation
	 * of transformation output (i.e., this list), but not the output itself.
	 * <p/>
	 * In fact, when further transforming the elements, the manual edit will be ignored, and this is
	 * true even for String due to imperfect implementation.
	 * </ul>
	 * <p/>
	 * When observing this list, changes of the text area will only be reflected in it (and fire
	 * list change events) when the output type is String. You may observe the text directly using
	 * {@link #output_string}
	 */
	public final ObservableList<Object> output = FXCollections.observableArrayList();
	/**
	 * Text of the text area. Editable both graphically and programmatically.
	 * <p/>
	 * The text of this area shows string representation of the transformation output {@link #output}
	 * and may not reflect it exactly when edited.
	 */
	public final StringProperty output_string = textarea.textProperty();

	public ListAreaNode() {
		transforms.onItemChange = f -> {
			List l = map(input, f);
			if (transforms.getTypeOut()!=String.class)   // prevents duplicate update
				output.setAll(l);
			textarea.setText(toS(l, Objects::toString, "\n"));     // update the text -> update the value
		};
		textarea.textProperty().addListener((o, ov, nv) -> {
			List<String> newval = split(nv, "\n", x -> x);
			changeValue(newval);
			// Capture manual changes. Only String can do this, but it only makes sense for String
			// too. For other types editing the text can't be reflected in the output, because
			// the text does not represent the output of the transformations in the first place - it
			// is only its representation (which user can freely change and use as he wants).
			//
			// Hovewer if the type is string, the manual edit must be considered part of the
			// transformation outputs. Of course, this will still ignore edits done in between individual
			// transformations, but if someone uses the transformation output, it will be there, which
			// is progress.
			if (transforms.getTypeOut()==String.class)
				output.setAll(newval);
		});
		// layout
		root.getChildren().addAll(textarea, transforms.getNode());
		VBox.setVgrow(textarea, ALWAYS);

		textarea.addEventHandler(KEY_PRESSED, e -> {
			if (e.getCode()==KeyCode.V && e.isControlDown()) {
				e.consume();
			}
		});
	}

	/**
	 * Sets the input list directly. Must be homogeneous - all elements of the
	 * same type.
	 * <p/>
	 * Discards previous input list. Transformation chain is cleared if the type
	 * of list has changed.
	 * Updates text of the text area.
	 */
	public void setData(List<?> data) {
		// In different implementation we could simply store the list as input like: input=data;
		// 1) could cause memory leak (hard reference)
		// 2) potentially dangerous (e.h. if areas output was set as its own input...)
		// 3) if it is observable, we could (with a bit of impl. change) invoke changes
		// We use non-observable final ordinary list and know to be safe.
		input.clear();
		input.addAll(data);
		Class<?> ec = getElementClass(data);
		transforms.setTypeIn(ec);    // fires update
	}

	/**
	 * Sets the input list by splitting the text by '\n' newline character.
	 * <p/>
	 * Discards previous input list. Transformation chain is cleared if the type
	 * of list has changed.
	 * Updates text of the text area.
	 */
	public void setData(String text) {
		setData(split(text, "\n", c -> c));
	}

	/**
	 * Returns the input that was last set or empty list if none, never null. Modifying the list
	 * will have no effect on this (but modifying its elements might).
	 */
	public List<Object> getInput() {
		// We wrap the list to avoid possible changes from outside.
		return list(input);
	}

	/**
	 * Returns the current text in the text area. Represents concatenation of
	 * string representations of the elements of the list after transformation,
	 * if any.
	 */
	public String getValueAsS() {
		return textarea.getText();
	}

	@Override
	public VBox getNode() {
		return root;
	}

	// TODO:implement properly, this is just embarrassing
	private static <E> Class<?> getElementClass(Collection<E> c) {
		for (E e : c) if (e!=null) return e.getClass();
		return Void.class;
	}

}