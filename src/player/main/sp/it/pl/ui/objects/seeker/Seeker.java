package sp.it.pl.ui.objects.seeker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.scene.control.Slider;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.jetbrains.annotations.NotNull;
import sp.it.pl.audio.playback.PlaybackState;
import sp.it.pl.audio.tagging.Chapter;
import sp.it.pl.audio.tagging.Metadata;
import sp.it.pl.ui.objects.icon.Icon;
import sp.it.util.access.V;
import sp.it.util.animation.Anim;
import sp.it.util.animation.interpolator.CircularInterpolator;
import sp.it.util.async.executor.EventReducer;
import sp.it.util.functional.Functors.F1;
import sp.it.util.reactive.Subscription;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ANGLE_DOWN;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ANGLE_UP;
import static java.lang.Double.max;
import static java.lang.Math.abs;
import static javafx.scene.input.KeyCode.LEFT;
import static javafx.scene.input.KeyCode.RIGHT;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.DRAG_DETECTED;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.MouseEvent.MOUSE_DRAGGED;
import static javafx.scene.input.MouseEvent.MOUSE_MOVED;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static javafx.util.Duration.millis;
import static sp.it.pl.main.AppExtensionsKt.getEmScaled;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.pl.ui.objects.seeker.SeekerUtilsKt.bindTimeToSmooth;
import static sp.it.pl.ui.objects.window.NodeShow.DOWN_CENTER;
import static sp.it.util.Util.clip;
import static sp.it.util.animation.Anim.mapConcave;
import static sp.it.util.animation.Anim.mapTo01;
import static sp.it.util.async.AsyncKt.runFX;
import static sp.it.util.dev.FailKt.noNull;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.functional.UtilKt.runnable;
import static sp.it.util.ui.UtilKt.pseudoclass;
import static sp.it.util.units.DurationKt.toHMSMs;

/**
 * Playback seeker. A slider-like control that controls playback, by seeking.
 * Also manages (displays, edits, etc.) song chapters ({@link Metadata#getChapters()}).
 * <p/>
 * This control overrides {@link #layoutChildren()} and some layout properties may not work. For
 * example padding. Use padding on the parent of this control, rather than this control directly.
 */
public final class Seeker extends AnchorPane {

	private static final String STYLECLASS = "seeker";
	private static final String STYLECLASS_CHAP = "seeker-marker";
	private static final PseudoClass STYLE_CHAP_NEW = pseudoclass("newly-created");

	private final Slider seeker = new Slider(0, 1, 0);
	private final List<SeekerChapter> chapters = new ArrayList<>();
	private final DoubleProperty seekerScaleY = seeker.scaleYProperty();
	private boolean user_drag = false;

