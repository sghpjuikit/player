package audio.tagging;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javafx.concurrent.Task;
import util.functional.Operable;

@SuppressWarnings("UnusedReturnValue")
public abstract class SuccessTask<T, O> extends Task<T> implements Operable<O> {

	private BiConsumer<Boolean,T> onEnd;
	private Consumer<O> onClose;
	protected final StringBuffer sb = new StringBuffer(40);

	public SuccessTask() {
		super();
	}

	public SuccessTask(String title) {
		this();
		updateTitle(title);
	}

	public SuccessTask(BiConsumer<Boolean,T> onEnd) {
		this();
		setOnDone(onEnd);
	}

	public SuccessTask(String title, BiConsumer<Boolean,T> onEnd) {
		this(title);
		setOnDone(onEnd);
	}

	@SuppressWarnings("unchecked")
	public final O setOnDone(BiConsumer<Boolean,T> onEnd) {
		this.onEnd = onEnd;
		return (O) this;
	}

	@SuppressWarnings("unchecked")
	public final O setOnClose(Consumer<O> onClose) {
		this.onClose = onClose;
		return (O) this;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void succeeded() {
		super.succeeded();
		updateMessage(getTitle() + " succeeded");
		if (onEnd!=null) onEnd.accept(true, getValue());
		if (onClose!=null) onClose.accept((O) this);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void cancelled() {
		super.cancelled();
		updateMessage(getTitle() + " cancelled");
		if (onEnd!=null) onEnd.accept(false, getValue());
		if (onClose!=null) onClose.accept((O) this);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void failed() {
		super.failed();
		updateMessage(getTitle() + " failed");
		if (onEnd!=null) onEnd.accept(false, getValue());
		if (onClose!=null) onClose.accept((O) this);
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