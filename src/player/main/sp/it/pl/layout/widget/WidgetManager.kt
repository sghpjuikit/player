package sp.it.pl.layout.widget

import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections.observableArrayList
import javafx.scene.Scene
import javafx.scene.input.KeyCode.ESCAPE
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.layout.Pane
import javafx.scene.layout.Region
import javafx.stage.Screen
import javafx.stage.WindowEvent.WINDOW_HIDING
import mu.KLogging
import sp.it.pl.gui.objects.window.stage.Window
import sp.it.pl.layout.Component
import sp.it.pl.layout.container.Container
import sp.it.pl.layout.container.Layout
import sp.it.pl.layout.widget.WidgetSource.NONE
import sp.it.pl.layout.widget.WidgetSource.OPEN
import sp.it.pl.layout.widget.WidgetSource.OPEN_LAYOUT
import sp.it.pl.layout.widget.WidgetSource.OPEN_STANDALONE
import sp.it.pl.layout.widget.controller.Controller
import sp.it.pl.layout.widget.feature.Feature
import sp.it.pl.main.APP
import sp.it.pl.main.thenWithAppProgress
import sp.it.util.access.Values
import sp.it.util.access.v
import sp.it.util.async.FX
import sp.it.util.async.burstTPExecutor
import sp.it.util.async.executor.EventReducer
import sp.it.util.async.future.Fut.Companion.fut
import sp.it.util.async.threadFactory
import sp.it.util.collections.mapset.MapSet
import sp.it.util.collections.materialize
import sp.it.util.collections.setTo
import sp.it.util.conf.EditMode
import sp.it.util.conf.IsConfig
import sp.it.util.conf.IsConfigurable
import sp.it.util.conf.c
import sp.it.util.conf.cr
import sp.it.util.conf.cv
import sp.it.util.conf.readOnlyUnless
import sp.it.util.dev.Idempotent
import sp.it.util.dev.failIfNotFxThread
import sp.it.util.file.FileMonitor
import sp.it.util.file.Util.isValidatedDirectory
import sp.it.util.file.childOf
import sp.it.util.file.div
import sp.it.util.file.hasExtension
import sp.it.util.file.isAnyChildOf
import sp.it.util.file.isAnyParentOf
import sp.it.util.file.isParentOf
import sp.it.util.file.listChildren
import sp.it.util.file.seqChildren
import sp.it.util.file.toURLOrNull
import sp.it.util.functional.Try
import sp.it.util.functional.asArray
import sp.it.util.functional.asIf
import sp.it.util.functional.getOrSupply
import sp.it.util.functional.ifNull
import sp.it.util.functional.invoke
import sp.it.util.functional.orNull
import sp.it.util.functional.toUnit
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.on
import sp.it.util.reactive.onChange
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.sync1If
import sp.it.util.system.Os
import sp.it.util.type.isSubclassOf
import sp.it.util.ui.Util
import sp.it.util.ui.anchorPane
import sp.it.util.ui.minPrefMaxWidth
import sp.it.util.ui.stylesheetToggle
import sp.it.util.units.seconds
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.ProcessBuilder.Redirect
import java.net.URLClassLoader
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.WatchEvent.Kind
import java.util.Optional
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import javax.tools.ToolProvider
import kotlin.math.ceil
import kotlin.streams.asSequence
import kotlin.streams.asStream
import kotlin.streams.toList

/** Handles operations with Widgets. */
class WidgetManager(private val userErrorLogger: (String) -> Unit) {

    /** Public API for layout management. */
    @JvmField val layouts = Layouts()
    /** Public API for factory management. */
    @JvmField val factories = Factories()
    /** Public API for widget management. */
    @JvmField val widgets = Widgets()
    /** All component factories by their name. */
    private val factoriesC = MapSet<String, ComponentFactory<*>> { it.nameGui() }
    /** All widget factories by their name. */
    private val factoriesW = MapSet<String, WidgetFactory<*>> { it.name() }
    /** All widget directories by their name. */
    private val monitors = MapSet<String, WidgetMonitor> { it.widgetName }
    /** Separates entries of a java classpath argument, passed to JVM. */
    private var classpathSeparator = Os.current.classpathSeparator
    private var initialized = false
    private val compilerThread by lazy { burstTPExecutor(ceil(Runtime.getRuntime().availableProcessors()/4.0).toInt(), 30.seconds, threadFactory("widgetCompiler", true)) }

