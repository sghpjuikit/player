package sp.it.pl.util;

import javafx.scene.image.Image;
import javafx.scene.text.TextBoundsType;
import javafx.stage.Screen;
import sp.it.pl.util.dev.Experimental;
import sp.it.pl.util.type.Util;

/** Umbrella for unsupported or inaccessible modules. */
public class JavaLegacy {

	// requires --add-exports javafx.graphics/com.sun.glass.ui=ALL-UNNAMED
	@Experimental(reason = "May not work properly")
	public static int screenOrdinal(Screen screen) {
		int ordinal = com.sun.glass.ui.Screen.getScreens().stream()
			.filter(it -> it.getWidth()==(int) screen.getBounds().getWidth() && it.getHeight()==(int)screen.getBounds().getHeight())
			.findFirst()
			.orElseThrow(() -> new AssertionError("Screen " + screen + " ordinal can not be determined. ui.Screen of such dimensions not found"))
			.getAdapterOrdinal();
		return ordinal+1;
	}

	public static String COMBO_BOX_STYLE_CLASS = "combo-box-popup"; // com.sun.javafx.scene.control.Properties.COMBO_BOX_STYLE_CLASS

	// requires --add-exports javafx.controls/com.sun.javafx.scene.control.skin=ALL-UNNAMED
	public static double computeFontWidth(javafx.scene.text.Font font, String text) {
		return com.sun.javafx.scene.control.skin.Utils.computeTextWidth(font, text, -1.0);
	}

	// requires --add-exports javafx.controls/com.sun.javafx.scene.control.skin=ALL-UNNAMED
	public static double computeFontHeight(javafx.scene.text.Font font, String text) {
		return com.sun.javafx.scene.control.skin.Utils.computeTextHeight(font, text, -1.0, TextBoundsType.LOGICAL);
	}

	// requires --add-opens javafx.graphics/com.sun.prism
	/**
	 * Dispose of the specified image with the intention of never being used again.
	 * Use {@code ImageView.setImage(null)} and only use this method if there is a memory leak.
	 *
	 * Renders the image object unusable, make sure it is no longer used by the application.
	 */
	@Experimental(reason = "Questionable API with usage conditioned by memory leaks and using reflection")
	public static void destroyImage(Image image) {
		var i = sp.it.pl.util.type.Util.invokeMethodP0(image, "getPlatformImage");
		if (i!=null) sp.it.pl.util.type.Util.setField(i, "pixelBuffer", null);
		if (i!=null) Util.setField(i, "pixelaccessor", null);
	}

}