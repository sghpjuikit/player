package sp.it.util;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.text.Text;
import sp.it.util.dev.Experimental;
import sp.it.util.type.Util;

/** Umbrella for unsupported or inaccessible modules. */
public class JavaLegacy {

	public static String COMBO_BOX_STYLE_CLASS = "combo-box-popup"; // com.sun.javafx.scene.control.Properties.COMBO_BOX_STYLE_CLASS

	private static final Text fontMeasuringText = new Text();
	private static final Scene fontMeasuringScene = new Scene(new Group(fontMeasuringText));

	public static double computeTextWidth(javafx.scene.text.Font font, String text) {
		fontMeasuringText.setFont(font);
		fontMeasuringText.setText(text);
		fontMeasuringText.applyCss();
		return fontMeasuringText.getLayoutBounds().getWidth();
		// requires --add-exports javafx.controls/com.sun.javafx.scene.control.skin=ALL-UNNAMED
		// return com.sun.javafx.scene.control.skin.Utils.computeTextWidth(font, text, -1.0);
	}

	public static double computeTextHeight(javafx.scene.text.Font font, String text) {
		fontMeasuringText.setFont(font);
		fontMeasuringText.setText(text);
		fontMeasuringText.applyCss();
		return fontMeasuringText.getLayoutBounds().getHeight();
		// requires --add-exports javafx.controls/com.sun.javafx.scene.control.skin=ALL-UNNAMED
		// return com.sun.javafx.scene.control.skin.Utils.computeTextHeight(font, text, -1.0, TextBoundsType.LOGICAL);
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
		if (image==null) return;
		var i = sp.it.util.type.Util.invokeMethodP0(image, "getPlatformImage");
		if (i!=null) sp.it.util.type.Util.setField(i, "pixelBuffer", null);
		if (i!=null) Util.setField(i, "pixelaccessor", null);
	}

	public static void suspendWindows(boolean hibernate, boolean forceCritical, boolean disableWakeEvent) {
		WindowsSuspend.SetSuspendState(hibernate, forceCritical, disableWakeEvent);
	}

	public static void shutdownWindows() {
		try {
			Runtime.getRuntime().exec("shutdown -s -t 0");
		} catch(Throwable e) {
			// TODO: use EnvironmentKt.runCommand() and handle errors
		}
	}

	public static void restartWindows() {
		try {
			Runtime.getRuntime().exec("shutdown -r -t 0");
		} catch(Throwable e) {
			// TODO: use EnvironmentKt.runCommand() and handle errors
		}
	}

	public static void logOffWindows() {
		try {
			Runtime.getRuntime().exec("shutdown -l -t 0");
		} catch(Throwable e) {
			// TODO: use EnvironmentKt.runCommand() and handle errors
		}
	}

	public static void lockWindows() {
		try {
			Runtime.getRuntime().exec("Rundll32.exe user32.dll,LockWorkStation");
		} catch(Throwable e) {
			// TODO: use EnvironmentKt.runCommand() and handle errors
		}
	}

	private static class WindowsSuspend {
		public static native boolean SetSuspendState(boolean hibernate, boolean forceCritical, boolean disableWakeEvent);

		static {
			if (Platform.isWindows())
				Native.register("powrprof");
		}
	}

}