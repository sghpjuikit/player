package sp.it.pl.main

import ch.qos.logback.classic.Level
import com.sun.tools.attach.VirtualMachine
import de.jensd.fx.glyphs.materialicons.MaterialIcon
import javafx.application.Application
import javafx.application.Platform
import javafx.scene.image.Image
import javafx.stage.Stage
import mu.KLogging
import org.atteo.evo.inflector.English.plural
import org.reactfx.EventSource
import sp.it.pl.audio.Item
import sp.it.pl.audio.Player
import sp.it.pl.audio.playlist.PlaylistItem
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.core.CoreInstances
import sp.it.pl.core.CoreLogging
import sp.it.pl.core.CoreMouse
import sp.it.pl.core.CoreSerializer
import sp.it.pl.core.CoreSerializerXml
import sp.it.pl.gui.Gui
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.objects.popover.PopOver
import sp.it.pl.gui.objects.tablecell.RatingCellFactory
import sp.it.pl.gui.objects.tablecell.RatingRatingCellFactory
import sp.it.pl.gui.objects.textfield.autocomplete.ConfigSearch
import sp.it.pl.gui.objects.window.stage.WindowManager
import sp.it.pl.gui.pane.ActionPane
import sp.it.pl.gui.pane.InfoPane
import sp.it.pl.gui.pane.MessagePane
import sp.it.pl.gui.pane.ShortcutPane
import sp.it.pl.layout.Component
import sp.it.pl.layout.widget.WidgetManager
import sp.it.pl.layout.widget.WidgetManager.WidgetSource.ANY
import sp.it.pl.layout.widget.WidgetManager.WidgetSource.NO_LAYOUT
import sp.it.pl.layout.widget.feature.ImageDisplayFeature
import sp.it.pl.layout.widget.feature.PlaylistFeature
import sp.it.pl.plugin.AppSearchPlugin
import sp.it.pl.plugin.DirSearchPlugin
import sp.it.pl.plugin.PluginManager
import sp.it.pl.plugin.ScreenRotator
import sp.it.pl.service.ClickEffect
import sp.it.pl.service.ServiceManager
import sp.it.pl.service.database.Db
import sp.it.pl.service.notif.Notifier
import sp.it.pl.service.playcount.PlaycountIncrementer
import sp.it.pl.service.tray.TrayService
import sp.it.pl.util.access.V
import sp.it.pl.util.access.VarEnum
import sp.it.pl.util.access.fieldvalue.ColumnField
import sp.it.pl.util.access.fieldvalue.FileField
import sp.it.pl.util.action.Action
import sp.it.pl.util.action.IsAction
import sp.it.pl.util.async.runAfter
import sp.it.pl.util.conf.Configurable
import sp.it.pl.util.conf.Configuration
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.IsConfigurable
import sp.it.pl.util.file.AudioFileFormat
import sp.it.pl.util.file.AudioFileFormat.Use
import sp.it.pl.util.file.ImageFileFormat
import sp.it.pl.util.file.Util
import sp.it.pl.util.file.mimetype.MimeTypes
import sp.it.pl.util.functional.Functors.Ƒ0
import sp.it.pl.util.functional.Try
import sp.it.pl.util.functional.orNull
import sp.it.pl.util.functional.seqOf
import sp.it.pl.util.graphics.image.getImageDim
import sp.it.pl.util.math.millis
import sp.it.pl.util.system.SystemOutListener
import sp.it.pl.util.type.ClassName
import sp.it.pl.util.type.InstanceInfo
import sp.it.pl.util.type.InstanceName
import sp.it.pl.util.type.ObjectFieldMap
import sp.it.pl.util.units.FileSize
import sp.it.pl.util.validation.Constraint.MinMax
import java.io.File
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.function.Consumer
import kotlin.test.fail

private typealias F = JvmField
private typealias C = IsConfig

/** Application. Represents the program. */
@Suppress("unused")
@IsConfigurable("General")
class App: Application(), Configurable<Any> {

