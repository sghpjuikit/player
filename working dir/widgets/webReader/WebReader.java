package webReader;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import layout.widget.Widget;
import layout.widget.controller.FXMLController;
import util.access.V;
import util.access.VarEnum;
import util.conf.IsConfig;
import util.conf.IsConfig.EditMode;
import util.dev.Dependency;
import web.DuckDuckGoQBuilder;
import web.SearchUriBuilder;
import web.WebBarInterpreter;
import static main.App.APP;
import static util.dev.Util.log;
import static util.file.UtilKt.childOf;
import static util.reactive.Util.maintain;
import static util.type.Util.getFieldValue;
import static util.type.Util.invokeMethodP1;

/**
 * Web browser component.
 */
@Widget.Info(
	name = "WebReader",
	author = "Martin Polakovic",
//	howto = "",
	description = "Very simple web browser widget.",
//	notes = "",
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

    @IsConfig(name = "Search engine")
    private final VarEnum<SearchUriBuilder> searchEngine = VarEnum.ofInstances(DuckDuckGoQBuilder.INSTANCE, SearchUriBuilder.class, APP.instances);

    @IsConfig(name = "No background")
    private final V<Boolean> noBgr = new V<>(false);

    @Override
    public void init() {
        engine = webView.getEngine();
		engine.setUserDataDirectory(APP.DIR_TEMP);
        addressBar.setOnKeyPressed(e -> {
            if (e.getCode().equals(KeyCode.ENTER)) {
            	String text = addressBar.getText();
            	String url = WebBarInterpreter.INSTANCE.toUrlString(text, searchEngine.get());
	            loadPage(url);
            }
        });
		engine.setUserDataDirectory(childOf(getUserLocation(), "browser"));
        engine.locationProperty().addListener(o -> {
            url = engine.getLocation();
            addressBar.setText(url);
        });
	    d(maintain(engine.documentProperty(), doc -> {
	    	if (noBgr.get())
	    		setTransparentBgrColor();
	    }));
	    getInputs().create("Html", String.class, this::loadHtml);
	    getInputs().create("Url", String.class, this::loadPage);

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
        loadPage(" ");
    }

	public void loadHtml(String html) {
		if (html!=null) {
			//			    String s = "<body text=white>" + text + "</body>";
			engine.loadContent(html);
			//		    engine.loadContent(s);
		}
	}

	public void loadPage(String url) {
		if (url!=null) {
			engine.load(url);
		}
	}

	@Dependency("requires access to javafx.web/com.sun.webkit.WebPage")
	private void setTransparentBgrColor() {
		// TODO: jigsaw
		try {
			// Use reflection to retrieve the WebEngine's private 'page' field.
			Object webPage = getFieldValue(engine, "page");
			if (webPage==null) return; // TODO: fix
			invokeMethodP1(webPage, "setBackgroundColor", int.class, new java.awt.Color(255, 255, 255, 1).getRGB());
		} catch (Exception e) {
			log(WebReader.class).error("Could not change background color to transparent", e);
		}
	}
}