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

/**
 * A <code>ParticleRenderer</code> is an object responsible for rendering a particle at a particular location in the
 * <code>Canvas</code>.
 * 
 * @author Felix Bembrick (@Lucan1d)
 * @version 1.0 August 2013
 */
public interface ParticleRenderer {

	/**
	 * Returns the recommended number of particles supported by this renderer in a <code>Canvas</code>. The more complex
	 * the rendering, the lower the number of particles.
	 * 
	 * @return The number of particles.
	 */
	public int getNumberOfParticles();

	/**
	 * Initialises the renderer for the specified <code>Canvas</code>.
	 * 
	 * @param canvas The <code>Canvas</code> to render the particles.
	 */
	public void init(final Canvas canvas);

	/**
	 * Renders the given <code>Particle</code> using the specified <code>GraphicsContext</code>.
	 * 
	 * @param gc The graphics context.
	 * @param p The particle to be rendered.
	 */
	public void render(final GraphicsContext gc, final Particle p);
}
