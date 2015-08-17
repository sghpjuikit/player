/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.TableCell;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

import AudioPlayer.plugin.IsPluginType;
import AudioPlayer.tagging.Metadata;
import util.functional.Functors.Ƒ1;

/** Factory producing rating table cells - cells displaying metadata rating. */
@IsPluginType
public interface RatingCellFactory extends Ƒ1<TableColumn<Metadata,Double>,TableCell<Metadata,Double>> {
    
}
