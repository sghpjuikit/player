package util.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import util.async.executor.EventReducer;
import util.collections.Tuple2;
import util.dev.TODO;
import static java.nio.file.StandardWatchEventKinds.*;
import static util.async.Async.runFX;
import static util.async.Async.runNew;
import static util.collections.Tuples.tuple;
import static util.dev.TODO.Purpose.DOCUMENTATION;
import static util.dev.Util.log;

/**
 * @author Martin Polakovic
 */
@TODO(purpose = DOCUMENTATION)
public class FileMonitor {
	/**
	 * Relatively simple to monitor a file? Think again.
	 * 1) Dir only!
	 * WatchService allows us to only monitor a directory. We then must simply ignore other
	 * events of files other than the one we monitor. This is really bad if we want to monitor
	 * multiple files in a single directory (each on its own). We would have to use 1 thread
	 * and 1 watch service per each file!
	 * Solved using a predicate parameter to filter out unwanted events.
	 * 2) Modification events.
	 * Not even going to try to understand - modifications events fire
	 * multiple times! When editing java source file in Netbeans and saving it throws 3 events
	 * at about 8-13 ms time gap (tested on SSD)!
	 * Solved using event reducer & checking modification times
	 * 3) Nested events
	 * CREATE and DELETE events are fired for files and directories up to level 2 (children of
	 * children of the monitored directory, e.g.,  mon_dir/lvl1/file.txt). MODIFIED events will
	 * be thrown for any direct child or
	 */

	private File monitoredFileDir;
	private WatchService watchService;
	private Predicate<File> filter;
	private BiConsumer<Kind<Path>,File> action;
	private boolean isFile;
	private String name; // purely for logging "Directory" or "File"

	EventReducer<Tuple2<Kind<Path>,File>> modificationReducer = EventReducer.toLast(50, e -> emitEvent(e._1, e._2));

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
		fm.isFile = true;
		fm.name = fm.isFile ? "File" : "Directory";

		Path dir = fm.monitoredFileDir.toPath();
		try {
			fm.watchService = FileSystems.getDefault().newWatchService();
			dir.register(fm.watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW);
			runNew(() -> {
				// The check requires I/O so lets do that on bgr thread as well
//                if (!monitoredDir.isDirectory()) throw new IllegalArgumentException("File not a directory or does not exist.");

				boolean valid;
				WatchKey watchKey;
				do {
					try {
						watchKey = fm.watchService.take();
					} catch (InterruptedException e) {
						log(FileMonitor.class).error("Interrupted monitoring of directory {}", dir, e);
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
										fm.modificationReducer.push(tuple((Kind) type, modifiedFile))
								);
							} else {
								fm.emitEvent((Kind) type, modifiedFile);
							}
						}
					}

					valid = watchKey.reset();
				} while (valid);
			});
		} catch (IOException e) {
			log(FileMonitor.class).error("Error when starting file monitoring {}", fm.monitoredFileDir, e);
		}

		return fm;
	}

	@SuppressWarnings({"unchecked", "ConstantConditions"})
	@Deprecated
	public static FileMonitor monitorDirectory(File toMonitor, BiConsumer<Kind<Path>,File> handler) {
		FileMonitor fm = new FileMonitor();
		fm.monitoredFileDir = toMonitor;
		fm.action = handler;
		fm.isFile = false;
		fm.name = fm.isFile ? "File" : "Directory";

		Path dir = fm.monitoredFileDir.toPath();
		try {
			fm.watchService = FileSystems.getDefault().newWatchService();
			dir.register(fm.watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW);
			runNew(() -> {
				// The check requires I/O so lets do that on bgr thread as well
//                if (!toMonitor.isDirectory()) throw new IllegalArgumentException("File not a directory or does not exist.");

				boolean valid;
				WatchKey watchKey;
				do {
					try {
						watchKey = fm.watchService.take();
					} catch (InterruptedException e) {
						log(FileMonitor.class).error("Interrupted monitoring of directory {}", dir, e);
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
			});
		} catch (IOException e) {
			log(FileMonitor.class).error("Error when starting directory monitoring {}", fm.monitoredFileDir, e);
		}

		return fm;
	}

	public void stop() {
		try {
			if (watchService!=null) watchService.close();
		} catch (IOException e) {
			log(FileMonitor.class).error("Error when closing file monitoring {}", monitoredFileDir, e);
		}
	}
}
