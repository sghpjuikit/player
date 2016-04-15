package webReader;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import util.conf.IsConfig;
import layout.widget.Widget;
import layout.widget.controller.FXMLController;

/**
 *
 */
@Widget.Info(
    name = "WebReader"
)
public class WebReader extends FXMLController {

    @FXML
    private TextField addressBar;
    @FXML
    private WebView webView;
    private WebEngine engine;

    @IsConfig(name = "Last visited address", info = "Last visited address")
    public String url = "http://duckduckgo.com/";


    @Override
    public void init() {
        engine = webView.getEngine();
        addressBar.setOnKeyPressed( e -> {
            if (e.getCode().equals(KeyCode.ENTER))
                refresh();
        });
        engine.locationProperty().addListener( o -> {
            url = engine.getLocation();
            addressBar.setText(engine.getLocation());
        });
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
    }
}