
package GUI.ItemHolders;

import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.tagging.Chapter;
import GUI.GUI;
import GUI.objects.PopOver.PopOver;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.ScaleTransition;
import javafx.beans.Observable;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;
import utilities.Log;
import utilities.Util;

/**
 * 
 * @author uranium
 */
public final class Seeker extends AnchorPane{
    
    boolean showChapters = true;
    boolean popupChapters = true;
            
    private class Chap extends Rectangle {
        double position;
        Duration time;
        Chap(Chapter c) {
            // resources
            double total = Player.getCurrentMetadata().getLength().toMillis();
            double pos = c.getTime().toMillis();
            double relPos = pos/total;
            final ScaleTransition start = new ScaleTransition(Duration.millis(150), this);
                                  start.setToX(10);
                                  start.setToY(1.5);
            final ScaleTransition end = new ScaleTransition(Duration.millis(150), this);
                                  end.setToX(1);
                                  end.setToY(1);
            // set up
            position = relPos;
            time = c.getTime();
            this.setWidth(2);
            this.setHeight(5);
            this.setFill(Paint.valueOf("black"));
            this.setOpacity(0.4);
            AnchorPane.setTopAnchor(this, 5.0);
            Text text = new Text(c.getInfo());
                 text.setFill(Color.ANTIQUEWHITE);
            PopOver p = new PopOver(text);
                    p.setAutoHide(true);
                    p.setAutoFix(true);
                    p.setDetachedTitle(Util.formatDuration(time));
                    p.setArrowLocation(PopOver.ArrowLocation.TOP_CENTER);
            this.setOnMouseEntered((MouseEvent t) -> {
                if (popupChapters) {
                    start.play();
                    p.show(this);
                }
            });
            this.setOnMouseExited((MouseEvent t) -> {
                end.play();
            });
            this.setOnMouseClicked((MouseEvent t) -> {
                PLAYBACK.seek(time);
            });
            this.setCursor(Cursor.HAND);
        }
    }
    
    // GUI
    @FXML
    Slider position;
    final List<Chap> chapters = new ArrayList<>();
    
    // tmp variables
    public boolean canUpdate = true;
    private final Seeker THIS = this;
    
    public Seeker() {
        // load graphics
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Seeker.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException ex) {
            Log.err("Seeker source data coudlnt be read.");
        }
    }
    
    /**
     * Should be run automatically. Dont invoke this method in constructor - it
     * would run twice.
     */
    public void initialize() {
        this.setHeight(15);
        position.setMin(0);
        position.setMax(1);
        position.setCursor(Cursor.HAND);
        
        position.valueChangingProperty().addListener((ObservableValue<? extends Boolean> o, Boolean oldV, Boolean newV) -> {
            if (oldV && !newV) {
                double pos = position.getValue();
                Duration seekTo = PLAYBACK.getTotalTime().multiply(pos);
                PLAYBACK.seek(seekTo);                
            }
        });
        position.setOnMouseDragged((MouseEvent t) -> {
            double distance = t.getX();
            double width = getWidth();
            // snap to chapter if nearby
            for (Chap c: chapters) {
                if (GUI.snapDistance>Math.abs(distance-c.position*width)) {
                    position.setValue(c.position);
                    return;
                }
            }
            position.setValue(distance / width);
        });
        position.setOnMouseReleased((MouseEvent t) -> {
            double pos = position.getValue();
            Duration seekTo = PLAYBACK.getTotalTime().multiply(pos);
            PLAYBACK.seek(seekTo);
            canUpdate = true;
        });
        position.setOnMousePressed((MouseEvent t) -> {
            canUpdate = false;
        });
        this.widthProperty().addListener((Observable o) -> {
            updateChapters();
        });
    }
    
    /**
     * Updates seeker's position to the most up to date.
     */
    public void updatePosition() {
        if (position.isValueChanging()){ return; }
        
        final Duration total = PLAYBACK.getTotalTime();
        final Duration currentTime = PLAYBACK.getCurrentTime();
        if (total == null || currentTime == null) {
            position.setValue(0);
        } else {
            position.setValue(currentTime.toMillis() / total.toMillis());
        }
    }
    /**
     * Updates positions of chapters. No reloading takes place. Use on resize etc.
     */
    public void updateChapters() {
        chapters.forEach((c) -> {
            c.setLayoutX(c.position*this.getWidth());
        });
    }
    /**
     * Reloads chapters from currently played item's metadata.
     */
    public void reloadChapters() {
        // clear 
        getChildren().removeAll(chapters);
        chapters.clear();
        
        // return if disabled or not available
        if (!showChapters) return;
        if(Player.getCurrentMetadata()==null) return;
        
        // populate
        for (Chapter ch: Player.getCurrentMetadata().getExtended().getChapters()) {            
            Chap c = new Chap(ch);
            getChildren().add(c);
            chapters.add(c);
        }
        updateChapters();
    }
    
    public void setChaptersPopUp(boolean val) {
        popupChapters = val;
    }
    public void setChaptersVisible(boolean val) {
        showChapters = val;
    } 
}
