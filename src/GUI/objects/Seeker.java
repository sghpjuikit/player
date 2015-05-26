
package GUI.objects;

import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.tagging.Chapters.Chapter;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataWriter;
import GUI.objects.PopOver.PopOver;
import static GUI.objects.PopOver.PopOver.ArrowLocation.TOP_CENTER;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.*;
import static java.time.Duration.ofMillis;
import java.util.ArrayList;
import java.util.List;
import static java.util.Objects.requireNonNull;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
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
import static javafx.util.Duration.*;
import org.reactfx.EventStreams;
import static org.reactfx.EventStreams.eventsOf;
import static util.async.Async.run;
import util.async.executor.FxTimer;
import util.dev.TODO;
import static util.dev.TODO.Purpose.BUG;
import util.graphics.fxml.ConventionFxmlLoader;

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
    
    @FXML Slider seeker;
    private final List<Chap> chapters = new ArrayList();
    private boolean canUpdate = true;
    private boolean snaPosToChap = false;
    
    public Seeker() {
        
        // load fxml part
        new ConventionFxmlLoader(this).loadNoEx();
        
        this.setMinSize(0,0);
        this.setMaxSize(USE_COMPUTED_SIZE,15);
        this.setPrefSize(USE_COMPUTED_SIZE,USE_COMPUTED_SIZE);
        
        seeker.setMin(0);
        seeker.setMax(1);
        seeker.setCursor(Cursor.HAND);
        seeker.getStyleClass().add(STYLECLASS);
        
        
        seeker.setOnMouseDragged(e -> {
            if(e.getButton()==PRIMARY) {
                double distance = e.getX();
                double width = getWidth();
                
                // snap to chapter if nearby
                if(snaPosToChap)
                    for (Chap c: chapters)
                        if (chapterSnapDistance.get() > Math.abs(distance-c.position*width)) {
                            seeker.setValue(c.position);
                            return;
                        }
                seeker.setValue(distance / width);
            }
        });
        seeker.setOnMousePressed( e -> canUpdate = false );
        seeker.setOnMouseReleased(e -> {
            if(e.getButton()==PRIMARY) {
                PLAYBACK.seek(seeker.getValue());
                run(100, () -> canUpdate = true);
            }
        });
        
        this.widthProperty().addListener( o -> chapters.forEach(Chap::position) );
        
        
        // new chapter button
        AddChapButton addB = new AddChapButton();
                      addB.i.setOpacity(0);
        addEventFilter(MOUSE_MOVED, e -> {
            if(addB.isVisible() && addB.i.getOpacity()>0) {
                addB.p.setX(e.getScreenX()-addB.i.getWidth()/2);
                addB.p.setY(seeker.localToScreen(0,0).getY()+18);
            }
        });
        seeker.addEventFilter(MOUSE_ENTERED, e -> {
            addB.show();
            addB.p.setX(e.getScreenX()-addB.i.getWidth()/2);
            addB.p.setY(seeker.localToScreen(0,0).getY()+18);
            addB.i.setDisable(!Player.playingtem.get().isFileBased());
        });
        
        EventStreams.merge(
            eventsOf(seeker, MOUSE_EXITED).reduceSuccessions((a,b)->b, ofMillis(350)),
            eventsOf(addB.root, MOUSE_EXITED).reduceSuccessions((a,b)->b, ofMillis(200))
        ).filter(p -> !addB.root.isHover() && !seeker.isHover())
         .subscribe(e -> addB.hide());
    }
    
    /**
     * Reloads chapters from currently played item's metadata. Use when chapter
     * data changes for example on chapter add/remove.
     *
     * @param m metadata
     * @throws NullPointerException if parameter null
     */
    public void reloadChapters(Metadata m) {
        requireNonNull(m);
        // clear 
        getChildren().removeAll(chapters);
        chapters.clear();
        
        if (!showChapters) return;
        
        // populate
        for (Chapter ch: m.getChapters()) {     
            Chap c = new Chap(ch, ch.getTime().toMillis()/m.getLength().toMillis());
            getChildren().add(c);
            chapters.add(c);
        }
        chapters.forEach(Chap::position);
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
    @TODO(purpose = BUG, note = "patched, but the (unknown) cause may have sideffects"
        + "invoking 'play next item' in rapid succession will cause total time value be null"
        + " test this with a shortcut")
    private final ChangeListener<Duration> positionUpdater = (o,ov,nv) -> {
        if (!canUpdate) return;
        if(totalTime.get()==null) return; // bugfix
        double newPos = currentTime.get().toMillis()/totalTime.get().toMillis();
        // turn on if you want discrete§ mode 
//        double unit = 1;
//        double oldPos = seeker.getValue();
//        double dist = seeker.getWidth() * Math.abs(newPos-oldPos);
//        if(dist > unit)
            seeker.setValue(newPos);
    };
    
    /**
     * Binds to total and current duration value.
     * @param totalTime length of the song
     * @param currentTime time seeker within the playback of the song.
     */
    public void bindTime(ObjectProperty<Duration> totalTime, ObjectProperty<Duration> currentTime) {
        // atomical binding to avoid illegal seeker value
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
        totalTime.set(ZERO);
        currentTime.set(ONE);
        positionUpdater.changed(null,ZERO, ZERO);
    }
    
    
/******************************************************************************/
    
    private class AddChapButton {
        Icon i = new Icon(PLUS_CIRCLE, 18);
        StackPane root = new StackPane();
        FadeTransition ft = new FadeTransition(millis(250), i);
        Popup p = new Popup();
        
        public AddChapButton() {
            root.setPrefSize(25, 25);
            p.getContent().add(new StackPane(i,root));
            i.getStyleClass().add("seeker-add-chapter-button");
            i.getGraphic().getStyleClass().add("seeker-add-chapter-button");
            i.setDisable(false);
            i.setOnMouseClicked(e -> {
                double deltaX = p.getX()+p.getWidth()/2-seeker.localToScreen(0,0).getX();
                addChapterAt(deltaX/seeker.getWidth());
            });
            i.setOpacity(0);
            Tooltip.install(root, new Tooltip("Create chapter."));
        }
        public void show() {
            ft.setOnFinished(null);
            ft.stop();
            p.show(seeker.getScene().getWindow());
            FadeTransition ft = new FadeTransition(millis(250), i);
            if(i.getOpacity()==0) ft.setDelay(millis(300));
            ft.setToValue(1);
            ft.setOnFinished(null);
            ft.play();
        }
        public void hide() {
            ft.setOnFinished(null);
            ft.stop();
            ft.setDelay(ZERO);
            ft.setToValue(0);
            ft.setOnFinished(a -> p.hide());
            ft.play();
        }
        
        boolean isVisible() {
            return p!=null && p.isShowing();
        }
    }
    
    
    private boolean editOnOpen = false;
    private void addChapterAt(double x) {
        Duration now = totalTime.get().multiply(x);
        Chap c = new Chap(new Chapter(now, ""), seeker.getValue());
             c.setOpacity(0.5);
        getChildren().add(c);
             c.setLayoutX(x*seeker.getWidth());
             c.showPopup();
        editOnOpen = true;
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
            double height = //Seeker.this.seeker.getLayoutY() + 
                    (Seeker.this.seeker.getBoundsInParent().getHeight() - getPadding().getTop())/2;
//            this.layoutYProperty().bind(Seeker.this.seeker.heightProperty().subtract(heightProperty()).divide(2));
//            double height = (Seeker.this.getLayoutBounds().getHeight()-getLayoutBounds().getHeight())/4;
            AnchorPane.setTopAnchor(this, 3d);
            // build animations
            start = new ScaleTransition(millis(150), this);
            start.setToX(8);
            start.setToY(1);
            end = new ScaleTransition(millis(150), this);
            end.setToX(1);
            end.setToY(1);
            // show popup when starting animation ends
            start.setOnFinished(e -> showPopup() );
            // set off starting animation when mouse hovers
            this.setOnMouseEntered(e -> showPopup());
            // set off ending animation if popup open
            this.setOnMouseExited(e -> {
                start.stop();
                if(p==null || !p.isShowing()) end.play();
            });
            // seek to chapter on click
            this.setOnMouseClicked(e -> seekTo());
        }
        
        public void showPopup() {
            start.setOnFinished(popupChapters ? e -> showPopupReal() : null);
            start.play();
        }
        
        public void showPopupReal() {
            // hide other popups if only one allowed
            if(singleChapterPopupMode) 
                chapters.stream().filter(f->f!=this).forEach(Chap::hidePopup);
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
                delB = new Icon(TRASH_ALT, 11, "Remove chapter", () -> {
                     Metadata m = Player.playingtem.get();
                     MetadataWriter.use(m, w->w.removeChapter(c,m));
                });
                cancelB = new Icon(REPLY, 11, "Cancel edit", this::cancelEdit);
                Icon nextB = new Icon(CHEVRON_RIGHT, 11, "Next chapter", () -> {
                    int i = Seeker.this.chapters.indexOf(this) + 1;
                    if(chapters.size()>i){
                        hidePopup();
                        chapters.get(i).showPopup();
                    }
                });
                Icon prevB = new Icon(CHEVRON_LEFT, 11, "Previous chapter", () -> {
                    int i = chapters.indexOf(this) - 1;
                    if(0<=i){
                        hidePopup();
                        chapters.get(i).showPopup();
                    }
                });
                int i = chapters.indexOf(this);
                if(chapters.size()-1 == i)
                    nextB.setDisable(true);
                if(0 == i)
                    prevB.setDisable(true);
                Icon helpB = new Icon(INFO, 11, "Help", e -> {
                    // build help content for help popup if not yet built
                    // with this we avoid constructing multuple popups
                    if(helpP == null) {
                        String t = "Single click : Close\n"
                                 + "Double L click : Seek\n"
                                 + "Double R click : Start edit\n"
                                 + "Enter : Apply edit changes\n"
                                 + "Escape : Cancel edit";
                        helpP = PopOver.createHelpPopOver(t);
                    }
                    helpP.show((Node)e.getSource());
                    e.consume();
                });
                // popup
                p = new PopOver(content);
                p.getSkinn().setContentPadding(new Insets(8));
                p.setArrowLocation(TOP_CENTER);
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
            if(editOnOpen) startEdit();
            editOnOpen = false;
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
            p.getHeaderIcons().remove(editB);                                   // testing bug
                // do not add remove buton if the chapter is being created
            if (!Player.playingtem.get().containsChapterAt(c.getTime()))
                p.getHeaderIcons().remove(delB);
        }
        
        /** Ends editable mode and applies changes. */
        public void commitEdit() {
            // apply new value only when changed
            String text = ta.getText();
            if (!c.getText().equals(text)) {
                // persist changes visually
                message.setText(text);
                message.setWrappingWidthNatural(true);
                // and physically 
                c.setText(text);
                Metadata m = Player.playingtem.get();
                MetadataWriter.use(m, w->w.addChapter(c,m));
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
            Seeker.this.getChildren().remove(this);
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
            Seeker.this.getChildren().remove(this);
            
        }
        
        public void seekTo() {
            PLAYBACK.seek(c.getTime());
        }
        
        public void position() {
            setLayoutX(position*Seeker.this.getWidth());
        }
    }
}