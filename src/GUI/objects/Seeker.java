
package gui.objects;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import org.reactfx.EventSource;
import org.reactfx.Subscription;

import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.tagging.Chapters.Chapter;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataWriter;
import gui.objects.PopOver.PopOver;
import gui.objects.icon.Icon;
import util.access.V;
import util.animation.Anim;
import util.animation.Loop;
import util.animation.interpolator.CircularInterpolator;
import util.async.executor.FxTimer;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;
import static gui.objects.PopOver.PopOver.ArrowLocation.TOP_CENTER;
import static gui.objects.icon.Icon.createInfoIcon;
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
import static javafx.scene.media.MediaPlayer.Status.PLAYING;
import static javafx.util.Duration.*;
import static util.Util.clip;
import static util.animation.Anim.mapConcave;
import static util.animation.Anim.mapTo01;
import static util.async.Async.run;
import static util.functional.Util.minBy;
import static util.graphics.Util.setAnchor;
import static util.reactive.Util.maintain;

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
        setAnchor(this,seeker,null,0d,null,0d);

        // mouse drag
        seeker.addEventFilter(MOUSE_PRESSED, e -> {
            if(e.getButton()==PRIMARY) user_drag = true;
            e.consume();
        });
        seeker.addEventFilter(DRAG_DETECTED, e -> {
            if(addB.isSelected()) addB.unselect();
            if(addB.isShown()) addB.hide();
            user_drag = true;
        });
        seeker.addEventFilter(MOUSE_DRAGGED, e -> {
            if(e.getButton()==PRIMARY && user_drag) {
                double x = e.getX();
                double w = getWidth();
                double v = x/w;

                // snap to chapter
                Chap ch = minBy(chapters, chapSnapDist.get(), c -> abs(x-c.position*w)).orElse(null);
                seeker.setValue(ch==null ? v : ch.position);
            }
            e.consume();
        });
        seeker.setOnMouseReleased(e -> {
            if(user_drag) {
                if(e.getButton()==PRIMARY) {
                    double p = e.getX()/getWidth();
                           p = clip(0,p,1);
                    PLAYBACK.seek(p);
                    run(100, () -> user_drag = false);
                    if(seeker.isHover()) addB.show(); // ~bugfix
                }
                if(e.getButton()==SECONDARY) {
                    user_drag = false;
                }
            }
        });

        // new chapter button
        addB.root.toFront();
        addEventFilter(MOUSE_MOVED, e -> {
            if(addB.isShown()) {
                if(addB.isSelected()) {
                    // if out of proximity -> unselect
                    // if chapter closer than selected one -> select it
                    double dist = abs(e.getX()-selectedChap.getCenterX());
                    minBy(chapters, chapSnapDist.get(), c -> abs(c.getCenterX()-e.getX()))
                       .map(c -> c!=selectedChap ? c : null)
                       .ifPresentOrElse(addB::select, () -> {
                            if(dist > chapSnapDist.get())
                                addB.unselect();
                        });
                } else {
                    // if chapter in proximity -> select it
                    minBy(chapters, chapSnapDist.get(), c -> abs(c.getCenterX()-e.getX()))
                       .ifPresent(addB::select);
                }
            }
            if(addB.isVisible() && !addB.isSelected())
                addB.setCenterX(e.getX());
        });
        seeker.hoverProperty().addListener((o,ov,nv) -> {
            if(nv) addB.show();
            else addB.hide();
        });
        seeker.addEventFilter(MOUSE_MOVED, e -> {
            if(!user_drag && !addB.isVisible())
                addB.show();
        });

        // animation 1
        ma_init();
        // animation 2
        Anim sa = new Anim(millis(1000),p -> {
                double p1 = mapTo01(p,0,0.5);
                double p2 = mapTo01(p,  0.8,1);
                double p3 = mapTo01(p,  0.3,0.6);

                r1.setOpacity(p1);
                r2.setOpacity(p1);

                double scale = 1 + 0.8*mapConcave(p3);
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

        if(!chapters.isEmpty()) {
            double fix = 1+chapters.get(0).getLayoutBounds().getWidth()/2; // bugfix
            for(Chap c : chapters) {
                c.relocate(clip(fix,w * c.position,getWidth()-fix), h/2 - c.getHeight()/2);
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

//****************************** runners animation *****************************/

    private static final double MA_ISIZE = 10;
    private static final double MA_WIDTH2 = 2.5;    // hardcoded, layoutBounds().getWidth() !work
    private final Loop ma = new Loop(this::ma_do);
    private final Icon r1 = new Icon(ANGLE_DOWN, MA_ISIZE);
    private final Icon r2 = new Icon(ANGLE_UP, MA_ISIZE);
    double matox = 0;
    double macurx = 0;
    double maspeed = 0;

    private void ma_do() {
        // calculate new x
        double diff = matox-macurx;
        if(diff==0) return;                     // perf optim. & bug fix
        double dir = signum(diff);
        double dist = abs(diff);
        maspeed = max(1,dist/10d);              // prevents animation from never finishing
        macurx += dir * maspeed;
        if(abs(macurx-matox)<1) macurx=matox;   // finish anim in next cycle

        // apply
        double x = macurx;
               x = clip(0,x,getWidth());        // fixes outside of area bugs
        r1.setX(x - MA_WIDTH2);
        r2.setX(x - MA_WIDTH2);
        // we can also move add chapter button here (for different behavior)
        // addB.root.setLayoutX(macurx-addB.root.getWidth()/2);
    }

    private void ma_init() {
        r1.setOpacity(0);
        r2.setOpacity(0);
        r1.setMouseTransparent(true);
        r2.setMouseTransparent(true);
        r1.setManaged(false);   // fixes a resizing issue
        r2.setManaged(false);
        getChildren().addAll(r1,r2);

        addEventFilter(MOUSE_MOVED, e -> matox = addB.isSelected() ? selectedChap.getCenterX() : e.getX());
        addEventFilter(MOUSE_DRAGGED, e -> matox = addB.isSelected() ? selectedChap.getCenterX() : e.getX());
        ma.start(); // starts animation
    }

//****************************** selection animation *************************/

    private final Anim selectChapAnim = new Anim(millis(500), p -> {
        double h = getHeight();
        double y = max(0,h-20-10)/3;
        double dy = y * p*p*p;
        r1.setTranslateY(dy);
        r2.setTranslateY(-dy);
    } );

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
        for(Chapter ch: m.getChapters()) {
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
    public final V<Boolean> selectChapOnHover = new V<>(true);

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

/****************************************** POSITION **********************************************/

    private ObjectProperty<Duration> timeTot = null;
    private ObjectProperty<Duration> timeCur = null;
    private final ChangeListener<Object> timeUpdater = (o,ov,nv) -> timeUpdate();
    private final Loop timeLoop = new Loop(this::timeUpdateDo);
    private double posLast = 0;
    private long posLastFrame = 0;
    private double posUpdateInterval = 20;
    private long polastUpdate = 0;

    /**
     * Binds to total and current duration value. This will cause seeker to update when the total
     * or current time value changes and display time position. At the end, the binding must be
     * disposed, which is done by running the returned object. It will remove the listeners and stop
     * the updating.
     * <p>
     * It is recommended to create a duration property which always contains the total and current
     * time of the playing song and then call this method only once and subsequently call dispose
     * only once as well at the end of the entire playback.
     *
     * @param totalTime length of the song
     * @param currentTime time seeker within the playback of the song.
     * @return the runnable which disposes of the binding
     */
    public Subscription bindTime(ObjectProperty<Duration> totalTime, ObjectProperty<Duration> currentTime) {
        if(timeTot!=null) timeTot.removeListener(timeUpdater);
        if(timeCur!=null) timeCur.removeListener(timeUpdater);
        widthProperty().removeListener(timeUpdater);

        timeTot = totalTime;
        timeCur = currentTime;

        timeTot.addListener(timeUpdater);
        timeCur.addListener(timeUpdater);
        widthProperty().addListener(timeUpdater);

        timeUpdater.changed(null,ZERO, ZERO);
        timeLoop.start();

        return () -> {
            ma.stop();
            timeLoop.stop();
            if(timeTot!=null) timeTot.unbind();
            if(timeCur!=null) timeCur.unbind();
            if(timeTot!=null) timeTot.removeListener(timeUpdater);
            if(timeCur!=null) timeCur.removeListener(timeUpdater);
            timeTot = new SimpleObjectProperty<>(Duration.ONE);
            timeCur = new SimpleObjectProperty<>(Duration.ONE);
            timeUpdater.changed(null,null,null);
        };
    }

    private void timeUpdate() {
        if(timeTot.get()==null) return; // bugfix
        posLast = timeCur.get().toMillis()/timeTot.get().toMillis();
        posLastFrame = 0;   // when we seek dt must be 0
        posUpdateInterval = clip(0,timeTot.get().toMillis()/getWidth(),60);
    }

    private void timeUpdateDo(long frame) {
        if(!user_drag && PLAYBACK.state.status.get()==PLAYING) {
            long dt = posLastFrame==0 ? 0 : (frame-posLastFrame)/1000000;
            double dp = dt/timeTot.get().toMillis();
            posLast += dp;

            long now = System.currentTimeMillis();
            if(now-polastUpdate>posUpdateInterval) {
                polastUpdate = now;
                seeker.setValue(posLast);
            }
        }
        posLastFrame = frame;
    }

/**************************************************************************************************/

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
            // this button is mouse transparent, we handle click on our own
            // and avoid nodes blocking events + we can use arbitrary click area
            seeker.addEventFilter(MOUSE_CLICKED, e -> {
                if(e.getButton()==SECONDARY) {
                    if(isShown() && abs(getCenterX()-e.getX()) < 16/2) { // if addB contains event
                        if(isSelected()) selectedChap.showPopup();
                        else addChap();
                    }
                    e.consume();
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
            fade.playOpenDo(() -> visible=true);
        }

        boolean isShown() {
            return visible;
        }

        boolean isVisible() {
            return root.getOpacity()!=0; // depends on animation
        }

        void hide() {
            selectedChap=null;
            visible=false;
            fade.playCloseDo(null);
        }

        void select(Chap c) {
            Runnable oc = selectChapOnHover.getValue() ? ()-> c.showPopup() : null; // open chap
            selectedChap = c;
            setCenterX(c.getCenterX());             // move this to chapter
            select.playOpen();                      // animate this
            selectChapAnim.playOpenDoClose(oc);     // animate runners & open chap in middle
            matox = c.getCenterX();                 // animate-move runners to chapter
        }

        boolean isSelected() {
            return selectedChap!=null;
        }

        void unselect() {
            selectedChap=null;
            select.playClose();
            selectChapAnim.playClose();   // animate runners
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
            chapters.add(c);
            Seeker.this.getChildren().add(c);
                 c.showPopup();
        }
    }

    private final class Chap extends Region {
        final double position;
        final Chapter c;
        boolean just_created;
        private final V<Boolean> isEdited = new V<>(false);

        StackPane content;
        Text message;
        TextArea ta;            // edit text area
        PopOver<?> p, helpP;           // main & help popup
        Icon helpB, prevB, nextB, editB, commitB, delB, cancelB; // popup controls
        Anim hover = new Anim(millis(150),this::setScaleX).intpl(x -> 1+7*x);

        Chap(double x) {
            this(new Chapter(timeTot.get().multiply(x), ""), x);
            just_created = true;
        }
        Chap(Chapter ch, double pos) {
            c = ch;
            position = pos;
            just_created = false;

            scaleYProperty().bind(seekerScaleY.multiply(0.5));
            getStyleClass().add(STYLECLASS_CHAP);
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
            // build popup if not yet
            if(p==null) {
                // text content
                message = new Text(c.getText());
                message.setWrappingWidthNatural(true);
                message.setTextAlignment(TextAlignment.CENTER);
                content = new StackPane(message);
                content.setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
                content.setPadding(new Insets(10));
                content.addEventHandler(Event.ANY, e -> { if(isEdited.getValue()) e.consume(); });
                content.addEventHandler(KeyEvent.ANY, Event::consume);
                content.autosize();
                // buttons
                editB = new Icon(EDIT, 11, "Edit chapter", this::startEdit);
                commitB = new Icon(CHECK, 11, "Confirm changes", this::commitEdit);
                delB = new Icon(TRASH_ALT, 11, "Remove chapter", () -> {
                     Metadata m = Player.playingtem.get();
                     MetadataWriter.use(m, w->w.removeChapter(c,m));
                });
                cancelB = new Icon(REPLY, 11, "Cancel edit", this::cancelEdit);
                nextB = new Icon(CHEVRON_RIGHT, 11, "Next chapter", () -> {
                    int i = Seeker.this.chapters.indexOf(this) + 1;
                    if(chapters.size()>i){
                        hidePopup();
                        chapters.get(i).showPopup();
                    }
                });
                prevB = new Icon(CHEVRON_LEFT, 11, "Previous chapter", () -> {
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
                helpB = createInfoIcon(
                       "Single click : Close\n"
                     + "Double L click : Play from this chapter\n"
                     + "Double R click : Start edit\n"
                     + "Enter : Apply edit changes\n"
                     + "Shift + Enter : Append new line\n"
                     + "Escape : If editing cancel edit, else hide"
                ).size(11);
                // popup
                p = new PopOver<>(content);
                p.getSkinn().setContentPadding(new Insets(10));
                p.setArrowLocation(TOP_CENTER);
                maintain(isEdited,v->!v,p::setAutoHide); // breaks editing >> p.setAutoHide(true);
                p.setHideOnEscape(true);
                p.setHideOnClick(false); // will emulate on our own
                p.setAutoFix(false);
                p.setOnHidden(e -> {
                    if(isEdited.getValue()) cancelEdit();
                    hover.playCloseDo(just_created ? () -> Seeker.this.getChildren().remove(this) : null);
                });
                p.title.setValue(c.getTime().toString());
                p.getHeaderIcons().setAll(helpB, prevB, nextB, editB, delB);
                content.setOnMouseClicked(e -> {
                    // if info popup displayed close it and act as if content is
                    // mouse transparent to prevent any action
                    if(helpP!=null && helpP.isShowing()) {
                        helpP.hideStrong();
                        return;
                    }

                    if(isEdited.getValue()) return;

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
                    // consume to prevent real hide on click (just in case even if disabled)
                    e.consume();
                });
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


        /** Returns whether editing is currently active. */
        public boolean isEdited() {
            return isEdited.getValue();
        }

        /** Starts editable mode. */
        public void startEdit() {
            if (!editableChapters || isEdited.getValue()) return;
            // start edit
            isEdited.setValue(true);
            ta = new TextArea();
            // resize on text change
            maintain(ta.textProperty(), text -> {
                int len = text==null ? 0 : text.length();
                double w = 110 + len/3;
                ta.setPrefWidth(w);
                ta.setPrefHeight(0.8*w);
            });
            ta.setWrapText(true);
            ta.setText(message.getText());
            ta.setOnKeyPressed(e -> {
                if (e.getCode()==ENTER) {
                    if(e.isShiftDown()) ta.insertText(ta.getCaretPosition(),"\n");
                    else commitEdit();
                    e.consume();
                }
                else if (e.getCode()==ESCAPE) {
                    cancelEdit();
                    e.consume();
                }
            });
            // maintain proper content
            content.getChildren().remove(message);
            content.getChildren().add(ta);
            p.getHeaderIcons().setAll(helpB,commitB,cancelB);
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
            // maintain proper content
            content.getChildren().remove(ta);
            content.getChildren().add(message);
            p.getHeaderIcons().setAll(helpB, prevB, nextB, editB, delB);
            // stop edit
            isEdited.setValue(false);
            if(just_created) Seeker.this.getChildren().remove(this);
        }

        /** Ends editable mode and discards all changes. */
        public void cancelEdit() {
            if(just_created) {
                hidePopup();
                chapters.remove(this);
            } else {
                // maintain proper content
                content.getChildren().remove(ta);
                content.getChildren().add(message);
                p.getHeaderIcons().setAll(helpB, prevB, nextB, editB, delB);
            }
            // stop edit
            isEdited.setValue(false);
        }

        public void seekTo() {
            PLAYBACK.seek(c.getTime());
        }

        double getCenterX() {
            return getBoundsInParent().getMinX()+getBoundsInParent().getWidth()/2;
        }

    }

}