package sp.it.pl.util.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.ThreadFactory;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import sp.it.pl.util.async.executor.EventReducer;
import sp.it.pl.util.system.Os;
import static com.sun.nio.file.ExtendedWatchEventModifier.FILE_TREE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static sp.it.pl.util.async.AsyncKt.runFX;
import static sp.it.pl.util.async.AsyncKt.threadFactory;
import static sp.it.pl.util.dev.Util.logger;

// TODO: use interface, leverage recursive directory watch on Windows
// TODO: provide documentation
public class FileMonitor {
	/**
	 * Relatively simple to monitor a file? Think again.
	 * 1) Dir only!
	 * WatchService allows us to only monitor a directory. We then must simply ignore other
	 * events of files other than the one we monitor. This is really bad if we want to monitor
	 * multiple files in a single directory independently.
	 * Solvable using a predicate parameter to filter out unwanted events.
	 * 2) Modification events.
	 * Events can fire multiple times when application use safe-rewrite saving or in various other scenarios! E.g.
	 * editing and saving .java file in Netbeans fires 3 events at about 8-13 ms time gap (tested on SSD).
	 * Solvable using event reducer & checking modification times
	 * 3) Nested events
	 * CREATE and DELETE events are fired for files and directories up to level 2 (children of
	 * children of the monitored directory, e.g.,  mon_dir/lvl1/file.txt). MODIFIED events will
	 * be thrown for any direct child.
	 * Furthermore, recursive monitoring is only supported on Windows, this is a platform limitation.
	 */

	private static final ThreadFactory threadCreator = threadFactory("WatchServiceConsumer", true);
	private File monitoredFileDir;
	private WatchService watchService;
	private Predicate<File> filter;
	private BiConsumer<Kind<Path>,File> action;

	private final EventReducer<Event> modificationReducer = EventReducer.toLast(50, e -> emitEvent(e.kind, e.file));

	private void emitEvent(Kind<Path> type, File file) {
		// This works as it should.
		// If anyone needs logging, they are free to do so in the event handler.
		// log(FileMonitor.class).info("{} event {} on {}", name,type,file);

		// always run on fx thread
		runFX(() -> action.accept(type, file));
	}

	/**
	 * Creates and starts directory monitoring reporting events for single file contained within the directory.
	 * <p/>
	 * Shortcut for:
	 * <p/>
	 * {@code monitorDirsFiles(monitoredFile.getParentFile(), file -> file.equals(monitoredFile), (type,file) ->
	 * handler.accept(type)); }
	 * <p/>
	 *
	 * @param handler handles the event taking the event type as parameter
	 * @return directory monitor
	 */
	public static FileMonitor monitorFile(File monitoredFile, Consumer<Kind<Path>> handler) {
		return monitorDirsFiles(monitoredFile.getParentFile(), file -> file.equals(monitoredFile), (type, file) -> handler.accept(type));
	}

