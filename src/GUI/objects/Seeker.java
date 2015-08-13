
package gui.objects;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import org.reactfx.EventSource;

import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.tagging.Chapters.Chapter;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataWriter;
import gui.objects.PopOver.PopOver;
import gui.objects.icon.Icon;
import util.animation.Anim;
import util.animation.Loop;
import util.animation.interpolator.CircularInterpolator;
import util.async.executor.FxTimer;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;
import static gui.objects.PopOver.PopOver.ArrowLocation.TOP_CENTER;
import static java.lang.Double.max;
import static java.lang.Math.abs;
import static java.lang.Math.signum;
import static java.time.Duration.ofMillis;
import static java.util.Objects.requireNonNull;
import static javafx.beans.binding.Bindings.notEqual;
import static javafx.css.PseudoClass.getPseudoClass;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.*;
import static javafx.util.Duration.*;
import static org.reactfx.EventStreams.valuesOf;
import static util.Util.clip;
import static util.animation.Anim.mapTo01;
import static util.async.Async.run;
import static util.functional.Util.minBy;

/**
 * Playback seeker. A slider-like control that controls playback, by seeking.
 * Also manages (displays, edits, etc.) song chapters ({@link Metadata#getChapters()}). 
 * <p>
 * This control overrides {@link #layoutChildren()} and some layout properties may not work. For 
 * example padding. Use padding on the parent of this control, rather than this control directly.
 * 
 * @author uranium
 */
public final class Seeker extends AnchorPane {

    private static final String STYLECLASS = "seeker";
    private static final String STYLECLASS_CHAP = "seeker-marker";
    private static final String STYLECLASS_CHAP_ADD_BUTTON = "seeker-add-chapter-button";
    private static final PseudoClass STYLE_CHAP_NEW = getPseudoClass("newly-created");
    
    private final Slider seeker = new Slider(0,1,0);
    private final AddChapButton addB = new AddChapButton();
    private final List<Chap> chapters = new ArrayList<>();
    private final DoubleProperty seekerScaleY = seeker.scaleYProperty();
    private boolean user_drag = false;
    private boolean snaPosToChap = false;
    private Chap selectedChap = null;
    
    public Seeker() {
        
        seeker.getStyleClass().add(STYLECLASS);
        getChildren().add(seeker);
        AnchorPane.setLeftAnchor(seeker, 0d);
        AnchorPane.setRightAnchor(seeker, 0d);
        
        // mouse drag
        seeker.addEventFilter(MOUSE_PRESSED, e -> {
            if(e.getButton()==PRIMARY && !addB.isVisible() && !addB.isSelected()) {
                user_drag = true;
            }
            e.consume();
        });
        seeker.addEventFilter(DRAG_DETECTED, e -> {
            if(addB.isSelected()) addB.unselect();
            if(addB.isVisible()) addB.hide();
            user_drag = true;
        });
        seeker.addEventFilter(MOUSE_DRAGGED, e -> {
            if(e.getButton()==PRIMARY && user_drag) {
                double x = e.getX();
                double w = getWidth();
                double v = x/w;
                
                // snap to chapter
                if(snaPosToChap)
                    for (Chap c: chapters)
                        if (abs(x-c.position*w) < chapSnapDist.get()) {
                            v = c.position;
                        }
                
                seeker.setValue(v);
            }
            e.consume();
        });
        seeker.setOnMouseReleased(e -> {
            if(user_drag && e.getButton()==PRIMARY && !addB.isVisible() && !addB.isSelected()) {
                double p = e.getX()/getWidth();
                PLAYBACK.seek(p);
                run(100, () -> user_drag = false);
            }
        });
        
        // new chapter button
        addB.root.toFront();
        addEventFilter(MOUSE_MOVED, e -> {
            if(addB.isSelected()) {
                double dist = abs(e.getX()-selectedChap.getCenterX());
                minBy(chapters, chapSnapDist.get(), c -> abs(c.getCenterX()-e.getX()))
                   .map(c -> c==selectedChap ? c : null)
                   .ifPresentOrElse(addB::select, () -> {
                        if(dist > chapSnapDist.get())
                            addB.unselect();
                   });
            } else {
                minBy(chapters, chapSnapDist.get(), c -> abs(c.getCenterX()-e.getX()))
                   .ifPresentOrElse(addB::select, () -> addB.setCenterX(e.getX()));
            }
        });
        seeker.addEventFilter(MOUSE_ENTERED, e -> {
            if(!user_drag) {
                addB.show();
                if(!addB.isSelected())
                    addB.setCenterX(e.getX());
            }
        });
        valuesOf(seeker.hoverProperty())
                .filter(v->!v).reduceSuccessions((a,b)->b, ofMillis(350))
                .subscribe(e -> addB.hide());
        
        // animation 1
        ma_init();
        // animation 2
        Anim sa = new Anim(millis(1000),p -> {
                double p1 = mapTo01(p,0,0.5);
                double p2 = mapTo01(p,  0.8,1);
                double p3 = mapTo01(p,  0.3,0.6);
                
                r1.setOpacity(p1);
                r2.setOpacity(p1);
                
                double scale = 1 + 0.8*(1-abs(2*(p3-0.5)));
                r1.setScaleX(scale);
                r1.setScaleY(scale);
                r2.setScaleX(scale);
                r2.setScaleY(scale);
                seeker.setScaleY(1+3*p2);
            })
           .intpl(new CircularInterpolator()).delay(150);
        onHoverChanged(v -> sa.playFromDir(v));
    }
    
