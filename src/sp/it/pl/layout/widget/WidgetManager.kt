package sp.it.pl.layout.widget

import javafx.scene.layout.Pane
import mu.KLogging
import sp.it.pl.gui.objects.window.stage.WindowManager
import sp.it.pl.layout.container.Container
import sp.it.pl.layout.container.layout.Layout
import sp.it.pl.layout.widget.WidgetSource.OPEN
import sp.it.pl.layout.widget.WidgetSource.OPEN_LAYOUT
import sp.it.pl.layout.widget.WidgetSource.OPEN_STANDALONE
import sp.it.pl.layout.widget.controller.Controller
import sp.it.pl.layout.widget.feature.Feature
import sp.it.pl.main.APP
import sp.it.pl.util.access.SequentialValue
import sp.it.pl.util.async.executor.EventReducer
import sp.it.pl.util.async.oneCachedThreadExecutor
import sp.it.pl.util.async.runFX
import sp.it.pl.util.async.runOn
import sp.it.pl.util.async.threadFactory
import sp.it.pl.util.collections.mapset.MapSet
import sp.it.pl.util.collections.materialize
import sp.it.pl.util.conf.EditMode
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.IsConfigurable
import sp.it.pl.util.conf.c
import sp.it.pl.util.conf.cr
import sp.it.pl.util.conf.cv
import sp.it.pl.util.conf.readOnlyUnless
import sp.it.pl.util.dev.Idempotent
import sp.it.pl.util.file.FileMonitor
import sp.it.pl.util.file.Util.isValidatedDirectory
import sp.it.pl.util.file.childOf
import sp.it.pl.util.file.div
import sp.it.pl.util.file.hasExtension
import sp.it.pl.util.file.isAnyParentOf
import sp.it.pl.util.file.isParentOf
import sp.it.pl.util.file.listChildren
import sp.it.pl.util.file.parentDir
import sp.it.pl.util.file.parentDirOrRoot
import sp.it.pl.util.file.seqChildren
import sp.it.pl.util.file.toURLOrNull
import sp.it.pl.util.functional.Try
import sp.it.pl.util.functional.asArray
import sp.it.pl.util.functional.ifNull
import sp.it.pl.util.functional.invoke
import sp.it.pl.util.functional.runIf
import sp.it.pl.util.functional.setTo
import sp.it.pl.util.math.seconds
import sp.it.pl.util.system.Os
import sp.it.pl.util.type.isSubclassOf
import java.io.File
import java.lang.ProcessBuilder.Redirect
import java.net.URLClassLoader
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.WatchEvent.Kind
import java.util.Optional
import java.util.stream.Stream
import javax.tools.ToolProvider
import kotlin.streams.asSequence
import kotlin.streams.asStream
import kotlin.streams.toList

/** Handles operations with Widgets. */
class WidgetManager(private val windowManager: WindowManager, private val userErrorLogger: (String) -> Unit) {

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
    private val monitors = MapSet<String, WidgetDir> { it.widgetName }
    /** Separates entries of a java classpath argument, passed to JVM. */
    private var classpathSeparator = Os.current.classpathSeparator
    private var initialized = false
    private val compilerThread by lazy { oneCachedThreadExecutor(30.seconds, threadFactory("widgetCompiler", true)) }

