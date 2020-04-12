package sp.it.pl.main

import ch.qos.logback.classic.Level
import com.sun.tools.attach.VirtualMachine
import javafx.application.Application
import javafx.application.Platform
import javafx.scene.image.Image
import javafx.scene.text.TextAlignment.RIGHT
import javafx.stage.FileChooser.ExtensionFilter
import javafx.stage.Stage
import mu.KLogging
import sp.it.pl.audio.PlayerManager
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.core.CoreConverter
import sp.it.pl.core.CoreEnv
import sp.it.pl.core.CoreFunctors
import sp.it.pl.core.CoreImageIO
import sp.it.pl.core.CoreInstances
import sp.it.pl.core.CoreLogging
import sp.it.pl.core.CoreMenus
import sp.it.pl.core.CoreMouse
import sp.it.pl.core.CoreSerializer
import sp.it.pl.core.CoreSerializerJson
import sp.it.pl.layout.widget.WidgetManager
import sp.it.pl.main.App.Rank.MASTER
import sp.it.pl.main.App.Rank.SLAVE
import sp.it.pl.main.AppSearch.Source
import sp.it.pl.plugin.PluginManager
import sp.it.pl.plugin.impl.AppSearchPlugin
import sp.it.pl.plugin.impl.DirSearchPlugin
import sp.it.pl.plugin.impl.Guide
import sp.it.pl.plugin.impl.LibraryPlugin
import sp.it.pl.plugin.impl.Notifier
import sp.it.pl.plugin.impl.PlaycountIncrementer
import sp.it.pl.plugin.impl.ScreenRotator
import sp.it.pl.plugin.impl.SongDb
import sp.it.pl.plugin.impl.StartScreen
import sp.it.pl.plugin.impl.Tray
import sp.it.pl.plugin.impl.Waifu2k
import sp.it.pl.plugin.impl.WallpaperChanger
import sp.it.pl.ui.objects.autocomplete.ConfigSearch.Entry
import sp.it.pl.ui.objects.autocomplete.ConfigSearch.Entry.SimpleEntry
import sp.it.pl.ui.objects.window.stage.WindowManager
import sp.it.util.access.v
import sp.it.util.action.Action
import sp.it.util.action.ActionManager
import sp.it.util.action.IsAction
import sp.it.util.async.runLater
import sp.it.util.conf.GlobalConfigDelegator
import sp.it.util.conf.MainConfiguration
import sp.it.util.conf.c
import sp.it.util.conf.collectActionsOf
import sp.it.util.conf.cr
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.readOnlyUnless
import sp.it.util.conf.uiNoOrder
import sp.it.util.conf.values
import sp.it.util.dev.fail
import sp.it.util.dev.stacktraceAsString
import sp.it.util.file.FileType.FILE
import sp.it.util.file.Util.isValidatedDirectory
import sp.it.util.file.div
import sp.it.util.file.type.MimeTypes
import sp.it.util.functional.Try
import sp.it.util.functional.apply_
import sp.it.util.functional.runTry
import sp.it.util.functional.traverse
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Handler1
import sp.it.util.system.Os
import sp.it.util.system.SystemOutListener
import sp.it.util.system.chooseFile
import sp.it.util.system.saveFile
import sp.it.util.type.ClassName
import sp.it.util.type.InstanceDescription
import sp.it.util.type.InstanceName
import sp.it.util.type.ObjectFieldMap
import sp.it.util.ui.label
import java.io.File
import java.lang.management.ManagementFactory
import java.net.URI
import java.net.URLConnection
import kotlin.system.exitProcess
import kotlin.text.Charsets.UTF_8
import sp.it.pl.main.AppSettings.app as conf

lateinit var APP: App
private val verify = File::verify
private lateinit var rawArgs: Array<String>

