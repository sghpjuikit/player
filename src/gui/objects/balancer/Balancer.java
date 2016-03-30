

package gui.objects.balancer;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;

import static javafx.scene.input.MouseEvent.MOUSE_DRAGGED;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;

/**
 *
 * @author Martin Polakovic
 */
public class Balancer extends Control {

    private static String STYLECLASS = "balancer";

    private DoubleProperty balance = new SimpleDoubleProperty(this, "balance", 0);
    private DoubleProperty max = new SimpleDoubleProperty(this, "max", 1);
    private DoubleProperty min = new SimpleDoubleProperty(this, "min", -1);

    /** Creates a default instance with default balance, min, max values. */
    public Balancer() {
        getStyleClass().setAll(STYLECLASS);
        setBalance(getBalance()); // refresh
    }

    /** Creates a default instance with a specified balance value and
     * default min, max values. */
    public Balancer(double balance) {
        this();
        setBalance(balance);
    }

    /**
     * Creates a Balancer instance with specified current, min and max
     * value.
     * @param balance The maximum allowed rating value. <-1,1>
     */
    public Balancer(double balance, double min, double max) {
        this(balance);
        setMax(max);
        setMin(min);
    }

    @Override protected Skin<?> createDefaultSkin() {
        return new BalancerSkin(this);
    }

    /** @return The current balance value. */
    public final DoubleProperty balanceProperty() {
        return balance;
    }

    /**  Returns the current balance value. */
    public final double getBalance() {
        return balance == null ? 0 : balance.get();
    }

    /**  Sets the current balance value. */
    public final void setBalance(double value) {
       double to = value;

       if (to > getMax()) to = getMax();
       if (to < getMin()) to = getMin();

       if (Math.abs(to)<0.2) to = 0;
       if (to-getMin() <0.2) to = getMin();
       if (getMax()-to <0.2) to = getMax();

       balanceProperty().set(to);
    }

    /** The maximum-allowed rating value. */
    public final DoubleProperty maxProperty() {
        return max;
    }

    /**  Sets the maximum-allowed rating value. */
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

    /**  Sets the maximum-allowed rating value. */
    public final void setMin(double value) {
       minProperty().set(value);
    }

    /** Returns the maximum-allowed rating value. */
    public final double getMin() {
        return min.get();
    }



    public class BalancerSkin extends SkinBase<Balancer> {

        // the container for the traditional rating control. If updateOnHover and
        // partialClipping are disabled, this will show a combination of strong
        // and non-strong graphics, depending on the current rating value
        private ImageView backgroundContainer;

        // the container for the strong graphics which may be partially clipped.
        // Note that this only exists if updateOnHover or partialClipping is enabled.
        private ImageView foregroundContainer;
        private Rectangle forgroundClipRect;
        private double balance;

        public BalancerSkin(Balancer b) {
            super(b);

            registerChangeListener(b.balanceProperty(), e -> updateBalance(getSkinnable().getBalance()));
            registerChangeListener(b.maxProperty(), e -> updateClip());
            registerChangeListener(b.minProperty(), e -> updateClip());

            // create graphics
            backgroundContainer = new ImageView();
            backgroundContainer.setPreserveRatio(false);
            backgroundContainer.fitHeightProperty().bind(getSkinnable().prefHeightProperty());
            backgroundContainer.fitWidthProperty().bind(getSkinnable().prefWidthProperty());
            backgroundContainer.getStyleClass().add("balancer-bgr");
            getChildren().setAll(backgroundContainer);

            foregroundContainer = new ImageView();
            foregroundContainer.setPreserveRatio(false);
            foregroundContainer.fitHeightProperty().bind(getSkinnable().prefHeightProperty());
            foregroundContainer.fitWidthProperty().bind(getSkinnable().prefWidthProperty());


            foregroundContainer.getStyleClass().add("balancer-foregr");
            foregroundContainer.setMouseTransparent(true);
            getChildren().add(foregroundContainer);

            forgroundClipRect = new Rectangle();
            foregroundContainer.setClip(forgroundClipRect);

            // install behavior
            // note: use container instead of stylable component. When putting the
            // stylable within a gridPane (for example) its PrefWidth is not what
            // one would expect. As such the balancig with mouse didnt work properly.
            getSkinnable().addEventHandler(MOUSE_DRAGGED, e -> {
                double x = e.getX() - foregroundContainer.getLayoutX();
                if (getSkinnable().contains(x, e.getY())) {
                    double bal = (x/foregroundContainer.getFitWidth()-0.5)*2;
                    getSkinnable().setBalance(bal);
                }
            });
            getSkinnable().addEventHandler(MOUSE_PRESSED, e -> {
                double x = e.getX() - foregroundContainer.getLayoutX();
                if (getSkinnable().contains(x, e.getY())) {
                    double bal = (x/foregroundContainer.getFitWidth()-0.5)*2;
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
            double end =  balance>0 ? w : w/2+(balance+1)*w/2;

            forgroundClipRect.relocate(start,0);
            forgroundClipRect.setWidth(end-start);
            forgroundClipRect.setHeight(h);
        }
    }

}