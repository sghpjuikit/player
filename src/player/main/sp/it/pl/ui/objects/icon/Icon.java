package sp.it.pl.ui.objects.icon;

import de.jensd.fx.glyphs.GlyphIcons;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.css.converter.EffectConverter;
import javafx.css.converter.PaintConverter;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.Effect;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.Text;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sp.it.util.access.V;
import sp.it.util.access.ref.LazyR;
import sp.it.util.action.Action;
import sp.it.util.animation.Anim;
import sp.it.util.dev.SwitchException;
import sp.it.util.functional.Functors.F1;
import sp.it.util.functional.TryKt;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ADJUST;
import static java.lang.Math.signum;
import static java.lang.Math.sqrt;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static javafx.scene.input.MouseEvent.MOUSE_MOVED;
import static javafx.util.Duration.millis;
import static kotlin.jvm.JvmClassMappingKt.getKotlinClass;
import static sp.it.pl.main.AppBuildersKt.appTooltip;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.util.animation.Anim.mapTo01;
import static sp.it.util.functional.TryKt.getOr;
import static sp.it.util.functional.Util.stream;
import static sp.it.util.text.StringExtensionsKt.keysUi;
import static sp.it.util.type.Util.getFieldValue;
import static sp.it.util.ui.Util.layHeaderBottom;
import static sp.it.util.ui.Util.layHeaderLeft;
import static sp.it.util.ui.Util.layHeaderRight;
import static sp.it.util.ui.Util.layHeaderTop;
import static sp.it.util.ui.UtilKt.createIcon;
import static sp.it.util.ui.UtilKt.pseudoclass;
import static sp.it.util.ui.UtilKt.setMinPrefMaxSize;
import static sp.it.util.ui.UtilKt.setScaleXY;
import static sp.it.util.ui.UtilKt.typeText;

/**
 * Icon.
 */
public class Icon extends StackPane {


	// animation builder, & reusable supplier
	private static final F1<Icon,Anim> Apress = i -> {
		double s = signum(i.node.getScaleX());
		return new Anim(millis(300), p -> setScaleXY(i.node, s*(1 - 0.3*sqrt(p)), 1 - 0.3*sqrt(p)));
	};
	private static final F1<Icon,Anim> Ahover = i -> {
		double s = signum(i.node.getScaleX());
		return new Anim(millis(200), p -> setScaleXY(i.node, s*(1 + 0.2*sqrt(p)), 1 + 0.2*sqrt(p)));
	};
	private static final String STYLECLASS = "icon";
	private static final GlyphIcons DEFAULT_GLYPH = ADJUST;
	private static final Double DEFAULT_ICON_SIZE = 12d;
	private static final Double DEFAULT_ICON_GAP = 0d;
	private static final String DEFAULT_FONT_SIZE = "1em";

	private final Text node = new Text();
	public DoubleProperty glyphOffsetX = node.translateXProperty();
	public DoubleProperty glyphOffsetY = node.translateYProperty();
	private final SimpleStyleableObjectProperty<String> icon = new SimpleStyleableObjectProperty<>(StyleableProperties.GLYPH_NAME, Icon.this, "glyphName", GlyphsKt.id(ADJUST));
	private boolean isGlyphSetProgrammatically = false;
	private final SimpleStyleableObjectProperty<Number> size = new SimpleStyleableObjectProperty<>(StyleableProperties.GLYPH_SIZE, Icon.this, "glyphSize", DEFAULT_ICON_SIZE);
	private boolean isGlyphSizeSetProgrammatically = false;
	private final SimpleStyleableObjectProperty<Number> gap = new SimpleStyleableObjectProperty<>(StyleableProperties.GLYPH_GAP, Icon.this, "glyphGap", DEFAULT_ICON_GAP);
	private boolean isGlyphGapSetProgrammatically = false;
	private double glyphScale = 1;

	public Icon() {
		this(null, -1);
	}

	public Icon(GlyphIcons i) {
		this(i, -1);
	}

