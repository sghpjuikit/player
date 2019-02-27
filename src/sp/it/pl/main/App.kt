package sp.it.pl.main

import ch.qos.logback.classic.Level
import com.sun.tools.attach.VirtualMachine
import javafx.application.Application
import javafx.application.Platform
import javafx.scene.image.Image
import javafx.stage.Stage
import mu.KLogging
import org.atteo.evo.inflector.English.plural
import org.reactfx.EventSource
import sp.it.pl.audio.Player
import sp.it.pl.audio.Song
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.playlist.PlaylistSong
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.core.CoreConverter
import sp.it.pl.core.CoreFunctors
import sp.it.pl.core.CoreImageIO
import sp.it.pl.core.CoreInstances
import sp.it.pl.core.CoreLogging
import sp.it.pl.core.CoreMenus
import sp.it.pl.core.CoreMouse
import sp.it.pl.core.CoreSerializer
import sp.it.pl.core.CoreSerializerXml
import sp.it.pl.gui.UiManager
import sp.it.pl.gui.objects.autocomplete.ConfigSearch.Entry
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.objects.image.Thumbnail
import sp.it.pl.gui.objects.search.SearchAutoCancelable
import sp.it.pl.gui.objects.tablecell.RatingCellFactory
import sp.it.pl.gui.objects.tablecell.RatingRatingCellFactory
import sp.it.pl.gui.objects.window.stage.WindowManager
import sp.it.pl.gui.pane.ActionPane
import sp.it.pl.gui.pane.InfoPane
import sp.it.pl.gui.pane.MessagePane
import sp.it.pl.gui.pane.ShortcutPane
import sp.it.pl.gui.pane.initApp
import sp.it.pl.layout.Component
import sp.it.pl.layout.container.Container
import sp.it.pl.layout.container.switchcontainer.SwitchContainer
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.WidgetManager
import sp.it.pl.layout.widget.WidgetUse.ANY
import sp.it.pl.layout.widget.feature.Feature
import sp.it.pl.layout.widget.feature.PlaylistFeature
import sp.it.pl.plugin.Plugin
import sp.it.pl.plugin.PluginManager
import sp.it.pl.plugin.appsearch.AppSearchPlugin
import sp.it.pl.plugin.dirsearch.DirSearchPlugin
import sp.it.pl.plugin.library.LibraryWatcher
import sp.it.pl.plugin.screenrotator.ScreenRotator
import sp.it.pl.plugin.waifu2k.Waifu2kPlugin
import sp.it.pl.service.Service
import sp.it.pl.service.ServiceManager
import sp.it.pl.service.database.SongDb
import sp.it.pl.service.notif.Notifier
import sp.it.pl.service.playcount.PlaycountIncrementer
import sp.it.pl.service.tray.TrayService
import sp.it.pl.util.access.VarEnum
import sp.it.pl.util.access.fieldvalue.ColumnField
import sp.it.pl.util.access.fieldvalue.FileField
import sp.it.pl.util.access.initSync
import sp.it.pl.util.action.Action
import sp.it.pl.util.action.ActionManager
import sp.it.pl.util.action.IsAction
import sp.it.pl.util.async.runLater
import sp.it.pl.util.conf.Configurable
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.IsConfigurable
import sp.it.pl.util.conf.MainConfiguration
import sp.it.pl.util.conf.between
import sp.it.pl.util.conf.c
import sp.it.pl.util.conf.cr
import sp.it.pl.util.conf.cv
import sp.it.pl.util.conf.preserveOrder
import sp.it.pl.util.dev.fail
import sp.it.pl.util.file.AudioFileFormat
import sp.it.pl.util.file.AudioFileFormat.Use
import sp.it.pl.util.file.FileType
import sp.it.pl.util.file.ImageFileFormat
import sp.it.pl.util.file.Util.isValidatedDirectory
import sp.it.pl.util.file.childOf
import sp.it.pl.util.file.div
import sp.it.pl.util.file.hasExtension
import sp.it.pl.util.file.mimetype.MimeTypes
import sp.it.pl.util.functional.Try
import sp.it.pl.util.functional.seqOf
import sp.it.pl.util.graphics.image.getImageDim
import sp.it.pl.util.reactive.Handler0
import sp.it.pl.util.stacktraceAsString
import sp.it.pl.util.system.SystemOutListener
import sp.it.pl.util.type.ClassName
import sp.it.pl.util.type.InstanceInfo
import sp.it.pl.util.type.InstanceName
import sp.it.pl.util.type.ObjectFieldMap
import sp.it.pl.util.type.Util.getGenericPropertyType
import sp.it.pl.util.units.FileSize
import java.io.File
import java.lang.management.ManagementFactory
import java.net.URI
import java.net.URLConnection
import java.nio.charset.StandardCharsets
import java.util.function.Consumer

