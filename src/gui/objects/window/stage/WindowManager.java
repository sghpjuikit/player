package gui.objects.window.stage;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javafx.beans.binding.DoubleBinding;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import org.reactfx.Subscription;
import org.slf4j.Logger;

import gui.Gui;
import gui.objects.icon.Icon;
import gui.objects.window.stage.WindowBase.Maximized;
import layout.Component;
import layout.container.layout.Layout;
import layout.widget.Widget;
import layout.widget.WidgetFactory;
import layout.widget.feature.HorizontalDock;
import main.App;
import util.access.V;
import util.access.VarEnum;
import util.action.IsAction;
import util.animation.Anim;
import util.async.executor.FxTimer;
import util.conf.Configurable;
import util.conf.IsConfig;
import util.conf.IsConfigurable;
import util.file.Util;
import util.graphics.fxml.ConventionFxmlLoader;
import util.validation.Constraint;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;
import static java.io.File.separator;
import static java.util.stream.Collectors.toSet;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.stage.StageStyle.UNDECORATED;
import static javafx.stage.StageStyle.UTILITY;
import static javafx.stage.WindowEvent.WINDOW_SHOWING;
import static javafx.util.Duration.ZERO;
import static javafx.util.Duration.millis;
import static main.App.APP;
import static util.async.Async.runLater;
import static util.dev.Util.log;
import static util.dev.Util.noØ;
import static util.file.Util.listFiles;
import static util.functional.Util.*;
import static util.graphics.Util.add1timeEventHandler;
import static util.graphics.Util.getScreen;
import static util.reactive.Util.maintain;
import static util.reactive.Util.onScreenChange;

/**
 * Manages windows.
 *
 * @author Martin Polakovic
 */
@IsConfigurable("Gui.Window")
public class WindowManager implements Configurable<Object> {

	private static final Logger LOGGER = log(WindowManager.class);

	public double screenMaxScaling;
	/**
	 * Main application window, see {@link gui.objects.window.stage.Window#isMain}. May be null.
	 */ private Window mainWindow;
	/**
	 * Observable list of all application windows.
	 */ public final ObservableList<Window> windows = Window.WINDOWS;
	/**
	 * Dock window. Null if not active.
	 */ public Window miniWindow;
	private static volatile boolean canBeMainTemp = false; // TODO: remove hack

    @IsConfig(name = "Opacity", info = "Window opacity.")
    @Constraint.MinMax(min=0, max=1)
    public final V<Double> windowOpacity = new V<>(1d);

    @IsConfig(name = "Borderless", info = "Hides borders.")
    public final V<Boolean> window_borderless = new V<>(true);

    @IsConfig(name = "Headerless", info = "Hides header.")
    public final V<Boolean> window_headerless = new V<>(false);

    @IsConfig(name="Show windows", info="Shows/hides all windows. Useful in mini mode.")
    public final V<Boolean> show_windows = new V<>(true, v -> {
        if (!App.APP.normalLoad) return;
        if (v) windows.stream().filter(w->w!=miniWindow).forEach(Window::show);
        else windows.stream().filter(w->w!=miniWindow).forEach(Window::hide);
    });

    @IsConfig(name="Mini mode", info="Whether application has mini window docked to top screen edge active.")
    public final V<Boolean> mini = new V<>(false, this::setMini);

    @IsConfig(name="Mini open hover delay", info="Time for mouse to hover to open mini window.")
    public Duration mini_hover_delay = millis(700);

    @IsConfig(name="Mini hide when inactive", info="Hide mini window when no mouse activity is detected.")
    public final V<Boolean> mini_hide_onInactive = new V<>(true);

    @IsConfig(name="Mini hide when inactive for", info="Time of no activity to hide mini window after.")
    public Duration mini_inactive_delay = millis(700);

    @IsConfig(name="Mini widget", info="Widget to use in mini window.")
    public final VarEnum<String> mini_widget = VarEnum.ofStream("PlayerControlsTiny",
        () -> APP.widgetManager.getFactories().filter(wf -> wf.hasFeature(HorizontalDock.class)).map(WidgetFactory::name)
    );

    public WindowManager() {
    	Runnable computeMaxUsedScaling = () -> screenMaxScaling = Screen.getScreens().stream()
			.mapToDouble(s -> max(s.getOutputScaleX(), s.getOutputScaleY())).max()
			.orElse(1);
    	computeMaxUsedScaling.run();
		Screen.getScreens().addListener((ListChangeListener<Screen>) change -> computeMaxUsedScaling.run());
	}

