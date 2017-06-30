package gui.objects.tablecell;

import audio.tagging.Metadata;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import util.functional.Functors.Ƒ1;

/** Factory producing rating table cells - cells displaying metadata rating. */
public interface RatingCellFactory extends Ƒ1<TableColumn<Metadata,Double>,TableCell<Metadata,Double>> {}