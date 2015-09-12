/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.icon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.css.*;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import org.slf4j.LoggerFactory;

import com.sun.javafx.css.ParsedValueImpl;
import com.sun.javafx.css.parser.CSSParser;

import action.Action;
import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import de.jensd.fx.glyphs.weathericons.WeatherIcon;
import de.jensd.fx.glyphs.weathericons.WeatherIconView;
import gui.objects.PopOver.PopOver;
import main.App;
import util.animation.Anim;
import util.functional.Functors.Ƒ1;
import util.graphics.Icons;
import util.Ɽ;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ADJUST;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.INFO;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static javafx.scene.input.MouseEvent.MOUSE_RELEASED;
import static javafx.scene.text.TextAlignment.JUSTIFY;
import static javafx.util.Duration.millis;
import static util.Util.getEnumConstants;
import static util.Util.getFieldValue;
import static util.functional.Util.stream;
import static util.graphics.Util.setScaleXY;

public class Icon<I extends Icon> extends Text {

    // animation builder, & reusable supplier
    private static final Ƒ1<Icon,Anim> A = i -> new Anim(millis(400), p -> setScaleXY(i,1-0.3*p*p*p));
    private static final Double DEFAULT_ICON_SIZE = 12.0;
    private static final String DEFAULT_FONT_SIZE = "1em";

    static {
        try {
            Font.loadFont(Icon.class.getResource(FontAwesomeIconView.TTF_PATH).openStream(), 10.0);
            Font.loadFont(Icon.class.getResource(WeatherIconView.TTF_PATH).openStream(), 10.0);
            Font.loadFont(Icon.class.getResource(MaterialDesignIconView.TTF_PATH).openStream(), 10.0);
            Font.loadFont(Icon.class.getResource(MaterialIconView.TTF_PATH).openStream(), 10.0);
        } catch (IOException e) {
            LoggerFactory.getLogger(Icon.class).error("Couldnt load font",e);
        }
    }

    public static Icon createInfoIcon(String text) {
        return new Icon(INFO, 13, "Help", e -> {
	    PopOver<gui.objects.Text> helpP = PopOver.createHelpPopOver(text);
	    helpP.show((Node) e.getSource());
	    helpP.getContentNode().setWrappingWidth(400);
	    App.actionStream.push("Info popup");
	});
    }


    private StringProperty glyphStyle; // needed as setStyle() is final in javafx.scene.text.Text
    private final ObjectProperty<String> icon = new SimpleStyleableObjectProperty<>(StyleableProperties.GLYPH_NAME, Icon.this, "glyphName", ADJUST.name());
    private final ObjectProperty<Number> size = new SimpleStyleableObjectProperty<>(StyleableProperties.GLYPH_SIZE, Icon.this, "glyphSize", 12);


    public Icon() {
        this(null,-1);
    }

    public Icon(GlyphIcons i) {
        this(i, -1);
    }

    public Icon(GlyphIcons i, double size) {
        this(i, size, null, (EventHandler)null);
    }

    public Icon(GlyphIcons i, double size, String tooltip) {
        this(i, size, tooltip, (EventHandler)null);
    }

    public Icon(GlyphIcons i, double size, String tooltip, EventHandler<MouseEvent> onClick) {
        glyphSizeProperty().addListener((o,ov,nv) -> {
            updateSize();
        });
        glyphStyleProperty().addListener((o,ov,nv) -> {
            updateStyle();
        });
        glyphNameProperty().addListener((o,ov,nv) -> {
            updateIcon();
        });

//        setFont(new Font("FontAwesome", DEFAULT_ICON_SIZE));

        getStyleClass().clear();
        styleclass("icon");
        if(size!=-1) size(size);
        if(i!=null) icon(i);
        tooltip(tooltip);
        onClick(onClick);
        setCache(true);

        // install click animation
        Ɽ<Anim> ra = new Ɽ<>(); // lazy singleton
        addEventFilter(MOUSE_PRESSED, e -> ra.get(this,A).playOpenDo(null));
        addEventFilter(MOUSE_RELEASED, e -> ra.get(this,A).playOpenDoClose(null));
    }

