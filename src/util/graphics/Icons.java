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
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
import de.jensd.fx.glyphs.weathericons.WeatherIcon;
import de.jensd.fx.glyphs.weathericons.WeatherIconName;
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

    /*
     * Weather Icons stuff 
     *
     */
    public static Text createIcon(GlyphIconName icon) {
        return createIcon(icon, GlyphIcon.DEFAULT_ICON_SIZE);
    }
    public static Text createIcon(GlyphIconName icon, String iconSize) {
        Text t = new Text(icon.characterToString());
        t.setStyle(String.format("-fx-font-family: %s; -fx-font-size: %s;",icon.getFontFamily(), iconSize));
        return t;
    }
    public static Text createIcon(GlyphIconName icon, int icons, int size) {
        String s = icon.characterToString();
        StringBuilder sb = new StringBuilder(icons);
        for(int i=0; i<icons; i++) sb.append(s);
        
        Text t = new Text(sb.toString());
        t.setStyle(String.format("-fx-font-family: %s; -fx-font-size: %s;",icon.getFontFamily(), size));
        return t;
    }
    
    
    public static Text createIcon(WeatherIconName icon) {
        return GlyphsDude.createIcon(icon, GlyphIcon.DEFAULT_ICON_SIZE);
    }

    public static Text createIcon(WeatherIconName icon, String iconSize) {
        Text text = new Text(icon.characterToString());
        text.setStyle(String.format("-fx-font-family: %s; -fx-font-size: %s;",icon.getFontFamily(), iconSize));
        return text;
    }

    /*
     * Font Awesome stuff 
     *
     */
    public static Text createIcon(FontAwesomeIconName icon) {
        return GlyphsDude.createIcon(icon, GlyphIcon.DEFAULT_ICON_SIZE);
    }

    public static Text createIcon(FontAwesomeIconName icon, String iconSize) {
        Text text = new Text(icon.characterToString());
        text.setStyle(String.format("-fx-font-family: %s; -fx-font-size: %s;", icon.getFontFamily(), iconSize));
        text.setStyle("-fx-font-family: FontAwesome; -fx-font-size: " + iconSize + ";");
        return text;
    }

    public static Label createIconLabel(FontAwesomeIconName icon, String text, String iconSize, String fontSize, ContentDisplay contentDisplay) {
        Text iconLabel = GlyphsDude.createIcon(icon, iconSize);
        Label label = new Label(text);
        label.setStyle("-fx-font-size: " + fontSize);
        label.setGraphic(iconLabel);
        label.setContentDisplay(contentDisplay);
        return label;
    }

    public static Button createIconButton(FontAwesomeIconName icon) {
        return createIconButton(icon, "");
    }

    public static Button createIconButton(FontAwesomeIconName icon, String text) {
        Text label = GlyphsDude.createIcon(icon, GlyphIcon.DEFAULT_ICON_SIZE);
        Button button = new Button(text);
        button.setGraphic(label);
        return button;
    }

    public static Button createIconButton(FontAwesomeIconName icon, String text, String iconSize, String fontSize, ContentDisplay contentDisplay) {
        Text label = GlyphsDude.createIcon(icon, iconSize);
        Button button = new Button(text);
        button.setStyle("-fx-font-size: " + fontSize);
        button.setGraphic(label);
        button.setContentDisplay(contentDisplay);
        return button;
    }

    public static ToggleButton createIconToggleButton(FontAwesomeIconName icon, String text, String iconSize, ContentDisplay contentDisplay) {
        return createIconToggleButton(icon, text, iconSize, GlyphIcon.DEFAULT_FONT_SIZE, contentDisplay);
    }

    public static ToggleButton createIconToggleButton(FontAwesomeIconName icon, String text, String iconSize, String fontSize, ContentDisplay contentDisplay) {
        Text label = GlyphsDude.createIcon(icon, iconSize);
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
    
    public static void setIcon(Tab tab, FontAwesomeIconName icon) {
        setIcon(tab, icon, GlyphIcon.DEFAULT_ICON_SIZE);
    }

    public static void setIcon(Tab tab, FontAwesomeIconName icon, String iconSize) {
        tab.setGraphic(GlyphsDude.createIcon(icon, iconSize));
    }

    public static void setIcon(Labeled labeled, FontAwesomeIconName icon) {
        setIcon(labeled, icon, GlyphIcon.DEFAULT_ICON_SIZE);
    }

    public static void setIcon(Labeled labeled, FontAwesomeIconName icon, ContentDisplay contentDisplay) {
        setIcon(labeled, icon, GlyphIcon.DEFAULT_ICON_SIZE, contentDisplay);
    }

    public static void setIcon(Labeled labeled, FontAwesomeIconName icon, String iconSize) {
        setIcon(labeled, icon, iconSize, ContentDisplay.LEFT);
    }

    public static void setIcon(Labeled labeled, FontAwesomeIconName icon, String iconSize, ContentDisplay contentDisplay) {
        if (labeled == null) {
            throw new IllegalArgumentException("The component must not be 'null'!");
        }
        labeled.setGraphic(GlyphsDude.createIcon(icon, iconSize));
        labeled.setContentDisplay(contentDisplay);
    }

    public static void setIcon(MenuItem menuItem, FontAwesomeIconName icon) {
        setIcon(menuItem, icon, GlyphIcon.DEFAULT_FONT_SIZE, GlyphIcon.DEFAULT_ICON_SIZE);
    }

    public static void setIcon(MenuItem menuItem, FontAwesomeIconName icon, String iconSize) {
        setIcon(menuItem, icon, GlyphIcon.DEFAULT_FONT_SIZE, iconSize);
    }

    public static void setIcon(MenuItem menuItem, FontAwesomeIconName icon, String fontSize, String iconSize) {
        if (menuItem == null) {
            throw new IllegalArgumentException("The menu item must not be 'null'!");
        }
        Text label = GlyphsDude.createIcon(icon, iconSize);
        menuItem.setStyle("-fx-font-size: " + fontSize);
        menuItem.setGraphic(label);
    }

    public static void setIcon(TreeItem treeItem, FontAwesomeIconName icon) {
        setIcon(treeItem, icon, GlyphIcon.DEFAULT_ICON_SIZE);
    }

    public static void setIcon(TreeItem treeItem, FontAwesomeIconName icon, String iconSize) {
        if (treeItem == null) {
            throw new IllegalArgumentException("The tree item must not be 'null'!");
        }
        Text label = GlyphsDude.createIcon(icon, iconSize);
        treeItem.setGraphic(label);
    }
}