lateinit var APP: App

fun main(args: Array<String>) {
    // Relocate temp & home under working directory
    // It is our principle to leave no trace of ever running on the system
    // User can also better see what the application is doing
    val tmp = File("").absoluteFile/"user"/"tmp"
    if (!isValidatedDirectory(tmp)) fail { "Invalid tmp directory" }
    System.setProperty("java.io.tmpdir", tmp.absolutePath)
    System.setProperty("user.home", tmp.absolutePath)

    // Disable url caching, which may cause jar files being held in memory
    URLConnection.setDefaultUseCaches("file", false)

    Application.launch(App::class.java, *args)
}

private typealias F = JvmField
private typealias C = IsConfig

/** Application. Represents the program. */
@Suppress("unused")
@IsConfigurable("General")
class App: Application(), Configurable<Any> {

    init {
        APP = this.takeUnless { ::APP.isInitialized } ?: fail { "Multiple application instances disallowed" }
    }

    private var closedPrematurely = false
    var isInitialized: Try<Void, Throwable> = Try.error(Exception("Initialization has not run yet"))
        private set

    /** Name of this application. */
    @F val name = "PlayerFX"
    /** Application code encoding. Useful for compilation during runtime. */
    @F val encoding = StandardCharsets.UTF_8!!
    /** Uri for github website for project of this application. */
    @F val uriGithub = URI.create("https://www.github.com/sghpjuikit/player/")!!
    /** Absolute file of location of this app. Working directory of the project. new File("").getAbsoluteFile(). */
    @F val DIR_APP = File("").absoluteFile!!
    /** Temporary directory of the os. */
    @F val DIR_TEMP = File(System.getProperty("java.io.tmpdir")).initForApp()
    /** Home directory of the os. */
    @F val DIR_HOME = File(System.getProperty("user.home")).initForApp()
    /** Directory containing widgets - source files, class files and widget's resources. */
    @F val DIR_WIDGETS = DIR_APP.childOf("widgets").initForApp()
    /** Directory containing application resources. */
    @F val DIR_RESOURCES = DIR_APP.childOf("resources").initForApp()
    /** Directory containing skins. */
    @F val DIR_SKINS = DIR_APP.childOf("skins").initForApp()
    /** Directory containing user data created by application usage, such as customizations, song library, etc. */
    @F val DIR_USERDATA = DIR_APP.childOf("user").initForApp()
    /** Directory containing library database. */
    @F val DIR_LIBRARY = DIR_USERDATA.childOf("library").initForApp()
    /** Directory containing user gui state. */
    @F val DIR_LAYOUTS = DIR_USERDATA.childOf("layouts").initForApp()
    /** Directory for application logging. */
    @F val DIR_LOG = DIR_USERDATA.childOf("log").initForApp()
    /** File for application configuration. */
    @F val FILE_SETTINGS = DIR_USERDATA.childOf("application.properties")

