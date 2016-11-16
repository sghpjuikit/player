package gui.infonode;

import javafx.scene.control.Labeled;
import javafx.scene.control.ProgressIndicator;
import org.reactfx.Subscription;
import audio.tagging.AddToLibTask;

import static util.reactive.Util.maintain;
import static util.reactive.Util.unsubscribe;

public class InfoAddToLibTask extends InfoTask<AddToLibTask<?,?>> {
	public final Labeled skipped;
	private Subscription skippedS;

	public InfoAddToLibTask(Labeled title, Labeled message, Labeled skipped, ProgressIndicator pi) {
		super(title, message, pi);
		this.skipped = skipped;
	}

	@Override
	public void setVisible(boolean v) {
		super.setVisible(v);
		if (skipped!=null) skipped.setVisible(v);
	}

	@Override
	public void bind(AddToLibTask<?,?> t) {
		super.bind(t);
		if (skipped!=null) skippedS = maintain(t.skippedProperty(), v -> "Skipped: " + v, skipped.textProperty());
	}

	@Override
	public void unbind() {
		super.unbind();
		skippedS = unsubscribe(skippedS);
	}
}