package sp.it.pl.main;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.sun.tools.attach.VirtualMachine;
import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.function.Consumer;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import sp.it.pl.audio.Item;
import sp.it.pl.audio.Player;
import sp.it.pl.audio.tagging.Metadata;
import sp.it.pl.audio.tagging.MetadataReader;
import sp.it.pl.gui.objects.grid.GridCell;
import sp.it.pl.gui.objects.grid.GridView;
import sp.it.pl.gui.objects.grid.GridView.SelectionOn;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.pl.gui.objects.icon.IconInfo;
import sp.it.pl.gui.objects.popover.PopOver;
import sp.it.pl.gui.objects.popover.ScreenPos;
import sp.it.pl.gui.pane.ActionPane.FastAction;
import sp.it.pl.gui.pane.OverlayPane;
import sp.it.pl.layout.Component;
import sp.it.pl.layout.area.ContainerNode;
import sp.it.pl.layout.container.layout.Layout;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.WidgetSource;
import sp.it.pl.layout.widget.feature.ConfiguringFeature;
import sp.it.pl.layout.widget.feature.ImageDisplayFeature;
import sp.it.pl.layout.widget.feature.TextDisplayFeature;
import sp.it.pl.unused.SimpleConfigurator;
import sp.it.pl.util.action.ActionRegistrar;
import sp.it.pl.util.action.IsAction;
import sp.it.pl.util.conf.IsConfigurable;
import sp.it.pl.util.conf.ValueConfig;
import sp.it.pl.util.dev.Blocks;
import sp.it.pl.util.file.AudioFileFormat;
import sp.it.pl.util.file.AudioFileFormat.Use;
import sp.it.pl.util.system.EnvironmentKt;
import sp.it.pl.util.validation.Constraint.StringNonEmpty;
import sp.it.pl.web.DuckDuckGoQBuilder;
import sp.it.pl.web.WebBarInterpreter;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javafx.scene.control.PopupControl.USE_COMPUTED_SIZE;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.paint.Color.BLACK;
import static javafx.stage.WindowEvent.WINDOW_HIDING;
import static javafx.util.Duration.millis;
import static sp.it.pl.audio.tagging.ExtKt.readAudioFile;
import static sp.it.pl.gui.pane.OverlayPane.Display.SCREEN_OF_MOUSE;
import static sp.it.pl.layout.widget.WidgetSource.NEW;
import static sp.it.pl.main.AppUtil.APP;
import static sp.it.pl.util.Util.urlEncodeUtf8;
import static sp.it.pl.util.async.AsyncKt.FX;
import static sp.it.pl.util.async.AsyncKt.runFX;
import static sp.it.pl.util.async.AsyncKt.runLater;
import static sp.it.pl.util.async.future.Fut.fut;
import static sp.it.pl.util.dev.Util.logger;
import static sp.it.pl.util.dev.Util.stackTraceAsString;
import static sp.it.pl.util.dev.Util.throwIfFxThread;
import static sp.it.pl.util.functional.Util.list;
import static sp.it.pl.util.functional.Util.map;
import static sp.it.pl.util.functional.Util.set;
import static sp.it.pl.util.functional.Util.stream;
import static sp.it.pl.util.functional.UtilKt.consumer;
import static sp.it.pl.util.graphics.Util.add1timeEventHandler;
import static sp.it.pl.util.graphics.Util.createFMNTStage;
import static sp.it.pl.util.graphics.Util.layHorizontally;
import static sp.it.pl.util.graphics.Util.layVertically;
import static sp.it.pl.util.graphics.UtilKt.bgr;
import static sp.it.pl.util.graphics.UtilKt.getScreenForMouse;
import static sp.it.pl.util.graphics.UtilKt.setMinPrefMaxSize;
import static sp.it.pl.util.system.EnvironmentKt.browse;
import static sp.it.pl.util.system.EnvironmentKt.open;
import static sp.it.pl.util.type.Util.getEnumConstants;

@SuppressWarnings("unused")
@IsConfigurable("Shortcuts")
public class AppActions {

