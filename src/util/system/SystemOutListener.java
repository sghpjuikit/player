package util.system;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.reactfx.Subscription;

import static util.async.Async.runFX;

/**
 * Stream that self-inserts as {@link System#out}, but instead of redirecting it, it continues
 * to provide to it, functioning effectively as a listener. Designed as a distributor to
 * end-listeners that actually process the data. These can be added or removed anytime and in
 * any count easily and without interfering with each other or the original stream. In addition,
 * execute on fx thread.
 * <p/>
 * It is not recommended to create multiple instances, instead observe this stream with multiple listeners..
 */
public class SystemOutListener extends PrintStream {
	private final SystemOutDuplicateStream stream;

	public SystemOutListener() {
		this(new SystemOutDuplicateStream());
		System.setOut(this);
	}

	private SystemOutListener(SystemOutDuplicateStream cloned) {
		super(cloned);
		this.stream = cloned;
	}

	/**
	 * Add listener that will receive the stream data (always on fx thread).
	 * @return action that removes the listener
	 */
	public Subscription addListener(Consumer<String> listener) {
		stream.listeners.add(listener);
		return () -> stream.listeners.remove(listener);
	}

	public void removeListener(Consumer<String> listener) {
		stream.listeners.remove(listener);
	}

	/** Helper class for {@link SystemOutListener}. */
	private static class SystemOutDuplicateStream extends OutputStream {
		private final PrintStream sout = System.out;
		private final List<Consumer<String>> listeners = new ArrayList<>();

		@Override
		public void write(int b) throws IOException {
			// Less efficient
			// sout.write(b);
			// if (!listeners.isEmpty())
			//     runFX(() -> listeners.forEach(l -> l.accept(b)));
			throw new AssertionError("Operation not allowed for performance reasons.");
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			// copied from super.write(...) implementation
			if (b == null) {
				throw new NullPointerException();
			} else if ((off<0) || (off>b.length) || (len<0) || (off+len >b.length) || (off+len <0)) {
				throw new IndexOutOfBoundsException();
			} else if (len == 0) {
				return;
			}

			// for (int i=0 ; i<len ; i++) write(b[off + i]);
			sout.write(b, off, len);
			if (!listeners.isEmpty()) {
				String s = new String(b, off, len, StandardCharsets.UTF_8);
				runFX(() -> listeners.forEach(l -> l.accept(s)));
			}
		}
	}
}