package sp.it.pl.main

import ch.qos.logback.classic.Level
import com.sun.tools.attach.VirtualMachine
import javafx.application.Application
import javafx.application.Platform
import javafx.scene.image.Image
import javafx.stage.Stage
import mu.KLogging
import org.atteo.evo.inflector.English.plural
import sp.it.pl.audio.Player
import sp.it.pl.audio.Song
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.playlist.PlaylistSong
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.core.CoreConverter
import sp.it.pl.core.CoreEnv
import sp.it.pl.core.CoreFunctors
import sp.it.pl.core.CoreImageIO
import sp.it.pl.core.CoreInstances
import sp.it.pl.core.CoreLogging
import sp.it.pl.core.CoreMenus
import sp.it.pl.core.CoreMouse
import sp.it.pl.core.CoreSerializer
import sp.it.pl.core.CoreSerializerXml
import sp.it.pl.gui.UiManager
import sp.it.pl.gui.initApp
import sp.it.pl.gui.objects.autocomplete.ConfigSearch.Entry
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.objects.window.stage.WindowManager
import sp.it.pl.gui.pane.MessagePane
import sp.it.pl.layout.Component
import sp.it.pl.layout.container.Container
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.WidgetManager
import sp.it.pl.layout.widget.controller.io.InOutput
import sp.it.pl.layout.widget.controller.io.Input
import sp.it.pl.layout.widget.controller.io.Output
import sp.it.pl.layout.widget.feature.Feature
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
import sp.it.util.access.fieldvalue.ColumnField
import sp.it.util.access.fieldvalue.FileField
import sp.it.util.action.ActionManager
import sp.it.util.action.IsAction
import sp.it.util.async.runLater
import sp.it.util.conf.Configurable
import sp.it.util.conf.IsConfigurable
import sp.it.util.conf.MainConfiguration
import sp.it.util.conf.between
import sp.it.util.conf.c
import sp.it.util.conf.cr
import sp.it.util.conf.cv
import sp.it.util.conf.uiNoOrder
import sp.it.util.conf.values
import sp.it.util.dev.fail
import sp.it.util.file.FileType
import sp.it.util.file.Util.isValidatedDirectory
import sp.it.util.file.childOf
import sp.it.util.file.div
import sp.it.util.file.type.MimeTypes
import sp.it.util.functional.Try
import sp.it.util.functional.getOr
import sp.it.util.functional.invoke
import sp.it.util.functional.runTry
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Handler1
import sp.it.util.stacktraceAsString
import sp.it.util.system.SystemOutListener
import sp.it.util.type.ClassName
import sp.it.util.type.InstanceInfo
import sp.it.util.type.InstanceName
import sp.it.util.type.ObjectFieldMap
import sp.it.util.type.Util.getGenericPropertyType
import sp.it.util.ui.image.getImageDim
import sp.it.util.units.FileSize
import java.io.File
import java.lang.management.ManagementFactory
import java.net.URI
import java.net.URLConnection
import java.nio.charset.StandardCharsets
import java.util.function.Consumer
import sp.it.util.conf.IsConfig as C
import kotlin.jvm.JvmField as F

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

/** Application. Represents the program. */
@Suppress("unused")
@IsConfigurable("General")
class App: Application(), Configurable<Any> {

    init {
        APP = this.takeUnless { ::APP.isInitialized } ?: fail { "Multiple application instances disallowed" }
    }

    /** Whether application should close and delegate arguments if there is already running instance. */
    var loadSingleton = true
    /** Whether application starts with a state. If false, state is not restored on start or stored on close. */
    var loadStateful = true
    private var closedPrematurely = false
    var isInitialized: Try<Unit, Throwable> = Try.error(Exception("Initialization has not run yet"))
        private set

