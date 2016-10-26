package logger;

import javafx.beans.property.BooleanProperty;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.paint.Color;

import com.terminalfx.TerminalBuilder;
import com.terminalfx.TerminalTab;
import com.terminalfx.config.TerminalConfig;

import layout.widget.Widget;
import layout.widget.controller.ClassController;
import util.async.Async;
import util.conf.IsConfig;

/**
 * Logger widget controller.
 *
 * @author Martin Polakovic
 */
@Widget.Info(
    author = "Martin Polakovic",
    name = "Logger",
    description = "Displays console output by listening to System.out, which contains all of the "
            + "application logging.",
    howto = "",
    notes = "",
    version = "1",
    year = "2015",
    group = Widget.Group.DEVELOPMENT
)
public class Logger extends ClassController {

    private final TextArea area = new TextArea();

    @IsConfig(name = "Wrap text", info = "Wrap text at the end of the text area to the next line.")
    public final BooleanProperty wrap_text = area.wrapTextProperty();

    public Logger() {
//        area.setEditable(false);
//	    area.setWrapText(false);
//        area.appendText("# This is redirected output (System.out) stream of this application.\n");
//        setMinPrefMaxSize(area, USE_COMPUTED_SIZE);
//        setMinPrefMaxSize(this, USE_COMPUTED_SIZE);
//        getChildren().add(area);
//        setAnchors(area, 0d);
//
//        d(APP.systemout.addListener(area::appendText));




	    TerminalConfig darkConfig = new TerminalConfig();
	    darkConfig.setBackgroundColor(Color.rgb(16, 16, 16));
	    darkConfig.setForegroundColor(Color.rgb(240, 240, 240));
	    darkConfig.setCursorColor(Color.rgb(255, 0, 0, 0.5));

	    TerminalBuilder terminalBuilder = new TerminalBuilder(darkConfig);
	    TerminalTab terminal = terminalBuilder.newTerminal();

	    TabPane tabPane = new TabPane();
	    tabPane.getTabs().add(terminal);
	    getChildren().add(tabPane);
	    Async.run(1000, () -> {
		    try {
		    	terminal.initialize();
		    	terminal.focusCursor();
//		    	terminal.onTerminalReady();
//			    terminal.command("pwd");
		    } catch(Throwable e) {
			    System.out.println(e);
		    }
	    });
    }

}