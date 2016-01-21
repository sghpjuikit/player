/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.TableCell;

import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

import AudioPlayer.plugin.IsPlugin;
import AudioPlayer.tagging.Metadata;
import util.parsing.ParsesFromString;
import util.parsing.StringParseStrategy;
import util.parsing.StringParseStrategy.From;

import static javafx.scene.control.ContentDisplay.GRAPHIC_ONLY;
import static util.parsing.StringParseStrategy.To.CONSTANT;

/** Cell for rating displaying the value as progress bar. */
@IsPlugin
@StringParseStrategy( from = From.ANNOTATED_METHOD, to = CONSTANT, constant = "Bar" )
public class BarRatingCellFactory implements RatingCellFactory {

    @ParsesFromString
    public BarRatingCellFactory() {}

    @Override
    public TableCell<Metadata, Double> apply(TableColumn<Metadata, Double> param) {
        return new TableCell<Metadata,Double>(){
                    ProgressBar p = new ProgressBar();
                    {
                        setContentDisplay(GRAPHIC_ONLY);
                    }
                    @Override
                    protected void updateItem(Double item, boolean empty) {
                        super.updateItem(item, empty);
                        if(empty) {
                            setGraphic(null);
                        } else {
                            if(getGraphic()==null) setGraphic(p);
                            p.setProgress(item);
                        }
                    }
                };
    }
}