/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.pane;

import gui.objects.Thumbnail.Thumbnail;
import static java.lang.Double.min;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.layout.Pane;

/**
 * Pane displaying an image and content - another pane.
 * <p>
 * The image and content are laid out horizontally or vertically both occupying
 * either total height or width of this pane, depending on
 * the aspect ratio of this pane and that of the image. The image attemps to
 * 'expand' as much as possible (taking aspect ratios into consideration), while
 * the remaining space is left for the content.
 * <p>
 * Content can have minimum size set, which forbids expanding the image if it
 * would shrink the content beyond it.
 * <p>
 * Both image and content can be
 * set invisible. Gap between image and pane can be set.
 *
 * @author Plutonium_
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
        if(image!=null) getChildren().remove(image.getPane());
        image = i;
        if(image!=null) {
            getChildren().add(image.getPane());
            image.ratioImgProperty().addListener((o,ov,nv) -> layout());
        }
        layout();
    }
    public void setContent(Pane i) {
        if(content!=null) getChildren().remove(content);
        content = i;
        if(i!=null) getChildren().add(i);
        layout();
    }
    
    public void setImageVisible(boolean b) {
       showImage = b;
       layout();
    }
    public void setContentVisible(boolean val) {
        showContent = val;
        layout();
    }
    /** Set gap between image and content. */
    public void setGap(double g) {
        gap = g;
        layout();
    }
    
    public void setMinCOntentSize(double w, double h) {
        minContentWidth = w;
        minContentHeight = h;
        layout();
    }
    
    
    @Override
    protected void layoutChildren() {
        if(image!=null) image.getPane().setVisible(showImage);
        if(content!=null) content.setVisible(showContent);
        
        double W = getWidth();
        double H = getHeight();
        double Pl = 0;//getPadding().getLeft();
        double Pr = 0;//getPadding().getRight();
        double Pt = 0;//getPadding().getTop();
        double Pb = 0;//getPadding().getBottom();
        
        if(showImage && showContent && image!=null && content!=null) {
            double imgRatio = image.getRatioImg();
            double thisRatio = W/H;
            boolean isHorizontal = thisRatio > imgRatio;
            if(isHorizontal) {
                double h = H;
                double imgW = min(imgRatio*h,W-minContentWidth);
                
                image.getPane().setLayoutX(0);
                image.getPane().setLayoutY(0);
//                image.getPane().setMinSize(imgW, h);
                image.getPane().setPrefSize(imgW, h);
//                image.getPane().setMaxSize(imgW, h);
                content.setLayoutX(imgW+gap);
                content.setLayoutY(0);
                content.setMinSize(W-imgW-gap,h);
                content.setPrefSize(W-imgW-gap,h);
                content.setMaxSize(W-imgW-gap,h);
            } else {
                double w = W;
                double imgH = min(w/imgRatio,H-minContentHeight);
                image.getPane().setLayoutX(0);
                image.getPane().setLayoutY(0);
//                image.getPane().setMinSize(w, imgH);
                image.getPane().setPrefSize(w, imgH);
//                image.getPane().setMaxSize(w, imgH);
                content.setLayoutX(0);
                content.setLayoutY(imgH+gap);
                content.setMinSize(w,H-gap-imgH);
                content.setPrefSize(w,H-gap-imgH);
                content.setMaxSize(w,H-gap-imgH);
            }
        }
        if((!showImage || image==null) && showContent && content!=null) {
            layoutInArea(content, 0+Pl,0+Pt,W-Pl-Pr,H-Pt-Pb, 0, HPos.CENTER, VPos.CENTER);
        }
        if(showImage && image!=null && (!showContent || content==null)) {
            layoutInArea(image.getPane(), 0+Pl,0+Pt,W-Pl-Pr,H-Pt-Pb, 0, HPos.CENTER, VPos.CENTER);
        }
        if((!showImage && !showContent) || (image==null && content==null)) {}
        
        super.layoutChildren();
    }

}
