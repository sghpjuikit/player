package sp.it.pl.main;

import ch.qos.logback.classic.Level;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import org.reactfx.EventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sp.it.pl.audio.Item;
import sp.it.pl.audio.Player;
import sp.it.pl.audio.playlist.PlaylistItem;
import sp.it.pl.audio.tagging.Metadata;
import sp.it.pl.audio.tagging.MetadataGroup;
import sp.it.pl.gui.Gui;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.pl.gui.objects.popover.PopOver;
import sp.it.pl.gui.objects.tablecell.RatingCellFactory;
import sp.it.pl.gui.objects.tablecell.RatingRatingCellFactory;
import sp.it.pl.gui.objects.textfield.autocomplete.ConfigSearch;
import sp.it.pl.gui.objects.window.stage.Window;
import sp.it.pl.gui.objects.window.stage.WindowManager;
import sp.it.pl.gui.pane.ActionPane;
import sp.it.pl.gui.pane.ActionPane.FastAction;
import sp.it.pl.gui.pane.ActionPane.FastColAction;
import sp.it.pl.gui.pane.ActionPane.SlowColAction;
import sp.it.pl.gui.pane.InfoPane;
import sp.it.pl.gui.pane.MessagePane;
import sp.it.pl.gui.pane.ShortcutPane;
import sp.it.pl.layout.Component;
import sp.it.pl.layout.widget.WidgetManager;
import sp.it.pl.layout.widget.WidgetManager.WidgetSource;
import sp.it.pl.layout.widget.feature.ImageDisplayFeature;
import sp.it.pl.layout.widget.feature.ImagesDisplayFeature;
import sp.it.pl.layout.widget.feature.Opener;
import sp.it.pl.layout.widget.feature.PlaylistFeature;
import sp.it.pl.core.CoreInstances;
import sp.it.pl.core.CoreLogging;
import sp.it.pl.core.CoreSerializer;
import sp.it.pl.core.CoreSerializerXml;
import sp.it.pl.plugin.AppSearchPlugin;
import sp.it.pl.plugin.DirSearchPlugin;
import sp.it.pl.plugin.PluginManager;
import sp.it.pl.plugin.ScreenRotator;
import sp.it.pl.service.ClickEffect;
import sp.it.pl.service.Service;
import sp.it.pl.service.ServiceManager;
import sp.it.pl.service.database.Db;
import sp.it.pl.service.notif.Notifier;
import sp.it.pl.service.playcount.PlaycountIncrementer;
import sp.it.pl.service.tray.TrayService;
import sp.it.pl.util.access.TypedValue;
import sp.it.pl.util.access.V;
import sp.it.pl.util.access.VarEnum;
import sp.it.pl.util.access.fieldvalue.ColumnField;
import sp.it.pl.util.access.fieldvalue.FileField;
import sp.it.pl.util.access.fieldvalue.ObjectField;
import sp.it.pl.util.action.Action;
import sp.it.pl.util.action.IsAction;
import sp.it.pl.util.conf.Config;
import sp.it.pl.util.conf.Configurable;
import sp.it.pl.util.conf.Configuration;
import sp.it.pl.util.conf.IsConfig;
import sp.it.pl.util.conf.IsConfigurable;
import sp.it.pl.util.file.AudioFileFormat;
import sp.it.pl.util.file.AudioFileFormat.Use;
import sp.it.pl.util.file.ImageFileFormat;
import sp.it.pl.util.file.Util;
import sp.it.pl.util.file.mimetype.MimeTypes;
import sp.it.pl.util.functional.Try;
import sp.it.pl.util.graphics.MouseCapture;
import sp.it.pl.util.reactive.SetƑ;
import sp.it.pl.util.system.EnvironmentKt;
import sp.it.pl.util.system.SystemOutListener;
import sp.it.pl.util.type.ClassName;
import sp.it.pl.util.type.InstanceInfo;
import sp.it.pl.util.type.InstanceName;
import sp.it.pl.util.type.ObjectFieldMap;
import sp.it.pl.util.units.FileSize;
import sp.it.pl.util.validation.Constraint;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CSS3;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.GITHUB;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.IMAGE;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.BRUSH;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.EXPORT;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.FOLDER;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.IMPORT;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.INFORMATION_OUTLINE;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.KEYBOARD_VARIANT;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.PLAYLIST_PLUS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.atteo.evo.inflector.English.plural;
import static sp.it.pl.layout.widget.WidgetManager.WidgetSource.ANY;
import static sp.it.pl.layout.widget.WidgetManager.WidgetSource.NEW;
import static sp.it.pl.layout.widget.WidgetManager.WidgetSource.NO_LAYOUT;
import static sp.it.pl.main.AppActionsKt.addToLibraryConsumer;
import static sp.it.pl.util.async.AsyncKt.run;
import static sp.it.pl.util.file.FileType.DIRECTORY;
import static sp.it.pl.util.file.Util.isValidatedDirectory;
import static sp.it.pl.util.functional.Util.list;
import static sp.it.pl.util.functional.Util.stream;
import static sp.it.pl.util.graphics.image.UtilKt.getImageDim;
import static sp.it.pl.util.system.EnvironmentKt.chooseFile;
import static sp.it.pl.util.system.EnvironmentKt.chooseFiles;
import static sp.it.pl.util.system.EnvironmentKt.saveFile;