    /** Name of this application. */
    @F val name = "PlayerFX"
    /** Application code encoding. Useful for compilation during runtime. */
    @F val encoding = StandardCharsets.UTF_8!!
    /** Uri for github website for project of this application. */
    @F val uriGithub = URI.create("https://www.github.com/sghpjuikit/player/")!!
    /** Absolute file of location of this app. Working directory of the project. new File("").getAbsoluteFile(). */
    @F val DIR_APP = File("").absoluteFile!!
    /** Temporary directory of the os. */
    @F val DIR_TEMP = File(System.getProperty("java.io.tmpdir"))
    /** Home directory of the os. */
    @F val DIR_HOME = File(System.getProperty("user.home"))
    /** Directory for application logging. */
    @F val DIR_LOG = File(DIR_APP, "log")
    /** File for application logging configuration. */
    @F val FILE_LOG_CONFIG = File(DIR_LOG, "log_configuration.xml")
    /** Directory containing widgets - source files, class files and widget's resources. */
    @F val DIR_WIDGETS = File(DIR_APP, "widgets")
    /** Directory containing skins. */
    @F val DIR_SKINS = File(DIR_APP, "skins")
    /** Directory containing user data created by application usage, such as customizations, song library, etc. */
    @F val DIR_USERDATA = File(DIR_APP, "user")
    /** Directory containing library database. */
    @F val DIR_LIBRARY = File(DIR_USERDATA, "library")
    /** Directory containing user gui state. */
    @F val DIR_LAYOUTS = File(DIR_USERDATA, "layouts")
    /** Directory containing application resources. */
    @F val DIR_RESOURCES = File(DIR_APP, "resources")
    /** File for application configuration. */
    @F val FILE_SETTINGS = File(DIR_USERDATA, "application.properties")

    // cores (always active, mostly singletons)
    @F val logging = CoreLogging(FILE_LOG_CONFIG, DIR_LOG)
    @F val serializerXml = CoreSerializerXml()
    @F val serializer = CoreSerializer
    @F val instances = CoreInstances
    @F val mimeTypes = MimeTypes
    @F val className = ClassName()
    @F val instanceName = InstanceName()
    @F val instanceInfo = InstanceInfo()
    @F val classFields = ObjectFieldMap()
    @F val mouse = CoreMouse

    // app stuff
    /** Application argument handler. */
    @F val parameterProcessor = AppParameterProcessor()
    /** Various actions for the application */
    @F val actions = AppActions()
    /** Global [String] event source/stream. Usage: simply push an event or observe. */
    @F val actionStream = EventSource<String>()
    /** Allows sending and receiving [java.lang.String] messages to and from other instances of this application. */
    @F val appCommunicator = AppInstanceComm()
    /** Configurable state, i.e., the settings of the application. */
    @F val configuration = Configuration()
    /** Observable [System.out] */
    @F val systemout = SystemOutListener()

    // ui
    @F val actionPane = ActionPane(className, instanceName, instanceInfo)
    @F val actionAppPane = ActionPane(className, instanceName, instanceInfo)
    @F val messagePane = MessagePane()
    @F val shortcutPane = ShortcutPane()
    @F val infoPane = InfoPane()
    @F val guide = Guide()
    @F val search = Search()

    @C(name = "Rating control", info = "The style of the graphics of the rating control.")
    @F val ratingCell = VarEnum.ofInstances<RatingCellFactory>(RatingRatingCellFactory, instances)

    @C(name = "Rating icon amount", info = "Number of icons in rating control.") @MinMax(min = 0.0, max = 10.0)
    @F val maxRating = V(5)

    @C(name = "Rating allow partial", info = "Allow partial values for rating.")
    @F val partialRating = V(true)

    @C(name = "Rating editable", info = "Allow change of rating. Defaults to application settings")
    @F val allowRatingChange = V(true)

    @C(name = "Enabled", group = "Taskbar", info = "Show taskbar icon. Disabling taskbar will also disable ALT+TAB functionality.")
    @F val taskbarEnabled = V(true)

    @C(info = "Preferred text when no tag value for field. This value can be overridden.")
    @F var textNoVal = "<none>"

    @C(info = "Preferred text when multiple tag values per field. This value can be overridden.")
    @F var textManyVal = "<multi>"

