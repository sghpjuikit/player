package plugin;

import audio.Player;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import gui.objects.icon.Icon;
import gui.objects.textfield.autocomplete.ConfigSearch;
import gui.objects.textfield.autocomplete.ConfigSearch.Entry;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Stream;
import main.App;
import plugin.Plugin.SimplePlugin;
import util.async.future.Fut;
import util.conf.Config.RunnableConfig;
import util.conf.Config.VarList;
import util.conf.Config.VarList.Elements;
import util.conf.IsConfig;
import util.file.Environment;
import util.validation.Constraint;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static util.async.Async.FX;
import static util.dev.Util.log;
import static util.file.Util.*;
import static util.functional.Util.stream;

/**
 * Created by Plutonium_ on 4/25/2017.
 */
public class DirSearchPlugin extends SimplePlugin {

	private static final String NAME = "Dir Search";
	private static final String INDEX_FILE_NAME = "dirs.txt";

	@Constraint.FileType(Constraint.FileActor.DIRECTORY)
	@IsConfig(name = "Location", group = Plugin.CONFIG_GROUP + "." + NAME,
			info = "Root directory the contents of to display "
					+ "This is not a file system browser, and it is not possible to "
					+ "visit parent of this directory."
	)
	private final VarList<File> searchDirs = new VarList<>(File.class, Elements.NOT_NULL);
	private final AtomicLong isUpdatingCache = new AtomicLong(0);
	private final File cacheFile = childOf(App.APP.DIR_USERDATA, "plugins", NAME, INDEX_FILE_NAME);

	@IsConfig(name = "Search depth", group = Plugin.CONFIG_GROUP + "." + NAME)
	final RunnableConfig searchDepth = new RunnableConfig("runsearch", "Update cache", Plugin.CONFIG_GROUP + "." + NAME, "", this::updateCache);

	// TODO: use Path or even better, String
	private List<File> dirs;
	private final Supplier<Stream<Entry>> src = () -> Optional.of(dirs)
				.stream().flatMap(List::stream)
				.map(f ->
						ConfigSearch.Entry.of(
								() -> "Open directory: " + f.getAbsolutePath(),
								() -> "Opens directory: " + f.getAbsolutePath(),
								() -> "Open directory: " + f.getAbsolutePath(),
								() -> Environment.browse(f),
								() -> new Icon<>(FontAwesomeIcon.FOLDER)
						)
				);

	public DirSearchPlugin() {
		super(NAME);
		searchDirs.onListInvalid(dirs -> computeFiles());
	}

	@Override
	public void onStart () {
		computeFiles();
		App.APP.search.sources.add(src);
	}

	@Override
	public void onStop () {
		App.APP.search.sources.remove(src);
	}

	private void computeFiles() {
		boolean cacheFileExists = cacheFile.exists();
		boolean isCacheInvalid = !cacheFileExists;

		if (isCacheInvalid) updateCache();
		else readCache();
	}

	private void readCache() {
		dirs = readFileLines(cacheFile)
				.map(line -> {
					try {
						return Paths.get(line).toFile();
					} catch (Exception e) {
						log(DirSearchPlugin.class).warn("Illegal path value in plugin cache", e);
						return null;
					}
				})
				.filter(Objects::nonNull)
				.collect(toList());
	}

	private void writeCache(List<File> files) {
		String lines = files.stream().map(File::getAbsolutePath).collect(joining("\n"));
		writeFile(cacheFile, lines);
	}

	private void updateCache() {
		long id = isUpdatingCache.getAndIncrement();
		Fut.fut(searchDirs.list)
			.map(Player.IO_THREAD, sourceDirs -> searchDirs.list.stream()
					.distinct()
					.flatMap(dir -> findDirs2(dir, id))
					.collect(toList())
			)
			.use(Player.IO_THREAD, this::writeCache)
			.use(FX, newDirs -> dirs = newDirs)
			.showProgressOnActiveWindow();
	}

	// TODO: remove or fix this not walking directories (it is fast thought)
	private Stream<File> findDirs(File rootDir, long id) {
		try {
			Stream.Builder<File> files = Stream.builder();
			Path dir = Paths.get(rootDir.toURI());
			Files.walkFileTree(dir, new SimpleFileVisitor<>() {
				@Override
				public FileVisitResult visitFile(Path path, BasicFileAttributes atts) throws IOException {
					try {
						if (atts.isDirectory()) files.add(path.toFile());
					} catch (Exception e) {
						e.printStackTrace();
					}
					return isUpdatingCache.get()!=id ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes atts) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException e) throws IOException {
					log(DirSearchPlugin.class).warn("Couldn't not properly read/access file= {}", file, e);
					return FileVisitResult.CONTINUE;
				}
			});
			return files.build();
		} catch (IOException e) {
			log(DirSearchPlugin.class).warn("Error while plugin searched for directories", e);
			return stream();
		}
	}

	// TODO: remove and use fast walking with out File::isDirectory
	private Stream<File> findDirs2(File dir, long id) {
		return listFiles(dir)
			.flatMap(file -> {
				try {
					if (isUpdatingCache.get()!=id) stream();
					return file.isDirectory() ? stream(file).append(findDirs2(file, id)) : stream();
				} catch (Exception e) {
					log(DirSearchPlugin.class).warn("Couldn't not properly read/access file= {}", file, e);
					return stream();
				}
			})
			.filter(File::isDirectory);
	}

}