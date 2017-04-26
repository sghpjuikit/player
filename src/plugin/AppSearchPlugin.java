package plugin;

import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import gui.objects.icon.Icon;
import gui.objects.textfield.autocomplete.ConfigSearch;
import gui.objects.textfield.autocomplete.ConfigSearch.Entry;
import java.io.File;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import main.App;
import plugin.Plugin.SimplePlugin;
import util.access.V;
import util.conf.Config.VarList;
import util.conf.Config.VarList.Elements;
import util.conf.IsConfig;
import util.file.Environment;
import util.validation.Constraint;
import static java.util.stream.Collectors.toList;
import static util.file.Util.getFilesR;

public class AppSearchPlugin extends SimplePlugin {

	@Constraint.FileType(Constraint.FileActor.DIRECTORY)
	@IsConfig(name = "Location", group = Plugin.CONFIG_GROUP + ".App Search",
			info = "Root directory the contents of to display "
					+ "This is not a file system browser, and it is not possible to "
					+ "visit parent of this directory."
	)
	final VarList<File> searchDirs = new VarList<>(File.class, Elements.NOT_NULL);

	@IsConfig(name = "Search depth", group = Plugin.CONFIG_GROUP + ".App Search")
	final V<Integer> searchDepth = new V<>(2, this::computeFiles);

	private List<File> apps;
	private final Supplier<Stream<Entry>> src = () -> apps.stream()
			.map(f ->
					ConfigSearch.Entry.of(
							() -> "Run app: " + f.getName(),
							() -> "Runs application: " + f.getAbsolutePath(),
							() -> "Run app: " + f.getAbsolutePath(),
							() -> Environment.runProgram(f),
							() -> new Icon<>(MaterialIcon.APPS)
					)
			);

	public AppSearchPlugin() {
		super("App Search");
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

	void computeFiles() {
		apps = searchDirs.list.stream()
				.distinct()
				.flatMap(dir -> getFilesR(dir, searchDepth.get(), f -> f.getPath().endsWith(".exe")))
				.collect(toList());
	}
}