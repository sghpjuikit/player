package sp.it.pl.main;

import javafx.scene.text.TextBoundsType;

/** Umbrella for unsupported or inaccessible modules. */
public class JavaLegacy {

	// requires --add-exports javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED
	public static final String COMBO_BOX_STYLE_CLASS = com.sun.javafx.scene.control.Properties.COMBO_BOX_STYLE_CLASS;

	// requires --add-exports javafx.controls/com.sun.javafx.scene.control.skin=ALL-UNNAMED
	public static double computeFontWidth(javafx.scene.text.Font font, String text) {
		return com.sun.javafx.scene.control.skin.Utils.computeTextWidth(font, text, -1.0);
	}

	// requires --add-exports javafx.controls/com.sun.javafx.scene.control.skin=ALL-UNNAMED
	public static double computeFontHeight(javafx.scene.text.Font font, String text) {
		return com.sun.javafx.scene.control.skin.Utils.computeTextHeight(font, text, -1.0, TextBoundsType.LOGICAL);
	}

}