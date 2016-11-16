package gui.infonode;

import javafx.scene.control.Labeled;
import javafx.scene.control.ProgressIndicator;

import org.reactfx.Subscription;

import static util.Util.enumToHuman;
import static util.reactive.Util.maintain;
import static util.reactive.Util.unsubscribe;

public class ConvertListTask extends InfoTask<audio.tagging.ConvertListTask<?,?>> {
	public final Labeled skipped, state;
	private Subscription skippedS, stateS;

	public ConvertListTask(Labeled title, Labeled message, Labeled skipped, Labeled state, ProgressIndicator pi) {
		super(title, message, pi);
		this.skipped = skipped;
		this.state = state;
	}

	@Override
	public void setVisible(boolean v) {
		super.setVisible(v);
		if (skipped!=null) skipped.setVisible(v);
	}

	@Override
	public void bind(audio.tagging.ConvertListTask<?,?> t) {
		super.bind(t);
		if (skipped!=null) skippedS = maintain(t.skippedProperty(), v -> "Skipped: " + v, skipped.textProperty());
		if (state!=null) stateS = maintain(t.stateProperty(), v -> "State: " + enumToHuman(v), state.textProperty());
	}

	@Override
	public void unbind() {
		super.unbind();
		skippedS = unsubscribe(skippedS);
		stateS = unsubscribe(stateS);
	}

}