	public Seeker() {
		seeker.getStyleClass().add(STYLECLASS);
		seeker.setManaged(false);
		seeker.setSnapToTicks(false);
		seeker.setLabelFormatter(new StringConverter<>() {
			@Override public String toString(Double object) { return timeTot.getValue()==null ? "" : toHMSMs(timeTot.getValue().multiply(seeker.getValue())); }
			@Override public Double fromString(String string) { return null; }
		});
		getChildren().add(seeker);

		// mouse drag
		seeker.addEventFilter(MOUSE_PRESSED, e -> {
			if (e.getButton()==PRIMARY) {
				user_drag = true;
			}
			if (e.getButton()==SECONDARY && user_drag) {
				user_drag = false;
				seeker.setValueChanging(false);
			}
			e.consume();
		});
		seeker.addEventFilter(DRAG_DETECTED, e -> {
			if (isSelected()) unselect();
			user_drag = true;
			seeker.setValueChanging(true);
		});
		seeker.addEventFilter(MOUSE_DRAGGED, e -> {
			if (e.getButton()==PRIMARY && user_drag) {
				seeker.setValueChanging(true);
				double x = e.getX();
				double w = seeker.getWidth();
				double v = x/w;

				// snap to chapter
				SeekerChapter ch = minBy(chapters, chapterSnapDistance(), c -> abs(x - c.sce.position01*w)).orElse(null);
				setSeekerValue(ch==null || e.isShortcutDown() ? v : ch.sce.position01);
			}
			e.consume();
		});
		seeker.setOnMouseReleased(e -> {
			if (user_drag) {
				if (e.getButton()==PRIMARY) {
					double p = clip(0, e.getX()/getWidth(), 1);
					APP.audio.seek(p);
					seeker.setValueChanging(false);
					runFX(millis(100), () -> user_drag = false);
				}
				if (e.getButton()==SECONDARY) {
					user_drag = false;
					seeker.setValueChanging(false);
				}
			}
		});
		// We simulate mouse click with mouse released events and should therefore consume it
		// so if any parent node waits for it, it won't cause double behavior
		seeker.addEventHandler(MOUSE_CLICKED, Event::consume);

		// new chapter button
		addEventFilter(MOUSE_MOVED, e -> {
			if (isSelected()) {
				// if out of proximity -> unselect
				// if chapter closer than selected one -> select it
				double dist = abs(e.getX() - chapterSelected.getCenterX());
				minBy(chapters, chapterSnapDistance(), c -> abs(c.getCenterX() - e.getX()))
						.map(c -> c!=chapterSelected ? c : null)
						.ifPresentOrElse(this::select, () -> {
							if (dist>chapterSnapDistance())
								unselect();
						});
			} else {
				// if chapter in proximity -> select it
				minBy(chapters, chapterSnapDistance(), c -> abs(c.getCenterX() - e.getX()))
						.ifPresent(this::select);
			}
		});
		seeker.addEventHandler(KEY_PRESSED, e -> {
			if (e.getCode()==RIGHT) {
				if (e.isShiftDown()) APP.audio.seekForwardRelative();
				else APP.audio.seekForwardAbsolute();
				e.consume();
			} else if (e.getCode()==LEFT) {
				if (e.isShiftDown()) APP.audio.seekBackwardRelative();
				else APP.audio.seekBackwardAbsolute();
				e.consume();
			}
		});

		maInstall();
	}

	private void setSeekerValue(double value) {
		 seeker.setValue(value);    // This triggers expensive layout

//		var v = clip(0, value, 1);
//		var skin = seeker.getSkin();
//		var thumb = skin==null ? null : Util.<StackPane>getFieldValue(skin, "thumb");
//		if (thumb!=null) thumb.setTranslateX(seeker.getLayoutBounds().getWidth()*v-thumb.getLayoutBounds().getWidth()/2.0);
	}

	@Override
	protected void layoutChildren() {
		super.layoutChildren();
		var w = getWidth();
		var h = getHeight();

		seeker.resizeRelocate(0, 0, w, h);

		if (!chapters.isEmpty()) {
			var ss = seeker.getLayoutBounds().getMinX();
			var sw = seeker.getLayoutBounds().getWidth();
			var cW2 = 1 + chapters.get(0).getLayoutBounds().getWidth()/2;
			for (var c : chapters) {
				c.relocate(ss + clip(cW2, sw*c.sce.position01, sw - cW2) - cW2, h/2 - c.getHeight()/2);
				c.requestLayout();
			}
		}

		maMoveTo(maX);
	}

	private void onHoverChanged(Consumer<? super Boolean> handler) {
		var reducer = EventReducer.<Boolean>toLast(50, handler);
		hoverProperty().addListener((o, ov, nv) -> reducer.push(nv || isHover()));
	}

//****************************** runners animation *****************************/

	private static final double MA_ICON_SIZE = 10;
	private final Icon maR1 = new Icon(ANGLE_UP, MA_ICON_SIZE);
	private final Icon maR2 = new Icon(ANGLE_DOWN, MA_ICON_SIZE);
	private double maX = 0.0;

