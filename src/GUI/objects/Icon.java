/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects;

import de.jensd.fx.glyphs.GlyphIconName;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
import java.util.List;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableValue;
import javafx.css.*;
import javafx.event.EventHandler;
import static javafx.scene.control.ContentDisplay.CENTER;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.TextBoundsType;

public class Icon extends Label {
    
    private static final StyleablePropertyFactory<Icon> FACTORY = new StyleablePropertyFactory(Label.getClassCssMetaData());
    private static final CssMetaData<Icon, FontAwesomeIconName> ICON_CMD = FACTORY.createEnumCssMetaData(FontAwesomeIconName.class, "icon", i -> i.icon);
    
    public final StyleableProperty<FontAwesomeIconName> icon = new SimpleStyleableObjectProperty<FontAwesomeIconName>(ICON_CMD, this, "icon") {

        @Override
        protected void invalidated() {
            super.invalidated();
            Text t = (Text)getGraphic();
            t.setText(get().characterToString());
            t.setStyle(String.format("-fx-font-family: %s; -fx-font-size: %s;",get().getFontFamily(), icon_size.get()));
        }
        
//        public void set(FontAwesomeIconName v) {
//            super.set(v);
//        }
    };
    
     public ObservableValue<GlyphIconName> iconProperty() { return ( ObservableValue<GlyphIconName>)icon; }
     public final GlyphIconName getIcon() { return icon.getValue(); }
     public final void setIcon(FontAwesomeIconName i) { icon.setValue(i); }
    
    
    public final IntegerProperty icon_size = new SimpleIntegerProperty() {
        public void set(int nv) {
            super.set(nv);
//            t.setText(icon.getValue().characterToString());
//            t.setStyle(String.format("-fx-font-family: %s; -fx-font-size: %s;",icon.getValue().getFontFamily(), get()));
//            t.getStyleClass().add("glyph");
            
        }
    };

    public Icon() {
        this(null,12);
    }
    public Icon(FontAwesomeIconName i) {
        this(i, 12);
    }
    public Icon(FontAwesomeIconName i, int size) {
        this(i, size, null, (EventHandler)null);
    }
    public Icon(FontAwesomeIconName i, int size, String tooltip) {
        this(i, size, tooltip, (EventHandler)null);
    }
    public Icon(FontAwesomeIconName ico, int size, String tooltip, EventHandler<MouseEvent> onClick) {
        setGraphic(new Text());
        getGraphic().getStyleClass().add("glyph");
        ((Text)getGraphic()).setBoundsType(TextBoundsType.LOGICAL);
        icon_size.set(size);
//        if(ico!=null) icon.applyStyle(null, ico);
        if(ico!=null) icon.setValue(ico);
        setTooltip(tooltip);
        if(onClick!=null) setOnMouseClicked(onClick);
        getStyleClass().add("icon");
        
        setContentDisplay(CENTER);
        setPickOnBounds(true);
        getGraphic().setPickOnBounds(true);
    }

    
    public Icon(FontAwesomeIconName ico, int size, String tooltip, Runnable onClick) {
        this(ico, size, tooltip, onClick==null ? null : e -> { onClick.run(); e.consume(); });
    }
    
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return FACTORY.getCssMetaData();
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return FACTORY.getCssMetaData();
    }
    
//    @Override
//    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
//        return FACTORY.getCssMetaData();
//    }
    
//    public final void setTooltip(Tooltip tooltip) {
//        if(tooltip!=null)
//            Tooltip.install(this, tooltip);
//    }
    
    public final void setTooltip(String text) {
        if(text!=null && !text.isEmpty())
            Tooltip.install(this, new Tooltip(text));
    }
}
//public class Icon extends Text {
//    
//    private static final StyleablePropertyFactory<Icon> FACTORY = new StyleablePropertyFactory(Text.getClassCssMetaData());
//    private static final CssMetaData<Icon, FontAwesomeIconName> ICON_CMD = FACTORY.createEnumCssMetaData(FontAwesomeIconName.class, "icon", i -> i.icon);
//    
//    public final StyleableProperty<FontAwesomeIconName> icon = new SimpleStyleableObjectProperty<FontAwesomeIconName>(ICON_CMD, this, "icon") {
//
//        @Override
//        protected void invalidated() {
//            super.invalidated();
//            setText(get().characterToString());
//            setStyle(String.format("-fx-font-family: %s; -fx-font-size: %s;",get().getFontFamily(), icon_size.get()));
//            getStyleClass().add("glyph");
//        }
//        
////        public void set(FontAwesomeIconName v) {
////            super.set(v);
////        }
//    };
//    
//     public ObservableValue<GlyphIconName> iconProperty() { return ( ObservableValue<GlyphIconName>)icon; }
//     public final GlyphIconName getIcon() { return icon.getValue(); }
//     public final void setIcon(FontAwesomeIconName isSelected) { icon.setValue(isSelected); }
//    
//    
//    public final IntegerProperty icon_size = new SimpleIntegerProperty() {
//        public void set(int nv) {
//            super.set(nv);
////            t.setText(icon.getValue().characterToString());
////            t.setStyle(String.format("-fx-font-family: %s; -fx-font-size: %s;",icon.getValue().getFontFamily(), get()));
////            t.getStyleClass().add("glyph");
//            
//        }
//    };
//
//    public Icon() {
//        this(null,12);
//    }
//    public Icon(FontAwesomeIconName i) {
//        this(i, 12);
//    }
//    public Icon(FontAwesomeIconName i, int size) {
//        this(i, size, null, (EventHandler)null);
//    }
//    public Icon(FontAwesomeIconName i, int size, String tooltip) {
//        this(i, size, tooltip, (EventHandler)null);
//    }
//    public Icon(FontAwesomeIconName ico, int size, String tooltip, EventHandler<MouseEvent> onClick) {
//        icon_size.set(size);
////        if(ico!=null) icon.applyStyle(null, ico);
//        if(ico!=null) icon.setValue(ico);
//        setTooltip(tooltip);
//        if(onClick!=null) setOnMouseClicked(onClick);
//        getStyleClass().add("icon");
//    }
//
//    
//    public Icon(FontAwesomeIconName ico, int size, String tooltip, Runnable onClick) {
//        this(ico, size, tooltip, onClick==null ? null : e -> { onClick.run(); e.consume(); });
//    }
//    
//    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
//        return FACTORY.getCssMetaData();
//    }
//
//    @Override
//    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
//        return FACTORY.getCssMetaData();
//    }
//    
//    public final void setTooltip(Tooltip tooltip) {
//        if(tooltip!=null)
//            Tooltip.install(this, tooltip);
//    }
//    
//    public final void setTooltip(String text) {
//        if(text!=null && !text.isEmpty())
//            Tooltip.install(this, new Tooltip(text));
//    }
//}















