package sp.it.pl.layout.widget

import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections.observableArrayList
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.input.KeyCode.ESCAPE
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.layout.Pane
import javafx.scene.layout.Region
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.WindowEvent.WINDOW_HIDING
import mu.KLogging
import sp.it.pl.core.NameUi
import sp.it.pl.ui.objects.window.stage.Window
import sp.it.pl.ui.objects.window.stage.asLayout
import sp.it.pl.layout.Component
import sp.it.pl.layout.container.ComponentUi
import sp.it.pl.layout.container.Container
import sp.it.pl.layout.container.Layout
import sp.it.pl.layout.container.SwitchContainer
import sp.it.pl.layout.widget.WidgetSource.NONE
import sp.it.pl.layout.widget.WidgetSource.OPEN
import sp.it.pl.layout.widget.WidgetSource.OPEN_LAYOUT
import sp.it.pl.layout.widget.WidgetSource.OPEN_STANDALONE
import sp.it.pl.layout.widget.controller.Controller
import sp.it.pl.layout.widget.feature.Feature
import sp.it.pl.main.APP
import sp.it.pl.main.App.Rank.SLAVE
import sp.it.pl.main.AppError
import sp.it.pl.main.AppErrorAction
import sp.it.pl.main.emScaled
import sp.it.pl.main.ifErrorNotify
import sp.it.pl.main.showFloating
import sp.it.pl.main.thenWithAppProgress
import sp.it.pl.main.withAppProgress
import sp.it.pl.ui.objects.window.ShowArea
import sp.it.pl.ui.objects.window.popup.PopWindow
import sp.it.pl.ui.objects.window.stage.installWindowInteraction
import sp.it.pl.ui.pane.OverlayPane
import sp.it.pl.ui.pane.OverlayPane.Display.SCREEN_OF_MOUSE
import sp.it.util.access.Values
import sp.it.util.access.v
import sp.it.util.async.FX
import sp.it.util.async.burstTPExecutor
import sp.it.util.async.executor.EventReducer
import sp.it.util.async.future.Fut.Companion.fut
import sp.it.util.async.runIO
import sp.it.util.async.threadFactory
import sp.it.util.collections.ObservableListRO
import sp.it.util.collections.mapset.MapSet
import sp.it.util.collections.materialize
import sp.it.util.conf.GlobalSubConfigDelegator
import sp.it.util.conf.c
import sp.it.util.conf.cr
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.singleton
import sp.it.util.dev.Idempotent
import sp.it.util.dev.fail
import sp.it.util.dev.failIf
import sp.it.util.dev.failIfNotFxThread
import sp.it.util.dev.stacktraceAsString
import sp.it.util.file.FileMonitor
import sp.it.util.file.Util.isValidatedDirectory
import sp.it.util.file.Util.saveFileAs
import sp.it.util.file.child
import sp.it.util.file.children
import sp.it.util.file.div
import sp.it.util.file.hasExtension
import sp.it.util.file.isAnyChildOf
import sp.it.util.file.isAnyParentOf
import sp.it.util.file.isParentOf
import sp.it.util.file.toURLOrNull
import sp.it.util.file.unzip
import sp.it.util.functional.Try
import sp.it.util.functional.and
import sp.it.util.functional.andAlso
import sp.it.util.functional.asArray
import sp.it.util.functional.asIf
import sp.it.util.functional.compose
import sp.it.util.functional.getOrSupply
import sp.it.util.functional.ifFalse
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.invoke
import sp.it.util.functional.let_
import sp.it.util.functional.runTry
import sp.it.util.functional.toOptional
import sp.it.util.functional.toTry
import sp.it.util.functional.toUnit
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.on
import sp.it.util.reactive.onChange
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.sync1If
import sp.it.util.system.Os
import sp.it.util.system.browse
import sp.it.util.type.isSubclassOf
import sp.it.util.ui.Util
import sp.it.util.ui.anchorPane
import sp.it.util.ui.getScreenForMouse
import sp.it.util.ui.minPrefMaxWidth
import sp.it.util.ui.removeFromParent
import sp.it.util.ui.scrollText
import sp.it.util.ui.stylesheetToggle
import sp.it.util.ui.text
import sp.it.util.units.seconds
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.lang.ProcessBuilder.Redirect
import java.net.URI
import java.net.URLClassLoader
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.WatchEvent.Kind
import java.util.concurrent.TimeUnit
import javax.tools.ToolProvider
import kotlin.math.ceil
import kotlin.reflect.KClass
import kotlin.streams.asSequence
import kotlin.text.Charsets.UTF_8
import javafx.stage.Window as WindowFX
import kotlin.reflect.cast
import sp.it.util.collections.setTo
import sp.it.util.conf.EditMode
import sp.it.util.conf.cList
import sp.it.util.type.map

/** Handles operations with Widgets. */
class WidgetManager {

