package sp.it.pl.layout.widget;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Stream;
import javafx.scene.layout.Pane;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sp.it.pl.gui.objects.window.stage.Window;
import sp.it.pl.gui.objects.window.stage.WindowManager;
import sp.it.pl.layout.container.Container;
import sp.it.pl.layout.container.layout.Layout;
import sp.it.pl.layout.widget.controller.Controller;
import sp.it.pl.layout.widget.feature.Feature;
import sp.it.pl.util.SwitchException;
import sp.it.pl.util.async.executor.EventReducer;
import sp.it.pl.util.collections.mapset.MapSet;
import sp.it.pl.util.dev.Idempotent;
import sp.it.pl.util.file.FileMonitor;
import sp.it.pl.util.file.Util;
import sp.it.pl.util.functional.Try;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static sp.it.pl.layout.widget.WidgetManager.WidgetSource.LAYOUT;
import static sp.it.pl.layout.widget.WidgetManager.WidgetSource.OPEN;
import static sp.it.pl.layout.widget.WidgetManager.WidgetSource.STANDALONE;
import static sp.it.pl.main.App.APP;
import static sp.it.pl.util.Util.capitalize;
import static sp.it.pl.util.async.AsyncKt.runFX;
import static sp.it.pl.util.async.AsyncKt.runNew;
import static sp.it.pl.util.file.Util.getName;
import static sp.it.pl.util.file.Util.readFileLines;
import static sp.it.pl.util.file.UtilKt.listChildren;
import static sp.it.pl.util.functional.Util.ISNTØ;
import static sp.it.pl.util.functional.Util.stream;
import static sp.it.pl.util.functional.Util.toS;

/**
 * Handles operations with Widgets.
 */