	public Icon(GlyphIcons i, double size) {
		this(i, size, null, (Function1<Icon,Unit>) null);
	}

	public Icon(GlyphIcons i, double size, String tooltip) {
		this(i, size, tooltip, (Function1<Icon,Unit>) null);
	}

	public Icon(GlyphIcons i, double size, String tooltip, Function1<Icon,Unit> onClick) {
		setId("icon");
		this.size.addListener((o, ov, nv) -> updateSize());
		gap.addListener((o, ov, nv) -> updateSize());
		icon.addListener((o, ov, nv) -> updateIcon());

		node.setId("icon-text");
		node.getStyleClass().clear();
		node.getStyleClass().add(STYLECLASS);
		styleclass(STYLECLASS);
		if (size!=-1) size(size);
		if (i!=null) icon(i);
		tooltip(tooltip);
		onClickDo(onClick);
		node.setCache(false);
		node.setSmooth(true);
		node.setFontSmoothingType(FontSmoothingType.GRAY);
		node.setMouseTransparent(true);
		node.setFocusTraversable(false);
		node.setManaged(false);
		getChildren().add(node);
		setFocusTraversable(true);

		// mouse hover animation
		// unfortunately, when effects such as drop shadow are enabled, this hover does not work properly
		// hoverProperty().addListener((o, ov, nv) -> select(nv));
		addEventHandler(MOUSE_EXITED, e -> select(false));
		addEventHandler(MOUSE_ENTERED, e -> { if (iconBounds().contains(e.getX(), e.getY())) select(true); });
		addEventHandler(MOUSE_MOVED, e -> { if (iconBounds().contains(e.getX(), e.getY())) select(true); });

		focusedProperty().addListener((o,ov,nv) -> {
			if (isAnimated.get()) ra.get(this, Ahover).playFromDir(nv);
		});
	}

	private Bounds iconBounds() {
		return new BoundingBox((getLayoutBounds().getWidth()-getPrefWidth())/2.0, (getLayoutBounds().getHeight()-getPrefHeight())/2.0, 0.0, getPrefWidth(), getPrefHeight(), 0.0);
	}

	public Icon(GlyphIcons ico, double size, String tooltip, Runnable onClick) {
		this(ico, size, tooltip);
		action(onClick);
	}

	public Icon(GlyphIcons ico, double size, Action action) {
		this(ico, size, action.getInfo(), action);
	}

	@Override
	protected void layoutChildren() {
		super.layoutChildren();
		double w = getWidth(), h = getHeight();
		double gapH = (w - node.getLayoutBounds().getWidth())/2.0;
		double gapV = (h - node.getLayoutBounds().getHeight())/2.0;
		node.relocate(gapH, gapV);
	}

	private Runnable click_runnable;

	public Runnable getOnClickRunnable() {
		return click_runnable;
	}

	public Action getOnClickAction() {
		return click_runnable instanceof Action ? (Action) click_runnable : Action.NONE;
	}

	private final LazyR<Anim> ra = new LazyR<>(() -> Ahover.apply(this));
	private boolean isSelected = false;
	public final V<Boolean> isAnimated = new V<>(true);

	// TODO: handle better along with focusing
	public void select(boolean value) {
		if (value==isSelected) return;
		isSelected = value;
		pseudoClassStateChanged(pseudoclass("hover"), value);
		node.pseudoClassStateChanged(pseudoclass("hover"), value);
		if (isAnimated.get()) ra.get(this, Ahover).playFromDir(value);
	}

	public Icon icon(GlyphIcons i) {
		isGlyphSetProgrammatically |= i!=null;
		glyph = i;
		setGlyphName(i==null ? "null" : GlyphsKt.id(i));
		return this;
	}

	public Icon size(@Nullable Number s) {
//		isGlyphSizeSetProgrammatically = true;
		setGlyphSize(s==null ? DEFAULT_ICON_SIZE : s.doubleValue());
		return this;
	}

