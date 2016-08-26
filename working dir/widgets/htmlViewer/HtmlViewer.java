package htmlViewer;

import javafx.scene.web.HTMLEditor;

import layout.widget.Widget;
import layout.widget.controller.ClassController;
import layout.widget.controller.io.Input;
import layout.widget.controller.io.Output;
import util.access.V;
import util.conf.IsConfig;

import static javafx.util.Duration.seconds;
import static util.async.Async.runPeriodic;
import static util.graphics.Util.setAnchor;

/**
 * Web browser component.
 *
 * @author Martin Polakovic
 */
@Widget.Info(
	name = "HtmlEditor",
	author = "Martin Polakovic",
	howto = "",
	description = "Very simple html text editor.",
	notes = "",
	version = "0.5",
	year = "2016",
	group = Widget.Group.OTHER
)
public class HtmlViewer extends ClassController {

	HTMLEditor editor = new HTMLEditor();

    @IsConfig(name = "Last visited address", info = "Last visited address", editable = false)
    final V<String> text = new V<>("", editor::setHtmlText);

    @Override
    public void init() {
	    Input<String> input = getInputs().create("Html", String.class, text::setValue);
	    d(input.monitor(text::setValue));

	    Output<String> output = getOutputs().create(widget.id, "Html", String.class, "");
	    d(text.maintain(output::setValue));

	    d(runPeriodic(seconds(5), timer -> text.setValue(editor.getHtmlText()))::stop);
	    setAnchor(this, editor, 0d);
    }

}