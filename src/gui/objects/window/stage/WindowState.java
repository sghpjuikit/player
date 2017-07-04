package gui.objects.window.stage;

import gui.objects.window.stage.WindowBase.Maximized;
import layout.container.layout.Layout;
import static main.App.APP;

class WindowState {
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
		Window window = APP.windowManager.create(WindowManager.canBeMainTemp);
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