    fun init() {
        if (initialized) return

        if (!APP.DIR_APP.childOf("java", "bin").exists())
            logger.error { "Java development kit is missing. Please install JDK in ${APP.DIR_APP/"java"}" }
        if (!APP.DIR_APP.childOf("kotlinc", "bin").exists())
            logger.error { "Kotlin compiler is missing. Please install kotlinc in ${APP.DIR_APP/"kotlinc"}" }

        // internal factories
        factoriesW += emptyWidgetFactory
        factoriesC += emptyWidgetFactory

        // external factories
        val isMetaInfWidget: File.() -> Boolean = { path.contains("META-INF") }
        val dirW = APP.DIR_WIDGETS
        if (!isValidatedDirectory(dirW)) {
            logger.error { "External widgets registration failed." }
        } else {
            dirW.listChildren()
                    .filter { it.isDirectory && !it.isMetaInfWidget() }
                    .forEach { widgetDir ->
                        val name = widgetDir.nameWithoutExtension.capitalize()
                        monitors.computeIfAbsent(name) { WidgetDir(name, widgetDir) }.registerExternalFactory()
                    }

            FileMonitor.monitorDirectory(dirW, true) { type, f ->
                when {
                    dirW==f -> {}
                    dirW.isMetaInfWidget() -> {}
                    dirW.isParentOf(f) -> {
                        val name = f.nameWithoutExtension.capitalize()
                        if (type===ENTRY_CREATE) {
                            if (f.isDirectory) {
                                monitors.computeIfAbsent(name) { WidgetDir(name, f) }.registerExternalFactory()
                            }
                        } else if (type===ENTRY_DELETE) {
                            monitors[name]?.dispose()
                        }
                    }
                    else -> monitors.find { it.widgetDir.isAnyParentOf(f) }?.handleResourceChange(type, f)
                }
            }
        }

        // external factories for .fxwl widgets - serialized widgets
        val dirL = APP.DIR_LAYOUTS
        if (!isValidatedDirectory(dirL)) {
            logger.error { "External .fxwl widgets registration failed." }
        } else {
            dirL.listChildren()
                    .filter { it hasExtension "fxwl" }
                    .filter { it.useLines { it.take(1).any { it.startsWith("<Widget") } } }
                    .forEach { fxwl -> factoriesC.computeIfAbsent(fxwl.nameWithoutExtension.capitalize()) { DeserializingFactory(fxwl) } }

            FileMonitor.monitorDirsFiles(dirL, { it hasExtension "fxwl" }) { type, fxwl ->
                if (type===ENTRY_CREATE) {
                    registerFactory(DeserializingFactory(fxwl))
                }
                if (type===ENTRY_DELETE) {
                    factoriesC.asSequence()
                            .filterIsInstance<DeserializingFactory>()
                            .filter { it.launcher==fxwl }
                            .materialize()
                            .forEach { unregisterFactory(it) }
                }
            }
        }

        factoriesC.forEach { logger.info { "Registered widget=${it.nameGui()}" } }
        initialized = true
    }

    private fun registerFactory(factory: ComponentFactory<*>) {
        logger.info { "Registering $factory" }

        if (factory is WidgetFactory<*>) factoriesW put factory
        factoriesC put factory
    }

    private fun unregisterFactory(factory: ComponentFactory<*>) {
        logger.info { "Unregistering $factory" }

        if (factory is WidgetFactory<*>) factoriesW -= factory
        factoriesC -= factory
    }

    private fun createFactory(controllerType: Class<*>?, dir: File) {
        if (controllerType==null) {
            logger.warn { "Widget class $controllerType is null" }
            return
        }
        if (!controllerType.isSubclassOf<Controller>()) {
            logger.warn { "Widget class $controllerType in $dir does not implement Controller" }
            return
        }

        @Suppress("UNCHECKED_CAST")
        val widgetType = (controllerType as Class<Controller>).kotlin
        val widgetFactory = WidgetFactory(widgetType, dir)
        registerFactory(widgetFactory)

        logger.info("Reloading all open widgets of {}", widgetFactory)
        widgets.findAll(OPEN).asSequence()
                .filter { it.name==widgetFactory.name() } // can not rely on type since we just reloaded the class!
                .toList()    // avoids possible concurrent modification
                .forEach { widgetOld ->
                    val i = widgetOld.indexInParent()
                    val widgetNew = widgetFactory.create()
                    widgetNew.setStateFrom(widgetOld)
                    if (i!=null) {
                        val c = widgetOld.parent
                        c.removeChild(i)
                        c.addChild(i, widgetNew)
                    } else {
                        // TODO: implement
                    }
                }
    }

