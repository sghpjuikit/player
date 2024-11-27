package sp.it.util;

import javafx.scene.image.Image;
import javafx.scene.text.TextBoundsType;
import sp.it.util.dev.Experimental;
import sp.it.util.type.Util;

/** Umbrella for unsupported or inaccessible modules. */
public class JavaLegacy {

//	private static final Text fontMeasuringText = new Text();
//	private static final Label fontMeasuringLabel = new Label();
//	private static final Scene fontMeasuringScene = new Scene(new Group(fontMeasuringText, fontMeasuringLabel));

	public static double computeTextWidth(javafx.scene.text.Font font, String text) {
//		fontMeasuringText.setFont(font);
//		fontMeasuringText.setText(text);
//		fontMeasuringText.applyCss();
//		return fontMeasuringText.getLayoutBounds().getWidth();

		// requires --add-exports javafx.controls/com.sun.javafx.scene.control.skin=ALL-UNNAMED
		 return com.sun.javafx.scene.control.skin.Utils.computeTextWidth(font, text, -1.0);
	}

	public static double computeTextHeight(javafx.scene.text.Font font, String text) {
//		fontMeasuringText.setFont(font);
//		fontMeasuringText.setText(text);
//		fontMeasuringText.applyCss();
//		return fontMeasuringText.getLayoutBounds().getHeight();

		// requires --add-exports javafx.controls/com.sun.javafx.scene.control.skin=ALL-UNNAMED
		 return com.sun.javafx.scene.control.skin.Utils.computeTextHeight(font, text, -1.0, TextBoundsType.LOGICAL);
	}

	public static double computeTextWidth(javafx.scene.text.Font font, double wrapWidth, String text) {
//		fontMeasuringLabel.setFont(font);
//		fontMeasuringLabel.setText(text);
//		fontMeasuringLabel.setWrapText(true);
//		fontMeasuringLabel.setPrefWidth(wrapWidth);
//		fontMeasuringLabel.applyCss();
//		return new Point2D(fontMeasuringText.getLayoutBounds().getWidth(), fontMeasuringText.getLayoutBounds().getHeight());

		return com.sun.javafx.scene.control.skin.Utils.computeTextWidth(font, text, wrapWidth);
	}

	public static double computeTextHeight(javafx.scene.text.Font font, double wrapWidth, String text) {
//		fontMeasuringLabel.setFont(font);
//		fontMeasuringLabel.setText(text);
//		fontMeasuringLabel.setWrapText(true);
//		fontMeasuringLabel.setPrefWidth(wrapWidth);
//		fontMeasuringLabel.applyCss();
//		return new Point2D(fontMeasuringText.getLayoutBounds().getWidth(), fontMeasuringText.getLayoutBounds().getHeight());

		return com.sun.javafx.scene.control.skin.Utils.computeTextHeight(font, text, wrapWidth, null);
	}

	// requires --add-opens javafx.graphics/com.sun.prism
	/**
	 * Dispose of the specified image with the intention of never being used again.
	 * Use {@code ImageView.setImage(null)} and only use this method if there is a memory leak.
	 *<p/>
	 * Renders the image object unusable, make sure it is no longer used by the application.
	 */
	@Experimental(reason = "Questionable API with usage conditioned by memory leaks and using reflection")
	public static void destroyImage(Image image) {
		if (image==null) return;
		var i = sp.it.util.type.Util.invokeMethodP0(image, "getPlatformImage");
		if (i!=null) sp.it.util.type.Util.setField(i, "pixelBuffer", null);
		if (i!=null) Util.setField(i, "pixelaccessor", null);
	}

}