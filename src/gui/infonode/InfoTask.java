package gui.infonode;

import javafx.concurrent.Task;
import javafx.scene.control.Labeled;
import javafx.scene.control.ProgressIndicator;
import org.reactfx.Subscription;
import static javafx.concurrent.Worker.State.READY;
import static javafx.concurrent.Worker.State.SCHEDULED;
import static util.reactive.Util.maintain;
import static util.reactive.Util.unsubscribe;

/**
 * Provides information about the task and its progress.
 */
public class InfoTask<T extends Task> implements InfoNode<T> {

	public final Labeled title;
	public final Labeled message;
	public final ProgressIndicator progressIndicator;
	private Subscription titleS, messageS, progressS;

	/**
	 * @param title title label. Use null if none.
	 * @param message message label. Use null if none.
	 * @param pi progress indicator. Use null if none.
	 */
	public InfoTask(Labeled title, Labeled message, ProgressIndicator pi) {
		this.title = title;
		this.message = message;
		this.progressIndicator = pi;
	}

	@Override
	public void setVisible(boolean v) {
		if (title!=null) title.setVisible(v);
		if (message!=null) message.setVisible(v);
		if (progressIndicator!=null) progressIndicator.setVisible(v);
	}

	@Override
	public void bind(T t) {
		unbind();
		if (progressIndicator!=null)
			maintain(t.progressProperty(), p -> t.getState()==SCHEDULED || t.getState()==READY ? 0 : p, progressIndicator.progressProperty());
		if (title!=null) maintain(t.titleProperty(), title.textProperty());
		if (message!=null) maintain(t.messageProperty(), message.textProperty());
	}

	@Override
	public void unbind() {
		if (titleS!=null) unsubscribe(titleS);
		if (messageS!=null) unsubscribe(messageS);
		if (progressS!=null) unsubscribe(progressS);
	}

}