    public Icon(GlyphIcons ico, double size, String tooltip, Runnable onClick) {
        this(ico, size, tooltip);
        onClick(onClick);
    }

    public Icon(GlyphIcons ico, double size, Action action) {
        this(ico, size, action.getInfo(), (Runnable)action);
    }

    @FXML
    public void init() {}

/******************************************************************************/

    private Runnable click_runnable;

    public Runnable getOnClickRunnable() {
        return click_runnable;
    }

    public Action getOnClickAction() {
        return click_runnable instanceof Action ? (Action) click_runnable : Action.EMPTY;
    }

/********************************* FLUENT API *********************************/

    public I icon(GlyphIcons i) {
        glyph = i;
        setIcon(i);
        return (I)this;
    }

    public I size(double s) {
        setGlyphSize(s);
        return (I)this;
    }

    public final I tooltip(String text) {
        if(text!=null && !text.isEmpty()) return tooltip(new Tooltip(text));
        return (I)this;
    }

    public final I tooltip(Tooltip t) {
        if(t!=null) {
            t.setWrapText(true);
            t.setMaxWidth(330);
            t.setTextAlignment(JUSTIFY);
            t.setOnShowing(e -> {
                // we can not set graphics normally, because some icons may not have the glyph ready
                // at this point, we do that when tooltip is being called on, this also avoids creating
                // useless objects
                GlyphIcons g = getGlyph();
                if(g!=null) {
                    t.setGraphic(Icons.createIcon(g, 30));
                    t.setGraphicTextGap(15);
                }

            });
            t.setOnShown(e -> {
                // animate
                Label s = getFieldValue(t.getSkin(), Label.class, "tipLabel");
                Text txt = s==null ? null : getFieldValue(s.getSkin(), Text.class, "text");
                Node ico = s==null ? null : getFieldValue(s.getSkin(), Node.class, "graphic");
                if(ico!=null && txt!=null) {
                    new Anim(millis(400), p -> {
                        double p2 = Anim.mapTo01(p, 0.4, 1);
                        txt.setTranslateX(20*p*p-20);
                        setScaleXY(ico, p2);
                    }).play();
                }
            });
            Tooltip.install(this, t);
        }
        return (I)this;
    }

    /** Sets styleclass. Returns this icon (fluent API). */
    public final I styleclass(String s) {
        getStyleClass().add(s);
        updateIcon();
        updateSize();
        updateStyle();
        return (I)this;
    }

    public final I embedded() {
        return styleclass("embedded-icon");
    }

    /** Equivalent to {@code setOnMouseClicked(action);}. Returns this icon (fluent API). */
    public final I onClick(EventHandler<MouseEvent> action) {
        setOnMouseClicked(action);
        return (I)this;
    }

    /**
     * Creates and sets onMouseClick handler. Can also set tooltip. Returns this icon (fluent API).
     * <p>
     * The handler executes the action only on left button click
     * THe handler can be retrieved or removed using {@link #getOnMouseClicked()} and
     * {@link #setOnMouseClicked(javafx.event.EventHandler)}.
     *
     * @param action Action to execute on left mouse click. If instance of {@link Action} tooltip is
     * set, with text set to {@link Action#getInfo()}. Null removes mouse click handler (but not the
     * tooltip).
     * @return this.
     */
    public final I onClick(Runnable action) {
        if(action instanceof Action) {
            Action a = (Action)action;
            tooltip(a.getName()+"\n\n" + a.getInfo());
        }
        click_runnable = action;
        return onClick(action==null ? null : e -> {
            if(e.getButton()==PRIMARY) {
                action.run();
                e.consume();
            }
        });
    }

/******************************************************************************/


