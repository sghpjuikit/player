/*
 * Implementation based on ControlsFX
 *
 * Copyright (c) 2014, 2015, ControlsFX
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of ControlsFX, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL CONTROLSFX BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package sp.it.pl.gui.objects.popover;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.PopupControl;
import javafx.scene.input.MouseEvent;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import sp.it.pl.gui.objects.window.stage.WindowBase;
import sp.it.pl.util.SwitchException;
import sp.it.pl.util.access.V;
import sp.it.pl.util.animation.Anim;
import sp.it.pl.util.math.P;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static javafx.scene.input.MouseEvent.MOUSE_RELEASED;
import static javafx.stage.WindowEvent.WINDOW_HIDING;
import static sp.it.pl.gui.objects.popover.ScreenUse.APP_WINDOW;
import static sp.it.pl.main.AppUtil.APP;
import static sp.it.pl.util.dev.Util.noØ;
import static sp.it.pl.util.graphics.UtilKt.getCentre;
import static sp.it.pl.util.graphics.UtilKt.getScreen;
import static sp.it.pl.util.graphics.UtilKt.setScaleXYByTo;
import static sp.it.pl.util.math.Util.millis;

/**
 * Customized popup window with enhanced functionalities and customizations.
 * <br><p/>
 * The popover can be queried for content safely because of the generic parameter
 * utilizing {@link #getContentNode()}
 * <br><p/>
 * The popover can be queried for skin (see {@link #getSkinn()}). The skin
 * should extend {@link PopOverSkin} and has additional methods that help with
 * the visual side of the popover.
 * <br><p/>
 * The popup window has a very lightweight appearance (no default
 * window decorations) and an arrow pointing at the owner. Due to the nature of
 * popup windows the PopOver will move around with the parent window when the
 * user drags it. <br>
 * PopOver controls are automatically resizing themselves when the content node
 * changes its size.
 * <br><p/><pre>
 * The popover has two modes of behavior marked with detached property.
 * Detached popover:
 *  - has no arrow
 *  - can be dragged and moved
 *  - contains a header with title and some controls.
 *  - autohide off
 * </pre>
 * It is a draggable more standalone window compared to
 * normal mode which comes close to tooltip in terms of functionality.
 * <p/>
 * The header can still be visible in normal mode. To be precise, the header is
 * visible when popover is detached or when at least one of the following is not
 * empty: header icons list, title text.
 * <p/>
 * The close button however appears only in detached mode. This is because in
 * normal mode autohide is usually on. Therefore in normal mode, close button is
 * substituted with autohide icon which allows setting autohiding on/off. It
 * 'pins' the popover to the screen.
 * <br><p/><p/>
 * Popover can become
 * detached when dragged by mouse but only if it is detachable. Detaching is not
 * reversible, respectively it is only during the initial mouse drag that detached
 * the popover - if the popover is returned to the original position.
 * <br><p/><p/>
 * There is a notion of owner node and owner window. The popover tries to
 * act as a child to the Node and Window.
 * <p/>
 * Popover will move with the owner as if locked onto its position. The relocation
 * takes place when Node owner changes location within its parent or when window
 * owner changes its location within the screen or its size.
 * The popover
 * will hide when window owner hides or when node owner is detached from the
 * scene graph (specifically when its Scene changes) or when node owner's turns
 * invisible.
 * <p/>
 * Both are set automatically when any of the show() methods is called depending
 * on the parameter of the show method. If Node is specified, it will become an
 * owner node and the window the node is within will become owner window. If
 * only window is specified it becomes owner window and owner node is left empty.
 * <br><p/><p/>
 * There is a lot of use cases for popover. The content can vary greatly from
 * simple text or icon to a very complex components in which case it is easier
 * to think of the popover as standalone window.
 * <p/>
 * Below are some of the expected use cases along with the recommended
 * configurations:
 * <pre>
 * Informative popover with static content - more sophisticated tooltip
 *  - autohide : true
 *  - hide on click : true
 *  - move with owner : optional
 *  - tip: hide when owner receives any kind of event
 *  - hiding immediately or disabling animations might be considered
 *  - detachable : false
 *  - detached : false or true with no arrow
 *
 * Variation tips:
 *  - autohide : optional but inverse with : close on click (see {@link #setHideOnClick(boolean)})
 *  - hide on click : see above or depending on content
 *  - move with owner : true
 *
 * Small content reactive popover - an alternative to context menus or default popups
 *  - autohide : true
 *  - hide on click : false or false with custom implementation through popover's content's mouse click event handler
 *  - move with owner : true
 *  - detachable : true
 *  - detached : false
 *
 * Sophisticated content - alternative to full fledged window
 *  - autohide : false
 *  - hide on click : false
 *  - move with owner : false
 *  - arrow visible : false or depending on context
 *  - detached : true or depending on context
 * </pre>
 * <br><p/>
 * The popover supports nested instances - popovers opened from other
 * popovers. However the owning window will be the owning window of the parent
 * popup, not the popup itself. Although Node inside a popup can be an owner node
 * for another popup.
 *
 * @param <N> Type of Content.
 */