public final class WidgetManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(WidgetManager.class);

	/**
	 * Collection of valid widget factories by their name..
	 * <p/>
	 * Factories can be removed, added or swapped for new one in runtime. This happens when
	 * widgets files are discovered/deleted/modified.
	 */
	private final MapSet<String,ComponentFactory<?>> factoriesC = new MapSet<>(ComponentFactory::nameGui);
	public final MapSet<String,WidgetFactory<?>> factories = new MapSet<>(WidgetFactory::name);
	public final WidgetFactory<?> widgetFactoryEmpty = new WidgetFactory<>(EmptyWidget.class);
	private final MapSet<String,WidgetDir> monitors = new MapSet<>(wd -> wd.widgetName);
	private boolean initialized = false;
	private final WindowManager windowManager; // use App instead, but that requires different App initialization
	private final Consumer<? super String> userErrorLogger;

	public WidgetManager(WindowManager windowManager, Consumer<? super String> userErrorLogger) {
		this.windowManager = windowManager;
		this.userErrorLogger = userErrorLogger;
	}

	public void initialize() {
		if (initialized) throw new IllegalStateException("Already initialized");

		// internal factories
		factories.add(widgetFactoryEmpty);

		// external factories
		File dirW = APP.DIR_WIDGETS;
		if (!Util.isValidatedDirectory(dirW)) {
			LOGGER.error("External widgets registration failed.");
		} else {
			listChildren(dirW).filter(File::isDirectory).forEach(widgetDir -> {
				String name = capitalize(getName(widgetDir));
				monitors.computeIfAbsent(name, n -> new WidgetDir(name, widgetDir)).registerExternalFactory();
			});

			FileMonitor.monitorDirsFiles(dirW, File::isDirectory, (type, widget_dir) -> {
				String name = capitalize(getName(widget_dir));
				if (type == ENTRY_CREATE) {
					monitors.computeIfAbsent(name, n -> new WidgetDir(name, widget_dir)).registerExternalFactory();
				}
				if (type == ENTRY_DELETE) {
					WidgetDir wd = monitors.get(name);
					if (wd != null) wd.dispose();
				}
			});
		}

		// external factories for .fxwl widgets - serialized widgets
		File dirL = APP.DIR_LAYOUTS;
		if (!Util.isValidatedDirectory(dirL)) {
			LOGGER.error("External .fxwl widgets registration failed.");
		} else {
			listChildren(dirL).filter(f -> f.getPath().endsWith(".fxwl"))
						   .filter(f -> readFileLines(f).limit(1).anyMatch(line -> line.startsWith("<Widget")))
						   .forEach(fxwl -> factoriesC.computeIfAbsent(capitalize(getName(fxwl)), key -> new DeserializingFactory(fxwl)));

			FileMonitor.monitorDirsFiles(dirL, f -> f.getPath().endsWith(".fxwl"), (type, fxwl) -> {
				if (type == ENTRY_CREATE) {
					registerFactory(new DeserializingFactory(fxwl));
				}
				if (type == ENTRY_DELETE) {
					stream(factoriesC)
						.filter(DeserializingFactory.class::isInstance).map(DeserializingFactory.class::cast)
						.filter(f -> f.getLauncher().equals(fxwl))
						.collect(toSet())    // materialize iteration to avoid concurrent modification
						.forEach(this::unregisterFactory);
				}
			});
		}

		initialized = true;
	}

	@SuppressWarnings("unchecked")
	private void constructFactory(Class<?> controllerType, File dir) {
		if (controllerType==null) {
			LOGGER.warn("Widget class {} is null", dir);
			return;
		}
		if (!Controller.class.isAssignableFrom(controllerType)) {
			LOGGER.warn("Widget class {} {} does not implement Controller", controllerType,dir);
			return;
		}

		WidgetFactory<?> f = new WidgetFactory<>((Class<Controller<?>>) controllerType, dir);
		boolean was_replaced = registerFactory(f);

		if (was_replaced) {
			LOGGER.info("Reloading all open widgets of {}", f);
			findAll(OPEN)
				.filter(w -> w.getInfo().name().equals(f.name())) // can not rely on type since we just reloaded the class!
				.collect(toList()) // avoids possible concurrent modification
				.forEach(w -> {
					Widget<?> nw = f.create();
					nw.setStateFrom((Widget) w);
					Integer i = w.indexInParent();  // TODO: avoid null, MUST be wrapped within container!
					if (i!=null) {
						Container c = w.getParent();
						c.removeChild(i);
						c.addChild(i, nw);
					}
				});
		}
	}

	private boolean registerFactory(ComponentFactory<?> factory) {
		LOGGER.info("Registering {}", factory);
		if (factory instanceof WidgetFactory) {
			factories.add((WidgetFactory) factory);
		}
		boolean exists = factoriesC.containsValue(factory);
		factoriesC.add(factory);
		return exists;
	}

	private void unregisterFactory(ComponentFactory<?> factory) {
		LOGGER.info("Unregistering {}", factory);
		if (factory instanceof WidgetFactory) {
			factories.remove(factory);
		}
		factoriesC.remove(factory);
	}

	private class WidgetDir {
		private final String widgetName;
		private final File widgetDir;
		private final File classFile;
		private final File srcFile;
		private final File skinFile;
		private FileMonitor fileMonitor;
		private final EventReducer<Void> scheduleRefresh = EventReducer.toLast(500,
			() -> runFX(this::registerExternalFactory));
		private final EventReducer<Void> scheduleCompilation = EventReducer.toLast(250,
			() -> runNew(() -> compile(getSrcFiles(),getLibFiles()).ifError(userErrorLogger)));

		WidgetDir(String name, File dir) {
			widgetName = name;
			widgetDir = dir;
			classFile = new File(widgetDir, widgetName + ".class");
			srcFile = new File(widgetDir, widgetName + ".java");
			skinFile = new File(widgetDir,"skin.css");
		}

		@Idempotent
		void monitorStart() {
			if (fileMonitor!=null) return;
			fileMonitor = FileMonitor.monitorDirsFiles(widgetDir, file -> true, (type, file) -> {
				if (type==ENTRY_CREATE || type==ENTRY_MODIFY) {
					if (file.getPath().endsWith(".class")) {
						// monitor class file (only the main class' one) & recreate factory on change
						if (file.equals(classFile)) {
							LOGGER.info("Widget {} class file changed {}", widgetName, type);
							// Register factory, but
							// - on fx thread
							// - throttle subsequent events to avoid inconsistent state & overloading ui thread
							// - give the compilation some time to finish
							scheduleRefresh.push(null);
						}
					} else if (file.equals(skinFile)) {
						// monitor skin file & on change reload skin on all open widgets
						LOGGER.info("Widget {} SKIN file changed {}", widgetName,type);
						runFX(() ->
							findAll(OPEN).filter(w -> w.getName().equals(widgetName))
								.forEach(w -> {
									try {
										if (w.root instanceof Pane) {
											((Pane)w.root).getStylesheets().remove(skinFile.toURI().toURL().toExternalForm());
											((Pane)w.root).getStylesheets().add(skinFile.toURI().toURL().toExternalForm());
										}
									} catch (MalformedURLException ex) {
										java.util.logging.Logger.getLogger(WidgetManager.class.getName()).log(Level.SEVERE, null, ex);
									}
								})
						);
					} else {
						// monitor source files (any .java file) & recompile on change
						// Because the widget may be skinned (.css), loaded from fxml (.fxml) or use any kind of resource
						// (.jar, .txt, etc.), we will monitor all of the files except for .class, not just .java.
						LOGGER.info("Widget {} source file changed {}", file,type);
						// Compile source files, but
						// - on new thread
						// - throttle subsequent events (multiple files may be changed at once, multiple events per file
						//   may be thrown at once (when applications create tmp files while saving)
						scheduleCompilation.push(null);
					}
				}
			});
		}

		@Idempotent
		void monitorStop() {
			if (fileMonitor!=null) fileMonitor.stop();
			fileMonitor = null;
		}

		@Idempotent
		void dispose() {
			monitorStop();
			factories.removeKey(widgetName);
			monitors.removeKey(widgetName);
		}

		void registerExternalFactory() {
			File classFile = new File(widgetDir, widgetName + ".class");
			File srcFile = new File(widgetDir, widgetName + ".java");

			// Source file is available if exists
			// Class file is available if exists and source file does not. But if both do, class file must
			// not be outdated, which we check by modification time. This avoids nasty class version
			// errors as consequently we recompile the source file.
			boolean srcFile_available = srcFile.exists();
			boolean classFile_available = classFile.exists() && (!srcFile_available || classFile.lastModified()>getSrcFiles().mapToLong(File::lastModified).max().orElseGet(srcFile::lastModified));

			// If class file is available, we just create factory for it.
			if (classFile_available) {
				Class<?> controller_class = loadClass(getName(widgetDir), classFile, getLibFiles());
				constructFactory(controller_class, widgetDir);
			}

			// If only source file is available, compile
			else if (srcFile_available) {
				// If we are initialized, app is running and some widget has been added (its factory
				// does not exist yet), so we compile in the bgr as we should.
				// Else, we are initializing now, and we must be able to
				// provide all factories before widgets start loading so we compile on this (ui/fx)
				// thread. This blocks and delays startup, but it is fine - it is necessary
				if (initialized)
					runNew(() -> compile(getSrcFiles(),getLibFiles()));
				else {
					// File monitoring is not and must not be running yet (as it creates factory
					// asynchronously). We do it manually.
					// TODO: use assert
					compile(getSrcFiles(),getLibFiles()).ifOk(v -> registerExternalFactory());
				}
			}

			monitorStart();
		}

		Stream<File> getSrcFiles() {
			return listChildren(widgetDir).filter(f -> f.getPath().endsWith(".java"));
		}

		Stream<File> getClassFiles() {
			return listChildren(widgetDir).filter(f -> f.getPath().endsWith(".class"));
		}

		Stream<File> getLibFiles() {
			return stream(listChildren(widgetDir), listChildren(widgetDir.getParentFile()))
					   .filter(f -> f.getPath().endsWith(".jar"));
		}

	}

	// TODO: collect compiler error output & clean-up
	/**
	 * Compiles the .java file into .class file. All project dependencies (including the project
	 * itself - its jar) are available because they are on the classpath.
	 *
	 * @param srcFiles .java files to compile
	 */
	private Try<Void,String> compile(Stream<File> srcFiles, Stream<File> libFiles) {
		List<File> ffs = srcFiles.collect(toList());
		String classpath = System.getProperty("java.class.path");
		Stream<String> options = stream(                            // Command-line options
			// Compiler defaults to system encoding, we need consistent system independent encoding, such as UTF-8
			"-encoding",APP.encoding.name(),
			// Includes information about each class loaded and each source file compiled.
			// "-verbose"
			"-cp", classpath + ";" + libFiles.map(File::getAbsolutePath).collect(joining(";"))
		);
//        Stream<String> sourceFiles = srcFiles.map(File::getPath);   // One or more source files to be compiled
		Stream<String> sourceFiles = ffs.stream().map(File::getPath);   // One or more source files to be compiled
		Stream<String> classes = stream();   // One or more classes to be processed for annotations
		Stream<String> argFiles = stream();   // One or more files that lists options and source files. The -J options are not allowed in these files
		String[] arguments = stream(options,sourceFiles,classes,argFiles).flatMap(s -> s).toArray(String[]::new);

		LOGGER.info("Compiling with arguments: {} ", toS(", ", arguments));
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

//	    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
////        compiler.getStandardFileManager(diagnostics, Locale.getDefault(), App.APP.encoding);
//        compiler.getStandardFileManager(diagnostics, null, null);
		int success = compiler.run(null, null, null, arguments);

//	    StringBuilder sb = new StringBuilder();
//	    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
////        compiler.getStandardFileManager(diagnostics, Locale.getDefault(), App.APP.encoding);
//        compiler.getStandardFileManager(diagnostics, null, null);
//        int success = compiler.run(null, new OutputStream() {
//	        @Override
//	        public void write(int b) throws IOException {
//		        throw new AssertionError("Operation not allowed for performance reasons.");
//	        }
//
//	        @Override
//	        public void write(byte[] b, int off, int len) throws IOException {
//	        	sb.append(new String(b));
//	        }
//
//            }, null, arguments);


		//	    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
		//	    StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
		//	    Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromStrings(map(ffs, File::getPath));
		//	    JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null, compilationUnits);
		//	    boolean isSuccess = task.call();

//	    StringBuilder sb = new StringBuilder();
//	    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
//	    StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
//	    Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromStrings(map(ffs, File::getPath));
//	    JavaCompiler.CompilationTask task = compiler.getTask(new Writer() {
//		    @Override
//		    public void write(@NotNull char[] cbuf, int off, int len) throws IOException {
//			    sb.append(cbuf);
//		    }
//
//		    @Override
//		    public void flush() throws IOException {
//
//		    }
//
//		    @Override
//		    public void close() throws IOException {
//
//		    }
//	    }, fileManager, diagnostics, null, null, compilationUnits);
//	    boolean isSuccess = task.call();

		boolean isSuccess = success==0;
		if (isSuccess) LOGGER.info("Compilation succeeded");
		else LOGGER.error("Compilation failed");
		return isSuccess
			? Try.ok()
			: Try.error("Compilation failed with errors");
//			: Try.error(sb.toString());
//			: Try.error("Compilation failed with errors:\n\n" + stream(diagnostics.getDiagnostics())
//					.map(d -> "" +
//						"Kind: " + d.getKind() + "\n" +
//						"Code: " + d.getCode() + "\n" +
//						"Message :" + d.getMessage(null) + "\n" +
//						"Source :" + d.getSource().getName() + "\n" +
//						"Column number :" + d.getColumnNumber() + "\n" +
//						"Position start :" + d.getStartPosition() + "\n" +
//						"Position :" + d.getPosition() + "\n" +
//						"Position end :" + d.getEndPosition() + "\n" +
//						"Line: " + d.getLineNumber() + "\n"
//					)
//					.joining("\n\n"));
	}

	private static Class<?> loadClass(String widgetName, File classFile, Stream<File> libFiles) {
		try {
			File dir = classFile.getParentFile();
			String className = widgetName + "." + Util.getName(classFile);

			ClassLoader controllerClassloader = createControllerClassLoader(dir, libFiles);

			// debug - checks if the classloader can load the same class multiple times
			// boolean isDifferentClassInstance =
			//         createControllerClassLoader(dir, name_widget).loadClass(name_class) ==
			//         createControllerClassLoader(dir, name_widget).loadClass(name_class);
			// System.out.println(isDifferentClassInstance);

			return controllerClassloader.loadClass(className);
		} catch (ClassNotFoundException e) {
			LOGGER.info("FXML widget factory controller class loading failed for: " + classFile, e);
			return null;
		}
	}

	/**
	 * Creates class loader which first loads classes in the given directory.
	 * <p/>
	 * In order to support runtime class reloading, classes are not cached. Multiple instances of this class loader
	 * load different class even when loading the same class file!
	 */
	private static ClassLoader createControllerClassLoader(File widget_dir, Stream<File> libFiles) {
		File dir = widget_dir.getParentFile();
		URL[] classpath = stream(stream(dir), libFiles)
						 .map(f -> {
							 try {
								 return f.toURI().toURL();
							 } catch (MalformedURLException e) {
								 return null;
							 }
						 })
						 .filter(ISNTØ).toArray(URL[]::new);
		return new URLClassLoader(classpath);
	}

	/** @return all widget factories */
	public Stream<WidgetFactory<?>> getFactories() {
		return factories.streamV();
	}

	/** @return all component factories (including widget factories) */
	public Stream<ComponentFactory<?>> getComponentFactories() {
		return stream(APP.widgetManager.factoriesC.stream(), getFactories()).distinct();
	}

	/** @return all features implemented by at least one widget */
	public Stream<Feature> getFeatures() {
		return getFactories().flatMap(f -> f.getFeatures().stream()).distinct();
	}