	public Optional<Window> getMain() {
		return Optional.ofNullable(mainWindow);
	}

    /**
     * Get focused window. There is zero or one focused window in the application at any given time.
     *
     * @see #getActive()
     * @return focused window or null if none focused.
     */
    public Optional<Window> getFocused() {
		return stream(windows).findAny(w -> w.focused.get());
    }

    /**
     * Same as {@link #getFocused()} but when none focused returns is main window
     * instead of null.
     * <p/>
     * Both methods are equivalent except for when the application itself has no
     * focus - which is when no window has focus.
     * <p/>
     * Use when null must absolutely be avoided and the main window substitute
     * for focused window will not break expected behavior and when this method
     * can get called when app has no focus (such as through global shortcut).
     *
     * @return focused window or main window if none.
     */
    public Optional<Window> getActive() {
		return getFocused().or(this::getMain);
    }

    public Window getActiveOrNew() {
	    return getActive().orElseGet(this::createWindow);
    }

    public Window create() {
    	return create(false);
    }

	public Stage createStageOwner() {
        Stage s = new Stage(UTILITY); // utility means no taskbar
              s.setWidth(10);
              s.setHeight(10);
              s.setX(0);
              s.setY(0);
              s.setOpacity(0);
              s.setScene(new Scene(new Pane())); // allows child stages (e.g. popup) to receive key events
              s.show();
		return s;
	}

    private Window create(boolean canBeMain) {
        return create(null,UNDECORATED, canBeMain);
    }

    public Window create(Stage owner, StageStyle style, boolean canBeMain) {
        Window w = new Window(owner,style);

		// load fxml part
        new ConventionFxmlLoader(Window.class, w.root, w).loadNoEx();
        if (canBeMain && mainWindow==null) setAsMain(w); // TODO: improve main window detection/decision strategy
        windows.add(w); // add to list of active windows

		w.initialize();
		w.setFont(Gui.font.get());
        File skinFile = new File(APP.DIR_SKINS.getPath(), Gui.skin.getValue() + separator + Gui.skin.getValue() + ".css");
        try {
            w.root.getStylesheets().add(skinFile.toURI().toURL().toExternalForm());
        } catch (MalformedURLException e) {
	        LOGGER.error("Could not load skin {}", skinFile, e);
        }

        // bind properties
        w.disposables.add(maintain(windowOpacity, w.getStage().opacityProperty()));
        w.disposables.add(maintain(window_borderless, w::setBorderless));
        w.disposables.add(maintain(window_headerless, v -> !v, w::setHeaderVisible));
	    w.getStage().setTitle(APP.name);
	    w.getStage().getIcons().addAll(APP.getIcons());

        return w;
    }

    private Window createWindow(boolean canBeMain) {
	    LOGGER.debug("Creating default window");
	    Window w = create(canBeMain);
	    w.setXYSizeInitial();
	    w.initLayout();
	    w.update();
	    w.show();
	    w.setScreen(getScreen(APP.mouseCapture.getMousePosition()));
	    w.setXYScreenCenter();
	    return w;
    }

	@IsAction(name = "Open new window", desc = "Opens new application window")
	public Window createWindow() {
		return createWindow(false);
	}

	public Window createWindow(Component widget) {
		Window w = createWindow();
		w.setContent(widget);
		return w;
	}

    void setAsMain(Window w) {
        noØ(w);
	    if (mainWindow==w) return;
	    if (mainWindow!=null) mainWindow.isMainDisposables.forEach(Subscription::unsubscribe);
	    if (mainWindow!=null) mainWindow.isMain.setValue(false);
		mainWindow = w;
		w.isMain.setValue(true);
	    w.isMainDisposables.add(() -> w.isMain.setValue(false));
    }

	@IsAction(name = "Close active window", keys = "CTRL+W", desc = "Opens new application window")
	private void closeActiveWindow() {
		getActive().ifPresent(Window::close);
	}

	// TODO: create dynamically from config annotation
	@IsAction(name = "Mini mode", global = true, keys = "F9", desc = "Dock auxiliary window with playback control to the screen edge")
    private void toggleMini() {
        setMini(!mini.get());
    }

    private void toggleMiniFull() {
        if (!App.APP.normalLoad) return;
        if (mini.get()) mainWindow.show();
        else mainWindow.hide();
        setMini(!mini.get());
    }