fun main(args: Array<String>) {
   rawArgs = args

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
class App: Application(), GlobalConfigDelegator {
   override val configurableGroupPrefix = conf.name

   init {
      APP = this.takeUnless { ::APP.isInitialized } ?: fail { "Multiple application instances per process disallowed" }
   }

   /** Name of this application. */
   val name = "Spit Player"
   /** Version of this application. */
   val version = KotlinVersion(0, 8, 0)
   /** Application code encoding. Useful for compilation during runtime. */
   val encoding = UTF_8
   /** Absolute file of location of this app. Working directory of the project. `new File("").getAbsoluteFile()`. */
   val location = AppLocation
   /** Temporary directory of the os as seen by this application. */
   val locationTmp = File(System.getProperty("java.io.tmpdir")) apply_ verify
   /** Home directory of the os. */
   val locationHome = File(System.getProperty("user.home")) apply_ verify
   /** Uri for github website for project of this application. */
   val githubUri = URI.create("https://www.github.com/sghpjuikit/player/")

   /** Rank this application instance started with. [MASTER] if started as the first instance, [SLAVE] otherwise. */
   val rankAtStart: Rank = if (getInstances()>1) SLAVE else MASTER
   /**
    * Rank of this application instance.
    * [MASTER] instance is the one that singleton [SLAVE]s delegate to.
    * [SLAVE] instance is partly read-only, mostly to avoid concurrent io.
    */
   val rank by c(rankAtStart) def conf.rank
   /** Whether application should close and delegate arguments if there is already running instance. */
   var isSingleton = true
   /** Whether application starts with a state. If false, state is not restored on start or stored on close. */
   var isStateful = true
   /** Whether application forbids no windows. If true, at least one window must be open and closing last one will close the app. */
   var isUiApp = true
   /** Whether application is initialized. Starts as error and transitions to ok in [App.start] if no error occurs. */
   var isInitialized: Try<Unit, Throwable> = Try.error(Exception("Initialization has not run yet"))
      private set
   /** Various actions for the application */
   val actions = AppActions()
   /** Global event bus. Usage: simply push an event or observe. Use of event constants/objects is advised. */
   val actionStream = Handler1<Any>()
   /** Allows sending and receiving [java.lang.String] messages to and from other instances of this application. */
   val appCommunicator = AppInstanceComm()
   /** Observable [System.out]. */
   val systemout = SystemOutListener()
   /** Called just after this application is started (successfully) and fully initialized. Runs at most once, on fx thread. */
   val onStarted = Disposer()
   /** Called just before this application is stopped when it is fully still running. Runs at most once, on fx thread. */
   val onStopping = Disposer()
   /** Application argument handler. */
   val parameterProcessor = AppCli()

   init {
      location.user.layouts
      parameterProcessor.process(fetchArguments())

      if (rankAtStart==SLAVE) {
         logger.info { "Multiple app instances detected" }
         if (isSingleton) {
            logger.info { "App will close and delegate parameters to already running instance" }
            appCommunicator.fireNewInstanceEvent(fetchArguments())
            exitProcess(0)
         } else {
            isStateful = false
         }
      }
   }

   /** Configuration core. */
   val configuration = MainConfiguration.apply { rawAdd(location.user.application_properties) }

   /** Logging core. */
   val logging = CoreLogging(location.resources/"log_configuration.xml", location.user.log)
   /** Logging level for logging to standard output. */
   val logLevelConsole by cv(Level.INFO).values(logLevels).uiNoOrder() def conf.logging.`level(stdout)` sync { logging.changeLogBackLoggerAppenderLevel("STDOUT", it) }
   /** Logging level for logging to file. */
   val logLevelFile by cv(Level.WARN).values(logLevels).uiNoOrder() def conf.logging.`level(file)` sync { logging.changeLogBackLoggerAppenderLevel("FILE", it) }

   init {
      logging.init()
      logger.info { "JVM Args: ${fetchVMArguments()}" }
      logger.info { "App Args: ${fetchArguments()}" }
   }

   /** Environment core. */
   val env = CoreEnv.apply { init() }
   /** Image I/O core. */
   val imageIo = CoreImageIO(locationTmp/"imageio")
   /** String-Object converter core. */
   val converter = CoreConverter().apply { init() }
   /** Json converter core. */
   val serializerJson = CoreSerializerJson()
   /** Single persistent storage per type core. */
   val serializer = CoreSerializer
   /** File mime type core. */
   val mimeTypes = MimeTypes
   /** Map of instances per type core. */
   val instances = CoreInstances
   /** Class to ui name core. */
   val className = ClassName()
   /** Instance to ui name core. */
   val instanceName = InstanceName()
   /** Instance to ui description core. */
   val instanceInfo = InstanceDescription()
   /** Instance description fields core. */
   val classFields = ObjectFieldMap.DEFAULT
   /** Context menus core. */
   val contextMenus = CoreMenus
   /** Mouse core. */
   val mouse = CoreMouse
   /** Object functions core. */
   val functors = CoreFunctors

   /** Developer mode. Enables certain features useful for developers or power users. */
   val developerMode by cv(false) { v(it || parameterProcessor.cli.dev) } def conf.developerMode
   /** Action that calls [System.gc]. */
   val actionCallGc by cr(conf.runGarbageCollector) { System.gc() }.readOnlyUnless(developerMode)
   /** Action that persists [configuration] to default application properties file. */
   val actionSettingsSave by cr(conf.settings.saveSettings) {
      configuration.save(name, location.user.application_properties)
   }
   /** Action that persists [configuration] to user specified file. */
   val actionSettingsSaveTo by cr(conf.settings.saveSettingsToFile) {
      saveFile("Export settings", location.user.application_properties, "SpitPlayer", null, ExtensionFilter("Properties", "*.properties")).ifOk {
         configuration.save(name, it)
      }
   }
   /** Action that loads [configuration] to default values. */
   val actionSettingsLoadDefault by cr(conf.settings.loadDefaultSettings) {
      configuration.toDefault()
   }
   /** Action that re-loads [configuration] to values in default application properties file. */
   val actionSettingsLoad by cr(conf.settings.loadSettings) {
      configuration.rawAdd(location.user.application_properties)
      configuration.rawSet()
   }
   /** Action that re-loads [configuration] to values in user specified file. */
   val actionSettingsLoadFrom by cr(conf.settings.loadSettingsFromFile) {
      chooseFile("Import settings", FILE, location.user.application_properties, null, ExtensionFilter("Properties", "*.properties")).ifOk {
         configuration.rawAdd(it)
         configuration.rawSet()
      }
   }

   /** Manages ui. */
   @JvmField val ui = AppUi(location.skins)
   /** Application search */
   @JvmField val search = AppSearch()
   /** Manages persistence and in-memory storage. */
   @JvmField val db = SongDb()
   /** Manages audio playback. */
   @JvmField val audio = PlayerManager()
   /** Manages widgets. */
   @JvmField val widgetManager = WidgetManager()
   /** Manages windows. */
   @JvmField val windowManager = WindowManager()
   /** Manages plugins. */
   @JvmField val plugins = PluginManager()

   override fun init() {
      // init cores
      classFields.initApp()
      className.initApp()
      instanceName.initApp()
      instanceInfo.initApp()
      serializer.init()
      serializerJson.init()
      imageIo.init()
      instances.init()
      contextMenus.init()
      mouse.init()
      functors.init()

      // init app stuff
      search.initForApp()
      appCommunicator.initApp()

      // start parts that can be started from non application fx thread
      ActionManager.onActionRunPost += { APP.actionStream(it.name) }
      ActionManager.startActionListening(rankAtStart==SLAVE)
      if (rankAtStart==MASTER) appCommunicator.start()
   }

   override fun start(primaryStage: Stage) {
      logger.info { "Starting..." }

      isInitialized = runTry {
         plugins.initForApp()
         collectActionsOf(PlaylistManager)
         collectActionsOf(this)
         collectActionsOf(ui)
         collectActionsOf(actions)
         collectActionsOf(windowManager)
         collectActionsOf(audio)

         widgetManager.init()
         db.init()
         audio.initialize()
         if (isStateful) windowManager.deserialize()
      }.ifError {
         runLater {
            logger.error(it) { "Application failed to start" }
            logger.info { "Application closing prematurely" }

            AppErrors.push(
               "Application did not start successfully and will close.",
               "Please fill an issue at $githubUri providing the logs in ${location.user.log}." +
               "\nThe exact problem was:\n ${it.stacktraceAsString}"
            )
            AppErrors.showDetailForLastError()
         }
      }.ifOk {
         runLater {
            if (isStateful) audio.loadLastState()
            onStarted()
         }
      }
   }

   /** Starts this application normally if not yet started that way, otherwise has no effect. */
   @IsAction(name = conf.startNormally.cname, info = conf.startNormally.cinfo)
   fun startNormally() {
      if (!isStateful) {
         isStateful = true
         windowManager.deserialize()
         audio.loadLastState()
      }
   }

   @Deprecated("Called automatically")
   override fun stop() {
      logger.info { "Stopping..." }

      store()
      onStopping()

      // app
      plugins.plugins.forEach { it.stop() }
      audio.dispose()
      db.stop()
      ActionManager.stopActionListening()
      appCommunicator.stop()

      // cores
      mouse.dispose()
      contextMenus.dispose()
      instances.dispose()
      serializer.dispose()
      serializerJson.dispose()
      converter.dispose()
      imageIo.dispose()
      env.dispose()
      logging.dispose()
   }

   private fun store() {
      if (isInitialized.isOk) {
         if (rank==MASTER && isStateful) audio.state.serialize()
         if (rank==MASTER && isStateful) windowManager.serialize()
         if (rank==MASTER) configuration.save(name, location.user.application_properties)
      }
   }

   /** Close this app normally. Causes invocation of [stop] as a result. */
   @IsAction(name = conf.close.cname, info = conf.close.cinfo)
   fun close() {
      logger.info { "Closing application" }

      Platform.exit()
   }

   /** @return arguments supplied to this application's main method when it was launched */
   fun fetchArguments(): List<String> = rawArgs.toList()

   /** @return JVM arguments supplied to JVM this application is running in */
   fun fetchVMArguments(): List<String> = ManagementFactory.getRuntimeMXBean().inputArguments

   /** @return number of instances of this application (including this one) running at this moment */
   fun getInstances(): Int = VirtualMachine.list().count { App::class.java.packageName in it.displayName() }

   /** @return largest (512x512) image of the icon of the application */
   fun getIcon(): Image = Image(File("icon512.png").toURI().toString())

   private fun PluginManager.initForApp() {
      installPlugin<Tray>()
      installPlugin<Guide>()
      installPlugin<Notifier>()
      installPlugin<PlaycountIncrementer>()
      installPlugin<LibraryPlugin>()
      installPlugin<AppSearchPlugin>()
      installPlugin<DirSearchPlugin>()
      installPlugin<ScreenRotator>()
      installPlugin<Waifu2k>()
      installPlugin<WallpaperChanger>()
      installPlugin<StartScreen>()
   }

   private fun AppSearch.initForApp() {
      sources += Source("Settings") {
         configuration.getConfigs().flatMap {
            it.group.traverse { it.substringBeforeLast(".", "").takeIf { it.isNotEmpty() } }.asIterable()
         }.toSet().asSequence()
      } by { it.substringAfterLast(".") } toSource {
         Entry.of(
            name = "Open settings: ${it.substringAfterLast(".")}",
            icon = IconFA.COG,
            graphics = label("Settings > " + it.replace(".", " > ")) {
               styleClass += Css.DESCRIPTION
               textAlignment = RIGHT
            }
         ) {
            actions.openSettings(it)
         }
      }
      sources += Source("Actions") {
         configuration.getConfigs().filterIsInstance<Action>().filter { it.isEditableByUserRightNow() }.asSequence()
      } by { it.name + it.keys } toSource {
         Entry.of(it)
      }
      sources += Source("Skins") {
         ui.skins.asSequence()
      } by { "Open skin: ${it.name}" } toSource {
         Entry.of(
            name = "Open skin: ${it.name}",
            icon = IconMA.BRUSH,
            graphics = null
         ) {
            ui.skin.value = it.name
         }
      }
      sources += Source("Components - open") {
         widgetManager.factories.getComponentFactories().filter { it.isUsableByUser() }
      } by { "Open widget ${it.name}" } toSource {
         SimpleEntry(
            name = "Open widget ${it.name}",
            icon = IconFA.TH_LARGE,
            infoΛ = { "Open widget ${it.name}\n\nOpens the widget in new window." }
         ) {
            APP.windowManager.showWindow(it.create())
         }
      }
      sources += Source("Components - open (in new process)") {
         widgetManager.factories.getComponentFactories().filter { it.isUsableByUser() }
      } by { "Open widget ${it.name} (in new process)" } toSource { c ->
         SimpleEntry(
            name = "Open widget ${c.name} (in new process)",
            icon = IconFA.REFRESH,
            infoΛ = { "Open widget ${c.name}\n\nOpens the widget in new process." }
         ) {
            val f = if (Os.WINDOWS.isCurrent) location.spitplayerc_exe else location.spitplayer_sh
            f.runAsAppProgram(
               "Launching component ${c.name} in new process",
               "--singleton=false", "--stateless=true", "open-component", "\"${c.name}\""
            )
         }
      }
      sources += Source("Components - recompile") {
         widgetManager.factories.getFactories().filter { it.isUsableByUser() }
      } by { "Recompile widget ${it.name}" } toSource {
         SimpleEntry(
            name = "Recompile widget ${it.name}",
            icon = IconFA.TH_LARGE,
            infoΛ = { "Recompile widget ${it.name} and reload all of its instances upon success" }
         ) {
            widgetManager.factories.recompile(it)
         }
      }
   }

   enum class Rank {
      MASTER, SLAVE
   }

   companion object: KLogging() {

      private val logLevels = listOf(Level.ALL, Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.OFF)

   }

}