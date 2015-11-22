/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.icon;

import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import de.jensd.fx.glyphs.GlyphIcons;

import static javafx.geometry.Pos.CENTER;
import static util.File.Environment.copyToSysClipboard;

/**
 * Displays an icon with its name. Has tooltip displaying additional information.
 * Mouse click copies icon name to system clipboard.
 *
 * @author Plutonium_
 */
public class IconInfo extends VBox {

    public IconInfo(GlyphIcons icon, double icon_size) {
        super(5);
        setAlignment(CENTER);
        setOnMouseClicked(e -> copyToSysClipboard(icon.name().toLowerCase()));

        String tooltip = icon.name() + "\n" +
                         icon.unicodeToString() + "\n" +
                         icon.getFontFamily();
        Icon i = new Icon(icon,icon_size,tooltip);

        getChildren().addAll(
            new StackPane(i),
            new Label(icon.name())
        );
    }

}