    fun init() {
        if (initialized) return

        if (!(APP.DIR_APP/"java"/"bin").exists())
            logger.error { "Java development kit is missing. Please install JDK in ${APP.DIR_APP/"java"}" }
        if (!(APP.DIR_APP/"kotlinc"/"bin").exists())
            logger.error { "Kotlin compiler is missing. Please install kotlinc in ${APP.DIR_APP/"kotlinc"}" }

        // internal factories
        factoriesW += emptyWidgetFactory
        factoriesC += emptyWidgetFactory

        // external factories
        val dirW = APP.DIR_WIDGETS
        if (!isValidatedDirectory(dirW)) {
            logger.error { "External widgets registration failed: $dirW is not a valid directory." }
        } else {
            dirW.listChildren().filter { it.isDirectory }.forEach { widgetDir ->
                val name = widgetDir.nameWithoutExtension.capitalize()
                monitors.computeIfAbsent(name) { WidgetMonitor(name, widgetDir) }.updateFactory()
            }

            FileMonitor.monitorDirectory(dirW, true) { type, f ->
                when {
                    dirW==f -> {}
                    dirW isParentOf f -> {
                        val name = f.nameWithoutExtension.capitalize()
                        if (type===ENTRY_CREATE) {
                            if (f.isDirectory) {
                                monitors.computeIfAbsent(name) { WidgetMonitor(name, f) }.updateFactory()
                            }
                        }
                        if (type===ENTRY_DELETE) {
                            monitors[name]?.dispose()
                        }
                    }
                    else -> monitors.find { it.widgetDir isAnyParentOf f }?.handleResourceChange(type, f)
                }
            }
        }

        // external factories for .fxwl widgets - serialized widgets
        val dirL = APP.DIR_LAYOUTS
        if (!isValidatedDirectory(dirL)) {
            logger.error { "External .fxwl widgets registration failed." }
        } else {
            dirL.walkTopDown().filter { it hasExtension "fxwl" }.forEach { factoriesC put DeserializingFactory(it) }

            FileMonitor.monitorDirectory(dirL, true, { it hasExtension "fxwl" }) { type, f ->
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

        factoriesC.forEach { logger.info { "Registered widget=${it.nameGui()}" } }
        initialized = true
    }

    private fun registerFactory(factory: ComponentFactory<*>): Boolean {
        logger.info { "Registering $factory" }

        if (factory is WidgetFactory<*>) factoriesW put factory
        return factoriesC put factory
    }

    private fun unregisterFactory(factory: ComponentFactory<*>) {
        logger.info { "Unregistering $factory" }

        if (factory is WidgetFactory<*>) factoriesW -= factory
        factoriesC -= factory
    }

    private inner class WidgetMonitor constructor(val widgetName: String, val widgetDir: File): ExternalWidgetFactoryData {
        val skinFile = widgetDir/"skin.css"
        val compileDir = widgetDir/"out"
        override val scheduleCompilation = EventReducer.toLast<Void>(500.0) { compileFx() }

        /** @return primary source file (either Kotlin or Java) or null if none exists */
        fun findSrcFile() = null
                ?: widgetDir.childOf("$widgetName.kt").takeIf { it.exists() }
                ?: widgetDir.childOf("$widgetName.java").takeIf { it.exists() }

        fun findSrcFiles() = widgetDir.seqChildren().filter { it.hasExtension("java", "kt") }

        fun findClassFile() = compileDir/widgetName.decapitalize()/"$widgetName.class"

        fun findClassFiles() = compileDir.walk().filter { it hasExtension "class" }

        fun computeClassPath(): String = computeClassPathElements().joinToString(classpathSeparator)

        private fun computeClassPathElements() = getAppJarFile()+(findAppLibFiles()+compileDir+findLibFiles()).map { it.relativeToApp() }

        private fun findLibFiles() = widgetDir.seqChildren().filterSourceJars()

        private fun findAppLibFiles() = APP.DIR_APP.childOf("lib").seqChildren().filterSourceJars()

        private fun getAppJarFile(): Sequence<String> {
            val mainJarFile = APP.DIR_APP/"PlayerFX.jar"
            return if (mainJarFile.exists()) {
                sequenceOf(mainJarFile.relativeToApp())
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
                                .filter { it.name==widgetName }
                                .forEach {
                                    val root = it.root
                                    val skinUrl = skinFile.toURLOrNull()?.toExternalForm()
                                    if (skinUrl!=null) {
                                        root?.stylesheetToggle(skinUrl, false)
                                        root?.stylesheetToggle(skinUrl, true)
                                    }
                                }
                    }
                    file==compileDir || file.isAnyChildOf(compileDir) -> {}
                    file hasExtension "class" -> {}
                    else -> {
                        logger.info { "Widget=$widgetName source file=${file.name} changed $type" }
                        if (widgets.autoRecompile.value && widgets.autoRecompileSupported)
                            scheduleCompilation()
                    }
                }
            }
        }

        @Idempotent
        fun dispose() {
            factories.factoriesInCompilation -= widgetName
            factoriesW.removeKey(widgetName)
            factoriesC.removeKey(widgetName)
            monitors.removeKey(widgetName)
        }

        @Suppress("ConstantConditionIf")
        fun updateFactory() {
            val srcFile = findSrcFile()
            val srcFiles = findSrcFiles().toList()
            val srcFilesKt = srcFiles.filter { it hasExtension "kt" }
            val srcFilesJava = srcFiles.filter { it hasExtension "java" }
            val classFile = findClassFile()
            val classFiles = findClassFiles().toList()
            val classFilesKt = classFiles.filter { cf -> srcFilesKt.any { sf -> sf.nameWithoutExtension==cf.nameWithoutExtension } }
            val classFilesJava = classFiles.filter { cf -> srcFilesJava.any { sf -> sf.nameWithoutExtension==cf.nameWithoutExtension } }

            val srcFileAvailable = srcFile!=null
            val classFileAvailableKt = classFilesKt modifiedAfter srcFilesKt
            val classFileAvailableJava = classFilesJava modifiedAfter srcFilesJava
            val classFileAvailable = classFile.exists() && classFileAvailableKt && classFileAvailableJava

            logger.info { "Widget=$widgetName factory update, source files available=$srcFileAvailable class files available=$classFileAvailable" }

            if (classFileAvailable) {
                val controllerType = loadClass(widgetDir.nameWithoutExtension, classFile, compileDir, findLibFiles())
                registerFactory(controllerType)
            } else if (srcFileAvailable) {
                compileFx()
            }
        }

        @Suppress("UNCHECKED_CAST")
        private fun registerFactory(controllerType: Class<*>?) {
            if (controllerType==null) {
                logger.warn { "Widget class $controllerType is null" }
                return
            }
            if (!controllerType.isSubclassOf<Controller>()) {
                logger.warn { "Widget class $controllerType in $widgetDir does not implement Controller" }
                return
            }

            val widgetType = (controllerType as Class<Controller>).kotlin
            val widgetFactory = WidgetFactory(widgetType, widgetDir, this)
            val isNew = registerFactory(widgetFactory)
            if (!isNew) widgetFactory.reloadAllOpen()
        }

        private fun compileFx() {
            failIfNotFxThread()

            factories.factoriesInCompilation += widgetName
            fut().thenWithAppProgress(compilerThread, "Compiling $widgetName") {
                compile()
            }.onDone(FX) {
                factories.factoriesInCompilation -= widgetName
                it.toTry()
                        .ifError { logger.error(it) { "Widget $widgetName failed to compile" } }
                        .getOrSupply { Try.error(it.message ?: "Unspecified error") }
                        .ifOk { updateFactory() }
                        .ifError { userErrorLogger("Widget $widgetName failed to compile. Reason: $it") }
            }
        }

        private fun compile(): Try<Nothing?, String> {
            logger.info { "Widget=$widgetName compiling..." }

            // TODO: enable. For some reason the delete-create fails sometimes (even though compilation can proceed fine without it), investigate & fix
            if (compileDir.exists()) compileDir.deleteRecursively()//.ifFalse { return Try.error("Failed to delete $compileDir") }
            compileDir.mkdirs()//.ifFalse { return Try.error("Failed to create $compileDir") }

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
            val options = sequenceOf(
                    "-encoding", APP.encoding.name(),
                    "-d", compileDir.relativeToApp(),
                    "-Xlint",
                    "-Xlint:-path",
                    "-Xlint:-processing",
                    "-cp", computeClassPath()
            )
            val sourceFiles = javaSrcFiles.map { it.path }
            val arguments = (options+sourceFiles).asArray()

            logger.info("Compiling with arguments=${arguments.joinToString(" ")}")
            val compiler = ToolProvider.getSystemJavaCompiler() ?: run {
                logger.error { "Compilation failed\nJava system compiler not available" }
                return Try.error("Java system compiler not available")
            }

            val streamStdOut = ByteArrayOutputStream(1000)
            val streamStdErr = ByteArrayOutputStream(1000)
            val success = compiler.run(null, streamStdOut, streamStdErr, *arguments)
            val isSuccess = success==0
            val textStdOut = streamStdOut.toString(Charsets.UTF_8).prettifyCompilerOutput()
            val textStdErr = streamStdErr.toString(Charsets.UTF_8).prettifyCompilerOutput()

            return if (isSuccess) {
                logger.info { "Compilation succeeded$textStdOut" }
                Try.ok()
            } else {
                logger.error { "Compilation failed$textStdErr" }
                Try.error(textStdErr)
            }
        }

        /** Compiles specified .kt files into .class files. */
        private fun compileKotlin(kotlinSrcFiles: Sequence<File>): Try<Nothing?, String> {
            try {
                val compilerFile = when (Os.current) {
                    Os.UNIX -> APP.DIR_APP/"kotlinc"/"bin"/"kotlinc"
                    else -> APP.DIR_APP/"kotlinc"/"bin"/"kotlinc.bat"
                }
                val command = listOf(
                        compilerFile.absolutePath,
                        "-d", compileDir.relativeToApp(),
                        "-jdk-home", APP.DIR_APP.childOf("java").relativeToApp(),
                        "-jvm-target", "12",
                        "-cp", computeClassPath(),
                        kotlinSrcFiles.joinToString(" ") { it.relativeToApp() }
                )

                logger.info("Compiling with command=${command.joinToString(" ")} ")

                val process = ProcessBuilder(command)
                        .directory(APP.DIR_APP)
                        .redirectOutput(Redirect.PIPE)
                        .redirectError(Redirect.PIPE)
                        .start()

                process.waitFor(1, TimeUnit.MINUTES)

                val success = process.exitValue()
                val textStdout = process.inputStream.bufferedReader(Charsets.UTF_8).readText().prettifyCompilerOutput()
                val textStdErr = process.errorStream.bufferedReader(Charsets.UTF_8).readText().prettifyCompilerOutput()
                val isSuccess = success==0

                return if (isSuccess) {
                    logger.info { "Compilation succeeded$textStdout" }
                    Try.ok()
                } else {
                    logger.error { "Compilation failed$textStdout$textStdErr" }
                    Try.error(textStdErr)
                }
            } catch (e: Exception) {
                logger.error(e) { "Compilation failed" }
                return Try.error(e.message ?: "")
            }
        }
    }

