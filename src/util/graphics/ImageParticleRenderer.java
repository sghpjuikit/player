/*
 * This code is completely free of any restrictions on usage.
 * 
 * Feel free to study it, modify it, redistribute it and even claim it as your own if you like!
 * 
 * Courtesy of Bembrick Software Labs in the interest of promoting JavaFX.
 */

package util.graphics;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;

/**
 * A <code>ImageParticleRenderer</code> is an implementation of the particle renderer that renders each particle as a
 * scaled image.
 * 
 * @author Felix Bembrick (@Lucan1d)
 * @version 1.0 August 2013
 */
public class ImageParticleRenderer implements ParticleRenderer {

	/**
	 * The image to be rendered.
	 */
	private static final Image image = new Image(ImageParticleRenderer.class.getResourceAsStream("duke.png"));

	/*
	 * @see com.bembrick.javafx.ParticleRenderer#getNumberOfParticles()
	 */
	@Override
	public int getNumberOfParticles() {

		// Image rendering performance is excellent so we can support a large number of particles..
		return 1000;
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
		blur.setRadius(2d);
		canvas.setEffect(blur);
	}

	/*
	 * @see com.bembrick.javafx.ParticleRenderer#render(javafx.scene.canvas.GraphicsContext,
	 * com.bembrick.javafx.Particle)
	 */
	@Override
	public void render(final GraphicsContext gc, final Particle p) {

		// Draw an image at the particle's coordinates and scale it based on the radius of the particle.
		gc.drawImage(image, p.getX(), p.getY(), p.getR() * 2, p.getR() * 2);
	}
}