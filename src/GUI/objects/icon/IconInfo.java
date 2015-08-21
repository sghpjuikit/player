/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.icon;

import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import de.jensd.fx.glyphs.GlyphIcons;

import static javafx.geometry.Pos.CENTER;

/**
 * Displays an icon with its name. Has tooltip displaying additional information.
 * Mouse click copies icon name to system clipboard.
 * 
 * @author Plutonium_
 */
public class IconInfo extends VBox {
    Icon i;
    public IconInfo(GlyphIcons icon, double icon_size) {
        super(5);
        setAlignment(CENTER);
        setOnMouseClicked(e -> {
            // copy exception trace to clipboard
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(icon.name().toLowerCase());
            clipboard.setContent(content);
        });
        
        String tooltip = icon.name() + "\n" + 
                         icon.unicodeToString() + "\n" +
                         icon.getFontFamily();
        i = new Icon(icon,icon_size,tooltip);
        
        getChildren().addAll(
            new StackPane(i),
            new Label(icon.name())
        );
    }
    
}
