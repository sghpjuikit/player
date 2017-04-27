package util.async.future;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javafx.application.Platform;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.SimpleLongProperty;
import util.async.future.ConvertListTask.Result;
import static util.dev.Util.noØ;
import static util.dev.Util.throwIfNotFxThread;

public abstract class ConvertListTask<T, R> extends FTask<Collection<? extends T>,Result<T,R>> {

	private final StringBuffer sb = new StringBuffer(40);
	private final AtomicLong skippedUpdate = new AtomicLong(-1);
	private final LongProperty skipped = new SimpleLongProperty(this, "skipped", 0);

	public ConvertListTask(String title) {
		updateTitle(noØ(title));
		updateMessage("Progress: -");
		updateProgress(0, 1);
	}

	public final ReadOnlyLongProperty skippedProperty() {
		throwIfNotFxThread();
		return skipped;
	}

	protected void updateSkipped(long skippedCount) {
		if (Platform.isFxApplicationThread()) {
			skipped.set(skippedCount);
		} else if (skippedUpdate.getAndSet(skippedCount)==-1) {
			Platform.runLater(() -> {
				long var1 = skippedUpdate.getAndSet(-1);
				skipped.set(var1);
			});
		}
	}

	protected void updateMessage(int all, int done) {
		sb.setLength(0);
		sb.append("Completed: ");
		sb.append(done);
		sb.append(" / ");
		sb.append(all);
		updateMessage(sb.toString());
	}

	public static class Result<T, R> {
		public final List<T> all;
		public final List<T> processed;
		public final List<R> converted;
		public final List<T> skipped;

		public Result(List<T> all, List<T> processed, List<R> converted, List<T> skipped) {
			this.all = noØ(all);
			this.processed = noØ(processed);
			this.converted = noØ(converted);
			this.skipped = noØ(skipped);
		}
	}

}