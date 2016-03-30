/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;

import util.animation.Loop;

import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static javafx.stage.StageStyle.TRANSPARENT;
import static javafx.util.Duration.seconds;

/**
 * Simple Preloader Using the ProgressBar Control
 * <p/>
 * @author Martin Polakovic
 */
public class AppPreloader {
    Stage stage;
    Loop loop;
    double fps = 60;
    double angle = 0;
    double rotation = 2*PI/seconds(1).toSeconds()/fps;

    public void start() {

        Canvas canvas = new Canvas();
               canvas.setWidth(300);
               canvas.setHeight(300);
        GraphicsContext gc = canvas.getGraphicsContext2D();

            gc.setFill(Color.RED);
            gc.fillRect(33,33,55,55);
        double X = 150;
        double H = 150;
        loop = new Loop(() -> {
            angle += rotation;
            gc.clearRect(0,0,300,300);
            gc.setFill(Color.GREY);
            gc.fillOval(X + 60*cos(angle)-5, H+ 60*sin(angle)-5, 10,10);
        });

        Pane root = new StackPane(canvas);
             root.setBackground(null);
             root.setStyle("-fx-background: null;");
             root.setMouseTransparent(true);
        Scene scene = new Scene(root, 300,300, null);
        stage = new Stage(TRANSPARENT);
        stage.setScene(scene);
        stage.sizeToScene();
        stage.setAlwaysOnTop(true);

        Screen screen = Screen.getPrimary();
        stage.setX(screen.getBounds().getWidth()/2 - stage.getWidth()/2);
        stage.setY(screen.getBounds().getHeight()/2 - stage.getHeight()/2);
//        stage.show();
        stage.toFront();

        loop.start();
    }

    public void stop() {
        loop.stop();
        stage.hide();
    }
}
