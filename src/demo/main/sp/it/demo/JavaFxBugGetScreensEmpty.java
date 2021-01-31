package sp.it.demo;

import javafx.application.Application;
import javafx.collections.ListChangeListener.Change;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

/** Showcasing javafx bug https://bugs.openjdk.java.net/browse/JDK-8252446 */
class JavaFxBugGetScreensEmpty extends Application {

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) {

		Screen.getScreens().addListener((Change<?> event) -> {
			System.out.println("");
			System.out.println(Screen.getPrimary());
			System.out.println(Screen.getScreens().size());
		});

		final StackPane background = new StackPane();
		final Scene scene = new Scene(background, 600, 500);
		stage.show();
	}

}