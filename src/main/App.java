package main;

import audio.Item;
import audio.Player;
import audio.SimpleItem;
import audio.playlist.Playlist;
import audio.playlist.PlaylistItem;
import audio.tagging.Metadata;
import audio.tagging.MetadataGroup;
import audio.tagging.MetadataReader;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.spi.FilterAttachable;
import ch.qos.logback.core.util.StatusPrinter;
 import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.mapper.Mapper;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import gui.Gui;
import gui.infonode.ConvertTaskInfo;
import gui.objects.icon.Icon;
import gui.objects.popover.PopOver;
import gui.objects.spinner.Spinner;
import gui.objects.tablecell.*;
import gui.objects.textfield.autocomplete.ConfigSearch;
import gui.objects.window.stage.Window;
import gui.objects.window.stage.WindowManager;
import gui.pane.*;
import gui.pane.ActionPane.ComplexActionData;
import gui.pane.ActionPane.FastAction;
import gui.pane.ActionPane.FastColAction;
import gui.pane.ActionPane.SlowColAction;
import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.LogManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import layout.Component;
import layout.widget.Widget;
import layout.widget.WidgetManager;
import layout.widget.WidgetManager.WidgetSource;
import layout.widget.feature.*;
import org.atteo.classindex.ClassIndex;
import org.reactfx.EventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import plugin.AppSearchPlugin;
import plugin.DirSearchPlugin;
import plugin.Plugin;
import plugin.ScreenRotator;
import services.ClickEffect;
import services.Service;
import services.ServiceManager;
import services.database.Db;
import services.notif.Notifier;
import services.playcount.PlaycountIncrementer;
import services.tray.TrayService;
import sun.awt.www.content.image.png;
import util.SingleR;
import util.access.TypedValue;
import util.access.V;
import util.access.VarEnum;
import util.access.fieldvalue.ColumnField;
import util.access.fieldvalue.FileField;
import util.access.fieldvalue.ObjectField;
import util.action.Action;
import util.action.IsAction;
import util.async.future.ConvertListTask;
import util.conf.*;
import util.conf.Config.PropertyConfig;
import util.file.AudioFileFormat;
import util.file.AudioFileFormat.Use;
import util.file.Environment;
import util.file.ImageFileFormat;
import util.file.Util;
import util.file.mimetype.MimeTypes;
import util.functional.Try;
import util.graphics.MouseCapture;
import util.reactive.SetƑ;
import util.serialize.xstream.*;
import util.system.SystemOutListener;
import util.type.*;
import util.units.FileSize;
import util.validation.Constraint;
import web.*;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.*;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.FOLDER;
import static gui.pane.ActionPane.collectionWrap;
import static java.util.stream.Collectors.toList;
import static layout.widget.WidgetManager.WidgetSource.*;
import static org.atteo.evo.inflector.English.plural;
import static util.Util.getImageDim;
import static util.async.Async.FX;
import static util.async.Async.run;
import static util.async.future.Fut.fut;
import static util.file.Environment.chooseFile;
import static util.file.Environment.chooseFiles;
import static util.file.FileType.DIRECTORY;
import static util.file.Util.*;
import static util.functional.Util.*;
import static util.graphics.Util.layHorizontally;
import static util.graphics.Util.layVertically;

