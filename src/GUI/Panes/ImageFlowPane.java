
package GUI.Panes;

import GUI.objects.ImageNode;
import java.io.File;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import utilities.Util;

/**
 *
 * @author uranium
 */
public final class ImageFlowPane {
    public enum ImagePosition {
        LEFT_TOP,
        RIGHT_TOP,
        LEFT_BOTTOM,
        RIGHT_BOTTOM,
        CENTER;
    }
    
    // properties
    private boolean showImage = true;
    private boolean showContent = true;
    private double padding = 0; // border padding
    private double gap = 0; // content <-> image distance
//    private final DoubleProperty minContentWidth = new SimpleDoubleProperty(0);   // bindable width limit of concent
//    private final DoubleProperty minContentHeight = new SimpleDoubleProperty(0);  // bindable height limit of concent
    private final DoubleProperty imageRatio = new SimpleDoubleProperty(1);      // width/height image ratio value
    private final DoubleProperty ratio = new SimpleDoubleProperty(1);           // width/height ratio of THIS
    private final BooleanBinding ratioVer = ratio.lessThanOrEqualTo(imageRatio);// ratio <= imgRatio
    private final BooleanBinding ratioHor = ratio.greaterThan(imageRatio);      // ratio > imgRatio
    
    private ImageFlowPane.ImagePosition imagePosition = ImageFlowPane.ImagePosition.CENTER;
    
    // panes
    private final AnchorPane THIS = new AnchorPane();           // rootPane
    private final BorderPane layout = new BorderPane();         // separates content and image
    private final BorderPane imagePane = new BorderPane();      // wrapps image (helps with aligning)
    private final AnchorPane contentBorder = new AnchorPane();  // solely to support gap, dont use
    private final AnchorPane content = new AnchorPane();        // here goes the content
    private ImageNode image;                                 // this is the image
    
    public ImageFlowPane(AnchorPane parent, ImageNode imgContainer) {System.out.println("CREATING");
        image = imgContainer;

        // layout this whole object onto parent
        parent.getChildren().add(THIS);
        AnchorPane.setBottomAnchor(THIS, 0.0);
        AnchorPane.setTopAnchor(THIS, 0.0);
        AnchorPane.setLeftAnchor(THIS, 0.0);
        AnchorPane.setRightAnchor(THIS, 0.0);
        
        // divide this layout - with BorderPane using CENTER+TOP/DOWN/LEFT/RIGHT
        THIS.getChildren().add(layout);
        AnchorPane.setBottomAnchor(layout, 0.0);
        AnchorPane.setTopAnchor(layout, 0.0);
        AnchorPane.setLeftAnchor(layout, 0.0);
        AnchorPane.setRightAnchor(layout, 0.0);
        
        // wrap content in ContentBorder to create gap - gap is applied on rebind
        contentBorder.getChildren().add(content);
        AnchorPane.setBottomAnchor(content, 0.0); // gap shifts betwween one of these four based on orientation of the layout
        AnchorPane.setTopAnchor(content, 0.0);
        AnchorPane.setLeftAnchor(content, 0.0);
        AnchorPane.setRightAnchor(content, 0.0);
        
        // maintain ratio
        ratio.bind(THIS.widthProperty().divide(THIS.heightProperty()));
        // rebind when ratios flip
        ratioVer.addListener((a,b,c)->layout());
        ratioHor.addListener((a,b,c)->layout());
        
        // initialize basic layout values
        THIS.setMinSize(0, 0);
        contentBorder.setMinSize(0, 0);
        content.setMinSize(0, 0);
        layout.setMinSize(0, 0);
        imagePane.setMinSize(0, 0);
        imagePane.setCenter(image.getPane());
//        setImagePosition(ImagePosition.LEFT_BOTTOM); // holy shit another WTF error
        layout.setCenter(contentBorder);
        

//        layout(); // unnecessary
//        System.out.println("parent width"+parent.getWidth());
//        System.out.println("This width"+THIS.getWidth());
//        System.out.println("layout width"+layout.getWidth());
//        System.out.println("contentBorder width"+contentBorder.getWidth());
//        System.out.println("content width"+content.getWidth());
    }
    
    
    public Image getImage() {
        return image.getImage();
    }
    public void setImage(Image img) {
        image.loadImage(img);
        double rat = (img==null) ? 1 : img.getWidth()/img.getHeight();
        imageRatio.set(rat);
        layout();
    }
    public void setImage(File img) {// System.out.println(Runtime.getRuntime().totalMemory());
        if (img != null) {
            Point2D size = image.calculateImageLoadSize(THIS);
            Image tmp = Util.loadImage(img, size.getX(), size.getY());
            image.loadImage(tmp);
            image.setFile(img);
            imageRatio.set(tmp.getWidth()/tmp.getHeight());
        } else {
            image.loadImage((Image)null);
            imageRatio.set(1);
        }
        layout();
    }
    public boolean hasImage() {
        return (getImage() != null);
    }
    public boolean isShowImage() {
        return showImage;
    }
    public void setShowImage(boolean val) {
        showImage = val;
        imagePane.setVisible(val);
        layout();
    }
    public void toggleShowImage() {
        setShowImage(!isShowImage());
    }
    public boolean isShowContent() {
        return showContent;
    }
    public void setShowContent(boolean val) {
        showContent = val;
        layout();
    }
    public void toggleShowContent() {
        setShowContent(!isShowContent());
    }
    
