/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.icon;

import action.Action;
import com.sun.javafx.css.ParsedValueImpl;
import com.sun.javafx.css.parser.CSSParser;
import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ADJUST;
import gui.objects.Text;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.css.*;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Tooltip;
import static javafx.scene.input.MouseButton.PRIMARY;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;
import static javafx.scene.text.TextAlignment.JUSTIFY;











public class Icon<I extends Icon> extends Text {
    
        public final static String TTF_PATH = "/de/jensd/fx/glyphs/fontawesome/fontawesome-webfont.ttf";

    static {
        Font.loadFont(Icon.class.getResource(TTF_PATH).toExternalForm(), 10.0);
    }

    public final static Double DEFAULT_ICON_SIZE = 12.0;
    public final static String DEFAULT_FONT_SIZE = "1em";

    private StringProperty glyphStyle; // needed as setStyle() is final in javafx.scene.text.Text 
    private final ObjectProperty<String> icon = new SimpleStyleableObjectProperty<>(StyleableProperties.GLYPH_NAME, Icon.this, "glyphName", ADJUST.name());
    private final ObjectProperty<Number> size = new SimpleStyleableObjectProperty<>(StyleableProperties.GLYPH_SIZE, Icon.this, "glyphSize", 12);
    private final Class typeOfT = FontAwesomeIcon.class;


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
//        ((Text)getGraphic()).setBoundsType(TextBoundsType.VISUAL);
        if(size!=-1) size(size);
        if(i!=null) icon(i);
        tooltip(tooltip);
        onClick(onClick);
        
        setCache(true);
    }

    
    public Icon(GlyphIcons ico, double size, String tooltip, Runnable onClick) {
        this(ico, size, tooltip);
        onClick(onClick);
    }
    public Icon(GlyphIcons ico, double size, Action action) {
        this(ico, size, action.getInfo(), (Runnable)action);
    }
    

    @FXML
    public void init() {
    }

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
            t.setMaxWidth(300);
            t.setTextAlignment(JUSTIFY);
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
    
    
    public final I onClick(EventHandler<MouseEvent> action) {
        if(action!=null) setOnMouseClicked(action);
        return (I)this;
    }
    
    public final I onClick(Runnable action) {
        if(action instanceof Action) tooltip(((Action)action).getInfo());
        click_runnable = action;
        return onClick(action==null ? null : e -> { 
            if(e.getButton()==PRIMARY) {
                action.run();
                e.consume(); 
            } 
        });
    }
    
/******************************************************************************/

    
    
    public FontAwesomeIcon getIco() {
        return FontAwesomeIcon.valueOf(getGlyphName());
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

    public final void setIcon(GlyphIcons glyph) {
        setGlyphName(glyph.name());
    }

    public FontAwesomeIcon getDefaultGlyph(){ return ADJUST; };

    private void updateSize() {
        Font f = new Font(getFont().getFamily(), getGlyphSize().doubleValue());
        setFont(f);
    }

    private void updateIcon() {
        GlyphIcons i = getDefaultGlyph();
        try {
            i = ((GlyphIcons) Enum.valueOf(typeOfT, getGlyphName()));
        } catch (Exception e) {}
        
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