public class PopOver<N extends Node> extends PopupControl {

	public static final ObservableList<PopOver<?>> active_popups = FXCollections.observableArrayList(new ArrayList<>());
	private static final Object CLOSE_OWNER = new Object();
	private static final String STYLE_CLASS = "popover";

	/**
	 * Creates a pop over with a label as the content node.
	 * Sets autoFix and consumeAutoHidingEvents to false.
	 */
	public PopOver() {
		super();

		// we need to make the skin or getSkin() throws null if we call i too
		// soon - improve this if possible
		setSkin(createDefaultSkin());

		getStyleClass().add(STYLE_CLASS);
		setConsumeAutoHidingEvents(false);
		setAutoFix(false);
	}

	/**
	 * Creates a pop over with the given node as the content node.
	 * Sets autoFix and consumeAutoHidingEvents to false.
	 *
	 * @param content shown by the pop over
	 */
	public PopOver(N content) {
		this();
		setContentNode(content);
	}

	/**
	 * Creates a pop over with the given node as the content node and provided
	 * title text.
	 * Sets autoFix and consumeAutoHidingEvents to false.
	 *
	 * @param content shown by the pop over
	 */
	public PopOver(String title, N content) {
		this();
		this.title.set(title);
		setContentNode(content);
	}

	@Override
	public final PopOverSkin createDefaultSkin() {
		return new PopOverSkin(this);
	}

	/**
	 * Type safe alternative to {@link #getSkin()} which should be avoided.
	 *
	 * @return skin
	 */
	public PopOverSkin getSkinn() {
		return (PopOverSkin) getSkin();
	}

	private final ObjectProperty<N> contentNode = new SimpleObjectProperty<>(this, "contentNode") {
		@Override
		public void setValue(N node) {
			noØ(node);
			super.setValue(node);
		}
	};

	/**
	 * Returns the content shown by the pop over.
	 *
	 * @return the content node property
	 */
	public final ObjectProperty<N> contentNodeProperty() {
		return contentNode;
	}

	/**
	 * Returns the content of this popover as previously set with
	 * {@link #setContentNode(javafx.scene.Node)}.
	 *
	 * @return the content node
	 * @see #contentNodeProperty()
	 */
	public final N getContentNode() {
		return contentNodeProperty().get();
	}

	/**
	 * Sets the content. There can only be one content. Old content will be
	 * removed.
	 *
	 * @param content the new content node value
	 * @throws NullPointerException if param null.
	 * @see #contentNodeProperty()
	 */
	public final void setContentNode(N content) {
		contentNodeProperty().set(content);
	}

/* ------------------------------------------------------------------------------------------------------------------ */

	/**
	 * Hides this popup if it is not detached. Otherwise does nothing.
	 * Equivalent to: if (!isDetached()) hideStrong();
	 * @implNote implementation should not close detached popup
	 */
	@Override
	public void hide() {
		if (!detached.get()) hideStrong();
	}

	/**
	 * Hides this popup. If is animated, the hiding animation will be set off,
	 * otherwise happens immediatelly.
	 */
	public void hideStrong() {
		if (animated.get()) fadeOut();
		else hideImmediately();
	}