	public Icon gap(@Nullable Number s) {
		isGlyphGapSetProgrammatically = true;
		setGlyphGap(s==null ? DEFAULT_ICON_SIZE : s.doubleValue());
		return this;
	}

	public Icon scale(double s) {
		glyphScale = s;
		updateSize();
		return this;
	}

	public Icon blank() {
		setVisible(false);
		return this;
	}

	/** @return installed tooltip or null if none */
	public Tooltip getTooltip() {
		return (Tooltip) getProperties().get("javafx.scene.control.Tooltip");
	}

	public final @NotNull Icon tooltip(String text) {
		boolean willBeEmpty = text==null || text.isEmpty();
		if (!willBeEmpty) {
			Tooltip old = getTooltip();
			if (old==null) {
				tooltip(appTooltip(text));
			} else {
				old.setText(text);
				tooltip(old);
			}
		}
		return this;
	}

	public final @NotNull Icon tooltip(Tooltip t) {
		Tooltip old = getTooltip();
		if (t!=null && (old!=t || old.getProperties().containsKey("was_setup"))) {
			t.setWrapText(true);
			t.getScene().getRoot().setOpacity(0.0);

			// Can not set graphics normally, because:
			// 1) icon may be null at this point
			// 2) the icon could change / tooltip graphics would have to be maintained
			// 3) lazy graphics == better
			t.setOnShowing(e -> {
				var text = t.getText();

				GlyphIcons g = getGlyph();
				if (g!=null) {
					Node icon = createIcon(g, 24.0);
					setScaleXY(icon, 0.0);
					t.setGraphic(icon);
					t.setGraphicTextGap(15);
				}

				t.setText(text);
				t.getScene().getRoot().applyCss();
				t.setMaxWidth(sp.it.pl.ui.objects.Text.Companion.computeNaturalWrappingWidth(t.getText(), t.getFont()));

				Label s = getFieldValue(t.getSkin(), "tipLabel");
				Text txt = s==null ? null : getFieldValue(s.getSkin(), "text");
				Node ico = s==null ? null : getFieldValue(s.getSkin(), "graphic");
				if (ico!=null && txt!=null) {
					t.setContentDisplay(ContentDisplay.LEFT);
					s.setAlignment(Pos.TOP_CENTER);
					txt.setTextOrigin(VPos.TOP);
					var textInterpolator = typeText(text);
					var anim = new Anim(millis(400), p -> {
						txt.setText(textInterpolator.invoke(p));
						setScaleXY(ico, mapTo01(p, 0.4, 1));
					});
					t.getProperties().put("text_animation", anim);
					anim.applyAt(0.0);
					anim.playOpen();
				}

				t.getScene().getRoot().setOpacity(1.0);
			});
			t.setOnHiding(e -> {
				var anim = (Anim) t.getProperties().get("text_animation");
				if (anim!=null) anim.applyAt(1.0);
			});
			t.getProperties().put("was_setup", true);
			Tooltip.install(this, t);
		}
		return this;
	}

	/** Sets css style class. Returns this icon (fluent API). */
	// TODO: remove method
	public final @NotNull Icon styleclass(String s) {
		if (!getStyleClass().contains(s)) {
			getStyleClass().add(s);
			updateIcon();
			updateSize();
		}
		return this;
	}

	public final @NotNull Icon embedded() {
		return styleclass("embedded-icon");
	}


	/**
	 * Installs action for left mouse click and ENTER press. The events will be consumed.
	 * @return this
	 */
	public final @NotNull Icon action(@Nullable Runnable action) {
		if (click_runnable!=null && getTooltip()!=null) Tooltip.uninstall(this, getTooltip());

		if (action instanceof Action) {
			Action a = (Action) action;
			String title = a.getName();
			String body = a.getInfo();
			String keysRaw = keysUi(a);
			String keys = keysRaw.isEmpty() ? keysRaw : " (" + keysRaw + ")";
			tooltip(title + keys + "\n\n" + body);
		}
		click_runnable = action;
		return action(action==null ? null : i -> action.run());
	}