    private void toggleShowWindows() {
        if (!App.APP.normalLoad) return;
        show_windows.set(!show_windows.get());
    }

    private void setMini(boolean val) {
        if (!App.APP.normalLoad) return;

        mini.set(val);
        if (val) {
            if (miniWindow!=null && miniWindow.isShowing()) return;
            if (miniWindow == null)  miniWindow = create(createStageOwner(), UNDECORATED, false);
            Window.WINDOWS.remove(miniWindow); // ignore mini window in window operations
            miniWindow.setSize(Screen.getPrimary().getBounds().getWidth(), 40);
            miniWindow.resizable.set(true);
            miniWindow.setAlwaysOnTop(true);
            miniWindow.disposables.add(onScreenChange(screen -> {
	            // TODO: implement for every window
                // maintain proper widget content until window closes
                if (screen.getBounds().contains(miniWindow.getCenterXY()))
		            runLater(() -> {
			            miniWindow.setScreen(screen);
			            miniWindow.setXYSize(
				            screen.getBounds().getMinX(),
				            screen.getBounds().getMinY(),
				            screen.getBounds().getWidth(),
				            miniWindow.getHeight()
			            );
		            });
            }));

            // content controls
            Icon miniB = new Icon(null, 13, "Docked mode", this::toggleMiniFull);
            maintain(miniB.hoverProperty(), mapB(ANGLE_DOUBLE_UP,ANGLE_UP), miniB::icon);
            Icon mainB = new Icon(null, 13, "Show main window", this::toggleShowWindows);
            maintain(mainB.hoverProperty(), mapB(ANGLE_DOUBLE_DOWN,ANGLE_DOWN), mainB::icon);
            HBox controls = new HBox(8,mainB,miniB);
                 controls.setAlignment(Pos.CENTER_RIGHT);
                 controls.setFillHeight(false);
                 controls.setPadding(new Insets(5,5,5,25));

            // content
            BorderPane content = new BorderPane();
            miniWindow.setContent(content);
            content.setRight(controls);

            // maintain proper widget content until window closes
            miniWindow.disposables.add(maintain(
                mini_widget,
                name -> {
                    // Create widget or supply empty if not available
                    Widget<?> newW = APP.widgetManager.factories.get(name,"Empty").create();
                    // Close old widget if any to free resources
                    Widget<?> oldW = (Widget) content.getProperties().get("widget");
                    if (oldW!=null) oldW.close();
                    // set new widget
                    content.getProperties().put("widget",newW);
                    content.setCenter(newW.load());
                }
            ));

            // show and apply state
            miniWindow.show();
            miniWindow.setHeaderAllowed(false);
            miniWindow.setBorderless(true);
            miniWindow.update();
            miniWindow.back.setStyle("-fx-background-size: cover;"); // disallow bgr stretching
            miniWindow.content.setStyle("-fx-background-color: -fx-pane-color;"); // imitate widget area bgr

            // auto-hiding
            DoubleBinding H = miniWindow.H.subtract(2); // leave 2 pixels visible
            Parent mw_root = miniWindow.getStage().getScene().getRoot();
            Anim a = new Anim(millis(200), x -> miniWindow.setY(-H.get()*x, false));

            FxTimer hider = new FxTimer(0, 1, () -> {
                if (miniWindow==null) return;
                if (miniWindow.getY()!=0) return;    // if not open
                Duration d = a.getCurrentTime();
                if (d.equals(ZERO)) d = millis(300).subtract(d);
                a.stop();
                a.setRate(1);
                a.playFrom(millis(300).subtract(d));
            });
            mw_root.addEventFilter(MouseEvent.ANY, e -> {
                if (!mini_hide_onInactive.get()) return;   // if disabled
                if (mw_root.isHover()) return;       // if mouse still in (we only want MOUSE_EXIT)
                hider.start(mini_inactive_delay);
            });
            hider.runNow();

            FxTimer shower = new FxTimer(0, 1, () -> {
                if (miniWindow==null) return;
                if (miniWindow.getY()==0) return;    // if open
                if (!mw_root.isHover()) return;      // if mouse left
                Duration d = a.getCurrentTime();
                if (d.equals(ZERO)) d = millis(300).subtract(d);
                a.stop();
                a.setRate(-1);
                a.playFrom(d);
            });
            mw_root.addEventFilter(MOUSE_ENTERED, e -> {
                if (!miniWindow.isShowing()) return;     // bug fix
                shower.start(mini_hover_delay);         // open after delay
            });
            mw_root.addEventHandler(MOUSE_CLICKED, e -> {
                if (e.getButton()==PRIMARY) {
                    if (!miniWindow.isShowing()) return; // bug fix
                    shower.runNow();                    // open with delay
                }
                if (e.getButton()==SECONDARY) {
                    if (!miniWindow.isShowing()) return; // bug fix
                    hider.runNow();                     // open with delay
                }
            });
        } else {
            if (miniWindow!=null) {
	            miniWindow.close();
	            miniWindow = null;
            }
        }
    }

