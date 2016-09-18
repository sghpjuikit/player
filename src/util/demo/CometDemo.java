package util.demo;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import comet.Comet;
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
		new App();
		stage.setFullScreen(true);
		stage.setFullScreenExitHint("");
		stage.setScene(new Scene(new Comet()));
		stage.show();
	}
}