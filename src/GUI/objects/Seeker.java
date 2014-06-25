
package GUI.objects;

import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.tagging.Chapter;
import AudioPlayer.tagging.Metadata;
import GUI.GUI;
import GUI.objects.PopOver.PopOver;
import static GUI.objects.PopOver.PopOver.ArrowLocation.TOP_CENTER;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.ScaleTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Slider;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.text.TextAlignment;
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
        
        Chap(Chapter c, Duration total_time) {
            // resources
            double total = total_time.toMillis();
            double pos = c.getTime().toMillis();
            double relPos = pos/total;
            // set properties
            position = relPos;
            time = c.getTime();
            text = c.getInfo();
            // set up skin
            this.getStyleClass().add(STYLECLASS);
            // set up layout
            double height = (Seeker.this.getLayoutBounds().getHeight()-getLayoutBounds().getHeight())/4;
            AnchorPane.setTopAnchor(this, height);
            // build animations
            final ScaleTransition start = new ScaleTransition(Duration.millis(150), this);
                                  start.setToX(8);
                                  start.setToY(1);
            final ScaleTransition end = new ScaleTransition(Duration.millis(150), this);
                                  end.setToX(1);
                                  end.setToY(1);
            // show popup when starting animation ends
            start.setOnFinished( e-> {
                if(p==null || !p.isShowing()) {
                    // build popup
                    Text message = new Text(text);
                         message.setWrappingWidthNaturally();
                         message.setTextAlignment(TextAlignment.CENTER);
                    BorderPane pane = new BorderPane(message);
                    BorderPane.setMargin(message, new Insets(8));
                    p = new PopOver(pane);
                    p.setAutoHide(true);
                    p.setHideOnEscape(true);
                    p.setHideOnClick(true);
                    p.setAutoFix(true);
                    p.setTitle(Util.formatDuration(time));
                    p.setArrowLocation(TOP_CENTER);
                    p.setOnHidden( event -> end.play());
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
    private final List<Chap> chapters = new ArrayList();
    
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
                Duration seekTo = totalTime.get().multiply(pos);
                PLAYBACK.seek(seekTo);                
            }
        });
        position.setOnMouseDragged( t -> {
            double distance = t.getX();
            double width = getWidth();
            // snap to chapter if nearby
            for (Chap c: chapters) {
                if (GUI.snapDistance > Math.abs(distance-c.position*width)) {
                    position.setValue(c.position);
                    return;
                }
            }
            position.setValue(distance / width);
        });
        position.setOnMouseReleased( e -> {
            double pos = position.getValue();
            Duration seekTo = totalTime.get().multiply(pos);
            PLAYBACK.seek(seekTo);
            canUpdate = true;
        });
        position.setOnMousePressed( e -> canUpdate = false );
        this.widthProperty().addListener( o -> positionChapters() );
    }
    
    // Updates positions of chapters. No reloading takes place. Use on resize.
    private void positionChapters() {
        chapters.forEach( c -> c.setLayoutX(c.position*this.getWidth()));
    }
    
    /**
     * Reloads chapters from currently played item's metadata. Use when chapter
     * data changes for example on chapter add/remove.
     */
    public void reloadChapters(Metadata m) {
        // clear 
        getChildren().removeAll(chapters);
        chapters.clear();
        
        // return if disabled or not available
        if (!showChapters) return;
        if(m==null || m.isEmpty()) return;
        
        // populate
        for (Chapter ch: m.getExtended().getChapters()) {            
            Chap c = new Chap(ch, m.getLength());
            getChildren().add(c);
            chapters.add(c);
        }
        positionChapters();
    }
    
    public void setChaptersShowPopUp(boolean val) {
        popupChapters = val;
    }
    public void setChaptersVisible(boolean val) {
        showChapters = val;
    }
    
    // data
    private final SimpleObjectProperty<Duration> totalTime = new SimpleObjectProperty(0);
    private final SimpleObjectProperty<Duration> currentTime = new SimpleObjectProperty(0);
    private final ChangeListener<Duration> positionUpdater = (o,oldV,newV)-> {
        if (!canUpdate) return;
        position.setValue(currentTime.get().toMillis()/totalTime.get().toMillis());
    };
    
    /**
     * Binds to total and current duration value.
     * @param totalTime length of the song
     * @param currentTime time position within the playback of the song.
     */
    public void bindTime(ObjectProperty<Duration> totalTime, ObjectProperty<Duration> currentTime) {
        // atomical binding to avoid illegal position value
        this.totalTime.removeListener(positionUpdater);
        this.currentTime.removeListener(positionUpdater);
        this.totalTime.bind(totalTime);
        this.currentTime.bind(currentTime);
        this.totalTime.addListener(positionUpdater);
        this.currentTime.addListener(positionUpdater);
        positionUpdater.changed(null,Duration.ZERO, Duration.ZERO);
    }
    public void unbindTime() {
        totalTime.unbind();
        currentTime.unbind();
        totalTime.set(Duration.ZERO);
        currentTime.set(Duration.ONE);
        positionUpdater.changed(null,Duration.ZERO, Duration.ZERO);
    }
}