	@IsAction(name = "Open on Github", desc = "Opens Github page for this application. For developers.")
	public void openAppGithubPage() {
		browse(APP.uriGithub);
	}

	@IsAction(name = "Open app directory", desc = "Opens directory from which this application is running from.")
	public void openAppLocation() {
		open(APP.DIR_APP);
	}

	@IsAction(name = "Open css guide", desc = "Opens css reference guide. For developers.")
	public void openCssGuide() {
		browse(URI.create("http://docs.oracle.com/javase/8/javafx/api/javafx/scene/doc-files/cssref.html"));
	}

	@IsAction(name = "Open icon viewer", desc = "Opens application icon browser. For developers.")
	public void openIconViewer() {
		double iconSize = 80;
		GridView<GlyphIcons,GlyphIcons> grid = new GridView<>(GlyphIcons.class, x -> x, iconSize,iconSize+30, 5, 5);
		grid.search.field = (object, substitute) -> object==null ? substitute : object.name();
		grid.selectOn.addAll(set(SelectionOn.MOUSE_HOVER, SelectionOn.MOUSE_CLICK, SelectionOn.KEY_PRESS));
		grid.setCellFactory(view -> new GridCell<>() {

			{
				getStyleClass().add("icon-grid-cell");
				setPickOnBounds(true);
			}

			@Override
			public void updateItem(GlyphIcons icon, boolean empty) {
				super.updateItem(icon, empty);
				IconInfo graphics;
				if (getGraphic() instanceof IconInfo)
					graphics = (IconInfo) getGraphic();
				else {
					graphics = new IconInfo(null, iconSize);
					graphics.setMouseTransparent(true);
					setGraphic(graphics);
				}
				graphics.setGlyph(empty ? null : icon);
			}

			@Override
			public void updateSelected(boolean selected) {
				super.updateSelected(selected);
				IconInfo graphics = (IconInfo) getGraphic();
				if (graphics!=null) graphics.select(selected);
			}
		});
		StackPane root = new StackPane(grid);
		List<Button> groups = stream(Icon.GLYPH_TYPES)
			  .map(c -> {
				  Button b = new Button(c.getSimpleName());
				  b.setOnMouseClicked(e -> {
					  if (e.getButton()==MouseButton.PRIMARY) {
						  grid.getItemsRaw().setAll(getEnumConstants(c));
						  e.consume();
					  }
				  });
				  return b;
			  })
			  .collect(toList());

		Pane layout = layVertically(20, Pos.TOP_CENTER,layHorizontally(8,Pos.CENTER,groups), root);
		layout.setPrefSize(600, 720);

		new PopOver<>(layout).show(ScreenPos.APP_CENTER);
	}

	@IsAction(name = "Open launcher", desc = "Opens program launcher widget.", keys = "CTRL+P")
	public void openLauncher() {
		File f = new File(APP.DIR_LAYOUTS,"AppMainLauncher.fxwl");
		Component c = APP.windowManager.instantiateComponent(f);
		if (c!=null) {
			OverlayPane<Void> op = new OverlayPane<>() {
				@Override
				public void show(Void data) {
					OverlayPane root = this;
					Pane componentRoot = (Pane) c.load();
//					getChildren().add(componentRoot);   // alternatively for borderless/fullscreen experience
					setContent(componentRoot);
					runFX(millis(500), () -> componentRoot.getChildren().stream()
							.filter(GridView.class::isInstance).map(GridView.class::cast)
							.findAny()
							.ifPresent(n -> n.implGetSkin().requestFocus())
					);
					if (c instanceof Widget) {
						((Widget<?>)c).getController().getFieldOrThrow("closeOnLaunch").setValue(true);
						((Widget<?>)c).getController().getFieldOrThrow("closeOnRightClick").setValue(true);
						((Widget<?>)c).areaTemp = new ContainerNode() {
							@Override public Pane getRoot() { return root; }
							@Override public void show() {}
							@Override public void hide() {}
							@Override public void close() { root.hide(); }
						};
					}
					super.show();
				}
			};
			op.getDisplay().set(SCREEN_OF_MOUSE);
			op.show(null);
			op.makeResizableByUser();
			c.load().prefWidth(900);
			c.load().prefHeight(700);
		}
	}