    private inner class WidgetDir constructor(val widgetName: String, val widgetDir: File) {
        val scheduleRefresh = EventReducer.toLast<Void>(500.0) { runFX { registerExternalFactory() } }
        val scheduleCompilation = EventReducer.toLast<Void>(500.0) { runOn(compilerThread) { compile().ifError(userErrorLogger) } }
        val skinFile = File(widgetDir, "skin.css")

        /** @return primary source file (either Kotlin or Java) or null if none exists */
        fun findSrcFile() = null
                ?: widgetDir.childOf("$widgetName.java").takeIf { it.exists() }
                ?: widgetDir.childOf("$widgetName.kt").takeIf { it.exists() }

        fun findSrcFiles() = widgetDir.seqChildren().filter { it.hasExtension("java", "kt") }

        fun findClassFile() = widgetDir/"$widgetName.class"

        fun findClassFiles() = widgetDir.seqChildren().filter { it hasExtension "class" }

        fun computeClassPath(): String = computeClassPathElements().joinToString(classpathSeparator)

        private fun computeClassPathElements() = sequenceOf(".")+getAppJarFile()+
                (findAppLibFiles()+sequenceOf(widgetDir)+findLibFiles()).map { it.relativeToApp() }

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
                        runFX {
                            widgets.findAll(OPEN)
                                    .filter { it.name==widgetName }
                                    .forEach {
                                        val root = it.root
                                        val skinUrl = skinFile.toURLOrNull()?.toExternalForm()
                                        if (root!=null && skinUrl!=null && root is Pane) {
                                            root.stylesheets -= skinUrl
                                            root.stylesheets += skinUrl
                                        }
                                    }
                        }
                    }
                    file hasExtension "class" -> {
                        logger.info { "Widget=$widgetName class file=${file.name} changed $type" }
                        if (widgets.autoRecompile.value && widgets.autoRecompileSupported)
                            scheduleRefresh()
                    }
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
            factoriesW.removeKey(widgetName)
            monitors.removeKey(widgetName)
        }

        fun registerExternalFactory() {
            val srcFile = findSrcFile()
            val srcFiles = findSrcFiles().toList()
            val srcFilesKt = srcFiles.filter { it hasExtension "kt" }
            val srcFilesJava = srcFiles.filter { it hasExtension "java" }
            val srcFilesKtExist = srcFilesKt.isNotEmpty()
            val srcFilesJavaExist = srcFilesJava.isNotEmpty()
            val classFile = findClassFile()
            val classFiles = findClassFiles().toList()
            val classFilesKt = classFiles.filter { cf -> srcFilesKt.any { sf -> sf.nameWithoutExtension==cf.nameWithoutExtension } }
            val classFilesJava = classFiles.filter { cf -> srcFilesJava.any { sf -> sf.nameWithoutExtension==cf.nameWithoutExtension } }

            val srcFileAvailable = srcFile!=null
            val classFileAvailableKt = classFilesKt modifiedAfter srcFilesKt
            val classFileAvailableJava = classFilesJava modifiedAfter srcFilesJava
            val classFileAvailable = classFile.exists() && classFileAvailableKt && classFileAvailableJava
            val isKtJavaCompilationRequired = srcFilesKtExist && srcFilesJavaExist
            val isKtJavaCompilationActive = isKtJavaCompilationRequired && (classFileAvailableKt xor classFileAvailableJava)

            if (isKtJavaCompilationActive) return

            logger.info { "Widget=$widgetName factory update, source files available=$srcFileAvailable class files available=$classFileAvailable" }

            if (classFileAvailable) {
                // If class file is available, we just create factory for it.
                val controllerType = loadClass(widgetDir.nameWithoutExtension, classFile, findLibFiles())
                createFactory(controllerType, widgetDir)
            } else if (srcFileAvailable) {
                // If only source file is available, compile
                if (initialized) {
                    // If we are initialized, app is running and some widget has been added (its factory
                    // does not exist yet), so we compile in the bgr. Dir monitoring will lead us back to this method
                    runOn(compilerThread) { compile() }
                } else {
                    // Else, we are initializing now, and we must be able to provide all factories before widgets start
                    // loading so we compile on this thread. This blocks and delays startup, but that is what we need.
                    // We register factory manually.
                    compile().ifOk { registerExternalFactory() }
                }
            }
        }

        private fun compile(): Try<Void, String> {
            logger.info { "Widget=$widgetName compiling..." }

            val srcFile = findSrcFile()
            val isKotlinMain = srcFile!=null && srcFile hasExtension "kt"
            val isJavaMain = srcFile!=null && srcFile hasExtension "java"
            val srcFiles = findSrcFiles().toList()
            val hasKotlin = srcFiles.any { it hasExtension "kt" }
            val hasJava = srcFiles.any { it hasExtension "java" }
            val result = when {
                hasJava && hasKotlin -> {
                    when {
                        isKotlinMain -> {
                            Try.ok<String>()
                                    .and { compileKotlin(srcFiles.asSequence().filter { it hasExtension "kt" }) }
                                    .and { compileJava(srcFiles.asSequence().filter { it hasExtension "java" }) }
                        }
                        isJavaMain -> Try.error("Mixed Kotlin-Java source code for Java-based widget is not supported")
                        else -> Try.error("No main widget source file available.")
                    }
                }
                hasKotlin -> compileKotlin(srcFiles.asSequence())
                hasJava -> compileJava(srcFiles.asSequence())
                else -> Try.error("No source files available")
            }
            return result.mapError { "Widget $widgetName failed to compile. Reason: $it" }
        }

        /** Compiles specified .java files into .class files. */
        private fun compileJava(javaSrcFiles: Sequence<File>): Try<Void, String> {
            val options = sequenceOf(
                    "-encoding", APP.encoding.name(),
                    "-Xlint",
                    "-Xlint:-path",
                    "-Xlint:-processing",
                    "-cp", computeClassPath()
            )
            val sourceFiles = javaSrcFiles.map { it.path }
            val arguments = (options+sourceFiles).asArray()

            logger.info("Compiling with arguments=${arguments.joinToString(" ")}")
            val compiler = ToolProvider.getSystemJavaCompiler()

            // TODO: capture output to result
            val success = compiler.run(null, null, null, *arguments)
            val isSuccess = success==0

            if (isSuccess) logger.info { "Compilation succeeded" }
            else logger.error { "Compilation failed" }

            return if (isSuccess) Try.ok() else Try.error("Widget $widgetName Compilation failed with errors")
        }

        /** Compiles specified .kt files into .class files. */
        private fun compileKotlin(kotlinSrcFiles: Sequence<File>): Try<Void, String> {
            try {
                val compilerFile = when (Os.current) {
                    Os.UNIX -> APP.DIR_APP/"kotlinc"/"bin"/"kotlinc"
                    else -> APP.DIR_APP/"kotlinc"/"bin"/"kotlinc.bat"
                }
                val command = listOf(
                        compilerFile.absolutePath,
                        "-d", APP.DIR_WIDGETS.relativeToApp(),
                        "-jdk-home", APP.DIR_APP.childOf("java").relativeToApp(),
                        "-jvm-target", "1.8",
                        "-cp", computeClassPath(),
                        kotlinSrcFiles.joinToString(" ") { it.relativeToApp() }
                )

                logger.info("Compiling with command=${command.joinToString(" ")} ")

                // TODO: capture output to result
                val success = ProcessBuilder(command)
                        .directory(APP.DIR_APP)
                        .redirectOutput(Redirect.INHERIT)
                        .redirectError(Redirect.INHERIT)
                        .start()
                        .waitFor()
                val isSuccess = success==0

                if (isSuccess) logger.info { "Compilation succeeded" }
                else logger.error { "Compilation failed" }

                return if (isSuccess) Try.ok() else Try.error("Compilation failed with errors")
            } catch (e: Exception) {
                logger.error { "Compilation failed" }
                return Try.error(e.message)
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

        /** Widgets that are not part of layout. */
        private val standaloneWidgets: MutableList<Widget<*>> = ArrayList()

        private fun Widget<*>.initAsStandalone() {
            standaloneWidgets += this
            onClose += { standaloneWidgets -= this }
        }

        fun findAll(source: WidgetSource): Stream<Widget<*>> = when (source) {
            WidgetSource.OPEN_LAYOUT -> layouts.findAllActive().flatMap { it.allWidgets }
            WidgetSource.OPEN_STANDALONE, WidgetSource.NO_LAYOUT -> standaloneWidgets.stream()
            WidgetSource.NEW -> Stream.empty()
            WidgetSource.OPEN, WidgetSource.ANY -> Stream.concat(findAll(OPEN_STANDALONE), findAll(OPEN_LAYOUT))
        }

        /**
         * Returns widget fulfilling condition. Any widget can be returned (if it fulfills the condition), but:
         * * if there exists preferred widget it will always be returned first
         * * if no widget is available it will be attempted to be created if allowed
         * * if created, preferred factory will be used first
         * * if all methods fail, null is returned
         *
         * Created widgets are displayed in a popup
         *
         * @param filter condition the widget must fulfill
         * @param source where and how the widget will be found/constructed
         * @param silent whether the widget will partake in application layout or simply constructed for manual use.
         * Use true if you want to use the widget as a custom graphics (note that its lifecycle is in developer's
         * hands). This is an advanced feature. Default false.
         *
         * @return optional of widget fulfilling condition or empty if not available
         */
        @JvmOverloads
        fun find(filter: (WidgetInfo) -> Boolean, source: WidgetSource, silent: Boolean = false): Optional<Widget<*>> {

            // get preferred type
            val preferred = factories.getFactories()
                    .filter(filter)
                    .filter { !it.isIgnored }
                    .filter { it.isPreferred }
                    .firstOrNull()?.nameGui()

            // get viable widgets - widgets of the feature & of preferred type if any
            val widgets = widgets.findAll(source)
                    .filter { filter(it.info) }
                    .filter { !it.forbid_use.value }
                    .filter { if (preferred==null) true else it.info.nameGui()==preferred }
                    .toList()

            val out: Widget<*>? = null
                    ?: widgets.find { it.preferred.value }
                    ?: widgets.firstOrNull()
                    ?: runIf(source.newWidgetsAllowed()) {
                        factories.getFactories()
                                .filter(filter)
                                .filter { !it.isIgnored }
                                .filter { if (preferred==null) true else it.nameGui()==preferred }
                                .firstOrNull()
                                ?.create()?.also {
                                    it.initAsStandalone()
                                    if (!silent) APP.windowManager.showFloating(it)
                                }
                    }

            return Optional.ofNullable(out)
        }

        /**
         * Roughly equivalent to: `find({ it.hasFeature(feature) }, source, ignore)`, but with type safety.
         * Controller is returned only the widget is/has been loaded without any errors.
         */
        @Suppress("UNCHECKED_CAST")
        @JvmOverloads
        fun <F> find(feature: Class<F>, source: WidgetSource, ignore: Boolean = false): Optional<F> =
                find({ it.hasFeature(feature) }, source, ignore).filterIsControllerInstance(feature)

        /** Equivalent to: `find({ it.name()==name || it.nameGui()==name }, source, ignore)` */
        @JvmOverloads
        fun find(name: String, source: WidgetSource, ignore: Boolean = false): Optional<Widget<*>> =
                find({ it.name()==name || it.nameGui()==name }, source, ignore)

        /** Equivalent to: `find(feature, source).ifPresent(action)` */
        fun <F> use(feature: Class<F>, source: WidgetSource, action: (F) -> Unit) =
                find(feature, source).ifPresent(action)

        /** Equivalent to: `use(T::class.java, source, action)` */
        inline fun <reified T> use(source: WidgetSource, noinline action: (T) -> Unit) =
                use(T::class.java, source, action)

        /** Equivalent to: `find(cond, source).ifPresent(action)` */
        fun use(cond: (WidgetInfo) -> Boolean, source: WidgetSource, action: (Widget<*>) -> Unit) =
                find(cond, source).ifPresent(action)

        fun use(name: String, source: WidgetSource, ignore: Boolean = false, action: (Widget<*>) -> Unit) =
                find(name, source, ignore).ifPresent(action)

        /** Select next widget or the first if no selected among the widgets in the specified window. */
        fun selectNextWidget(root: Container<*>) {
            val all = findAll(OPEN).asSequence().filter { it.rootParent===root }.toList()
            if (all.size<=1) return
            val i = SequentialValue.incrIndex(all, all.indexOfFirst { it.focused.value }.let { if (it==-1) 0 else it })
            all.getOrNull(i)?.focus()
        }

        /** Select previous widget or the first if no selected among the widgets in the specified window. */
        fun selectPreviousWidget(root: Container<*>) {
            val all = findAll(OPEN).asSequence().filter { it.rootParent===root }.toList()
            if (all.size<=1) return
            val iNew = SequentialValue.decrIndex(all, all.indexOfFirst { it.focused.value }.let { if (it==-1) 0 else it })
            all.getOrNull(iNew)?.focus()
        }

    }

    inner class Factories {

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
        fun getComponentFactories(): Sequence<ComponentFactory<*>> = (APP.widgetManager.factoriesC.asSequence()+getFactories()).distinct()

//        /** @return all widget factories that create widgets with specified feature (see [Widgets.use]) */
        inline fun <reified FEATURE> getFactoriesWith(): Sequence<FactoryRef<FEATURE>> = getFactoriesWith(FEATURE::class.java).asSequence()

//        /** @return all widget factories that create widgets with specified feature (see [Widgets.use]) */
        fun <FEATURE> getFactoriesWith(feature: Class<FEATURE>) =
                factoriesW.streamV().filter { it.hasFeature(feature) }.map { FactoryRef<FEATURE>(it) }!!
    }

    inner class Layouts {

        private val layoutsAvailable = ArrayList<String>()

        /** @return layout of focused window or null if no window focused */
        fun findActive(): Layout? = windowManager.focused.map { it.layout }.orElse(null)

        /** @return all Layouts in the application */
        fun findAllActive(): Stream<Layout> = windowManager.windows.asSequence().mapNotNull { it.layout }.asStream()

        /** @return all names of all layouts available to the application, including serialized layouts in files. */
        fun getAllNames(): Stream<String> {
            findPersisted()
            return Stream.concat(findAllActive().map { it.name }, layoutsAvailable.stream()).distinct()
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
        fun use(source: WidgetSource, ignore: Boolean = false, action: (FEATURE) -> Unit) = widgets
                .find(nameGui(), source, ignore)
                .filterIsControllerInstance(factory.controllerType)
                .map { it as FEATURE }  // if controller is factory.controllerType then it is also FEATURE
                .ifPresent(action)
    }

    companion object: KLogging() {

        /** @return new instance of a class represented by specified class file using one shot class loader or null if error */
        private fun loadClass(widgetName: String, classFile: File, libFiles: Sequence<File>): Class<*>? {
            val className = "$widgetName.${classFile.nameWithoutExtension}"
            val dir = classFile.parentDir!!

            return try {
                createControllerClassLoader(dir, libFiles)
                        .ifNull { logger.info { "Class loading failed for $classFile" } }
                        ?.loadClass(className)
            } catch (e: ClassNotFoundException) {
                logger.info(e) { "Class loading failed for $classFile" }
                null
            }
        }

        /** @return new class loader using specified files to load classes and resources from or null if error */
        private fun createControllerClassLoader(dir: File, libFiles: Sequence<File>): ClassLoader? {
            return (libFiles+dir.parentDirOrRoot)
                    .map { it.toURLOrNull().ifNull { logger.error { "Failed to construct class loader due to invalid URL of file=$it" } } }
                    .asArray()
                    .takeIf { it.all { it!=null } }
                    ?.let { URLClassLoader(it) }
        }

        private fun <R> Optional<Widget<*>>.filterIsControllerInstance(type: Class<R>): Optional<R> =
                map { it.controller }.filter(type::isInstance).map { type.cast(it) }

        private fun Collection<File>.lastModifiedMax() = asSequence().map { it.lastModified() }.max()

        private fun Collection<File>.lastModifiedMin() = asSequence().map { it.lastModified() }.min()

        private infix fun Collection<File>.modifiedAfter(that: Collection<File>) = (this.lastModifiedMax() ?: 0)>=(that.lastModifiedMax() ?: 0)

        private fun File.relativeToApp() = relativeTo(APP.DIR_APP).path

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

/** Source for widgets when looking for a widget. */
enum class WidgetSource {

    /** New widget will always be created. */
    NEW,

    /** All loaded widgets within any layout in any window. */
    OPEN_LAYOUT,

    /** All loaded standalone widgets - widgets not part of the layout, such as in popups. */
    OPEN_STANDALONE,

    /** [OPEN_LAYOUT] + [OPEN_STANDALONE]. Use when creating widget is not intended. */
    OPEN,

    /** [NEW] + [OPEN_LAYOUT] + [OPEN_STANDALONE]. */
    ANY,

    /** [NEW] + [OPEN_STANDALONE]. [NEW] that tries to reuse [OPEN_STANDALONE] if available. */
    NO_LAYOUT;

    fun newWidgetsAllowed(): Boolean = this==NO_LAYOUT || this==ANY || this==NEW

}

enum class WidgetTarget {
    LAYOUT,
    TAB,
    POPUP,
    WINDOW
}