    @IsConfigurable("Widgets")
    inner class Widgets {

        @IsConfig(name = "Auto-compilation supported", info = "On some system, this feature may be unsupported", editable = EditMode.NONE)
        val autoRecompileSupported by c(Os.WINDOWS.isCurrent)

        @IsConfig(name = "Auto-compilation", info = "Automatic compilation and reloading of widgets when their source code changes")
        val autoRecompile by cv(true).readOnlyUnless(autoRecompileSupported)

        @IsConfig(name = "Recompile all widgets", info = "Re-compiles every widget. Useful when auto-compilation is disabled or unsupported.")
        val recompile by cr { monitors.forEach { it.scheduleCompilation() } }

        @IsConfig(name = "Separate widgets & templates in UI", info = "Show widgets and templates (exported layouts) as separate categoriesin UI picker")
        val separateWidgets by cv(true)

        /** Widgets that are not part of layout. */
        private val standaloneWidgets: MutableList<Widget> = ArrayList()

        fun initAsStandalone(widget: Widget) {
            standaloneWidgets += widget
            widget.onClose += { standaloneWidgets -= widget }
        }

        fun findAll(source: WidgetSource): Stream<Widget> = when (source) {
            NONE -> Stream.empty()
            OPEN_LAYOUT -> layouts.findAllActive().asStream().flatMap { it.allWidgets }
            OPEN_STANDALONE -> standaloneWidgets.stream()
            OPEN -> Stream.concat(findAll(OPEN_STANDALONE), findAll(OPEN_LAYOUT))
        }

        /**
         * Returns widget fulfilling condition. Any widget can be returned (if it fulfills the condition), but:
         * * if there exists preferred widget it will always be returned first
         * * if no widget is available it will be attempted to be created if allowed
         * * if created, preferred factory will be used first
         * * if all methods fail, null is returned
         *
         * @param filter condition the widget must fulfill
         * @param source where and how the widget will be found/constructed
         * @return optional of widget fulfilling condition or empty if not available
         */
        fun find(filter: (WidgetInfo) -> Boolean, source: WidgetUse): Optional<Widget> {

            val preferred by lazy {
                factories.getFactories()
                    .filter(filter)
                    .filter { !it.isIgnored }
                    .filter { it.isPreferred }
                    .firstOrNull()?.nameGui()
            }

            val widgets = widgets.findAll(source.widgetFinder)
                    .filter { filter(it.info) }
                    .filter { !it.forbid_use.value }
                    .filter { if (preferred==null) true else it.info.nameGui()==preferred }
                    .toList()

            val out: Widget? = null
                    ?: widgets.find { it.preferred.value }
                    ?: widgets.firstOrNull()
                    ?: run {
                        if (source is WidgetUse.NewAnd) {
                            factories.getFactories()
                                    .filter(filter)
                                    .filter { !it.isIgnored }
                                    .filter { if (preferred==null) true else it.nameGui()==preferred }
                                    .firstOrNull()
                                    ?.create()?.also(source.layouter)
                        } else {
                            null
                        }
                    }

            return Optional.ofNullable(out)
        }

        /** Equivalent to: `find({ it.name()==name || it.nameGui()==name }, source, ignore)` */
        fun find(name: String, source: WidgetUse): Optional<Widget> =
                find({ it.name()==name || it.nameGui()==name }, source)

        /**
         * Roughly equivalent to: `find({ it.hasFeature(feature) }, source, ignore)`, but with type safety.
         * Controller is returned only if the widget is/has been loaded without any errors.
         */
        fun <F> use(feature: Class<F>, source: WidgetUse, action: (F) -> Unit) =
                find({ it.hasFeature(feature) }, source).filterIsControllerInstance(feature).ifPresent(action)

        /** Equivalent to: `use(T::class.java, source, action)` */
        inline fun <reified T> use(source: WidgetUse, noinline action: (T) -> Unit) =
                use(T::class.java, source, action)

        /** Equivalent to: `find(cond, source).ifPresent(action)` */
        fun use(cond: (WidgetInfo) -> Boolean, source: WidgetUse, action: (Widget) -> Unit) =
                find(cond, source).ifPresent(action)

        fun use(name: String, source: WidgetUse, action: (Widget) -> Unit) =
                find(name, source).ifPresent(action)

        /** Select next widget or the first if no selected among the widgets in the specified window. */
        fun selectNextWidget(root: Container<*>) {
            val all = findAll(OPEN).asSequence().filter { it.rootParent===root }.toList()
            if (all.size<=1) return
            val i = Values.incrIndex(all, all.indexOfFirst { it.focused.value }.let { if (it==-1) 0 else it })
            all.getOrNull(i)?.focus()
        }

        /** Select previous widget or the first if no selected among the widgets in the specified window. */
        fun selectPreviousWidget(root: Container<*>) {
            val all = findAll(OPEN).asSequence().filter { it.rootParent===root }.toList()
            if (all.size<=1) return
            val iNew = Values.decrIndex(all, all.indexOfFirst { it.focused.value }.let { if (it==-1) 0 else it })
            all.getOrNull(iNew)?.focus()
        }

    }

