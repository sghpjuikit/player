package sp.it.pl.main

import ch.qos.logback.classic.Level
import com.sun.tools.attach.VirtualMachine
import javafx.application.Application
import javafx.application.Platform
import javafx.scene.image.Image
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
import sp.it.pl.gui.UiManager
import sp.it.pl.gui.objects.autocomplete.ConfigSearch.Entry
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.objects.window.stage.WindowManager
import sp.it.pl.gui.pane.VmOptionsPane
import sp.it.pl.layout.widget.WidgetManager
import sp.it.pl.main.App.Rank.MASTER
import sp.it.pl.main.App.Rank.SLAVE
import sp.it.pl.main.AppSearch.Source
import sp.it.pl.plugin.Plugin
import sp.it.pl.plugin.PluginManager
import sp.it.pl.plugin.appsearch.AppSearchPlugin
import sp.it.pl.plugin.database.SongDb
import sp.it.pl.plugin.dirsearch.DirSearchPlugin
import sp.it.pl.plugin.library.LibraryPlugin
import sp.it.pl.plugin.notif.Notifier
import sp.it.pl.plugin.playcount.PlaycountIncrementer
import sp.it.pl.plugin.screenrotator.ScreenRotator
import sp.it.pl.plugin.tray.TrayPlugin
import sp.it.pl.plugin.waifu2k.Waifu2kPlugin
import sp.it.pl.plugin.wallpaper.WallpaperPlugin
import sp.it.util.access.v
import sp.it.util.action.Action
import sp.it.util.action.ActionManager
import sp.it.util.action.IsAction
import sp.it.util.async.runLater
import sp.it.util.conf.GlobalConfigDelegator
import sp.it.util.conf.MainConfiguration
import sp.it.util.conf.between
import sp.it.util.conf.c
import sp.it.util.conf.cr
import sp.it.util.conf.cv
import sp.it.util.conf.readOnlyUnless
import sp.it.util.conf.uiNoOrder
import sp.it.util.conf.values
import sp.it.util.dev.fail
import sp.it.util.dev.stacktraceAsString
import sp.it.util.file.Util.isValidatedDirectory
import sp.it.util.file.div
import sp.it.util.file.type.MimeTypes
import sp.it.util.functional.Try
import sp.it.util.functional.apply_
import sp.it.util.functional.invoke
import sp.it.util.functional.runTry
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Handler1
import sp.it.util.system.Os
import sp.it.util.system.SystemOutListener
import sp.it.util.type.ClassName
import sp.it.util.type.InstanceDescription
import sp.it.util.type.InstanceName
import sp.it.util.type.ObjectFieldMap
import java.io.File
import java.lang.management.ManagementFactory
import java.net.URI
import java.net.URLConnection
import kotlin.system.exitProcess
import kotlin.text.Charsets.UTF_8
import sp.it.util.conf.IsConfig as C

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
   override val configurableGroupPrefix = "General"

   init {
      APP = this.takeUnless { ::APP.isInitialized } ?: fail { "Multiple application instances disallowed" }
   }

   /** Name of this application. */
   val name = "PlayerFX"
   /** Version of this application. */
   val version = "0.7.0"
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
   val rank = rankAtStart
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

   // cores (always active, mostly singletons)
   @JvmField val configuration = MainConfiguration.apply { rawAdd(location.user.`application properties`) }
   @JvmField val logging = CoreLogging(location.resources/"log_configuration.xml", location.user.log)
   @JvmField val env = CoreEnv.apply { init() }
   @JvmField val imageIo = CoreImageIO(locationTmp/"imageio")
   @JvmField val converter = CoreConverter().apply { init() }
   @JvmField val serializerJson = CoreSerializerJson()
   @JvmField val serializer = CoreSerializer
   @JvmField val instances = CoreInstances
   @JvmField val mimeTypes = MimeTypes
   @JvmField val className = ClassName()
   @JvmField val instanceName = InstanceName()
   @JvmField val instanceInfo = InstanceDescription()
   @JvmField val classFields = ObjectFieldMap.DEFAULT
   @JvmField val contextMenus = CoreMenus
   @JvmField val mouse = CoreMouse
   @JvmField val functors = CoreFunctors

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

   @C(
      name = "Developer mode",
      info = "Unlock developer features." +
         "\n * widgets will not recompile when any library or application jar is modified after widget's source file" +
         "\n * enables Java VM management" +
         "\n * enables menu items that call object's methods using reflection" +
         "\n * shows experimental widgets" +
         "\n * shows class information about objects in object details" +
         "\nCan be forced to true by starting the application with a `--dev` flag"
   )
   val developerMode by cv(false) { v(it || parameterProcessor.cli.dev) }

   @C(group = "Settings", name = "Settings use default", info = "Set all settings to default values.")
   val actionSettingsReset by cr { configuration.toDefault() }

   @C(group = "Settings", name = "Settings save", info = "Save all settings. Also invoked automatically when application closes")
   val actionSettingsSave by cr { configuration.save(name, location.user.`application properties`) }

   @C(name = "Manage VM options", info = "Manage Java virtual machine settings. Requires developer mode.")
   val manageVM by cr {
      windowManager.showFloating("VM options") {
         VmOptionsPane().apply {
            prefHeight = 600.emScaled
         }
      }
      Unit
   }.readOnlyUnless(
      developerMode
   )

   /** Manages ui. */
   @JvmField val ui = UiManager(location.skins)
   /** Guide containing tips and useful information. */
   @JvmField val guide = Guide(actionStream)
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
      logging.init()

      logger.info { "JVM Args: ${fetchVMArguments()}" }
      logger.info { "App Args: ${fetchArguments()}" }

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
      isInitialized = runTry {
         plugins.initForApp()
         configuration.gatherActions(APP.audio::class.java, null)
         configuration.gatherActions(PlaylistManager::class.java, null)
         configuration.installActions(
            this,
            ui,
            actions,
            windowManager,
            audio
         )

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
   @IsAction(name = "Start app normally", desc = "Loads last application state if not yet loaded.")
   fun startNormally() {
      if (!isStateful) {
         isStateful = true
         windowManager.deserialize()
         audio.loadLastState()
      }
   }

   @Deprecated("Called automatically")
   override fun stop() {
      logger.info { "Stopping application" }

      store()
      onStopping()

      // app
      plugins.getAll().forEach { if (it.isRunning()) it.stop() }
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
         if (rank==MASTER) configuration.save(name, location.user.`application properties`)
      }
   }

   /** Close this app normally. Causes invocation of [stop] as a result. */
   @IsAction(name = "Close app", desc = "Closes this application.")
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

   /** @return image of the icon of the application */
   fun getIcon(): Image = Image(File("icon512.png").toURI().toString())

   /** @return images of the icon of the application in all possible sizes */
   fun getIcons(): List<Image> = sequenceOf(16, 24, 32, 48, 128, 256, 512)
      .map { File("resources/icons/icon$it.png").toURI().toString() }
      .map { Image(it) }
      .toList()

   private fun PluginManager.initForApp() {
      fun installIfMaster(plugin: () -> Plugin) = if (rank==MASTER) installPlugins(plugin()) else Unit

      installIfMaster { TrayPlugin() }
      installPlugins(Notifier())
      installPlugins(PlaycountIncrementer())
      installPlugins(LibraryPlugin())
      installIfMaster { AppSearchPlugin() }
      installIfMaster { DirSearchPlugin() }
      installIfMaster { ScreenRotator() }
      installIfMaster { Waifu2kPlugin() }
      installIfMaster { WallpaperPlugin() }
   }

   private fun AppSearch.initForApp() {
      sources += Source("Actions") {
         configuration.getFields()
            .filter { it.type==Action::class.javaObjectType && it.isEditableByUserRightNow() }
            .asSequence().map { Entry.of(it) }
      }
      sources += Source("Skins") {
         ui.skins.asSequence().map {
            Entry.of({ "Open skin: ${it.name}" }, graphicsΛ = { Icon(IconMA.BRUSH) }) { ui.skin.value = it.name }
         }
      }
      sources += Source("Components - widgets") {
         widgetManager.factories.getComponentFactories().filter { it.isUsableByUser() }.map {
            Entry.SimpleEntry(
               "Open widget ${it.name()}",
               { "Open widget ${it.name()}\n\nOpens the widget in new window." },
               { APP.windowManager.launchComponent(it.create()) }
            )
         }
      }
      sources += Source("Components - all") {
         widgetManager.factories.getComponentFactories().filter { it.isUsableByUser() }.map { c ->
            Entry.SimpleEntry(
               "Open widget ${c.name()} (in new process)",
               { "Open widget ${c.name()}\n\nOpens the widget in new process." },
               {
                  val f = if (Os.WINDOWS.isCurrent) location.`playerfxc exe` else location.`playerfx sh`
                  f.runAsAppProgram(
                     "Launching component ${c.name()} in new process",
                     "--singleton=false", "--stateless=true", "open-component", "\"${c.name()}\""
                  )
               }
            )
         }
      }
      sources += Source("Components - compile)") {
         widgetManager.factories.getFactories().filter { it.isUsableByUser() && it.externalWidgetData!=null }.map {
            Entry.SimpleEntry(
               "Recompile widget ${it.name()}",
               { "Recompile widget ${it.name()} and reload all of its instances upon success" },
               { it.externalWidgetData!!.scheduleCompilation() }
            )
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