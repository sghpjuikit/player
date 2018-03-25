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
import sp.it.pl.main.AppUtil.APP
import sp.it.pl.util.access.SequentialValue
import sp.it.pl.util.async.executor.EventReducer
import sp.it.pl.util.async.runFX
import sp.it.pl.util.async.runNew
import sp.it.pl.util.collections.mapset.MapSet
import sp.it.pl.util.dev.Idempotent
import sp.it.pl.util.file.FileMonitor
import sp.it.pl.util.file.Util.isValidatedDirectory
import sp.it.pl.util.file.childOf
import sp.it.pl.util.file.endsWithSuffix
import sp.it.pl.util.file.isAnyParentOf
import sp.it.pl.util.file.isParentOf
import sp.it.pl.util.file.listChildren
import sp.it.pl.util.file.parentDir
import sp.it.pl.util.file.parentDirOrRoot
import sp.it.pl.util.file.seqChildren
import sp.it.pl.util.file.toURLOrNull
import sp.it.pl.util.functional.Try
import sp.it.pl.util.functional.asArray
import sp.it.pl.util.functional.clearSet
import sp.it.pl.util.functional.orNull
import sp.it.pl.util.functional.runIf
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

    fun init() {
        if (initialized) return

        if (!APP.DIR_APP.childOf("jre", "bin").exists())
            logger.error { "Java development kit is missing. Please install JDK in ${APP.DIR_APP.childOf("jre")}" }
        if (!APP.DIR_APP.childOf("kotlinc", "bin").exists())
            logger.error { "Kotlin compiler is missing. Please install kotlinc in ${APP.DIR_APP.childOf("kotlinc")}" }

        // internal factories
        factoriesW += emptyWidgetFactory
        factoriesC += emptyWidgetFactory

        // external factories
        val dirW = APP.DIR_WIDGETS
        if (!isValidatedDirectory(dirW)) {
            logger.error { "External widgets registration failed." }
        } else {
            dirW.listChildren()
                    .filter { it.isDirectory }
                    .forEach { widgetDir ->
                        val name = widgetDir.nameWithoutExtension.capitalize()
                        monitors.computeIfAbsent(name) { WidgetDir(name, widgetDir) }.registerExternalFactory()
                    }

            FileMonitor.monitorDirectory(dirW, true) { type, f ->
                if (dirW==f) {

                } else if (dirW.isParentOf(f)) {
                    val name = f.nameWithoutExtension.capitalize()
                    if (type===ENTRY_CREATE) {
                        monitors.computeIfAbsent(name) { WidgetDir(name, f) }.registerExternalFactory()
                    } else if (type===ENTRY_DELETE) {
                        val wd = monitors.get(name)
                        wd?.dispose()
                    }
                } else {
                    monitors.find { it.widgetDir.isAnyParentOf(f) }?.handleResourceChange(type, f)
                }
            }
        }

        // external factories for .fxwl widgets - serialized widgets
        val dirL = APP.DIR_LAYOUTS
        if (!isValidatedDirectory(dirL)) {
            logger.error { "External .fxwl widgets registration failed." }
        } else {
            dirL.listChildren()
                    .filter { it endsWithSuffix "fxwl" }
                    .filter { it.useLines { it.take(1).any { it.startsWith("<Widget") } } }
                    .forEach { fxwl -> factoriesC.computeIfAbsent(fxwl.nameWithoutExtension.capitalize()) { DeserializingFactory(fxwl) } }

            FileMonitor.monitorDirsFiles(dirL, { it endsWithSuffix "fxwl" }) { type, fxwl ->
                if (type===ENTRY_CREATE) {
                    registerFactory(DeserializingFactory(fxwl))
                }
                if (type===ENTRY_DELETE) {
                    factoriesC.asSequence()
                            .filterIsInstance<DeserializingFactory>()
                            .filter { it.launcher==fxwl }
                            .toSet()    // materialize iteration to avoid concurrent modification
                            .forEach { unregisterFactory(it) }
                }
            }
        }

        factoriesW.forEach { logger.info { "Registered widget=${it.nameGui()}" } }
        factoriesC.forEach { logger.info { "Registered widget=${it.nameGui()}" } }
        initialized = true
    }

    private fun registerFactory(factory: ComponentFactory<*>): Boolean {
        logger.info { "Registering $factory" }

        if (factory is WidgetFactory<*>) factoriesW put factory
        val exists = factory in factoriesC
        factoriesC put factory
        return exists
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
        val wasReplaced = registerFactory(widgetFactory)
        if (wasReplaced) {
            logger.info("Reloading all open widgets of {}", widgetFactory)
            widgets.findAll(OPEN).asSequence()
                    .filter { it.info.name()==widgetFactory.name() } // can not rely on type since we just reloaded the class!
                    .toList()    // avoids possible concurrent modification
                    .forEach { widgetOld ->
                        val i = widgetOld.indexInParent()  // TODO: avoid null, MUST be wrapped within container!
                        val widgetNew = widgetFactory.create()
                        widgetNew.setStateFrom(widgetOld)
                        if (i!=null) {
                            val c = widgetOld.parent
                            c.removeChild(i)
                            c.addChild(i, widgetNew)
                        }
                    }
        }
    }

    private inner class WidgetDir internal constructor(val widgetName: String, val widgetDir: File) {
        private val skinFile = File(widgetDir, "skin.css")
        private val scheduleRefresh = EventReducer.toLast<Void>(500.0, Runnable { runFX(Runnable { this.registerExternalFactory() }) })
        private val scheduleCompilation = EventReducer.toLast<Void>(250.0, Runnable { runNew { compile().ifError(userErrorLogger) } })

        /** @return primary source file (either Kotlin or Java) or null if none exists */
        fun findSrcFile() = null
                ?: widgetDir.childOf("$widgetName.java").takeIf { it.exists() }
                ?: widgetDir.childOf("$widgetName.kt").takeIf { it.exists() }

        fun findSrcFiles() = widgetDir.seqChildren().filter { it.endsWithSuffix("java", "kt") }

        fun computeClassPath(): String = computeClassPathElements().joinToString(classpathSeparator)

        private fun computeClassPathElements() = sequenceOf(".")+getAppJarFile()+(findAppLibFiles()+findLibFiles()).map { it.absolutePath }

        private fun findLibFiles() = widgetDir.seqChildren().filterSourceJars()

        private fun findAppLibFiles() = APP.DIR_APP.childOf("lib").seqChildren().filterSourceJars()

        private fun getAppJarFile(): Sequence<String> {
            val mainJarFile = APP.DIR_APP.childOf("PlayerFX.jar")
            return if (mainJarFile.exists()) {
                sequenceOf(mainJarFile.absolutePath)
            } else {
                System.getProperty("java.class.path")
                        .splitToSequence(classpathSeparator)
                        .filter { it.contains("build\\classes") || it.contains("build\\kotlin-classes") }
            }
        }

        private fun Sequence<File>.filterSourceJars() = this
                .filter { it endsWithSuffix "jar" }
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
                    file endsWithSuffix "class" -> {
                        logger.info { "Widget=$widgetName class file=${file.name} changed $type" }
                        scheduleRefresh.push(null)
                    }
                    else -> {
                        logger.info { "Widget=$widgetName source file=${file.name} changed $type" }
                        scheduleCompilation.push(null)
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
            logger.info { "Widget=$widgetName factory updating" }

            val srcFiles = findSrcFiles().toList()
            val classFile = widgetDir.childOf("$widgetName.class")
            val srcFile = findSrcFile()

            val srcFileAvailable = srcFile!=null
            val classFileAvailable = classFile.exists() && classFile.lastModified()>(srcFiles.lastModified() ?: 0)

            logger.info { "Widget=$widgetName source files available=$srcFileAvailable" }
            logger.info { "Widget=$widgetName class files available=$classFileAvailable" }

            if (classFileAvailable) {
                // If class file is available, we just create factory for it.
                val controllerType = loadClass(widgetDir.nameWithoutExtension, classFile, findLibFiles())
                createFactory(controllerType, widgetDir)
            } else if (srcFileAvailable) {
                // If only source file is available, compile
                if (initialized) {
                    // If we are initialized, app is running and some widget has been added (its factory
                    // does not exist yet), so we compile in the bgr. Dir monitoring will lead us back to this method
                    runNew { compile() }
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

            val srcFiles = findSrcFiles().toList()
            val isKotlin = srcFiles.any { it endsWithSuffix "kt" }
            val isJava = srcFiles.any { it endsWithSuffix "java" }
            val result = when {
                isJava && isKotlin -> Try.error("Mixed Kotlin-Java source code for widget is not supported")
                isKotlin -> compileKotlin(srcFiles.asSequence())
                else -> compileJava(srcFiles.asSequence())
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
                var compilerFile = APP.DIR_APP.childOf("kotlinc", "bin", "kotlinc.bat")
                if (Os.UNIX.isCurrent) compilerFile = APP.DIR_APP.childOf("kotlinc", "bin", "kotlinc")

                val command = ArrayList<String>()
                command.add(compilerFile.absolutePath)
                command.add("-d")
                command.add(APP.DIR_WIDGETS.absolutePath)
                command.add("-jdk-home")
                command.add(APP.DIR_APP.childOf("jre").absolutePath)
                command.add("-jvm-target")
                command.add("1.8")
                command.add("-cp")
                command.add(computeClassPath())
                command.add(kotlinSrcFiles.map { it.absolutePath }.joinToString(" "))

                logger.info("Compiling with command=${command.joinToString(" ")} ")

                // TODO: capture output to result
                val success = ProcessBuilder(command)
                        .directory(APP.DIR_APP.childOf("kotlinc", "bin"))
                        .redirectOutput(Redirect.INHERIT)
                        .redirectError(Redirect.INHERIT)
                        .start()
                        .waitFor()
                val isSuccess = success==0

                if (isSuccess) logger.info { "Compilation succeeded" }
                else logger.error { "Compilation failed" }

                return if (isSuccess) Try.ok()
                else Try.error("Compilation failed with errors")
            } catch (e: Exception) {
                logger.error { "Compilation failed" }
                return Try.error(e.message)
            }
        }
    }

    inner class Widgets {

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

        fun createNew(name: String) = factoriesW.find { it.name()==name }.orEmpty().create()   // TODO: no empty widget...

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

        /** Equivalent to: `getWidget(w->w.hasFeature(feature), source).map(w->(F)w.getController())` */
        @Suppress("UNCHECKED_CAST")
        fun <F> find(feature: Class<F>, source: WidgetSource, ignore: Boolean = false): Optional<F> =
                find({ it.hasFeature(feature) }, source, ignore).map { it.getController() as F }

        @JvmOverloads
        fun find(name: String, source: WidgetSource, ignore: Boolean = false): Optional<Widget<*>> =
                find({ it.name()==name || it.nameGui()==name }, source, ignore)

        /** Equivalent to: `getWidget(type, source).ifPresent(action)` */
        fun <F> use(feature: Class<F>, source: WidgetSource, action: (F) -> Unit) =
                find(feature, source).ifPresent(action)

        inline fun <reified T> use(source: WidgetSource, noinline action: (T) -> Unit) =
                use(T::class.java, source, action)

        /** Equivalent to: `getWidget(cond, source).ifPresent(action)` */
        fun use(cond: (WidgetInfo) -> Boolean, source: WidgetSource, action: (Widget<*>) -> Unit) =
                find(cond, source).ifPresent(action)

        fun use(name: String, source: WidgetSource, ignore: Boolean = false, action: (Widget<*>) -> Unit) {
            widgets.find(name, source, ignore).ifPresent(action)
        }

        @Suppress("UNCHECKED_CAST")
        fun <F> use(factory: FactoryRef<F>, source: WidgetSource, ignore: Boolean = false, action: (F) -> Unit) {
            widgets.find(factory.nameGui(), source, ignore).ifPresent { action(it.getController() as F) }
        }

        /** Select next widget or the first if no selected among the widgets in the specified window. */
        fun selectNextWidget(root: Container<*>) {
            val all = findAll(OPEN).asSequence().filter { it.rootParent===root }.toList()
            if (all.size<=1) return
            val i = SequentialValue.incrIndex(all, all.indexOfFirst { it.focused.value })
            all.getOrNull(i)?.focus()
        }

        /** Select previous widget or the first if no selected among the widgets in the specified window. */
        fun selectPreviousWidget(root: Container<*>) {
            val all = findAll(OPEN).asSequence().filter { it.rootParent===root }.toList()
            if (all.size<=1) return
            val iNew = SequentialValue.decrIndex(all, all.indexOfFirst { it.focused.value })
            all.getOrNull(iNew)?.focus()
        }

    }

    inner class Factories {

        /** @return all features implemented by at least one widget */
        fun getFeatures(): Sequence<Feature> = getFactories().flatMap { it.getFeatures().asSequence() }.distinct()

        fun getFactory(name: String): ComponentFactory<*>? = factoriesW.get(name) ?: factoriesC.get(name)

        fun getFactoryOrEmpty(name: String): ComponentFactory<*> = getFactory(name) ?: emptyWidgetFactory

        /** @return all widget factories */
        fun getFactories(): Sequence<WidgetFactory<*>> = factoriesW.streamV().asSequence()

        /** @return all component factories (including widget factories) */
        fun getComponentFactories(): Sequence<ComponentFactory<*>> = (APP.widgetManager.factoriesC.asSequence()+getFactories()).distinct()

        /** @return all widget factories that create widgets with specified feature (see [Widgets.use]]) */
        inline fun <reified FEATURE> getFactoriesWith(): Sequence<FactoryRef<FEATURE>> = getFactoriesWith(FEATURE::class.java).asSequence()

        /** @return all widget factories that create widgets with specified feature (see [Widgets.use]]) */
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

            layoutsAvailable clearSet dir.seqChildren().filter { it endsWithSuffix "l" }.map { it.nameWithoutExtension }
        }
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

        private inline fun <T: Any?> T.ifNull(block: () -> Unit) = apply { if (this==null) block() }

        private fun Collection<File>.lastModified() = asSequence().map { it.lastModified() }.max()

        private val Os.classpathSeparator
            get() = when (this) { Os.WINDOWS -> ";"
                else -> ":"
            }
    }

}

/** Reified version of [WidgetInfo.hasFeature] */
inline fun <reified F> WidgetInfo.hasFeature() = hasFeature(F::class)

/** @return this factory or [emptyWidgetFactory] if null */
fun WidgetFactory<*>?.orEmpty(): WidgetFactory<*> = this ?: emptyWidgetFactory

/** Reified reference to a factory of widget with a feature, enabling convenient use of its feature */
class FactoryRef<out FEATURE>(private val factory: ComponentFactory<*>) {
    fun nameGui() = factory.nameGui()

    @Suppress("UNCHECKED_CAST")
    fun use(source: WidgetSource, ignore: Boolean = false, action: (FEATURE) -> Unit) = APP.widgetManager.widgets
                .find(nameGui(), source, ignore).orNull()
                ?.let { it.load(); action(it.getController() as FEATURE) }    // TODO fix it.load()
}

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