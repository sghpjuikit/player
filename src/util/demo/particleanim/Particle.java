package util.demo.particleanim;

import javafx.scene.Node;

/**
 * A single particle with a per-frame reduced lifespan and now view. The particle is drawn on a canvas, it isn't actually a node
 */
public class Particle extends Sprite {
	
	public Particle(Vector2D location, Vector2D velocity, Vector2D acceleration, double width, double height) {
		super( location, velocity, acceleration, width, height);
	}

	@Override
	public Node createView() {
		return null;
	}
	
	public void decreaseLifeSpan() {
		lifeSpan--;
	}

}