/**
 * Application. Represents the program.
 * <p/>
 * Single instance:<br>
 * Application can only instantiate single instance, any subsequent call to constructor throws an exception.
 */
@SuppressWarnings({"WeakerAccess", "unused", "Convert2Diamond"})
@IsConfigurable("General")
public class App extends Application implements Configurable {

	/**
	 * Single instance of the application representing this running application.
	 */ public static App APP;
	/**
	 * Logger.
	 */ private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

	/**
	 * Starts program.
	 *
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		// relocate temp to working directory
		// It is our principle to leave no trace of ever running on the system
		// User can also better see what the application is doing
		File tmp = new File(new File(new File("").getAbsoluteFile(), "user"),"tmp");
		isValidatedDirectory(tmp);
		System.setProperty("java.io.tmpdir", tmp.getAbsolutePath());

		launch(args);
		// LauncherImpl.launchApplication(App.class, preloaderClass, args); launch with preloader
	}

	/**
	 * Name of this application.
	 */ public final String name = "PlayerFX";

	/**
	 * Application code encoding. Useful for compilation during runtime.
	 */ public final Charset encoding = StandardCharsets.UTF_8;
	/**
	 * Uri for github website for project of this application.
	 */ public final URI GITHUB_URI = URI.create("https://www.github.com/sghpjuikit/player/");

	/**
	 * Absolute file of directory of this app. Working directory of the project.
	 * Equivalent to new File("").getAbsoluteFile().
	 */ public final File DIR_APP = new File("").getAbsoluteFile();
	/**
	 * Temporary directory of the os.
	 */ public final File DIR_TEMP = new File(System.getProperty("java.io.tmpdir"));
	/**
	 * Home directory of the os.
	 */ public final File DIR_HOME = new File(System.getProperty("user.home"));
	/**
	 * Directory for application logging.
	 */ public final File DIR_LOG = new File(DIR_APP, "log");
	/**
	 * File for application logging configuration.
	 */ public final File FILE_LOG_CONFIG = new File(DIR_LOG, "log_configuration.xml");
	/**
	 * Directory containing widgets - source files, class files and widget's resources.
	 */ public final File DIR_WIDGETS = new File(DIR_APP, "widgets");
	/**
	 * Directory containing skins.
	 */ public final File DIR_SKINS = new File(DIR_APP, "skins");
	/**
	 * Directory containing user data created by application usage, such as customizations, song library, etc.
	 */ public final File DIR_USERDATA =  new File(DIR_APP, "user");
	/**
	 * Directory containing library database.
	 */ public final File DIR_LIBRARY = new File(DIR_USERDATA, "library");
	/**
	 * Directory containing user gui state.
	 */ public final File DIR_LAYOUTS = new File(DIR_USERDATA, "layouts");
	/**
	 * Directory containing application resources.
	 */ public final File DIR_RESOURCES = new File(DIR_APP, "resources");
	/**
	 * File for application configuration.
	 */ public final File FILE_SETTINGS = new File(DIR_USERDATA, "application.properties");

