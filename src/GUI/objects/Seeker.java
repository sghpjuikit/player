
package GUI.objects;

import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.tagging.Chapters.Chapter;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataWriter;
import GUI.objects.PopOver.PopOver;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static java.util.Objects.requireNonNull;
import java.util.function.Consumer;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextAlignment;
import javafx.stage.Popup;
import javafx.util.Duration;
import static util.async.Async.run;
import util.async.FxTimer;
import util.dev.Dependency;
import util.dev.Log;

/**
 * 
 * If the seeker turns out to be problematic to resize (particularly height)
 * in layouts such as BorderPane, it is recommended to set different min, max
 * and prefSize.
 * 
 * @author uranium
 */
public final class Seeker extends AnchorPane {
    
    public static final String STYLECLASS = "seeker";
    
    // GUI
    @FXML Slider position;
    private final List<Chap> chapters = new ArrayList();
    
    private boolean canUpdate = true;
    private boolean snaPosToChap = false;
    
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
        initializeC();
    }
    
    @Dependency("Public because of FXMLLoader. Runs automatically. Dont use.")
    public void initializeC() {
        this.setMinSize(0,0);
        this.setMaxSize(USE_COMPUTED_SIZE,15);
        this.setPrefSize(USE_COMPUTED_SIZE,USE_COMPUTED_SIZE);
        
        position.setMin(0);
        position.setMax(1);
        position.setCursor(Cursor.HAND);
        position.getStyleClass().add(STYLECLASS);
        
        
        position.setOnMouseDragged( e -> {
            if(e.getButton()==PRIMARY) {
                double distance = e.getX();
                double width = getWidth();
                
                // snap to chapter if nearby
                if(snaPosToChap)
                    for (Chap c: chapters)
                        if (chapterSnapDistance.get() > Math.abs(distance-c.position*width)) {
                            position.setValue(c.position);
                            return;
                        }
                position.setValue(distance / width);
            }
        });
        position.setOnMousePressed( e -> canUpdate = false );
        position.setOnMouseReleased( e -> {
            if(e.getButton()==PRIMARY) {
                PLAYBACK.seek(position.getValue());
                run(100, () -> canUpdate = true);
            }
        });
        
        this.widthProperty().addListener( o -> positionChapters() );
        
        
        // new chapter button
        AddChapButton addB = new AddChapButton(position);
                      addB.l.setOpacity(0);
        addEventFilter(MOUSE_MOVED, e -> {
            if(addB.isVisible() && addB.l.getOpacity()>0) {
                addB.p.setX(e.getScreenX()-addB.l.getWidth()/2);
                addB.p.setY(position.localToScreen(0,0).getY()+13);
            }
        });
        position.addEventFilter(MOUSE_ENTERED, e -> {
            addB.show();
            addB.p.setX(e.getScreenX()-addB.l.getWidth()/2);
            addB.p.setY(position.localToScreen(0,0).getY()+13);
            addB.l.setDisable(!Player.playingtem.get().isFileBased());
        });
        position.addEventFilter(MOUSE_EXITED, e -> 
            run(300, () -> {
                if(!addB.l.isHover() && !position.isHover()) 
                    addB.hide();
        }));
        addB.l.addEventHandler(MOUSE_EXITED, e ->
            run(150, () -> {
                 if(!addB.l.isHover() && !position.isHover()) 
                    addB.hide();
        }));
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
        if (!showChapters || m==null || m.isEmpty()) return;
        
        // populate
        for (Chapter ch: m.getChaptersFromAny()) {     
            Chap c = new Chap(ch, ch.getTime().toMillis()/m.getLength().toMillis());
            getChildren().add(c);
            chapters.add(c);
        }
        positionChapters();
    }
    
    // properties
    boolean showChapters = true;
    boolean popupChapters = true;
    public final DoubleProperty chapterSnapDistance = new SimpleDoubleProperty(8);
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
    
    /** Set whether chapters can be edited. Default true. */
    public void setChaptersEditable(boolean val) {
        editableChapters = val;
    }
    
    /** Set snapping to chapters during seeker dragging. */
    public void setSnapToChapters(boolean v) {
        snaPosToChap = v;
    }
    
    public boolean isSnapToChapters() {
        return snaPosToChap;
    }
    
    /** 
     * Set whether only one chapter popup can be displayed at any given time.
     * Default false. 
     */
    public void setSinglePopupMode(boolean val) {
        singleChapterPopupMode = val;
    }
    
    // data
    private final SimpleObjectProperty<Duration> totalTime = new SimpleObjectProperty();
    private final SimpleObjectProperty<Duration> currentTime = new SimpleObjectProperty();
    private final ChangeListener<Duration> positionUpdater = (o,ov,nv) -> {
        if (!canUpdate) return;
        requireNonNull(currentTime);
        requireNonNull(currentTime.get());
        requireNonNull(totalTime);
        requireNonNull(totalTime.get());
        double newPos = currentTime.get().toMillis()/totalTime.get().toMillis();
        // turn on if you want discrete§ mode 
//        double unit = 1;
//        double oldPos = position.getValue();
//        double dist = position.getWidth() * Math.abs(newPos-oldPos);
//        if(dist > unit)
            position.setValue(newPos);
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
    
    private class AddChapButton {
        Popup p;
        FadeButton l = new FadeButton(PLUS_CIRCLE, 18);
        Slider s;
        FadeTransition ft = new FadeTransition(Duration.millis(250), l);
        
        public AddChapButton(Slider s) {
            p = new Popup();
            p.getContent().add(l);
            this.s = s;
            
            l.setDisable(false);
            l.setOnMouseClicked(e -> {
                double deltaX = p.getX()-Seeker.this.position.localToScreen(0,0).getX()+p.getWidth();
                addChapterAt(deltaX/s.getWidth());
            });
            l.setOpacity(0);
            Tooltip.install(l, new Tooltip("Create chapter."));
        }
        public void show() {
            ft.setOnFinished(null);
            ft.stop();
            p.show(s.getScene().getWindow());
            FadeTransition ft = new FadeTransition(Duration.millis(250), l);
            if(l.getOpacity()==0) ft.setDelay(Duration.millis(300));
            ft.setToValue(1);
            ft.setOnFinished(null);
            ft.play();
        }
        public void hide() {
            ft.setOnFinished(null);
            ft.stop();
            ft.setDelay(Duration.ZERO);
            ft.setToValue(0);
            ft.setOnFinished(a -> p.hide());
            ft.play();
        }
        
        boolean isVisible() {
            return p!=null && p.isShowing();
        }
    }
    
    
    
    private void addChapterAt(double x) {
        Duration now = totalTime.get().multiply(x);
        Chap c = new Chap(new Chapter(now, ""), position.getValue());
             c.setOpacity(0.5);
        getChildren().add(c);
             c.setLayoutX(x*position.getWidth());
             c.showPopup();
             c.setOnEditFinish( success -> getChildren().remove(c));
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
        Icon editB;    // start edit button
        Icon commitB;  // apply edit button
        Icon delB;     // delete chapter button
        Icon cancelB;  // cancel button
        private PopOver p;      // main chapter popup
        private PopOver helpP;  // help popup
        private final ScaleTransition start;
        private final ScaleTransition end;
            
        Chap(Chapter c, double pos) {
            // resources
            this.c = c;
            position = pos;
            // set up skin
            this.getStyleClass().add(STYLECLASS);
            // set up layout
            double height = //Seeker.this.position.getLayoutY() + 
                    (Seeker.this.position.getBoundsInParent().getHeight() - getPadding().getTop())/2;
//            this.layoutYProperty().bind(Seeker.this.position.heightProperty().subtract(heightProperty()).divide(2));
//            double height = (Seeker.this.getLayoutBounds().getHeight()-getLayoutBounds().getHeight())/4;
            AnchorPane.setTopAnchor(this, 3d);
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
                content.setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
                content.autosize();
                // buttons
                editB = new Icon(PENCIL, 11, "Edit chapter", this::startEdit);
                commitB = new Icon(CHECK, 11, "Confirm changes", this::commitEdit);
                delB = new Icon(TRASH_ALT, 11, "Remove chapter", e -> {
                     Metadata m = Player.playingtem.get();
//                     // avoid removing chapter that does not exist
//                     if (!m.containsChapterAt((long) c.getTime().toMillis()))
//                         return;
                     MetadataWriter.use(m, w->w.removeChapter(c,m));
                     e.consume();
                });
                cancelB = new Icon(REPLY, 11, "Cancel edit", this::cancelEdit);
                Icon nextB = new Icon(CHEVRON_RIGHT, 11, "Next chapter", () -> {
                    int i = Seeker.this.chapters.indexOf(this) + 1;
                    if(Seeker.this.chapters.size()>i){
                        hidePopup();
                        Seeker.this.chapters.get(i).showPopup();
                    }
                });
                Icon prevB = new Icon(CHEVRON_LEFT, 11, "Previous chapter", () -> {
                    int i = Seeker.this.chapters.indexOf(this) - 1;
                    if(0<=i){
                        hidePopup();
                        Seeker.this.chapters.get(i).showPopup();
                    }
                });
                int i = Seeker.this.chapters.indexOf(this);
                if(Seeker.this.chapters.size()-1 == i)
                    nextB.setDisable(true);
                if(0 == i)
                    prevB.setDisable(true);
                Label helpB = new Icon(INFO, 11, "Help", e -> {
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
                    helpP.show((Node)e.getSource());
                    e.consume();
                });
                // popup
                p = new PopOver(content);
                p.getSkinn().setContentPadding(new Insets(8));
                p.setAutoHide(true);
                p.setHideOnEscape(true);
                p.setHideOnClick(false); // we will emulate it on our own
                p.setAutoFix(false);
                p.title.set(c.getTime().toString());
                p.setOnHidden( e -> {
                    if (editOn) cancelEdit();
                    end.play();
                });
                p.getHeaderIcons().addAll(prevB, nextB, editB, delB, helpB);
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
        private FxTimer delayerCloser = new FxTimer(200, 1, ()->{
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
            // create text area
            ta = new TextArea();
            ta.setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
                // maintain 'sensible' width dynamically by content
            ChangeListener<String> resizer = (o,ov,nv) -> {
                int len = nv==null ? 0 : nv.length();
                ta.setPrefWidth(100+len/3);
                ta.setPrefHeight(ta.getPrefWidth() * 0.8);
            };
            ta.textProperty().addListener(resizer);
                // initialize size, resizer wont fire if the setText() below
                // does not change text value! fire manually here
            resizer.changed(null,null,"");
                // set more properties
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
                // do not add remove buton if the chapter is being created
            if (!Player.playingtem.get().containsChapterAt(c.getTime()))
                p.getHeaderIcons().remove(delB);
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
                Metadata m = Player.playingtem.get();
                MetadataWriter.use(m, w->w.setChapter(c,m));
            }
            // go back visually
            content.getChildren().remove(ta);
            // maintain proper content
            p.getHeaderIcons().set(0, editB);
            p.getHeaderIcons().remove(cancelB);
                // add remove button back if it wasnt added yet
            p.getHeaderIcons().add(p.getHeaderIcons().size()-2,delB);
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
        
        Consumer<Boolean> onEditFinish;
        
        /**
         * Sets behavior executed after editing finishes.
         * @param procedure to execute with a success parameter where true
         * signifies changes were applied and false indicates cancellation.
         */
        public void setOnEditFinish(Consumer<Boolean> onEditFinish) {
            this.onEditFinish = onEditFinish;
        }
    }
}