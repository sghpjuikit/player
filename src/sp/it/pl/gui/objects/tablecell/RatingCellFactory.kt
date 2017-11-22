package sp.it.pl.gui.objects.tablecell

import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.util.functional.Functors.Ƒ1

/** Factory producing rating table cells - cells displaying metadata rating. */
interface RatingCellFactory: Ƒ1<TableColumn<Metadata, Double>, TableCell<Metadata, Double>>