	// cores (always active, mostly singletons)
	public final CoreLogging logging = new CoreLogging(FILE_LOG_CONFIG, DIR_LOG);
	public final CoreSerializerXml serializerXml = new CoreSerializerXml();
	public final CoreSerializer serializer = CoreSerializer.INSTANCE;
	public final CoreInstances instances = CoreInstances.INSTANCE;
	public final MimeTypes mimeTypes = MimeTypes.INSTANCE;
	public final ClassName className = new ClassName();
	public final InstanceName instanceName = new InstanceName();
	public final InstanceInfo instanceInfo = new InstanceInfo();
	public final ObjectFieldMap classFields = new ObjectFieldMap();

	// app stuff
	public final AppParameterProcessor parameterProcessor = new AppParameterProcessor();
	/**
	 * Various actions for the application
	 */ public final AppActions actions = new AppActions();
	/**
	 * Event source and stream for executed actions, providing their name. Use
	 * for notifications of running the action or executing additional behavior.
	 * <p/>
	 * A use case could be an application wizard asking user to do something.
	 * The code in question simply notifies this stream of the name of action
	 * upon execution. The wizard would then monitor this stream
	 * and get notified if the expected action was executed.
	 * <p/>
	 * Running an {@link Action} always fires an event.
	 * <p/>
	 * Usage: simply push a String value into the stream.
	 */ public final EventSource<String> actionStream = new EventSource<>();
	/**
	 * Allows sending and receiving {@link java.lang.String} messages to and from other instances of this application.
	 */ public final AppInstanceComm appCommunicator = new AppInstanceComm();

	/**
	 * Configurable state, i.e., the settings of the application.
	 */ public final Configuration configuration = new Configuration();
	/**
	 * System mouse monitor.
	 */ public final MouseCapture mouseCapture = new MouseCapture();
	/**
	 * Observable {@link System#out}
	 */ public final SystemOutListener systemout = new SystemOutListener();

	public final ActionPane actionPane = new ActionPane(className, instanceName, instanceInfo);
	public final ActionPane actionAppPane = new ActionPane(className, instanceName, instanceInfo);
	public final MessagePane messagePane = new MessagePane();
	public final ShortcutPane shortcutPane = new ShortcutPane();
	public final InfoPane infoPane = new InfoPane();
	public final Guide guide = new Guide();
	public final Search search = new Search();

	/**
	 * Actions ran just before application stopping.
	 * <p/>
	 * At the time of execution all parts of application are fully operational, i.e., the stopping
	 * has not started yet. However, this assumption is valid only for operations on fx thread.
	 * Simply put, do not run any more background tasks, as the application will begin closing in
	 * the meantime and be in inconsistent state.
	 */ public final SetƑ onStop = new SetƑ();
	@IsConfig(name = "Normal mode", info = "Whether application loads into previous/default state or no state at all.")
	public boolean normalLoad = true;
	private boolean isInitialized = false;
	private boolean close_prematurely = false;

	/**
	 * Manages persistence and in-memory storage.
	 */ public final Db db = new Db();
	/**
	 * Manages windows.
	 */ public final WindowManager windowManager = new WindowManager();
	/**
	 * Manages widgets.
	 */ public final WidgetManager widgetManager = new WidgetManager(windowManager, messagePane::show);
	/**
	 * Manages services.
	 */ public final ServiceManager services = new ServiceManager();
	/**
	 * Manages services.
	 */ public final PluginManager plugins = new PluginManager(configuration, messagePane::show);

	@IsConfig(name = "Rating control", info = "The style of the graphics of the rating control.")
	public final VarEnum<RatingCellFactory> ratingCell = VarEnum.ofInstances(RatingRatingCellFactory.INSTANCE, RatingCellFactory.class, instances);

	@IsConfig(name = "Rating icon amount", info = "Number of icons in rating control.")
	@Constraint.MinMax(min=0, max=10)
	public final V<Integer> maxRating = new V<>(5);

	@IsConfig(name = "Rating allow partial", info = "Allow partial values for rating.")
	public final V<Boolean> partialRating = new V<>(true);

	@IsConfig(name = "Rating editable", info = "Allow change of rating. Defaults to application settings")
	public final V<Boolean> allowRatingChange = new V<>(true);

	@IsConfig(name = "Debug value (double)", info = "For application testing. Generic number value "
			+ "to control some application value manually.")
	public final V<Double> debug = new V<>(0d, () -> {});

	@IsConfig(name = "Debug value (boolean)", info = "For application testing. Generic yes/false value "
			+ "to control some application value manually.")
	public final V<Boolean> debug2 = new V<>(false, () -> {});

