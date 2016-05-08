package webReader;

import java.lang.reflect.Field;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import org.w3c.dom.Document;

import layout.widget.Widget;
import layout.widget.controller.FXMLController;
import util.conf.IsConfig;

/**
 * Web browser component.
 *
 * @author Martin Polakovic
 */
@Widget.Info(
	name = "WebReader",
	author = "Martin Polakovic",
	programmer = "Martin Polakovic",
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

    @IsConfig(name = "Last visited address", info = "Last visited address", editable = false)
    public String url = "http://duckduckgo.com/";

    @Override
    public void init() {
        engine = webView.getEngine();

//	    webView.setBlendMode(BlendMode.DIFFERENCE);
//	    webView.setStyle("-fx-background-color: red;");

        addressBar.setOnKeyPressed( e -> {
            if (e.getCode().equals(KeyCode.ENTER))
                refresh();
        });
        engine.locationProperty().addListener( o -> {
            url = engine.getLocation();
            addressBar.setText(engine.getLocation());
        });
	    engine.documentProperty().addListener(new DocListener());
	    getInputs().create("Html", String.class, text -> {
		    if(text!=null) {
//			    String s = "<body text=white>" + text + "</body>";
			    String s = text;
			    engine.loadContent(s);
		    }
	    });
	    engine.setUserStyleSheetLocation("skin/Flow/Flow.css");
    }

    @FXML
    @Override
    public void refresh() {
        if(addressBar.getText().isEmpty()){
            loadPage(url);
            return;
        }
        reloadPage();
    }

    public void reloadPage() {
        engine.load(addressBar.getText());
    }

    public void loadPage(String page) {
        engine.load(page);

//	    com.sun.webkit.WebPage webPage = com.sun.javafx.webkit.Accessor.getPageFor(engine);
//	    webPage.setBackgroundColor((new java.awt.Color(0,0,0, 0)).getRGB());


//	    try {
//		    // Use reflection to retrieve the WebEngine's private 'page' field.
//		    Field f = engine.getClass().getDeclaredField("page");
//		    f.setAccessible(true);
//		    com.sun.webkit.WebPage p = (com.sun.webkit.WebPage) f.get(engine);
//		    				p.setBackgroundColor((new java.awt.Color(0,0,0, 0)).getRGB());
//
//	    } catch (Exception e) {
//	    }
    }


	class DocListener implements ChangeListener<Document> {
		@Override
		public void changed(ObservableValue<? extends Document> observable, Document oldValue, Document newValue) {
			try {
				// Use reflection to retrieve the WebEngine's private 'page' field.
				Field f = engine.getClass().getDeclaredField("page");
				f.setAccessible(true);
				com.sun.webkit.WebPage page = (com.sun.webkit.WebPage) f.get(engine);
				page.setBackgroundColor((new java.awt.Color(255, 255, 255, 1)).getRGB());

			} catch (Exception e) {
			}

		}
	}
}