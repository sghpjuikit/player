package webReader;

import java.io.File;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import layout.widget.Widget;
import layout.widget.controller.FXMLController;
import util.conf.IsConfig;
import util.conf.IsConfig.EditMode;
import util.dev.Dependency;
import util.file.Util;

import static main.App.APP;
import static util.dev.Util.log;
import static util.reactive.Util.maintain;
import static util.type.Util.getFieldValue;
import static util.type.Util.invokeMethodP1;

/**
 * Web browser component.
 *
 * @author Martin Polakovic
 */
@Widget.Info(
	name = "WebReader",
	author = "Martin Polakovic",
	howto = "",
	description = "Very simple web browser widget.",
	notes = "",
	version = "0.8",
	year = "2015",
	group = Widget.Group.OTHER
)
public class WebReader extends FXMLController {

    @FXML
    private TextField addressBar;
    @FXML
    private WebView webView;
    private WebEngine engine;

    @IsConfig(name = "Last visited address", info = "Last visited address", editable = EditMode.APP)
    public String url = "http://duckduckgo.com/";

    @Override
    public void init() {
        engine = webView.getEngine();

	    File userDir = new File(APP.DIR_USERDATA, "webdata");
	    Util.isValidatedDirectory(userDir);
		engine.setUserDataDirectory(userDir);

//	    webView.setBlendMode(BlendMode.DIFFERENCE);
//	    webView.setStyle("-fx-background-color: red;");

        addressBar.setOnKeyPressed( e -> {
            if (e.getCode().equals(KeyCode.ENTER))
                refresh();
        });
        engine.locationProperty().addListener(o -> {
            url = engine.getLocation();
            addressBar.setText(engine.getLocation());
        });
	    d(maintain(engine.documentProperty(), doc -> setTransparentBgrColor()));
	    getInputs().create("Html", String.class, this::loadPage);

//	    try {
////		    engine.setUserStyleSheetLocation(new File(APP.DIR_SKINS.getPath(), Gui.skin.get() + separator + Gui.skin.get() + ".css").toURI().toURL().toExternalForm());
//		    engine.setUserStyleSheetLocation(new File(APP.DIR_SKINS.getPath(), "Test" + separator + "Test.css").toURI().toURL().toExternalForm());
//	    } catch (MalformedURLException e) {
//		    log(WebReader.class).error("Could not apply app skin on the web reader");
//	    }

    }

    @FXML
    @Override
    public void refresh() {
        if (addressBar.getText().isEmpty()) { // TODO: improve this
            // For now we do not reload the page, just set up the address
	        // loadPage(url);
	        addressBar.setText(url);
        }

	    // setTransparentBgrColor(); // !work, probably due to some initialization
        loadPage(" ");
    }

    public void reloadPage() {
        engine.load(addressBar.getText());
    }

    public void loadPage(String page) {
	    if (page!=null) {
		    //			    String s = "<body text=white>" + text + "</body>";
		    String s = page;
		    engine.loadContent(s);
	    }
    }

	@Dependency("requires access to javafx.web/com.sun.webkit.WebPage")
	private void setTransparentBgrColor() {
		// TODO: jigsaw
		try {
			// Use reflection to retrieve the WebEngine's private 'page' field.
			Object webPage = getFieldValue(engine, "page");
			invokeMethodP1(webPage, "setBackgroundColor", int.class, new java.awt.Color(255, 255, 255, 1).getRGB());
		} catch (Exception e) {
			log(WebReader.class).error("Could not change background color to transparent", e);
		}
	}
}