///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package GUI.objects;
//
//import de.jensd.fx.glyphs.GlyphIconName;
//import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
//import java.util.List;
//import javafx.beans.property.IntegerProperty;
//import javafx.beans.property.SimpleIntegerProperty;
//import javafx.beans.value.ObservableValue;
//import javafx.css.*;
//import javafx.event.EventHandler;
//import javafx.scene.control.ContentDisplay;
//import javafx.scene.control.Label;
//import javafx.scene.control.Tooltip;
//import javafx.scene.input.MouseEvent;
//import util.graphics.Icons;
//
///**
// <p>
// @author Plutonium_
// */
//public class Icon extends Label {
//    
//    private static final StyleablePropertyFactory<Icon> FACTORY = new StyleablePropertyFactory(Label.getClassCssMetaData());
//    private static final CssMetaData<Icon, FontAwesomeIconName> ICON_CMD = FACTORY.createEnumCssMetaData(FontAwesomeIconName.class, "icon", i -> i.icon);
//    
//    
//    
//    public final StyleableProperty<FontAwesomeIconName> icon = new SimpleStyleableObjectProperty<FontAwesomeIconName>(ICON_CMD, this, "icon") {
//
//        @Override
//        protected void invalidated() {
//            super.invalidated(); //To change body of generated methods, choose Tools | Templates.
//            Icons.setIcon(Icon.this, get(), String.valueOf(icon_size.get()));
//        }
//        
////        public void set(FontAwesomeIconName v) {
////            super.set(v);
////        }
//    };
//    
//     public ObservableValue<GlyphIconName> iconProperty() { return ( ObservableValue<GlyphIconName>)icon; }
//     public final GlyphIconName getIcon() { return icon.getValue(); }
//     public final void setIcon(FontAwesomeIconName isSelected) { icon.setValue(isSelected); }
//    
//    
//    public final IntegerProperty icon_size = new SimpleIntegerProperty() {
//        public void set(int nv) {
//            super.set(nv);
//            setMinSize(nv, nv);
//            setPrefSize(nv, nv);
//            setMaxSize(nv, nv);
//        }
//    };
//
//    public Icon() {
//        this(null,12);
//    }
//    public Icon(FontAwesomeIconName i) {
//        this(i, 12);
//    }
//    public Icon(FontAwesomeIconName i, int size) {
//        this(i, size, null, (EventHandler)null);
//    }
//    public Icon(FontAwesomeIconName i, int size, String tooltip) {
//        this(i, size, tooltip, (EventHandler)null);
//    }
//    public Icon(FontAwesomeIconName ico, int size, String tooltip, EventHandler<MouseEvent> onClick) {
//        setContentDisplay(ContentDisplay.CENTER);
//        icon_size.set(size);
////        if(ico!=null) icon.applyStyle(null, ico);
//        if(ico!=null) icon.setValue(ico);
//        if(tooltip!=null && !tooltip.isEmpty()) setTooltip(new Tooltip(tooltip));
//        if(onClick!=null) setOnMouseClicked(onClick);
//        getStyleClass().add("icon");
//    }
//    public Icon(FontAwesomeIconName ico, int size, String tooltip, Runnable onClick) {
//        this(ico, size, tooltip, onClick==null ? null : e -> { onClick.run(); e.consume(); });
//    }
//    
//    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
//        return FACTORY.getCssMetaData();
//    }
//
//     @Override
//    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
//        return FACTORY.getCssMetaData();
//    }
//     
//}