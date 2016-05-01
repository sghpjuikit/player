
package logger;

import java.util.function.Consumer;

import javafx.beans.property.BooleanProperty;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.TextArea;

import org.reactfx.Subscription;

import layout.widget.Widget;
import layout.widget.controller.ClassController;
import util.conf.IsConfig;

import static main.App.APP;
import static util.functional.Util.toS;
import static util.graphics.Util.setAnchors;
import static util.graphics.Util.setMinPrefMaxSize;

/**
 * Logger widget controller.
 *
 * @author Martin Polakovic
 */
@Widget.Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
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
    private final Consumer<String> writer = area::appendText;
    Subscription d;

    @IsConfig(name = "Wrap text", info = "Wrap text at the end of the text area to the next line.")
    public final BooleanProperty wrap_text = area.wrapTextProperty();

    public Logger() {
        area.setEditable(false);
	    area.setWrapText(false);
        area.appendText("# This is redirected System.out stream of this application.\n");
        setMinPrefMaxSize(area, USE_COMPUTED_SIZE);
        setMinPrefMaxSize(this, USE_COMPUTED_SIZE);
        getChildren().add(area);
        setAnchors(area, 0d);

        APP.systemout.addListener(writer);

        EventHandler<javafx.scene.input.MouseEvent> h = e -> {
            if(e.getPickResult().getIntersectedNode() instanceof Node) {
                System.out.println();
                System.out.println(toS(((Node)e.getPickResult().getIntersectedNode()).getStyleClass()) + " - " + toS(((Node)e.getPickResult().getIntersectedNode()).getPseudoClassStates()));
                System.out.println();
            }
        };
        APP.windowManager.windows.forEach(w -> w.getStage().getScene().getRoot().addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, h));
        d = () -> APP.windowManager.windows.forEach(w -> w.getStage().getScene().getRoot().removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, h));
    }

    @Override
    public void onClose() {
        APP.systemout.removeListener(writer);
    }

}