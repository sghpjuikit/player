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
import javafx.geometry.Insets;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.jetbrains.annotations.NotNull;
import sp.it.pl.audio.playback.PlaybackState;
import sp.it.pl.audio.tagging.Chapter;
import sp.it.pl.audio.tagging.Metadata;
import sp.it.pl.ui.objects.SpitText;
import sp.it.pl.ui.objects.icon.Icon;
import sp.it.pl.ui.objects.window.popup.PopWindow;
import sp.it.util.access.V;
import sp.it.util.animation.Anim;
import sp.it.util.animation.interpolator.CircularInterpolator;
import sp.it.util.async.executor.EventReducer;
import sp.it.util.async.executor.FxTimer;
import sp.it.util.functional.Functors.F1;
import sp.it.util.reactive.Subscription;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ANGLE_DOWN;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ANGLE_UP;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CHECK;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CHEVRON_LEFT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CHEVRON_RIGHT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.EDIT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.REPLY;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.TRASH_ALT;
import static java.lang.Double.max;
import static java.lang.Math.abs;
import static javafx.geometry.Pos.CENTER;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.ESCAPE;
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
import static sp.it.pl.audio.tagging.Chapter.validateChapterText;
import static sp.it.pl.audio.tagging.SongWritingKt.write;
import static sp.it.pl.main.AppBuildersKt.appTooltip;
import static sp.it.pl.main.AppExtensionsKt.getEmScaled;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.pl.ui.itemnode.ConfigFieldEditorsKt.STYLECLASS_CONFIG_EDITOR_WARN_BUTTON;
import static sp.it.pl.ui.objects.seeker.SeekerUtilsKt.bindTimeToSmooth;
import static sp.it.pl.ui.objects.window.NodeShow.DOWN_CENTER;
import static sp.it.util.Util.clip;
import static sp.it.util.animation.Anim.mapConcave;
import static sp.it.util.animation.Anim.mapTo01;
import static sp.it.util.async.AsyncKt.runFX;
import static sp.it.util.async.executor.FxTimer.fxTimer;
import static sp.it.util.dev.FailKt.noNull;
import static sp.it.util.functional.TryKt.getAny;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.functional.UtilKt.runnable;
import static sp.it.util.reactive.UtilKt.syncC;
import static sp.it.util.ui.Util.layHeaderRight;
import static sp.it.util.ui.UtilKt.pseudoclass;
import static sp.it.util.ui.UtilKt.typeText;
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
	private final AddChapButton addB = new AddChapButton();
	private final List<SeekerChapter> chapters = new ArrayList<>();
	private final DoubleProperty seekerScaleY = seeker.scaleYProperty();
	private boolean user_drag = false;

	public Seeker() {
		seeker.getStyleClass().add(STYLECLASS);
		seeker.setManaged(false);
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
			if (addB.isSelected()) addB.unselect();
			if (addB.isShown()) addB.hide();
			user_drag = true;
			seeker.setValueChanging(true);
		});
		seeker.addEventFilter(MOUSE_DRAGGED, e -> {
			if (e.getButton()==PRIMARY && user_drag) {
				seeker.setValueChanging(true);
				double x = e.getX();
				double w = getWidth();
				double v = x/w;

				// snap to chapter
				SeekerChapter ch = minBy(chapters, chapterSnapDistance(), c -> abs(x - c.position01*w)).orElse(null);
				setSeekerValue(ch==null ? v : ch.position01);
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
					if (seeker.isHover()) addB.show(); // ~bug fix
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
		addB.root.toFront();
		addEventFilter(MOUSE_MOVED, e -> {
			if (addB.isShown()) {
				if (addB.isSelected()) {
					// if out of proximity -> unselect
					// if chapter closer than selected one -> select it
					double dist = abs(e.getX() - chapterSelected.getCenterX());
					minBy(chapters, chapterSnapDistance(), c -> abs(c.getCenterX() - e.getX()))
							.map(c -> c!=chapterSelected ? c : null)
							.ifPresentOrElse(addB::select, () -> {
								if (dist>chapterSnapDistance())
									addB.unselect();
							});
				} else {
					// if chapter in proximity -> select it
					minBy(chapters, chapterSnapDistance(), c -> abs(c.getCenterX() - e.getX()))
							.ifPresent(addB::select);
				}
			}
			if (addB.isVisible() && !addB.isSelected())
				addB.setCenterX(e.getX());
		});
		seeker.hoverProperty().addListener((o, ov, nv) -> {
			if (nv) addB.show();
			else addB.hide();
		});
		seeker.addEventFilter(MOUSE_MOVED, e -> {
			if (!user_drag && !addB.isVisible())
				addB.show();
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
		double w = getWidth();
		double h = getHeight();

		if (!chapters.isEmpty()) {
			double fix = 1 + chapters.get(0).getLayoutBounds().getWidth()/2; // bug fix
			for (var c : chapters) {
				c.relocate(clip(fix, w*c.position01, getWidth() - fix), h/2 - c.getHeight()/2);
			}
		}

		seeker.resizeRelocate(0, 0, w, h);
		addB.root.relocate(addB.root.getLayoutX(), h/2 - addB.root.getHeight()/2);
		maMoveTo(maX);
	}

	private void onHoverChanged(Consumer<? super Boolean> handler) {
		var reducer = EventReducer.<Boolean>toLast(50, handler);
		hoverProperty().addListener((o, ov, nv) -> reducer.push(nv || addB.root.isHover()));
		addB.root.hoverProperty().addListener((o, ov, nv) -> reducer.push(nv || isHover()));
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

		addEventFilter(MOUSE_MOVED, e -> maMoveTo(addB.isSelected() ? chapterSelected.getCenterX() : e.getX()));
		addEventFilter(MOUSE_DRAGGED, e -> maMoveTo(addB.isSelected() ? chapterSelected.getCenterX() : e.getX()));
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
			List<Chapter> chapterList = m.getChapters().getChapters();
			for (int i = 0; i<chapterList.size(); i++) {
				var c = chapterList.get(i);
				var sc = new SeekerChapter(m, c, c.getTime().toMillis()/m.getLength().toMillis(), i);
				getChildren().add(sc);
				chapters.add(sc);
			}
		}
	}

	private double chapterSnapDistance() {
		return getEmScaled(chapterSnapDistance.getValue());
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

	/**************************************************************************************************/

	private final class AddChapButton {
		private final StackPane root = new StackPane();
		private final Anim fade = new Anim(millis(800), p -> {});
		private final Anim select = new Anim(millis(250), p -> {});
		private boolean visible = false;

		public AddChapButton() {
			// this button is mouse transparent, we handle click on our own
			// and avoid nodes blocking events + we can use arbitrary click area
			seeker.addEventFilter(MOUSE_CLICKED, e -> {
				if (e.getButton()==SECONDARY) {
					if (isShown() && abs(getCenterX() - e.getX())<16.0/2.0) { // if addB contains event
						if (isSelected()) {
							if (chapterDisplayActivation.get()==ChapterDisplayActivation.RIGHT_CLICK) chapterSelected.showPopup();
						} else {
							addChap();
						}
					}
					e.consume();
				}
			});

			root.setPrefSize(25, 25);
			root.setMouseTransparent(true);
			root.setVisible(false);
			root.setManaged(false);   // fixes a resizing issue
			getChildren().add(root);

			fade.applyNow();
			select.applyNow();
		}

		void show() {
			fade.playOpenDo(runnable(() -> visible = true));
		}

		boolean isShown() {
			return visible;
		}

		boolean isVisible() {
			return root.getOpacity()!=0; // depends on animation
		}

		void hide() {
			chapterSelected = null;
			visible = false;
			fade.playCloseDo(null);
		}

		void select(SeekerChapter c) {
			var oc = chapterDisplayActivation.get()==ChapterDisplayActivation.HOVER ? runnable(c::showPopup) : null;
			chapterSelected = c;
			setCenterX(c.getCenterX());                     // move this to chapter
			select.playOpen();                              // animate this
			selectChapAnim.playOpenDoClose(oc);             // animate runners & open chap in middle
			maMoveTo(c.getCenterX());                       // animate-move runners to chapter
		}

		boolean isSelected() {
			return chapterSelected!=null;
		}

		void unselect() {
			chapterSelected = null;
			select.playClose();
			selectChapAnim.playClose();   // animate runners
		}

		double getCenterX() {
			return root.getBoundsInParent().getMinX() + root.getBoundsInParent().getWidth()/2;
		}

		void setCenterX(double x) {
			double xx = x - root.getWidth()/2;
			root.setLayoutX(clip(0, xx, getWidth()));
		}

		void addChap() {
			double pos = getCenterX()/seeker.getWidth();
			pos = clip(0, pos, 1);     // fixes outside of area bugs
			SeekerChapter c = new SeekerChapter(APP.audio.getPlayingSong().getValue(), pos);
			c.pseudoClassStateChanged(STYLE_CHAP_NEW, true);
			chapters.add(c);
			Seeker.this.getChildren().add(c);
			c.showPopup();
		}
	}

	private final class SeekerChapter extends Region {
		final Metadata song;
		final Chapter c;
		final double position01;
		final int i;
		/** Whether this chapter is being created.  */
		boolean isNew;
		/** Whether editing is currently active.  */
		public final V<Boolean> isEdited = new V<>(false);

		StackPane content;
		TextArea message;
		Anim messageAnimation;
		TextArea ta;                    // edit text area
		PopWindow p;
		Icon prevB, nextB, editB, commitB, delB, cancelB; // popup controls
		final Anim hover = new Anim(millis(150), this::setScaleX).intpl(x -> 1 + 7*x);

		private boolean can_hide = true;
		private final FxTimer delayerCloser = fxTimer(millis(200), 1, runnable(() -> {
			if (can_hide) p.hide();
			can_hide = true;
		}));

		SeekerChapter(Metadata song, double position01) {
			this(song, new Chapter(timeTot.get().multiply(position01), ""), position01, -1);
			isNew = true;
		}

		SeekerChapter(Metadata song, Chapter chapter, double position01, int i) {
			this.song = song;
			this.c = chapter;
			this.position01 = position01;
			this.i = i;
			isNew = false;

			scaleYProperty().bind(seekerScaleY.multiply(0.5));
			getStyleClass().add(STYLECLASS_CHAP);
			setOnMouseEntered(e -> addB.select(this));
			setOnMouseClicked(e -> seekTo());
			setMouseTransparent(true);
		}

		public void showPopup() {
			hover.playOpenDo(runnable(chapterDisplayMode.get().isShownAsPopup() ? this::showPopupReal : () -> {}));
		}

		public void hidePopup() {
			if (p!=null && p.isShowing()) p.hide();
			else hover.playCloseDo(null);
		}

		public void showPopupReal() {
			// hide other popups if only one allowed
			if (chapterDisplayMode.get()==ChapterDisplayMode.POPUP_SHARED)
				chapters.stream().filter(f -> f!=this).forEach(SeekerChapter::hidePopup);
			// build popup if not yet
			if (p==null) {
				// text content
				message = new TextArea();
				message.setWrapText(true);
				message.setEditable(false);
				message.setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
				var messageInterpolator = typeText(c.getText(), '\u2007');
				messageAnimation = new Anim(millis(10*c.getText().length()), p -> message.setText(messageInterpolator.invoke(p))).delay(millis(200));
				content = new StackPane(message);
				content.setPrefSize(getEmScaled(300), getEmScaled(200));
				content.setPadding(new Insets(10));
				content.addEventHandler(Event.ANY, e -> { if (isEdited.getValue()) e.consume(); });
				content.autosize();
				// buttons
				editB = new Icon(EDIT, 11, "Edit chapter", this::startEdit);
				commitB = new Icon(CHECK, 11, "Confirm changes", this::commitEdit);
				delB = new Icon(TRASH_ALT, 11, "Remove chapter", () -> write(song, consumer(it -> it.removeChapter(c, song))));
				cancelB = new Icon(REPLY, 11, "Cancel edit", this::cancelEdit);
				prevB = new Icon(CHEVRON_LEFT, 11, "Previous chapter", () -> {
					hidePopup();
					chapters.get(i-1).showPopup();
				});
				nextB = new Icon(CHEVRON_RIGHT, 11, "Next chapter", () -> {
					hidePopup();
					chapters.get(i+1).showPopup();
				});
				int i = chapters.indexOf(this);
				if (chapters.size() - 1==i)
					nextB.setDisable(true);
				if (0==i)
					prevB.setDisable(true);
				// popup
				p = new PopWindow();
				p.getContent().setValue(content);
				syncC(isEdited, it -> p.isAutohide().setValue(!it)); // breaks editing >> p.setAutoHide(true);
				p.isEscapeHide().setValue(true);
				p.getOnHidden().add(runnable(() -> {
					if (isEdited.getValue()) cancelEdit();
					hover.playCloseDo(isNew ? runnable(() -> Seeker.this.getChildren().remove(this)) : null);
				}));
				p.getTitle().setValue(toHMSMs(c.getTime()));
				p.getHeaderIcons().setAll(prevB, nextB, editB, delB);
				content.addEventFilter(MOUSE_CLICKED, e -> {
					if (isEdited.getValue()) return;

					if (e.getClickCount()==1 && e.getButton()==PRIMARY && e.isStillSincePress() && p.isAutohide().getValue()) {
						delayerCloser.start(); // attempt to hide but only if click will not follow into double click
						e.consume();
					}
					if (e.getClickCount()==2) {
						can_hide = false;
						if (e.getButton()==SECONDARY) startEdit();
						if (e.getButton()==PRIMARY) seekTo();
						e.consume();
					}
				});
			}

			if (!p.isShowing()) {
				p.show(DOWN_CENTER.invoke(this));
				messageAnimation.play();
			}

			if (isNew) startEdit();
		}

		/** Starts editable mode. */
		public void startEdit() {
			if (isEdited.getValue()) return;

			// start edit
			isEdited.setValue(true);
			ta = new TextArea();

			// resize on text change
			syncC(ta.textProperty(), text -> {
				double w = SpitText.Companion.computeNaturalWrappingWidth(text, ta.getFont());
				ta.setPrefWidth(w);
				ta.setPrefHeight(0.8*w);
			});
			ta.setWrapText(true);
			ta.setText(message.getText());
			ta.setOnKeyPressed(e -> {
				if (e.getCode()==ENTER) {
					if (e.isShiftDown()) ta.insertText(ta.getCaretPosition(), "\n");
					else commitEdit();
					e.consume();
				} else if (e.getCode()==ESCAPE) {
					cancelEdit();
					e.consume();
				}
			});

			// validation
			var warnTooltip = appTooltip();
			var warnB = new Icon();
				warnB.size(11);
				warnB.styleclass(STYLECLASS_CONFIG_EDITOR_WARN_BUTTON);
				warnB.tooltip(warnTooltip);
			syncC(ta.textProperty(), text -> {
				var result = validateChapterText(text);
				warnB.setVisible(result.isError());
				commitB.setDisable(result.isError());
				warnTooltip.setText(getAny(result.map(t -> "")));
			});

			// maintain proper content
			var children = new ArrayList<>(content.getChildren());
			    children.remove(message);
			    children.add(layHeaderRight(5, CENTER, ta, warnB));
			content.getChildren().setAll(children);
			p.getHeaderIcons().setAll( commitB, cancelB);
		}

		/** Ends editable mode and applies changes. */
		public void commitEdit() {
			if (!c.getText().equals(ta.getText())) {
				// persist changes visually
				message.setText(ta.getText());
				// and physically
				c.setText(ta.getText());
				write(song, consumer(it -> it.addChapter(c, song)));
			}
			// maintain proper content
			content.getChildren().remove(ta.getParent());
			content.getChildren().add(message);
			p.getHeaderIcons().setAll( prevB, nextB, editB, delB);
			// stop edit
			isEdited.setValue(false);
			if (isNew) Seeker.this.getChildren().remove(this);
		}

		/** Ends editable mode and discards all changes. */
		public void cancelEdit() {
			if (isNew) {
				hidePopup();
				chapters.remove(this);
			} else {
				// maintain proper content
				content.getChildren().remove(ta.getParent());
				content.getChildren().add(message);
				p.getHeaderIcons().setAll(prevB, nextB, editB, delB);
			}
			// stop edit
			isEdited.setValue(false);
		}

		public void seekTo() {
			APP.audio.seek(c.getTime());
		}

		double getCenterX() {
			return getBoundsInParent().getMinX() + getBoundsInParent().getWidth()/2;
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