	@SuppressWarnings("unchecked")
	@IsAction(name = "Open settings", desc = "Opens application settings.")
	public void openSettings() {
		APP.widgetManager.widgets.use(ConfiguringFeature.class, WidgetSource.NO_LAYOUT, consumer(c -> c.configure(APP.configuration.getFields())));
	}

	@IsAction(name = "Open layout manager", desc = "Opens layout management widget.")
	public void openLayoutManager() {
		APP.widgetManager.widgets.find(Widgets.LAYOUTS, WidgetSource.NO_LAYOUT, false);
	}

	@IsAction(name = "Open app actions", desc = "Actions specific to whole application.")
	public void openActions() {
		APP.actionAppPane.show(APP);
	}

	@IsAction(name = "Open", desc = "Opens all possible open actions.", keys = "CTRL+SHIFT+O", global = true)
	public void openOpen() {
		APP.actionPane.show(Void.class, null, false,
			new FastAction<>(
				"Open widget",
				"Open file chooser to open an exported widget",
				MaterialIcon.WIDGETS,
				none -> {
					FileChooser fc = new FileChooser();
					fc.setInitialDirectory(APP.DIR_LAYOUTS);
					fc.getExtensionFilters().add(new ExtensionFilter("component file","*.fxwl"));
					fc.setTitle("Open widget...");
					File f = fc.showOpenDialog(APP.actionAppPane.getScene().getWindow());
					if (f!=null) APP.windowManager.launchComponent(f);
				}
			),
			new FastAction<>(
				"Open skin",
				"Open file chooser to find a skin",
				MaterialIcon.BRUSH,
				none -> {
					FileChooser fc = new FileChooser();
					fc.setInitialDirectory(APP.DIR_SKINS);
					fc.getExtensionFilters().add(new ExtensionFilter("skin file","*.css"));
					fc.setTitle("Open skin...");
					File f = fc.showOpenDialog(APP.actionAppPane.getScene().getWindow());
					if (f!=null) APP.ui.setSkin(f);
				}
			),
			new FastAction<>(
				"Open audio files",
				"Open file chooser to find a audio files",
				MaterialDesignIcon.MUSIC_NOTE,
				none -> {
					FileChooser fc = new FileChooser();
					fc.setInitialDirectory(APP.DIR_SKINS);
					fc.getExtensionFilters().addAll(map(AudioFileFormat.supportedValues(Use.APP), AudioFileFormat::toExtFilter));
					fc.setTitle("Open audio...");
					List<File> fs = fc.showOpenMultipleDialog(APP.actionAppPane.getScene().getWindow());
					// Action pane may auto-close when this action finishes, so we make sure to call
					// show() after that happens by delaying using runLater
					if (fs!=null) runLater(() -> APP.actionAppPane.show(fs));
				}
			)
		);
	}

	@IsAction(name = "Show shortcuts", desc = "Display all available shortcuts.", keys = "COMMA")
	public void showShortcuts() {
		APP.shortcutPane.show(ActionRegistrar.INSTANCE.getActions());
	}

	@IsAction(name = "Show system info", desc = "Display system information.")
	public void showSysInfo() {
		APP.actionPane.hide();
		APP.infoPane.show(null);
	}

	@IsAction(name = "Run garbage collector", desc = "Runs java's garbage collector using 'System.gc()'.")
	public void runGarbageCollector() {
		System.gc();
	}

	@IsAction(name = "Run system command", desc = "Runs command just like in a system's shell's command line.", global = true)
	public void runCommand() {
		SimpleConfigurator sc = new SimpleConfigurator<>(
			new ValueConfig<>(String.class, "Command", "").constraints(new StringNonEmpty()),
			c -> EnvironmentKt.runCommand(c)
		);
		PopOver<?> p = new PopOver<>(sc);
				   p.title.set("Run system command");
				   p.show(ScreenPos.APP_CENTER);
	}

