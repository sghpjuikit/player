package sp.it.pl.util.demo;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class JavaFxStagePlaygroundDemo extends Application {

	@Override
	public void start(Stage primaryStage) throws Exception {
		StackPane testPane = new StackPane();
		testPane.setStyle("-fx-background-color:transparent;");
		Label someText = new Label("TEXT AGAINST TRANSPARENT SCENE");
		testPane.getChildren().add(someText);
		Scene myScene = new Scene(testPane, 500, 500);
		myScene.setFill(Color.TRANSPARENT);
		primaryStage.setScene(myScene);
		primaryStage.initStyle(StageStyle.UNDECORATED);
		primaryStage.setTitle("Application");
		primaryStage.show();
	}

	public static void main(String[] args) {
		launch(args);
	}

}