package terminal;

import com.terminalfx.TerminalBuilder;
import com.terminalfx.TerminalTab;
import com.terminalfx.config.TerminalConfig;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import java.io.File;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.controller.ClassController;
import sp.it.pl.util.access.V;
import sp.it.pl.util.conf.IsConfig;
import sp.it.pl.util.graphics.drag.Placeholder;
import sp.it.pl.util.system.Os;
import sp.it.pl.util.validation.Constraint.FileType;
import static sp.it.pl.util.functional.Util.stream;
import static sp.it.pl.util.graphics.Util.setAnchors;
import static sp.it.pl.util.reactive.Util.maintain;
import static sp.it.pl.util.reactive.Util.sizeOf;
import static sp.it.pl.util.validation.Constraint.FileActor.FILE;

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
	private final Placeholder p = new Placeholder(FontAwesomeIcon.TERMINAL, "New terminal (Ctrl+T)", this::openTerminal);

	@FileType(FILE)
	@IsConfig(name = "Shell path", info = "Path to the shell or none for default")
	private final V<File> shellPath = new V<>(null, v -> {
		if (Os.WINDOWS.isCurrent()) {
			closeAllTabs();
			tConfig.setWindowsTerminalStarter(v==null ? "cmd.exe" : v.getAbsolutePath());
		}
		if (Os.UNIX.isCurrent()) {
			closeAllTabs();
			tConfig.setUnixTerminalStarter(v==null ? "/bin/bash -i" : v.getAbsolutePath());
		}
	});

	public Terminal() {
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
		d(() -> closeAllTabs());
	}

	@Override
	public void refresh() {
		if (p.isVisible()) p.requestFocus();
		shellPath.applyValue();
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
					.filter(TerminalTab.class::isInstance).map(TerminalTab.class::cast)
					.filter(TerminalTab::isSelected).findFirst()
					.ifPresent(TerminalTab::closeTerminal);

			e.consume();
		}
	}

	private void closeAllTabs() {
		tabPane.getTabs().stream()
			.filter(TerminalTab.class::isInstance).map(TerminalTab.class::cast)
			.findAny().ifPresent(TerminalTab::closeAllTerminal);
		tabPane.getTabs().clear();
	}
}