    // independent of app configuration
    /** Name of this application. */
    @F val name = "PlayerFX"
    /** Version of this application. */
    @F val version = "0.7"
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
    /** Directory containing persisted user ui. */
    @F val DIR_LAYOUTS_INIT = DIR_APP.childOf("templates").initForApp()
    /** Directory containing initial templates - persisted user ui bundled with the application. */
    @F val DIR_LAYOUTS = DIR_USERDATA.childOf("layouts").initForApp()
    /** Directory for application logging. */
    @F val DIR_LOG = DIR_USERDATA.childOf("log").initForApp()
    /** File for application configuration. */
    @F val FILE_SETTINGS = DIR_USERDATA.childOf("application.properties")

    /** Various actions for the application */
    val actions = AppActions()
    /** Global event bus. Usage: simply push an event or observe. Use of event constants and === is advised. */
    val actionStream = Handler1<Any>()
    /** Allows sending and receiving [java.lang.String] messages to and from other instances of this application. */
    val appCommunicator = AppInstanceComm()
    /** Observable [System.out]. */
    val systemout = SystemOutListener()
    /** Called just after this application is started (successfully) and fully initialized. Runs at most once. */
    val onStarted = Disposer()
    /** Called just before this application is stopped when it is fully still running. Runs at most once. */
    val onStopping = Disposer()
    /** Application argument handler. */
    val parameterProcessor = AppCli()

    init {
//        parameterProcessor.process(listOf("--version"))
        parameterProcessor.process(fetchArguments())
    }

    // cores (always active, mostly singletons)
    @F val configuration = MainConfiguration.apply { rawAdd(FILE_SETTINGS) }
    @F val logging = CoreLogging(DIR_RESOURCES.childOf("log_configuration.xml"), DIR_LOG)
    @F val env = CoreEnv.apply { init() }
    @F val imageIo = CoreImageIO(DIR_TEMP.childOf("imageio"))
    @F val converter = CoreConverter().apply { init() }
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
    val logLevelConsole by cv(Level.INFO).values(logLevels).uiNoOrder() sync { logging.changeLogBackLoggerAppenderLevel("STDOUT", it) }

    @C(name = "Level (file)", group = "Logging", info = "Logging level for logging to file")
    val logLevelFile by cv(Level.WARN).values(logLevels).uiNoOrder() sync { logging.changeLogBackLoggerAppenderLevel("FILE", it) }

    @C(name = "Text for no value", info = "Preferred text when no tag value for field. This value can be overridden.")
    var textNoVal by c("<none>")

    @C(name = "Text for multi value", info = "Preferred text when multiple tag values per field. This value can be overridden.")
    var textManyVal by c("<multi>")

    @C(name = "Animation FPS", info = "Update frequency in Hz for performance-heavy animations.")
    var animationFps by c(60.0).between(10.0, 60.0)

    @C(name = "Developer mode", info = "Unlock developer features.")
    var developerMode by c(false)

    @C(group = "Settings", name = "Settings use default", info = "Set all settings to default values.")
    val actionSettingsReset by cr { configuration.toDefault() }

    @C(group = "Settings", name = "Settings save", info = "Save all settings. Also invoked automatically when application closes")
    val actionSettingsSave by cr { configuration.save(name, FILE_SETTINGS) }

    /** Manages ui. */
    @F val ui = UiManager(DIR_SKINS)
    /** Guide containing tips and useful information. */
    @F val guide = Guide(actionStream)
    /** Application search */
    @F val search = Search()
    /** Manages persistence and in-memory storage. */
    @F val db = SongDb()
    /** Manages widgets. */
    @F val widgetManager = WidgetManager({ ui.messagePane.orBuild.show(it) })
    /** Manages windows. */
    @F val windowManager = WindowManager()
    /** Manages services. */
    @F val services = ServiceManager()
    /** Manages services. */
    @F val plugins = PluginManager()

