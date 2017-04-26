package gui.objects;

import javafx.scene.control.Slider;
import javafx.scene.control.skin.SliderSkin;
import javafx.scene.layout.StackPane;
import util.animation.Anim;
import static javafx.geometry.Orientation.VERTICAL;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static javafx.util.Duration.millis;
import static util.dev.Util.noØ;
import static util.type.Util.getFieldValue;

/**
 * SliderSkin skin that adds animations & improved usability - track expands on mouse hover.
 */
public class ImprovedSliderSkin extends SliderSkin {

	public ImprovedSliderSkin(Slider slider) {
		super(slider);

		// install hover animation
		StackPane track = getFieldValue(this, "track");
		noØ(track);
		Anim v = new Anim(millis(350), p -> track.setScaleX(1 + p*p));
		Anim h = new Anim(millis(350), p -> track.setScaleY(1 + p*p));
		slider.addEventHandler(MOUSE_ENTERED, e -> (slider.getOrientation()==VERTICAL ? v : h).playOpen());
		slider.addEventHandler(MOUSE_EXITED, e -> (slider.getOrientation()==VERTICAL ? v : h).playClose());
	}
}