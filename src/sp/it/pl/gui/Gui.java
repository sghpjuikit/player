package sp.it.pl.gui;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javafx.animation.Animation;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sp.it.pl.gui.objects.window.stage.Window;
import sp.it.pl.gui.objects.window.stage.WindowBase;
import sp.it.pl.layout.container.layout.Layout;
import sp.it.pl.layout.container.switchcontainer.SwitchPane;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.WidgetManager.WidgetSource;
import sp.it.pl.util.access.V;
import sp.it.pl.util.access.VarEnum;
import sp.it.pl.util.action.IsAction;
import sp.it.pl.util.action.IsActionable;
import sp.it.pl.util.animation.Anim;
import sp.it.pl.util.conf.IsConfig;
import sp.it.pl.util.conf.IsConfigurable;
import sp.it.pl.util.file.Util;
import sp.it.pl.util.validation.Constraint;
import static java.util.stream.Collectors.toSet;
import static javafx.util.Duration.millis;
import static sp.it.pl.gui.Gui.OpenStrategy.INSIDE;
import static sp.it.pl.gui.GuiKt.applySkin;
import static sp.it.pl.main.AppUtil.APP;
import static sp.it.pl.util.file.UtilKt.listChildren;
import static sp.it.pl.util.functional.Util.set;
import static sp.it.pl.util.graphics.UtilKt.isAnyParentOf;

@IsActionable
@IsConfigurable
public class Gui {

	private static final Logger LOGGER = LoggerFactory.getLogger(Gui.class);

	static final Set<SkinCss> skins = new HashSet<>();

	public static final BooleanProperty layout_mode = new SimpleBooleanProperty(false);
	public static final Consumer<Node> focusChangedHandler = n -> {
		Scene window = n==null ? null : n.getScene();
		(n==null ? Stream.<Widget<?>>empty() : APP.widgetManager.findAll(WidgetSource.ANY))
			.filter(w -> w.areaTemp!=null && isAnyParentOf(w.areaTemp.getRoot(), n))
			.findAny().ifPresent(fw -> {
				APP.widgetManager.findAll(WidgetSource.ANY)
					.filter(w -> w!=fw)
					.filter(w -> w.getWindow().map(Window::getStage).map(Stage::getScene).map(s -> s==window).orElse(false))
					.forEach(w -> w.focused.set(false));
				fw.focused.set(true);
			});
	};
	public static void focusClickedWidget(MouseEvent e) {
		Node n = e.getTarget() instanceof Node ? (Node) e.getTarget() : null;
		(n==null ? Stream.<Widget<?>>empty() : APP.widgetManager.findAll(WidgetSource.ANY))
			.filter(w -> !w.focused.get() && w.isLoaded() && isAnyParentOf(w.load(), n))
			.findAny().ifPresent(w -> w.focus());
	}

	@IsConfig(name = "Skin", info = "Application skin.")
	public static final VarEnum<String> skin = VarEnum.ofStream("Flow", () -> skins.stream().map(s -> s.name));

	/**
	 * Font of the application. Overrides font defined by skin. The font can be
	 * overridden programmatically or stylesheet.
	 * <p>
	 * Note: font is applied only if the GUI is fully initialized, otherwise does
	 * nothing.
	 */
	@IsConfig(name = "Font", info = "Application font.")
	public static final V<Font> font = new V<>(Font.getDefault());

	// non applied configs
	@IsConfig(name = "Layout mode blur bgr", info = "Layout mode use blur effect.")
	public static boolean blur_layoutMode = false;
	@IsConfig(name = "Layout mode fade bgr", info = "Layout mode use fade effect.")
	public static boolean opacity_layoutMode = true;
	@IsConfig(name = "Layout mode fade intensity", info = "Layout mode fade effect intensity.")
	@Constraint.MinMax(min = 0, max = 1)
	public static double opacity_LM = 1;
	@IsConfig(name = "Layout mode blur intensity", info = "Layout mode blur effect intensity.")
	@Constraint.MinMax(min = 0, max = 20)
	public static double blur_LM = 4;
	@IsConfig(name = "Layout mode anim length", info = "Duration of layout mode transition effects.")
	public static Duration duration_LM = millis(250);
	@IsConfig(name = "Snap", info = "Allows snapping feature for windows and controls.")
	public static final V<Boolean> snapping = new V<>(true);
	@IsConfig(name = "Snap activation distance", info = "Distance at which snap feature gets activated")
	public static final V<Double> snapDistance = new V<>(6d);
	@IsConfig(name = "Lock layout", info = "Locked layout will not enter layout mode.")
	public final static BooleanProperty locked_layout = new SimpleBooleanProperty(false) {
		@Override
		public void set(boolean v) {
			super.set(v);
			APP.actionStream.push("Layout lock");
		}
	};
	@IsConfig(name = "Layout open strategy", info = "How will certain layout element open and close.")
	public static OpenStrategy open_strategy = INSIDE;

