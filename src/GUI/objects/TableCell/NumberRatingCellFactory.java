/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.TableCell;

import AudioPlayer.plugin.IsPlugin;
import AudioPlayer.tagging.Metadata;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import util.parsing.StringParseStrategy;
import static util.parsing.StringParseStrategy.From.CONSTRUCTOR;
import static util.parsing.StringParseStrategy.To.CONSTANT;

/** Cell for rating displaying the value as number. */
@IsPlugin
@StringParseStrategy( from = CONSTRUCTOR, to = CONSTANT, constant = "Number" )
public class NumberRatingCellFactory implements RatingCellFactory {

    @Override
    public TableCell<Metadata, Double> apply(TableColumn<Metadata, Double> param) {
        return new TableCell<Metadata,Double>(){
                    {
                        setAlignment(Pos.CENTER_RIGHT);
                    }
                    @Override
                    protected void updateItem(Double item, boolean empty) {
                        super.updateItem(item, empty);
                        if(empty) {
                            setText(null);
                        } else {
                            String s = item.toString();
                            if(s.length()>4) s=s.substring(0, 4);
                            setText(s);
                        }
                    }
                };
    }
    
}