    GlyphIcons glyph = null;    // cache
    // the problem is the name is not necessarily unique across different fonts
    // however the cache already guarantees correct icon when loading programmatically
    // css however remains a problem, should be reimplemented
    public GlyphIcons getGlyph() {
        String n = getGlyphName();
        if(glyph==null || !glyph.name().equalsIgnoreCase(n)) {
            glyph = (GlyphIcons) stream(FontAwesomeIcon.class,WeatherIcon.class,MaterialDesignIcon.class,MaterialIcon.class)
                .flatMap(c -> stream(getEnumConstants(c)))
                .filter(i -> ((GlyphIcons)i).name().equalsIgnoreCase(n))
                .findFirst().orElseGet(this::getDefaultGlyph);
        }
        return glyph;
    }




    public final StringProperty glyphStyleProperty() {
        if (glyphStyle == null) {
            glyphStyle = new SimpleStringProperty("");
        }
        return glyphStyle;
    }

    public final String getGlyphStyle() {
        return glyphStyleProperty().getValue();
    }

    public final void setGlyphStyle(String style) {
        glyphStyleProperty().setValue(style);
    }

    public final ObjectProperty<String> glyphNameProperty() { return icon; }

    public final String getGlyphName() { return icon.getValue(); }

    public final void setGlyphName(String glyphName) { icon.setValue(glyphName); }

    public final ObjectProperty<Number> glyphSizeProperty() {
        return size;
    }

    public final Number getGlyphSize() {
        return glyphSizeProperty().getValue();
    }

    public final void setGlyphSize(Number size) {
        size = (size == null) ? DEFAULT_ICON_SIZE : size;
        glyphSizeProperty().setValue(size);
    }

    // kept for compability reasons and for SceneBuilder/FXML support
    public final String getSize() {
        return getGlyphSize().toString();
    }

    // kept for compability reasons and for SceneBuilder/FXML support
    public final void setSize(String sizeExpr) {
        Number s = convert(sizeExpr);
        setGlyphSize(s);
    }

    public final void setIcon(GlyphIcons i) {
        setGlyphName(i.name());
    }

    public FontAwesomeIcon getDefaultGlyph(){ return ADJUST; };

    private void updateSize() {
        Font f = new Font(getFont().getFamily(), getGlyphSize().doubleValue());
        setFont(f);
    }

    private void updateIcon() {
        GlyphIcons i = getGlyph();
        Font f = new Font(i.getFontFamily(), getFont().getSize());
        setFont(f);
        setText(i.characterToString());
    }

    private void updateStyle() {
        setStyle(getGlyphStyle());
    }

    // CSS
    private static class StyleableProperties {

        private static final CssMetaData<Icon, String> GLYPH_NAME
                = new CssMetaData<Icon, String>("-glyph-name", StyleConverter.getStringConverter(), "BLANK") {

                    @Override
                    public boolean isSettable(Icon styleable) {
                        return styleable.icon == null || !styleable.icon.isBound();
                    }

                    @Override
                    public StyleableProperty<String> getStyleableProperty(Icon styleable) {
                        return (StyleableProperty) styleable.glyphNameProperty();
                    }

                    @Override
                    public String getInitialValue(Icon styleable) {
                        return "BLANK";
                    }
                };

        private static final CssMetaData<Icon, Number> GLYPH_SIZE
                = new CssMetaData<Icon, Number>("-glyph-size", StyleConverter.getSizeConverter(), DEFAULT_ICON_SIZE) {
                    @Override
                    public boolean isSettable(Icon styleable) {
                        return styleable.size == null || !styleable.size.isBound();
                    }

                    @Override
                    public StyleableProperty<Number> getStyleableProperty(Icon styleable) {
                        return (StyleableProperty) styleable.glyphSizeProperty();
                    }

                    @Override
                    public Number getInitialValue(Icon styleable) {
                        return DEFAULT_ICON_SIZE;
                    }
                };
        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;

        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<>(Text.getClassCssMetaData());
            Collections.addAll(styleables, GLYPH_NAME, GLYPH_SIZE);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    private static final CSSParser CSS_PARSER = CSSParser.getInstance();

    public Number convert(String sizeString) {
        ParsedValueImpl parsedValueImpl = CSS_PARSER.parseExpr("", sizeString);
        return (Number) parsedValueImpl.convert(getFont());
    }

}