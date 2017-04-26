package gui.objects.icon;

import de.jensd.fx.glyphs.GlyphIcons;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import static javafx.geometry.Pos.CENTER;
import static util.Util.capitalizeStrong;
import static util.file.Environment.copyToSysClipboard;

/**
 * Displays an icon with its name. Has tooltip displaying additional information.
 * Mouse click copies icon name to system clipboard.
 */
public class IconInfo extends VBox {

	private GlyphIcons glyph;
	private final Label nameLabel = new Label();
	private final Icon graphics;

	public IconInfo(GlyphIcons icon, double icon_size) {
		super(5);
		setAlignment(CENTER);
		setOnMouseClicked(e -> copyToSysClipboard(glyph.name()));

		String tooltip = ""; // initializes tooltip
		graphics = new Icon(icon, icon_size, tooltip);
		getChildren().addAll(new StackPane(graphics), nameLabel);

		setGlyph(icon);
	}

	public void setGlyph(GlyphIcons icon) {
		glyph = icon;
		nameLabel.setText(icon==null ? "" : capitalizeStrong(icon.name()));
		graphics.icon(icon);
		graphics.tooltip(icon==null ? "" :
			icon.name() + "\n" +
				icon.unicodeToString() + "\n" +
				icon.getFontFamily()
		);
	}

	public GlyphIcons getGlyph() {
		return glyph;
	}

	public void select(boolean value) {
		graphics.select(value);
	}
}