	@IsConfig(name = "Enabled", group = "Taskbar", info = "Show taskbar icon. Disabling taskbar will"
			+ "also disable ALT+TAB functionality.")
	public final V<Boolean> taskbarEnabled = new V<>(true);

	@IsConfig(info = "Preferred text when no tag value for field. This value can be overridden.")
	public String TAG_NO_VALUE = "<none>";

	@IsConfig(info = "Preferred text when multiple tag values per field. This value can be overridden.")
	public String TAG_MULTIPLE_VALUE = "<multi>";

	@Constraint.MinMax(min=10, max=60)
	@IsConfig(info = "Update frequency in Hz for performance-heavy animations.")
	public double animationFps = 60.0;

	@IsConfig(name = "Level (console)", group = "Logging", info = "Logging level for logging to console")
	public final VarEnum<Level> logLevelConsole = new VarEnum<Level>(Level.DEBUG,
			list(Level.ALL, Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.OFF),
			l -> logging.changeLogBackLoggerAppenderLevel("STDOUT", l)
	);

	@IsConfig(name = "Level (file)", group = "Logging", info = "Logging level for logging to file")
	public final VarEnum<Level> logLevelFile = new VarEnum<Level>(Level.WARN,
			list(Level.ALL, Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.OFF),
			l -> logging.changeLogBackLoggerAppenderLevel("FILE", l)
	);

	public App() {
		if (APP==null) APP = this;
		else throw new RuntimeException("Multiple application instances disallowed");
	}

