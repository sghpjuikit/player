package gui.pane;

import java.util.List;

import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;

import static java.lang.Integer.max;
import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import static javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED;
import static javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER;
import static util.async.Async.runLater;
import static util.functional.Util.forEachWithI;
import static util.graphics.Util.setAnchors;

/**
 * Pane displaying a grid of cells - nodes of same size. Similar to
 * {@link javafx.scene.layout.TilePane}. The cells are always of specified size,
 * vertical gap as well, but horizontal gap is adjusted so the cells are laid
 * out in the whole horizontal space.
 */
public class CellPane extends Pane {
	double cellW = 100;
	double cellH = 100;
	double cellG = 5;

	/**
	 * @param cellWidth cell width
	 * @param cellHeight cell height
	 * @param cellGao cell gap. Vertically it will be altered as needed to maintain
	 * layout.
	 */
	public CellPane(double cellWidth, double cellHeight, double cellGao) {
		cellW = cellWidth;
		cellH = cellHeight;
		cellG = cellGao;
	}

	@Override
	protected void layoutChildren() {
		double width = getWidth();
		List<Node> cells = getChildren();

		int elements = cells.size();
		if (elements==0) return;

		int c = (int) floor((width+cellG)/(cellW+cellG));
		int columns = max(1,c);
		double gapX = cellG +(width+cellG -columns*(cellW+cellG))/columns;
		double gapY = cellG;

		forEachWithI(cells, (i,n) -> {
			double x = i%columns * (cellW+gapX);
			double y = i/columns * (cellH+gapY);
			n.relocate(x,y);
			n.resize(cellW, cellH);
		});

		int rows = (int) ceil(elements/(double)columns);

		runLater(()->setPrefHeight(rows*(cellH+gapY)));
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
		setAnchors(s,0d);
		return s;
	}

}