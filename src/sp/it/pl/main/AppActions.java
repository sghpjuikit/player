package sp.it.pl.main;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;
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
import sp.it.pl.gui.Gui;
import sp.it.pl.gui.objects.grid.GridCell;
import sp.it.pl.gui.objects.grid.GridView;
import sp.it.pl.gui.objects.grid.GridView.SelectionOn;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.pl.gui.objects.icon.IconInfo;
import sp.it.pl.gui.objects.popover.PopOver;
import sp.it.pl.gui.objects.popover.ScreenPos;
import sp.it.pl.gui.objects.tree.TreeItemsKt;
import sp.it.pl.gui.pane.ActionPane.FastAction;
import sp.it.pl.gui.pane.OverlayPane;
import sp.it.pl.layout.Component;
import sp.it.pl.layout.area.ContainerNode;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.WidgetManager.WidgetSource;
import sp.it.pl.layout.widget.feature.ConfiguringFeature;
import sp.it.pl.layout.widget.feature.ImageDisplayFeature;
import sp.it.pl.unused.SimpleConfigurator;
import sp.it.pl.util.action.Action;
import sp.it.pl.util.action.IsAction;
import sp.it.pl.util.action.IsActionable;
import sp.it.pl.util.conf.ValueConfig;
import sp.it.pl.util.file.AudioFileFormat;
import sp.it.pl.util.file.AudioFileFormat.Use;
import sp.it.pl.util.functional.Functors;
import sp.it.pl.util.system.EnvironmentKt;
import sp.it.pl.util.validation.Constraint.StringNonEmpty;
import sp.it.pl.web.DuckDuckGoQBuilder;
import sp.it.pl.web.WebBarInterpreter;
import static java.util.stream.Collectors.toList;
import static javafx.scene.control.PopupControl.USE_COMPUTED_SIZE;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.paint.Color.BLACK;
import static sp.it.pl.gui.pane.OverlayPane.Display.SCREEN_OF_MOUSE;
import static sp.it.pl.layout.widget.WidgetManager.WidgetSource.NEW;
import static sp.it.pl.main.AppUtil.APP;
import static sp.it.pl.util.Util.urlEncodeUtf8;
import static sp.it.pl.util.async.AsyncKt.FX;
import static sp.it.pl.util.async.AsyncKt.runAfter;
import static sp.it.pl.util.async.AsyncKt.runFX;
import static sp.it.pl.util.async.AsyncKt.runLater;
import static sp.it.pl.util.async.future.Fut.fut;
import static sp.it.pl.util.dev.Util.log;
import static sp.it.pl.util.functional.Util.list;
import static sp.it.pl.util.functional.Util.map;
import static sp.it.pl.util.functional.Util.set;
import static sp.it.pl.util.functional.Util.stream;
import static sp.it.pl.util.graphics.Util.add1timeEventHandler;
import static sp.it.pl.util.graphics.Util.createFMNTStage;
import static sp.it.pl.util.graphics.Util.layHorizontally;
import static sp.it.pl.util.graphics.Util.layVertically;
import static sp.it.pl.util.graphics.UtilKt.bgr;
import static sp.it.pl.util.graphics.UtilKt.setMinPrefMaxSize;
import static sp.it.pl.util.math.Util.millis;
import static sp.it.pl.util.system.EnvironmentKt.browse;
import static sp.it.pl.util.system.EnvironmentKt.open;
import static sp.it.pl.util.type.Util.getEnumConstants;
import static sp.it.pl.util.type.Util.getFieldValue;

