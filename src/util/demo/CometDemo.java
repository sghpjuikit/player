package util.demo;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import main.App;

/**
 * Demo showcasing the comet game widget.
 *
 *
 * @author Martin Polakovic
 */
public class CometDemo extends Application{

	public static void main(String... args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		App app = new App();
		app.widgetManager.initialize();
		Node comet = app.widgetManager.getFactories().filter(f -> f.name().equals("Comet")).findFirst().get()
			.create()
			.load();
		stage.setFullScreen(true);
		stage.setFullScreenExitHint("");
		stage.setScene(new Scene(new StackPane(comet)));
		stage.show();
	}
}