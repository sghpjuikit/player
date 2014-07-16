
package GUI.objects;

import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.tagging.Chapters.Chapter;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataWriter;
import GUI.objects.PopOver.PopOver;
import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static javafx.scene.input.MouseEvent.MOUSE_MOVED;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import utilities.FxTimer;
import utilities.Log;
import utilities.Util;
import utilities.functional.functor.UnProcedure;

/**
 * 
 * @author uranium
 */
public final class Seeker extends AnchorPane {
    
    public static final String STYLECLASS = "seeker";
    
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
                    if (snapDistance > Math.abs(distance-c.position*width)) {
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
                addChapterAt(e.getX());
            }
        });
        this.widthProperty().addListener( o -> positionChapters() );
        
        
        // new chapter button
        AddChapButton adder = new AddChapButton();
        getChildren().add(adder);
        addEventFilter(MOUSE_MOVED, e -> {
            if(adder.isVisible() && adder.getOpacity()>0)
                adder.setLayoutX(e.getX()-adder.getWidth()/2);
        });
        position.addEventFilter(MOUSE_ENTERED, e -> {
            adder.show();
            adder.setDisable(!Player.getCurrentMetadata().isFileBased());
        });
        position.addEventFilter(MOUSE_EXITED, e -> 
                FxTimer.run(Duration.millis(200), () -> {
                    if(!adder.isHover() && !position.isHover()) 
                        adder.hide();
        }));
        adder.addEventHandler(MOUSE_EXITED, e -> adder.hide());
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
    
    // properties
    boolean showChapters = true;
    boolean popupChapters = true;
    double snapDistance = 8;
    boolean editableChapters = true;
    boolean singleChapterPopupMode = false;
    
    /** 
     * Set whether chapters should display whole information in a pop up.
     * Default true.
     */
    public void setChaptersShowPopUp(boolean val) {
        popupChapters = val;
    }
    /** Set whether chapters should be displayed on the seeker. Default true */
    public void setChaptersVisible(boolean val) {
        showChapters = val;
    }
    /** 
     * Set distance for snapping seeker thumb to chapter when dragging it.
     * Default 8.
     */
    public void setChapterSnapDistance(double val) {
        snapDistance = val;
    }
    /** Set whether chapters can be edited. Default true. */
    public void setChaptersEditable(boolean val) {
        editableChapters = val;
    }
    /** 
     * Set whether only one chapter popup can be displayed at any given time.
     * Default false. 
     */
    public void setSinglePopupMode(boolean val) {
        singleChapterPopupMode = val;
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
    
    
/******************************************************************************/
    
    private class AddChapButton extends StackPane {
        Label l = AwesomeDude.createIconLabel(AwesomeIcon.PLUS_CIRCLE,"","18","18", ContentDisplay.GRAPHIC_ONLY);
        
        public AddChapButton() {
            getChildren().add(l);
            setLayoutY(-23);
            setDisable(false);
            setOnMouseClicked(e -> addChapterAt(getLayoutX()+getWidth()/2));
            setOpacity(0);
            Tooltip.install(this, new Tooltip("Create chapter."));
        }
        public void show() {
            FadeTransition ft = new FadeTransition(Duration.millis(250), this);
            if(getOpacity()==0) ft.setDelay(Duration.millis(250));
            ft.setToValue(1);
            ft.play();
        }
        public void hide() {
            FadeTransition ft = new FadeTransition(Duration.millis(250), this);
            ft.setDelay(Duration.millis(0));
            ft.setToValue(0);
            ft.play();
        }
    }
    
    private void addChapterAt(double x) {
        Duration now = totalTime.get().multiply(x/position.getWidth());
        Chap c = new Chap(new Chapter(now, ""), now);
             c.setOpacity(0.5);
        getChildren().add(c);
             c.setLayoutX(x);
             c.showPopup();
             c.setOnEditFinish((success)-> getChildren().remove(c));
             c.startEdit();
    }
    
/******************************************************************************/
    
    private final class Chap extends Region {
        public static final String STYLECLASS = "seeker-marker";
        // inherent properties of the chapter
        private final double position;
        private Chapter c;
        // lazy initialized, we dont want to spam rarely used objects
        StackPane content;
        Text message;
        TextArea ta;    // edit text area
        Label editB;    // start edit button
        Label commitB;  // apply edit button
        Label delB;     // delete chapter button
        Label cancelB;  // cancel button
        private PopOver p;      // main chapter popup
        private PopOver helpP;  // help popup
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
            // set off starting animation when mouse hovers
            this.setOnMouseEntered( e -> {
                if (popupChapters)start.play();
            });
            // set off ending animation if popup open
            this.setOnMouseExited( e -> {
                start.stop();
                if(p==null || !p.isShowing()) end.play();
            });
            // seek to chapter on click
            this.setOnMouseClicked( e -> seekTo() );
//            this.setCursor(Cursor.HAND);
        }
        
        public final void showPopup() {
            // hide other popups if only one allowed
            if(Seeker.this.singleChapterPopupMode) 
                Seeker.this.chapters.forEach(Chap::hidePopup);
            // build popup if not yet built
            if(p==null) {
                // --------   build
                // text content
                message = new Text(c.getText());
                message.setWrappingWidthNatural(true);
                message.setTextAlignment(TextAlignment.CENTER);
                content = new StackPane(message);
                content.setPadding(new Insets(8)); // 8 - default padding
                
                // buttons
                editB = AwesomeDude.createIconLabel(AwesomeIcon.PENCIL,"11");
                editB.setOnMouseClicked( e -> {
                    startEdit();
                    e.consume();
                });
                editB.setTooltip(new Tooltip("Edit chapter"));
                commitB = AwesomeDude.createIconLabel(AwesomeIcon.CHECK,"11");
                commitB.setOnMouseClicked( e -> {
                    commitEdit();
                    e.consume();
                });
                commitB.setTooltip(new Tooltip("Confirm changes"));
                delB = AwesomeDude.createIconLabel(AwesomeIcon.TRASH_ALT,"11");                     
                delB.setOnMouseClicked( e -> {
                     Metadata m = Player.getCurrentMetadata();            
                     MetadataWriter mw = MetadataWriter.create(m);
                                    mw.removeChapter(c,m);
                                    mw.write();
                     e.consume();
                });
                delB.setTooltip(new Tooltip("Remove chapter"));
                cancelB = AwesomeDude.createIconLabel(AwesomeIcon.BACKWARD,"11");                     
                cancelB.setOnMouseClicked( e -> {
                    cancelEdit();
                    e.consume();
                });
                cancelB.setTooltip(new Tooltip("Cancel edit"));
                Label helpB = AwesomeDude.createIconLabel(AwesomeIcon.INFO,"11");                     
                helpB.setOnMouseClicked( e -> {
                    // build help content for help popup if not yet built
                    // with this we avoid constructing multuple popups
                    if(helpP == null) {
                        String t = "Single click : Close\n" +
                                   "Double L click : Seek\n" +
                                   "Double R click : Start edit\n" +
                                   "Enter : Apply edit changes\n" +
                                   "Escape : Cancel edit";
                        helpP = PopOver.createHelpPopOver(t);
                    }
                    helpP.show(helpB);
                    e.consume();
                });
                helpB.setTooltip(new Tooltip("Help"));
                
                // popup
                p = new PopOver(content);
                p.setAutoHide(true);
                p.setHideOnEscape(true);
                p.setHideOnClick(false); // we will emulate it on our own
                p.setAutoFix(false);
                p.setTitle(Util.formatDuration(c.getTime()));
                p.setOnHidden( e -> {
                    if (editOn) cancelEdit();
                    end.play();
                });
                p.getHeaderIcons().addAll(editB,delB,helpB);
                content.setOnMouseClicked( e -> {
                    // if info popup displayed close it and act as if content is
                    // mouse transparent to prevent any action
                    if(helpP!=null && helpP.isShowing()) {
                        helpP.hideStrong();
                        e.consume();
                        return;
                    }
                    // otherwise handle click event
                    if(e.getClickCount()==1 && e.isStillSincePress())
                        // attempt to hide but wait to check if the click is not
                        // part of the double click - ignore if it is
                        // also prevent closing when not still since press
                        // as dragging also activates click which we need to avoid
                        delayerCloser.restart();
                    if(e.getClickCount()==2) {
                        can_hide = false;
                        if (e.getButton()==SECONDARY) startEdit();
                        else if (e.getButton()==PRIMARY) seekTo();
                    }
                    // consume to prevent real hide on click (just in case
                    // even if disabled)
                    e.consume();
                });
            }
            // show if not already
            if(!p.isShowing()) p.show(this);
        }
        
        public void hidePopup() {
            if(p!=null && p.isShowing()) p.hideStrong();
        }
        
        private boolean can_hide = true;
        private FxTimer delayerCloser = FxTimer.create(Duration.millis(200), ()->{
            if(can_hide) p.hideStrong();
            can_hide = true;
        });
        private boolean editOn = false;
        
        
        /** Returns whether editing is currently active. */
        public boolean isEdited() {
            return editOn;
        }
        
        /** Starts editable mode. */
        public void startEdit() {
            if (!editableChapters) return;
            // start edit
            editOn = true;
            ta = new TextArea();
            ta.setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
            // maintain 'sensible' width dynamically by content
            ta.textProperty().addListener((o,oldV,newV)->{
                if(newV!=null && !newV.isEmpty()) 
                   ta.setPrefWidth(100+newV.length()/3);
            });
            // maintain proportional size
            ta.prefHeightProperty().bind(ta.prefWidthProperty().multiply(0.8));
            ta.setWrapText(true);
            ta.setText(message.getText());
            ta.setOnKeyPressed(e->{
                if (e.getCode()==ENTER) commitEdit();
                else if (e.getCode()==ESCAPE) cancelEdit();
                else return;    // dont consume if nothing happens
                e.consume();
            });
            // maintain proper content
            content.getChildren().add(ta);
            p.getHeaderIcons().set(0, commitB);
            p.getHeaderIcons().add(1, cancelB);
        }
        
        /** Ends editable mode and applies changes. */
        public void commitEdit() {
            // apply new value only when changed
            if (!c.getText().equals(ta.getText())) {
                // persist changes visually
                message.setText(ta.getText());
                message.setWrappingWidthNatural(true);
                // and physically 
                c.setText(ta.getText());
                Metadata m = Player.getCurrentMetadata();            
                MetadataWriter mw = MetadataWriter.create(m);
                               mw.setChapter(c, m);
                               mw.write();
            }
            // go back visually
            content.getChildren().remove(ta);
            // maintain proper content
            p.getHeaderIcons().set(0, editB);
            p.getHeaderIcons().remove(cancelB);
            // stop edit
            editOn = false;
            // fire successful edit finish event after edit ends
            if(onEditFinish!=null) onEditFinish.accept(true);
        }
        
        /** Ends editable mode and discards all changes. */
        public void cancelEdit() {
            // go back & dont persist changes
            content.getChildren().remove(ta);
            // maintain proper content
            p.getHeaderIcons().set(0, editB);
            p.getHeaderIcons().remove(cancelB);
            // stop edit
            editOn = false;
            // fire unsuccessful edit finish event after edit ends
            if(onEditFinish!=null) onEditFinish.accept(false);
            
        }
        
        public void seekTo() {
            PLAYBACK.seek(c.getTime());
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
}
