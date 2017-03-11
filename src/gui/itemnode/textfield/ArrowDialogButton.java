package gui.itemnode.textfield;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import gui.objects.icon.Icon;
import javafx.scene.layout.StackPane;

/**
 * Button for calling dialogs, from within {@link javafx.scene.control.TextField}.
 */
public class ArrowDialogButton extends StackPane {

	public ArrowDialogButton() {
		// Non-icon pure css implementation, that looks exactly like other javaFx dialog
		// String styleClass = "dialog-button";
		// Region r = new Region();
		//        r.getStyleClass().add(styleClass);
		//        r.setMinSize(0, 0);
		//        r.setPrefSize(7, 6);
		//        r.setMaxSize(7, 6);
		// setPrefSize(22,22);

		Icon r = new Icon(FontAwesomeIcon.CARET_DOWN, 7).scale(2).tooltip("Open dialog");
		getChildren().add(r);
	}
}