/* --------------------- LOOK UP ------------------------------------------------------------------------------------ */

	/**
	 * remembers standalone widgets not part of any layout, mostly in popups
	 * as for normal widgets - they can be obtained from layouts
	 * Do not use.
	 */
	public final List<Widget<?>> standaloneWidgets = new ArrayList<>();

	public Stream<Widget<?>> findAll(WidgetSource source) {
		switch(source) {
			case LAYOUT:
				return getLayoutsNoStandalone().flatMap(Container::getAllWidgets);
			case STANDALONE:
			case NO_LAYOUT:
				return standaloneWidgets.stream();
			case NEW:
				return Stream.empty();
			case OPEN:
			case ANY:
				return Stream.concat(findAll(STANDALONE),findAll(LAYOUT));
			default: throw new SwitchException(source);
		}
	}

	public Optional<Widget<?>> find(String name, WidgetSource source, boolean ignore) {
		return find(w -> w.name().equals(name) || w.nameGui().equals(name), source, ignore);
	}

	/**
	 * Equivalent to {@code find(filter, source, false);}
	 * @see #find(java.util.function.Predicate, sp.it.pl.layout.widget.WidgetManager.WidgetSource, boolean)
	 */
	public Optional<Widget<?>> find(Predicate<WidgetInfo> filter, WidgetSource source) {
		return find(filter, source, false);
	}

	/**
	 * Returns widget fulfilling condition.
	 * <pre>
	 * - any widget can be returned (if it fulfills the cond.), but:
	 * - if there is preferred widget it will always be returned first
	 * - if no widget available it will be attempted to be created
	 * - preferred factory will be used first.
	 * - if all methods fail, null is returned
	 * <pre>
	 * Created widgets are displayed in a popup
	 *
	 * @param filter filter
	 * @param source strategy for widget lookup
	 * @param ignore whether the widget will partake in application layout and be
	 * available for future widget search query. Use true if you want to simply
	 * use the widget as a custom graphics, use false to let it be part of layout.
	 * Can not be changed later.
	 * <p/>
	 * Note that using true will cause the returned widget (if any) be visible in
	 * one way or another depending on strategy. So for example newly created
	 * widget will be put into layout or will show in a popup. If this behavior is
	 * not desired, use false.
	 * @return optional of widget fulfilling condition or empty if not available
	 */
	public Optional<Widget<?>> find(Predicate<WidgetInfo> filter, WidgetSource source, boolean ignore) {
		Widget<?> out = null;

		// get preferred type
		String preferred = getFactories()
				.filter(filter::test)
				.filter(w -> !w.isIgnored())
				.filter(WidgetFactory::isPreferred)
				.findAny().map(WidgetFactory::nameGui).orElse("");

		// get viable widgets - widgets of the feature & of preferred type if any
		List<Widget<?>> widgets = findAll(source)
				.filter(w -> filter.test(w.getInfo()))
				.filter(w -> !w.forbid_use.getValue())
				.filter(preferred.isEmpty() ? w -> true : w -> w.getInfo().nameGui().equals(preferred))
				.collect(toList());

		// get preferred widget or any if none preferred
		for (Widget<?> w : widgets) {
			if (out==null) out = w;
			if (w.preferred.getValue()) {
				out = w;
				break;
			}
		}

		// if no active or layout widget available & new widgets allowed
		if (out == null && source.newWidgetsAllowed()) {
			// get factory
			WidgetFactory<?> f = getFactories()
				   .filter(filter::test)
				   .filter(w -> !w.isIgnored())
				   .filter(preferred.isEmpty() ? w->true : w->w.nameGui().equals(preferred))
				   .findAny().orElse(null);

			// open widget as standalone if found
			if (f!=null) {
				out = f.create();
				standaloneWidgets.add(out);
				if (!ignore) {
					APP.windowManager.showFloating(out);
				}
			}
		}

		return Optional.ofNullable(out);
	}

	/**
	 * Equivalent to: {@code
	 * getWidget(w->w.hasFeature(feature), source).map(w->(F)w.getController())}
	 */
	@SuppressWarnings("unchecked")
	public <F> Optional<F> find(Class<F> feature, WidgetSource source, boolean ignore) {
		return find(w -> w.hasFeature(feature), source, ignore).map(w -> (F)w.getController());
	}

	@SuppressWarnings("unchecked")
	public <F> Optional<F> find(Class<F> feature, WidgetSource source) {
		return find(w -> w.hasFeature(feature), source).map(w -> (F)w.getController());
	}

	/** Equivalent to: {@code getWidget(type, source).ifPresent(action)} */
	public <F> void use(Class<F> type, WidgetSource source, Consumer<F> action) {
		find(type, source).ifPresent(action);
	}

	/** Equivalent to: {@code getWidget(cond, source).ifPresent(action)} */
	public void use(Predicate<WidgetInfo> cond, WidgetSource source, Consumer<Widget> action) {
		find(cond, source).ifPresent(action);
	}

	public void use(String name, WidgetSource source, Consumer<Widget> action) {
		find(name, source, false).ifPresent(action);
	}
	public void use(String name, WidgetSource source, Consumer<Widget> action, boolean ignore) {
		find(name, source, ignore).ifPresent(action);
	}

	/**
	 * Denotes source for widgets. Used when looking up a widget. Sometimes it
	 * is desirable to limit the source.
	 */
	public enum WidgetSource {

		/**
		 * The source is be all currently active widgets contained within all
		 * of the layouts in all of the windows. Does not contain standalone
		 * widgets. Standalone widget is one that is not part of the layout. for
		 * example located in the popup. Most limited source.
		 */
		LAYOUT,

		/**
		 * Source is all currently active standalone widgets - widgets not part
		 * of the layout, such as in popups.
		 */
		STANDALONE,

		/**
		 * Union of {@link #LAYOUT} and {@link #STANDALONE}.
		 * <p/>
		 * This is the recommended source when creating widget is not intended.
		 */
		OPEN,

		/**
		 * The source is all available widget factories. In other words new
		 * widget will always be created if possible.
		 */
		NEW,

		/**
		 * Union of {@link #NEW}, {@link #STANDALONE}.
		 * <p/>
		 * This is the recommended source when it is expected to call the widget
		 * multiple times and layout is not to be included,
		 * because it creates new widget, but reuses standalone ones.
		 */
		NO_LAYOUT,

		/**
		 * Union of {@link #LAYOUT}, {@link #STANDALONE} and {@link #NO_LAYOUT}
		 * Most complete source.
		 */
		ANY;

		public boolean newWidgetsAllowed() {
			return this==NO_LAYOUT || this==ANY || this==NEW;
		}
	}

	@SuppressWarnings("unused")
	public enum WidgetTarget {
		LAYOUT,
		TAB,
		POPUP,
		WINDOW
	}








	private final List<String> layoutsAvailable = new ArrayList<>();

	public Layout getActive() {
		// If no window is focused no layout should be active as application
		// is either not focused or in an illegal state itself.
		return windowManager.getFocused().map(Window::getLayout).orElse(null);
	}

	/**
	 * @return all Layouts in the application.
	 */
	public Stream<Layout> getLayouts() {
		return windowManager.windows.stream().map(Window::getLayout).filter(ISNTØ);
	}

	public Stream<Layout> getLayoutsNoStandalone() {
		return windowManager.windows.stream().map(Window::getLayout).filter(ISNTØ);
	}

	/**
	 * Return all names of all layouts available to the application, including
	 * serialized layouts in files.
	 */
	public Stream<String> getAllLayoutsNames() {
		findLayouts();
		// get all windows and fetch their layouts
		return Stream.concat(getLayouts().map(Layout::getName), layoutsAvailable.stream()).distinct();
	}

	/**
	 * Searches for .l files in layout folder and registers them as available
	 * layouts. Use on app start or to discover newly added layouts.
	 */
	public void findLayouts() {
		// get + verify path
		File dir = APP.DIR_LAYOUTS;
		if (!Util.isValidatedDirectory(dir)) {
			LOGGER.error("Layout directory not accessible: ", dir);
			return;
		}
		// find layout files
		File[] files = listChildren(dir).filter(f -> f.getName().endsWith(".l")).toArray(File[]::new);
		// load layouts
		layoutsAvailable.clear();
		if (files.length == 0) return;
		for (File f : files) {
			layoutsAvailable.add(Util.getName(f));
		}
	}

}