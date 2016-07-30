package gui.objects.seeker;

import javafx.scene.control.Slider;
import javafx.scene.control.skin.SliderSkin;
import javafx.scene.layout.StackPane;

import util.animation.Anim;
import util.type.Util;

import static javafx.scene.input.MouseDragEvent.MOUSE_DRAG_RELEASED;
import static javafx.scene.input.MouseEvent.*;
import static javafx.util.Duration.millis;

/** Custom skin for {@link Slider} adding more animations. Otherwise identical to official impl. */
public class SeekerSkin extends SliderSkin {

	public SeekerSkin(Slider slider) {
		super(slider);
		installCustomFunctionality(this);
	}

	/** Called in constructor. */
	public static void installCustomFunctionality(SliderSkin skin) {
		StackPane thumb = Util.getFieldValue(skin, "thumb");
		// hover scaling animation
		Anim scaling = new Anim(millis(350), p -> thumb.setScaleX(1+4*p*p));
		thumb.addEventFilter(MOUSE_ENTERED, e -> scaling.playOpen());
		thumb.addEventFilter(MOUSE_EXITED, e -> scaling.playClose());
		skin.getSkinnable().addEventFilter(DRAG_DETECTED, e -> scaling.playOpen());
		skin.getSkinnable().addEventFilter(MOUSE_DRAG_RELEASED, e -> scaling.playClose());
	}
}