	/**
	 * Creates and starts directory monitoring for specified directory reporting events for any 1st
	 * level child file which passes the predicate filter.
	 *
	 * @param monitoredDir directory to be monitored
	 * @param filter filter narrowing down events, for example any text file.
	 * @param handler handles the event containing type and modified file parameters
	 * @return directory monitor
	 */
	@SuppressWarnings({"unchecked", "ConstantConditions"})
	public static FileMonitor monitorDirsFiles(File monitoredDir, Predicate<File> filter, BiConsumer<Kind<Path>,File> handler) {

		FileMonitor fm = new FileMonitor();
		fm.monitoredFileDir = monitoredDir;
		fm.filter = filter;
		fm.action = handler;

		Path dir = fm.monitoredFileDir.toPath();
		try {
			fm.watchService = FileSystems.getDefault().newWatchService();
			dir.register(fm.watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW);
			threadCreator.newThread(() -> {
				// The check requires I/O so lets do that on bgr thread as well
//                if (!monitoredDir.isDirectory()) throw new IllegalArgumentException("File not a directory or does not exist.");

				boolean valid;
				WatchKey watchKey;
				do {
					try {
						watchKey = fm.watchService.take();
					} catch (InterruptedException e) {
						logger(FileMonitor.class).error("Interrupted monitoring of directory {}", dir, e);
						return;
					} catch (ClosedWatchServiceException e) {
						// watching ended
						return;
					}

					for (WatchEvent<?> event : watchKey.pollEvents()) {
						Kind<?> type = event.kind();

						if (type==OVERFLOW) continue;

						WatchEvent<Path> ev = (WatchEvent<Path>) event;
						String modifiedFileName = ev.context().toString();
						File modifiedFile = new File(fm.monitoredFileDir, modifiedFileName);

//                        if (modifiedFile.getParentFile().equals(fm.monitoredFileDir)) // to do
						if (fm.filter.test(modifiedFile)) {
							if (type==ENTRY_MODIFY) {
								runFX(() ->
										fm.modificationReducer.push(new Event((Kind) type, modifiedFile))   // TODO: fix overwriting events
								);
							} else {
								fm.emitEvent((Kind) type, modifiedFile);
							}
						}
					}

					valid = watchKey.reset();
				} while (valid);
			}).start();
		} catch (IOException e) {
			logger(FileMonitor.class).error("Error when starting file monitoring {}", fm.monitoredFileDir, e);
		}

		return fm;
	}

	@SuppressWarnings({"unchecked", "ConstantConditions", "UnnecessaryLocalVariable"})
	public static FileMonitor monitorDirectory(File toMonitor, boolean recursive, BiConsumer<Kind<Path>,File> handler) {
		FileMonitor fm = new FileMonitor();
		fm.monitoredFileDir = toMonitor;
		fm.action = handler;

		Path dir = fm.monitoredFileDir.toPath();
		try {
			fm.watchService = FileSystems.getDefault().newWatchService();

			boolean recursiveRequested = recursive;
			boolean recursiveSupported = Os.WINDOWS.isCurrent();
			boolean recursiveWarn = recursiveRequested && !recursiveSupported;
			boolean recursiveUsed = !recursiveRequested || !recursiveSupported;

			if (recursiveWarn)
				logger(FileMonitor.class).warn("Recursive file watcher is not supported, using standard, file={}", fm.monitoredFileDir);

			if (recursiveUsed) dir.register(fm.watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW);
			else dir.register(fm.watchService, new Kind<?>[] {ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW}, FILE_TREE);

			threadCreator.newThread(() -> {
				// The check requires I/O so lets do that on bgr thread as well
//                if (!toMonitor.isDirectory()) throw new IllegalArgumentException("File not a directory or does not exist.");

				boolean valid;
				WatchKey watchKey;
				do {
					try {
						watchKey = fm.watchService.take();
					} catch (InterruptedException e) {
						logger(FileMonitor.class).error("Interrupted monitoring of directory {}", dir, e);
						return;
					} catch (ClosedWatchServiceException e) {
						// watching ended
						return;
					}

					for (WatchEvent<?> event : watchKey.pollEvents()) {
						Kind<?> type = event.kind();
						if (type!=OVERFLOW) {
							WatchEvent<Path> ev = (WatchEvent<Path>) event;
							String modifiedFileName = ev.context().toString();
							File modifiedFile = new File(fm.monitoredFileDir, modifiedFileName);

							fm.emitEvent((Kind) type, modifiedFile);
						}
					}

					valid = watchKey.reset();
				} while (valid);
			}).start();
		} catch (IOException e) {
			logger(FileMonitor.class).error("Error when starting directory monitoring {}", fm.monitoredFileDir, e);
		}

		return fm;
	}

	public void stop() {
		try {
			if (watchService!=null) watchService.close();
		} catch (IOException e) {
			logger(FileMonitor.class).error("Error when closing file monitoring {}", monitoredFileDir, e);
		}
	}

	private static class Event {
		public final Kind<Path> kind;
		public final File file;

		private Event(Kind<Path> kind, File file) {
			this.kind = kind;
			this.file = file;
		}
	}
}