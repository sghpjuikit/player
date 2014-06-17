
package GUI.ItemHolders;

import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.tagging.Chapter;
import GUI.GUI;
import GUI.objects.PopOver.PopOver;
import static GUI.objects.PopOver.PopOver.ArrowLocation.TOP_CENTER;
import GUI.objects.Text;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.control.Slider;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import utilities.Log;
import utilities.Util;

/**
 * 
 * @author uranium
 */
public final class Seeker extends AnchorPane {
    
    public static final String STYLECLASS = "seeker";
    boolean showChapters = true;
    boolean popupChapters = true;
            
    private class Chap extends Region {
        public static final String STYLECLASS = "seeker-marker";
        // inherent properties of the chapter
        private final double position;
        private Duration time;
        private String text;
        // lazy initialized, we dont want to spam unused popups
        private PopOver p; 
        
        Chap(Chapter c) {
            // resources
            double total = Player.getCurrentMetadata().getLength().toMillis();
            double pos = c.getTime().toMillis();
            double relPos = pos/total;
            // set properties
            position = relPos;
            time = c.getTime();
            text = c.getInfo();
            // set up skin
            this.getStyleClass().add(STYLECLASS);
            // set up layout
            double height = (Seeker.this.getBoundsInParent().getHeight()-getPadding().getTop())/4;
            AnchorPane.setTopAnchor(this, height);
            // build animations
            final ScaleTransition start = new ScaleTransition(Duration.millis(150), this);
                                  start.setToX(8);
                                  start.setToY(1.5);
            final ScaleTransition end = new ScaleTransition(Duration.millis(150), this);
                                  end.setToX(1);
                                  end.setToY(1);
            // show popup when starting animation ends
            start.setOnFinished( e-> {
                if(p==null || !p.isShowing()) {
                    // build popup
                    p = new PopOver(new Text(text));
                    p.setAutoHide(false);
                    p.setHideOnEscape(true);
                    p.setHideOnClick(true);
                    p.setAutoFix(true);
                    p.setDetachedTitle(Util.formatDuration(time));
                    p.setArrowLocation(TOP_CENTER);
                    p.setOnHidden( event -> end.play());
                    // show popup
                    p.show(this);
                }
            });
            // set off starting animation
            this.setOnMouseEntered( e -> {
                if (popupChapters)start.play();
            });
            // set off ending animation if popup open
            this.setOnMouseExited( e -> {
                start.stop();
                if(p==null || !p.isShowing()) end.play();
            });
            // seek to chapter on click
            this.setOnMouseClicked( e -> PLAYBACK.seek(time) );
            this.setCursor(Cursor.HAND);
        }
    }
    
    // GUI
    @FXML Slider position;
    final List<Chap> chapters = new ArrayList();
    
    // tmp variables
    public boolean canUpdate = true;
    
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
        position.getStyleClass().add(STYLECLASS);
        
        position.valueChangingProperty().addListener((o,oldV,newV) -> {
            if (oldV && !newV) {
                double pos = position.getValue();
                Duration seekTo = PLAYBACK.getTotalTime().multiply(pos);
                PLAYBACK.seek(seekTo);                
            }
        });
        position.setOnMouseDragged( t -> {
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
        position.setOnMouseReleased( e -> {
            double pos = position.getValue();
            Duration seekTo = PLAYBACK.getTotalTime().multiply(pos);
            PLAYBACK.seek(seekTo);
            canUpdate = true;
        });
        position.setOnMousePressed( e -> canUpdate = false );
        this.widthProperty().addListener( o -> updateChapters() );
    }
    
    /**
     * Updates seeker's position to the most up to date.
     */
    public void updatePosition() {
        if (position.isValueChanging()) return;
        
        final Duration total = PLAYBACK.getTotalTime();
        final Duration currentTime = PLAYBACK.getCurrentTime();
        if (total == null || currentTime == null) {
            position.setValue(0);
        } else {
            position.setValue(currentTime.toMillis() / total.toMillis());
        }
    }
    /**
     * Updates positions of chapters. No reloading takes place. Use on resize.
     */
    public void updateChapters() {
        chapters.forEach( c -> c.setLayoutX(c.position*this.getWidth()));
    }
    /**
     * Reloads chapters from currently played item's metadata. Use when chapter
     * data changes for example on chapter add/remove.
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
