/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package logger;

import java.util.function.Consumer;

import javafx.beans.property.BooleanProperty;
import javafx.scene.control.TextArea;

import layout.widget.Widget;
import layout.widget.controller.ClassController;
import util.conf.IsConfig;

import static main.App.APP;
import static util.graphics.Util.setAnchors;
import static util.graphics.Util.setMinPrefMaxSize;

/**
 * Logger widget conroller.
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

    @IsConfig(name = "Wrap text", info = "Wrap text at the end of the text area to the next line.")
    public final BooleanProperty wrap_text = area.wrapTextProperty(); // default == false

    public Logger() {
        area.setEditable(false);
        setMinPrefMaxSize(area, USE_COMPUTED_SIZE);
        setMinPrefMaxSize(this, USE_COMPUTED_SIZE);
        area.appendText("# This is redirected System.out stream of this application.\n");
        getChildren().add(area);
        setAnchors(area, 0d);

        APP.systemout.addListener(writer);
    }

    @Override
    public void onClose() {
        APP.systemout.removeListener(writer);
    }

}