    // we override this to conveniently layout chapters
    // note that some nodes are unmanaged to fix pane resizing issues, so we handle them too
    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        double w = getWidth();
        double h = getHeight();
        
        for(Node n : getChildren()) {
            if(n instanceof Chap) {
                Chap c = (Chap) n;
                c.relocate(w * c.position, h/2 - c.getHeight()/2);
            }
        }
        
        seeker.relocate(0, h/2 - seeker.getHeight()/2);
        addB.root.relocate(addB.root.getLayoutX(),h/2-addB.root.getHeight()/2);
        r1.relocate(r1.getX(),5);
        r2.relocate(r2.getX(),h-r2.getLayoutBounds().getHeight()-5);
    }
    
    private void onHoverChanged(Consumer<? super Boolean> handler) {
        EventSource<Boolean> h = new EventSource<>();
        hoverProperty().addListener((o,ov,nv) -> h.push(nv || addB.root.isHover()));
        addB.root.hoverProperty().addListener((o,ov,nv) -> h.push(nv || isHover()));
        h.successionEnds(ofMillis(50)).subscribe(handler);
    }
    
//****************************** moving animation *****************************/
    
    private static final double MA_ISIZE = 10;
    private static final double MA_WIDTH2 = 2.5;    // hardcoded, as layoutBounds().getWidth() !work
    private final Loop ma = new Loop(this::ma_do);
    private final Icon r1 = new Icon(ANGLE_DOWN, MA_ISIZE);
    private final Icon r2 = new Icon(ANGLE_UP, MA_ISIZE);
    double matox = 0;
    double macurx = 0;
    double maspeed = 0;
    double madir = 1;
    
    private void ma_do() {
        macurx += madir * maspeed;
        double x = macurx;
               //x = clip(0,getWidth(),1);   // fixes outside of area bugs
        r1.setX(x - MA_WIDTH2);
        r2.setX(x - MA_WIDTH2);
        // we can also move add chapter button here (for different behavior), i did not
        // addB.root.setLayoutX(macurx-addB.root.getWidth()/2);
        double diff = matox-macurx;
        madir = signum(diff);
        double dist = abs(diff);
        maspeed = max(1,dist/10d);
        if(abs(macurx-matox)<1) maspeed = 0;
    }
    
    private void ma_init() {
        r1.setOpacity(0);
        r2.setOpacity(0);
        r1.setMouseTransparent(true);
        r2.setMouseTransparent(true);
        r1.setManaged(false);   // fixes a resizing issue
        r2.setManaged(false);
        getChildren().addAll(r1,r2);
        
        addEventFilter(MOUSE_MOVED, e -> matox = e.getX());
        ma.start(); // starts animation
    }

