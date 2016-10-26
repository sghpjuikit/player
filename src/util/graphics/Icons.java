package util.graphics;

import javafx.scene.control.*;
import javafx.scene.text.Text;

import de.jensd.fx.glyphs.GlyphIcons;

/**
 * @author Martin Polakovic
 */
public class Icons {

//    static {
//        Font.loadFont(GlyphsDude.class.getResource(FontAwesomeIcon.TTF_PATH).toExternalForm(), 10.0);
//        Font.loadFont(GlyphsDude.class.getResource(WeatherIcon.TTF_PATH).toExternalForm(), 10.0);
//    }

	public static final String DEFAULT_ICON_SIZE = "12";

	public static Text createIcon(GlyphIcons icon) {
		return createIcon(icon, DEFAULT_ICON_SIZE);
	}

	public static Text createIcon(GlyphIcons icon, int icon_size) {
		return createIcon(icon, String.valueOf(icon_size));
	}

	private static Text createIcon(GlyphIcons icon, String iconSize) {
		Text t = new Text(icon.characterToString());
		t.setStyle(String.format("-fx-font-family: %s; -fx-font-size: %s;",icon.getFontFamily(), iconSize));
		t.getStyleClass().add("icon");
		return t;
	}

	public static Text createIcon(GlyphIcons icon, int icons, int size) {
		String s = icon.characterToString();
		StringBuilder sb = new StringBuilder(icons);
		for (int i=0; i<icons; i++) sb.append(s);

		Text t = new Text(sb.toString());
		t.setStyle(String.format("-fx-font-family: %s; -fx-font-size: %s;",icon.getFontFamily(), size));
		t.getStyleClass().add("icon");
		return t;
	}

	public static Label createIconLabel(GlyphIcons icon, String text, int iconSize, String fontSize, ContentDisplay contentDisplay) {
		Text iconLabel = createIcon(icon, String.valueOf(iconSize));
		Label l = new Label(text);
			  l.setStyle("-fx-font-size: " + fontSize);
			  l.setGraphic(iconLabel);
			  l.setContentDisplay(contentDisplay);
			  l.setPrefSize(iconSize, iconSize);
		return l;
	}

	public static Button createIconButton(GlyphIcons icon) {
		return createIconButton(icon, "");
	}

	public static Button createIconButton(GlyphIcons icon, String text) {
		Text label = createIcon(icon, DEFAULT_ICON_SIZE);
		Button button = new Button(text);
		button.setGraphic(label);
		return button;
	}

	public static Button createIconButton(GlyphIcons icon, String text, String iconSize, String fontSize, ContentDisplay contentDisplay) {
		Text label = createIcon(icon, iconSize);
		Button button = new Button(text);
		button.setStyle("-fx-font-size: " + fontSize);
		button.setGraphic(label);
		button.setContentDisplay(contentDisplay);
		return button;
	}

	public static ToggleButton createIconToggleButton(GlyphIcons icon, String text, String iconSize, ContentDisplay contentDisplay) {
		return createIconToggleButton(icon, text, iconSize, DEFAULT_ICON_SIZE, contentDisplay);
	}

	public static ToggleButton createIconToggleButton(GlyphIcons icon, String text, String iconSize, String fontSize, ContentDisplay contentDisplay) {
		Text label = createIcon(icon, iconSize);
		ToggleButton button = new ToggleButton(text);
		button.setStyle("-fx-font-size: " + fontSize);
		button.setGraphic(label);
		button.setContentDisplay(contentDisplay);
		return button;
	}

}