	/**
	 * Hides this popup immediatelly.
	 * Use when the hiding animation is not desired (regardless the set value)
	 * or could cause problems such as delaying application exit.
	 *
	 * @implSpec This is the default hide implementation overriding the one in the super class. It should not cause any
	 * delays and keep changes to hiding mechanism to minimum as otherwise it could cause problems. For example casting
	 * javafx.stage.Window to PopOver (when it in fact is an instance of this class) and calling modified hide() method
	 * has been observed to cause serious problems.
	 */
	public void hideImmediately() {
		active_popups.remove(this);
		uninstallMoveWith();
		super.hide();
		if (getProperties().containsKey(CLOSE_OWNER)) ((Stage) getOwnerWindow()).close();
		getProperties().remove(CLOSE_OWNER);
	}

	/******************************************************************************/

	private void showThis(Node ownerNode, Window ownerWindow) {
		noØ(ownerWindow);
		Screen s = getScreen(getCentre(ownerWindow));
		setMaxWidth(s.getBounds().getWidth());
		setMaxHeight(s.getBounds().getHeight());

		// we must set the owners so moving with owner behavior knows which
		// mode should be done
		if (ownerNode!=null) {
			this.ownerNode = ownerNode;
			this.ownerWindow = ownerNode.getScene().getWindow();
			if (this.ownerWindow instanceof PopOver) {
				setParentPopup((PopOver) this.ownerWindow);
			}
		} else this.ownerWindow = ownerWindow;

		detached.set(false);

		// show the popup
		super.show(ownerWindow, 0, 0);
		active_popups.add(this);

		// initialize moving with owner behavior to respect set value
		initializeMovingBehavior(move_with_owner);

		// hideOnESC calls hide(), so there is no effect in detached mode, fix here
		getScene().addEventHandler(KEY_PRESSED, e -> {
			if (e.getCode()==ESCAPE && isHideOnEscape()) {
				hideStrong();
				e.consume();
			}
		});

		if (animated.get()) fadeIn();
	}

	private void position(Supplier<P> position) {
		position(position.get());
	}

	private void position(P pos) {
		if (getArrowSize()>0)
			pos = pos.plus(adjustWindowLocation());

		setX(pos.getX());
		setY(pos.getY());
		deltaThisX = pos.getX();
		deltaThisY = pos.getY();
	}

	/**
	 * Makes the pop over visible at the give location and associates it with
	 * the given owner node. The x and y coordinate will be the target location
	 * of the arrow of the pop over and not the location of the window.
	 *
	 * @param owner the owning node
	 * @param x the x coordinate for the pop over arrow tip
	 * @param y the y coordinate for the pop over arrow tip
	 * @throws NullPointerException if owner param null or is not residing within any {@link Window} - its
	 * getScene().getWindow() must not return null
	 */
	@Override
	public final void show(Node owner, double x, double y) {
		showThis(owner, owner.getScene().getWindow());
		position(() -> {
			Point2D a = owner.localToScreen(x, y);
			double X = a.getX() + owner.getLayoutBounds().getWidth()/2;
			double Y = a.getY() + owner.getLayoutBounds().getHeight()/2;
			return new P(X, Y);
		});
	}

	/**
	 * Shows popup. Equivalent to: show(owner,0,0);
	 */
	public final void show(Node owner) {
		show(owner, 0, 0);
	}

	/** Display at specified designated position relative to node. */
	public void show(Node owner, NodePos pos) {
		showThis(owner, owner.getScene().getWindow());
		position(() -> pos.computeXY(owner, this));
	}

	/** Display at specified screen coordinates */
	@Override
	public void show(Window window, double x, double y) {
		showThis(null, window);
		position(() -> new P(x, y));
	}

	public void show(Window window, P position) {
		show(window, position.getX(), position.getY());
	}

	private static Stage UNFOCUSED_OWNER;

	static {
		Platform.runLater(() -> UNFOCUSED_OWNER = APP.windowManager.createStageOwner());
	}

