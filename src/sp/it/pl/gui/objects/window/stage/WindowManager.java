package sp.it.pl.gui.objects.window.stage;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javafx.beans.binding.DoubleBinding;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.reactfx.Subscription;
import org.slf4j.Logger;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.pl.gui.objects.popover.PopOver;
import sp.it.pl.gui.objects.popover.ScreenPos;
import sp.it.pl.layout.Component;
import sp.it.pl.layout.container.layout.Layout;
import sp.it.pl.layout.widget.ComponentFactory;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.feature.HorizontalDock;
import sp.it.pl.main.Settings;
import sp.it.pl.unused.SimpleConfigurator;
import sp.it.pl.util.access.V;
import sp.it.pl.util.access.VarEnum;
import sp.it.pl.util.action.IsAction;
import sp.it.pl.util.animation.Anim;
import sp.it.pl.util.async.executor.FxTimer;
import sp.it.pl.util.conf.Configurable;
import sp.it.pl.util.conf.IsConfig;
import sp.it.pl.util.conf.IsConfigurable;
import sp.it.pl.util.file.Util;
import sp.it.pl.util.graphics.fxml.ConventionFxmlLoader;
import sp.it.pl.util.validation.Constraint;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ANGLE_DOUBLE_DOWN;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ANGLE_DOUBLE_UP;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ANGLE_DOWN;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ANGLE_UP;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.stage.StageStyle.UNDECORATED;
import static javafx.stage.StageStyle.UTILITY;
import static javafx.stage.WindowEvent.WINDOW_HIDING;
import static javafx.stage.WindowEvent.WINDOW_SHOWING;
import static javafx.util.Duration.ZERO;
import static javafx.util.Duration.millis;
import static sp.it.pl.layout.widget.WidgetManagerKt.orEmpty;
import static sp.it.pl.main.AppUtil.APP;
import static sp.it.pl.unused.SimpleConfigurator.simpleConfigurator;
import static sp.it.pl.util.async.AsyncKt.runLater;
import static sp.it.pl.util.async.executor.FxTimer.fxTimer;
import static sp.it.pl.util.dev.Util.logger;
import static sp.it.pl.util.dev.Util.noNull;
import static sp.it.pl.util.file.UtilKt.listChildren;
import static sp.it.pl.util.functional.Util.ISNTØ;
import static sp.it.pl.util.functional.Util.mapB;
import static sp.it.pl.util.functional.Util.max;
import static sp.it.pl.util.functional.Util.set;
import static sp.it.pl.util.functional.Util.stream;
import static sp.it.pl.util.functional.UtilKt.consumer;
import static sp.it.pl.util.functional.UtilKt.runnable;
import static sp.it.pl.util.graphics.Util.addEventHandler1Time;
import static sp.it.pl.util.graphics.UtilKt.getScreenForMouse;
import static sp.it.pl.util.reactive.Util.maintain;
import static sp.it.pl.util.reactive.Util.onItemRemoved;

/**
 * Manages windows.
 */
@IsConfigurable(value = Settings.Ui.WINDOW)
public class WindowManager implements Configurable<Object> {

    private static final Logger LOGGER = logger(WindowManager.class);

    public double screenMaxScaling;
    /**
     * Main application window, see {@link sp.it.pl.gui.objects.window.stage.Window#isMain}. May be null.
     */ private Window mainWindow;
    /**
     * Observable list of all application windows.
     */ public final ObservableList<Window> windows = Window.WINDOWS;
    /**
     * Dock window. Null if not active.
     */ public Window miniWindow;
    static volatile boolean canBeMainTemp = false; // TODO: remove hack

    @IsConfig(name = "Opacity", info = "Window opacity.")
    @Constraint.MinMax(min=0, max=1)
    public final V<Double> windowOpacity = new V<>(1d);

    @IsConfig(name = "Borderless", info = "Hides borders.")
    public final V<Boolean> window_borderless = new V<>(true);

    @IsConfig(name = "Headerless", info = "Hides header.")
    public final V<Boolean> window_headerless = new V<>(false);

