/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.TableCell;

import AudioPlayer.plugin.IsPlugin;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataWriter;
import gui.objects.Rater.Rating;

import javafx.geometry.Pos;

import static javafx.scene.control.ContentDisplay.GRAPHIC_ONLY;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

import util.parsing.StringParseStrategy;

import static main.App.APP;
import static util.parsing.StringParseStrategy.From.CONSTRUCTOR;
import static util.parsing.StringParseStrategy.To.CONSTANT;

/** Cell for rating displaying the value as rating control. */
@IsPlugin
@StringParseStrategy( from = CONSTRUCTOR, to = CONSTANT, constant = "Stars" )
public class RatingRatingCellFactory implements RatingCellFactory {

    @Override
    public TableCell<Metadata, Double> apply(TableColumn<Metadata, Double> c) {
        return new TableCell<Metadata,Double>(){
            Rating r = new Rating(APP.maxRating.get(), 0);
            {
                setContentDisplay(GRAPHIC_ONLY);
                setAlignment(Pos.CENTER);
                r.icons.bind(APP.maxRating);
                r.partialRating.bind(APP.partialRating);
                r.updateOnHover.bind(APP.hoverRating);
                r.editable.bind(APP.allowRatingChange);
                if(c.getUserData().equals(Metadata.Field.RATING)) {
                    r.setOnRatingChanged(rv -> MetadataWriter.useToRate(c.getTableView().getItems().get(getIndex()), rv));
                }
            }
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if(empty) {
                    setGraphic(null);
                } else {
                    if(getGraphic()==null) setGraphic(r);
                    r.rating.set(item);
                }
            }
        };
    }

}