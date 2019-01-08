package sp.it.pl.gui.objects.contextmenu;

import java.util.List;
import java.util.function.Consumer;
import javafx.beans.property.Property;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import sp.it.pl.gui.objects.icon.CheckIcon;
import sp.it.pl.util.functional.Functors.Ƒ1;
import static sp.it.pl.util.dev.Fail.noNull;
import static sp.it.pl.util.functional.Util.by;
import static sp.it.pl.util.functional.Util.map;

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
		selected.setValue(s);
		// action = toggle selection
		setOnMouseClicked(() -> selected.setValue(!selected.getValue()));

		// hide open submenu arrow if no children
		getStyleClass().add(NO_CHILDREN_STYLECLASS);
		getItems().addListener((ListChangeListener<MenuItem>) c -> {
			ObservableList<String> sc = getStyleClass();
			if (c.getList().isEmpty()) {
				if (!sc.contains(NO_CHILDREN_STYLECLASS)) sc.add(NO_CHILDREN_STYLECLASS);
			} else
				sc.remove(NO_CHILDREN_STYLECLASS);
		});
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
		setOnAction(e -> h.run());
		icon.setOnMouseClicked(e -> h.run());
	}

	public static <I> Menu buildSingleSelectionMenu(String text, List<I> inputs, I selected, Ƒ1<I,String> toText, Consumer<I> action) {
		Menu menu = new Menu(text);
		menu.getItems().addAll(buildSingleSelectionMenu(inputs, selected, toText, action));
		return menu;
	}

	public static <I> List<MenuItem> buildSingleSelectionMenu(List<I> inputs, I selected, Ƒ1<I,String> toText, Consumer<I> action) {
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