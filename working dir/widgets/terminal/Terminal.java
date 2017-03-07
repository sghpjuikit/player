package terminal;

import com.terminalfx.TerminalBuilder;
import com.terminalfx.TerminalTab;
import com.terminalfx.config.TerminalConfig;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.paint.Color;
import layout.widget.Widget;
import layout.widget.controller.ClassController;
import util.graphics.drag.Placeholder;
import static util.functional.Util.list;
import static util.functional.Util.stream;
import static util.graphics.Util.setAnchors;
import static util.reactive.Util.maintain;
import static util.reactive.Util.sizeOf;

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

	public Terminal() {
		tConfig.setWindowsTerminalStarter("C:\\Program Files\\Git\\usr\\bin\\bash"); // TODO: make configurable
		tConfig.setBackgroundColor(Color.rgb(16, 16, 16));
		tConfig.setForegroundColor(Color.rgb(240, 240, 240));
		tConfig.setCursorColor(Color.rgb(255, 0, 0, 0.1));
		tConfig.setScrollbarVisible(false);
		tabPane.setTabClosingPolicy(TabClosingPolicy.ALL_TABS);

		getChildren().add(tabPane);
		setAnchors(tabPane, 0d);

		Placeholder p = new Placeholder(FontAwesomeIcon.TERMINAL, "New terminal", this::openTerminal);
		d(sizeOf(tabPane.getTabs(), i -> p.show(this, i==0)));
		d(maintain(p.visibleProperty(), v -> !v, tabPane.visibleProperty()));

		d(() -> stream(tabPane.getTabs()).select(TerminalTab.class).findAny().ifPresent(TerminalTab::closeAllTerminal));
	}

	private void openTerminal() {
		TerminalTab terminal = new TerminalTab(tConfig, tBuilder.getNameGenerator(), tBuilder.getTerminalPath());
		tabPane.getTabs().add(terminal);
	}
}