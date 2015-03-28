/*
 * This code is completely free of any restrictions on usage.
 * 
 * Feel free to study it, modify it, redistribute it and even claim it as your own if you like!
 * 
 * Courtesy of Bembrick Software Labs in the interest of promoting JavaFX.
 */

package unused.visualisation;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.GaussianBlur;

/**
 * A <code>BallParticleRenderer</code> is an implementation of the particle renderer that renders each particle as a
 * filled oval.
 * 
 * @author Felix Bembrick (@Lucan1d)
 * @version 1.0 August 2013
 */
public class BallParticleRenderer implements ParticleRenderer {

	/*
	 * @see com.bembrick.javafx.ParticleRenderer#getNumberOfParticles()
	 */
	@Override
	public int getNumberOfParticles() {

		// This type of renderer actually seems to support a high number of particles.
		return 100;
	}

	/*
	 * @see com.bembrick.javafx.ParticleRenderer#init(javafx.scene.canvas.Canvas)
	 */
	@Override
	public void init(final Canvas canvas) {

		// Set an alpha level across all rendering for the <code>Canvas</code> to apply a translucent effect.
		canvas.getGraphicsContext2D().setGlobalAlpha(0.7);

		// Apply a Gaussian Blur to the entire <code>Canvas</code> for a cool, soft effect.
		final GaussianBlur blur = new GaussianBlur();
		blur.setRadius(4d);
		canvas.setEffect(blur);
	}

	/*
	 * @see com.bembrick.javafx.ParticleRenderer#render(javafx.scene.canvas.GraphicsContext,
	 * com.bembrick.javafx.Particle)
	 */
	@Override
	public void render(final GraphicsContext gc, final Particle p) {

		// Set the current fill colour to that of the specified <code>Particle</code>.
		gc.setFill(p.getColour());

		// Render the particle by filling the oval specified by the attributes of the <code>Particle</code> with the
		// current fill colour.
		gc.fillOval(p.getX(), p.getY(), p.getR(), p.getR());
	}
}