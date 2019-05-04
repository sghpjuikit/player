package sp.it.pl.gui.objects.icon;

import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import de.jensd.fx.glyphs.octicons.OctIcon;
import de.jensd.fx.glyphs.octicons.OctIconView;
import de.jensd.fx.glyphs.weathericons.WeatherIcon;
import de.jensd.fx.glyphs.weathericons.WeatherIconView;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.css.converter.EffectConverter;
import javafx.css.converter.PaintConverter;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.Effect;
import javafx.scene.input.MouseEvent;
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
import org.slf4j.LoggerFactory;
import sp.it.util.access.V;
import sp.it.util.access.ref.LazyR;
import sp.it.util.action.Action;
import sp.it.util.animation.Anim;
import sp.it.util.collections.mapset.MapSet;
import sp.it.util.dev.SwitchException;
import sp.it.util.functional.Functors.Ƒ1;
import sp.it.util.text.UtilKt;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ADJUST;
import static java.lang.Math.signum;
import static java.lang.Math.sqrt;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.util.Duration.millis;
import static sp.it.pl.main.AppBuildersKt.appTooltip;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.util.animation.Anim.mapTo01;
import static sp.it.util.functional.Util.setRO;
import static sp.it.util.functional.Util.stream;
import static sp.it.util.type.Util.getEnumConstants;
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
	private static final Ƒ1<Icon,Anim> Apress = i -> {
		double s = signum(i.node.getScaleX());
		return new Anim(millis(300), p -> setScaleXY(i.node, s*(1 - 0.3*p*p), 1 - 0.3*p*p));
	};
	private static final Ƒ1<Icon,Anim> Ahover = i -> {
		double s = signum(i.node.getScaleX());
		return new Anim(millis(150), p -> setScaleXY(i.node, s*(1 + 0.2*p*p), 1 + 0.2*p*p));
	};
	private static final String STYLECLASS = "icon";
	private static final GlyphIcons DEFAULT_GLYPH = ADJUST;
	private static final Double DEFAULT_ICON_SIZE = 12d;
	private static final Double DEFAULT_ICON_GAP = 0d;
	private static final String DEFAULT_FONT_SIZE = "1em";

	/** Collection of all glyphs types. */
	@SuppressWarnings("unchecked")
	public static Set<Class<GlyphIcons>> GLYPH_TYPES = (Set) setRO(
		FontAwesomeIcon.class,
		WeatherIcon.class,
		MaterialDesignIcon.class,
		MaterialIcon.class,
		OctIcon.class
	);

	/** Collection of all glyphs mapped to a unique names that identify them. */
	public static final MapSet<String,GlyphIcons> GLYPHS = stream(GLYPH_TYPES)
		.flatMap(c -> stream(getEnumConstants(c)))
		.filter(GlyphIcons.class::isInstance).map(GlyphIcons.class::cast)
		.collect(toCollection(() -> new MapSet<>(glyph -> glyph.getClass().getSimpleName() + "." + glyph.name())));

	// load fonts
	static {
		try {
			Font.loadFont(FontAwesomeIconView.class.getResource(FontAwesomeIconView.TTF_PATH).openStream(), 10.0);
			Font.loadFont(WeatherIconView.class.getResource(WeatherIconView.TTF_PATH).openStream(), 10.0);
			Font.loadFont(MaterialDesignIconView.class.getResource(MaterialDesignIconView.TTF_PATH).openStream(), 10.0);
			Font.loadFont(MaterialIconView.class.getResource(MaterialIconView.TTF_PATH).openStream(), 10.0);
			Font.loadFont(OctIconView.class.getResource(OctIconView.TTF_PATH).openStream(), 10.0);
		} catch (IOException e) {
			LoggerFactory.getLogger(Icon.class).error("Could not load font", e);
		}
	}

	private final Text node = new Text();
	public DoubleProperty glyphOffsetX = node.translateXProperty();
	public DoubleProperty glyphOffsetY = node.translateYProperty();
	private StringProperty glyphStyle = new SimpleStringProperty(""); // needed as setStyle() is final in javafx.scene.text.Text
	private final SimpleStyleableObjectProperty<String> icon = new SimpleStyleableObjectProperty<>(StyleableProperties.GLYPH_NAME, Icon.this, "glyphName", GLYPHS.keyMapper.invoke(ADJUST));
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
		this(i, size, null, (EventHandler<MouseEvent>) null);
	}

	public Icon(GlyphIcons i, double size, String tooltip) {
		this(i, size, tooltip, (EventHandler<MouseEvent>) null);
	}

	public Icon(GlyphIcons i, double size, String tooltip, EventHandler<MouseEvent> onClick) {
		setId("icon");
		this.size.addListener((o, ov, nv) -> updateSize());
		gap.addListener((o, ov, nv) -> updateSize());
		glyphStyle.addListener((o, ov, nv) -> updateStyle());
		icon.addListener((o, ov, nv) -> updateIcon());

		node.setId("icon-text");
		node.getStyleClass().clear();
		node.getStyleClass().add(STYLECLASS);
		styleclass(STYLECLASS);
		if (size!=-1) size(size);
		if (i!=null) icon(i);
		tooltip(tooltip);
		onClick(onClick);
		node.setCache(false);
		node.setSmooth(true);
		node.setFontSmoothingType(FontSmoothingType.GRAY);
		node.setMouseTransparent(true);
		node.setFocusTraversable(false);
		node.setManaged(false);
		getChildren().add(node);

		// mouse hover animation
		// unfortunately, when effects such as drop shadow are enabled, this hover does not work properly
		// hoverProperty().addListener((o, ov, nv) -> select(nv));
		addEventHandler(MouseEvent.MOUSE_EXITED, e -> select(false));
		addEventHandler(MouseEvent.MOUSE_ENTERED, e -> select(e.getX()>0 && e.getX()<getPrefWidth() && e.getY()>0 && e.getY()<getPrefHeight()));
		addEventHandler(MouseEvent.MOUSE_MOVED, e -> select(e.getX()>0 && e.getX()<getPrefWidth() && e.getY()>0 && e.getY()<getPrefHeight()));

		focusedProperty().addListener((o,ov,nv) -> {
			if (isAnimated.get()) ra.get(this, Ahover).playFromDir(nv);
		});
	}

	public Icon(GlyphIcons ico, double size, String tooltip, Runnable onClick) {
		this(ico, size, tooltip);
		onClick(onClick);
	}

	public Icon(GlyphIcons ico, double size, Action action) {
		this(ico, size, action.getInfo(), action);
	}

	@FXML
	public void init() {}

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

	// TODO: handle better along with focusing as well as subclasses and IconChooser grid
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
		setGlyphName(i==null ? "null" : GLYPHS.keyMapper.invoke(i));
		return this;
	}

	public Icon size(double s) {
//		isGlyphSizeSetProgrammatically = true;
		setGlyphSize(s);
		return this;
	}

	public Icon gap(double g) {
		isGlyphGapSetProgrammatically = true;
		setGlyphGap(g);
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

	public final Icon tooltip(String text) {
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

	public final Icon tooltip(Tooltip t) {
		Tooltip old = getTooltip();
		if (t!=null && (old!=t || old.getProperties().containsKey("was_setup"))) {
			t.setWrapText(true);
			t.setMaxWidth(330);
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

				Label s = getFieldValue(t.getSkin(), "tipLabel");
				Text txt = s==null ? null : getFieldValue(s.getSkin(), "text");
				Node ico = s==null ? null : getFieldValue(s.getSkin(), "graphic");
				if (ico!=null && txt!=null) {
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
	public final Icon styleclass(String s) {
		getStyleClass().add(s);
		updateIcon();
		updateSize();
		updateStyle();
		return this;
	}

	public final Icon embedded() {
		return styleclass("embedded-icon");
	}

	/**
	 * Installs on left mouse click behavior that consumes mouse event, using {@code setOnMouseClicked(action);}.
	 * <p/>
	 * When action is invoked the mouse event will be automatically consumed as well.
	 *
	 * @return this
	 */
	public final Icon onClick(EventHandler<MouseEvent> action) {
		setOnMouseClicked(action==null ? null : e -> {
			if (e.getButton()==PRIMARY) {
				action.handle(e);
				e.consume();
			}
		});

		return this;
	}

	/**
	 * Installs on left mouse click behavior that consumes mouse event, using {@code setOnMouseClicked(action);}.
	 * <p/>
	 * When action is invoked the mouse event will be automatically consumed as well.
	 * <p/>
	 * If action is {@link Action}, tooltip is set, with text set to {@link Action#getInfo()}.
	 *
	 * @param action to execute on left mouse click or null to set no action
	 * @return this
	 */
	public final Icon onClick(Runnable action) {
		if (click_runnable!=null && getTooltip()!=null) Tooltip.uninstall(this, getTooltip());

		if (action instanceof Action) {
			Action a = (Action) action;
			String title = a.getName();
			String body = a.getInfo();
			String keysRaw = UtilKt.getKeysPretty(a);
			String keys = keysRaw.isEmpty() ? keysRaw : " (" + keysRaw + ")";
			tooltip(title + keys + "\n\n" + body);
		}
		click_runnable = action;
		return onClick(action==null ? null : e -> {
			if (e.getButton()==PRIMARY) {
				action.run();
				e.consume();
			}
		});
	}

	/** Kotlin version of {@link #onClick(javafx.event.EventHandler)}. */
	public final Icon onClickDo(Function1<MouseEvent,Unit> action) {
		return onClick(action==null ? null : action::invoke);
	}

	@NotNull
	public Pane withText(String text) {
		return withText(text, Side.BOTTOM);
	}

	@NotNull
	public Pane withText(String text, Side side) {
		switch (side) {
			case LEFT: return layHeaderLeft(10, Pos.CENTER, this, new Label(text));
			case TOP: return layHeaderTop(5, Pos.CENTER, this, new Label(text));
			case RIGHT: return layHeaderRight(10, Pos.CENTER, this, new Label(text));
			case BOTTOM: return layHeaderBottom(5, Pos.CENTER, this, new Label(text));
			default: throw new SwitchException(side);
		}
	}

	GlyphIcons glyph = null;    // cache

	public GlyphIcons getGlyph() {
		String n = getGlyphName();
		if (glyph==null || !GLYPHS.keyMapper.invoke(glyph).equals(n))
			glyph = GLYPHS.getOr(n, DEFAULT_GLYPH);
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

	private StringProperty glyphStyleProperty() {
		if (glyphStyle==null) {
			glyphStyle = new SimpleStringProperty("");
		}
		return glyphStyle;
	}

	private String getGlyphStyle() {
		return glyphStyleProperty().getValue();
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
		// .replace("\'", "") is bug fix for some fonts having wrong font family or something
		// WeatherIcon & MaterialDesign
		Font f = new Font(i.getFontFamily().replace("\'", ""), node.getFont().getSize());
		node.setFont(f);
		node.setText(i.characterToString());
	}

	private void updateStyle() {
		setStyle(getGlyphStyle());
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
		return APP.converter.general.ofS(Double.class, sizeString).getOr(DEFAULT_ICON_SIZE);
	}

}