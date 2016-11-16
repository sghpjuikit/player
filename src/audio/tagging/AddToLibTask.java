package audio.tagging;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.concurrent.Task;

import util.functional.Operable;

import static util.dev.Util.throwIfNotFxThread;

/**
 *
 * @author Martin Polakovic
 */
public abstract class AddToLibTask<T,O> extends Task<T> implements Operable<O> {

	private BiConsumer<Boolean,T> onEnd;
	private Consumer<O> onClose;
	protected final StringBuffer sb = new StringBuffer(40);

	private AtomicLong skippedUpdate = new AtomicLong(-1);
	private LongProperty skipped = new SimpleLongProperty(this, "skipped", 0);

	public AddToLibTask() {
		super();
	}

	public AddToLibTask(String title) {
		this();
		updateTitle(title);
	}

	public AddToLibTask(BiConsumer<Boolean,T> onEnd) {
		this();
		setOnDone(onEnd);
	}

	public AddToLibTask(String title, BiConsumer<Boolean,T> onEnd) {
		this(title);
		setOnDone(onEnd);
	}

	public final ReadOnlyLongProperty skippedProperty() {
		throwIfNotFxThread();
		return skipped;
	}

	protected void updateSkipped(long skippedCount) {
		if(Platform.isFxApplicationThread()) {
			skipped.set(skippedCount);
		} else if(skippedUpdate.getAndSet(skippedCount) == -1) {
			Platform.runLater(() -> {
				long var1 = skippedUpdate.getAndSet(-1);
				skipped.set(var1);
			});
		}
	}

	public final O setOnDone(BiConsumer<Boolean,T> onEnd) {
		this.onEnd = onEnd;
		return (O)this;
	}

	public final O setOnClose(Consumer<O> onClose) {
		this.onClose = onClose;
		return (O)this;
	}

	@Override
	protected void succeeded() {
		super.succeeded();
		updateMessage(getTitle() + " succeeded");
		if (onEnd!=null) onEnd.accept(true, getValue());
		if (onClose!=null) onClose.accept((O)this);
	}

	@Override
	protected void cancelled() {
		super.cancelled();
		updateMessage(getTitle() + " cancelled");
		if (onEnd!=null) onEnd.accept(false, getValue());
		if (onClose!=null) onClose.accept((O)this);
	}

	@Override
	protected void failed() {
		super.failed();
		updateMessage(getTitle() + " failed");
		if (onEnd!=null) onEnd.accept(false, getValue());
		if (onClose!=null) onClose.accept((O)this);
	}

	protected void updateMessage(int all, int done, int skipped) {
		sb.setLength(0);
		sb.append("Completed: ");
		sb.append(done);
		sb.append(" / ");
		sb.append(all);
		sb.append(" ");
		sb.append(skipped);
		sb.append(" skipped.");
		updateMessage(sb.toString());
	}

	protected void updateMessage(int all, int done) {
		sb.setLength(0);
		sb.append("Completed: ");
		sb.append(done);
		sb.append(" / ");
		sb.append(all);
		sb.append(".");
		updateMessage(sb.toString());
	}

}