	/** Display at specified designated screen position */
	public void show(ScreenPos pos) {
		setArrowSize(0); // disable arrow
		Optional<Window> owneR = APP.windowManager.getFocused()
				.filter(w -> pos.isAppCentric())
				.map(WindowBase::getStage);
		Optional<Window> ownerO = owneR.or(() -> Optional.ofNullable(getOwnerWindow()).filter(Window::isShowing));
		boolean isOwnerCreated = !ownerO.isPresent() && focusOnShow.get();
		Window owner = ownerO.orElseGet(() -> focusOnShow.get() ? APP.windowManager.createStageOwner() : UNFOCUSED_OWNER);
		ScreenPos p = normalize(pos, owneR);

		if (isShowing()) hideImmediately();    // showing when shown can get us into trouble -> hide first // TODO: remove

		showThis(null, owner);
		position(() -> p.computeXY(this));

		if (focusOnShow.get()) focus();
		if (!p.isAppCentric()) uninstallMoveWith();
		if (isOwnerCreated) getProperties().put(CLOSE_OWNER, CLOSE_OWNER);
	}

	@Override
	public void show(Window window) {
		showThis(null, window);
		uninstallMoveWith();
	}

	private ScreenPos normalize(ScreenPos pos, Optional<Window> owner) {
		return owner.isPresent() ? pos : pos.toScreenCentric();
	}

	private void focus() {
		Window w = getOwnerWindow();
		if (w!=null && w.isFocused()) w.requestFocus();
		requestFocus();
	}

/* --------------------- MOVE WITH OWNER ---------------------------------------------------------------------------- */

	private boolean move_with_owner = true;

	/**
	 * Sets moving with owner behavior on or off. Default on (true).
	 * <p/>
	 * This functionality is characterized by the popup moving with the owning
	 * Window or Node - its positioning relative to its owner becomes absolute.
	 * <p/>
	 * There are two 'modes' decided automatically depending on the way this
	 * popover was shown. The position can be absolute relative to the Node or
	 * relative to the Window (or none). The first will reposition this popup
	 * in order for it to stay with the Node (even when the Node only changes
	 * its position within the Window that remained static). Second simply
	 * follows the Window and the third does nothing.
	 * <p/>
	 * The 'mode' is decided based on the parameters of the show method that was
	 * called.
	 * Must never be called before the popover is shown.
	 */
	public void setMoveWithOwner(boolean val) {
		// avoid wasteful operation, continue only if values differ
		if (val!=move_with_owner) {
			move_with_owner = val;

			// if is showing the behavior (one way or the other) was already initialized
			// and we need to change it. Otherwise, it needs to be applied from
			// show method as the initialisation can not precede show method
			// The behavior is uninitialized on hide so it will not be started twice
			if (isShowing()) {
				initializeMovingBehavior(val);
			}
		}
	}

	public boolean isMoveWithOwner() {
		return move_with_owner;
	}

	private void initializeMovingBehavior(boolean val) {
		noØ(ownerWindow, "Error, popup window owner null while isShowing. Cant apply moving with owner behavior");

		// install/uninstall correct 'mode'
		if (val) {
			if (ownerNode!=null) installMoveWithNode(ownerNode); // node mode
			else installMoveWithWindow(ownerWindow);            // window mode
		} else {
			uninstallMoveWith();    // uninstalls both
		}
	}

	// move with handling -------
	// owners
	private Node ownerNode = null;
	private Window ownerWindow = null;

	// variables for dragging behavior
	private double deltaX = 0;
	private double deltaY = 0;
	private double deltaThisX = 0;
	private double deltaThisY = 0;
	private boolean deltaThisLock = false;

	// listeners for relocating
	private final ChangeListener<Number> xListener = (o, oldX, newX) -> {
		if (ownerNode!=null)
			setX(deltaThisX + ownerNode.localToScreen(0, 0).getX() - deltaX);
	};
	private final ChangeListener<Number> yListener = (o, oldY, newY) -> {
		if (ownerNode!=null)
			setY(deltaThisY + ownerNode.localToScreen(0, 0).getY() - deltaY);
	};
	private final ChangeListener<Number> winXListener = (o, oldX, newX) -> {
		if (ownerWindow!=null)
			setX(deltaThisX + ownerWindow.getX() - deltaX);
	};
	private final ChangeListener<Number> winYListener = (o, oldY, newY) -> {
		if (ownerWindow!=null)
			setY(deltaThisY + ownerWindow.getY() - deltaY);
	};