	private void maInstall() {
		ma_init();
		Anim sa = new Anim(millis(1000), p -> {
			double p1 = mapTo01(p, 0, 0.5);
			double p2 = mapTo01(p, 0.8, 1);
			double p3 = mapTo01(p, 0.3, 0.6);

			maR1.setOpacity(p1);
			maR2.setOpacity(p1);

			double scale = 1 + 0.8*mapConcave(p3);
			maR1.setScaleX(scale);
			maR1.setScaleY(scale);
			maR2.setScaleX(scale);
			maR2.setScaleY(scale);
		}).intpl(new CircularInterpolator())
			.delay(millis(150));
		onHoverChanged(sa::playFromDir);
	}

	private void ma_init() {
		maR1.setFocusTraversable(false);
		maR2.setFocusTraversable(false);
		maR1.setOpacity(0);
		maR2.setOpacity(0);
		maR1.setMouseTransparent(true);
		maR2.setMouseTransparent(true);
		maR1.setManaged(false);
		maR2.setManaged(false);
		getChildren().addAll(maR1, maR2);

		addEventFilter(MOUSE_MOVED, e -> maMoveTo(isSelected() ? chapterSelected.getCenterX() : e.getX()));
		addEventFilter(MOUSE_DRAGGED, e -> maMoveTo(isSelected() ? chapterSelected.getCenterX() : e.getX()));
	}

	private void maMoveTo(double x) {
		maX = x;
		maR1.relocate(maX - maR1.getLayoutBounds().getWidth()/2.0, getHeight()/2 + getEmScaled(5));
		maR2.relocate(maX - maR2.getLayoutBounds().getWidth()/2.0, getHeight()/2 - getEmScaled(5) - maR2.getLayoutBounds().getHeight());
		maR1.requestLayout();
		maR2.requestLayout();
	}

//********************************** chapters *********************************/


	/** Chapter display strategy. */
	@NotNull public final V<ChapterDisplayMode> chapterDisplayMode = new V<>(ChapterDisplayMode.POPUP_SHARED);
	/** Chapter display activation strategy. */
	@NotNull public final V<ChapterDisplayActivation> chapterDisplayActivation = new V<>(ChapterDisplayActivation.HOVER);
	/** Whether seeker snaps to chapters. */
	@NotNull public final V<Boolean> chapterSnap = new V<>(false);
	/** Seeker snap to chapter activation distance. */
	@NotNull public final DoubleProperty chapterSnapDistance = new SimpleDoubleProperty(15);
	/** Chapter selected */
	private SeekerChapter chapterSelected = null;

	private final Anim selectChapAnim = new Anim(millis(500), p -> {
		double h = getHeight();
		double y = max(0, h - 20 - 10)/3;
		double dy = y*p*p*p;
		maR1.setTranslateY(dy);
		maR2.setTranslateY(-dy);
	});

	/** Reload chapters. Use on chapter data change, e.g., on chapter add/remove. */
	public void reloadChapters(Metadata m) {
		noNull(m);

		getChildren().removeAll(chapters);
		chapters.clear();

		if (chapterDisplayMode.get().canBeShown()) {
			var chapterList = m.getChapters().getChapters();
			for (int i = 0; i<chapterList.size(); i++) {
				var c = chapterList.get(i);
				var sc = new SeekerChapter(m, c, c.getTime().toMillis()/m.getLength().toMillis(), i);
				chapters.add(sc);
				getChildren().add(sc);
			}
		}

		requestLayout();
	}

	private double chapterSnapDistance() {
		return getEmScaled(chapterSnapDistance.getValue());
	}

	{
		seeker.addEventFilter(MOUSE_CLICKED, e -> {
			if (e.getButton()==SECONDARY) {
				if (isSelected()) {
					if (chapterDisplayActivation.get()==ChapterDisplayActivation.RIGHT_CLICK) chapterSelected.showPopup();
				} else {
					chapterAdd(e.getX()/seeker.getWidth());
				}
				e.consume();
			}
		});
	}

	private void select(SeekerChapter c) {
		var oc = chapterDisplayActivation.get()==ChapterDisplayActivation.HOVER ? runnable(c::showPopup) : null;
		chapterSelected = c;
		selectChapAnim.playOpenDoClose(oc);             // animate runners & open chap in middle
		maMoveTo(c.getCenterX());                       // animate-move runners to chapter
	}