    // cores (always active, mostly singletons)
    @F val logging = CoreLogging(DIR_RESOURCES.childOf("log_configuration.xml"), DIR_LOG)
    @F val imageIo = CoreImageIO(DIR_TEMP.childOf("imageio"))
    @F val converter = CoreConverter().apply { init() }
    @F val configuration = MainConfiguration.apply { rawAdd(FILE_SETTINGS) }
    @F val serializerXml = CoreSerializerXml()
    @F val serializer = CoreSerializer
    @F val instances = CoreInstances
    @F val mimeTypes = MimeTypes
    @F val className = ClassName()
    @F val instanceName = InstanceName()
    @F val instanceInfo = InstanceInfo()
    @F val classFields = ObjectFieldMap()
    @F val contextMenus = CoreMenus
    @F val mouse = CoreMouse
    @F val functors = CoreFunctors

    @C(name = "Level (console)", group = "Logging", info = "Logging level for logging to console")
    val logLevelConsole by cv(Level.INFO) {
        VarEnum.ofSequence(it) { logLevels }.initSync { logging.changeLogBackLoggerAppenderLevel("STDOUT", it) }
    }.preserveOrder()

    @C(name = "Level (file)", group = "Logging", info = "Logging level for logging to file")
    val logLevelFile by cv(Level.WARN) {
        VarEnum.ofSequence(it) { logLevels }.initSync { logging.changeLogBackLoggerAppenderLevel("FILE", it) }
    }.preserveOrder()

    @C(name = "Rating control", info = "The style of the graphics of the rating control.")
    val ratingCell by cv(RatingRatingCellFactory as RatingCellFactory) { VarEnum.ofInstances(it, instances) }

    @C(name = "Rating icon amount", info = "Number of icons in rating control.")
    val maxRating by cv(5).between(0, 10)

    @C(name = "Rating allow partial", info = "Allow partial values for rating.")
    val partialRating by cv(true)

    @C(name = "Text for no value", info = "Preferred text when no tag value for field. This value can be overridden.")
    var textNoVal by c("<none>")

    @C(name = "Text for multi value", info = "Preferred text when multiple tag values per field. This value can be overridden.")
    var textManyVal by c("<multi>")

    @C(name = "Animation FPS", info = "Update frequency in Hz for performance-heavy animations.")
    var animationFps by c(60.0).between(10.0, 60.0)

    @C(name = "Normal mode", info = "Whether application loads into previous/default state or no state at all.")
    var normalLoad by c(true)

    @C(name = "Developer mode", info = "Unlock developer features.")
    var developerMode by c(false)

    @C(group = "Settings", name = "Settings use default", info = "Set all settings to default values.")
    val actionSettingsReset by cr { configuration.toDefault() }

    @C(group = "Settings", name = "Settings save", info = "Save all settings. Also invoked automatically when application closes")
    val actionSettingsSave by cr { configuration.save(name, FILE_SETTINGS) }

    // app stuff
    /** Application argument handler. */
    @F val parameterProcessor = AppParameterProcessor()
    /** Various actions for the application */
    @F val actions = AppActions()
    /** Global event bus. Usage: simply push an event or observe. Use of Any() event constants and === is advised. */
    @F val actionStream = EventSource<Any>()
    /** Allows sending and receiving [java.lang.String] messages to and from other instances of this application. */
    @F val appCommunicator = AppInstanceComm()
    /** Observable [System.out]. */
    @F val systemout = SystemOutListener()
    /** Called just after this application is started (successfully) and fully initialized. Runs at most once. */
    val onStarted = Handler0()
    /** Called just before this application is stopped when it is fully still running. Runs at most once. */
    val onStopping = Handler0()

    // ui
    @F val ui = UiManager(DIR_SKINS)
    @F val actionPane = ActionPane("View.Action Chooser", className, instanceName, instanceInfo).initApp()
    @F val shortcutPane = ShortcutPane("View.Shortcut Viewer").initApp()
    @F val messagePane = MessagePane().initApp()
    @F val infoPane = InfoPane("View.System").initApp()
    @F val guide = Guide(actionStream)
    @F val search = Search()

