package gui.pane;

import gui.objects.icon.Icon;
import gui.objects.window.stage.Window;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import util.access.V;
import util.animation.Anim;
import util.conf.IsConfig;
import util.reactive.SetƑ;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.RESIZE_BOTTOM_RIGHT;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.*;
import static javafx.util.Duration.millis;
import static main.App.APP;
import static util.graphics.Util.*;
import static util.reactive.Util.maintain;

/**
 * Pane laying 'above' window creating 'overlay' bgr above it.
 * <p/>
 * This implementation has no content. Rather than using {@link StackPane#getChildren()}, use
 * {@link #setContent(javafx.scene.layout.Pane)}, which also applies {@link #CONTENT_STYLECLASS}
 * styleclass on it. Content will align to center unless set otherwise.
 */
public abstract class OverlayPane<T> extends StackPane {

	private static final String IS_SHOWN = "visible";
	private static final String ROOT_STYLECLASS = "overlay-pane";
	private static final String CONTENT_STYLECLASS = "overlay-pane-content";

	/**
	 * Display method.
	 */
	@IsConfig(
			name = "Display method",
			info = "Can be shown per window or per screen. The latter provides more space as window "
					+ "usually does not cover entire screen, but can get in the way of other apps.")
	public final V<Display> display = new V<>(Display.SCREEN_OF_MOUSE);
	/**
	 * Handlers called just after this pane was shown.
	 */
	public final SetƑ onShown = new SetƑ();
	/**
	 * Handlers called just after this pane was hidden.
	 */
	public final SetƑ onHidden = new SetƑ();
	private Pane content;
	private Icon resizeB;

	public OverlayPane() {
		setVisible(false);

		getStyleClass().add(ROOT_STYLECLASS);
		setOnMouseClicked(e -> {
			if (e.getButton()==SECONDARY && isShown()) {
				hide();
				e.consume();
			}
		});
		addEventHandler(KeyEvent.ANY, e -> {
			// close on ESC press
			if (e.getEventType()==KeyEvent.KEY_PRESSED && e.getCode()==ESCAPE && isShown()) {
				hide();
			}
			// prevent events from propagating
			// user should not be able to interact with UI below
			e.consume();
		});

		resizeB = new Icon(RESIZE_BOTTOM_RIGHT).scale(1.5);
		resizeB.setCursor(Cursor.SE_RESIZE);
		resizeB.setVisible(false);
	}

	/**
	 * Shows this pane with given value.
	 *
	 * @param value to be used
	 */
	abstract public void show(T value);

	/**
	 * Shows this pane. The content should be set before calling this method.
	 *
	 * @see #setContent(javafx.scene.layout.Pane)
	 */
	protected void show() {
		if (!isShown()) {
			getProperties().put(IS_SHOWN, IS_SHOWN);
			animStart();
		}
	}

	/** Returns true iff {@link #show()} has been called and {@link #hide()} not yet. */
	public boolean isShown() {
		return getProperties().containsKey(IS_SHOWN);
	}

	/** Hides this pane. */
	public void hide() {
		if (isShown()) {
			getProperties().remove(IS_SHOWN);
			animation.playCloseDo(this::animEnd);
		}
	}

	/**
	 * Sets graphics as content. Only one object can be set as content and any previous content will be cleared.
	 * Consequent invocations with the same parameter have no effect. Null clears the content.
	 *
	 * @param contentRoot new content used instead of the old one or null for none
	 */
	public void setContent(Pane contentRoot) {
		if (contentRoot==null) {
			if (content!=null) {
				getChildren().clear();
				content.getStyleClass().add(CONTENT_STYLECLASS);
				content = null;
			}
		} else {
			content = contentRoot;
			if (!getChildren().contains(content)) {
				getChildren().setAll(content, layStack(resizeB, Pos.BOTTOM_RIGHT));
				content.getStyleClass().add(CONTENT_STYLECLASS);
				resizeB.getParent().setManaged(false);
				maintain(content.paddingProperty(), ((StackPane) resizeB.getParent()).paddingProperty());
				resizeB.getParent().setMouseTransparent(true);
			}
		}
	}