   /** Public API for layout management. */
   @JvmField val layouts = Layouts()
   /** Public API for factory management. */
   @JvmField val factories = Factories()
   /** Public API for widget management. */
   @JvmField val widgets = Widgets()
   /** All component factories by their name. */
   private val factoriesC = MapSet<String, ComponentFactory<*>> { it.name }
   /** All component factories, observable, writable. */
   private val factoriesObservableCImpl = observableArrayList<ComponentFactory<*>>()
   /** All component factories, observable, readable. */
   private val factoriesObservableC = ObservableListRO<ComponentFactory<*>>(factoriesObservableCImpl)
   /** All widget factories by their name. */
   private val factoriesW = MapSet<String, WidgetFactory<*>> { it.id }
   /** All widget factories, observable, writable. */
   private val factoriesObservableWImpl = observableArrayList<WidgetFactory<*>>()
   /** All widget factories, observable, readable. */
   private val factoriesObservableW = ObservableListRO<WidgetFactory<*>>(factoriesObservableWImpl)
   /** All widget directories by their name. */
   private val monitors = MapSet<File, WidgetMonitor> { it.widgetDir }
   /** Separates entries of a java classpath argument, passed to JVM. */
   private var classpathSeparator = Os.current.classpathSeparator
   private var initialized = false
   private val compilerThread by lazy { burstTPExecutor(ceil(Runtime.getRuntime().availableProcessors()/4.0).toInt(), 30.seconds, threadFactory("widgetCompiler", true)) }
   private val kotlinc by lazy {
      val kotlinVersion = KotlinVersion.CURRENT.toString()
      val kotlincDir = APP.location.kotlinc
      val kotlincVersionFile = kotlincDir/"version"
      val kotlincZipName = "kotlin-compiler-$kotlinVersion.zip"
      val kotlincBinary = when (Os.current) {
         Os.WINDOWS -> APP.location.kotlinc/"bin"/"kotlinc.bat"
         else -> APP.location.kotlinc/"bin"/"kotlinc"
      }
      val kotlincZip = kotlincDir/kotlincZipName
      val kotlincLink = URI("https://github.com/JetBrains/kotlin/releases/download/v$kotlinVersion/$kotlincZipName")
      runIO {
         fun isCorrectVersion() = kotlincVersionFile.exists() && kotlincVersionFile.readText()==kotlincLink.toString()
         infix fun Boolean.orFailIO(message: () -> String) = also { if (!this) throw IOException(message()) }

         if (!isCorrectVersion() || !kotlincBinary.exists()) {
            if (kotlincDir.exists()) kotlincDir.deleteRecursively() orFailIO { "Failed to remove Kotlin compiler in=$kotlincDir" }
            saveFileAs(kotlincLink.toString(), kotlincZip)
            kotlincZip.unzip(kotlincDir) { it.substringAfter("kotlinc/");  }
            kotlincBinary.setExecutable(true) orFailIO { "Failed to make file=$kotlincBinary executable" }
            kotlincZip.delete() orFailIO { "Failed to clean up downloaded file=$kotlincZip" }
            kotlincVersionFile.writeText(kotlincLink.toString())
         }

         failIf(!isCorrectVersion()) { "Kotlinc has wrong version" }
         failIf(!kotlincBinary.exists()) { "Kotlinc executable=$kotlincBinary does not exist" }
         failIf(!kotlincBinary.canExecute()) { "Kotlinc executable=$kotlincBinary must be executable" }
         kotlincBinary
      }.withAppProgress("Obtaining Kotlin compiler").onDone(FX) {
         it.toTry().ifErrorNotify {
            AppError(
               "Failed to obtain Kotlin compiler",
               "Kotlin version: $kotlinVersion\nKotlinc link: $kotlincLink\n\n${it.stacktraceAsString}",
               AppErrorAction("Download manually") {
                  kotlincLink.browse()
                  kotlincDir.browse()
                  showFloating("Setup Kotlin compiler") {
                     scrollText {
                        text(
                           buildString {
                              appendLine("It is recommended to let the application set up the compiler, you may wish to check the exact error instead.")
                              appendLine()
                              appendLine("If you still wish to set up the compiler manually, you need to:")
                              appendLine(" * Download the compiler from $kotlincLink")
                              appendLine(" * Extract the contents to $kotlincDir so there exists executable file $kotlincBinary")
                              appendLine(" * Create file $kotlincVersionFile (no extension) and set its text content to the link, you obtained the compiler from")
                           }
                        )
                     }
                  }
               }
            )
         }
      }
   }

   fun init() {
      if (initialized) return

      if (!(APP.location/"java"/"bin").exists())
         logger.error { "Java development kit is missing. Please install JDK in ${APP.location/"java"}" }
      if (!(APP.location/"kotlinc"/"bin").exists())
         logger.error { "Kotlin compiler is missing. Please install kotlinc in ${APP.location/"kotlinc"}" }

      // internal factories
      registerFactory(emptyWidgetFactory)
      registerFactory(introWidgetFactory)
      registerFactory(initialTemplateFactory)

      // external factories
      val dirW = APP.location.widgets
      if (!isValidatedDirectory(dirW)) {
         logger.error { "External widgets registration failed: $dirW is not a valid directory" }
      } else {
         dirW.children().filter { it.isDirectory }.forEach { widgetDir ->
            monitors.computeIfAbsent(widgetDir) { WidgetMonitor(it) }.updateFactory()
         }

         FileMonitor.monitorDirectory(dirW, true) { type, f ->
            when {
               dirW==f -> Unit
               dirW isParentOf f -> {
                  if (type===ENTRY_CREATE) {
                     if (f.isDirectory) {
                        monitors.computeIfAbsent(f) { WidgetMonitor(it) }.updateFactory()
                     }
                  }
                  if (type===ENTRY_DELETE) {
                     monitors[f]?.dispose()
                  }
               }
               else -> {
                  monitors.find { it.widgetDir isAnyParentOf f }?.handleResourceChange(type, f)
               }
            }
         }
      }

      // external factories for .fxwl components
      val lDir = APP.location.user.layouts
      val lDirInitial = APP.location.templates
      if (!isValidatedDirectory(lDir) && !isValidatedDirectory(lDirInitial)) {
         logger.error { "External .fxwl widgets registration failed: $lDir or $lDirInitial is not a valid directory" }
      } else {
         listOf(lDirInitial, lDir).forEach {
            it.walkTopDown().filter { it hasExtension "fxwl" }.forEach { registerFactory(DeserializingFactory(it)) }
         }

         FileMonitor.monitorDirectory(lDir, true, { it hasExtension "fxwl" }) { type, f ->
            if (type===ENTRY_CREATE) {
               registerFactory(DeserializingFactory(f))
            }
            if (type===ENTRY_DELETE) {
               factoriesC.asSequence()
                  .filter { it is DeserializingFactory && it.launcher==f }
                  .materialize()
                  .forEach { unregisterFactory(it) }
            }
         }
      }

      initialized = true
   }