    public void serialize() {
        // make sure directory is accessible
        File dir = new File(APP.DIR_LAYOUTS,"current");
        if (!Util.isValidatedDirectory(dir)) {
	        LOGGER.error("Serialization of windows and layouts failed. {} not accessible.", dir);
            return;
        }

        Set<File> filesOld = listFiles(dir).collect(toSet());
        List<Window> windows = stream(Window.WINDOWS).without(miniWindow).toList();
	    LOGGER.info("Serializing " + windows.size() + " application windows");

        // serialize - for now each window to its own file with .ws extension
	    String sessionUniqueName = System.currentTimeMillis()+"";
	    boolean isError = false;
	    Set<File> filesNew = new HashSet<>();
        for (int i=0; i<windows.size(); i++) {
            Window w = windows.get(i);
            File f = new File(dir, "window_" + sessionUniqueName + "_" + i + ".ws");
	        filesNew.add(f);
	        isError |= App.APP.serializators.toXML(new WindowState(w), f)
		        .ifError(e -> LOGGER.error("Window serialization failed", e))
	            .isError();
	        if (isError) break;
        }

	    // remove unneeded files, either old or new session will remain
        (isError ? filesNew : filesOld).forEach(File::delete);
    }

    public void deserialize(boolean load_normally) {
	    Set<Window> ws = set();
        if (load_normally) {
	        canBeMainTemp = true;

            // make sure directory is accessible
            File dir = new File(APP.DIR_LAYOUTS, "current");
            if (!Util.isValidatedDirectory(dir)) {
	            LOGGER.error("Deserialization of windows and layouts failed. {} not accessible.", dir);
                return;
            }

            // deserialize windows
            File[] fs = listFiles(dir).filter(f -> f.getPath().endsWith(".ws")).toArray(File[]::new);
	        LOGGER.info("Deserializing {} application windows", fs.length);
	        stream(fs)
		        .map(f -> App.APP.serializators.fromXML(WindowState.class, f)
					        .ifError(e -> LOGGER.error("Unable to load window", e))
					        .map(WindowState::toWindow)
					        .getOr(null)
		        )
				.filter(ISNTØ)
				.forEach(ws::add);
	        canBeMainTemp = false;
         }

	    LOGGER.info("Deserialized " + ws.size() + " windows.");
        // show windows
        if (ws.isEmpty()) {
        	if(load_normally)
		        ws.add(createWindow(true));
        } else {
            ws.forEach(w -> add1timeEventHandler(w.s, WINDOW_SHOWING, e -> w.update()));
	        ws.forEach(Window::show);
            Widget.deserializeWidgetIO();
        }
    }

    private static class WindowState {
    	public final double x, y, w, h;
	    public final boolean resizable, minimized, fullscreen, onTop;
	    public final Maximized maximized;
	    public final Layout layout;

	    public WindowState(Window window) {
	    	x = window.X.getValue();
	    	y = window.Y.getValue();
	    	w = window.W.getValue();
	    	h = window.H.getValue();
	    	resizable = window.resizable.getValue();
	    	minimized = window.s.iconifiedProperty().getValue();
	    	fullscreen = window.fullscreen.getValue();
	    	onTop = window.alwaysOnTop.getValue();
	    	maximized = window.maximized.getValue();
	    	layout = window.getLayout();
	    }

	    public Window toWindow() {
		    Window window = APP.windowManager.create(canBeMainTemp);
		    window.X.set(x);
		    window.Y.set(y);
		    window.W.set(w);
		    window.H.set(h);
		    window.s.setIconified(minimized);
		    window.MaxProp.set(maximized);
		    window.FullProp.set(fullscreen);
		    window.resizable.set(resizable);
		    window.setAlwaysOnTop(onTop);
		    window.initLayout(layout);
		    return window;
	    }
    }
}