    override fun init() {
        logging.init()

        logger.info { "JVM Args: ${fetchVMArguments()}" }
        logger.info { "App Args: ${fetchArguments()}" }

        if (getInstances()>1) {
            logger.info { "Multiple app instances detected" }
            if (loadSingleton) {
                logger.info { "App will close and delegate parameters to already running instance" }
                appCommunicator.fireNewInstanceEvent(fetchArguments())
                closedPrematurely = true
                return
            } else {
                loadStateful = false
            }
        }

        classFields.initApp()
        className.initApp()
        instanceName.initApp()
        instanceInfo.initApp()

        // init cores
        serializer.init()
        serializerXml.init()
        imageIo.init()
        instances.init()
        contextMenus.init()
        mouse.init()
        functors.init()

        // init app stuff
        search.initForApp()
        plugins.initForApp()
        appCommunicator.initForApp()

        // start parts that can be started from non application fx thread
        ActionManager.onActionRunPost += { APP.actionStream(it.name) }
        ActionManager.startActionListening()
        appCommunicator.start()
    }

    override fun start(primaryStage: Stage) {
        if (closedPrematurely) {
            logger.info { "Application closing prematurely" }
            close()
            return
        }

        isInitialized = runTry {
            services += TrayService()
            services += Notifier()
            services += PlaycountIncrementer()

            // install actions
            configuration.gatherActions(Player::class.java, null)
            configuration.gatherActions(PlaylistManager::class.java, null)
            configuration.installActions(
                    this,
                    ui,
                    actions,
                    windowManager,
                    services.getAllServices().toList()
            )

            widgetManager.init()
            db.init()
            Player.initialize()
            windowManager.deserialize()
        }.ifError {
            logger.error(it) { "Application failed to start" }
            logger.info { "Application closing prematurely" }

            MessagePane().initApp().apply { onHidden += { close() } }.show(
                    "Application did not start successfully and will close. Please fill an issue at $uriGithub "+
                    "providing the logs in $DIR_LOG. The exact problem was:\n ${it.stacktraceAsString}"
            )
        }.ifOk {
            if (loadStateful) Player.loadLastState()
            runLater { onStarted() }
        }
    }