   private fun registerFactory(factory: ComponentFactory<*>) {
      logger.info { "Registering $factory" }

      if (factory is WidgetFactory<*> && factory !in factoriesW) {
         factoriesW += factory
         factoriesObservableWImpl += factory
      }
      if (factory !in factoriesC) {
         factoriesC += factory
         factoriesObservableCImpl += factory
      }
   }

   private fun unregisterFactory(factory: ComponentFactory<*>) {
      logger.info { "Unregistering $factory" }

      if (factory is WidgetFactory<*>) factoriesW -= factory
      if (factory is WidgetFactory<*>) factoriesObservableWImpl -= factory
      factoriesC -= factory
      factoriesObservableCImpl -= factory
   }

   private inner class WidgetMonitor constructor(val widgetDir: File) {
      val widgetName = widgetDir.name.capitalize()
      val packageName  = widgetName.decapitalize()
      val classFqName  = "$packageName.$widgetName"
      val skinFile = widgetDir/"skin.css"
      val compileDir = widgetDir/"out"
      val scheduleCompilation = EventReducer.toLast<Void>(500.0) { compileFx() }

      /** @return primary source file (either Kotlin or Java) or null if none exists */
      fun findSrcFile() = null
         ?: widgetDir.child("$widgetName.kt").takeIf { it.exists() }
         ?: widgetDir.child("$widgetName.java").takeIf { it.exists() }

      fun findSrcFiles() = widgetDir.children().filter { it.hasExtension("java", "kt") }

      fun findClassFile() = (compileDir/widgetName.decapitalize()/"$widgetName.class").takeIf { it.exists() }

      fun findClassFiles() = compileDir.walk().filter { it hasExtension "class" }

      fun computeClassPath(): String = computeClassPathElements().joinToString(classpathSeparator)

      private fun computeClassPathElements() = getAppJarFile() + (findAppLibFiles() + compileDir + findLibFiles()).map { it.relativeToApp() }

      private fun findLibFiles() = widgetDir.children().filterSourceJars()

      private fun findAppLibFiles() = APP.location.lib.children().filterSourceJars()

      private fun getAppJarFile(): Sequence<String> {
         return if (APP.location.spitplayer_jar.exists()) {
            sequenceOf(APP.location.spitplayer_jar.relativeToApp())
         } else {
            System.getProperty("java.class.path")
               .splitToSequence(classpathSeparator)
               .filter { it.contains("build\\classes") || it.contains("build\\kotlin-classes") }
         }
      }

      private fun Sequence<File>.filterSourceJars() = this
         .filter { it hasExtension "jar" }
         .filter { !it.path.endsWith("sources.jar") }
         .filter { !it.path.endsWith("javadoc.jar") }


      fun handleResourceChange(type: Kind<Path>, file: File) {
         if (type===ENTRY_CREATE || type===ENTRY_MODIFY) {
            when {
               file==skinFile -> {
                  logger.info { "Widget=$widgetName skin file=${file.name}} changed $type" }
                  widgets.findAll(OPEN)
                     .filter { it.factory.id==widgetName }
                     .filter { it.isLoaded }
                     .forEach {
                        val root = it.graphics
                        val skinUrl = skinFile.toURLOrNull()?.toExternalForm()
                        if (skinUrl!=null) {
                           root?.stylesheetToggle(skinUrl, false)
                           root?.stylesheetToggle(skinUrl, true)
                        }
                     }
               }
               file==compileDir || file.isAnyChildOf(compileDir) -> Unit
               file hasExtension "class" -> Unit
               else -> {
                  logger.info { "Widget=$widgetName source file=${file.name} changed $type" }
                  if (widgets.autoRecompile.value)
                     scheduleCompilation()
               }
            }
         }
      }

      @Idempotent
      fun dispose() {
         factories.factoriesInCompilation -= widgetDir
         factoriesW.removeKey(widgetName)
         monitors.removeKey(widgetDir)
      }

      @Suppress("ConstantConditionIf")
      fun updateFactory() {
         val srcFile = findSrcFile()
         val srcFiles = findSrcFiles().toList()
         val srcFilesAvailable = srcFile!=null
         val classFile = findClassFile()
         val classFiles = findClassFiles().toList()
         val dependencies = when {
            APP.developerMode.value -> srcFiles
            else -> srcFiles + computeClassPathElements().map { File(it) }.filterSourceJars().toList()
         }
         val classFilesAvailable = classFile!=null && classFiles modifiedAfter dependencies

         logger.info { "Widget=$widgetName factory update, source files available=$srcFilesAvailable class files available=$classFilesAvailable" }

         if (classFilesAvailable) {
            val controllerType = loadClass(classFqName, compileDir, findLibFiles())
            registerFactory(controllerType)
         } else if (srcFilesAvailable) {
            compileFx()
         }
      }

      @Suppress("UNCHECKED_CAST")
      private fun registerFactory(controllerType: Try<Class<*>, Throwable>) {
         controllerType
            .ifError {
               logger.warn(it) { "Widget $widgetName failed to register factory" }
               if (it is UnsupportedClassVersionError)
                  compileFx()
            }
            .ifOk { type ->
               if (!type.isSubclassOf<Controller>()) {
                  logger.warn { "Widget $widgetName failed to register factory, class $type in $widgetDir does not implement ${Controller::class}" }
               } else {
                  val widgetType = (type as Class<Controller>).kotlin
                  val widgetFactory = WidgetFactory(widgetType, widgetDir)
                  registerFactory(widgetFactory)
                  if (initialized) widgetFactory.reloadAllOpen()
               }
            }
      }

      private fun compileFx() {
         failIfNotFxThread()
         if (APP.rank==SLAVE) return

         factories.factoriesInCompilation += widgetDir
         fut().thenWithAppProgress(compilerThread, "Compiling $widgetName") {
            compile()
         }.onDone(FX) {
            factories.factoriesInCompilation -= widgetDir
            it.toTry()
               .ifError { logger.error(it) { "Widget $widgetName failed to compile" } }
               .getOrSupply { Try.error(it.message ?: "Unspecified error") }
               .and {
                  if (findClassFile()==null) {
                     logger.error { "Widget $widgetName was compiled, but the class file was not found. Does the class name match widget name?" }
                     Try.error("Invalid compilation result. Does the class name match widget name?")
                  } else {
                     Try.ok()
                  }
               }
               .ifOk { updateFactory() }
               .ifErrorNotify { AppError("Widget $widgetName failed to compile", "Reason: $it") }
         }
      }

      private fun compile(): Try<Nothing?, String> {
         logger.info { "Widget=$widgetName compiling..." }

         if (compileDir.exists()) compileDir.deleteRecursively().ifFalse { return Try.error("Failed to delete $compileDir") }
         if (!compileDir.exists()) compileDir.mkdirs().ifFalse { return Try.error("Failed to create $compileDir") }

         val srcFiles = findSrcFiles().toList()
         val hasKotlin = srcFiles.any { it hasExtension "kt" }
         val hasJava = srcFiles.any { it hasExtension "java" }
         return when {
            hasJava && hasKotlin -> Try.error("Mixed Java-Kotlin code not supported")
            hasKotlin -> compileKotlin(srcFiles.asSequence())
            hasJava -> compileJava(srcFiles.asSequence())
            else -> Try.error("No source files available")
         }
      }

      /** Compiles specified .java files into .class files. */
      private fun compileJava(javaSrcFiles: Sequence<File>): Try<Nothing?, String> {
         return try {
            val options = sequenceOf(
               "-encoding", APP.encoding.name(),
               "-d", compileDir.relativeToApp(),
               "-Xlint",
               "-Xlint:-path",
               "-Xlint:-processing",
               "-cp", computeClassPath()
            )
            val sourceFiles = javaSrcFiles.map { it.path }
            val arguments = (options + sourceFiles).asArray()

            logger.info("Compiling with arguments=${arguments.joinToString(" ")}")
            val compiler = ToolProvider.getSystemJavaCompiler() ?: run {
               logger.error { "Compilation failed\nJava system compiler not available" }
               return Try.error("Java system compiler not available")
            }

            val streamStdOut = ByteArrayOutputStream(1000)
            val streamStdErr = ByteArrayOutputStream(1000)
            val success = compiler.run(null, streamStdOut, streamStdErr, *arguments)
            val isSuccess = success==0
            val textStdOut = streamStdOut.toString(UTF_8).prettifyCompilerOutput()
            val textStdErr = streamStdErr.toString(UTF_8).prettifyCompilerOutput()

            if (isSuccess) {
               logger.info { "Compilation succeeded$textStdOut" }
               Try.ok()
            } else {
               logger.error { "Compilation failed$textStdErr" }
               Try.error(textStdErr)
            }
         } catch (e: Exception) {
            Try.error(e.message ?: "")
         }
      }

      /** Compiles specified .kt files into .class files. */
      private fun compileKotlin(kotlinSrcFiles: Sequence<File>): Try<Nothing?, String> {
         return try {
            failIfWrongPackage(kotlinSrcFiles)
            val kotlincFile = kotlinc.getDone().toTry().getOrSupply { fail(it) { "Kotlin compiler not available" } }
            val command = listOf(
               kotlincFile.relativeToApp(),
               "-d", compileDir.relativeToApp(),
               "-jdk-home", APP.location.child("java").relativeToApp(),
               "-jvm-target", Runtime.version().feature().toString(),
               "-progressive",
               "-Xno-call-assertions",
               "-Xno-param-assertions",
               "-Xjvm-default=all",
               "-Xuse-experimental=kotlin.Experimental",
               "-Xstring-concat=indy-with-constants",
               "-cp", '"' + computeClassPath() + '"',
               kotlinSrcFiles.joinToString(" ") { it.relativeToApp() }
            )

            logger.info("Compiling with command=${command.joinToString(" ")} ")

            val process = ProcessBuilder(command)
               .directory(APP.location)
               .redirectOutput(Redirect.PIPE)
               .redirectError(Redirect.PIPE)
               .start()

            process.waitFor(1, TimeUnit.MINUTES)

            val success = process.exitValue()
            val textStdout = process.inputStream.bufferedReader(UTF_8).readText().prettifyCompilerOutput()
            val textStdErr = process.errorStream.bufferedReader(UTF_8).readText().prettifyCompilerOutput()
            val isSuccess = success==0

            if (isSuccess) {
               logger.info { "Compilation succeeded$textStdout" }
               Try.ok()
            } else {
               logger.error { "Compilation failed$textStdout$textStdErr" }
               Try.error(textStdErr)
            }
         } catch (e: Exception) {
            logger.error(e) { "Compilation failed" }
            Try.error(e.message ?: "")
         }
      }

      private fun failIfWrongPackage(kotlinSrcFiles: Sequence<File>) {
         kotlinSrcFiles.forEach { f ->
            f.useLines {
               val packageDef = it.firstOrNull { it.startsWith("package") }.orEmpty()
               failIf(!packageDef.startsWith("package $packageName")) { "Class source file=$f package must be $packageName" }
            }
         }
      }
   }