    /** Manages persistence and in-memory storage. */
    @F val db = SongDb()
    /** Manages windows. */
    @F val windowManager = WindowManager()
    /** Manages widgets. */
    @F val widgetManager = WidgetManager(windowManager, messagePane::show)
    /** Manages services. */
    @F val services = ServiceManager()
    /** Manages services. */
    @F val plugins = PluginManager()

    override fun init() {
        logging.init()

        // Forbid multiple application instances, instead notify the 1st instance of 2nd (this one)
        // trying to run and this instance's run parameters and close prematurely
        if (getInstances()>1) {
            logger.info { "App will close prematurely: Multiple app instances detected" }
            appCommunicator.fireNewInstanceEvent(fetchArguments())
            closedPrematurely = true
            return
        }

        logger.info { "JVM Args: ${fetchVMArguments()}" }

        // add optional object fields
        classFields.add(PlaylistSong::class.java, PlaylistSong.Field.FIELDS)
        classFields.add(Metadata::class.java, Metadata.Field.FIELDS)
        classFields.add(MetadataGroup::class.java, MetadataGroup.Field.FIELDS)
        classFields.add(Any::class.java, ColumnField.FIELDS)
        classFields.add(File::class.java, FileField.FIELDS)

        // add optional object class -> string converters
        className.addNoLookup(Void::class.java, "Nothing")
        className.add(String::class.java, "Text")
        className.add(File::class.java, "File")
        className.add(App::class.java, "Application")
        className.add(Song::class.java, "Song")
        className.add(PlaylistSong::class.java, "Playlist Song")
        className.add(Metadata::class.java, "Library Song")
        className.add(MetadataGroup::class.java, "Song Group")
        className.add(Service::class.java, "Service")
        className.add(Plugin::class.java, "Plugin")
        className.add(Widget::class.java, "Widget")
        className.add(Container::class.java, "Container")
        className.add(Feature::class.java, "Feature")
        className.add(List::class.java, "List")

        // add optional object instance -> string converters
        instanceName.add(Void::class.java) { "<none>" }
        instanceName.add(File::class.java, { it.path })
        instanceName.add(App::class.java) { "This application" }
        instanceName.add(Song::class.java, { it.getPathAsString() })
        instanceName.add(PlaylistSong::class.java, { it.getTitle() })
        instanceName.add(Metadata::class.java, { it.getTitleOrEmpty() })
        instanceName.add(MetadataGroup::class.java) { it.getValueS("<none>") }
        instanceName.add(Service::class.java, { it.name })
        instanceName.add(Plugin::class.java, { it.name })
        instanceName.add(Component::class.java, { it.exportName })
        instanceName.add(Feature::class.java, { "Feature" })
        instanceName.add(Collection::class.java) {
            val eType = getGenericPropertyType(it.javaClass)
            val eName = if (eType==null || eType==Any::class.java) "Item" else className[eType]
            it.size.toString()+" "+plural(eName, it.size)
        }

        // add optional object instance -> info string converters
        instanceInfo.add(Void::class.java) { _, _ -> }
        instanceInfo.add(String::class.java) { s, map -> map["Length"] = Integer.toString(s?.length ?: 0) }
        instanceInfo.add(File::class.java) { f, map ->
            val type = FileType.of(f)
            map["File type"] = type.name

            if (type==FileType.FILE) {
                val fs = FileSize(f)
                map["Size"] = ""+fs+(if (fs.isKnown()) " (%,d bytes)".format(fs.inBytes()).replace(',', ' ') else "")
                map["Format"] = f.name.substringAfterLast('.', "<none>")
            }

            map[FileField.TIME_CREATED.name()] = FileField.TIME_CREATED.getOfS(f, "n/a")
            map[FileField.TIME_MODIFIED.name()] = FileField.TIME_MODIFIED.getOfS(f, "n/a")

            val iff = ImageFileFormat.of(f.toURI())
            if (iff.isSupported) {
                val res = getImageDim(f).map { "${it.width} x ${it.height}" }.getOr("n/a")
                map["Resolution"] = res
            }
        }
        instanceInfo.add(App::class.java) { v, map -> map["Name"] = v.name }
        instanceInfo.add(Component::class.java) { v, map -> map["Name"] = v.exportName }
        instanceInfo.add(Metadata::class.java) { m, map ->
            Metadata.Field.FIELDS.asSequence()
                    .filter { it.isTypeStringRepresentable() && !it.isFieldEmpty(m) }
                    .forEach { map[it.name()] = it.getOfS(m, "<none>") }
        }
        instanceInfo.add(PlaylistSong::class.java) { p, map ->
            PlaylistSong.Field.FIELDS.asSequence()
                    .filter { it.isTypeStringRepresentable() }
                    .forEach { map[it.name()] = it.getOfS(p, "<none>") }
        }
        instanceInfo.add(Feature::class.java) { f, map ->
            map["Name"] = f.name
            map["Description"] = f.description
        }

        // init cores
        serializer.init()
        serializerXml.init()
        imageIo.init()
        instances.init()
        contextMenus.init()
        mouse.init()
        functors.init()

        // init app stuff
        parameterProcessor.initForApp()
        search.initForApp()
        plugins.initForApp()
        appCommunicator.initForApp()

        // start parts that can be started from non application fx thread
        ActionManager.startActionListening()
        appCommunicator.start()
    }