/**
 * Application. Represents the program.
 * <p/>
 * Single instance:<br>
 * Application can only instantiate single instance, any subsequent call to constructor throws an exception.
 */
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
	 * Directory containing playlists.
	 */ public final File DIR_PLAYLISTS = new File(DIR_USERDATA, "playlists");
	/**
	 * Directory containing application resources.
	 */ public final File DIR_RESOURCES = new File(DIR_APP, "resources");
	/**
	 * File for application configuration.
	 */ public final File FILE_SETTINGS = new File(DIR_USERDATA, "application.properties");

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
	 * Handles application parameters as commands
	 */ public final AppParameterProcessor parameterProcessor = new AppParameterProcessor();
	/**
	 * Serializators and deserializators, e.g., to save object into a file.
	 */ public final AppSerializer serializators = new AppSerializer(encoding);
	/**
	 * Configurable state, i.e., the settings of the application.
	 */ public final Configuration configuration = new Configuration();
	/**
	 * System mouse monitor.
	 */ public final MouseCapture mouseCapture = new MouseCapture();
	/**
	 * Observable {@link System#out}
	 */ public final SystemOutListener systemout = new SystemOutListener();

	public final ClassName className = new ClassName();
	public final InstanceName instanceName = new InstanceName();
	public final InstanceInfo instanceInfo = new InstanceInfo();
	public final ObjectFieldMap classFields = new ObjectFieldMap();

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
	 * Instance enumerations.
	 */ public final InstanceMap instances = new InstanceMap();
	/**
	 * File mime type map.
	 * Initialized with the built-in mime types definitions.
	 */ public final MimeTypes mimeTypes = MimeTypes.standard();

	@IsConfig(name = "Rating control", info = "The style of the graphics of the rating control.")
	public final VarEnum<RatingCellFactory> ratingCell = VarEnum.ofInstances(RatingRatingCellFactory.INSTANCE, RatingCellFactory.class, instances);

	@IsConfig(name = "Rating icon amount", info = "Number of icons in rating control.")
	@Constraint.MinMax(min=0, max=10)
	public final V<Integer> maxRating = new V<>(5);

	@IsConfig(name = "Rating allow partial", info = "Allow partial values for rating.")
	public final V<Boolean> partialRating = new V<>(true);

	@IsConfig(name = "Rating editable", info = "Allow change of rating. Defaults to application settings")
	public final V<Boolean> allowRatingChange = new V<>(true);

	@IsConfig(name = "Rating react on hover", info = "Move rating according to mouse when hovering.")
	public final V<Boolean> hoverRating = new V<>(true);

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

	@IsConfig(name = "log level (console)", group = "Logging", info = "Logging level for logging to console")
	public final VarEnum<Level> logLevelConsole = new VarEnum<Level>(Level.DEBUG,
			() -> list(Level.ALL, Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.OFF),
			l -> changeLogBackLoggerAppenderLevel("STDOUT", l)
	);

	@IsConfig(name = "log level (file)", group = "Logging", info = "Logging level for logging to file")
	public final VarEnum<Level> logLevelCFile = new VarEnum<Level>(Level.WARN,
			() -> list(Level.ALL, Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.OFF),
			l -> changeLogBackLoggerAppenderLevel("STDOUT", l)
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
		configureLogging();

		// Forbid multiple application instances, instead notify the 1st instance of 2nd (this one)
		// trying to run and this instance's run parameters and close prematurely
		if (getInstances()>1) {
			LOGGER.info("Multiple app instances detected.");
			appCommunicator.fireNewInstanceEvent(fetchParameters());
			close_prematurely = true;
			LOGGER.info("App wil close prematurely.");
		}
		if (close_prematurely) return;

		// configure serialization
		XStream x = serializators.x;
		Mapper xm = x.getMapper();
		x.autodetectAnnotations(true);
		x.registerConverter(new StringPropertyConverter(xm));   // javafx properties
		x.registerConverter(new BooleanPropertyConverter(xm));  // -||-
		x.registerConverter(new DoublePropertyConverter(xm));   // -||-
		x.registerConverter(new LongPropertyConverter(xm));     // -||-
		x.registerConverter(new IntegerPropertyConverter(xm));  // -||-
		x.registerConverter(new ObjectPropertyConverter(xm));   // -||-
		x.registerConverter(new ObservableListConverter(xm));   // -||-
		x.registerConverter(new VConverter(xm));
		x.registerConverter(new PlaybackStateConverter());
		x.registerConverter(new PlaylistConverter(xm));
		x.registerConverter(new PlaylistItemConverter());
		x.alias("Component", Component.class);
		x.alias("Playlist", Playlist.class);
		x.alias("item", PlaylistItem.class);
		ClassIndex.getSubclasses(Component.class).forEach(c -> x.alias(c.getSimpleName(),c));
		x.useAttributeFor(Component.class, "id");
		x.useAttributeFor(Widget.class, "name");

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
		instanceName.add(Item.class, Item::getPath);
		instanceName.add(PlaylistItem.class, PlaylistItem::getTitle);
		instanceName.add(Metadata.class, Metadata::getTitle);
		instanceName.add(MetadataGroup.class, o -> Objects.toString(o.getValue()));
		instanceName.add(Component.class, Component::getName);
		instanceName.add(File.class, File::getPath);
		instanceName.add(Collection.class, o -> {
			Class<?> eType = util.type.Util.getGenericPropertyType(o.getClass());
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
					.toMap(ObjectField::name, f -> (String)f.getOf(p))
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
			new FastColAction<>("Set as data",
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
		actionPane.register(Widget.class,
			new FastAction<>("Create launcher (def)",
				"Creates a launcher for this widget with default (no predefined) settings. \n"
				+ "Opening the launcher with this application will open this "
				+ "widget as if it were a standalone application.",
				EXPORT,
				w -> Environment
					.chooseFile("Export to...", DIRECTORY, DIR_APP, APP.actionPane.getScene().getWindow())
					.ifOk(w::exportFxwlDefault)
			)
		);
		actionPane.register(Component.class,
			new FastAction<>("Export",
				  "Creates a launcher for this component with current settings. \n"
				+ "Opening the launcher with this application will open this component as if it were a standalone application.",
				EXPORT,
				w -> Environment
					.chooseFile("Export to...", DIRECTORY, DIR_LAYOUTS, APP.actionPane.getScene().getWindow())
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
				Player::refreshItems // this needs to be asynchronous
			),
			new FastColAction<>("Remove from library",
				"Removes all specified items from library. After this library will contain none of these items.",
				MaterialDesignIcon.DATABASE_MINUS,
				db::removeItems // this needs to be asynchronous
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
				Environment::open),
			new FastAction<>("Edit (OS)", "Edit file in a native editor program associated with this file type.",
				FontAwesomeIcon.EDIT,
				Environment::edit),
			new FastAction<>("Browse (OS)", "Browse file in a native file system browser.",
				FontAwesomeIcon.FOLDER_OPEN_ALT,
				Environment::browse),
			new FastColAction<>("Add to new playlist",
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
			).preventClosing(new ComplexActionData<Collection<File>,List<File>>(
				() -> {
					V<Boolean> makeWritable = new V<>(true);
					V<Boolean> editInTagger = new V<>(true);
					V<Boolean> editOnlyAdded = new V<>(false);
					V<Boolean> enqueue = new V<>(false);
					ConvertListTask<Item,Metadata> task = MetadataReader.buildAddItemsToLibTask();
					ConvertTaskInfo info = new ConvertTaskInfo(null, new Label(), new Label(), new Label(), new Spinner().hidingOnIdle(true));
									info.bind(task);
					SingleR<Widget,Void> tagger = new SingleR<>(() -> stream(APP.widgetManager.factories)
								.findFirst(f -> f.name().equals("Tagger"))
								.get().create());
					return layHorizontally(50, Pos.CENTER,
						layVertically(50, Pos.CENTER,
							new ConfigPane<>(
								Config.forProperty(Boolean.class, "Make writable if read-only", makeWritable),
								Config.forProperty(Boolean.class, "Edit in Tagger", editInTagger),
								Config.forProperty(Boolean.class, "Edit only added files", editOnlyAdded),
								Config.forProperty(Boolean.class, "Enqueue in playlist", enqueue)
							).getNode(),
							layVertically(10, Pos.CENTER_LEFT,
								info.state,
								layHorizontally(10, Pos.CENTER_LEFT,
									info.message,
									info.progressIndicator
								),
								info.skipped
							),
							new Icon(FontAwesomeIcon.CHECK,25).onClick(e -> {
								((Icon) e.getSource()).setDisable(true);
								fut((List<File>) collectionWrap(actionPane.getData()))  // TODO: make automatic
									.use(files -> {
										if (makeWritable.get()) files.forEach(f -> f.setWritable(true));
									})
									.map(files -> map(files, SimpleItem::new))
									.map(task)
									.showProgress(actionPane.actionProgress)
									.use(FX, r -> {
										if (editInTagger.get()) {
											List<? extends Item> items = editOnlyAdded.get() ? r.converted : r.all;
											((SongReader) tagger.get().getController()).read(items);
										}
										if (enqueue.get() && !r.all.isEmpty()) {
											APP.widgetManager.find(PlaylistFeature.class, WidgetSource.ANY)
												.map(PlaylistFeature::getPlaylist)
												.ifPresent(p -> p.addItems(r.all));
										}
									});
							}).withText("Execute")
						),
						tagger.get().load()
					);
				},
				files -> fut(files).map(fs -> getFilesAudio(fs, Use.APP, Integer.MAX_VALUE).collect(toList()))
			)),
			new FastColAction<>("Add to existing playlist",
				"Add items to existing playlist widget if possible or to a new one if not.",
				PLAYLIST_PLUS,
				f -> AudioFileFormat.isSupported(f, Use.APP),
				f -> widgetManager.use(PlaylistFeature.class, ANY, p -> p.getPlaylist().addFiles(f))
			),
			new FastAction<>("Apply skin", "Apply skin on the application.",
				BRUSH,
				Util::isValidSkinFile,
				skin_file -> Gui.setSkin(getName(skin_file))),
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
			fs -> Gui.setSkin(getName(fs.get(0)))
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

		// add search sources
		search.getSources().addAll(list(
			() -> APP.configuration.getFields().stream().map(ConfigSearch.Entry::of),
			() -> APP.widgetManager.factories.streamV().map(ConfigSearch.Entry::of),
			() -> Gui.skin.streamValues().map(s -> ConfigSearch.Entry.of(() -> "Open skin: " + s, () -> Gui.skin.setNapplyValue(s), () -> new Icon(MaterialIcon.BRUSH)))
		));

		// TODO: implement plugin discovery
		installPlugins(
			new AppSearchPlugin(),
			new DirSearchPlugin(),
			new ScreenRotator()
		);

		// listen to other application instance launches
		try {
			appCommunicator.start();
			// process app parameters of newly started instance
			appCommunicator.onNewInstanceHandlers.add(parameterProcessor::process);
		} catch (RemoteException e) {
			LOGGER.warn("App instance communicator failed to start.", e);
		}

		// start global shortcuts
		Action.startActionListening();
		Action.loadCommandActions();
	}

	// TODO: move out to plugin handling class
	public void installPlugins(Plugin... plugins) {
		stream(plugins).forEach(this::installPlugins);
	}

	/**
	 * Plugin is like Service, but user or developer never uses it directly, hence allows for simpler API. Its basically
	 * a Runnable which can be started and stopped.
	 *
	 * @implNote In the future Plugin and Service may extend a common supertype and share lifecycle management.
	 */
	public void installPlugins(Plugin plugin) {
		String name = "Enable";
		String group = Plugin.CONFIG_GROUP + "." + plugin.getName();
		String info = "Enable/disable this plugin";
		V<Boolean> starter = new V<>(false, plugin::activate);
		configuration.collect(new PropertyConfig<>(Boolean.class, name, name, starter, group, info, IsConfig.EditMode.USER));
		configuration.collect(plugin);
		Action.installActions(plugin);

		isValidatedDirectory(plugin.getLocation());
		isValidatedDirectory(plugin.getUserLocation());
		// TODO: handle error
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
				// enumerate instances
				instances.addInstance(SearchUriBuilder.class,
					DuckDuckGoQBuilder.INSTANCE,
					DuckDuckGoImageQBuilder.INSTANCE,
					WikipediaQBuilder.INSTANCE,
					BingImageSearchQBuilder.INSTANCE,
					GoogleImageQBuilder.INSTANCE
				);
				instances.addInstance(RatingCellFactory.class,
					BarRatingCellFactory.INSTANCE,
					HyphenRatingCellFactory.INSTANCE,
					TextStarRatingCellFactory.INSTANCE,
					NumberRatingCellFactory.INSTANCE,
					RatingRatingCellFactory.INSTANCE
				);

				widgetManager.initialize();

				// services must be created before Configuration
				services.addService(new TrayService());
				services.addService(new Notifier());
				services.addService(new PlaycountIncrementer());
				services.addService(new ClickEffect());

				// install actions
				// TODO: unify services & managers ?
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

				// gather configs
				configuration.rawAdd(FILE_SETTINGS);
				configuration.collectStatic();
				configuration.collect(Action.getActions());
				services.forEach(configuration::collect);
				configuration.collect(this, windowManager, guide, actionPane);

				// deserialize values (some configs need to apply it, will do when ready)
				configuration.rawSet();

				// initializing, the order is important
				Player.initialize();

				List<String> ps = fetchParameters();
				normalLoad &= ps.stream().noneMatch(s -> s.endsWith(".fxwl") || widgetManager.factories.get(s)!=null);

				// use for faster widget testing
				// Set project to run with application parameters and use widget name
				// This will only load the widget and start app faster
				// normalLoad = false;

				// load windows, layouts, widgets
				// we must apply skin before we load graphics, solely because if skin defines custom
				// Control Skins, it will only have effect when set before control is created
				// and yes, this means reapplying different skin will have no effect in this regard...
				configuration.getFields(f -> f.getGroup().equals("Gui") && f.getGuiName().equals("Skin")).get(0).applyValue();
				windowManager.deserialize(normalLoad);

				db.start();

				isInitialized = true;

			})
			.ifError(e -> {
				LOGGER.info("Application failed to start", e);
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

				// TODO: implement properly - load with skin
				// runNew(() -> {
				//     Image image = new Image(new File("cursor.png").getAbsoluteFile().toURI().toString());
				//     ImageCursor c = new ImageCursor(image,3,3);
				//     runLater(() -> window.getStage().getScene().setCursor(c));
				// });

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

	public <S extends Service> void use(Class<S> type, Consumer<S> action) {
		services.getService(type).filter(Service::isRunning).ifPresent(action);
	}

/******************************************************************************/

	private void changeLogBackLoggerAppenderLevel(String appenderName, Level level) {
		Optional.ofNullable((ch.qos.logback.classic.Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME))
				.map(logger -> logger.getAppender(appenderName))
				.map(FilterAttachable::getCopyOfAttachedFiltersList)
				.flatMap(filters -> filters.stream().findFirst())
				.filter(filter -> filter instanceof ch.qos.logback.classic.filter.ThresholdFilter)
				.map(filter -> ((ch.qos.logback.classic.filter.ThresholdFilter)filter))
				.ifPresent(filter -> filter.setLevel(level.toString()));
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

	private void configureLogging() {
		// disable java.util.logging logging
		// Im not sure this is really wise, but otherwise we get a lot of unwanted log content in console from
		// libraries
		LogManager.getLogManager().reset();

		// configure slf4 logging
		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		try {
			JoranConfigurator jc = new JoranConfigurator();
			jc.setContext(lc);
			lc.reset();
			lc.putProperty("LOG_DIR", DIR_LOG.getPath());
			// override default configuration
			jc.doConfigure(FILE_LOG_CONFIG);
		} catch (JoranException ex) {
			LOGGER.error(ex.getMessage());
		}
		StatusPrinter.printInCaseOfErrorsOrWarnings(lc);

		// log uncaught thread termination exceptions
		Thread.setDefaultUncaughtExceptionHandler((thread,ex) -> LOGGER.error("Uncaught exception", ex));
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
			.toList();
	}

}