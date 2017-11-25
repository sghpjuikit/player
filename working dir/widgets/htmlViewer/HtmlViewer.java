package htmlViewer;

import javafx.scene.web.HTMLEditor;

import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.controller.ClassController;
import sp.it.pl.layout.widget.controller.io.Input;
import sp.it.pl.layout.widget.controller.io.Output;
import sp.it.pl.util.access.V;
import sp.it.pl.util.conf.IsConfig;
import sp.it.pl.util.conf.IsConfig.EditMode;

import static javafx.util.Duration.seconds;
import static sp.it.pl.util.async.AsyncKt.runPeriodic;
import static sp.it.pl.util.graphics.Util.setAnchor;

/**
 * Web browser component.
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

	public final HTMLEditor editor = new HTMLEditor();

    @IsConfig(name = "Last visited address", info = "Last visited address", editable = EditMode.APP)
    final V<String> text = new V<>("", editor::setHtmlText);

    @Override
    public void init() {
	    Input<String> input = getInputs().create("Html", String.class, text::setValue);
	    d(input.monitor(text::setValue));

	    Output<String> output = getOutputs().create(widget.id, "Html", String.class, "");
	    d(text.maintain(output::setValue));

	    d(runPeriodic(seconds(5), () -> text.setValue(editor.getHtmlText()))::stop);
	    setAnchor(this, editor, 0d);
    }

}