   inner class Widgets: GlobalSubConfigDelegator("Widget management") {

      /** Compilation order. Prioritizes currently open widgets. */
      private val compilationOrder: Comparator<WidgetMonitor>
         get() {
            val openFactories = findAll(OPEN).map { it.factory.id }.toSet()
            val isOpen = WidgetMonitor::widgetName compose openFactories::contains compose { if (it) 0 else 1 }
            return compareBy<WidgetMonitor> { 0 }.thenBy(isOpen).thenBy(WidgetMonitor::widgetName)
         }
      /** Plugin management ui. */
      private var settings by c(this).singleton()
         .def(name = "Widgets", info = "Manage application widgets")
      val autoRecompile by cv(true)
         .def(name = "Auto-compilation", info = "Automatic compilation and reloading of widgets when their source code changes")
      val recompile by cr { monitors.sortedWith(compilationOrder).forEach { it.scheduleCompilation() } }
         .def(name = "Recompile all widgets", info = "Re-compiles every widget. Useful when auto-compilation is disabled or unsupported.")
      val separateWidgets by cv(true)
         .def(name = "Separate widgets & templates in UI", info = "Show widgets and templates (exported layouts) as separate categories in UI picker")

      // TODO: use MapLike object with get/set operators
      private val componentLastOpenStrategies by cList<String>()
         .def(
            name = "Last used 'Open component' load strategy",
            info = "Last used 'Open component' load strategy per component",
            editable = EditMode.APP
         )
      val componentLastOpenStrategiesMap: Map<String, ComponentLoaderStrategy>
         get() = componentLastOpenStrategies.associate { it.substringBefore("|") to ComponentLoaderStrategy.valueOf(it.substringAfter("|")) }
      fun componentLastOpenStrategiesMap(id: String, strategy:ComponentLoaderStrategy) {
         val m = componentLastOpenStrategiesMap + (id to strategy)
         componentLastOpenStrategies setTo m.entries.map { (a, b) -> "$a|$b" }
      }

      /** @return widgets based on search criteria */
      fun findAll(source: WidgetSource): Sequence<Widget> = layouts.findAll(source).flatMap { it.allWidgets.asSequence() }

      /**
       * Returns widget fulfilling condition. Any widget can be returned (if it fulfills the condition), but:
       * * if there exists preferred widget it will always be returned first
       * * if no widget is available it will be attempted to be created if allowed
       * * if created, preferred factory will be used first
       * * if all methods fail, null is returned
       *
       * @param filter condition the widget must fulfill
       * @param source where and how the widget will be found/constructed
       * @return widget fulfilling condition or null otherwise
       */
      fun find(filter: (WidgetInfo) -> Boolean, source: WidgetUse): Widget? {

         val preferred by lazy {
            factories.getFactories()
               .filter(filter)
               .filter { !it.isIgnored }
               .filter { it.isPreferred }
               .toSet()
         }

         val widgets = widgets.findAll(source.widgetFinder)
            .filter { !it.forbidUse.value }
            .filter { filter(it.factory) }
            .toList()

         return null
            ?: widgets.firstOrNull { it.preferred.value }
            ?: widgets.firstOrNull { it.factory in preferred }
            ?: widgets.firstOrNull()
            ?: run {
               when (source) {
                  is WidgetUse.NewAnd -> {
                     val f = preferred.firstOrNull() ?: factories.getFactories().firstOrNull { !it.isIgnored && filter(it) }
                     val w = f?.create()
                     w?.let(source.layouter)
                     w
                  }
                  else -> null
               }
            }
      }

      /** Equivalent to: `find({ it.id()==name || it.name()==name }, source, ignore)` */
      fun find(name: String, source: WidgetUse): Widget? =
         find({ it.id==name || it.name==name }, source)

      /**
       * Roughly equivalent to: `find({ it.hasFeature(feature) }, source, ignore)`, but with type safety.
       * Controller is returned only if the widget is/has been loaded without any errors.
       */
      fun <F: Any> use(feature: KClass<F>, source: WidgetUse, action: (F) -> Unit) =
         find({ it.hasFeature(feature) }, source).focusWithWindow().filterIsControllerInstance(feature).ifNotNull(action).toUnit()

      /** Equivalent to: `use(T::class.java, source, action)` */
      inline fun <reified T: Any> use(source: WidgetUse, noinline action: (T) -> Unit) =
         use(T::class, source, action)

      /** Equivalent to: `find(cond, source).ifPresent(action)` */
      fun use(cond: (WidgetInfo) -> Boolean, source: WidgetUse, action: (Widget) -> Unit) =
         find(cond, source).focusWithWindow().ifNotNull(action).toUnit()

      fun use(name: String, source: WidgetUse, action: (Widget) -> Unit) =
         find(name, source).focusWithWindow().ifNotNull(action).toUnit()

      /** Select next widget or the first if no selected among the widgets in the specified window. */
      fun selectNextWidget(root: Container<*>) {
         val all = findAll(OPEN).asSequence().filter { it.rootParent===root }.toList()
         if (all.size<=1) return
         val i = Values.incrIndex(all, all.indexOfFirst { it.focused.value }.let { if (it==-1) 0 else it })
         all.getOrNull(i)?.focusAndTraverse()
      }

      /** Select previous widget or the first if no selected among the widgets in the specified window. */
      fun selectPreviousWidget(root: Container<*>) {
         val all = findAll(OPEN).asSequence().filter { it.rootParent===root }.toList()
         if (all.size<=1) return
         val iNew = Values.decrIndex(all, all.indexOfFirst { it.focused.value }.let { if (it==-1) 0 else it })
         all.getOrNull(iNew)?.focusAndTraverse()
      }

   }

