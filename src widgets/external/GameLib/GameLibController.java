/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GameLib;

import GUI.objects.Icon;
import GUI.objects.Thumbnail;
import Layout.Widgets.FXMLController;
import Layout.Widgets.gamelib.GameItem;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.FOLDER;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.GAMEPAD;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.animation.ScaleTransition;
import javafx.animation.Transition;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import static javafx.scene.control.SelectionMode.SINGLE;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_RELEASED;
import javafx.scene.layout.HBox;
import static javafx.scene.layout.Region.USE_PREF_SIZE;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import static javafx.scene.text.TextAlignment.JUSTIFY;
import javafx.util.Duration;
import util.File.Enviroment;

/**
import static javafx.scene.control.SelectionMode.SINGLE;
 FXML Controller class
 <p>
 @author Plutonium_
 */
public class GameLibController extends FXMLController {
    
    @FXML StackPane cover_root;
    @FXML ListView<GameItem> game_list;
    @FXML StackPane inforoot;
    @FXML Text info_text;
    Thumbnail cover;
    @FXML HBox controls;
    GameItem game;
    @FXML StackPane inner;
    @FXML StackPane outer;
    
    public void loadGames(File dir) {
        try {
            Files.list(dir.getAbsoluteFile().toPath())
//            Files.find(dir.getAbsoluteFile().toPath(), 0, (p,ba) -> ba.isDirectory())
                 .map(p -> {
                     GameItem g = new GameItem(p.toFile());
                     return g;
                 }).forEach(game_list.getItems()::add);
        } catch (IOException ex) {
            Logger.getLogger(GameLibController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void openGame(GameItem g) {
        if(game!=null && game.equals(g)) return;
        game = g;
        if(g==null) return;
        cover.loadImage(g.getCover());
        info_text.toFront();
        info_text.setTextAlignment(JUSTIFY);
//        info_text.getChildren().clear();
        
        File pi = new File(g.getLocation(),"play-howto.txt");
        if(pi.exists()) {
            try {
                String s = Files.lines(pi.toPath()).collect(Collectors.joining("\n"));
                info_text.setText(s);
//                info_text.getChildren().add(new Text(s));
            } catch (IOException ex) {
                Logger.getLogger(GameLibController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        
        inforoot.setMaxSize(USE_PREF_SIZE,USE_PREF_SIZE);
        inforoot.setMinSize(USE_PREF_SIZE,USE_PREF_SIZE);
//        inforoot.setPrefSize(300, 200);
        
        inforoot.setPrefSize(50, 50);
        ScaleTransition st = new ScaleTransition(Duration.millis(200),inforoot);
                        st.setFromX(0);
                        st.setFromY(0);
                        st.setToX(1);
                        st.setToY(1);
        st.setOnFinished(a -> {
            Transition t = new Transition() {
                {
                    setCycleDuration(Duration.millis(600));
                }
                @Override
                protected void interpolate(double frac) {
                    inforoot.setPrefSize(50 + (300-50)*frac,50 + (200-50)*frac);
                }
            };
            t.play();
        });
        st.play();
    }

    @Override
    public void init() {
        game_list.setCellFactory( listview -> new ListCell<GameItem>(){
            {
                setOnMouseClicked(e -> {
                    openGame(getItem());
                });
            }
            @Override
            protected void updateItem(GameItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item.getName());
            }
            
        });
        game_list.getSelectionModel().setSelectionMode(SINGLE);
        game_list.getSelectionModel().selectedItemProperty().addListener((o,ov,nv) -> openGame(nv));
        loadGames(new File("H:\\games"));
        
        
        cover = new Thumbnail();
        cover.setBackgroundVisible(false);
        cover.setBorderVisible(false);
        cover.setContextMenuOn(false);
        cover_root.getChildren().add(cover.getPane());
        cover.getPane().prefHeightProperty().bind(cover_root.heightProperty());
        cover.getPane().prefWidthProperty().bind(cover_root.widthProperty());
        
        
        inforoot.setVisible(false);
        cover_root.addEventFilter(MOUSE_RELEASED, e -> {
            if(e.getButton()==SECONDARY)
                inforoot.setVisible(!inforoot.isVisible());
        });
        
        Icon playB = new Icon(GAMEPAD, 40, null, () -> {
            if(game!=null) game.play();
        });
        Icon exploreB = new Icon(FOLDER, 40, null, () -> {
            if(game!=null) Enviroment.browse(game.getLocation().toURI());
        });
        
        info_text.setWrappingWidth(300-30-16);
        controls.setSpacing(17);
        controls.getChildren().addAll(playB,exploreB);
        

    }

    @Override
    public void refresh() {
    }

    @Override
    public void close() {
    }
    
}


