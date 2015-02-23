/*
 * This code is completely free of any restrictions on usage.
 * 
 * Feel free to study it, modify it, redistribute it and even claim it as your own if you like!
 * 
 * Courtesy of Bembrick Software Labs in the interest of promoting JavaFX.
 */

package util.graphics.visualisation;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import static util.graphics.visualisation.Particle.MIN_RADIUS;
import static util.graphics.visualisation.Particle.RADIUS_DELTA;

/**
 * A <code>CanvasParticleAnimater</code> is the main object involved in testing a JavaFX <code>Canvas</code> by
 * rendering and animating a number of particles.
 * 
 * The algorithm for animating the particles is very simplistic but still enables some interesting patterns of movement.
 * An animation timer is used to control the repeated rendering of "frames". During each frame both the coordinates and
 * the radius of the particles are changed in such a way that the particle is continually getting smaller or larger in
 * size until either minimum or maximum dimensions are reached and is moving in a straight path until it encounters the
 * bounds of the canvas.
 * 
 * @author Felix Bembrick (@Lucan1d)
 * @version 1.0 August 2013
 */
public class CanvasParticleAnimater {

	/**
	 * The width of the actual <code>Canvas</code>
	 */
	private final int width;

	/**
	 * The height of the actual <code>Canvas</code>
	 */
	private final int height;

	/**
	 * The <code>Canvas</code> in which the particles are to be rendered.
	 */
	private final Canvas canvas;

	/**
	 * This is the context in which all graphical operations on the <code>Canvas</code> are performed.
	 */
	private final GraphicsContext gc;

	/**
	 * An array of the particles to be rendered.
	 */
	private Particle[] particles;

	/**
	 * The particular <code>ParticleRenderer</code> implementation to be used for the test.
	 */
	private final ParticleRenderer renderer;

	/**
	 * Creates a new instance of <code>CanvasParticleAnimater</code> using the specified <code>ParticleRenderer</code>.
	 * 
	 * @param renderer The particular renderer to be used for rendering each particle.
	 */
	public CanvasParticleAnimater(final int width, final int height, final ParticleRenderer renderer) {
		this.width = width;
		this.height = height;
		canvas = new Canvas(width, height);
		gc = canvas.getGraphicsContext2D();
		this.renderer = renderer;

		// Initialise the test.
		init();

		// Animate the particles.
		animate();
	}

	/**
	 * Animates the particles.
	 * 
	 * The animation is driven by the
	 * <code>AnimationTimer<code> which repeatedly invokes the method to render each frame.
	 */
	private void animate() {
		new AnimationTimer() {

			/*
			 * @see javafx.animation.AnimationTimer#handle(long)
			 */
			@Override
			public void handle(final long now) {
				renderFrame();
			}
		}.start();
	}

	/**
	 * Returns the actual JavaFX <code>Canvas</code> used in this test.
	 * 
	 * @return The actual canvas.
	 */
	public Canvas getCanvas() {
		return canvas;
	}

	/**
	 * Initialises the test by creating and initialising the particles themselves and initialising the renderer.
	 */
	private void init() {

		// Create the particles to be rendered.
		particles = new Particle[renderer.getNumberOfParticles()];
		for (int i = 0; i < renderer.getNumberOfParticles(); i++) {
			particles[i] = new Particle(width, height);
		}

		// Initialise the particle renderer.
		renderer.init(canvas);
	}

	/**
	 * Renders each "frame" of the animation and adjusts the attributes of each <code>Particle</code>.
	 */
	private void renderFrame() {

		// Start by clearing the canvas by filling it with a solid colour.
		gc.setFill(Color.DARKSLATEGRAY);
		gc.fillRect(0, 0, width, height);

		// Loop through each of our particles.
		for (final Particle p : particles) {

			// Firstly invoke the particular implementation of the renderer to render each particle.
			renderer.render(gc, p);

			// Now we want to adjust the radius of the particle. If the current radius is less than the minimum we allow
			// then set the radius delta to the standard positive value so that in future it will grow larger. If the
			// current radius has grown larger than the maximum for this particle then set the radius delta to a
			// negative value to ensure that it decreases in size in future. In all other cases we don't alter the
			// radius delta.
			if (p.getR() <= MIN_RADIUS) {
				p.setDr(RADIUS_DELTA);
			} else if (p.getR() > p.getMaxR()) {
				p.setDr(-RADIUS_DELTA);
			}

			// Use the radius delta to change the size of the particle's radius.
			p.setR(p.getR() + p.getDr());

			// Now we want to actually move the particle so increment or decrement the x and y coordinates of the
			// particle by the x delta and y delta defined for it.
			p.setX(p.getX() + p.getDx());
			p.setY(p.getY() + p.getDy());

			// We need to ensure that the particle does not move outside the bounds of the canvas so if the current x
			// coordinate is greater than the width of the canvas or less than zero then we need to reverse the motion
			// by changing the sign of the x delta.
			if (p.getX() > width) {
				p.setX(width);
				p.setDx(-p.getDx());
			} else if (p.getX() < 0) {
				p.setX(0);
				p.setDx(-p.getDx());
			}

			// We also need to handle a similar situation with the y coordinate so if the current y
			// coordinate is greater than the height of the canvas or less than zero then we need to reverse the motion
			// by changing the sign of the y delta.
			if (p.getY() > height) {
				p.setY(height);
				p.setDy(-p.getDy());
			} else if (p.getY() < 0) {
				p.setY(0);
				p.setDy(-p.getDy());
			}
		}
	}
}