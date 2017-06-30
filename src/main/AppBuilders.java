package main;

import gui.objects.icon.Icon;
import gui.objects.spinner.Spinner;
import java.util.function.Consumer;
import javafx.scene.Cursor;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import util.animation.Anim;
import util.animation.interpolator.ElasticInterpolator;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.RESIZE_BOTTOM_RIGHT;
import static javafx.util.Duration.millis;
import static javafx.util.Duration.seconds;
import static util.graphics.Util.setScaleXY;

public interface AppBuilders {

	static ProgressIndicator appProgressIndicator() {
		return appProgressIndicator(null, null);
	}

	static ProgressIndicator appProgressIndicator(Consumer<ProgressIndicator> onStart, Consumer<ProgressIndicator> onFinish) {
		Spinner p = new Spinner();
		Anim a = new Anim(at -> setScaleXY(p,at*at)).dur(500).intpl(new ElasticInterpolator());
			 a.applier.accept(0d);
		p.progressProperty().addListener((o,ov,nv) -> {
			if (nv.doubleValue()==-1) {
				if (onStart!=null) onStart.accept(p);
				a.then(null)
				 .play();
			}
			if (nv.doubleValue()==1) {
				a.then(() -> { if (onFinish!=null) onFinish.accept(p); })
				 .playClose();
			}
		});
		return p;
	}

	static Tooltip appTooltip() {
		return appTooltip("");
	}

	static Tooltip appTooltip(String text) {
		Tooltip t = new Tooltip(text);
		t.setHideOnEscape(true);
		t.setConsumeAutoHidingEvents(true);
		// TODO: make configurable
		t.setShowDelay(seconds(1));
		t.setShowDuration(seconds(10));
		t.setHideDelay(millis(200));
		return t;
	}

	static Icon resizeButton() {
		// TODO: use css
		Icon b = new Icon(RESIZE_BOTTOM_RIGHT).scale(1.5);
		b.setCursor(Cursor.SE_RESIZE);
		return b;
	}

}