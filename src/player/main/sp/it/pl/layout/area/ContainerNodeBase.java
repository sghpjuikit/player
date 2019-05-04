/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sp.it.pl.layout.area;

import javafx.scene.layout.AnchorPane;
import sp.it.pl.gui.objects.window.stage.Window;
import sp.it.pl.layout.Component;
import sp.it.pl.layout.container.Container;
import sp.it.pl.layout.container.bicontainer.BiContainer;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.WidgetLoader;
import sp.it.pl.main.AppAnimator;
import sp.it.util.access.ref.LazyR;
import static javafx.scene.input.MouseButton.SECONDARY;
import static sp.it.util.functional.UtilKt.runnable;
import static sp.it.util.reactive.UtilKt.syncC;
import static sp.it.util.ui.UtilKt.pseudoclass;

public abstract class ContainerNodeBase<C extends Container<?>> implements ContainerNode {

    protected final C container;
    protected final AnchorPane root_ = new AnchorPane();
    protected LazyR<ContainerAreaControls> controls = new LazyR<>(this::buildControls);
    protected boolean isLayoutMode = false;
    protected boolean isContainerMode = false;

    public ContainerNodeBase(C container) {
        this.container = container;

        root_.getStyleClass().add("container-area");

	    // report component graphics changes
	    syncC(root_.parentProperty(), v -> IOLayer.allLayers.forEach(it -> it.requestLayout()));
	    syncC(root_.layoutBoundsProperty(), v -> IOLayer.allLayers.forEach(it -> it.requestLayout()));

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
	    return new ContainerAreaControls(this);
    }

    @Override
    public AnchorPane getRoot() {
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
        if (b==false) controls.get().a.setOnFinished(e -> {
        	controls.get().disposer.invoke();
	        controls = new LazyR<>(this::buildControls);
        });
    }

    private void toggleAbsSize() {
		Container c = container;
		if (c instanceof BiContainer) {
		    BiContainerArea s = ((BiContainer) c).ui;
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