	/**
	 * Installs action for left mouse click and ENTER press. The events will be consumed.
	 * @return this
	 */
	public final @NotNull Icon action(@Nullable Consumer<? super Icon> action) {
		return onClickDo(action==null ? null : i -> { action.accept(i); return Unit.INSTANCE; });
	}

	/**
	 * Installs action for left mouse click or ENTER release.
	 * The events will be consumed.
	 * @return this
	 */
	public final @NotNull Icon onClickDo(@Nullable Function1<Icon,Unit> action) {
		return onClickDo(null, action);
	}

	/**
	 * Installs action for left mouse click with specified click count or ENTER release.
	 * The events will be consumed.
	 * @return this
	 */
	public final @NotNull Icon onClickDo(@Nullable Integer clickCount, @Nullable Function1<Icon,Unit> action) {
		setOnMouseClicked(action==null ? null : e -> {
			if (e.getButton()==PRIMARY && (clickCount==null || e.getClickCount()==clickCount)) {
				action.invoke(this);
				e.consume();
			}
		});
		setOnKeyReleased(action==null ? null : e -> {
			if (e.getCode()==ENTER) {
				action.invoke(this);
				e.consume();
			}
		});
		return this;
	}

	@NotNull
	public Pane withText(String text) {
		return withText(Side.BOTTOM, text);
	}

	@NotNull
	public Pane withText(Side side, String text) {
		return withText(side, Pos.CENTER, text);
	}

	@NotNull
	public Pane withText(Side side, Pos alignment, String text) {
		switch (side) {
			case TOP: return layHeaderBottom(5, alignment, new Label(text), this);
			case RIGHT: return layHeaderLeft(10, alignment, this, new Label(text));
			case BOTTOM: return layHeaderTop(5, alignment, this, new Label(text));
			case LEFT: return layHeaderRight(10, alignment, new Label(text), this);
			default: throw new SwitchException(side);
		}
	}

	private GlyphIcons glyph = null;    // cache

	public GlyphIcons getGlyph() {
		String n = getGlyphName();
		if (glyph==null || !GlyphsKt.id(glyph).equals(n))
			glyph = TryKt.getOr(Glyphs.INSTANCE.get(n), DEFAULT_GLYPH);
		return glyph;
	}

	public void setFill(Paint value) {
		node.setFill(value);
	}

	private Paint getFill() {
		return node.getFill();
	}

	private ObjectProperty<Paint> fillProperty() {
		return node.fillProperty();
	}

	private String getGlyphName() { return icon.getValue(); }

	private void setGlyphName(String glyphName) { icon.setValue(glyphName); }

	private ObjectProperty<Number> glyphGapProperty() {
		return gap;
	}

	private void setGlyphGap(Number gap) {
		gap = (gap==null) ? DEFAULT_ICON_GAP : gap;
		glyphGapProperty().setValue(gap);
	}

	private void setGlyphSize(Number size) {
		Number sn = (size==null) ? DEFAULT_ICON_SIZE : size;
//		setStyle("-glyph-size: " + sn.doubleValue()/12.0 + "em;");
		setStyle("-glyph-size: " + sqrt(sn.doubleValue()/12.0) + "em;");
		// glyphSizeProperty().setValue(s);
	}

	private void updateSize() {
		double glyphSize = size.getValue().doubleValue();
		Font f = new Font(node.getFont().getFamily(), glyphScale*glyphSize);
		node.setFont(f);

		setMinPrefMaxSize(this, 1.3*glyphSize + gap.getValue().doubleValue());
	}

	private void updateIcon() {
		GlyphIcons i = getGlyph();
		// TODO: investigate
		// .replace("\'", "") is bug fix for some fonts having wrong font family or something
		// WeatherIcon & MaterialDesign

		Font f = new Font(i.getFontFamily().replace("'", ""), node.getFont().getSize());
		node.setFont(f);
		node.setText(i.characterToString());
	}