    @C(info = "Update frequency in Hz for performance-heavy animations.") @MinMax(min = 10.0, max = 60.0)
    @F var animationFps = 60.0

    @C(name = "Level (console)", group = "Logging", info = "Logging level for logging to console")
    @F val logLevelConsole = VarEnum.ofSequence(Level.DEBUG,
            { seqOf(Level.ALL, Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.OFF) },
            { logging.changeLogBackLoggerAppenderLevel("STDOUT", it) }
    )

    @C(name = "Level (file)", group = "Logging", info = "Logging level for logging to file")
    @F val logLevelFile = VarEnum.ofSequence(Level.WARN,
            { seqOf(Level.ALL, Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.OFF) },
            { logging.changeLogBackLoggerAppenderLevel("FILE", it) }
    )

    @C(name = "Normal mode", info = "Whether application loads into previous/default state or no state at all.")
    @F var normalLoad = true
    private var isInitialized = false
    private var closedPrematurely = false

    /** Manages persistence and in-memory storage. */
    @F val db = Db()
    /** Manages windows. */
    @F val windowManager = WindowManager()
    /** Manages widgets. */
    @F val widgetManager = WidgetManager(windowManager, { messagePane.show(it) })
    /** Manages services. */
    @F val services = ServiceManager()
    /** Manages services. */
    @F val plugins = PluginManager(configuration, { messagePane.show(it) })

    init {
        AppUtil.APP = takeIf { AppUtil.APP==null } ?: fail("Multiple application instances disallowed")
    }

    override fun init() {
        logging.init()

        // Forbid multiple application instances, instead notify the 1st instance of 2nd (this one)
        // trying to run and this instance's run parameters and close prematurely
        if (getInstances()>1) {
            logger().info { "App will close prematurely: Multiple app instances detected" }
            appCommunicator.fireNewInstanceEvent(fetchParameters())
            closedPrematurely = true
            return
        }

        // add optional object fields
        classFields.add(PlaylistItem::class.java, PlaylistItem.Field.FIELDS)
        classFields.add(Metadata::class.java, Metadata.Field.FIELDS)
        classFields.add(MetadataGroup::class.java, MetadataGroup.Field.FIELDS)
        classFields.add(Any::class.java, ColumnField.FIELDS)
        classFields.add(File::class.java, FileField.FIELDS)

        // add optional object class -> string converters
        className.addNoLookup(Void::class.java, "Nothing")
        className.add(String::class.java, "Text")
        className.add(App::class.java, "Application")
        className.add(Item::class.java, "Song")
        className.add(PlaylistItem::class.java, "Playlist Song")
        className.add(Metadata::class.java, "Library Song")
        className.add(MetadataGroup::class.java, "Song Group")
        className.add(List::class.java, "List")

        // add optional object instance -> string converters
        instanceName.add(Void::class.java) { "<none>" }
        instanceName.add(App::class.java) { "This application" }
        instanceName.add(Item::class.java, { it.getPathAsString() })
        instanceName.add(PlaylistItem::class.java, { it.getTitle() })
        instanceName.add(Metadata::class.java, { it.getTitleOrEmpty() })
        instanceName.add(MetadataGroup::class.java) { it.getValueS("<none>") }
        instanceName.add(Component::class.java, { it.name })
        instanceName.add(File::class.java, { it.path })
        instanceName.add(Collection::class.java) {
            val eType = sp.it.pl.util.type.Util.getGenericPropertyType(it.javaClass)
            val eName = if (eType==null || eType==Any::class.java) "Item" else className.get(eType)
            it.size.toString()+" "+plural(eName, it.size)
        }

        // add optional object instance -> info string converters
        instanceInfo.add(Void::class.java) { _, _ -> }
        instanceInfo.add(String::class.java) { s, map -> map.put("Length", Integer.toString(s?.length ?: 0)) }
        instanceInfo.add(File::class.java) { f, map ->
            val suffix = Util.getSuffix(f)
            val fs = FileSize(f)
            val fsBytes = if (fs.isUnknown()) "" else " ("+String.format("%,d ", fs.inBytes()).replace(',', ' ')+"bytes)"
            map.put("Size", fs.toString()+fsBytes)
            map.put("Format", if (suffix.isEmpty()) "n/a" else suffix)

            val iff = ImageFileFormat.of(f.toURI())
            if (iff.isSupported) {
                val res = getImageDim(f).map { id -> id.width.toString()+" x "+id.height }.getOr("n/a")
                map.put("Resolution", res)
            }
        }
        instanceInfo.add(App::class.java) { v, map -> map.put("Name", v.name) }
        instanceInfo.add(Metadata::class.java) { m, map ->
            Metadata.Field.FIELDS.asSequence()
                    .filter { it.isTypeStringRepresentable() && !it.isFieldEmpty(m) }
                    .forEach { map.put(it.name(), it.getOfS(m, "<none>")) }
        }
        instanceInfo.add(PlaylistItem::class.java) { p, map ->
            PlaylistItem.Field.FIELDS.asSequence()
                    .filter { it.isTypeStringRepresentable() }
                    .forEach { map.put(it.name(), it.getOfS(p, "<none>")) }
        }

        // init cores
        serializer.init()
        serializerXml.init()
        instances.init()
        mouse.init()

        // init app stuff
        parameterProcessor.initForApp()
        search.initForApp()
        plugins.initForApp()
        appCommunicator.initForApp()

        // start parts that can be started from non application fx thread
        Action.startActionListening()
        Action.loadCommandActions()
        appCommunicator.start()
    }