	private boolean isSelected() {
		return chapterSelected!=null;
	}

	private void unselect() {
		chapterSelected = null;
		selectChapAnim.playClose();   // animate runners
	}

	private void chapterAdd(double at) {
		SeekerChapter c = new SeekerChapter(APP.audio.getPlayingSong().getValue(), clip(0, at, 1));
		chapters.add(c);
		Seeker.this.getChildren().add(c);
		Seeker.this.requestLayout();
		c.requestLayout();
		c.showPopup();
	}

	private void chapterRem(SeekerChapter c) {
		chapters.remove(c);
		Seeker.this.getChildren().remove(c);
		Seeker.this.requestLayout();
	}

	/****************************************** POSITION **********************************************/

	private ObjectProperty<Duration> timeTot = null;

	/**
	 * Binds to total and current duration value. This will cause seeker to update when the total
	 * or current time value changes and display time position. At the end, the binding must be
	 * disposed, which is done by running the returned object. It will remove the listeners and stop
	 * the updating.
	 * <p/>
	 * It is recommended to create a duration property which always contains the total and current
	 * time of the playing song and then call this method only once and subsequently call dispose
	 * only once as well at the end of the entire playback.
	 *
	 * @return the runnable which disposes of the binding
	 */
	public Subscription bindTime(PlaybackState playback) {
		timeTot = playback.getDuration();
		return bindTimeToSmooth(playback, consumer(it -> { if (!user_drag) setSeekerValue(it); }));
	}

	private final class SeekerChapter extends Region {
		final SongChapterEdit sce;
		final Anim hover = new Anim(millis(150), this::setScaleX).intpl(x -> 1 + 7*x);

		SeekerChapter(Metadata song, double position01) {
			getStyleClass().add(STYLECLASS_CHAP);
			pseudoClassStateChanged(STYLE_CHAP_NEW, true);
			scaleYProperty().bind(seekerScaleY.multiply(0.5));

			sce = new SongChapterEdit(song, new Chapter(timeTot.get().multiply(position01), ""), position01, -1, true);
			sce.onHidden.add(runnable(() -> hover.playCloseDo(sce.isNew ? runnable(() -> chapterRem(this)) : null)));
		}

		SeekerChapter(Metadata song, Chapter chapter, double position01, int i) {
			getStyleClass().add(STYLECLASS_CHAP);
			scaleYProperty().bind(seekerScaleY.multiply(0.5));
			setOnMouseEntered(e -> select(this));
			setOnMouseClicked(e -> seekTo());
			setMouseTransparent(true);

			sce = new SongChapterEdit(song, chapter, position01, i, false);
			sce.onHidden.add(runnable(() -> hover.playCloseDo(sce.isNew ? runnable(() -> chapterRem(this)) : null)));
		}

		public void showPopup() {
			hover.playOpenDo(runnable(chapterDisplayMode.get().isShownAsPopup() ? () -> sce.showPopup(DOWN_CENTER.invoke(this)) : () -> {}));
		}

		public void hidePopup() {
			hover.playCloseDo(null);
			sce.hidePopup();
		}

		public void seekTo() {
			sce.seekTo();
			hover.playOpenDo(runnable(chapterDisplayMode.get().isShownAsPopup() ? () -> sce.showPopup(DOWN_CENTER.invoke(this)) : () -> {}));
		}

		double getCenterX() {
			return getBoundsInParent().getCenterX();
		}

	}

	private static <V, C extends Comparable<? super C>> Optional<V> minBy(Collection<V> c, C atMost, F1<? super V,C> by) {
		V minV = null;
		C minC = atMost;
		for (V v : c) {
			C vc = by.apply(v);
			if (minC==null || vc.compareTo(minC)<0) {
				minV = v;
				minC = vc;
			}
		}
		return Optional.ofNullable(minV);
	}
}