    inner class Factories {

        /** Factories that are waiting to be compiled or are being compiled. */
        val factoriesInCompilation = observableArrayList<String>()!!

        fun recompile(factory: WidgetFactory<*>) = monitors[factory.name()]?.scheduleCompilation()

        /** @return all features implemented by at least one widget */
        fun getFeatures(): Sequence<Feature> = getFactories().flatMap { it.getFeatures().asSequence() }.distinct()

        /** @return widget factory with the specified [WidgetFactory.name] or null if none */
        fun getFactory(name: String): WidgetFactory<*>? = factoriesW[name]

        /** @return widget factory with the specified [WidgetFactory.nameGui] or null if none */
        fun getFactoryByGuiName(guiName: String): WidgetFactory<*>? = factoriesW.find { it.nameGui()==guiName }

        /** @return component factory with the specified [ComponentFactory.nameGui] or null if none */
        fun getComponentFactoryByGuiName(guiName: String): ComponentFactory<*>? = getFactoryByGuiName(guiName) ?: factoriesC[guiName]

        /** @return all widget factories */
        fun getFactories(): Sequence<WidgetFactory<*>> = factoriesW.streamV().asSequence()

        /** @return all component factories (including widget factories) */
        fun getComponentFactories(): Sequence<ComponentFactory<*>> = (factoriesC.asSequence()+getFactories()).distinct()

//        /** @return all widget factories that create widgets with specified feature (see [Widgets.use]) */
        inline fun <reified FEATURE> getFactoriesWith(): Sequence<FactoryRef<FEATURE>> = getFactoriesWith(FEATURE::class.java).asSequence()

//        /** @return all widget factories that create widgets with specified feature (see [Widgets.use]) */
        fun <FEATURE> getFactoriesWith(feature: Class<FEATURE>) =
                factoriesW.streamV().filter { it.hasFeature(feature) }.map { FactoryRef<FEATURE>(it) }!!
    }