	@IsConfig(name = "Table orientation", group = "Table", info = "Orientation of the table.")
	public static final ObjectProperty<NodeOrientation> table_orient = new SimpleObjectProperty<>(NodeOrientation.INHERIT);
	@IsConfig(name = "Zeropad numbers", group = "Table", info = "Adds 0s for number length consistency.")
	public static final V<Boolean> table_zeropad = new V<>(false);
	@IsConfig(name = "Search show original index", group = "Table", info = "Show unfiltered table item index when filter applied.")
	public static final V<Boolean> table_orig_index = new V<>(false);
	@IsConfig(name = "Show table header", group = "Table", info = "Show table header with columns.")
	public static final V<Boolean> table_show_header = new V<>(true);
	@IsConfig(name = "Show table controls", group = "Table", info = "Show table controls at the bottom of the table. Displays menu bar and table items information")
	public static final V<Boolean> table_show_footer = new V<>(true);

	static {
		GuiKt.initSkins();
	}

	/**
	 * Component might rely on this method to alter its behavior. For example
	 * it can leave layout mode on its own independently but forbid this
	 * behavior if this method returns TRUE.
	 * <p/>
	 * Note that this method consistently returns FALSE at the time
	 * of entering and leaving the layout mode, thus allowing to safely query
	 * layout mode state just before the state change. In other words the state
	 * change itself will not influence the result, which changes only after the
	 * change occurred. not after it was invoked.
	 * <p/>
	 * Basically, at the time of invoking show() and hide() methods the flag
	 * is (and must be) FALSE and never TRUE
	 *
	 * @return whether layout mode was on or not.
	 */
	public static boolean isLayoutMode() {
		return layout_mode.get();
	}

	public static BooleanProperty layoutLockedProperty() {
		return locked_layout;
	}

	/** Set lock to prevent layouting. */
	public static void setLayoutlocked(boolean val) {
		locked_layout.set(val);
	}

	/** Return lock to prevent layouting. */
	public static boolean isLayoutLocked() {
		return locked_layout.get();
	}

	/** Toggles lock to prevent layouting. */
	@IsAction(name = "Toggle layout lock", desc = "Lock/unlock layout.", keys = "F4")
	public static void toggleLayoutLocked() {
		locked_layout.set(!locked_layout.get());
	}

	/** Loads/refreshes active layout. */
	@IsAction(name = "Reload layout", desc = "Reload layout.", keys = "F6")
	public static void loadLayout() {
		APP.widgetManager.getLayouts().forEach(Layout::load);
	}

	/** Toggles layout controlling mode. */
	@IsAction(name = "Reload skin", desc = "Reloads skin.", keys = "F7")
	public static void reloadSkin() {
		LOGGER.info("Reloading skin={}", skin.get());
		applySkin(skin.get());
	}

	/**
	 * Sets layout mode on/off.
	 * <p/>
	 * Note that {@link #isLayoutMode()} consistently returns FALSE at the time
	 * of entering and leaving the layout mode.
	 *
	 * @see #isLayoutMode()
	 */
	public static void setLayoutMode(boolean val) {
		// avoid pointless operation
		if (layout_mode.get()==val) return;
		// Note that we set the layout mode flag after invoking show() but
		// before invoking hide().
		// This is important to maintain consistency. See documentation.
		if (val) {
			APP.widgetManager.getLayouts().forEach(Layout::show);
			layout_mode.set(val);
		} else {
			layout_mode.set(val);
			APP.widgetManager.getLayouts().forEach(Layout::hide);
			setZoomMode(false);
		}
		if (val) APP.actionStream.push("Layout mode");
	}

	public static void setZoomMode(boolean val) {
		APP.windowManager.getFocused().map(Window::getSwitchPane).ifPresent(sp -> sp.zoom(val));
	}

	/** Toggles layout mode. */
	@IsAction(name = "Manage Layout", desc = "Toggles layout mode on/off.")
	public static void toggleLayoutMode() {
		setLayoutMode(!layout_mode.get());
	}

	/** Toggles zoom mode. */
	@IsAction(name = "Zoom Layout", desc = "Toggles layout zoom in/out.")
	public static void toggleZoomMode() {
		APP.windowManager.getFocused().map(Window::getSwitchPane).ifPresent(SwitchPane::toggleZoom);
	}

	public static void setLayoutNzoom(boolean v) {
		setLayoutMode(v);
		setZoomMode(v);
	}

	@IsAction(name = "Manage Layout & Zoom", desc = "Enables layout managment mode and zooms.", keys = "F8")
	public static void toggleLayoutNzoom() {
		toggleLayoutMode();
		toggleZoomMode();
	}