   inner class Factories {

      /** Factories that are waiting to be compiled or are being compiled. */
      val factoriesInCompilation = observableArrayList<File>()!!

      /** @return all features implemented by at least one widget */
      fun getFeatures(): Sequence<Feature> = getFactories().flatMap { it.features.asSequence() }.distinct()

      /** @return widget factory with the specified [WidgetFactory.id] or null if none */
      fun getFactory(factoryId: String): Try<WidgetFactory<*>, String> = factoriesW[factoryId].toOptional().toTry { factoryId }

      /** @return widget factory with the specified [WidgetFactory.name] or null if none */
      fun getFactoryByName(name: String): WidgetFactory<*>? = factoriesW.find { it.name==name }

      /** @return component factory with the specified [ComponentFactory.name] or null if none */
      fun getComponentFactoryByName(name: String): ComponentFactory<*>? = getFactoryByName(name) ?: factoriesC[name]

      /** @return all widget factories */
      fun getFactories(): Sequence<WidgetFactory<*>> = factoriesW.streamV().asSequence()

      /** @return all widget factories, observable */
      fun getFactoriesObservable() = factoriesObservableW

      /** @return all component factories (including widget factories) */
      fun getComponentFactories(): Sequence<ComponentFactory<*>> = factoriesC.asSequence()

      /** @return all component factories (including widget factories), observable */
      fun getComponentFactoriesObservable() = factoriesObservableC

      //        /** @return all widget factories that create widgets with specified feature (see [Widgets.use]) */
      inline fun <reified FEATURE: Any> getFactoriesWith(): Sequence<FactoryRef<FEATURE>> = getFactoriesWith(FEATURE::class)

      //        /** @return all widget factories that create widgets with specified feature (see [Widgets.use]) */
      fun <FEATURE: Any> getFactoriesWith(feature: KClass<FEATURE>) =
         factoriesW.streamV().asSequence().filter { it.hasFeature(feature) }.map { FactoryRef(it, feature) }

      /**
       * Register the specified factory.
       *
       * The factory will be immediately available both programmatically and in UI.
       * Component instances of this factory (presumably in 'no factory' state) will be reloaded.
       * Must be called from FX thread.
       */
      fun register(factory: ComponentFactory<*>) {
         failIfNotFxThread()

         registerFactory(factory)
         if (factory is WidgetFactory<*>) factory.reloadAllOpen()
      }

      /**
       * Unregister the specified factory.
       * Must be called from FX thread.
       */
      fun unregister(factory: ComponentFactory<*>) {
         failIfNotFxThread()

         unregisterFactory(factory)
      }

      fun recompile(factory: WidgetFactory<*>) {
         monitors[factory.location]?.scheduleCompilation?.invoke()
      }
   }