    @IsConfig(name="Show windows", info="Shows/hides all windows. Useful in mini mode.")
    public final V<Boolean> show_windows = new V<>(true, v -> {
        if (!APP.getNormalLoad()) return;
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
    public final VarEnum<String> mini_widget = VarEnum.ofStream("Playback Mini",
        () -> APP.widgetManager.factories.getFactoriesWith(HorizontalDock.class).map(w -> w.nameGui())
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
        return stream(windows).filter(w -> w.focused.get()).findAny();
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

    Window create(boolean canBeMain) {
        return create(null,UNDECORATED, canBeMain);
    }

    public Window create(Stage owner, StageStyle style, boolean canBeMain) {
        Window w = new Window(owner,style);

        // load fxml part
        new ConventionFxmlLoader(Window.class, w.root, w).loadNoEx();
        if (canBeMain && mainWindow==null) setAsMain(w); // TODO: improve main window detection/decision strategy
        windows.add(w); // add to list of active windows

        w.initialize();

        // bind properties
        w.disposables.add(maintain(window_borderless, v -> w.isBorderless.set(v)));
        w.disposables.add(maintain(window_headerless, v -> w.isHeaderVisible.set(!v)));
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
        w.setXYToCenter(getScreenForMouse());
        return w;
    }

    @IsAction(name = "Open new window", desc = "Opens new application window")
    public Window createWindow() {
        return createWindow(false);
    }

    void setAsMain(Window w) {
        noNull(w);
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
        if (!APP.getNormalLoad()) return;
        if (mini.get()) mainWindow.show();
        else mainWindow.hide();
        setMini(!mini.get());
    }

    private void toggleShowWindows() {
        if (!APP.getNormalLoad()) return;
        show_windows.set(!show_windows.get());
    }

    private void setMini(boolean val) {
        if (!APP.getNormalLoad()) return;

        mini.set(val);
        if (val) {
            if (miniWindow!=null && miniWindow.isShowing()) return;
            if (miniWindow == null)  miniWindow = create(createStageOwner(), UNDECORATED, false);
            Window.WINDOWS.remove(miniWindow); // ignore mini window in window operations
            miniWindow.setSize(Screen.getPrimary().getBounds().getWidth(), 40);
            miniWindow.resizable.set(true);
            miniWindow.setAlwaysOnTop(true);
            miniWindow.disposables.add(onItemRemoved(Screen.getScreens(), consumer(it -> {
                if (it.equals(miniWindow.getScreen()))
                    runLater(() -> {
                        Screen screen = Screen.getPrimary();
                        miniWindow.setScreen(screen);
                        miniWindow.setXYSize(
                            screen.getBounds().getMinX(),
                            screen.getBounds().getMinY(),
                            screen.getBounds().getWidth(),
                            miniWindow.getHeight()
                        );
                    });

            })));

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
                    Component newW = orEmpty(APP.widgetManager.factories.getComponentFactory(name)).create();
                    Component oldW = (Widget) content.getProperties().get("widget");

                    if (oldW!=null) oldW.close();

                    content.getProperties().put("widget",newW);
                    content.setCenter(newW.load());
                }
            ));

            // show and apply state
            miniWindow.show();
            miniWindow.isHeaderAllowed.set(false);
            miniWindow.isBorderless.set(true);
            miniWindow.update();
            miniWindow.back.setStyle("-fx-background-size: cover;"); // disallow bgr stretching
            miniWindow.content.setStyle("-fx-background-color: -fx-pane-color;"); // imitate widget area bgr

            // auto-hiding
            DoubleBinding H = miniWindow.H.subtract(2); // leave 2 pixels visible
            Parent mw_root = miniWindow.getStage().getScene().getRoot();
            Anim a = new Anim(millis(200), x -> miniWindow.setY(-H.get()*x, false));

            FxTimer hider = fxTimer(ZERO, 1, runnable(() -> {
                if (miniWindow==null) return;
                if (miniWindow.getY()!=0) return;    // if not open
                Duration d = a.getCurrentTime();
                if (d.equals(ZERO)) d = millis(300).subtract(d);
                a.stop();
                a.setRate(1);
                a.playFrom(millis(300).subtract(d));
            }));
            mw_root.addEventFilter(MouseEvent.ANY, e -> {
                if (!mini_hide_onInactive.get()) return;   // if disabled
                if (mw_root.isHover()) return;       // if mouse still in (we only want MOUSE_EXIT)
                hider.start(mini_inactive_delay);
            });
            hider.runNow();

            FxTimer shower = fxTimer(ZERO, 1, runnable(() -> {
                if (miniWindow==null) return;
                if (miniWindow.getY()==0) return;    // if open
                if (!mw_root.isHover()) return;      // if mouse left
                Duration d = a.getCurrentTime();
                if (d.equals(ZERO)) d = millis(300).subtract(d);
                a.stop();
                a.setRate(-1);
                a.playFrom(d);
            }));
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

        Set<File> filesOld = listChildren(dir).collect(toSet());
        List<Window> windows = stream(Window.WINDOWS).filter(w -> w!=miniWindow).collect(toList());
        LOGGER.info("Serializing " + windows.size() + " application windows");

        // serialize - for now each window to its own file with .ws extension
        String sessionUniqueName = System.currentTimeMillis()+"";
        boolean isError = false;
        Set<File> filesNew = new HashSet<>();
        for (int i=0; i<windows.size(); i++) {
            Window w = windows.get(i);
            File f = new File(dir, "window_" + sessionUniqueName + "_" + i + ".ws");
            filesNew.add(f);
            isError |= APP.serializerXml.toXML(new WindowState(w), f).isError();
            if (isError) break;
        }

        // remove unneeded files, either old or new session will remain
        (isError ? filesNew : filesOld).forEach(File::delete);
    }

    public void deserialize(boolean load_normally) {
        Set<Window> ws = set();
        if (load_normally) {
            canBeMainTemp = true;

            File dir = new File(APP.DIR_LAYOUTS, "current");
            if (Util.isValidatedDirectory(dir)) {
                File[] fs = listChildren(dir)
                    .filter(f -> f.getPath().endsWith(".ws"))
                    .toArray(File[]::new);
                stream(fs)
                    .map(f -> APP.serializerXml.fromXML(WindowState.class, f)
                                .map(WindowState::toWindow)
                                .getOr(null)
                    )
                    .filter(ISNTØ)
                    .forEach(ws::add);
                canBeMainTemp = false;
                LOGGER.info("Restored " + fs.length + "/" + ws.size() + " windows.");
            } else {
                LOGGER.error("Restoring windows/layouts failed: {} not accessible.", dir);
                return;
            }
         }

        // show windows
        if (ws.isEmpty()) {
            if(load_normally)
                ws.add(createWindow(true));
        } else {
            ws.forEach(w -> addEventHandler1Time(w.s, WINDOW_SHOWING, e -> w.update()));
            ws.forEach(Window::show);
            Widget.deserializeWidgetIO();
        }
    }

/* ------------------------------------------------------------------------------------------------------------------ */

    // TODO: remove
    public Window createWindow(Component c) {
        Window w = createWindow();
        w.setContent(c);
        c.focus();
        return w;
    }

    /**
     * @param c non-null widget widget to open
     */
    public Window showWindow(Component c) {
        noNull(c);

        Window w = create();
        w.initLayout();
        w.setContent(c);
        c.focus();
        w.show();
        w.setXYToCenter(getScreenForMouse());
        return w;
    }

    public PopOver showFloating(Widget c) {
        noNull(c);

        Layout l = Layout.openStandalone(new AnchorPane());
        PopOver<?> p = new PopOver<>(l.getRoot());
        p.title.set(c.getInfo().nameGui());
        p.setAutoFix(false);

        p.addEventFilter(WINDOW_HIDING, we -> l.close());
        l.setChild(c);  // load widget when graphics ready & shown
        c.focus();

        p.show(ScreenPos.APP_CENTER);
        return p;
    }

    public void showSettings(Configurable c, MouseEvent e) {
        showSettings(c, (Node) e.getSource());
    }

    public void showSettings(Configurable c, Node n) {
        showSettingsSimple(c, n);
        // TODO: decide whether we use SimpleConfigurator or Configurator widget
//		String name = c instanceof Widget ? ((Widget)c).getName() : "";
//		Configurator sc = new Configurator(true);
//		sc.configure(c);
//		PopOver p = new PopOver<>(sc);
//		p.title.set((name==null ? "" : name+" ") + " Settings");
//		p.setArrowSize(0); // auto-fix breaks the arrow position, turn off - sux
//		p.setAutoFix(true); // we need auto-fix here, because the popup can get rather big
//		p.setAutoHide(true);
//		p.show(n);
    }

    public void showSettingsSimple(Configurable<?> c, MouseEvent e) {
        showSettingsSimple(c, (Node) e.getSource());
    }

    public void showSettingsSimple(Configurable<?> c, Node n) {
        boolean isComponent = c instanceof Component;
        String name = c instanceof Widget ? ((Widget) c).getName() : "";
        SimpleConfigurator<?> sc = simpleConfigurator(c);
        PopOver<?> p = new PopOver<>(sc);
        p.title.set((name==null ? "" : name + " ") + " Settings");
        p.arrowSize.set(0); // auto-fix breaks the arrow position, turn off - sux
        p.setAutoFix(true); // we need auto-fix here, because the popup can get rather big
        p.setAutoHide(true);
        if (isComponent) p.addEventFilter(WINDOW_HIDING, we -> ((Component) c).close());
        p.showInCenterOf(n);
    }

    public PopOver showFloating(Node content, String title) {
        noNull(content);
        noNull(title);

        PopOver<?> p = new PopOver<>(content);
        p.title.set(title);
        p.setAutoFix(false);
        Window w = getActive().get();
        p.show(w.getStage(), w.getCenterX(), w.getCenterY());
        return p;
    }

    public void launchComponent(File launcher) {
        launchComponent(instantiateComponent(launcher));
    }

    public void launchComponent(String name) {
        ComponentFactory<?> wf = APP.widgetManager.factories.getComponentFactory(name);
        Component w = wf==null ? null : wf.create();
        launchComponent(w);
    }

    public void launchComponent(Component w) {
        try {

        if (w!=null) {
            if (windows.isEmpty()) {
                getActiveOrNew().setContent(w);
            } else {
                createWindow(w);
            }
        }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public Component instantiateComponent(File launcher) {
        ComponentFactory<?> wf;
        Component c = null;

        // try to build widget using just launcher filename
        boolean isLauncherEmpty = Util.readFileLines(launcher).count()==0;
        String wn = isLauncherEmpty ? Util.getName(launcher) : "";
        wf = APP.widgetManager.factories.getComponentFactory(wn);
        if (wf!=null)
            c = wf.create();

        // try to deserialize normally
        if (c==null)
            c = APP.serializerXml.fromXML(Component.class, launcher).getOr(null);

        return c;
    }

}