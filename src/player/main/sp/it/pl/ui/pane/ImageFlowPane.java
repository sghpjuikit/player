package sp.it.pl.ui.pane;

import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.layout.Pane;
import sp.it.pl.ui.objects.image.Thumbnail;
import static java.lang.Double.min;

/**
 * Pane displaying an image and content - another pane.
 * <p/>
 * The image and content are laid out horizontally or vertically both occupying
 * either total height or width of this pane, depending on
 * the aspect ratio of this pane and that of the image. The image attempts to
 * 'expand' as much as possible (taking aspect ratios into consideration), while
 * the remaining space is left for the content.
 * <p/>
 * Content can have minimum size set, which forbids expanding the image if it
 * would shrink the content beyond it.
 * <p/>
 * Both image and content can be set invisible. Gap between image and pane can
 * be set.
 */
public class ImageFlowPane extends Pane {

	private boolean showImage = true;
	private boolean showContent = true;
	private double gap = 0;
	private double minContentWidth = 0;
	private double minContentHeight = 0;

	private Thumbnail image;
	private Pane content;

	public ImageFlowPane(Thumbnail image, Pane content) {
		setImage(image);
		setContent(content);
	}

	public void setImage(Thumbnail i) {
		if (image==i) return;
		if (image!=null) getChildren().remove(image.getPane());
		image = i;
		if (image!=null) {
			getChildren().add(image.getPane());
			image.ratioImgProperty().addListener((o, ov, nv) -> layoutChildren());
		}
		requestLayout();
	}

	public void setContent(Pane i) {
		if (content==i) return;
		if (content!=null) getChildren().remove(content);
		content = i;
		if (i!=null) getChildren().add(i);
		requestLayout();
	}

	/** Set image visibility. Default true. */
	public void setImageVisible(boolean b) {
		if (showImage==b) return;
		showImage = b;
		requestLayout();
	}

	/** Set content visibility. Default true. */
	public void setContentVisible(boolean val) {
		if (showContent==val) return;
		showContent = val;
		requestLayout();
	}

	/** Set gap between image and content. */
	public void setGap(double g) {
		if (gap==g) return;
		gap = g;
		requestLayout();
	}

	/** Set minimal size of the content. Default 0. */
	public void setMinContentSize(double w, double h) {
		if (minContentWidth==w && minContentHeight==h) return;
		minContentWidth = w;
		minContentHeight = h;
		requestLayout();
	}

	@Override
	protected void layoutChildren() {
		if (image!=null) image.getPane().setVisible(showImage);
		if (content!=null) content.setVisible(showContent);

		double pl = getPadding().getLeft();
		double pr = getPadding().getRight();
		double pt = getPadding().getTop();
		double pb = getPadding().getBottom();
		double W = getWidth() - pl - pr;
		double H = getHeight() - pt - pb;

		if (showImage && showContent && image!=null && content!=null) {
			double imgRatio = image.getRatioImg();
			double thisRatio = W/H;
			boolean isHorizontal = thisRatio>imgRatio;
			if (isHorizontal) {
				double imgW = min(imgRatio*H, W - minContentWidth);

				image.getPane().setLayoutX(pl);
				image.getPane().setLayoutY(pt);
				image.getPane().setMinSize(imgW, H);
				image.getPane().setPrefSize(imgW, H);
				image.getPane().setMaxSize(imgW, H);
				content.setLayoutX(imgW + gap + pl);
				content.setLayoutY(pt);
				content.setMinSize(W - imgW - gap, H);   // why min & max are needed is not clear
				content.setPrefSize(W - imgW - gap, H);
				content.setMaxSize(W - imgW - gap, H);
			} else {
				double imgH = min(W/imgRatio, H - minContentHeight);
				image.getPane().setLayoutX(pl);
				image.getPane().setLayoutY(pt);
				image.getPane().setMinSize(W, imgH);
				image.getPane().setPrefSize(W, imgH);
				image.getPane().setMaxSize(W, imgH);
				content.setLayoutX(pl);
				content.setLayoutY(imgH + gap + pt);
				content.setMinSize(W, H - gap - imgH);
				content.setPrefSize(W, H - gap - imgH);
				content.setMaxSize(W, H - gap - imgH);
			}
		}
		if ((!showImage || image==null) && showContent && content!=null) {
			layoutInArea(content, 0 + pl, 0 + pt, W - pl - pr, H - pt - pb, 0, HPos.CENTER, VPos.CENTER);
		}
		if (showImage && image!=null && (!showContent || content==null)) {
			layoutInArea(image.getPane(), 0 + pl, 0 + pt, W - pl - pr, H - pt - pb, 0, HPos.CENTER, VPos.CENTER);
		}
		// if ((!showImage && !showContent) || (image==null && content==null)) {}

		super.layoutChildren();
	}

}