	@IsAction(name = "Run app command", desc = "Runs app command. Equivalent of launching this application with the command as a parameter.")
	public void runAppCommand() {
		SimpleConfigurator sc = new SimpleConfigurator<>(
			new ValueConfig<>(String.class, "Command", "").constraints(new StringNonEmpty()),
			(String command) -> APP.parameterProcessor.process(list(command)));
		PopOver<?> p = new PopOver<>(sc);
				   p.title.set("Run app command");
				   p.show(ScreenPos.APP_CENTER);
	}

	@IsAction(name = "Search (os)", desc = "Display application search.", keys = "CTRL+SHIFT+I", global = true)
	public void showSearchPosScreen() {
		showSearch(ScreenPos.SCREEN_CENTER);
	}

	@IsAction(name = "Search (app)", desc = "Display application search.", keys = "CTRL+I")
	public void showSearchPosApp() {
		showSearch(ScreenPos.APP_CENTER);
	}

	public void showSearch(ScreenPos pos) {
		PopOver<?> p = new PopOver<>(APP.search.build());
		p.title.set("Search for an action or option");
		p.setAutoHide(true);
		p.show(pos);
	}

	@IsAction(name = "Open web search", desc = "Opens website or search engine result for given phrase", keys = "CTRL + SHIFT + W", global = true)
	public void openWebBar() {
		doWithUserString("Open on web...", "Website or phrase",
			phrase -> {
				String uriString = WebBarInterpreter.INSTANCE.toUrlString(phrase, DuckDuckGoQBuilder.INSTANCE);
				try {
					URI uri = new URI(uriString);
					browse(uri);
				} catch (URISyntaxException e) {
					logger(AppActions.class).warn("{} is not a valid URI", uriString, e);
				}
			}
		);
	}

	@IsAction(name = "Open web dictionary", desc = "Opens website dictionary for given word", keys = "CTRL + SHIFT + E", global = true)
	public void openDictionary() {
		doWithUserString("Look up in dictionary...", "Word",
			phrase -> browse(URI.create("http://www.thefreedictionary.com/" + urlEncodeUtf8(phrase)))
		);
	}

	public void doWithUserString(String title, String inputName, Consumer<? super String> action) {
		PopOver<SimpleConfigurator<?>> p = new PopOver<>(new SimpleConfigurator<>(
			new ValueConfig<>(String.class, inputName, "").constraints(new StringNonEmpty()),
			action
		));
		p.title.set(title);
		p.setAutoHide(true);
		p.show(ScreenPos.APP_CENTER);
		p.contentNode.getValue().focusFirstConfigField();
		p.contentNode.getValue().hideOnOk.setValue(true);
	}

	public void openImageFullscreen(File image) {
		openImageFullscreen(image, getScreenForMouse());
	}

	public void openImageFullscreen(File image, Screen screen) {
		// find appropriate widget
		Widget<?> c = APP.widgetManager.widgets.find(w -> w.hasFeature(ImageDisplayFeature.class),NEW,true).orElse(null);
		if (c==null) return; // one can never know


		Layout l = Layout.openStandalone(new AnchorPane());
		AnchorPane root = l.getRoot();
		Node cn = c.load();
		setMinPrefMaxSize(cn, USE_COMPUTED_SIZE); // make sure no settings prevents full size
		Stage window = createFMNTStage(screen, false);
		window.setScene(new Scene(root));
		window.getScene().setFill(BLACK);

		window.addEventFilter(WINDOW_HIDING, e -> c.close());
		root.addEventHandler(KEY_PRESSED, Event::consume);
		root.addEventFilter(KEY_PRESSED, e -> {
			e.consume();
			if (e.getCode()==ESCAPE || e.getCode()==ENTER)
				window.hide();
		});

		window.show();
		cn.requestFocus();       // enables key events, focusing widget should do it, but we do not want assumptions
		c.focus();
		l.setChild(c);

		root.setBackground(bgr(BLACK));

		// use widget for image viewing
		// note: although we know the image size (== screen size) we can not use it
		//       as widget will use its own size, which can take time to initialize,
		//       so we need to delay execution
		Runnable a = () -> ((ImageDisplayFeature)c.getController()).showImage(image);
		Runnable r = () -> runFX(millis(100.0), a); // give layout some time to initialize (could display wrong size)
		if (window.isShowing()) r.run(); /// execute when/after window is shown
		else add1timeEventHandler(window, WindowEvent.WINDOW_SHOWN, t -> r.run());
	}

