/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sp.it.pl.layout.area;

import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.pl.gui.objects.window.stage.Window;
import sp.it.pl.layout.Component;
import sp.it.pl.layout.container.Container;
import sp.it.pl.layout.container.bicontainer.BiContainer;
import sp.it.pl.layout.container.freeformcontainer.FreeFormContainer;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.WidgetLoader;
import sp.it.pl.main.AppAnimator;
import sp.it.pl.main.Df;
import sp.it.util.access.ref.LazyR;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.EXCHANGE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.GAVEL;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.LOCK;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.TIMES;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.UNLOCK;
import static javafx.geometry.NodeOrientation.LEFT_TO_RIGHT;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static sp.it.pl.layout.area.Area.PSEUDOCLASS_DRAGGED;
import static sp.it.pl.main.AppBuildersKt.infoIcon;
import static sp.it.pl.main.AppDragKt.contains;
import static sp.it.pl.main.AppDragKt.get;
import static sp.it.pl.main.AppDragKt.installDrag;
import static sp.it.pl.main.AppDragKt.set;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.functional.UtilKt.runnable;
import static sp.it.util.reactive.UtilKt.maintain;
import static sp.it.util.ui.UtilKt.pseudoclass;

public abstract class ContainerNodeBase<C extends Container<?>> implements ContainerNode {

    protected final C container;
    protected final AnchorPane root_ = new AnchorPane();
    protected final LazyR<ContainerAreaControls> controls = new LazyR<>(this::buildControls);
    protected boolean isLayoutMode = false;
    protected boolean isContainerMode = false;

    public ContainerNodeBase(C container) {
        this.container = container;

        root_.getStyleClass().add("container-area");

	    // report component graphics changes
	    maintain(root_.parentProperty(), v -> IOLayer.allLayers.forEach(it -> it.requestLayout()));
	    maintain(root_.layoutBoundsProperty(), v -> IOLayer.allLayers.forEach(it -> it.requestLayout()));

        // switch to container/normal layout mode using right/left click
        root_.setOnMouseClicked(e -> {
	        if (isLayoutMode && !isContainerMode && e.getButton()==SECONDARY) {
	            if (container.getChildren().isEmpty()){
		            AppAnimator.INSTANCE.closeAndDo(root_, runnable(container::close));
	            } else {
	                setContainerMode(true);
	            }
                e.consume();
	        }
        });
    }

    protected ContainerAreaControls buildControls() {
	    var controls = new ContainerAreaControls(this);


	    // build header buttons
	    Icon infoB = infoIcon(
		    "Container settings. See icon tooltips."
			    + "\nActions:"
			    + "\n\tLeft click: visit children"
			    + "\n\tRight click: visit parent container"
	    ).styleclass("header-icon");
	    Icon actB = new Icon(GAVEL, -1, "Actions\n\n"
		    + "Opens action chooser for this container. Browse and run additional non-layout actions "
		    + "for this container.", () ->
		    APP.actionPane.show(Container.class, container)
	    ).styleclass("header-icon");
	    Icon lockB = new Icon(null, -1, "Lock widget layout", () -> {
		    container.locked.set(!container.locked.get());
		    APP.actionStream.invoke("Widget layout lock");
	    }).styleclass("header-icon");
	    maintain(container.locked, it -> lockB.icon(it ? LOCK : UNLOCK));
	    Icon closeB = new Icon(TIMES, -1, "Close widget", () -> {
		    container.close();
		    APP.actionStream.invoke("Close widget");
	    }).styleclass("header-icon");

	    // drag
	    installDrag(controls, EXCHANGE, "Switch components",
		    e -> contains(e.getDragboard(), Df.COMPONENT),
		    e -> get(e.getDragboard(), Df.COMPONENT) == container,
		    consumer(e -> get(e.getDragboard(), Df.COMPONENT).swapWith(container.getParent(), container.indexInParent()))
	    );

	    EventHandler<MouseEvent> dh = e -> {
		    if (e.getButton()==PRIMARY && !(container.getParent() instanceof FreeFormContainer)) {
			    Dragboard db = root_.startDragAndDrop(TransferMode.ANY);
			    set(db, Df.COMPONENT, container);
			    // signal dragging graphically with css
			    controls.pseudoClassStateChanged(PSEUDOCLASS_DRAGGED, true);
			    e.consume();
		    }
	    };

	    controls.setOnDragDetected(dh);
	    // return graphics to normal
	    root_.setOnDragDone(e -> controls.pseudoClassStateChanged(PSEUDOCLASS_DRAGGED, false));

	    controls.icons.setNodeOrientation(LEFT_TO_RIGHT);
	    controls.icons.setAlignment(Pos.CENTER_RIGHT);
	    controls.icons.setPrefColumns(10);
	    controls.icons.setPrefHeight(25);
	    AnchorPane.setTopAnchor(controls.icons,0d);
	    AnchorPane.setRightAnchor(controls.icons,0d);
	    AnchorPane.setLeftAnchor(controls.icons,0d);
	    controls.icons.getChildren().addAll(infoB, lockB, actB, closeB);

	    
	    // switch to container/normal layout mode using right/left click
	    controls.setOnMouseClicked(e -> {
		    if (isContainerMode && e.getButton()==PRIMARY) {
			    setContainerMode(false);
			    e.consume();
		    }
	    });

	    controls.updateIcons();
	    
	    return controls;
    }

    @Override
    public Pane getRoot() {
        return root_;
    }

    @Override
    public void show() {
        // Interesting idea, but turns out it is not intuitive. Most of the time, user
        // simply wants to add child, so this gets in the way.
        // go to container if children empty
        // if (container.getChildren().isEmpty())
        //     setAltCon(true);

        isLayoutMode = true;
	    root_.pseudoClassStateChanged(pseudoclass("layout-mode"), true);

        container.getChildren().values().forEach(c -> {
            if (c instanceof Container) ((Container)c).show();
            if (c instanceof Widget) {
                ContainerNode ct = ((Widget)c).areaTemp;
                if (ct!=null) ct.show();
            }
        });
    }

    @Override
    public void hide() {
        if (isContainerMode) setContainerMode(false);
        isLayoutMode = false;
	    root_.pseudoClassStateChanged(pseudoclass("layout-mode"), false);

        container.getChildren().values().forEach(c -> {
            if (c instanceof Container) ((Container)c).hide();
            if (c instanceof Widget) {
                ContainerNode ct = ((Widget)c).areaTemp;
                if (ct!=null) ct.hide();
            }
        });
    }

    void setContainerMode(boolean b) {
        if (isContainerMode==b) return;
        isContainerMode = b;
        controls.get().toFront();
        controls.get().a.playFromDir(b);
    }

    private void toggleAbsSize() {
		Container c = container;
		if (c instanceof BiContainer) {
		    Splitter s = ((BiContainer) c).ui;
		    s.toggleAbsoluteSizeFor(c.indexInParent());
		}
    }

    // TODO: fix & merge with Area.detach
    public void detach() {
        if (!container.hasParent()) return;

        Component c = container;
        c.getParent().addChild(c.indexInParent(),null);
        Window w = WidgetLoader.WINDOW.INSTANCE.invoke(c);

        w.setSize(root_.getWidth()+10, root_.getHeight()+30);
    }

}