//********************************** chapters *********************************/
    
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
    }
    
    // properties
    boolean showChapters = true;
    boolean popupChapters = true;
    public final DoubleProperty chapSnapDist = new SimpleDoubleProperty(7);
    boolean editableChapters = true;
    boolean singleChapterPopupMode = false;
    
    /** 
     * Set whether chapters should display whole information in a pop up.
     * Default true.
     */
    public void setChaptersShowPopUp(boolean val) {
//        popupChapters = val;
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
        if (user_drag) return;
        if(totalTime.get()==null) return; // bugfix
        double newPos = currentTime.get().toMillis()/totalTime.get().toMillis();
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
    
    private final class AddChapButton {
        Icon i = new Icon(SORT, 16);
        StackPane root = new StackPane(i);
        Anim fade = new Anim(millis(800),p -> {
            double p1 = mapTo01(p,0,0.45);
            double p2 = mapTo01(p,  0.55,1);
            i.setScaleY(p1);
            i.setRotate(90*p2);
        });
        Anim select = new Anim(millis(250),p -> i.setRotate(90 + 90*p));
        boolean visible = false;
        
        public AddChapButton() {
            seeker.addEventFilter(MOUSE_CLICKED, e -> {
                if(isVisible()) e.consume();
                if(e.getButton()==PRIMARY) {
                    if(isVisible() && abs(getCenterX()-e.getX()) < 16/2) { // if addB contains event
                        if(isSelected()) selectedChap.showPopup();
                        else addChap();
                    }
                } else {
                    if(isSelected()) unselect();
                    else {
                        if(isVisible()) hide();
                        else show();
                    }
                }
            });
            
            root.setPrefSize(25,25);
            root.setMouseTransparent(true);
            root.visibleProperty().bind(notEqual(i.scaleYProperty(),0)); // fixes potential bugs
            getChildren().add(root);
            root.setManaged(false);   // fixex a resizing issue
            i.styleclass(STYLECLASS_CHAP_ADD_BUTTON);
            i.setDisable(false);
            i.tooltip("Create chapter.\n\nCreates a new empty comment at this "
                    + "position and opens the editor.");
            
            fade.affector.accept(0d);
            select.affector.accept(0d);
        }
        
        void show() {
            i.setDisable(!Player.playingtem.get().isFileBased());
            fade.delay(i.getScaleY()==0 ? 350 : 0)
                .playOpenDo(() -> visible=true);
        }
        
        boolean isVisible() {
            return visible;
        }
        
        void hide() {
            visible=false;
            fade.delay(0).playCloseDo(null);
        }
        
        void select(Chap c) {
            selectedChap = c;
            addB.root.setLayoutX(c.getCenterX()-addB.root.getWidth()/2);
            select.playOpen();
        }
        
        boolean isSelected() {
            return selectedChap!=null;
        }
        
        void unselect() {
            selectedChap=null;
            select.playClose();
        }
        
        double getCenterX() {
            return root.getBoundsInParent().getMinX()+root.getBoundsInParent().getWidth()/2;
        }
        
        void setCenterX(double x) {
            double xx = x - root.getWidth()/2;
            root.setLayoutX(clip(0,xx,getWidth()));
        }
        
        void addChap() {
            double pos = getCenterX()/seeker.getWidth();
                   pos = clip(0,pos,1);     // fixes outside of area bugs
            Chap c = new Chap(pos);
                 c.pseudoClassStateChanged(STYLE_CHAP_NEW,true);
            Seeker.this.getChildren().add(c);
                 c.showPopup();
        }
    }

    private final class Chap extends Region {
        // inherent properties of the chapter
        private final double position;
        private Chapter c;
        private boolean just_created = false;
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
        Anim hover = new Anim(millis(150),this::setScaleX).intpl(x -> 1+7*x);
            
        Chap(double x) {
            this(new Chapter(totalTime.get().multiply(x), ""), x);
            just_created = true;
        }
        Chap(Chapter ch, double pos) {
            c = ch;
            position = pos;

            scaleYProperty().bind(seekerScaleY.multiply(0.5));
            getStyleClass().add(STYLECLASS_CHAP);
//            setOnMouseEntered(e -> showPopup());
            setOnMouseEntered(e -> addB.select(this));
            setOnMouseClicked(e -> seekTo());
            setMouseTransparent(true);
        }
        
        public void showPopup() {
            hover.playOpenDo(popupChapters ? () -> showPopupReal() : null);
        }
        
        public void hidePopup() {
            if(p!=null && p.isShowing()) p.hideStrong();
            else hover.playCloseDo(null);
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
                editB = new Icon(EDIT, 11, "Edit chapter", this::startEdit);
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
                p.setOnHidden(e -> {
                    if(editOn) cancelEdit();
                    hover.playCloseDo(just_created ? () -> Seeker.this.getChildren().remove(this) : null);
                });
                p.title.set(c.getTime().toString());
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
                        delayerCloser.start();
                    if(e.getClickCount()==2) {
                        can_hide = false;
                        if (e.getButton()==SECONDARY) startEdit();
                        else if (e.getButton()==PRIMARY) seekTo();
                    }
                    // consume to prevent real hide on click (just in case
                    // even if disabled)
                    e.consume();
                });
                // consume all key events to prevent accidents
                content.addEventHandler(KeyEvent.ANY, Event::consume);
            }
            // show if not already
            if(!p.isShowing()) p.show(this);
            if(just_created) startEdit();
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
                if (e.getCode()==ENTER) {
                    if(e.isShiftDown()) appendToCaret(ta, "\n");//ta.appendText("\n");
                    else commitEdit();
                    e.consume();
                }
                else if (e.getCode()==ESCAPE) {
                    cancelEdit();
                    e.consume();
                }
            });
            // maintain proper content
            content.getChildren().add(ta);
            message.setVisible(false);
            p.getHeaderIcons().set(0, commitB);
            p.getHeaderIcons().add(1, cancelB);
            p.getHeaderIcons().remove(editB);                                   // testing bug
                // do not add remove buton if the chapter is being created
            if (!Player.playingtem.get().containsChapterAt(c.getTime()))
                p.getHeaderIcons().remove(delB);
        }
        
        private void appendToCaret(TextArea a, String s) {
            String t = a.getText();
            int i = a.getCaretPosition();
            if(i>=t.length()) 
                a.appendText(s);
            else {
                String s1 = t.substring(0, i);
                String s2 = t.substring(i, t.length());
                a.setText(s1+s+s2);
                // actually we should somehow move caret to i+s/length but HOW?
            }
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
            message.setVisible(true);
            // maintain proper content
            p.getHeaderIcons().set(0, editB);
            p.getHeaderIcons().remove(cancelB);
            p.getHeaderIcons().remove(delB);    // make sure remB was removed
            p.getHeaderIcons().add(p.getHeaderIcons().size()-2,delB);   // add remove button back
            // stop edit
            editOn = false;
            if(just_created) Seeker.this.getChildren().remove(this);
        }
        
        /** Ends editable mode and discards all changes. */
        public void cancelEdit() {
            if(just_created) {
                hidePopup();
            } else {
                // go back & dont persist changes
                content.getChildren().remove(ta);
                message.setVisible(true);
                // maintain proper content
                p.getHeaderIcons().set(0, editB);
                p.getHeaderIcons().remove(cancelB);
            }
            // stop edit
            editOn = false;
        }
        
        public void seekTo() {
            PLAYBACK.seek(c.getTime());
        }
        
        double getCenterX() {
            return getBoundsInParent().getMinX()+getBoundsInParent().getWidth()/2;
        }

    }

}