	@SuppressWarnings("unchecked")
	private interface StyleableProperties {

		/**
		 * Css -fx-fill: <a href="../doc-files/cssref.html#typepaint">&lt;paint&gt;</a>
		 *
		 * @see javafx.scene.shape.Shape#fillProperty()
		 */
		CssMetaData<Icon,Paint> FILL = new CssMetaData<>("-fx-fill", PaintConverter.getInstance(), Color.BLACK) {

			@Override
			public boolean isSettable(Icon node) {
				return node.fillProperty()!=null || !node.fillProperty().isBound();
			}

			@Override
			public StyleableProperty<Paint> getStyleableProperty(Icon node) {
				return (StyleableProperty<Paint>) node.fillProperty();
			}

			@Override
			public Paint getInitialValue(Icon node) {
				return Color.BLACK;
			}

		};

		CssMetaData<Icon,Effect> EFFECT = new CssMetaData<>("-fx-effect", EffectConverter.getInstance()) {

			@Override
			public boolean isSettable(Icon node) {
				return node.node.effectProperty()==null || !node.node.effectProperty().isBound();
			}

			@Override
			public StyleableProperty<Effect> getStyleableProperty(Icon node) {
				return (StyleableProperty<Effect>) node.effectProperty();
			}
		};

		CssMetaData<Icon,String> GLYPH_NAME = new CssMetaData<>("-glyph-name", StyleConverter.getStringConverter(), "BLANK") {

			@Override
			public boolean isSettable(Icon styleable) {
				return !styleable.isGlyphSetProgrammatically && (styleable.icon==null || !styleable.icon.isBound());
			}

			@Override
			public StyleableProperty<String> getStyleableProperty(Icon styleable) {
				return styleable.icon;
			}

			@Override
			public String getInitialValue(Icon styleable) {
				return "BLANK";
			}
		};

		CssMetaData<Icon,Number> GLYPH_SIZE = new CssMetaData<>("-glyph-size", StyleConverter.getSizeConverter(), DEFAULT_ICON_SIZE) {
			@Override
			public boolean isSettable(Icon styleable) {
				return !styleable.isGlyphSizeSetProgrammatically && (styleable.size==null || !styleable.size.isBound());
			}

			@Override
			public StyleableProperty<Number> getStyleableProperty(Icon styleable) {
				return styleable.size;
			}

			@Override
			public Number getInitialValue(Icon styleable) {
				return DEFAULT_ICON_SIZE;
			}
		};

		CssMetaData<Icon,Number> GLYPH_GAP = new CssMetaData<>("-glyph-gap", StyleConverter.getSizeConverter(), DEFAULT_ICON_GAP) {
			@Override
			public boolean isSettable(Icon styleable) {
				return !styleable.isGlyphGapSetProgrammatically && (styleable.gap==null || !styleable.gap.isBound());
			}

			@Override
			public StyleableProperty<Number> getStyleableProperty(Icon styleable) {
				return styleable.gap;
			}

			@Override
			public Number getInitialValue(Icon styleable) {
				return DEFAULT_ICON_GAP;
			}
		};

		List<CssMetaData<? extends Styleable,?>> STYLEABLES = stream(
			stream(FILL, GLYPH_NAME, GLYPH_SIZE, GLYPH_GAP),
			stream(StackPane.getClassCssMetaData()).filter(a -> !"-fx-effect".equals(a.getProperty())) // we use our own effect
		)
			.collect(collectingAndThen(toList(), Collections::unmodifiableList));
	}

	public static List<CssMetaData<? extends Styleable,?>> getClassCssMetaData() {
		return StyleableProperties.STYLEABLES;
	}

	@Override
	public List<CssMetaData<? extends Styleable,?>> getCssMetaData() {
		return getClassCssMetaData();
	}

	public Number convert(String sizeString) {
		return getOr(APP.getConverter().general.ofS(getKotlinClass(Double.class), sizeString), DEFAULT_ICON_SIZE);
	}

}