package util.file;

import audio.playlist.PlaylistManager;
import gui.Gui;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import layout.widget.feature.ImageDisplayFeature;
import layout.widget.feature.ImagesDisplayFeature;
import util.async.Async;
import util.dev.TODO;
import util.file.AudioFileFormat.Use;
import util.functional.Try;
import util.system.Os;
import static java.awt.Desktop.Action.*;
import static java.util.stream.Collectors.groupingBy;
import static layout.widget.WidgetManager.WidgetSource.NO_LAYOUT;
import static main.App.APP;
import static util.dev.TODO.Purpose.UNIMPLEMENTED;
import static util.dev.TODO.Purpose.UNTESTED;
import static util.dev.Util.log;
import static util.dev.Util.noØ;
import static util.file.FileType.DIRECTORY;
import static util.file.Util.getSuffix;
import static util.file.Util.traverseExistingDir;
import static util.functional.Try.error;
import static util.functional.Try.ok;
import static util.functional.Util.*;

/**
 * Provides methods to handle external platform specific tasks. Browsing
 * files, opening files in external apps, clipboard, etc.
 *
 * @author Martin Polakovic
 */
public interface Environment {

	/** Copies the string to system clipboard. Does nothing if null. */
	static void copyToSysClipboard(String s) {
		copyToSysClipboard(DataFormat.PLAIN_TEXT, s);
	}

	/** Puts given object to system clipboard. Does nothing if object null. */
	static void copyToSysClipboard(DataFormat df, Object o) {
		if (o!=null) {
			ClipboardContent c = new ClipboardContent();
			c.put(df, o);
			Clipboard.getSystemClipboard().setContent(c);
		}
	}

