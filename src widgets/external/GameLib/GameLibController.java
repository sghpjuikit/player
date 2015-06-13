/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GameLib;

import static GameLib.GameLibController.InfoType.EXPLORER;
import static GameLib.GameLibController.InfoType.PLAY;
import Layout.Widgets.FXMLController;
import Layout.Widgets.gamelib.GameItem;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.FOLDER;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.GAMEPAD;
import gui.GUI;
import gui.objects.Icon;
import gui.objects.Thumbnail.Thumbnail;
import gui.objects.tree.FileTree;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.stream.Collectors.joining;
import javafx.animation.Interpolator;
import static javafx.animation.Interpolator.LINEAR;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import static javafx.scene.control.SelectionMode.SINGLE;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_RELEASED;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import static javafx.scene.layout.Region.USE_PREF_SIZE;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import static javafx.scene.text.TextAlignment.JUSTIFY;
import static javafx.util.Duration.millis;
import util.Animation.Anim;
import util.Animation.Interpolators.ElasticInterpolator;
import util.File.Environment;
import util.File.FileUtil;
import static util.File.FileUtil.readFileLines;
import util.async.executor.FxTimer;
import util.hierarchy.FileHierarchy;

/**
import static javafx.scene.control.SelectionMode.SINGLE;
 FXML Controller class
 <p>
 @author Plutonium_
 */
public class GameLibController extends FXMLController {
    
    @FXML AnchorPane root;
    @FXML StackPane cover_root;
    @FXML ListView<GameItem> game_list;
    @FXML StackPane file_tree_root;
    @FXML TreeView<File> file_tree;
    @FXML StackPane inforootroot;
    @FXML StackPane inforoot;
    @FXML Text info_text;
    Thumbnail cover;
    @FXML HBox controls;
    FileHierarchy fh = new FileHierarchy();
    
    GameItem game;
    InfoType at;
    
    @FXML StackPane inner;
    @FXML StackPane outer;
    @FXML Label titleL;
    @FXML Label infoL;
    FxTimer infoLHider = new FxTimer(7000, 1, ()->infoL.setText(null));
    
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
        titleL.setText(g.getName());
        info_text.toFront();
        info_text.setTextAlignment(JUSTIFY);
//        info_text.getChildren().clear();
        
        File f = new File(g.getLocation(),"play-howto.txt");
        String s = "";
        if(f.exists()) s = readFileLines(f).collect(joining("\n"));
        info_text.setText("Play\n\n" + s);
        
        goTo(PLAY);
        
        // animate
            // init values
        inforoot.setMaxSize(USE_PREF_SIZE,USE_PREF_SIZE);
        inforoot.setMinSize(USE_PREF_SIZE,USE_PREF_SIZE);
        inforoot.setPrefSize(50, 50);
            // play
        Interpolator i = new ElasticInterpolator();
        new ParallelTransition(
            new SequentialTransition(
                new Anim(millis(450), i, at -> inforoot.setPrefSize(300*at,50)),
                new Anim(millis(550), i, at -> inforoot.setPrefSize(300,50+(inforootroot.getHeight()-50)*at))
            ),
            new Anim(millis(1000),LINEAR, at -> {
                int length = g.getName().length();
                int n = (int) Math.floor(length * at);
                titleL.setText(g.getName().substring(0, n));
            })
        ).play();
    }

    @Override
    public void init() {
        loadSkin("skin.css",root);
        
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
        fh.setItem(new File("P:\\"));
        
        cover = new Thumbnail();
        cover.setBackgroundVisible(false);
        cover.setBorderVisible(false);
        cover.setContextMenuOn(false);
        cover_root.getChildren().add(cover.getPane());
//        cover.getPane().prefHeightProperty().bind(cover_root.heightProperty());
//        cover.getPane().prefWidthProperty().bind(cover_root.widthProperty());
        
        
        inforoot.setVisible(false);
        cover_root.addEventFilter(MOUSE_RELEASED, e -> {
            if(e.getButton()==SECONDARY)
                inforoot.setVisible(!inforoot.isVisible());
        });
        
        Icon playB = new Icon(GAMEPAD, 40, null, () -> {
            if(game!=null) {
                if(at!=PLAY) goTo(PLAY);
                else {
                    String s = game.play();
                    infoL.setText(s);
                    infoLHider.restart();
                }
            }
        });
        Icon exploreB = new Icon(FOLDER, 40, null, () -> {
            if(game!=null) {
                if(at!=EXPLORER) goTo(EXPLORER);
                else Environment.browse(game.getLocation().toURI());
            }
        });
        
        info_text.setWrappingWidth(300-30-16);
        controls.setSpacing(17);
        controls.getChildren().addAll(playB,exploreB);
        
        
        file_tree.setFixedCellSize(GUI.font.getValue().getSize() + 5);
        file_tree.getSelectionModel().setSelectionMode(SINGLE);
        FileTree.from(file_tree);
        file_tree.getSelectionModel().selectedItemProperty().addListener((o,ov,nv) -> {
            if(nv.getValue()!=null && nv.getValue().isDirectory()) {
                File f = new File(nv.getValue(),"readme.txt");
                if(f.exists()) {
                    String s = readFileLines(f).collect(joining("\n"));
                    info_text.setText(FileUtil.getName(f) + "\n\n" + s);
                }
            }
        });
        // create invisible root
        file_tree.setShowRoot(false);
//        file_tree.setRoot(new TreeItem<>(new File("")));
        
        file_tree_root.getChildren().setAll(fh.getPane());
        fh.getPane().prefWidthProperty().bind(file_tree_root.widthProperty());
        fh.getPane().prefHeightProperty().bind(file_tree_root.heightProperty());
    }

    @Override
    public void refresh() {
    }

    @Override
    public void close() {
    }
    
    public void goTo(InfoType to) {
        if(game==null || at==to) return;
        
        switch(to) {
            case EXPLORER:  cover_root.setVisible(false);
                            file_tree_root.setVisible(true);
                            file_tree.setRoot(FileTree.createTreeItem(game.getLocation()));
                            break;
            case PLAY:      cover_root.setVisible(true);
                            file_tree_root.setVisible(false);
                            break;
            default: throw new AssertionError("Illegal switch case: " + to);
        }
        at = to;
    }
    
    public static enum InfoType {
        PLAY,
        EXPLORER;
    }
    
}