	@Override
	protected void layoutChildren() {
		super.layoutChildren();

		if (getChildren().contains(resizeB.getParent())) {
			resizeB.getParent().resizeRelocate(
					content.getLayoutX(),
					content.getLayoutY(),
					content.getWidth(),
					content.getHeight()
			);
		}
	}

	/**
	 * @return content if any or null if none
	 */
	public Pane getContent() {
		return content;
	}

	public void makeResizableByUser() {
		if (resizeB.isVisible()) return;
		resizeB.setVisible(true);
		new PolarResize().install(resizeB, this, content);
	}

/* ---------- ANIMATION --------------------------------------------------------------------------------------------- */

	private Display displayForHide; // prevents inconsistency in start() and stop(), see use
	private Anim animation = new Anim(30, this::animDo).dur(millis(200)).intpl(x -> x*x); // lowering fps can help on fullHD+ resolutions
	private Stage stg = null;
	private BoxBlur blurBack = new BoxBlur(0, 0, 3);  // we need best possible quality
	private BoxBlur blurFront = new BoxBlur(0, 0, 1); // we do not need quality, hence iterations==1
	private Node opacityNode = null;
	private Node blurFrontNode = null;
	private Node blurBackNode = null;

	private void animStart() {
		displayForHide = display.get();
		display.get().animStart(this);
	}

	private void animDo(double x) {
		display.get().animDo(this, x);
	}

	private void animEnd() {
		// we must use the same display type as the one used
		displayForHide.animEnd(this);
	}

	public enum Display {
		WINDOW, SCREEN_OF_WINDOW, SCREEN_OF_MOUSE;

		private void animStart(OverlayPane op) {
			if (this==WINDOW) {
				APP.windowManager.getActive().ifPresentOrElse(
						window -> {
							// display overlay pane
							AnchorPane root = window.root;
							if (!root.getChildren().contains(op)) {
								root.getChildren().add(op);
								setAnchors(op, 0d);
								op.toFront();
							}
							op.setVisible(true);
							op.requestFocus();     // 'bug fix' - we need focus or key events wont work

							// apply effects (will be updated in animation)
							//
							// blur front performance optimization
							// We want to apply blur on this overlay pane, but that can be potentially
							// giant area (tests show HD and beyond can kill fps here). So we just apply
							// the blur on the content of the overlay pane instead, which is generally
							// smaller.
							// More tweaking:
							// It is possible with the overlay blur to:
							// - disable (does not look the best)
							// - decrease blur iteration count (we don not need super blur quality here)
							// - decrease blur amount
							//
							op.opacityNode = window.content;
							op.blurBackNode = window.subroot;
							if (!op.getChildren().isEmpty()) op.blurFrontNode = op.getChildren().get(0);
							op.blurBackNode.setEffect(op.blurBack);
							op.blurFrontNode.setEffect(op.blurFront);

							// start showing
							op.animation.playOpenDo(null);
							op.onShown.run();
						},
						() -> {
							op.displayForHide = SCREEN_OF_MOUSE;
							SCREEN_OF_MOUSE.animStart(op);
						}
				);
			} else {
				Screen screen = this==SCREEN_OF_WINDOW
						? APP.windowManager.getActive().map(Window::getScreen).orElseGet(() -> getScreen(APP.mouseCapture.getMousePosition()))
						: getScreen(APP.mouseCapture.getMousePosition());
				screenCaptureAndDo(screen, image -> {
					Pane bgr = new Pane();
					bgr.getStyleClass().add("bgr-image");   // replicate app window bgr for style & consistency
					ImageView contentImg = new ImageView(image); // screen screenshot
					StackPane content = new StackPane(bgr, contentImg); // we will use effect on this
					StackPane root = new StackPane(content); // we will inject overlay pane here

					op.stg = createFMNTStage(screen);
					op.stg.setScene(new Scene(root));

					// display overlay pane
					if (!root.getChildren().contains(op)) {
						root.getChildren().add(op);
						op.toFront();
					}
					op.setVisible(true);
					op.requestFocus();     // 'bug fix' - we need focus or key events wont work

					// apply effects (will be updated in animation)
					op.opacityNode = contentImg;
					op.blurBackNode = contentImg;
					if (!op.getChildren().isEmpty()) op.blurFrontNode = op.getChildren().get(0);
					op.blurBackNode.setEffect(op.blurBack);
					op.blurFrontNode.setEffect(op.blurFront);

					op.animation.applier.accept(0d);
					op.stg.show();
					op.stg.requestFocus();

					// start showing
					op.animation.playOpenDo(null);
					op.onShown.run();
				});
			}
		}