	/**
	 * The application initialization method. This method is called immediately
	 * after the Application class is loaded and constructed. An application may
	 * override this method to perform initialization prior to the actual starting
	 * of the application.
	 * <p/>
	 * NOTE: This method is not called on the JavaFX Application Thread. An
	 * application must not construct a Scene or a Stage in this method. An
	 * application may construct other JavaFX objects in this method.
	 */
	@Override
	public void init() {
		logging.init();

		// Forbid multiple application instances, instead notify the 1st instance of 2nd (this one)
		// trying to run and this instance's run parameters and close prematurely
		if (getInstances()>1) {
			LOGGER.info("Multiple app instances detected.");
			appCommunicator.fireNewInstanceEvent(fetchParameters());
			close_prematurely = true;
			LOGGER.info("App wil close prematurely.");
		}
		if (close_prematurely) return;

		// add optional object fields
		classFields.add(PlaylistItem.class, PlaylistItem.Field.FIELDS);
		classFields.add(Metadata.class, Metadata.Field.FIELDS);
		classFields.add(MetadataGroup.class, MetadataGroup.Field.FIELDS);
		classFields.add(Object.class, ColumnField.FIELDS);
		classFields.add(File.class, FileField.FIELDS);

		// add optional object class -> string converters
		className.addNoLookup(Void.class, "Nothing");
		className.add(String.class, "Text");
		className.add(App.class, "Application");
		className.add(Item.class, "Song");
		className.add(PlaylistItem.class, "Playlist Song");
		className.add(Metadata.class, "Library Song");
		className.add(MetadataGroup.class, "Song Group");
		className.add(List.class, "List");

		// add optional object instance -> string converters
		instanceName.add(Void.class, o -> "<none>");
		instanceName.add(App.class, app -> "This application");
		instanceName.add(Item.class, Item::getPathAsString);
		instanceName.add(PlaylistItem.class, PlaylistItem::getTitle);
		instanceName.add(Metadata.class, Metadata::getTitleOrEmpty);
		instanceName.add(MetadataGroup.class, o -> Objects.toString(o.getValue()));
		instanceName.add(Component.class, Component::getName);
		instanceName.add(File.class, File::getPath);
		instanceName.add(Collection.class, o -> {
			Class<?> eType = sp.it.pl.util.type.Util.getGenericPropertyType(o.getClass());
			String eName = eType == null || eType==Object.class ? "Item" : className.get(eType);
			return o.size() + " " + plural(eName,o.size());
		});

		// add optional object instance -> info string converters
		instanceInfo.add(Void.class, (v,map) -> {});
		instanceInfo.add(String.class, (s,map) -> map.put("Length", Integer.toString(s==null ? 0 : s.length())));
		instanceInfo.add(File.class, (f,map) -> {
			String suffix = Util.getSuffix(f);
			FileSize fs = new FileSize(f);
			String fsBytes = fs.isUnknown() ? "" : " (" + String.format("%,d ", fs.inBytes()).replace(',', ' ') + "bytes)";
			map.put("Size", fs.toString() + fsBytes);
			map.put("Format", suffix.isEmpty() ? "n/a" : suffix);

			ImageFileFormat iff = ImageFileFormat.of(f.toURI());
			if (iff.isSupported()) {
				String res = getImageDim(f).map(id -> id.width + " x " + id.height).getOr("n/a");
				map.put("Resolution", res);
			}
		});
		instanceInfo.add(App.class, (v,map) -> map.put("Name", v.name));
		instanceInfo.add(Metadata.class, (m,map) ->
				stream(Metadata.Field.FIELDS)
					.filter(f -> f.isTypeStringRepresentable() && !f.isFieldEmpty(m))
					.forEach(f -> map.put(f.name(), m.getFieldS(f)))
		);
		instanceInfo.add(PlaylistItem.class, p ->
			stream(PlaylistItem.Field.FIELDS)
					.filter(TypedValue::isTypeString)
					.collect(toMap(ObjectField::name, f -> (String)f.getOf(p)))
		);

		// register actions
		actionPane.register(Void.class,
			new FastAction<>(
				"Select file",
				"Open file chooser to select files",
				MaterialDesignIcon.FILE,
				actionPane.converting(none ->
					chooseFiles("Select file...", null, actionPane.getScene().getWindow())
				)
			),
			new FastAction<>(
				"Select directory",
				"Open file chooser to select directory",
				MaterialDesignIcon.FOLDER,
				actionPane.converting(none ->
					chooseFile("Select directory...", DIRECTORY, null, actionPane.getScene().getWindow())
				)
			)
		);
		actionPane.register(Object.class,
			new FastColAction<Object>("Set as data",
				"Sets the selected data as input.",
				MaterialDesignIcon.DATABASE,
				actionPane.converting(Try::ok)
			),
			new FastColAction<>("Open in Converter",
				"Open data in Converter.",
				MaterialDesignIcon.SWAP_HORIZONTAL,
				// TODO: make sure it opens Converter or support multiple Opener types
				f -> widgetManager.use(Opener.class, ANY, o -> o.open(f))
			)
		);
		actionPane.register(Component.class,
			new FastAction<>("Export",
				  "Creates a launcher for this component. \n"
				+ "Opening the launcher with this application will open this component with current settings "
				+ "as if it were a standalone application.",
				EXPORT,
				w -> saveFile("Export to...", DIR_LAYOUTS, w.getExportName(), APP.actionPane.getScene().getWindow(), new ExtensionFilter("Component", "*.fxwl"))
					.ifOk(w::exportFxwl)
			)
		);
		actionPane.register(Item.class,
			new FastColAction<>("Add to new playlist",
				"Add items to new playlist widget.",
				PLAYLIST_PLUS,
				items -> widgetManager.use(PlaylistFeature.class, NEW, p -> p.getPlaylist().addItems(items))
			),
			new FastColAction<>("Add to existing playlist",
				"Add items to existing playlist widget if possible or to a new one if not.",
				PLAYLIST_PLUS,
				items -> widgetManager.use(PlaylistFeature.class, ANY, p -> p.getPlaylist().addItems(items))
			),
			new FastColAction<>("Update from file",
				"Updates library data for the specified items from their file metadata. The difference between the data "
				+ "in the database and real metadata cab be a result of a bug or file edited externally. "
				+ "After this, the library will be synchronized with the file data.",
				FontAwesomeIcon.REFRESH,
				Player::refreshItems
			),
			new FastColAction<>("Remove from library",
				"Removes all specified items from library. After this library will contain none of these items.",
				MaterialDesignIcon.DATABASE_MINUS,
				db::removeItems
			),
			new FastColAction<>("Show",
				"Shows items in a table.",
				MaterialIcon.COLLECTIONS,
				items -> widgetManager.find("Library", WidgetSource.NEW, false)
						.ifPresent(w -> w.getController().getInputs().getInput("To display").setValue(items))
			),
			new FastColAction<>("Show as Group",
				"Group items in a table.",
				MaterialIcon.COLLECTIONS,
				items -> widgetManager.find("Library View", WidgetSource.NEW, false)
						.ifPresent(w -> w.getController().getInputs().getInput("To display").setValue(items))
			)
		);
		actionPane.register(File.class,
			new FastAction<>("Recycle", "Moves file to recycle bin.",
				MaterialIcon.DELETE,
				Util::recycleFile),
			new FastAction<>("Read metadata", "Prints all image metadata to console.",
				MaterialIcon.IMAGE_ASPECT_RATIO,
				ImageFileFormat::isSupported,
				actions::printAllImageFileMetadata),
			new FastAction<>("Open (OS)", "Opens file in a native program associated with this file type.",
				MaterialIcon.OPEN_IN_NEW,
				EnvironmentKt::open),
			new FastAction<>("Edit (OS)", "Edit file in a native editor program associated with this file type.",
				FontAwesomeIcon.EDIT,
				EnvironmentKt::edit),
			new FastAction<>("Browse (OS)", "Browse file in a native file system browser.",
				FontAwesomeIcon.FOLDER_OPEN_ALT,
				EnvironmentKt::browse),
			new FastColAction<File>("Add to new playlist",
				"Add items to new playlist widget.",
				MaterialDesignIcon.PLAYLIST_PLUS,
				f -> AudioFileFormat.isSupported(f, Use.APP),
				fs -> widgetManager.use(PlaylistFeature.class, NEW, p -> p.getPlaylist().addFiles(fs))
			),
			new SlowColAction<File>("Find files",
				"Looks for files recursively in the the data.",
				MaterialDesignIcon.FILE_FIND,
				actionPane.converting(fs ->
					// TODO: make fully configurable, recursion depth lvl, filtering, ...
					Try.ok(Util.getFilesR(fs, Integer.MAX_VALUE).collect(toList()))
				)
			),
			new SlowColAction<File>("Add to library",
				"Add items to library if not yet contained and edit added items in tag editor. If "
				+ "item already was in the database it will not be added or edited.",
				MaterialDesignIcon.DATABASE_PLUS,
				items -> {}
			).preventClosing(addToLibraryConsumer(actionPane)),
			new FastColAction<>("Add to existing playlist",
				"Add items to existing playlist widget if possible or to a new one if not.",
				PLAYLIST_PLUS,
				f -> AudioFileFormat.isSupported(f, Use.APP),
				f -> widgetManager.use(PlaylistFeature.class, ANY, p -> p.getPlaylist().addFiles(f))
			),
			new FastAction<>("Apply skin", "Apply skin on the application.",
				BRUSH,
				Util::isValidSkinFile,
				skin_file -> Gui.setSkin(skin_file)),
			new FastAction<>("View image", "Opens image in an image viewer widget.",
				IMAGE,
				ImageFileFormat::isSupported,
				img_file -> widgetManager.use(ImageDisplayFeature.class, NO_LAYOUT, w -> w.showImage(img_file))
			),
			new FastColAction<>("View image", "Opens image in an image browser widget.",
				IMAGE,
				ImageFileFormat::isSupported,
				img_files -> widgetManager.use(ImagesDisplayFeature.class, NO_LAYOUT, w -> w.showImages(img_files))
			),
			new FastAction<>("Open widget", "Opens exported widget.",
				IMPORT,
				f -> f.getPath().endsWith(".fxwl"),
				windowManager::launchComponent
			)
		);

		// add app parameter handlers
		parameterProcessor.addFileProcessor(
			f -> AudioFileFormat.isSupported(f, Use.APP),
			fs -> widgetManager.use(PlaylistFeature.class, ANY, p -> p.getPlaylist().addFiles(fs))
		);
		parameterProcessor.addFileProcessor(
			Util::isValidSkinFile,
			fs -> Gui.setSkin(fs.get(0))
		);
		parameterProcessor.addFileProcessor(
			ImageFileFormat::isSupported,
			fs -> widgetManager.use(ImageDisplayFeature.class, NO_LAYOUT, w->w.showImages(fs))
		);
		parameterProcessor.addFileProcessor(f -> f.getPath().endsWith(".fxwl"),
			fs -> fs.forEach(windowManager::launchComponent)
		);
		parameterProcessor.addStringProcessor(
			s -> widgetManager.getFactories().anyMatch(f -> f.nameGui().equals(s) || f.name().equals(s)),
			ws -> ws.forEach(windowManager::launchComponent)
		);

		// init cores
		serializer.init();
		serializerXml.init();
		instances.init();

		// add search sources
		search.getSources().addAll(list(
			() -> APP.configuration.getFields().stream().map(ConfigSearch.Entry::of),
			() -> APP.widgetManager.getComponentFactories().map(ConfigSearch.Entry::of),
			() -> Gui.skin.streamValues().map(s -> ConfigSearch.Entry.of(() -> "Open skin: " + s, () -> Gui.skin.setNapplyValue(s), () -> new Icon(MaterialIcon.BRUSH)))
		));

		plugins.installPlugins(
			new AppSearchPlugin(),
			new DirSearchPlugin(),
			new ScreenRotator()
		);

		// listen to other application instance launches
		try {
			appCommunicator.start();
			appCommunicator.onNewInstanceHandlers.add(parameterProcessor::process);
		} catch (RemoteException e) {
			LOGGER.warn("App instance communicator failed to start.", e);
		}

		// start global shortcuts
		Action.startActionListening();
		Action.loadCommandActions();

	}