	@IsAction(name = "Show/Hide application", desc = "Equal to switching minimized mode.", keys = "CTRL+ALT+W", global = true)
	@IsAction(name = "Minimize", desc = "Switch minimized mode.", keys = "F10")
	public static void toggleMinimizeFocus() {
		// After this operation, all windows are either minimized or not, but before it, the state
		// may vary. Thus we need to decide whether we minimize all windows or the opposite.
		// if any window is not minimized, app
		boolean m = APP.windowManager.windows.stream().anyMatch(w -> w.isMinimized());
		boolean f = APP.windowManager.windows.stream().anyMatch(w -> w.focused.get());
		APP.windowManager.windows.forEach((!m && !f) ? Window::focus : w -> w.setMinimized(!m));
	}

	@IsAction(name = "Show application", desc = "Shows application.", global = true)
	public static void showApp() {
		APP.windowManager.windows.forEach(w -> w.setMinimized(false));
	}

	@IsAction(name = "Hide application", desc = "Hides application.", global = true)
	public static void hideApp() {
		APP.windowManager.windows.forEach(w -> w.setMinimized(true));
	}

	public static void toggleMinimize() {
		boolean m = APP.windowManager.windows.stream().anyMatch(Window::isMinimized);
		APP.windowManager.windows.forEach(w -> w.setMinimized(!m));
	}

	@IsAction(name = "Maximize", desc = "Switch maximized mode.", keys = "F11")
	public static void toggleMaximize() {
		APP.windowManager.getActive().ifPresent(WindowBase::toggleMaximize);
	}

	@IsAction(name = "Loop maximized state", desc = "Switch to different maximized window states.", keys = "F3")
	public static void toggleMaximizedState() {
		APP.windowManager.getActive().ifPresent(w -> w.setMaximized(w.isMaximized().next()));
	}

	@IsAction(name = "Fullscreen", desc = "Switch fullscreen mode.", keys = "F12")
	public static void toggleFullscreen() {
		APP.windowManager.getActive().ifPresent(WindowBase::toggleFullscreen);
	}

	@IsAction(name = "Align tabs", desc = "Aligns tabs to the window", keys = "SHIFT+UP")
	public static void tabAlign() {
		APP.windowManager.getActive().map(Window::getSwitchPane).ifPresent(SwitchPane::alignTabs);
	}

	@IsAction(name = "Align to next tab", desc = "Goes to next tab and aligns tabs to the window", keys = "SHIFT+RIGHT")
	public static void tabNext() {
		APP.windowManager.getActive().map(Window::getSwitchPane).ifPresent(SwitchPane::alignRightTab);
	}

	@IsAction(name = "Align to previous tab", desc = "Goes to previous tab and aligns tabs to the window", keys = "SHIFT+LEFT")
	public static void tabPrevious() {
		APP.windowManager.getActive().map(Window::getSwitchPane).ifPresent(SwitchPane::alignLeftTab);
	}

	/**
	 * Searches for .css files in skin folder and registers them as available
	 * skins. Use on app start or to discover newly added layouts dynamically.
	 */
	public static Set<SkinCss> getSkins() {
		File dir = APP.DIR_SKINS;
		if (!Util.isValidatedDirectory(dir)) {
			LOGGER.error("Skin lookup failed." + dir.getPath() + " could not be accessed.");
			return set();
		}

		return listChildren(dir)
			.filter(File::isDirectory)
			.map(d -> {
				String name = d.getName();
				File css = new File(d, name + ".css");
				if (Util.isValidFile(css)) {
					LOGGER.info("Registering skin: " + name);
					return new SkinCss(css);
				} else {
					return null;
				}
			})
			.filter(o -> o!=null)
			.collect(toSet());
	}

	private static SkinCss registerSkin(SkinCss s) {
		LOGGER.info("Registering skin={}", s.name);
		skins.add(s);
		return s;
	}

	public static void setSkin(SkinCss s) {
		LOGGER.info("Setting skin={}", s.name);

		registerSkin(s);
		skin.set(s.name);
	}

	public static void setSkin(File cssFile) {
		LOGGER.info("Setting skin file={}", cssFile);

		SkinCss s = skins.stream()
			.filter(ss -> ss.file.equals(cssFile)).findAny()
			.orElseGet(() -> registerSkin(new SkinCss(cssFile)));
		setSkin(s);
	}


	public static final Duration ANIM_DUR = Duration.millis(300);

    public static void closeAndDo(Node n, Runnable action) {
    	Anim a = (Anim) n.getProperties().computeIfAbsent("ANIMATION_OPEN_CLOSE", k -> buildAnimation(n));
    	if (!a.isRunning()) a.applier.accept(1);
    	a.playCloseDo(action);
    }

    public static void openAndDo(Node n, Runnable action) {
	    Anim a = (Anim) n.getProperties().computeIfAbsent("ANIMATION_OPEN_CLOSE", k -> buildAnimation(n));
    	if (!a.isRunning()) a.applier.accept(0);
	    a.playOpenDo(action);
    }

    private static Animation buildAnimation(Node n) {
        return new Anim(ANIM_DUR, x -> n.setOpacity(x*x));
    }


	public enum OpenStrategy {
		POPUP, INSIDE
	}
}