
package GUI.objects;

import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.tagging.Chapter;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataWriter;
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
import javafx.scene.control.TextArea;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import utilities.Log;
import utilities.Util;
import utilities.functional.functor.UnProcedure;

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
        private Chapter c;
        // lazy initialized, we dont want to spam rarely used objects
        StackPane content;
        Text message;
        TextArea ta; // edit text area
        private PopOver p; 
        private final ScaleTransition start;
        private final ScaleTransition end;
            
        Chap(Chapter c, Duration total_time) {
            // resources
            this.c = c;
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
            start = new ScaleTransition(Duration.millis(150), this);
            start.setToX(8);
            start.setToY(1);
            end = new ScaleTransition(Duration.millis(150), this);
            end.setToX(1);
            end.setToY(1);
            // show popup when starting animation ends
            start.setOnFinished( e-> showPopup() );
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
        
        public final void showPopup() {
            if(p==null || !p.isShowing()) {
                // build popup
                message = new Text(text);
                message.setWrappingWidthNaturally();
                message.setTextAlignment(TextAlignment.CENTER);
                content = new StackPane(message);
                content.setPadding(new Insets(8));
                p = new PopOver(content);
                p.setAutoHide(true);
                p.setHideOnEscape(true);
                p.setHideOnClick(true);
//                p.setAutoFix(true);
                p.setTitle(Util.formatDuration(time));
                p.setArrowLocation(TOP_CENTER);
                p.setOnHidden( event -> {
                    if (editOn) cancelEdit();
                    end.play();
                });
                p.show(this);
                content.setOnMouseClicked(me->{
                    if(me.getClickCount()==2) startEdit();
                    me.consume();
                });
            }
        }
        
        private boolean editOn = false;
        
        /** Returns whether editing is currently active. */
        public boolean isEdited() {
            return editOn;
        }
        
        /** Starts editable mode. */
        public void startEdit() {
            editOn = true;
            ta = new TextArea();
            ta.setPrefSize(TextArea.USE_COMPUTED_SIZE, TextArea.USE_COMPUTED_SIZE);
            ta.textProperty().addListener((o,oldV,newV)->{
                if(newV!=null && !newV.isEmpty()) 
                   ta.setPrefWidth(100+newV.length()/3);
            });
            ta.prefHeightProperty().bind(ta.prefWidthProperty().multiply(0.8));
            ta.setWrapText(true);
            ta.setText(message.getText());
            ta.setOnKeyPressed(ke->{
                if(ke.getCode()==ENTER) okEdit();
                else if(ke.getCode()==ESCAPE) cancelEdit();
            });
            content.getChildren().add(ta);
        }
        
        /** Ends editable mode and applies changes. */
        public void okEdit() {
            editOn = false;
            // go back
            content.getChildren().remove(ta);
            // & persist changes
            message.setText(ta.getText());
            c.setInfo(ta.getText());

            List<Chapter> chaps = Player.getCurrentMetadata().getChapters();
            int ind = chaps.indexOf(c);
            Chapter newCh = new Chapter(c.getTime(), message.getText());

            // reflect the change physically
            if(ind==-1) chaps.add(newCh);
            else chaps.set(ind, newCh);
            MetadataWriter mw = MetadataWriter.create(Player.getCurrentMetadata());
                           mw.setChapters(chaps);
                           mw.write();
                           
             // fire successful edit finish event
            if(onEditFinish!=null) onEditFinish.accept(true);
        }
        
        /** Ends editable mode and discards all changes. */
        public void cancelEdit() {
            editOn = false;
            // go back & dont persist changes
            content.getChildren().remove(ta);
            // fire unsuccessful edit finish event
            if(onEditFinish!=null) onEditFinish.accept(false);
        }
        
        UnProcedure<Boolean> onEditFinish;
        
        /**
         * Sets behavior executed after editing finishes.
         * @param procedure to execute with a success parameter where true
         * signifies changes were applied and false indicates cancellation.
         */
        public void setOnEditFinish(UnProcedure<Boolean> onEditFinish) {
            this.onEditFinish = onEditFinish;
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
        position.setOnMouseDragged( e -> {
            if(e.getButton()==PRIMARY) {
                double distance = e.getX();
                double width = getWidth();
                // snap to chapter if nearby
                for (Chap c: chapters) {
                    if (GUI.snapDistance > Math.abs(distance-c.position*width)) {
                        position.setValue(c.position);
                        return;
                    }
                }
                position.setValue(distance / width);
            }
        });
        position.setOnMouseReleased( e -> {
            if(e.getButton()==PRIMARY) {
                double pos = position.getValue();
                Duration seekTo = totalTime.get().multiply(pos);
                PLAYBACK.seek(seekTo);
                canUpdate = true;
            }
        });
        position.setOnMousePressed( e -> canUpdate = false );
        position.addEventFilter(MOUSE_CLICKED, e -> {
            if(e.getButton()==SECONDARY) {
                Duration now = totalTime.get().multiply(position.getValue());
                Chap c = new Chap(new Chapter(now, "New Chapter"), now);
                     c.setOpacity(0.5);
                getChildren().add(c);
                     c.setLayoutX(e.getX());
                     c.showPopup();
                     c.setOnEditFinish((success)-> getChildren().remove(c));
                     c.startEdit();
            }
        });
        this.widthProperty().addListener( o -> positionChapters() );
    }
    
    // Updates positions of chapters. No reloading takes place. Use on resize.
    private void positionChapters() {
        chapters.forEach( c -> c.setLayoutX(c.position*getWidth()));
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
        for (Chapter ch: m.getChapters()) {            
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