    override fun start(primaryStage: Stage) {
        if (closedPrematurely) {
            logger.info { "Application closing prematurely" }
            close()
            return
        }

        isInitialized = Try.tryCatchAll {

            services += TrayService()
            services += Notifier()
            services += PlaycountIncrementer()

            // install actions
            Action.gatherActions(Player::class.java, null)
            Action.gatherActions(PlaylistManager::class.java, null)
            Action.gatherActions(Thumbnail::class.java, null)
            Action.gatherActions(SearchAutoCancelable::class.java, null)
            Action.gatherActions(SwitchContainer::class.java, null)
            Action.gatherActions(Action::class.java, null)
            Action.installActions(
                    this,
                    ui,
                    actions,
                    windowManager,
                    services.getAllServices().toList()
            )

            actionPane.initActionPane()
            widgetManager.init()
            db.init()

            // TODO: remove
            configuration.collect(windowManager)

            Player.initialize()

            normalLoad = normalLoad && fetchArguments().none { it.endsWith(".fxwl") || widgetManager.factories.getComponentFactoryByGuiName(it)!=null }

            windowManager.deserialize(normalLoad)
        }.ifError {
            logger.error(it) { "Application failed to start" }
            logger.info { "Application closing prematurely" }

            messagePane.onHidden += { close() }
            messagePane.show(
                    "Application did not start successfully and will close. Please fill an issue at $uriGithub "+
                            "providing the logs in $DIR_LOG. The exact problem was:\n ${it.stacktraceAsString}"
            )
        }.ifOk {
            if (normalLoad) Player.loadLastState() // initialize non critical parts
            parameterProcessor.process(fetchArguments()) // process app parameters passed when app started
            runLater { onStarted() }
        }
    }

    /** Starts this application normally if not yet started that way, otherwise has no effect. */
    @IsAction(name = "Start app normally", desc = "Loads last application state if not yet loaded.")
    fun startNormally() {
        if (!normalLoad) {
            normalLoad = true
            windowManager.deserialize(true)
            Player.loadLastState()
        }
    }

    @Deprecated("Called automatically")
    override fun stop() {
        logger.info { "Stopping application" }

        store()
        onStopping()

        // app
        Player.dispose()
        db.stop()
        ActionManager.stopActionListening()
        appCommunicator.stop()

        // cores
        mouse.dispose()
        contextMenus.dispose()
        instances.dispose()
        serializer.dispose()
        serializerXml.dispose()
        converter.dispose()
        imageIo.dispose()
        logging.dispose()
    }

