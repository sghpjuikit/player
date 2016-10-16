package gui.objects.tablecell;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import audio.tagging.Metadata;
import util.functional.Functors.Ƒ1;
import util.plugin.IsPluginType;

/** Factory producing rating table cells - cells displaying metadata rating. */
@IsPluginType
public interface RatingCellFactory extends Ƒ1<TableColumn<Metadata,Double>,TableCell<Metadata,Double>> {}