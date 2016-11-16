package gui.infonode;

import javafx.concurrent.Task;
import javafx.scene.control.Labeled;
import javafx.scene.control.ProgressIndicator;

/**
 *  Provides information about the task and its progress.
 */
public class InfoTask<T extends Task> implements InfoNode<T> {

	public final Labeled title;
	public final Labeled message;
	public final ProgressIndicator progressIndicator;

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

	/** {@inheritDoc} */
	@Override
	public void setVisible(boolean v) {
		if (title!=null) title.setVisible(v);
		if (message!=null) message.setVisible(v);
		if (progressIndicator!=null) progressIndicator.setVisible(v);
	}

	/** {@inheritDoc} */
	@Override
	public void bind(T t) {
		unbind();
		if (progressIndicator!=null) progressIndicator.progressProperty().bind(t.progressProperty());
		if (title!=null) title.textProperty().bind(t.titleProperty());
		if (message!=null) message.textProperty().bind(t.messageProperty());
	}

	/** {@inheritDoc} */
	@Override
	public void unbind() {
		if (progressIndicator!=null) progressIndicator.progressProperty().unbind();
		if (title!=null) title.textProperty().unbind();
		if (message!=null) message.textProperty().unbind();
	}

}