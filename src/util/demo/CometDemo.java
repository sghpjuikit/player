package util.demo;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import comet.Comet;

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
		stage.setFullScreen(true);
		stage.setFullScreenExitHint("");
		stage.setScene(new Scene(new Comet()));
		stage.show();
	}
}