    override fun start(primaryStage: Stage) {
        if (closedPrematurely) {
            logger().info { "Application closing prematurely" }
            close()
            return
        }

        isInitialized = Try.tryCatchAll {
            // services must be created before Configuration
            services.addService(TrayService())
            services.addService(Notifier())
            services.addService(PlaycountIncrementer())
            services.addService(ClickEffect())

            // install actions
            Action.installActions(
                    this,
                    actions,
                    windowManager,
                    guide,
                    services.getAllServices()
            )

            // ui
            actionPane.initActionPane()
            actionAppPane.initAppActionPane()

            widgetManager.init()
            db.init()

            // gather configs
            configuration.rawAdd(FILE_SETTINGS)
            configuration.collectStatic()
            configuration.collect(Action.getActions())
            services.getAllServices().forEach { configuration.collect(it) }
            configuration.collect(this, windowManager, guide)
            configuration.collect(Configurable.configsFromFieldsOf("actionChooserView", "View.Action Chooser", actionPane))
            configuration.collect(Configurable.configsFromFieldsOf("actionChooserAppView", "View.Action App Chooser", actionAppPane))
            configuration.collect(Configurable.configsFromFieldsOf("shortcutViewer", "View.Shortcut Viewer", shortcutPane))
            configuration.collect(Configurable.configsFromFieldsOf("infoView", "View.System info", infoPane))

            // deserialize values (some configs need to apply it, will do when ready)
            configuration.rawSet()

            // initializing, the order is important
            Player.initialize()

            val ps = fetchParameters()
            normalLoad = normalLoad && ps.none { it.endsWith(".fxwl") || widgetManager.factories.get(it)!=null }

            // load windows, layouts, widgets
            // we must apply skin before we load graphics, solely because if skin defines custom
            // Control Skins, it will only have effect when set before control is created
            configuration.getFields { it.group=="Gui" && it.guiName=="Skin" }.findFirst().orNull()!!.applyValue()
            windowManager.deserialize(normalLoad)

            isInitialized = true

        }
                .ifError {
                    logger().error(it) { "Application failed to start" }
                    messagePane.show("Application did not start successfully.")
                }
                .ifOk {
                    // initialization is complete -> apply all settings
                    configuration.fields.forEach { it.applyValue() }

                    // initialize non critical parts
                    if (normalLoad) Player.loadLast()

                    // show guide
                    if (guide.first_time.get()) runAfter(millis(3000), { guide.start() })

                    // process app parameters passed when app started
                    parameterProcessor.process(fetchParameters())
                }
                .isOk
    }

