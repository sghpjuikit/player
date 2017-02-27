package util.demo.particleanim;

import javafx.scene.paint.Color;

/**
 * Application settings
 */
public class Settings {

	public static double SCENE_WIDTH = 1600;
	public static double SCENE_HEIGHT = 900;
	public static Color SCENE_COLOR = Color.BLACK;

	public static int ATTRACTOR_COUNT = 1;
	public static int REPELLER_COUNT = 1;

	// emitter parameters
	public static int PARTICLES_PER_ITERATION = 50;
	public static int EMITTER_WIDTH = (int) SCENE_WIDTH;
	public static double EMITTER_LOCATION_Y = SCENE_HEIGHT/2;

	// particle parameters
	public static int PARTICLE_WIDTH = 40;
	public static int PARTICLE_HEIGHT = PARTICLE_WIDTH;
	public static double PARTICLE_LIFE_SPAN_MAX = 256;
	public static double PARTICLE_MAX_SPEED = 4;

	// just some artificial strength value that matches our needs. 
	public static double REPELLER_STRENGTH = 500;

	// gravity. use negative if you want the particles to always go up, eg new Vector2D( 0,-0.04);
	public static Vector2D FORCE_GRAVITY = new Vector2D(0, 0);
}