   @Suppress("RedundantInnerClassModifier")
   inner class Layouts {

      /** @return layout of focused window or null if no window focused */
      fun findActive(): Layout? = APP.windowManager.getFocused()?.layout

      /** @return layouts based on search criteria */
      fun findAll(source: WidgetSource): Sequence<Layout> = when (source) {
         NONE -> sequenceOf()
         OPEN_LAYOUT -> APP.windowManager.windows.asSequence().mapNotNull { it.layout }.filter { !it.isStandalone }
         OPEN_STANDALONE -> WindowFX.getWindows().asSequence().mapNotNull { it.asLayout() }.filter { it.isStandalone }
         OPEN -> findAll(OPEN_STANDALONE) + findAll(OPEN_LAYOUT)
      }

   }

   /** Lazy reified reference to a factory of widget with a feature, enabling convenient use of its feature */
   data class FactoryRef<out FEATURE: Any> constructor(val name: String, val id: String, private val feature: KClass<FEATURE>): NameUi {
      override val nameUi = name

      fun use(source: WidgetUse, action: (FEATURE) -> Unit) {
         APP.widgetManager.widgets
            .find(id, source)
            .focusWithWindow()
            .filterIsControllerInstance(feature)
            .ifNotNull(action).toUnit()
      }

      /** @return reference to a factory of a widget derived from this but with the specified [feature] */
      inline fun <reified FEATURE2: Any> withFeature() = FactoryRef(name, id, FEATURE2::class)