@SuppressWarnings("unused")
@IsActionable("Shortcuts")
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
		double iconSize = 45;
		GridView<GlyphIcons,GlyphIcons> grid = new GridView<>(GlyphIcons.class, x -> x, iconSize+25,iconSize+35,5,5);
		grid.search.field = (object, substitute) -> object==null ? substitute : object.name();
		grid.selectOn.addAll(set(SelectionOn.MOUSE_HOVER, SelectionOn.MOUSE_CLICK, SelectionOn.KEY_PRESS));
		grid.setCellFactory(view -> new GridCell<>() {
//			    Anim a;

			{
				getStyleClass().add("icon-grid-cell");
			}

			@Override
			public void updateItem(GlyphIcons icon, boolean empty) {
				super.updateItem(icon, empty);
				IconInfo graphics;
				if (getGraphic() instanceof IconInfo)
					graphics = (IconInfo) getGraphic();
				else {
					graphics = new IconInfo(null,iconSize);
					setGraphic(graphics);
//					    a = new Anim(graphics::setOpacity).dur(100).intpl(x -> x*x*x*x);
				}
				graphics.setGlyph(empty ? null : icon);

				// really cool when scrolling with scrollbar
				// but when using mouse wheel it is very ugly & distracting
				// a.play();
			}

			@Override
			public void updateSelected(boolean selected) {
				super.updateSelected(selected);
				IconInfo graphics = (IconInfo) getGraphic();
				if (graphics!=null) graphics.select(selected);
			}
		});
		StackPane root = new StackPane(grid);
		root.setPrefSize(600, 720); // determines popup size
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
		PopOver o = new PopOver<>(layVertically(20, Pos.TOP_CENTER,layHorizontally(8,Pos.CENTER,groups), root));
		o.show(ScreenPos.APP_CENTER);
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
					runAfter(millis(500), () -> componentRoot.getChildren().stream()
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

	@IsAction(name = "Open settings", desc = "Opens application settings.")
	public void openSettings() {
		APP.widgetManager.use(ConfiguringFeature.class, WidgetSource.NO_LAYOUT, c -> c.configure(APP.configuration.getFields()));
	}

	@IsAction(name = "Open layout manager", desc = "Opens layout management widget.")
	public void openLayoutManager() {
		APP.widgetManager.find("Layouts", WidgetSource.NO_LAYOUT, false);
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
					if (f!=null) Gui.setSkin(f);
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
		APP.shortcutPane.show(Action.getActions());
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
					log(AppActions.class).warn("{} is not a valid URI", uriString, e);
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

	public void openImageFullscreen(File image, Screen screen) {
		// find appropriate widget
		Widget<?> c = APP.widgetManager.find(w -> w.hasFeature(ImageDisplayFeature.class),NEW,true).orElse(null);
		if (c==null) return; // one can never know
		Node cn = c.load();
		setMinPrefMaxSize(cn, USE_COMPUTED_SIZE); // make sure no settings prevents full size
		StackPane root = new StackPane(cn);
		root.setBackground(bgr(BLACK));
		Stage s = createFMNTStage(screen, false);
		s.setScene(new Scene(root));
		s.show();

		cn.requestFocus(); // for key events to work - just focus some root child
		root.addEventFilter(KEY_PRESSED, ke -> {
			if (ke.getCode()==ESCAPE)
				s.hide();
		});

		// use widget for image viewing
		// note: although we know the image size (== screen size) we can not use it
		//       as widget will use its own size, which can take time to initialize,
		//       so we need to delay execution
		Functors.Ƒ a = () -> ((ImageDisplayFeature)c.getController()).showImage(image);
		Functors.Ƒ r = () -> runFX(100, a); // give layout some time to initialize (could display wrong size)
		if (s.isShowing()) r.apply(); /// execute when/after window is shown
		else add1timeEventHandler(s, WindowEvent.WINDOW_SHOWN, t -> r.apply());
	}

	public void printAllImageFileMetadata(File file) {
		try {
			StringBuilder sb = new StringBuilder("Metadata of ").append(file.getPath());
			com.drew.metadata.Metadata metadata = ImageMetadataReader.readMetadata(file);
			metadata.getDirectories().forEach(d -> {
				sb.append("\nName: ").append(d.getName());
				d.getTags().forEach(tag -> sb.append("\n\t").append(tag.toString()));
			});
			APP.widgetManager.find(w -> w.name().equals("Logger"), WidgetSource.ANY); // open console automatically
			System.out.println(sb.toString());
		} catch (IOException|ImageProcessingException e) {
			e.printStackTrace();
		}
	}

	public void refreshItemsFromFileJob(List<? extends Item> items) {
		fut(items)
			.map(Player.IO_THREAD, is -> is.stream().map(MetadataReader::readMetadata).filter(m -> !m.isEmpty()).collect(toList()))
			.use(Player.IO_THREAD, Player::refreshItemsWith)
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
				.map(Player.IO_THREAD, MetadataReader::readMetadata)
				.use(FX, action);
		}
	}

	public void inspectObjectInInspector(Object o) {
		APP.widgetManager.find("Inspector", WidgetSource.OPEN, true)
			.ifPresent(w ->
				((TreeView) getFieldValue(w.getController(), "tree"))    // TODO: improve
					.getRoot().getChildren()
					.add(TreeItemsKt.tree(o))
			);
	}
}