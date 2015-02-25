/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.graphics;

import de.jensd.fx.glyphs.GlyphIcon;
import de.jensd.fx.glyphs.GlyphIconName;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.weathericons.WeatherIcon;
import javafx.scene.control.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 <p>
 @author Plutonium_
 */
public class Icons {
    
    static {
        Font.loadFont(GlyphsDude.class.getResource(FontAwesomeIcon.TTF_PATH).toExternalForm(), 10.0);
        Font.loadFont(GlyphsDude.class.getResource(WeatherIcon.TTF_PATH).toExternalForm(), 10.0);
    }

    
    public static Text createIcon(GlyphIconName icon) {
        return createIcon(icon, GlyphIcon.DEFAULT_ICON_SIZE);
    }
    public static Text createIcon(GlyphIconName icon, int icon_size) {
        return createIcon(icon, String.valueOf(icon_size));
    }
    private static Text createIcon(GlyphIconName icon, String iconSize) {
        Text t = new Text(icon.characterToString());
        t.setStyle(String.format("-fx-font-family: %s; -fx-font-size: %s;",icon.getFontFamily(), iconSize));
        t.getStyleClass().add("glyph");
        t.setPickOnBounds(true);
        return t;
    }
    public static Text createIcon(GlyphIconName icon, int icons, int size) {
        String s = icon.characterToString();
        StringBuilder sb = new StringBuilder(icons);
        for(int i=0; i<icons; i++) sb.append(s);
        
        Text t = new Text(sb.toString());
        t.setStyle(String.format("-fx-font-family: %s; -fx-font-size: %s;",icon.getFontFamily(), size));
        t.getStyleClass().add("glyph");
        return t;
    }

    public static Label createIconLabel(GlyphIconName icon, String text, int iconSize, String fontSize, ContentDisplay contentDisplay) {
        Text iconLabel = createIcon(icon, String.valueOf(iconSize));
        Label l = new Label(text);
              l.setStyle("-fx-font-size: " + fontSize);
              l.setGraphic(iconLabel);
              l.setContentDisplay(contentDisplay);
              l.setPrefSize(iconSize, iconSize);
        return l;
    }

    public static Button createIconButton(GlyphIconName icon) {
        return createIconButton(icon, "");
    }

    public static Button createIconButton(GlyphIconName icon, String text) {
        Text label = createIcon(icon, GlyphIcon.DEFAULT_ICON_SIZE);
        Button button = new Button(text);
        button.setGraphic(label);
        return button;
    }

    public static Button createIconButton(GlyphIconName icon, String text, String iconSize, String fontSize, ContentDisplay contentDisplay) {
        Text label = createIcon(icon, iconSize);
        Button button = new Button(text);
        button.setStyle("-fx-font-size: " + fontSize);
        button.setGraphic(label);
        button.setContentDisplay(contentDisplay);
        return button;
    }

    public static ToggleButton createIconToggleButton(GlyphIconName icon, String text, String iconSize, ContentDisplay contentDisplay) {
        return createIconToggleButton(icon, text, iconSize, GlyphIcon.DEFAULT_FONT_SIZE, contentDisplay);
    }

    public static ToggleButton createIconToggleButton(GlyphIconName icon, String text, String iconSize, String fontSize, ContentDisplay contentDisplay) {
        Text label = createIcon(icon, iconSize);
        ToggleButton button = new ToggleButton(text);
        button.setStyle("-fx-font-size: " + fontSize);
        button.setGraphic(label);
        button.setContentDisplay(contentDisplay);
        return button;
    }


    /*
     * 
     * 
     * 
     */
    
    public static void setIcon(Tab tab, GlyphIconName icon) {
        setIcon(tab, icon, GlyphIcon.DEFAULT_ICON_SIZE);
    }

    public static void setIcon(Tab tab, GlyphIconName icon, String iconSize) {
        tab.setGraphic(createIcon(icon, iconSize));
    }

    public static void setIcon(Labeled labeled, GlyphIconName icon) {
        setIcon(labeled, icon, GlyphIcon.DEFAULT_ICON_SIZE);
    }

    public static void setIcon(Labeled labeled, GlyphIconName icon, ContentDisplay contentDisplay) {
        setIcon(labeled, icon, GlyphIcon.DEFAULT_ICON_SIZE, contentDisplay);
    }

    public static void setIcon(Labeled labeled, GlyphIconName icon, String iconSize) {
        setIcon(labeled, icon, iconSize, ContentDisplay.LEFT);
    }

    public static void setIcon(Labeled labeled, GlyphIconName icon, String iconSize, ContentDisplay contentDisplay) {
        labeled.setGraphic(createIcon(icon, iconSize));
        labeled.setContentDisplay(contentDisplay);
    }

    public static void setIcon(MenuItem menuItem, GlyphIconName icon) {
        setIcon(menuItem, icon, GlyphIcon.DEFAULT_FONT_SIZE, GlyphIcon.DEFAULT_ICON_SIZE);
    }

    public static void setIcon(MenuItem menuItem, GlyphIconName icon, String iconSize) {
        setIcon(menuItem, icon, GlyphIcon.DEFAULT_FONT_SIZE, iconSize);
    }

    public static void setIcon(MenuItem menuItem, GlyphIconName icon, String fontSize, String iconSize) {
        Text label = createIcon(icon, iconSize);
        menuItem.setStyle("-fx-font-size: " + fontSize);
        menuItem.setGraphic(label);
    }

    public static void setIcon(TreeItem treeItem, GlyphIconName icon) {
        setIcon(treeItem, icon, GlyphIcon.DEFAULT_ICON_SIZE);
    }

    public static void setIcon(TreeItem treeItem, GlyphIconName icon, String iconSize) {
        Text label = createIcon(icon, iconSize);
        treeItem.setGraphic(label);
    }
}