      companion object {
         operator fun <FEATURE: Any>invoke(factory: WidgetFactory<*>, feature: KClass<FEATURE>) = FactoryRef(factory.name, factory.id, feature)

         operator fun invoke(name: String, id: String): FactoryRef<*> = FactoryRef(name, id, Any::class)
      }
   }

   companion object: KLogging() {

      /** @return new instance of a class represented by specified class file using one shot class loader or null if error */
      private fun loadClass(classFqName: String, compileDir: File, libFiles: Sequence<File>): Try<Class<*>, Throwable> {
         return createControllerClassLoader(compileDir, libFiles).andAlso {
            try {
               runTry {
                  it.loadClass(classFqName)
               }
            } catch (t: LinkageError) {
               Try.error(t)
            }
         }
      }

      /** @return new class loader using specified files to load classes and resources from or null if error */
      private fun createControllerClassLoader(compileDir: File, libFiles: Sequence<File>): Try<ClassLoader, Throwable> = runTry {
         (libFiles + compileDir).map { it.toURI().toURL() }.asArray() let_ ::URLClassLoader
      }

      private fun Widget?.focusWithWindow() = this?.apply { window?.requestFocus(); focus(); }

      private fun <R: Any> Widget?.filterIsControllerInstance(type: KClass<R>): R? = this?.controller.takeIf(type::isInstance)?.let(type::cast)

      private fun Collection<File>.lastModifiedMax() = asSequence().map { it.lastModified() }.maxOrNull()

      private fun Collection<File>.lastModifiedMin() = asSequence().map { it.lastModified() }.minOrNull()

      private infix fun Collection<File>.modifiedAfter(that: Collection<File>) = (this.lastModifiedMax() ?: 0)>=(that.lastModifiedMax() ?: 0)

      private fun File.relativeToApp() = relativeTo(APP.location).path

      private fun String.prettifyCompilerOutput() = if (isNullOrBlank()) "" else "\n$this"

      private val Os.classpathSeparator
         get() = when (this) {
            Os.WINDOWS -> ";"
            else -> ":"
         }
   }

}

/** Reified version of [WidgetInfo.hasFeature] */
inline fun <reified F> WidgetInfo.hasFeature() = hasFeature(F::class)

/** @return the factory or [NoFactoryFactory] with [NoFactoryFactory.factoryId] set to the error value */
fun Try<WidgetFactory<*>, String>.orNone(): WidgetFactory<*> = getOrSupply { NoFactoryFactory(it) }

fun WidgetFactory<*>.isCompiling(disposer: Disposer): ObservableValue<Boolean> {
   fun isCompiling() = APP.widgetManager.factories.factoriesInCompilation.any { location==it }
   return v(isCompiling()).apply {
      APP.widgetManager.factories.factoriesInCompilation.onChange { value = isCompiling() } on disposer
   }
}

fun WidgetFactory<*>.reloadAllOpen() = also { widgetFactory ->
   failIfNotFxThread()
   WidgetManager.logger.info("Reloading all open widgets of {}", widgetFactory)

   APP.widgetManager.widgets.findAll(OPEN).asSequence()
      .filter { it.factory.id==widgetFactory.id }
      .filter { it.isLoaded }
      .materialize()
      .forEach {
         val widgetOld = it
         val widgetNew = widgetFactory.createRecompiled(widgetOld.id).apply {
            setStateFrom(widgetOld)
            forceLoading = true
         }
         val wasFocused = widgetOld.focused.value
         val widgetOldInputs = widgetOld.controller!!.io.i.getInputs().associate { it.name to it.value }
         fun Widget.restoreAuxiliaryState() {
            forceLoading = false
            failIf(controller==null)
            controller!!.io.i.getInputs().forEach { i -> widgetOldInputs[i.name].ifNotNull { i.valueAny = it } }
            if (wasFocused) focus()
         }

         val p = widgetOld.parent
         if (p!=null) {
            val i = widgetOld.indexInParent()
            p.removeChild(i)
            p.addChild(i, widgetNew)
            widgetNew.restoreAuxiliaryState()
         } else {
            val parent = widgetOld.graphics!!.parent
            val i = parent.childrenUnmodifiable.indexOf(widgetOld.graphics!!)
            widgetOld.close()
            parent.asIf<Pane?>()?.children?.add(i, widgetNew.load())
            widgetNew.restoreAuxiliaryState()
         }
      }
}

/** Source for widgets when looking for a widget. */
enum class WidgetSource {
   /** None. No widget will be found. */
   NONE,
   /** All open widgets. [OPEN_LAYOUT] + [OPEN_STANDALONE]. */
   OPEN,
   /** All open widgets within any layout of any window. */
   OPEN_LAYOUT,
   /** All open standalone widgets - widgets not part of the layout, such as in popups. */
   OPEN_STANDALONE
}

/** Enumeration of parameter-less [ComponentLoader]s, that could be dynamically chosen to opening a widget .*/
enum class ComponentLoaderStrategy(val loader: ComponentLoader) {
   WINDOW(ComponentLoader.WINDOW),
   WINDOW_FULLSCREEN(ComponentLoader.WINDOW_FULLSCREEN_ACTIVE),
   OVERLAY(ComponentLoader.OVERLAY),
   DOCK(ComponentLoader.DOCK),
   POPUP(ComponentLoader.POPUP)
}

