package sp.it.demo;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/** Showcasing javafx bug https://bugs.openjdk.java.net/browse/JDK-9068854 */
class JavaFxBugTextAreaDispose extends Application {

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) {
		final TextArea textArea = new TextArea();
		final StackPane background = new StackPane(textArea);
		final Scene scene = new Scene(background, 600, 500);

		stage.setScene(scene);
		stage.show();

		Platform.runLater(() -> {
			background.getChildren().clear();
			textArea.setSkin(null);
		});
	}

}