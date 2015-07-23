/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.pane;

import static java.lang.Integer.max;
import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import static javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED;
import static javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER;
import javafx.scene.layout.Pane;
import static util.Util.setAnchors;
import static util.async.Async.runLater;
import static util.functional.Util.forEachWithI;

/**
 * Pane displaying a grid of cells - nodes of same size. Similar to 
 * {@link javafx.scene.layout.TilePane}. The cells are always of specified size,
 * vertical gap as well, but horizontal gap is adjusted so the cells are laid
 * out in the whole horizontal space.
 * 
 * @author Plutonium_
 */
public class CellPane extends Pane {
    double cellw = 100;
    double cellh = 100;
    double cellg = 5;

    /**
     * @param cellw cell width
     * @param cellh cell height
     * @param gap cell gap. Vertically it will be altered as needed to maintain
     * layout.
     */
    public CellPane(double cellw, double cellh, double gap) {
        this.cellw = cellw;
        this.cellh = cellh;
        this.cellg = gap;
    }

    @Override
    protected void layoutChildren() {
        double width = getWidth();
        List<Node> cells = getChildren();

        int elements = cells.size();
        if(elements==0) return;

        int c = (int) floor((width+cellg)/(cellw+cellg));
        int columns = max(1,c);
        double gapx = cellg+(width+cellg-columns*(cellw+cellg))/columns;
        double gapy = cellg;

        forEachWithI(cells, (i,n) -> {
            double x = i%columns * (cellw+gapx);
            double y = i/columns * (cellh+gapy);
            n.relocate(x,y);
            n.resize(cellw, cellh);
        });

        int rows = (int) ceil(elements/(double)columns);

        runLater(()->setPrefHeight(rows*(cellh+gapy)));
    }
    
    /** Puts this pane to scrollbar and returns it. */
    public ScrollPane scrollable() {
        ScrollPane s = new ScrollPane();
        s.setContent(this);
        s.setFitToWidth(true);
        s.setFitToHeight(false);
        s.setHbarPolicy(NEVER);
        s.setVbarPolicy(AS_NEEDED);
        getChildren().add(s);
        setAnchors(s,0);
        return s;
    }

}