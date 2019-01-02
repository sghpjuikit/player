package logger;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javafx.application.Platform;
import javafx.scene.control.TextArea;

public interface LoggerAuxiliary {

	/**
	 * Implementation based on <a href="http://stackoverflow.com/questions/3340342/java-writing-to-the-real-stdout-after-system-setout">
	 * Stackoverflow java-writing-to-the-real-stdout-after-system-setout</a><br/>.
	 * Maybe it will be useful.
	 */
	static class TextOutputStream<T> extends OutputStream {

		/** array for write(int val) */
		private byte[] oneByte;
		/** most recent action */
		private Appender<T> appender;

		private Lock jcosLock = new ReentrantLock();

		public TextOutputStream(T txtara, AppenderHandler<T> handler) {
			this(txtara, 1000, handler);
		}

		public TextOutputStream(T txtara, int maxlin, AppenderHandler<T> handler) {
			if (maxlin<1) {
				throw new IllegalArgumentException("JComponentOutputStream maximum lines must be positive (value=" + maxlin + ")");
			} oneByte = new byte[1]; appender = new Appender<>(txtara, maxlin, handler);
		}

		/** Clear the current console text area. */
		public void clear() {
			jcosLock.lock(); try {
				if (appender!=null) {
					appender.clear();
				}
			} finally {
				jcosLock.unlock();
			}
		}

		public void close() {
			jcosLock.lock(); try {
				appender = null;
			} finally {
				jcosLock.unlock();
			}
		}

		public void flush() {
			// sstosLock.lock();
			// try {
			// // TODO: Add necessary code here...
			// } finally {
			// sstosLock.unlock();
			// }
		}

		public void write(int val) {
			jcosLock.lock(); try {
				oneByte[0] = (byte) val; write(oneByte, 0, 1);
			} finally {
				jcosLock.unlock();
			}
		}

		public void write(byte[] ba) {
			jcosLock.lock(); try {
				write(ba, 0, ba.length);
			} finally {
				jcosLock.unlock();
			}
		}

		public void write(byte[] ba, int str, int len) {
			jcosLock.lock(); try {
				if (appender!=null) {
					appender.append(bytesToString(ba, str, len));
				}
			} finally {
				jcosLock.unlock();
			}
		}

		static private String bytesToString(byte[] ba, int str, int len) {
			try {
				return new String(ba, str, len, "UTF-8");
			} catch (UnsupportedEncodingException thr) {
				return new String(ba, str, len);
			} // all JVMs are required to support UTF-8
		}

		static class Appender<T> implements Runnable {
			private final T swingComponent;
			/** maximum lines allowed in text area */
			private final int maxLines;
			/** length of lines within text area */
			private final Deque<Integer> lengths = new ArrayDeque<>();
			/** values waiting to be appended */
			private final List<String> values = new ArrayList<>();

			private int curLength; // length of current line
			private boolean clear;
			private boolean queue;

			private Lock appenderLock;

			private AppenderHandler<T> handler;

			Appender(T cpt, int maxlin, AppenderHandler<T> hndlr) {
				appenderLock = new ReentrantLock();

				swingComponent = cpt; maxLines = maxlin;

				curLength = 0; clear = false; queue = true;

				handler = hndlr;
			}

			void append(String val) {
				appenderLock.lock(); try {
					values.add(val); if (queue) {
						queue = false; Platform.runLater(this);
					}
				} finally {
					appenderLock.unlock();
				}
			}

			void clear() {
				appenderLock.lock(); try {

					clear = true; curLength = 0; lengths.clear(); values.clear(); if (queue) {
						queue = false; Platform.runLater(this);
					}
				} finally {
					appenderLock.unlock();
				}
			}

			// MUST BE THE ONLY METHOD THAT TOUCHES the JComponent!
			public void run() {
				appenderLock.lock(); try {
					if (clear) {
						handler.setText(swingComponent, "");
					} for (String val : values) {
						curLength += val.length(); if (val.endsWith(EOL1) || val.endsWith(EOL2)) {
							if (lengths.size() >= maxLines) {
								handler.replaceRange(swingComponent, "", 0, lengths.removeFirst());
							} lengths.addLast(curLength); curLength = 0;
						} handler.append(swingComponent, val);
					}

					values.clear(); clear = false; queue = true;
				} finally {
					appenderLock.unlock();
				}
			}

			static private final String EOL1 = "\n";
			static private final String EOL2 = System.getProperty("line.separator", EOL1);
		}

		public interface AppenderHandler<T> {
			void setText(T swingComponent, String text);

			void replaceRange(T swingComponent, String text, int start, int end);

			void append(T swingComponent, String text);
		}
	}

	static class TextAreaStream extends TextOutputStream<TextArea> {

		public TextAreaStream(TextArea txtara) {
			super(txtara, new AppenderHandler<TextArea>() {
				private StringBuilder sb = new StringBuilder();

				@Override
				public void setText(TextArea area, String text) {
					sb.delete(0, sb.length()); append(area, text);
				}

				@Override
				public void replaceRange(TextArea area, String text, int start, int end) {
					sb.replace(start, end, text); redrawTextOf(area);
				}

				@Override
				public void append(TextArea area, String text) {
					sb.append(text); redrawTextOf(area);
				}

				private void redrawTextOf(TextArea area) {
					area.setText(sb.toString());
				}
			});
		}
	}

}