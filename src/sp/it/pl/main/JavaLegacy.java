package sp.it.pl.main;

import javafx.scene.text.TextBoundsType;
import javafx.stage.Screen;
import sp.it.pl.util.dev.Experimental;

/** Umbrella for unsupported or inaccessible modules. */
public class JavaLegacy {

	@Experimental
	public static int screenOrdinal(Screen screen) {
		com.sun.glass.ui.Screen.getScreens().stream()
			.sorted((a,b) -> Integer.compare(a.getAdapterOrdinal(), b.getAdapterOrdinal()))
			.forEach(it -> System.out.println(it.getAdapterOrdinal() + " " + it.getWidth() + " x " + it.getHeight()));
		int ordinal = com.sun.glass.ui.Screen.getScreens().stream()
			.filter(it -> it.getWidth()==(int) screen.getBounds().getWidth() && it.getHeight()==(int)screen.getBounds().getHeight())
			.findFirst()
			.orElseThrow(() -> new AssertionError("Screen " + screen + " ordinal can not be determined. ui.Screen of such dimensions not found"))
			.getAdapterOrdinal();
		return ordinal+1;
	}

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