    /** Starts this application normally if not yet started that way, otherwise has no effect. */
    @IsAction(name = "Start app normally", desc = "Loads last application state if not yet loaded.")
    fun startNormally() {
        if (!normalLoad) {
            normalLoad = true
            windowManager.deserialize(true)
            Player.loadLast()
        }
    }

    @Deprecated("Called automatically")
    override fun stop() {
        store()
        dispose()

        // cores
        logging.dispose()
        serializerXml.dispose()
        serializer.dispose()
        instances.dispose()
        mouse.dispose()
    }

    private fun store() {
        if (isInitialized) {
            if (normalLoad) Player.state.serialize()
            if (normalLoad) windowManager.serialize()
            configuration.save(name, FILE_SETTINGS)
            services.getAllServices()
                    .filter { it.isRunning() }
                    .forEach { it.stop() }
        }
    }

    private fun dispose() {
        db.stop()
        Action.stopActionListening()
        appCommunicator.stop()
    }

    /** Close this app normally. Invokes [stop] as a result.  */
    @IsAction(name = "Close app", desc = "Closes this application.")
    fun close() {
        PopOver.active_popups.toList().forEach { it.hideImmediatelly() }    // javaFX bug - must close popups before windows
        windowManager.windows.forEach { it.hide() }     // close app in bgr (assumes we don't restore window visibility state!)
        Platform.exit()
    }

    /** Close this app abnormally, so next time this app does not start normally. Invokes [close] and then [stop]. */
    @IsAction(name = "Close app abnormally", desc = "Next start up will not restore current state, but it can be done manually later.")
    fun closeAbnormally() {
        normalLoad = false
        close()
    }

    fun fetchParameters(): List<String> = parameters?.let { it.raw+it.unnamed+it.named.values } ?: listOf()

    /** @return number of instances of this application (including this one) running at this moment */
    fun getInstances(): Int = VirtualMachine.list().count { App::class.java.name==it.displayName() }

    /** @return image of the icon of the application */
    fun getIcon(): Image = Image(File("icon512.png").toURI().toString())

    /** @return images of the icon of the application in all possible sizes */
    fun getIcons(): List<Image> = seqOf(16, 24, 32, 48, 128, 256, 512)
            .map { File("icon$it.png").toURI().toString() }
            .map { Image(it) }
            .toList()

    companion object: KLogging()

    private fun AppParameterProcessor.initForApp() {
        addFileProcessor(
                { AudioFileFormat.isSupported(it, Use.APP) },
                { fs -> widgetManager.use(PlaylistFeature::class.java, ANY) { it.playlist.addFiles(fs) } }
        )
        addFileProcessor(
                { Util.isValidSkinFile(it) },
                { Gui.setSkin(it[0]) }
        )
        addFileProcessor(
                { ImageFileFormat.isSupported(it) },
                { fs -> widgetManager.use(ImageDisplayFeature::class.java, NO_LAYOUT) { it.showImages(fs) } }
        )
        addFileProcessor(
                { it.path.endsWith(".fxwl") },
                { it.forEach { windowManager.launchComponent(it) } }
        )
        addStringProcessor(
                { s -> widgetManager.getFactories().anyMatch { it.nameGui()==s || it.name()==s } },
                { it.forEach { windowManager.launchComponent(it) } }
        )
    }

    private fun PluginManager.initForApp() {
        installPlugins(
                AppSearchPlugin(),
                DirSearchPlugin(),
                ScreenRotator()
        )
    }

    private fun Search.initForApp() {
        sources += listOf(
                { configuration.fields.stream().map { ConfigSearch.Entry.of(it) } },
                { widgetManager.componentFactories.map { ConfigSearch.Entry.of(it) } },
                { Gui.skin.streamValues().map { ConfigSearch.Entry.of(Ƒ0 { "Open skin: $it" }, { Gui.skin.setNapplyValue(it) }, Ƒ0 { Icon(MaterialIcon.BRUSH) }) } }
        )
    }

    private fun AppInstanceComm.initForApp() {
        onNewInstanceHandlers += Consumer { parameterProcessor.process(it) }
    }
}