	/**
	 * Launches the program represented by the provided file. Does not wait for the program or block.
	 * <p/>
	 * Working directory of the program will be set to the parent directory of its file.
	 * <p/>
	 * If the program fails to start, it will be attempted to be run (again) with elevated privileges (as an
	 * administrator) if possible.
	 *
	 * @param program executable file of the program
	 * @param arguments arguments to run the program with
	 * @return success if the program is executed or error if it is not, irrespective of if and how the program finishes
	 * @throws java.lang.RuntimeException if any param null
	 */
	static Try<Void,Exception> runProgram(File program, String... arguments) {
		noØ(program);
		noØ((Object[]) arguments);

		File dir = program.getParentFile();
		List<String> command = new ArrayList<>();

		try {
			// run this program
			command.add(program.getAbsoluteFile().getPath());

			// with optional parameter
			for (String a : arguments)
				if (!a.isEmpty()) command.add("-" + a);

			new ProcessBuilder(command)
					.directory(dir)
					.start();

			return Try.ok();
		} catch (IOException e) {
			log(Environment.class).warn("Failed to launch program", e);

			if (Os.getCurrent()==Os.WINDOWS) {
				// we might have failed due to the program requiring elevation (run
				// as admin) so we use a little utility we package along
				log(Environment.class).warn("Attempting to run as administrator...");
				try {
					// use elevate.exe to run what we wanted
					command.add(0, "elevate.exe");
					log(Environment.class).info("Executing command= {}", command);
					new ProcessBuilder(command)
							.directory(dir)
							.start();

					return Try.ok();
				} catch (IOException x) {
					log(Environment.class).error("Failed to launch program", x);
					Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, x);
					return Try.error(x);
				}
			} else {
				return Try.error(e);
			}
		}
	}

	/**
	 * Runs a command as a process in new background thread.
	 * <p/>
	 * This is a non-blocking method.
	 *
	 * @param command non null file to open
	 * @throws java.lang.RuntimeException if any param null
	 */
	static void runCommand(String command) {
		noØ(command);
		runCommand(noØ(command), null);
	}

	/**
	 * Runs a command as a process in new background thread and executes an action right after it launches.
	 * This allows process monitoring or waiting for it to.
	 * <p/>
	 * This is a non-blocking method.
	 *
	 * @param command non null command
	 * @throws java.lang.RuntimeException if command null
	 */
	static void runCommand(String command, Consumer<Process> then) {
		noØ(command);
		if (!command.isEmpty())
			Async.runNew(() -> {
				try {
					Process p = Runtime.getRuntime().exec(command);
					if (then!=null) then.accept(p);
				} catch (IOException e) {
					log(Environment.class).error("Could not run command '{}'", command, e);
				}
			});
	}

	/**
	 * Equivalent to {@code browse(f.toURI()); }.
	 *
	 * @param file non null file to browse
	 * @throws java.lang.RuntimeException if any param null
	 */
	static void browse(File file) {
		browse(file.toURI());
	}

	/**
	 * Equivalent to {@code browse(f.toURI(), openDir); }.
	 *
	 * @param file non null file to browse
	 * @throws java.lang.RuntimeException if any param null
	 */
	static void browse(File file, boolean openDir) {
		browse(file.toURI(), openDir);
	}

	/**
	 * Browses uri - opens it in its respective browser, e.g. predefined internet browser or OS' file browser.
	 * <p/>
	 * On some platforms the operation may be unsupported. In that case this method is a no-op.
	 *
	 * @param uri non null uri to browse
	 * @throws java.lang.RuntimeException if any param null
	 */
	@TODO(purpose = {UNIMPLEMENTED, UNTESTED}, note = "Non-windows platform impl. naively & untested")
	static void browse(URI uri) {
		browse(uri, false);
	}

	private static void browse(URI uri, boolean openDir) {
		if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(BROWSE)) {
			log(Environment.class).warn("Unsupported operation : " + BROWSE + " uri");
			return;
		}

		try {
			// If uri denotes a file, file explorer should be open, highlighting the file
			// However Desktop.browse does nothing (a bug?). We have 2 alternatives:
			// 1: open the parent directory of the file (and sacrifice the file highlighting functionality)
			// 2: open the file with Desktop.open() which opens the file in the associated program.
			// Both have problems.
			//
			// Ultimately, for Windows we run explorer.exe manually and select the file. For
			// other systems we browse the parent directory instead. Non Windows platforms
			// need some testing to do...
			try {
				File f = new File(uri);
				boolean isDir = f.isDirectory();
				if (f.exists()) {
					if (Os.WINDOWS.isCurrent() && (!isDir || !openDir)) {
						openWindowsExplorerAndSelect(f);
					} else {
						open(isDir ? f.getParentFile() : f);
					}
				}
				return;
			} catch (IllegalArgumentException e) {
				// ignore exception, it just means the uri does not denote a
				// file which is fine
			}
			Desktop.getDesktop().browse(uri);
		} catch (IOException e) {
			log(Environment.class).error("Browsing uri {} failed", uri, e);
			APP.parameterProcessor.process(list(uri.getPath())); // try open with this app
		}
	}

	/**
	 * Browses files or directories. On some platforms the operation may be unsupported.
	 */
	static void browse(Stream<File> files) {
		files.distinct()
				.collect(groupingBy(f -> f.isFile() ? f.getParentFile() : f))
				.forEach((dir, children) -> {
					if (children.size()==1) browse(children.get(0));
					else if (children.stream().anyMatch(f -> f==dir)) browse(dir);
					else open(dir);
				});
	}

	// TODO: if no associated editor exists exception is thrown! fix and use Try
	/**
	 * Edits file in default associated editor program. If the file denotes a directory, {@link #open(java.io.File)}
	 * is called instead.
	 * <p/>
	 * On some platforms the operation may be unsupported. In that case this method is a no-op.
	 */
	static void edit(File file) {
		noØ(file);

		if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(EDIT)) {
			log(Environment.class).warn("Unsupported operation : " + EDIT + " uri");
			return;
		}

		if (file.isDirectory()) {
			open(file);
		} else {
			try {
				Desktop.getDesktop().edit(file);
			} catch (IOException e) {
				log(Environment.class).error("Opening file {} in editor failed", file, e);
				APP.parameterProcessor.process(list(file.getPath())); // try open with this app
			} catch (IllegalArgumentException e) {
				// file does not exists, nothing for us to do
			}
		}
	}

	// TODO: if no associated program exists exception is thrown! fix and use Try
	/**
	 * Opens the file.<br/>
	 * If the file is executable, it will be executed, using {@link #runProgram(java.io.File, String...)} <br/>
	 * Otherwise it will be opened in the default associated program.
	 * <p/>
	 * On some platforms the operation may be unsupported. In that case this method is a no-op.
	 *
	 * @param file non null file to open
	 * @throws java.lang.RuntimeException if any param null
	 */
	static void open(File file) {
		noØ(file);
		if (isExecutable(file)) {
			// If the file is executable, Desktop#open() will execute it, however the spawned process' working directory
			// will be set to the working directory of this application, which is not illegal, but definitely dangerous
			// Hence, we executable files specifically
			runProgram(file);
		} else {
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(OPEN)) {
				try {
					Desktop.getDesktop().open(file);
				} catch (IOException e) {
					log(Environment.class).error("Opening file {} in native app failed", file, e);
					APP.parameterProcessor.process(list(file.getPath())); // try open with this app
				} catch (IllegalArgumentException e) {
					// file does not exists, nothing for us to do
				}
			} else {
				log(Environment.class).warn("Unsupported operation : " + OPEN + " file");
			}
		}
	}

	// TODO: implement properly
	/**
	 *
	 * @param f non null file
	 * @return true if the file is an executable file
	 * @throws java.lang.RuntimeException if any param null
	 */
	static boolean isExecutable(File f) {
		String extension = getSuffix(f);
		return isContainedIn(extension.toLowerCase(), "exe");
	}

	static boolean isOpenableInApp(File f) {
		return ((f.isDirectory() && APP.DIR_SKINS.equals(f.getParentFile())) || Util.isValidSkinFile(f)) ||
				((f.isDirectory() && APP.DIR_WIDGETS.equals(f.getParentFile())) || Util.isValidWidgetFile(f)) ||
				AudioFileFormat.isSupported(f, Use.PLAYBACK) || ImageFileFormat.isSupported(f);
	}

	static void openIn(File f, boolean inApp) {
		// open skin - always in app
		if ((f.isDirectory() && APP.DIR_SKINS.equals(f.getParentFile())) || Util.isValidSkinFile(f)) {
			Gui.setSkin(Util.getName(f));
		}

		// open widget
		else if ((f.isDirectory() && APP.DIR_WIDGETS.equals(f.getParentFile())) || Util.isValidWidgetFile(f)) {
			String n = Util.getName(f);
			APP.widgetManager.find(n, NO_LAYOUT, false);
		}

		// open audio file
		else if (inApp && AudioFileFormat.isSupported(f, Use.PLAYBACK)) {
			PlaylistManager.use(p -> p.addUri(f.toURI()));
		}

		// open image file
		else if (inApp && ImageFileFormat.isSupported(f)) {
			APP.widgetManager.use(ImageDisplayFeature.class, NO_LAYOUT, w -> w.showImage(f));
		}

		// delegate to native app cant handle
		else open(f);
	}

	static void openIn(List<File> files, boolean inApp) {
		noØ(files);
		if (files.isEmpty()) return;
		if (files.size()==1) {
			openIn(files.get(0), inApp);
		} else {
			if (inApp) {
				List<File> audio = filter(files, f -> AudioFileFormat.isSupported(f, Use.PLAYBACK));
				List<File> images = filter(files, ImageFileFormat::isSupported);

				if (!audio.isEmpty())
					PlaylistManager.use(p -> p.addUris(map(audio, File::toURI)));

				if (images.size()==1) {
					APP.widgetManager.use(ImageDisplayFeature.class, NO_LAYOUT, w -> w.showImage(images.get(0)));
				} else if (images.size()>1) {
					APP.widgetManager.use(ImagesDisplayFeature.class, NO_LAYOUT, w -> w.showImages(images));
				}
			} else {
				browse(files.stream());
			}
		}
	}

	static Try<File,Void> chooseFile(String title, FileType type, File initial, Window w, ExtensionFilter... extensions) {
		if (type==DIRECTORY) {
			DirectoryChooser c = new DirectoryChooser();
			c.setTitle(title);
			c.setInitialDirectory(traverseExistingDir(initial).getOr(APP.DIR_APP));
			File f = c.showDialog(w);
			return f!=null ? ok(f) : error();
		} else {
			FileChooser c = new FileChooser();
			c.setTitle(title);
			c.setInitialDirectory(traverseExistingDir(initial).getOr(APP.DIR_APP));
			if (extensions!=null) c.getExtensionFilters().addAll(extensions);
			File f = c.showOpenDialog(w);
			return f!=null ? ok(f) : error();
		}
	}

	static Try<List<File>,Void> chooseFiles(String title, File initial, Window w, ExtensionFilter... extensions) {
		FileChooser c = new FileChooser();
		c.setTitle(title);
		c.setInitialDirectory(traverseExistingDir(initial).getOr(APP.DIR_APP));
		if (extensions!=null) c.getExtensionFilters().addAll(extensions);
		List<File> fs = c.showOpenMultipleDialog(w);
		return fs!=null && !fs.isEmpty() ? ok(fs) : error();
	}

	static Try<File,Void> saveFile(String title, File initial, String initialName, Window w, ExtensionFilter... extensions) {
		FileChooser c = new FileChooser();
		c.setTitle(title);
		c.setInitialDirectory(traverseExistingDir(initial).getOr(APP.DIR_APP));
		c.setInitialFileName(initialName);
		if (extensions!=null) c.getExtensionFilters().addAll(extensions);
		File f = c.showSaveDialog(w);
		return f!=null ? Try.ok(f) : Try.error();
	}

	// TODO: remove & use Desktop#browseFileDirectory instead
	private static void openWindowsExplorerAndSelect(File file) throws IOException {
		// TODO: make sure the path does not have to be quoted in " like:  "path". Although quoting the path
		// does cause FileNotFoundException, the explorer.exe does open and select the file. I added the
		// quoting after I noticed files with ',' do not work properly, but I have removed the quiting
		// since its working now. Not sure what is going on.
		// Anyway, here are some alternatives to play with, in case the problem reappears:
		// new ProcessBuilder("explorer.exe /select," + f.getPath()).start();
		Runtime.getRuntime().exec(new String[]{"explorer.exe", "/select,", "\"" + file.getPath() + "\""});
//        Runtime.getRuntime().exec("explorer.exe /select," + f.getPath());
	}

}