/** Strategy for opening a new component in ui. */
@Suppress("ClassName")
sealed class ComponentLoader: (Component) -> Any {
   /** Does not load component and leaves it upon the consumer to load and manage it appropriately. */
   object CUSTOM: ComponentLoader() {
      override operator fun invoke(c: Component) = Unit
   }

   /** Loads the component in a layout of a new window. */
   object WINDOW: ComponentLoader() {
      override operator fun invoke(c: Component): Window = APP.windowManager.showWindow(c)
   }

   /** Loads the component in a layout of a new window. */
   object DOCK: ComponentLoader() {
      override operator fun invoke(c: Component): Window = APP.windowManager.slideWindow(c)
   }

   /** Loads the component as a standalone in a simplified layout of a new always on top fullscreen window on active screen. */
   object WINDOW_FULLSCREEN_ACTIVE: ComponentLoader() {
      override operator fun invoke(c: Component): Stage = WINDOW_FULLSCREEN(getScreenForMouse())(c)
   }

   /** Loads the component as a standalone in a simplified layout of a new always on top fullscreen window on specified screen. */
   object WINDOW_FULLSCREEN {
      operator fun invoke(screen: Screen): (Component) -> Stage = { c ->
         val root = anchorPane()
         val window = Util.createFMNTStage(screen, false).apply {
            scene = Scene(root)
            onEventUp(WINDOW_HIDING) { c.rootParent?.close() }
         }

         root.onEventUp(KEY_PRESSED, ESCAPE) { window.hide() }

         c.load().apply {
            minPrefMaxWidth = Region.USE_COMPUTED_SIZE
         }
         window.show()
         Layout.openStandalone(root).apply {
            child = SwitchContainer().apply {
               addChild(0, c)
            }
         }

         window.showingProperty().sync1If({ it }) {
            c.focus()
         }
         window
      }
   }

   /** Loads the component as a standalone in a new [OverlayPane]. */
   object OVERLAY: ComponentLoader() {
      override operator fun invoke(c: Component): OverlayPane<Unit> {

         val op = object: OverlayPane<Unit>() {
            lateinit var layout: Layout

            init {
               onShowing += {
                  stage!!.installWindowInteraction()::unsubscribe.let(onHidden::addSOnetime)
                  stage!!.properties[Window.keyWindowLayout] = layout
                  layout.focus()
               }
               onHidden += {
                  properties -= Window.keyWindowLayout
                  layout.focus()
                  removeFromParent()
               }
            }

            override fun show(data: Unit) {
               content = anchorPane {
                  padding = Insets(20.0.emScaled)
                  layout = Layout.openStandalone(this).apply {
                     child = SwitchContainer().apply {
                        addChild(0, c)
                     }
                  }
               }
               if (c is Widget) {
                  val parent = this
                  c.ui = object: ComponentUi {
                     override val root = parent
                     override fun show() {}
                     override fun hide() {}
                  }
               }
               super.show()
            }

         }
         op.display.value = SCREEN_OF_MOUSE
         op.displayBgr.value = APP.ui.viewDisplayBgr.value
         op.show(Unit)
         op.makeResizableByUser()
         c.load().apply {
            // autosize()
            prefWidth(900.0)
            prefHeight(700.0)
         }
         c.focus()

         return op
      }
   }

   /** Loads the component as a standalone widget in a simplified layout of a new popup. */
   object POPUP: ComponentLoader() {
      override operator fun invoke(c: Component): PopWindow {
         val l = Layout.openStandalone(anchorPane())
         val p = PopWindow().apply {
            content.value = l.root
            title.value = c.name
            root.installWindowInteraction()::unsubscribe.let(onHiding::addSOnetime)
            properties[Window.keyWindowLayout] = l
            onHiding += { properties -= Window.keyWindowLayout }
            onHiding += { l.close() }
         }

         l.child = SwitchContainer().apply {
            addChild(0, c)
         }
         c.focus()

         p.show(ShowArea.WINDOW_ACTIVE(Pos.CENTER))
         return p
      }
   }
}

/** Strategy for using a widget. This can be an existing widget or even one to be created on demand. */
@Suppress("ClassName")
sealed class WidgetUse(val widgetFinder: WidgetSource) {
   /** Use open widget as per [WidgetSource.OPEN_LAYOUT] or do nothing if none available. */
   object OPEN_LAYOUT: WidgetUse(WidgetSource.OPEN_LAYOUT)

   /** Use open widget as per [WidgetSource.OPEN_STANDALONE] or do nothing if none available. */
   object OPEN_STANDALONE: WidgetUse(WidgetSource.OPEN_STANDALONE)

   /** Use open widget as per [WidgetSource.OPEN] or do nothing if none available. */
   object OPEN: WidgetUse(WidgetSource.OPEN)

   /** Use newly created widget. */
   object NEW: NewAnd(NONE, ComponentLoader.POPUP)

   /** Use open widget as per [WidgetSource.OPEN] or use newly created widget. */
   object ANY: NewAnd(WidgetSource.OPEN, ComponentLoader.POPUP)

   /** Use open widget as per [WidgetSource.OPEN_STANDALONE] or use newly created widget. */
   object NO_LAYOUT: NewAnd(WidgetSource.OPEN_STANDALONE, ComponentLoader.POPUP)

   open class NewAnd(widgetFinder: WidgetSource, val layouter: (Component) -> Any): WidgetUse(widgetFinder) {
      operator fun invoke(layouter: (Component) -> Any) = NewAnd(widgetFinder, layouter)
   }
}