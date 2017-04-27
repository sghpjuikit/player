package gui.pane;

import gui.objects.Text;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextAlignment;
import static util.graphics.Util.*;

public class MessagePane extends OverlayPane<String> {

	private final Text text = new Text();

	public MessagePane() {
		text.setTextOrigin(VPos.CENTER);
		text.setTextAlignment(TextAlignment.JUSTIFY);
		setMinPrefMaxSize(text, -1.0);

		ScrollPane textPane = layScrollVTextCenter(text);
		textPane.setPrefWidth(400);
		textPane.setMaxWidth(400);

		StackPane root = layStack(textPane, Pos.CENTER);
		root.setPadding(new Insets(50));
		root.setMaxHeight(200);

		setContent(root);
	}

	@Override
	public void show(String message) {
		text.setText(message);
		super.show();
	}
}