    /** Starts this application normally if not yet started that way, otherwise has no effect. */
    @IsAction(name = "Start app normally", desc = "Loads last application state if not yet loaded.")
    fun startNormally() {
        if (!loadStateful) {
            loadStateful = true
            windowManager.deserialize()
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
        env.dispose()
        logging.dispose()
    }

    private fun store() {
        if (isInitialized.isOk) {
            if (loadStateful) Player.state.serialize()
            if (loadStateful) windowManager.serialize()
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

    /** @return arguments supplied to this application when it was launched */
    fun fetchArguments(): List<String> = parameters?.let { it.raw+it.unnamed+it.named.values }.orEmpty()

    /** @return JVM arguments supplied to JVM this application is running in */
    fun fetchVMArguments(): List<String> = ManagementFactory.getRuntimeMXBean().inputArguments

    /** @return number of instances of this application (including this one) running at this moment */
    fun getInstances(): Int = VirtualMachine.list().count { it.displayName().contains(App::class.java.packageName) }

    /** @return image of the icon of the application */
    fun getIcon(): Image = Image(File("icon512.png").toURI().toString())

    /** @return images of the icon of the application in all possible sizes */
    fun getIcons(): List<Image> = sequenceOf(16, 24, 32, 48, 128, 256, 512)
            .map { File("resources/icons/icon$it.png").toURI().toString() }
            .map { Image(it) }
            .toList()

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
        sources += {
            widgetManager.factories.getComponentFactories().filter { it.isUsableByUser() }.map {
                Entry.SimpleEntry(
                        "Open widget ${it.nameGui()}",
                        "Open widget ${it.nameGui()}\n\nOpens the widget in new window.",
                        { APP.windowManager.launchComponent(it.create()) }
                )
            }
        }
        sources += { ui.skins.asSequence().map { Entry.of({ "Open skin: ${it.name}" }, graphicsΛ = { Icon(IconMA.BRUSH) }) { ui.skin.value = it.name } } }
        sources += {
            widgetManager.factories.getFactories().filter { it.isUsableByUser() && it.externalWidgetData!=null}.map {
                Entry.SimpleEntry(
                        "Recompile widget ${it.nameGui()}",
                        "Recompile widget ${it.nameGui()} and reload all of its instances upon success",
                        { it.externalWidgetData!!.scheduleCompilation() }
                )
            }
        }
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

    private fun ObjectFieldMap.initApp() {
        add(PlaylistSong::class.java, PlaylistSong.Field.FIELDS)
        add(Metadata::class.java, Metadata.Field.FIELDS)
        add(MetadataGroup::class.java, MetadataGroup.Field.FIELDS)
        add(Any::class.java, ColumnField.FIELDS)
        add(File::class.java, FileField.FIELDS)
    }

    private fun ClassName.initApp() {
        addNoLookup(Void::class.java, "Nothing")
        add(String::class.java, "Text")
        add(File::class.java, "File")
        add(App::class.java, "Application")
        add(Song::class.java, "Song")
        add(PlaylistSong::class.java, "Playlist Song")
        add(Metadata::class.java, "Library Song")
        add(MetadataGroup::class.java, "Song Group")
        add(Service::class.java, "Service")
        add(Plugin::class.java, "Plugin")
        add(Widget::class.java, "Widget")
        add(Container::class.java, "Container")
        add(Input::class.java, "Input")
        add(Output::class.java, "Output")
        add(InOutput::class.java, "In-Output")
        add(Feature::class.java, "Feature")
        add(List::class.java, "List")
    }

    private fun InstanceName.initApp() {
        add(Void::class.java) { "<none>" }
        add(File::class.java) { it.path }
        add(App::class.java) { "This application" }
        add(Song::class.java) { it.getPathAsString() }
        add(PlaylistSong::class.java) { it.getTitle() }
        add(Metadata::class.java) { it.getTitleOrEmpty() }
        add(MetadataGroup::class.java) { it.getValueS("<none>") }
        add(Service::class.java) { it.name }
        add(Plugin::class.java) { it.name }
        add(Component::class.java) { it.exportName }
        add(Feature::class.java) { "Feature" }
        add(Input::class.java) { it.name }
        add(Output::class.java) { it.name }
        add(InOutput::class.java) { it.o.name }
        add(Collection::class.java) {
            val eType = getGenericPropertyType(it.javaClass)
            val eName = if (eType==it.javaClass || eType==null || eType==Any::class.java) "Item" else className[eType]
            it.size.toString()+" "+plural(eName, it.size)
        }
    }

    private fun InstanceInfo.initApp() {
        add(Void::class.java) { _, _ -> }
        add(String::class.java) { s, map -> map["Length"] = Integer.toString(s?.length ?: 0) }
        add(File::class.java) { f, map ->
            val type = FileType.of(f)
            map["File type"] = type.name

            if (type==FileType.FILE) {
                val fs = FileSize(f)
                map["Size"] = ""+fs+(if (fs.isKnown()) " (%,d bytes)".format(fs.inBytes()).replace(',', ' ') else "")
                map["Format"] = f.name.substringAfterLast('.', "<none>")
            }

            map[FileField.TIME_CREATED.name()] = FileField.TIME_CREATED.getOfS(f, "n/a")
            map[FileField.TIME_MODIFIED.name()] = FileField.TIME_MODIFIED.getOfS(f, "n/a")

            if (f.isImage()) {
                val res = getImageDim(f).map { "${it.width} x ${it.height}" }.getOr("n/a")
                map["Resolution"] = res
            }
        }
        add(App::class.java) { v, map -> map["Name"] = v.name }
        add(Component::class.java) { v, map -> map["Name"] = v.exportName }
        add(Metadata::class.java) { m, map ->
            Metadata.Field.FIELDS.asSequence()
                    .filter { it.isTypeStringRepresentable() && !it.isFieldEmpty(m) }
                    .forEach { map[it.name()] = it.getOfS(m, "<none>") }
        }
        add(PlaylistSong::class.java) { p, map ->
            PlaylistSong.Field.FIELDS.asSequence()
                    .filter { it.isTypeStringRepresentable() }
                    .forEach { map[it.name()] = it.getOfS(p, "<none>") }
        }
        add(Feature::class.java) { f, map ->
            map["Name"] = f.name
            map["Description"] = f.description
        }
    }

    companion object: KLogging() {

        private val logLevels = listOf(Level.ALL, Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.OFF)

    }

}