	/**
	 * The main entry point for applications. The start method is
	 * called after the init method has returned, and after the system is ready
	 * for the application to begin running.
	 * <p/>
	 * NOTE: This method is called on the JavaFX Application Thread.
	 * @param primaryStage the primary stage for this application, onto which
	 * the application scene can be set. The primary stage will be embedded in
	 * the browser if the application was launched as an applet. Applications
	 * may create other stages, if needed, but they will not be primary stages
	 * and will not be embedded in the browser.
	 */
	@Override
	public void start(Stage primaryStage) {
		if (close_prematurely) {
			LOGGER.info("Application closing prematurely.");
			close();
			return;
		}

		isInitialized = Try.tryCatchAll(() -> {
				// services must be created before Configuration
				services.addService(new TrayService());
				services.addService(new Notifier());
				services.addService(new PlaycountIncrementer());
				services.addService(new ClickEffect());

				// install actions
				Action.installActions(
					this,
					actions,
					windowManager,
					guide,
					services.getAllServices()
				);

				actionAppPane.register(App.class,
					new FastAction<>(
						"Export widgets",
						"Creates launcher file in the destination directory for every widget.\n"
						+ "Launcher file is a file that when opened by this application opens the widget. "
						+ "If application was not running before, it will not load normally, but will only "
						+ "open the widget.\n"
						+ "Essentially, this exports the widgets as 'standalone' applications.",
						EXPORT,
						app -> {
							DirectoryChooser dc = new DirectoryChooser();
							dc.setInitialDirectory(app.DIR_LAYOUTS);
							dc.setTitle("Export to...");
						File dir = dc.showDialog(app.actionAppPane.getScene().getWindow());
							if (dir!=null) {
								app.widgetManager.getFactories().forEach(w -> w.create().exportFxwlDefault(dir));
							}
						}
					),
					new FastAction<>(KEYBOARD_VARIANT,Action.get("Show shortcuts")),
					new FastAction<>(INFORMATION_OUTLINE,Action.get("Show system info")),
					new FastAction<>(GITHUB,Action.get("Open on Github")),
					new FastAction<>(CSS3,Action.get("Open css guide")),
					new FastAction<>(IMAGE,Action.get("Open icon viewer")),
					new FastAction<>(FOLDER,Action.get("Open app directory"))
				);

				widgetManager.init();
				db.init();

				// gather configs
				configuration.rawAdd(FILE_SETTINGS);
				configuration.collectStatic();
				configuration.collect(Action.getActions());
				services.getAllServices().forEach(configuration::collect);
				configuration.collect(this, windowManager, guide);
				configuration.collect(Configurable.configsFromFieldsOf("actionChooserView", "View.Action Chooser", actionPane));
				configuration.collect(Configurable.configsFromFieldsOf("actionChooserAppView", "View.Action App Chooser", actionAppPane));
				configuration.collect(Configurable.configsFromFieldsOf("shortcutViewer", "View.Shortcut Viewer", shortcutPane));
				configuration.collect(Configurable.configsFromFieldsOf("infoView", "View.System info", infoPane));

				// deserialize values (some configs need to apply it, will do when ready)
				configuration.rawSet();

				// initializing, the order is important
				Player.initialize();

				List<String> ps = fetchParameters();
				normalLoad &= ps.stream().noneMatch(s -> s.endsWith(".fxwl") || widgetManager.factories.get(s)!=null);

				// load windows, layouts, widgets
				// we must apply skin before we load graphics, solely because if skin defines custom
				// Control Skins, it will only have effect when set before control is created
				configuration.getFields(f -> f.getGroup().equals("Gui") && f.getGuiName().equals("Skin")).findFirst().get().applyValue();
				windowManager.deserialize(normalLoad);

				isInitialized = true;

			})
			.ifError(e -> {
				LOGGER.error("Application failed to start", e);
				messagePane.show("Application did not start successfully.");
			})
			.ifOk(ignored -> {
				// initialization is complete -> apply all settings
				configuration.getFields().forEach(Config::applyValue);

				// initialize non critical parts
				if (normalLoad) Player.loadLast();

				// show guide
				if (guide.first_time.get()) run(3000, guide::start);

				// process app parameters passed when app started
				parameterProcessor.process(fetchParameters());
			})
			.isOk();
	}