	private void installMoveWithWindow(Window owner) {
		ownerNode = null;
		ownerWindow = owner;
		ownerWindow.xProperty().addListener(winXListener);
		ownerWindow.yProperty().addListener(winYListener);
		ownerWindow.widthProperty().addListener(winXListener);
		ownerWindow.heightProperty().addListener(winYListener);
		// uninstall just before window gets hidden to prevent possible illegal states
		ownerWindow.addEventFilter(WINDOW_HIDING, e -> visibilityListener.changed(null, null, null));
		// remember owner's position to monitor its position change
		deltaX = owner.getX();
		deltaY = owner.getY();
		installMonitoring();
	}

	private void installMoveWithNode(Node owner) {
		ownerNode = owner;
		ownerWindow = ownerNode.getScene().getWindow();
		ownerWindow.xProperty().addListener(xListener);
		ownerWindow.yProperty().addListener(yListener);
		ownerWindow.widthProperty().addListener(xListener);
		ownerWindow.heightProperty().addListener(yListener);
		ownerNode.layoutXProperty().addListener(xListener);
		ownerNode.layoutYProperty().addListener(yListener);
		// uninstall when Node is disconnected from scene graph to prevent possible illegal states
		ownerNode.sceneProperty().addListener(visibilityListener);
		ownerNode.visibleProperty().addListener(visibilityListener2);
		// remember owner's position to monitor its position change
		deltaX = ownerNode.localToScreen(0, 0).getX();
		deltaY = ownerNode.localToScreen(0, 0).getY();
		installMonitoring();
	}

	private void uninstallMoveWith() {
		if (ownerWindow!=null) {
			ownerWindow.xProperty().removeListener(winXListener);
			ownerWindow.yProperty().removeListener(winYListener);
			ownerWindow.widthProperty().removeListener(winXListener);
			ownerWindow.heightProperty().removeListener(winYListener);
		}
		if (ownerNode!=null) {
			ownerNode.layoutXProperty().removeListener(xListener);
			ownerNode.layoutYProperty().removeListener(yListener);
			ownerWindow.xProperty().removeListener(xListener);
			ownerWindow.yProperty().removeListener(yListener);
			ownerWindow.widthProperty().removeListener(xListener);
			ownerWindow.heightProperty().removeListener(yListener);
			ownerNode.sceneProperty().removeListener(visibilityListener);
			ownerNode.visibleProperty().removeListener(visibilityListener2);
		}
		ownerNode = null;
		ownerWindow = null;
		uninstallMonitoring();
	}

	// monitoring ----------
	// turn lock on/off to prevent dragging to change state
	EventHandler<MouseEvent> lockOnHandler = e -> deltaThisLock = true;
	EventHandler<MouseEvent> lockOffHandler = e -> deltaThisLock = false;
	// remember position for dragging functionality
	InvalidationListener deltaXListener = o -> { if (deltaThisLock) deltaThisX = getX(); };
	InvalidationListener deltaYListener = o -> { if (deltaThisLock) deltaThisY = getY(); };
	// hide on scene change
	ChangeListener<Scene> visibilityListener = (o, ov, nv) -> {
		uninstallMoveWith();
		hideStrong();
	};
	// hide on node owoner setVisible(false)
	ChangeListener<Boolean> visibilityListener2 = (o, ov, nv) -> {
		uninstallMoveWith();
		hideStrong();
	};

