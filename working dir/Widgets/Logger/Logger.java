/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javafx.scene.control.TextArea;

import Layout.widget.Widget;
import Layout.widget.controller.ClassController;

import static util.async.Async.runFX;
import static util.graphics.Util.setAnchors;

/**
 *
 * @author Plutonium_
 */
@Widget.Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Logger",
    description = "Displays console output by redirecting System.out, which includes all of the "
            + "application logging.",
    howto = "",
    notes = "",
    version = "0.8",
    year = "2015",
    group = Widget.Group.DEVELOPMENT
)
public class Logger extends ClassController {

    private final TextArea area = new TextArea();
    private final PrintStream stream;

    public Logger() {
        // gui
        area.setEditable(false);
        area.appendText("# This is redirected System.out stream of this application.");
        getChildren().add(area);
        setAnchors(area, 0d);

        // catch output stream
        stream = new java.io.PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                runFX(() -> {
                    area.appendText(String.valueOf((char) b));
                });
            }
        }, true);
        System.setOut(stream);
    }

    @Override
    public void onClose() {
        stream.close();
    }

}