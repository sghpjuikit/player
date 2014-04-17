
package unused;

import javafx.scene.layout.AnchorPane;

/**
 * Half-abstract half-graphical container. Wraps widgets by real graphical
 * components and serves as graphical part of abstract Containers.
 * 
 * @author uranium
 */
public class PseudoContainer {
    AnchorPane entireArea = new AnchorPane();
    
    /**
     * @return graphical root of this pseudo container.
     */
    public AnchorPane getPane() {
        return entireArea;
    }
}