	/** Starts this application normally if not yet started that way, otherwise has no effect. */
	@IsAction(name = "Start app normally", desc = "Loads last application state if not yet loaded. Has only effect once.")
	public void startNormally() {
		if(!normalLoad) {
			normalLoad = true;
			windowManager.deserialize(true);
			Player.loadLast();
		}
	}

	/**
	 * This method is called when the application should stop, and provides a
	 * convenient place to prepare for application exit and destroy resources.
	 * NOTE: This method is called on the JavaFX Application Thread.
	 */
	@Override
	public void stop() {
		if (isInitialized) {
			onStop.run();
			if (normalLoad) Player.state.serialize();
			if (normalLoad) windowManager.serialize();
			configuration.save(name,FILE_SETTINGS);
			services.getAllServices()
					.filter(Service::isRunning)
					.forEach(Service::stop);
		}
		db.stop();
		CoreSerializer.INSTANCE.dispose();
		Action.stopActionListening();
		appCommunicator.stop();
	}

	/** Closes this app normally. Invokes {@link #stop()} as a result. */
	@IsAction(name = "Close app", desc = "Closes this application.")
	public void close() {
		// javaFX bug fix - must close all popups before windows (new list avoids ConcurrentModificationError)
		list(PopOver.active_popups).forEach(PopOver::hideImmediatelly);
		// closing app can take a little while, during which the gui will not respond
		// To avoid that effect, hide all windows instantly and close app "in the background".
		windowManager.windows.forEach(Window::hide); // this assumes we don't serialize visibility state !

		Platform.exit();    // close app
	}

