/*
 * This code is completely free of any restrictions on usage.
 * 
 * Feel free to study it, modify it, redistribute it and even claim it as your own if you like!
 * 
 * Courtesy of Bembrick Software Labs in the interest of promoting JavaFX.
 */

package unused.visualisation;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * A <code>CanvasTextParticleTest</code> is a simple JavaFX application to test
 * <code>Canvas<code> rendering with a large number of particles in the form of simple coloured text strings.
 * 
 * @author Felix Bembrick (@Lucan1d)
 * @version 1.0 August 2013
 */
public class CanvasTextParticleTest extends Application {

	/**
	 * This is the program's entry point and launches the application.
	 * 
	 * @param args An array of arguments (usually specified on the command line).
	 */
	public static void main(final String[] args) {
		Application.launch(args);
	}

	/*
	 * @see javafx.application.Application#start(javafx.stage.Stage)
	 */
	@Override
	public void start(final Stage stage) {

		// Set the title of the primary <code>Stage</code>.
		stage.setTitle("JavaFX Canvas Text Particle Test"); //$NON-NLS-1$

		// Create a new particle tester using a text renderer.
		final CanvasParticleAnimater animater = new CanvasParticleAnimater(800, 600, new TextParticleRenderer());

		// Create a pane to contain the <code>Canvas</code> that will automatically center it.
		final BorderPane pane = new BorderPane();
		pane.setCenter(animater.getCanvas());

		// Create a new JavaFX <code>Scene</code> with the pane as it's root in the scene graph.
		final Scene scene = new Scene(pane);

		// Set the active scene for the stage and show it on the screen.
		stage.setScene(scene);
		stage.show();
	}
}