    inner class Layouts {

        private val layoutsAvailable = ArrayList<String>()

        /** @return layout of focused window or null if no window focused */
        fun findActive(): Layout? = APP.windowManager.getFocused().orNull()?.layout

        /** @return all Layouts in the application */
        fun findAllActive(): Sequence<Layout> = APP.windowManager.windows.asSequence().mapNotNull { it.layout }

        /** @return all names of all layouts available to the application, including serialized layouts in files. */
        fun getAllNames(): Sequence<String> {
            findPersisted()
            return (findAllActive().map { it.name } + layoutsAvailable.asSequence()).distinct()
        }

        /** @return layouts found in layout folder and sets them as available layouts */
        fun findPersisted() {
            val dir = APP.DIR_LAYOUTS
            if (!isValidatedDirectory(dir)) {
                logger.error { "Layout directory=$dir not accessible" }
                return
            }

            layoutsAvailable setTo dir.seqChildren().filter { it hasExtension "l" }.map { it.nameWithoutExtension }
        }
    }

    /** Reified reference to a factory of widget with a feature, enabling convenient use of its feature */
    inner class FactoryRef<out FEATURE>(private val factory: WidgetFactory<*>) {
        fun nameGui() = factory.nameGui()

        @Suppress("UNCHECKED_CAST")
        fun use(source: WidgetUse, action: (FEATURE) -> Unit) = widgets
                .find(nameGui(), source)
                .filterIsControllerInstance(factory.controllerType)
                .map { it as FEATURE }  // if controller is factory.controllerType then it is also FEATURE
                .ifPresent(action)
    }

