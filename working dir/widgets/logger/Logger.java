package logger;

import javafx.beans.property.BooleanProperty;
import javafx.scene.control.TextArea;

import layout.widget.Widget;
import layout.widget.controller.ClassController;
import util.conf.IsConfig;

import static main.App.APP;
import static util.graphics.Util.setAnchors;
import static util.graphics.Util.setMinPrefMaxSize;

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
        area.setEditable(false);
	    area.setWrapText(false);
        area.appendText("# This is redirected output (System.out) stream of this application.\n");
        setMinPrefMaxSize(area, USE_COMPUTED_SIZE);
        setMinPrefMaxSize(this, USE_COMPUTED_SIZE);
        getChildren().add(area);
        setAnchors(area, 0d);

        d(APP.systemout.addListener(area::appendText));
    }

}