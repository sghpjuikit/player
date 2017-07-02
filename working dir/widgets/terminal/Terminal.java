package terminal;

import com.terminalfx.TerminalBuilder;
import com.terminalfx.TerminalTab;
import com.terminalfx.config.TerminalConfig;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import layout.widget.Widget;
import layout.widget.controller.ClassController;
import util.graphics.drag.Placeholder;
import static util.functional.Util.stream;
import static util.graphics.Util.setAnchors;
import static util.reactive.Util.maintain;
import static util.reactive.Util.sizeOf;

@SuppressWarnings("unused")
@Widget.Info(
		author = "Martin Polakovic",
		name = "Terminal",
		description = "Terminal for working with shell.",
//		howto = "",
//		notes = "",
		version = "0.5",
		year = "2015",
		group = Widget.Group.DEVELOPMENT
)
public class Terminal extends ClassController {

	private final TerminalConfig tConfig = new TerminalConfig();
	private final TerminalBuilder tBuilder = new TerminalBuilder(tConfig);
	private final TabPane tabPane = new TabPane();
	private final Placeholder p = new Placeholder(FontAwesomeIcon.TERMINAL, "New terminal", this::openTerminal);

	public Terminal() {
		tConfig.setWindowsTerminalStarter("C:\\software\\Git\\usr\\bin\\bash"); // TODO: make configurable, TODO: handle dir not existing
		tConfig.setBackgroundColor(Color.rgb(16, 16, 16));
		tConfig.setForegroundColor(Color.rgb(240, 240, 240));
		tConfig.setCursorColor(Color.rgb(255, 0, 0, 0.1));
		tConfig.setScrollbarVisible(false);
		tabPane.setTabClosingPolicy(TabClosingPolicy.ALL_TABS);
		tabPane.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKey);
		tabPane.addEventFilter(KeyEvent.KEY_RELEASED, this::handleKey);
		tabPane.addEventFilter(KeyEvent.KEY_TYPED, this::handleKey);

		getChildren().add(tabPane);
		setAnchors(tabPane, 0d);

		d(sizeOf(tabPane.getTabs(), i -> p.show(this, i==0)));
		d(maintain(p.visibleProperty(), v -> !v, tabPane.visibleProperty()));

		d(() -> stream(tabPane.getTabs()).select(TerminalTab.class).findAny().ifPresent(TerminalTab::closeAllTerminal));
	}

	@Override
	public void refresh() {
		if (p.isVisible()) p.requestFocus();
	}

	private void openTerminal() {
		TerminalTab terminal = new TerminalTab(tConfig, tBuilder.getNameGenerator(), tBuilder.getTerminalPath());
		tabPane.getTabs().add(terminal);
	}

	private void handleKey(KeyEvent e) {
		if (e.getEventType() == KeyEvent.KEY_PRESSED && e.isShortcutDown()) {
			if (e.getCode() == KeyCode.T)
				openTerminal();

			if (e.getCode() == KeyCode.W)
				stream(tabPane.getTabs())
					.select(TerminalTab.class)
					.filter(TerminalTab::isSelected).findFirst()
					.ifPresent(TerminalTab::closeTerminal);

			e.consume();
		}
	}
}