    companion object: KLogging() {

        fun WidgetFactory<*>.scheduleCompilation() = APP.widgetManager.monitors[nameGui()]!!.scheduleCompilation()

        /** @return new instance of a class represented by specified class file using one shot class loader or null if error */
        private fun loadClass(widgetName: String, classFile: File, compileDir: File, libFiles: Sequence<File>): Class<*>? {
            val className = "$widgetName.${classFile.nameWithoutExtension}"

            return try {
                createControllerClassLoader(compileDir, libFiles)
                        .ifNull { logger.info { "Class loading failed for $classFile" } }
                        ?.loadClass(className)
            } catch (e: ClassNotFoundException) {
                logger.info(e) { "Class loading failed for $classFile" }
                null
            }
        }

        /** @return new class loader using specified files to load classes and resources from or null if error */
        private fun createControllerClassLoader(compileDir: File, libFiles: Sequence<File>): ClassLoader? {
            return (libFiles+compileDir)
                    .map { it.toURLOrNull().ifNull { logger.error { "Failed to construct class loader due to invalid URL of file=$it" } } }
                    .asArray()
                    .takeIf { it.all { it!=null } }
                    ?.let { URLClassLoader(it) }
        }

        private fun <R> Optional<Widget>.filterIsControllerInstance(type: Class<R>): Optional<R> =
                map { it.controller }.filter(type::isInstance).map { type.cast(it) }

        private fun Collection<File>.lastModifiedMax() = asSequence().map { it.lastModified() }.max()

        private fun Collection<File>.lastModifiedMin() = asSequence().map { it.lastModified() }.min()

        private infix fun Collection<File>.modifiedAfter(that: Collection<File>) = (this.lastModifiedMax() ?: 0)>=(that.lastModifiedMax() ?: 0)

        private fun File.relativeToApp() = relativeTo(APP.DIR_APP).path

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

/** @return this factory or [emptyWidgetFactory] if null */
fun WidgetFactory<*>?.orEmpty(): WidgetFactory<*> = this ?: emptyWidgetFactory

/** @return this factory or [emptyWidgetFactory] if null */
fun ComponentFactory<*>?.orEmpty(): ComponentFactory<*> = this ?: emptyWidgetFactory

fun WidgetFactory<*>.isCompiling(disposer: Disposer): ObservableValue<Boolean> {
    fun isCompiling() = name() in APP.widgetManager.factories.factoriesInCompilation
    return v(isCompiling()).apply {
        APP.widgetManager.factories.factoriesInCompilation.onChange { value = isCompiling() } on disposer
    }
}

fun WidgetFactory<*>.reloadAllOpen() = also { widgetFactory ->
    WidgetManager.logger.info("Reloading all open widgets of {}", widgetFactory)
    APP.widgetManager.widgets.findAll(OPEN).asSequence()
            .filter { it.name==widgetFactory.name() }   // it.factory must not be used due to temporary factories in unrecognized widgets
            .materialize()
            .forEach { widgetOld ->
                val wasFocused = widgetOld.focused.value
                val widgetNew = widgetFactory.create()
                widgetNew.setStateFrom(widgetOld)
                val p = widgetOld.parent
                if (p!=null) {
                    val i = widgetOld.indexInParent()
                    p.removeChild(i)
                    p.addChild(i, widgetNew)
                    if (wasFocused) widgetNew.focus()
                } else {
                    val parent = widgetOld.graphics.parent
                    val i = parent.childrenUnmodifiable.indexOf(widgetOld.graphics)
                    widgetOld.close()
                    parent?.asIf<Pane>()?.let { it.children.add(i, widgetNew.load()) }
                    if (wasFocused) widgetNew.focus()
                }
            }
}

interface ExternalWidgetFactoryData {
    val scheduleCompilation: EventReducer<Void>
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

/** Strategy for opening a new widget in ui. */
@Suppress("ClassName")
sealed class WidgetLoader: (Widget) -> Unit {
    /**
     * Does not load widget and leaves it upon the consumer to load and manage it appropriately.
     * Note that widget loaded as standalone should first invoke [WidgetManager.Widgets.initAsStandalone].
     */
    object CUSTOM: WidgetLoader() {
        override fun invoke(w: Widget) {}
    }
    /** Loads the widget in a layout of a new window. */
    object WINDOW: WidgetLoader() {
        override fun invoke(w: Widget) = invoke(w as Component).toUnit()
        fun invoke(w: Component): Window = APP.windowManager.showWindow(w)
    }
    /** Loads the widget as a standalone widget in a simplified layout of a new always on top fullscreen window. */
    object WINDOW_FULLSCREEN {
        operator fun invoke(screen: Screen): (Widget) -> Unit = { w ->
            APP.widgetManager.widgets.initAsStandalone(w)

            val root = anchorPane()
            val window = Util.createFMNTStage(screen, false).apply {
                scene = Scene(root)
                onEventUp(WINDOW_HIDING) { w.rootParent.close() }
            }

            root.onEventUp(KEY_PRESSED, ESCAPE) { window.hide() }

            w.load().apply {
                minPrefMaxWidth = Region.USE_COMPUTED_SIZE
            }
            window.show()
            Layout.openStandalone(root).apply {
                child = w
            }

            window.showingProperty().sync1If({ it }) {
                w.focus()
            }
        }
    }
    /** Loads the widget as a standalone widget in a simplified layout of a new popup. */
    object POPUP: WidgetLoader() {
        override fun invoke(w: Widget) {
            APP.widgetManager.widgets.initAsStandalone(w)
            APP.windowManager.showFloating(w)
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
    object NEW: NewAnd(NONE, WidgetLoader.POPUP)
    /** Use open widget as per [WidgetSource.OPEN] or use newly created widget. */
    object ANY: NewAnd(WidgetSource.OPEN, WidgetLoader.POPUP)
    /** Use open widget as per [WidgetSource.OPEN_STANDALONE] or use newly created widget. */
    object NO_LAYOUT: NewAnd(WidgetSource.OPEN_STANDALONE, WidgetLoader.POPUP)

    open class NewAnd(widgetFinder: WidgetSource, val layouter: (Widget) -> Unit): WidgetUse(widgetFinder) {
        operator fun invoke(layouter: (Widget) -> Unit) = NewAnd(widgetFinder, layouter)
    }
}