	/**
	 * Closes this app abnormally, so next time this app does not start normally.
	 * Invokes {@link #close()} and then {@link #stop()} as a result.
	 */
	@IsAction(name = "Close app abnormally", desc = "Closes this application so next time it loads it does not loads" +
													" its state. The state is saved and can be loaded manually at any time.")
	public void closeAbnormally() {
		normalLoad = false;
		close();
	}

	public List<String> fetchParameters() {
		// Note: Parameters are never null, but if the application is created manually from other class
		// it is possible and we don't want failure, hence the null check
		List<String> params = new ArrayList<>();
		Parameters ps = getParameters();
		if (ps!=null) {
			getParameters().getRaw().forEach(params::add);
			getParameters().getUnnamed().forEach(params::add);
			getParameters().getNamed().forEach((t, value) -> params.add(value));
		}
		return params;
	}

	/** @return number of instances of this application (including this one) running at this moment */
	public static int getInstances() {
		int instances = 0;
		for (VirtualMachineDescriptor vmd : VirtualMachine.list()) {
			boolean isSame = App.class.getName().equals(vmd.displayName());
			if (isSame) instances++;
		}
		return instances;
	}

	/** @return image of the icon of the application */
	public Image getIcon() {
		return new Image(new File("icon512.png").toURI().toString());
	}

	/** @return images of the icon of the application in all possible sizes */
	public List<Image> getIcons() {
		return stream(16, 24, 32, 48, 128, 256, 512)
			.map(size -> "icon" + size + ".png")
			.map(path -> new File(path).toURI().toString())
			.map(Image::new)
			.collect(toList());
	}

}