	private void installMonitoring() {
		// this should have been handled like this:
		//     deltaThisX.bind(Bindings.when(deltaThisLock).then(xProperty()).otherwise(deltaThisX));
		// but the binding doesn seem to allow binding to itself or simply 'not
		// do' anything in the 'otherwise' part - there we need to maintain the
		// current value but it does not seem possible - it would really clean
		// this up a bit (way too many listeners for my taste)

		// maintain lock to prevent illegal position monitoring
		getScene().addEventHandler(MOUSE_PRESSED, lockOnHandler);
		getScene().addEventHandler(MOUSE_RELEASED, lockOffHandler);
		// monitor this' position for change if lock allows
		xProperty().addListener(deltaXListener);
		yProperty().addListener(deltaYListener);
		// fire listeners to initialize the values (listeners dont get fired when added)
		deltaXListener.invalidated(xProperty());
		deltaYListener.invalidated(yProperty());
		deltaThisX = getX();
		deltaThisY = getY();
	}

	private void uninstallMonitoring() {
		getScene().removeEventHandler(MOUSE_PRESSED, lockOnHandler);
		getScene().removeEventHandler(MOUSE_RELEASED, lockOffHandler);
		xProperty().removeListener(deltaXListener);
		yProperty().removeListener(deltaYListener);
	}

	/******************************************************************************/

	/* Move the window so that the arrow will end up pointing at the target coordinates. */
	// TODO: fix this computing wrong coordinates
	private P adjustWindowLocation() {
		Bounds bounds = PopOver.this.getSkin().getNode().getLayoutBounds();
		switch (getArrowLocation()) {
			case TOP_CENTER:
			case TOP_LEFT:
			case TOP_RIGHT:
				return new P(
					+ bounds.getMinX() - computeXOffset(),
					+ bounds.getMinY() + getArrowSize()
				);
			case LEFT_TOP:
			case LEFT_CENTER:
			case LEFT_BOTTOM:
				return new P(
					+ bounds.getMinX() + getArrowSize(),
					+ bounds.getMinY() - computeYOffset()
				);
			case BOTTOM_CENTER:
			case BOTTOM_LEFT:
			case BOTTOM_RIGHT:
				return new P(
					+ bounds.getMinX() - computeXOffset(),
					- bounds.getMinY() - bounds.getMaxY() - 1
				);
			case RIGHT_TOP:
			case RIGHT_BOTTOM:
			case RIGHT_CENTER:
				return new P(
					- bounds.getMinX() - bounds.getMaxX() - 1,
					+ bounds.getMinY() - computeYOffset()
				);
			default: throw new SwitchException(getArrowLocation());
		}
	}

	private double computeXOffset() {
		switch (getArrowLocation()) {
			case TOP_LEFT:
			case BOTTOM_LEFT:
				return getCornerRadius() + getArrowIndent() + getArrowSize();
			case TOP_CENTER:
			case BOTTOM_CENTER:
				return getWidth()/2 - getArrowSize();
//				return getContentNode().prefWidth(-1)/2 + getArrowSize() + getArrowIndent();
			case TOP_RIGHT:
			case BOTTOM_RIGHT:
				return getContentNode().prefWidth(-1) - getArrowIndent()
						- getCornerRadius() - getArrowSize();
			default:
				return 0;
		}
	}

	private double computeYOffset() {
		switch (getArrowLocation()) {
			case LEFT_TOP:
			case RIGHT_TOP:
				return getCornerRadius() + getArrowIndent() + getArrowSize();
			case LEFT_CENTER:
			case RIGHT_CENTER:
				return getContentNode().prefHeight(-1)/2 + getArrowSize() + getArrowIndent();
			case LEFT_BOTTOM:
			case RIGHT_BOTTOM:
				return getContentNode().prefHeight(-1) - getCornerRadius()
						- getArrowIndent() - getArrowSize();
			default:
				return 0;
		}
	}

/* ------------------------------------------------------------------------------------------------------------------ */

	/** Whether resizing by user is allowed. */
	public final V<Boolean> userResizable = new V<>(true);

	public ScreenUse screenPreference = APP_WINDOW;

	/******************************************************************************/

	// TODO: make styleable
	private final DoubleProperty arrowSize = new SimpleDoubleProperty(this,"arrowSize", 9);

	/**
	 * Controls the size of the arrow. Default value is 12.
	 * Set to arbitrary positive value or 0 to disable arrow.
	 * <p/>
	 * Disabling arrow
	 * has an effect of not positioning the popover so the arrow points to the
	 * specific point. Rather the popover's upper left corner will.
	 *
	 * @return the arrow size property
	 */
	public final DoubleProperty arrowSizeProperty() {
		return arrowSize;
	}

