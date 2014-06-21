package WebReader;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import Configuration.IsConfig;
import Layout.Widgets.FXMLController;
import javafx.beans.Observable;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 *
 */
public final class WebReaderController extends FXMLController {
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
        addressBar.setOnKeyPressed((KeyEvent event) -> {
            if (event.getCode().equals(KeyCode.ENTER))
                refresh();
        });
        engine.locationProperty().addListener((Observable observable) -> {
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