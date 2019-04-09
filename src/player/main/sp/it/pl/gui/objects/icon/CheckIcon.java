package sp.it.pl.gui.objects.icon;

import de.jensd.fx.glyphs.GlyphIcons;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import sp.it.util.reactive.Subscription;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static sp.it.util.reactive.UtilKt.maintain;
import static sp.it.util.ui.UtilKt.pseudoclass;

/**
 * Very simple alternative CheckBox control.
 */
public class CheckIcon extends Icon {
	private static final PseudoClass selectedPC = pseudoclass("selected");
	private static final String STYLECLASS = "check-icon";
	private static final String STYLECLASS_DISABLING = "check-icon-disabling";

	/** Selection state. */
	public final Property<Boolean> selected;
	private Subscription s = null;

	/** Creates icon with selection true. */
	public CheckIcon() {
		this(true);
	}

	/** Creates icon with selection set to provided value. */
	public CheckIcon(boolean s) {
		this(new SimpleBooleanProperty(s));
	}

	/** Creates icon with property as selection value. {@link #selected}==s will always be true. */
	public CheckIcon(Property<Boolean> s) {
		selected = s==null ? new SimpleBooleanProperty(true) : s;

		styleclass(STYLECLASS);
		maintain(selected, v -> pseudoClassStateChanged(selectedPC, v));

		setFocusTraversable(true);
		addEventHandler(MOUSE_CLICKED, e -> {
			selected.setValue(!selected.getValue());
			e.consume();
		});
		addEventHandler(KeyEvent.KEY_PRESSED, e -> {
			if (e.getCode()==KeyCode.ENTER || e.getCode()==KeyCode.SPACE) {
				selected.setValue(!selected.getValue());
				e.consume();
			}
		});
	}

	/** Sets normal and selected icons. Overrides icon css values. */
	public CheckIcon icons(GlyphIcons selectedIcon, GlyphIcons unselectedIcon) {
		getStyleClass().remove(STYLECLASS_DISABLING);
		if (s!=null) s.unsubscribe();
		s = maintain(selected, v -> icon(v ? selectedIcon : unselectedIcon));
		return this;
	}

	/**
	 * Sets normal and selected icons to the same icon. Overrides icon css values. Installs styleclass
	 * that imitates 'disabled' pseudoclass when not selected. This is used as a state indicator
	 * instead of different icon.
	 */
	public CheckIcon icons(GlyphIcons icon) {
		if (!getStyleClass().contains(STYLECLASS_DISABLING)) getStyleClass().add(STYLECLASS_DISABLING);
		if (s!=null) s.unsubscribe();
		icon(icon);
		return this;
	}
}