    public double getPadding() {
        return padding;
    }
    /**
     * Padding for the layout.
     * Note that gap size defaultly mirrors padding value.
     * @param val 
     */
    public void setPadding(double val) {
        if (padding == gap) gap = val; // mirror gap if suposed to
        padding = val;
        THIS.setPadding(new Insets(val));
    }
    public double getGap() {
        return gap;
    }
    /**
     * Set gap between image and content.
     * Use negative value to cause gap mirror padding value. Assigning gap to
     * the same value as padding will cause automatic mirroring (which defaults
     * to true).  
     * @param val
     */
    public void setGap(double val) {
        if (val < 0) gap = padding;
        else gap = val;
    }
    public double getMinContentHeight() {
        return minContentHeightProperty().get();
    }
    /**
     * @return bindable min height of content
     */
    public DoubleProperty minContentHeightProperty() {
        return content.minHeightProperty(); //minContentHeight;
    }
    public void setMinContentHeight(double val) {
        minContentHeightProperty().set(val);
    }
    public double getMinContentWidth() {
        return minContentWidthProperty().get();
    }
    /**
     * @return bindable min width of content
     */
    public DoubleProperty minContentWidthProperty() {
        return content.minWidthProperty();//minContentWidth;
    }
    public void setMinContentWidth(double val) {
        minContentWidthProperty().set(val);
    }
    public ImageFlowPane.ImagePosition getImagePosition() {
        return imagePosition;
    }
    public void setImagePosition(ImageFlowPane.ImagePosition val) {
        imagePosition = val;
//        if (val == ImagePosition.CENTER) {
//            imagePane.getChildren().clear();
//            imagePane.setCenter(pane);
//        } else
//        if (val == ImagePosition.LEFT_BOTTOM) {
//            imagePane.getChildren().clear();
//            imagePane.setBottom(pane);
//        } else
//        if (val == ImagePosition.LEFT_TOP) {
//            imagePane.getChildren().clear();
//            imagePane.setTop(pane);
//        } else
//        if (val == ImagePosition.RIGHT_BOTTOM) {
//            imagePane.getChildren().clear();
//            imagePane.setRight(pane);
//        } else
//        if (val == ImagePosition.RIGHT_TOP) {
//            imagePane.getChildren().clear();
//            imagePane.setLeft(pane);
//        }
    }
    public double getHeight() {
        return THIS.getHeight();
    }
    public double getWidth() {
        return THIS.getWidth();
    }
    public void setHeight(double val) {
        THIS.setPrefHeight(val);
    }
    public void setWidth(double val) {
        THIS.setPrefWidth(val);
    }
    public double imageHeight() {
        return imagePane.getHeight();
    }
    public double imageWidth() {
        return imagePane.getWidth();
    }
    public double contentHeight() {
        return content.getHeight();
    }
    public double contentWidth() {
        return content.getWidth();
    }
    public List<Node> getChildren() {
        return content.getChildren();
    }
    public void setChildren(List<? extends Node> children) {
        content.getChildren().setAll(children);
    }
    public void addChild(Node child) {
        content.getChildren().add(child);
    }
    public double getImageRatio() {
        return imageRatio.get();
    }
    public double getPaneRatio() {
        return ratio.get();
    }
    
    public AnchorPane getContentPane() {
        return content;
    }

/******************************************************************************/

    /** Lays out children. Do not use other rebind methods. */
    public void layout() {
        clearBinding();
        if (showImage && showContent) {
            if (ratioHor.get()) bindHorizontal();
            else bindVertical();
        }
        else if (showImage && !showContent) bindImageOnly();
        else if (!showImage && showContent) bindContentOnly();            
    }
    
    private void bindHorizontal() {
        layout.setLeft(image.getPane());
        layout.setCenter(contentBorder);
        contentBorder.setPadding(new Insets(0,0,0,gap));
        image.getPane().prefHeightProperty().bind(layout.heightProperty());
        image.getPane().prefWidthProperty().bind(Bindings.min(
                layout.heightProperty().multiply(imageRatio),
                layout.widthProperty().subtract(minContentWidthProperty().get())));
    }
    private void bindVertical() {
        layout.setTop(image.getPane());
        layout.setCenter(contentBorder);
        contentBorder.setPadding(new Insets(gap,0,0,0));
        image.getPane().prefWidthProperty().bind(layout.widthProperty());
        image.getPane().prefHeightProperty().bind(Bindings.min(
                layout.widthProperty().divide(imageRatio),
                layout.heightProperty().subtract(minContentHeightProperty().get())));
    }
    private void bindContentOnly() {
        contentBorder.setPadding(new Insets(0,0,0,0));
        layout.setCenter(contentBorder);
    }
    private void bindImageOnly() {
        if (getImageRatio() < getPaneRatio()) {
            image.getPane().prefHeightProperty().bind(layout.heightProperty());
            image.getPane().prefWidthProperty().bind(Bindings.min(
                    layout.heightProperty().multiply(imageRatio),
                    layout.widthProperty().subtract(minContentWidthProperty().get())));
        } else {
            image.getPane().prefWidthProperty().bind(layout.widthProperty());
            image.getPane().prefHeightProperty().bind(Bindings.min(
                    layout.widthProperty().divide(imageRatio),
                    layout.heightProperty().subtract(minContentHeightProperty().get())));
        }
        layout.setCenter(image.getPane());
    }
    private void clearBinding() {
        layout.getChildren().clear();
        image.getPane().prefHeightProperty().unbind();
        image.getPane().prefWidthProperty().unbind();
    }
}