    private fun store() {
        if (isInitialized.isOk) {
            if (normalLoad) Player.state.serialize()
            if (normalLoad) windowManager.serialize()
            configuration.save(name, FILE_SETTINGS)
            services.getAllServices()
                    .filter { it.isRunning() }
                    .forEach { it.stop() }
        }
    }

    /** Close this app normally. Causes invocation of [stop] as a result. */
    @IsAction(name = "Close app", desc = "Closes this application.")
    fun close() {
        logger.info { "Closing application" }

        Platform.exit()
    }

    /** Close this app abnormally, so next time this app does not start normally. Invokes [close] and then [stop]. */
    @IsAction(name = "Close app abnormally", desc = "Next start up will not restore current state, but it can be done manually later.")
    fun closeAbnormally() {
        normalLoad = false
        close()
    }

    /** @return arguments supplied to this application when it was launched */
    fun fetchArguments(): List<String> = parameters?.let { it.raw+it.unnamed+it.named.values }.orEmpty()

    /** @return JVM arguments supplied to JVM this application is running in */
    fun fetchVMArguments(): List<String> = ManagementFactory.getRuntimeMXBean().inputArguments

    /** @return number of instances of this application (including this one) running at this moment */
    fun getInstances(): Int = VirtualMachine.list().count { it.displayName().contains(App::class.java.packageName) }

    /** @return image of the icon of the application */
    fun getIcon(): Image = Image(File("icon512.png").toURI().toString())

    /** @return images of the icon of the application in all possible sizes */
    fun getIcons(): List<Image> = seqOf(16, 24, 32, 48, 128, 256, 512)
            .map { File("resources/icons/icon$it.png").toURI().toString() }
            .map { Image(it) }
            .toList()

    private fun AppParameterProcessor.initForApp() {
        addFileProcessor(
                { AudioFileFormat.isSupported(it, Use.APP) },
                { fs -> widgetManager.widgets.use<PlaylistFeature>(ANY) { it.playlist.addFiles(fs) } }
        )
        addFileProcessor(
                { it.isValidSkinFile() },
                { ui.setSkin(it[0]) }
        )
        addFileProcessor(
                { ImageFileFormat.isSupported(it) },
                // { fs -> widgetManager.use(ImageDisplayFeature::class.java, NO_LAYOUT) { it.showImages(fs) } }
                { fs -> fs.firstOrNull()?.let { actions.openImageFullscreen(it) } } // More convenient for user, add option to call one or the other
        )
        addFileProcessor(
                { it hasExtension "fxwl" },
                { it.forEach { windowManager.launchComponent(it) } }
        )
        addStringProcessor(
                { s -> widgetManager.factories.getFactories().any { it.nameGui()==s || it.name()==s } },
                { it.forEach { windowManager.launchComponent(it) } }
        )
    }

    private fun PluginManager.initForApp() {
        installPlugins(
                LibraryWatcher(),
                AppSearchPlugin(),
                DirSearchPlugin(),
                ScreenRotator(),
                Waifu2kPlugin()
        )
    }

    private fun Search.initForApp() {
        sources += { configuration.fields.asSequence().map { Entry.of(it) } }
        sources += { widgetManager.factories.getComponentFactories().filter { it.isUsableByUser() }.map { Entry.of(it) } }
        sources += { ui.skin.enumerateValues().asSequence().map { Entry.of({ "Open skin: $it" }, graphicsΛ = { Icon(IconMA.BRUSH) }) { ui.skin.value = it } } }
    }

    private fun AppInstanceComm.initForApp() {
        onNewInstanceHandlers += Consumer { parameterProcessor.process(it) }
    }

    private fun File.initForApp() = apply {
        if (!isAbsolute)
            fail { "File $this is not absolute" }

        if (!isValidatedDirectory(this))
            fail { "File $this is not accessible" }
    }

    companion object: KLogging() {

        private val logLevels = seqOf(Level.ALL, Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.OFF)

    }

}