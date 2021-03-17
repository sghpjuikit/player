package sp.it.pl.ui.objects.contextmenu;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import javafx.beans.property.Property;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import sp.it.pl.ui.objects.icon.CheckIcon;
import sp.it.util.functional.Functors.F1;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static sp.it.util.dev.FailKt.noNull;
import static sp.it.util.functional.Util.by;
import static sp.it.util.functional.Util.map;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.ui.UtilKt.styleclassToggle;

/**
 * Simple {@link Menu} implementation with check icon. Clicking on the icon
 * always has the same effect as clicking on the menu item text. The icon
 * selection changes on/off on mouse click but this default behavior can be
 * changed using {@link #setOnMouseClicked(java.lang.Runnable)} which overrides
 * it.
 * <p/>
 * Features:
 * <ul>
 * <li> Check icon reflecting selection state
 * <li> Observable selection state
 * <li> Custom click implementation
 * </ul>
 */
public class SelectionMenuItem extends Menu {

	private static final String STYLECLASS = "checkicon-menu-item";
	private static final String STYLECLASS_ICON = "checkicon-menu-item-icon";
	private static final String STYLECLASS_ICON_SINGLE_SEL = "checkicon-menu-item-icon-single-selection";
	private static final String NO_CHILDREN_STYLECLASS = "menu-nochildren";

	private final CheckIcon icon = new CheckIcon(false);
	/** Selection state reflected by the icon. Changes on click. Default false. */
	public final Property<Boolean> selected = icon.selected;

	/**
	 * Creates menu item with specified text and selection.
	 *
	 * @param text text of this menu item
	 * @param s initial selection state
	 */
	public SelectionMenuItem(String text, boolean s) {
		super(text);
		setGraphic(icon);
		icon.styleclass(STYLECLASS_ICON);
		icon.gap(0);
		selected.setValue(s);

		// action = toggle selection
		setOnMouseClicked(() -> selected.setValue(!selected.getValue()));

		// hide open submenu arrow if no children
		getStyleClass().add(NO_CHILDREN_STYLECLASS);
		getItems().addListener((ListChangeListener<MenuItem>) c ->
			styleclassToggle(this, NO_CHILDREN_STYLECLASS, c.getList().isEmpty())
		);
	}

	/**
	 * Creates menu item and adds selection listener for convenience.
	 *
	 * @param text text of this menu item
	 * @param s initial selection state
	 * @param sh selection listener to add. Equivalent to: {@code selected.addListener((o,ov,nv) -> sel_han.accept(nv));
	 * }
	 */
	public SelectionMenuItem(String text, boolean s, Consumer<Boolean> sh) {
		this(text, s);

		noNull(sh);
		selected.addListener((o, ov, nv) -> sh.accept(nv));
	}

	/**
	 * Creates menu item with specified text and false selection.
	 *
	 * @param text text of this menu item
	 */
	public SelectionMenuItem(String text) {
		this(text, false);
	}

	/**
	 * Creates menu item with empty text and false selection.
	 */
	public SelectionMenuItem() {
		this("");
	}

	/**
	 * Overrides default click implementation which changes the selection value.
	 * After using this method, icon will still reflect the selection, but it it
	 * will not change unless changed manually from the handler.
	 * <p/>
	 * This is useful for cases, where the menu lists items to choose from and
	 * exactly one must be selected at any time. This requires deselection to
	 * be impossible.
	 *
	 * @param h click handler
	 */
	public void setOnMouseClicked(Runnable h) {
		addEventHandler(MOUSE_CLICKED, e -> h.run());
		icon.onClickDo(consumer(i -> h.run()));
	}

	public static <I> List<MenuItem> buildSingleSelectionMenu(Collection<I> inputs, I selected, F1<I,String> toText, Consumer<I> action) {
		List<MenuItem> ms = map(inputs,
				input -> {
					SelectionMenuItem m = new SelectionMenuItem(toText.apply(input), input==selected);
					m.icon.styleclass(STYLECLASS_ICON_SINGLE_SEL);
					m.setOnMouseClicked(() -> {
						Menu parent = m.getParentMenu();
						if (parent!=null) {
							parent.getItems().forEach(i -> ((SelectionMenuItem) i).selected.setValue(false));
							m.selected.setValue(true);
							action.accept(input);
						}
					});
					return m;
				}
		);
		ms.sort(by(MenuItem::getText));
		return ms;
	}
}