	/**
	 * Returns the value of the arrow size property.
	 *
	 * @return the arrow size property value
	 * @see #arrowSizeProperty()
	 */
	public final double getArrowSize() {
		return arrowSizeProperty().get();
	}

	/**
	 * Sets the value of the arrow size property.
	 * <p/>
	 * Set to arbitrary positive value or 0 to disable arrow.
	 *
	 * @param size arrow size
	 * @see #arrowSizeProperty()
	 */
	public final void setArrowSize(double size) {
		arrowSizeProperty().set(size);
	}

	// TODO: make styleable
	private final DoubleProperty arrowIndent = new SimpleDoubleProperty(this, "arrowIndent", 12);

	/**
	 * Controls the distance between the arrow and the corners of the pop over.
	 * The default value is 12.
	 *
	 * @return the arrow indent property
	 */
	public final DoubleProperty arrowIndentProperty() {
		return arrowIndent;
	}

	/**
	 * Returns the value of the arrow indent property.
	 *
	 * @return the arrow indent value
	 * @see #arrowIndentProperty()
	 */
	public final double getArrowIndent() {
		return arrowIndentProperty().get();
	}

	/**
	 * Sets the value of the arrow indent property.
	 *
	 * @param size the arrow indent value
	 * @see #arrowIndentProperty()
	 */
	public final void setArrowIndent(double size) {
		arrowIndentProperty().set(size);
	}

	// radius support

	// TODO: make styleable

	private final DoubleProperty cornerRadius = new SimpleDoubleProperty(this,
			"cornerRadius", 6);

	/**
	 * Returns the corner radius property for the pop over.
	 *
	 * @return the corner radius property (default is 6)
	 */
	public final DoubleProperty cornerRadiusProperty() {
		return cornerRadius;
	}

	/**
	 * Returns the value of the corner radius property.
	 *
	 * @return the corner radius
	 * @see #cornerRadiusProperty()
	 */
	public final double getCornerRadius() {
		return cornerRadiusProperty().get();
	}

	/**
	 * Sets the value of the corner radius property.
	 *
	 * @param radius the corner radius
	 * @see #cornerRadiusProperty()
	 */
	public final void setCornerRadius(double radius) {
		cornerRadiusProperty().set(radius);
	}

	private final ObjectProperty<PopOver.ArrowLocation> arrowLocation = new SimpleObjectProperty<>(
			this, "arrowLocation", PopOver.ArrowLocation.LEFT_TOP);

	/**
	 * Stores the preferred arrow location. This might not be the actual
	 * location of the arrow if auto fix is enabled.
	 *
	 * @return the arrow location property
	 * @see #setAutoFix(boolean)
	 */
	public final ObjectProperty<PopOver.ArrowLocation> arrowLocationProperty() {
		return arrowLocation;
	}

	/**
	 * Sets the value of the arrow location property.
	 *
	 * @param location the requested location
	 * @see #arrowLocationProperty()
	 */
	public final void setArrowLocation(PopOver.ArrowLocation location) {
		arrowLocationProperty().set(location);
	}

	/**
	 * Returns the value of the arrow location property.
	 *
	 * @return the preferred arrow location
	 * @see #arrowLocationProperty()
	 */
	public final PopOver.ArrowLocation getArrowLocation() {
		return arrowLocationProperty().get();
	}

	/** All possible arrow locations. */
	public enum ArrowLocation {
		LEFT_TOP, LEFT_CENTER, LEFT_BOTTOM, RIGHT_TOP, RIGHT_CENTER, RIGHT_BOTTOM,
		TOP_LEFT, TOP_CENTER, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
	}

	/**************************** IN/OUT ANIMATIONS *******************************/

	/** Whether is animated. Uses fade in and out animations on show/hide. */
	public V<Boolean> animated = new V<>(true);
	public V<Duration> animationDuration = new V<>(millis(300));
	private final Anim animation = new Anim(p -> {
		getSkinn().getNode().setOpacity(p*p);
		setScaleXYByTo(getSkinn().getNode(), p, -20.0, 0.0);  // TODO: causes slight position shift sometimes
	});

