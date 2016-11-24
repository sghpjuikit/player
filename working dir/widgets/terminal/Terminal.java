package terminal;

import javafx.scene.control.TabPane;
import javafx.scene.paint.Color;

import com.terminalfx.TerminalBuilder;
import com.terminalfx.TerminalTab;
import com.terminalfx.config.TerminalConfig;
import layout.widget.Widget;
import layout.widget.controller.ClassController;

import static util.graphics.Util.setAnchors;

/**
 * Logger widget controller.
 *
 * @author Martin Polakovic
 */
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

	public Terminal() {
		TerminalConfig cfg = new TerminalConfig();
		cfg.setWindowsTerminalStarter("C:\\Program Files\\Git\\usr\\bin\\bash"); // TODO: make configurable
		cfg.setBackgroundColor(Color.rgb(16, 16, 16));
		cfg.setForegroundColor(Color.rgb(240, 240, 240));
		cfg.setCursorColor(Color.rgb(255, 0, 0, 0.1));
		cfg.setScrollbarVisible(false);

		TerminalBuilder terminalBuilder = new TerminalBuilder(cfg);
		TerminalTab terminal = terminalBuilder.newTerminal();

		TabPane tabPane = new TabPane();
		tabPane.getTabs().add(terminal);

		getChildren().add(tabPane);
		setAnchors(tabPane, 0d);
	}

}