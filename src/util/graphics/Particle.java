/*
 * This code is completely free of any restrictions on usage.
 * 
 * Feel free to study it, modify it, redistribute it and even claim it as your own if you like!
 * 
 * Courtesy of Bembrick Software Labs in the interest of promoting JavaFX.
 */

package util.graphics;

import javafx.scene.paint.Color;

/**
 * A <code>Particle</code> is the basic graphical unit that we are going to render on the <code>Canvas</code>.
 * 
 * Each particle encapsulates a current x and y coordinate along with a radius and colour and also delta values for each
 * of those attributes. Each particle also has its own maximum radius for variety.
 * 
 * @author Felix Bembrick (@Lucan1d)
 * @version 1.0 August 2013
 */
public class Particle {

	/**
	 * The absolute minimum radius we would like to see.
	 */
	public static final int MIN_RADIUS = 8;

	/**
	 * The absolute maximum radius we would like to see.
	 */
	public static final int MAX_RADIUS = 120;

	/**
	 * The absolute maximum x and y delta we would like to use to control motion.
	 */
	public static final int MAX_DELTA = 12;

	/**
	 * The standard radius delta.
	 */
	public static final int RADIUS_DELTA = 2;

	/**
	 * An array of the range of colours we would like to select from when creating a <code>Particle</code>.
	 */
	private static final Color[] COLOURS = new Color[] { Color.RED, Color.INDIGO, Color.GOLD, Color.GREEN, Color.BROWN, Color.BLUE,
			Color.ORANGERED, Color.YELLOW, Color.AQUA, Color.LIGHTPINK };

	/**
	 * The current x coordinate of the particle.
	 */
	private double x;

	/**
	 * The current y coordinate of the particle.
	 */
	private double y;

	/**
	 * The colour of the particle.
	 */
	private final Color colour;

	/**
	 * The current value for the x delta of the particle.
	 */
	private double dx;

	/**
	 * The current value for the y delta of the particle.
	 */
	private double dy;

	/**
	 * The current radius of the particle.
	 */
	private double r;

	/**
	 * The maximum radius permitted for this particle.
	 */
	private final double maxR;

	/**
	 * The current radius delta for this particle.
	 */
	private int dr;

	/**
	 * Creates a new instance of <code>Particle</code> based onthe specified dimensions of the <code>Canvas</code>.
	 * 
	 * @param canvasWidth The width of the canvas where the particle will be rendered.
	 * @param canvasHeight The height of the canvas where the particle will be rendered.
	 */
	public Particle(final int canvasWidth, final int canvasHeight) {

		// Initialise the x and y coordinates as being randomly positioned somewhere within the dimensions of the
		// canvas. For approximately half the particles we set the x coordinate to be oriented toward the left and half
		// oriented toward the right. Similarly half the particles have the y coordinate set to be oriented toward the
		// top and half are oriented toward the bottom.
		x = Math.round(Math.random() * 50);
		if (Math.random() > 0.5) {
			x = canvasWidth - x;
		}
		y = Math.round(Math.random() * 50);
		if (Math.random() > 0.5) {
			y = canvasHeight - y;
		}

		// Select a random colour from our preselected range.
		colour = COLOURS[(int)Math.round(Math.random() * (COLOURS.length - 1))];

		// Randomly set the x and y deltas to be a positive or negative value within the allowable range.
		dx = Math.round(Math.random() * MAX_DELTA) - MAX_DELTA / 2;
		dy = Math.round(Math.random() * MAX_DELTA) - MAX_DELTA / 2;

		// Randomly set the radius, maximum radius and radius deltas to be within the allowable range.
		r = Math.round(Math.random() * MAX_RADIUS) - MAX_RADIUS / 2;
		maxR = Math.round(Math.random() * MAX_RADIUS / 2) + 10;
		dr = Math.random() > 0.5 ? RADIUS_DELTA : -RADIUS_DELTA;
	}

	/**
	 * Returns the particle's colour.
	 * 
	 * @return The colour.
	 */
	public Color getColour() {
		return colour;
	}

	/**
	 * Returns the particle's radius delta.
	 * 
	 * @return The radius delta.
	 */
	public int getDr() {
		return dr;
	}

	/**
	 * Returns the particle's x delta.
	 * 
	 * @return The x delta.
	 */
	public double getDx() {
		return dx;
	}

	/**
	 * Returns the particle's y delta.
	 * 
	 * @return The y delta.
	 */
	public double getDy() {
		return dy;
	}

	/**
	 * Returns the particle's maximum radius.
	 * 
	 * @return The maximum radius.
	 */
	public double getMaxR() {
		return maxR;
	}

	/**
	 * Returns the particle's radius.
	 * 
	 * @return The radius.
	 */
	public double getR() {
		return r;
	}

	/**
	 * Returns the particle's x coordinate.
	 * 
	 * @return The x coordinate.
	 */
	public double getX() {
		return x;
	}

	/**
	 * Returns the particle's y coordinate.
	 * 
	 * @return The y coordinate.
	 */
	public double getY() {
		return y;
	}

	/**
	 * Sets the particle's radius delta.
	 * 
	 * @param The new radius delta.
	 */
	public void setDr(final int dr) {
		this.dr = dr;
	}

	/**
	 * Sets the particle's x delta.
	 * 
	 * @param The new x delta.
	 */
	public void setDx(final double dx) {
		this.dx = dx;
	}

	/**
	 * Sets the particle's y delta.
	 * 
	 * @param The new y delta.
	 */
	public void setDy(final double dy) {
		this.dy = dy;
	}

	/**
	 * Sets the particle's radius.
	 * 
	 * @param The new radius.
	 */
	public void setR(final double r) {
		this.r = r;
	}

	/**
	 * Sets the particle's x coordinate.
	 * 
	 * @param The new x coordinate.
	 */
	public void setX(final double x) {
		this.x = x;
	}

	/**
	 * Sets the particle's y coordinate.
	 * 
	 * @param The new y coordinate.
	 */
	public void setY(final double y) {
		this.y = y;
	}
}