	/**
	 * The check whether file exists, is accessible or of correct type/format is left on the caller and behavior in
	 * such cases is undefined.
	 */
	@Blocks
	public void printAllImageFileMetadata(File file) {
		throwIfFxThread();

		String t = "Metadata of " + file.getPath();
		try {
			StringBuilder sb = new StringBuilder();
			com.drew.metadata.Metadata metadata = ImageMetadataReader.readMetadata(file);
			metadata.getDirectories().forEach(d -> {
				sb.append("\nName: ").append(d.getName());
				d.getTags().forEach(tag -> sb.append("\n\t").append(tag.toString()));
			});
			t = t + sb.toString();
		} catch (IOException | ImageProcessingException e) {
			t = t + "\n" + stackTraceAsString(e);
		}
		String text = t;
		runFX(() -> APP.widgetManager.widgets.find(TextDisplayFeature.class, NEW).ifPresent(w -> w.showText(text)));
	}

	@Blocks
	public void printAllAudioItemMetadata(Item item) {
		throwIfFxThread();

		if (item.isFileBased()) {
			printAllAudioFileMetadata(item.getFile());
		} else {
			String text = "Metadata of " + item.getUri()+ "\n<only supported for files>";
			runFX(() -> APP.widgetManager.widgets.find(TextDisplayFeature.class, NEW).ifPresent(w -> w.showText(text)));
		}
	}

	/**
	 * The check whether file exists, is accessible or of correct type/format is left on the caller and behavior in
	 * such cases is undefined.
	 */
	@Blocks
	public void printAllAudioFileMetadata(File file) {
		throwIfFxThread();

		String text = ""
			+ "Metadata of " + file.getPath()
			+  readAudioFile(file)
				.map(af -> ""
						+ "\nHeader:" + "\n" + af.getAudioHeader().toString().replace("\n", "\n\t")
						+ "\nTag:" + ((af.getTag()==null)
							? " " + "<none>"
							: stream(af.getTag().getFields()).map(it -> "\n\t" + it.getId() + ":" + it.toString()).collect(joining(""))
						)
				)
				.getOrSupply(e -> "\n" + stackTraceAsString(e));
		runFX(() -> APP.widgetManager.widgets.find(TextDisplayFeature.class, NEW).ifPresent(w -> w.showText(text)));
	}

	@IsAction(name = "Print running java processes")
	public void printJavaProcesses() {
		String text = VirtualMachine.list().stream()
			.map(vm -> ""
				+ "\nVM:"
				+ "\n\tid: " + vm.id()
				+ "\n\tdisplayName: " + vm.displayName()
				+ "\n\tprovider: " + vm.provider()
			)
			.collect(joining(""));
		System.out.println("displaying " + text);
		APP.widgetManager.widgets.find(TextDisplayFeature.class, NEW).ifPresent(w -> w.showText(text));
	}

	public void refreshItemsFromFileJob(List<? extends Item> items) {
		fut(items)
			.then(Player.IO_THREAD, is -> is.stream().map(MetadataReader::readMetadata).filter(m -> !m.isEmpty()).collect(toList()))
			.useBy(Player.IO_THREAD, Player::refreshItemsWith)
			.showProgressOnActiveWindow();
	}

	public void itemToMeta(Item i, Consumer<Metadata> action) {
		if (i.same(Player.playingItem.get())) {
			action.accept(Player.playingItem.get());
			return;
		}

		Metadata m = APP.db.getItemsById().get(i.getId());
		if (m!=null) {
			action.accept(m);
		} else {
			fut(i)
				.then(Player.IO_THREAD, MetadataReader::readMetadata)
				.useBy(FX, action);
		}
	}

}