	private void fadeIn() {
		animation.dur(animationDuration.get());
		animation.playOpenDo(null);
	}

	private void fadeOut() {
		animation.dur(animationDuration.get());
		animation.playCloseDo(() -> hideImmediately());
	}

/* ------------------------------------------------------------------------------------------------------------------ */

	private EventHandler<MouseEvent> hideOnClick;

	/** Whether hiding on click behavior is set true. Default false. */
	public boolean isHideOnClick() {
		return hideOnClick!=null;
	}

	/**
	 * Sets closing behavior when receives click event. The detached value is
	 * ignored - popup will always hide. Default false (off).
	 * <p/>
	 * Even if true, this behavior can be prevented when the content of the
	 * popup consumes the MOUSE_CLICKED event.
	 * <p/>
	 * Tip: It should only be used when content is static and does not react
	 * on mouse events - for example in case of informative popups.
	 * Also, it is recommended to use this in conjunction with {@link #setAutoHide(boolean)}
	 * but with inverted value. One might want to have a popup with reactive
	 * content auto-closing when clicked anywhere but on it, or static popup
	 * displaying information hovering above its owner, while the owner can be
	 * used with the popup open but conveniently closing on mouse click.
	 * <p/>
	 * Of course any combination of the values is possible. Completely custom
	 * implementation can be used too using mouse click event handler on the
	 * content.
	 */
	public void setHideOnClick(boolean val) {
		if (val) {
			// construct the handler lazily on demand
			if (hideOnClick==null) {
				hideOnClick = e -> {
					// close only if the popup was not dragged since mouse press
					if (e.isStillSincePress()) hideStrong();
				};
			}
			// set the behavior
			getScene().addEventHandler(MOUSE_CLICKED, hideOnClick);
		} else {
			// remove the behavior but watch out for lazy initialization
			if (hideOnClick!=null) {
				getScene().removeEventHandler(MOUSE_CLICKED, hideOnClick);
				hideOnClick = null;
			}
		}
	}

	private void setParentPopup(PopOver popover) {
		popover.addEventFilter(WindowEvent.WINDOW_HIDING, e -> {
			// same bug as with 'open popups preventing app closing properly' due to owner being closed before
			// the child -> we need to close immediately
			if (isShowing())
				hideImmediately();
		});
	}

	/** Receive focus on shown to be able to receive input events. Use true for interactive content. Default true. */
	public final V<Boolean> focusOnShow = new V<>(true);

	/** Whether the pop over can be detached. */
	public final V<Boolean> detachable = new V<>(true);

	/** Whether this popover is detached so it no longer displays an arrow pointing at the owner node. Default false */
	public final V<Boolean> detached = new V<>(false);

	/** Title text. Default "". */
	public final V<String> title = new V<>("");

	/** Header visibility. Header contains title and icons. Default true. */
	public final V<Boolean> headerVisible = new V<>(true);

	private ObservableList<Node> headerContent = null;

	/**
	 * Modifiable list of children of the header. The elements are Nodes displayed in the header.
	 * The list is modifiable and any changes will be immediately reflected visually.
	 * <p/>
	 * Use to customize header for example by adding icon or buttons for controlling the content of the popup.
	 * <p/>
	 * Note that the order of the items matters. The nodes will be displayed in
	 * the header in the same order as they are in the list. The ascending order
	 * goes from left to right.
	 * <p/>
	 * Tip: add node to specific position by add(int i, element e) and replace
	 * the node with set(int i, element e); called on the returned list.
	 * <p/>
	 * The added node will probably have assigned a mouse click event handler
	 * that executes a click behavior. In such case it is recommended to always
	 * consume the event to prevent popover autohiding features to occur by
	 * stopping the event propagation.
	 *
	 * @return all custom children of the header
	 */
	public ObservableList<Node> getHeaderIcons() {
		if (headerContent==null) headerContent = FXCollections.observableArrayList();
		return headerContent;
	}
}