		private void animDo(OverlayPane op, double x) {
			if (op.opacityNode==null) return; // bug fix, not 100% sure why it is necessary

			op.opacityNode.setOpacity(1 - x*0.5);
			op.setOpacity(x);
			// un-focus bgr
			op.blurBack.setHeight(15*x*x);
			op.blurBack.setWidth(15*x*x);
			op.opacityNode.setScaleX(1 - 0.02*x);
			op.opacityNode.setScaleY(1 - 0.02*x);
			// focus this
			op.blurFront.setHeight(20*(1 - x*x));
			op.blurFront.setWidth(20*(1 - x*x));
			// zoom in effect - make it appear this pane comes from the front
			op.setScaleX(1 + 2*(1 - x));
			op.setScaleY(1 + 2*(1 - x));
		}

		private void animEnd(OverlayPane op) {
			op.opacityNode.setEffect(null);
			op.blurFrontNode.setEffect(null);
			op.blurBackNode.setEffect(null);
			op.opacityNode = null;
			op.blurFrontNode = null;
			op.blurBackNode = null;
			op.onHidden.run();

			if (this==WINDOW) {
				op.setVisible(false);
			} else {
				op.stg.close();
			}
		}
	}

	private class PolarResize {
		boolean isActive = false;
		Point2D offset = null;

		/**
		 * @param dragActivator some child node or null to use corner of the resizable
		 * @param eventEmitter the node that has mouse event handlers installed
		 * @param resizable the node that will resize
		 */
		public void install(Node dragActivator, Node eventEmitter, Pane resizable) {
			resizable.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
			resizable.addEventHandler(MOUSE_PRESSED, e -> {
				if (dragActivator!=null) {
					// drag by a resizable Node
					if (dragActivator.contains(resizeB.sceneToLocal(e.getSceneX(), e.getSceneY()))) {
						isActive = true;
						offset = new Point2D(resizable.getWidth(), resizable.getHeight()).subtract(resizable.sceneToLocal(e.getSceneX(), e.getSceneY()));
					}
				} else {
					// drag by corner
					double cornerSize = 30;
					Pane n = (Pane) e.getSource();
					if (e.getX()>=n.getWidth() - cornerSize && e.getY()>=n.getHeight() - cornerSize) {
						isActive = true;
						offset = new Point2D(resizable.getWidth(), resizable.getHeight()).subtract(resizable.sceneToLocal(e.getSceneX(), e.getSceneY()));
					}
				}
			});
			eventEmitter.addEventHandler(MOUSE_RELEASED, e -> isActive = false);
			eventEmitter.addEventHandler(MOUSE_DRAGGED, e -> {
				if (isActive) {
					Pane n = (Pane) e.getSource();
					resizable.setPrefSize(
							2*(e.getX() + offset.getX() - n.getLayoutBounds().getWidth()/2),
							2*(e.getY() + offset.getY() - n.getLayoutBounds().getHeight()/2)
					);
					// resizable.setMaxSize(getContent().getPrefWidth(), getContent().getPrefHeight());
					e.consume();
				}
			});
		}
	}
}