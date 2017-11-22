package sp.it.pl.gui.objects.balancer;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import static javafx.scene.input.MouseEvent.MOUSE_DRAGGED;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;

public class Balancer extends Control {

	public static String STYLECLASS = "balancer";

	private DoubleProperty balance = new SimpleDoubleProperty(this, "balance", 0);
	private DoubleProperty max = new SimpleDoubleProperty(this, "max", 1);
	private DoubleProperty min = new SimpleDoubleProperty(this, "min", -1);

	/** Creates a default instance with default balance, min, max values. */
	public Balancer() {
		getStyleClass().setAll(STYLECLASS);
		setBalance(getBalance()); // refresh
	}

	/**
	 * Creates a Balancer instance with specified current, min and max
	 * value.
	 *
	 * @param balance The maximum allowed rating value. <-1,1>
	 */
	public Balancer(double balance, double min, double max) {
		setMax(max);
		setMin(min);
	}

	@Override
	protected Skin<?> createDefaultSkin() {
		return new BalancerSkin(this);
	}

	/** @return The current balance value. */
	public final DoubleProperty balanceProperty() {
		return balance;
	}

	/** Returns the current balance value. */
	public final double getBalance() {
		return balance==null ? 0 : balance.get();
	}

	/** Sets the current balance value. */
	public final void setBalance(double value) {
		double to = value;

		if (to>getMax()) to = getMax();
		if (to<getMin()) to = getMin();

		if (Math.abs(to)<0.2) to = 0;
		if (to - getMin()<0.2) to = getMin();
		if (getMax() - to<0.2) to = getMax();

		balanceProperty().set(to);
	}

	/** The maximum-allowed rating value. */
	public final DoubleProperty maxProperty() {
		return max;
	}

	/** Sets the maximum-allowed rating value. */
	public final void setMax(double value) {
		maxProperty().set(value);
	}

	/** Returns the maximum-allowed rating value. */
	public final double getMax() {
		return max.get();
	}

	/** The maximum-allowed rating value. */
	public final DoubleProperty minProperty() {
		return min;
	}

	/** Sets the maximum-allowed rating value. */
	public final void setMin(double value) {
		minProperty().set(value);
	}

	/** Returns the maximum-allowed rating value. */
	public final double getMin() {
		return min.get();
	}

	public static class BalancerSkin extends SkinBase<Balancer> {

		// the container for the traditional rating control. If updateOnHover and
		// partialClipping are disabled, this will show a combination of strong
		// and non-strong graphics, depending on the current rating value
		private ImageView bgrContainer;

		// the container for the strong graphics which may be partially clipped.
		// Note that this only exists if updateOnHover or partialClipping is enabled.
		private ImageView fgrContainer;
		private Rectangle fgrClipRect;
		private double balance;

		public BalancerSkin(Balancer b) {
			super(b);

			registerChangeListener(b.balanceProperty(), e -> updateBalance(getSkinnable().getBalance()));
			registerChangeListener(b.maxProperty(), e -> updateClip());
			registerChangeListener(b.minProperty(), e -> updateClip());

			// create graphics
			bgrContainer = new ImageView();
			bgrContainer.setPreserveRatio(false);
			bgrContainer.fitHeightProperty().bind(getSkinnable().prefHeightProperty());
			bgrContainer.fitWidthProperty().bind(getSkinnable().prefWidthProperty());
			bgrContainer.getStyleClass().add("balancer-bgr");
			getChildren().setAll(bgrContainer);

			fgrContainer = new ImageView();
			fgrContainer.setPreserveRatio(false);
			fgrContainer.fitHeightProperty().bind(getSkinnable().prefHeightProperty());
			fgrContainer.fitWidthProperty().bind(getSkinnable().prefWidthProperty());

			fgrContainer.getStyleClass().add("balancer-fgr");
			fgrContainer.setMouseTransparent(true);
			getChildren().add(fgrContainer);

			fgrClipRect = new Rectangle();
			fgrContainer.setClip(fgrClipRect);

			// install behavior
			// note: use container instead of styleable component. When putting the
			// styleable within a gridPane (for example) its PrefWidth is not what
			// one would expect. As such the balancing with mouse did not work properly.
			getSkinnable().addEventHandler(MOUSE_DRAGGED, e -> {
				double x = e.getX() - fgrContainer.getLayoutX();
				if (getSkinnable().contains(x, e.getY())) {
					double bal = (x/fgrContainer.getFitWidth() - 0.5)*2;
					getSkinnable().setBalance(bal);
				}
			});
			getSkinnable().addEventHandler(MOUSE_PRESSED, e -> {
				double x = e.getX() - fgrContainer.getLayoutX();
				if (getSkinnable().contains(x, e.getY())) {
					double bal = (x/fgrContainer.getFitWidth() - 0.5)*2;
					getSkinnable().setBalance(bal);
				}
			});

			updateBalance(getSkinnable().getBalance());
		}

		private void updateBalance(double newBalance) {
			balance = newBalance;
			getSkinnable().setBalance(balance);
			updateClip();
		}

		private void updateClip() {
			final Balancer control = getSkinnable();
			final double w = control.getPrefWidth();// - (snappedLeftInset() + snappedRightInset());
			final double h = control.getPrefHeight();// - (snappedTopInset() + snappedBottomInset());

			double start = balance<0 ? 0 : balance*w/2;
			double end = balance>0 ? w : w/2 + (balance + 1)*w/2;

			fgrClipRect.relocate(start, 0);
			fgrClipRect.setWidth(end - start);
			fgrClipRect.setHeight(h);
		}
	}

}