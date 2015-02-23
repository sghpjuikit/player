/*
 * This code is completely free of any restrictions on usage.
 * 
 * Feel free to study it, modify it, redistribute it and even claim it as your own if you like!
 * 
 * Courtesy of Bembrick Software Labs in the interest of promoting JavaFX.
 */

package util.graphics.visualisation;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;

/**
 * A <code>TextParticleRenderer</code> is an implementation of the particle renderer that renders each particle as a
 * simple coloured text string.
 * 
 * @author Felix Bembrick (@Lucan1d)
 * @version 1.0 August 2013
 */
public class TextParticleRenderer implements ParticleRenderer {

	/*
	 * @see com.bembrick.javafx.ParticleRenderer#getNumberOfParticles()
	 */
	@Override
	public int getNumberOfParticles() {

		// There seem to be major performance issues with stroking text but filling text is fast so the recommended
		// number of particles is quite high.
		return 1000;
	}

	/*
	 * @see com.bembrick.javafx.ParticleRenderer#init(javafx.scene.canvas.Canvas)
	 */
	@Override
	public void init(final Canvas canvas) {

		// Set an alpha level across all rendering for the <code>Canvas</code> to apply a translucent effect.
		canvas.getGraphicsContext2D().setGlobalAlpha(0.7);
	}

	/*
	 * @see com.bembrick.javafx.ParticleRenderer#render(javafx.scene.canvas.GraphicsContext,
	 * com.bembrick.javafx.Particle)
	 */
	@Override
	public void render(final GraphicsContext gc, final Particle p) {

		// Set the font to be used for rendering text across the <code>Canvas</code> with a size dependent on the radius
		// of the particle.
		gc.setFont(Font.font("Arial", FontPosture.REGULAR, p.getR() / 2));

		// Set the current stroke colour to that of the particle.
		gc.setFill(p.getColour());

		// Render the text by stroking it with the current stroke colour at the particle's current coordinates.
		gc.fillText("JavaFX", p.getX(), p.getY()); //$NON-NLS-1$
	}
}