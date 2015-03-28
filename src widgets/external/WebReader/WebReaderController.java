package WebReader;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import AudioPlayer.Player;
import Configuration.IsConfig;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Widget;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import main.App;
import util.access.AccessorEnum;
import web.DuckDuckGoImageQBuilder;
import web.HttpSearchQueryBuilder;

/**
 *
 */
@Widget.Info(
    name = "WebReader"
)
public final class WebReaderController extends FXMLController {
    
    @IsConfig(name = "Rating control", info = "The style of the graphics of the rating control.")
    private static final AccessorEnum<HttpSearchQueryBuilder> searchQB = new AccessorEnum<>(new DuckDuckGoImageQBuilder(),() -> App.plugins.getPlugins(HttpSearchQueryBuilder.class));
    
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
        String s = Player.playingtem.get().getAlbum();
        if(!s.isEmpty())
            engine.load(searchQB.getValue().apply(s));
    }
    public void loadPage(String page) {
        engine.load(page);
    }
}