/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.TableCell;

import AudioPlayer.tagging.Metadata;
import GUI.objects.Rater.Rating;
import static javafx.application.Platform.runLater;
import javafx.geometry.Pos;
import static javafx.scene.control.ContentDisplay.GRAPHIC_ONLY;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import main.App;
import util.parsing.StringParseStrategy;
import static util.parsing.StringParseStrategy.From.CONSTRUCTOR;
import static util.parsing.StringParseStrategy.To.CONSTANT;

/** Cell for rating displaying the value as rating control. */
@StringParseStrategy( from = CONSTRUCTOR, to = CONSTANT, constant = "Stars" )
public class RatingRatingCellFactory implements RatingCellFactory {

    @Override
    public TableCell<Metadata, Double> call(TableColumn<Metadata, Double> param) {
        return new TableCell<Metadata,Double>(){
            Rating r = new Rating(App.maxRating.get(), 0);
            {
                setContentDisplay(GRAPHIC_ONLY);    
                setAlignment(Pos.CENTER);
                r.max.bind(App.maxRating);
                r.partialRating.bind(App.partialRating);
                r.updateOnHover.bind(App.hoverRating);
                r.editable.bind(App.allowRatingChange);
            }
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if(empty) {
                    setGraphic(null);
                } else {
                    if(getGraphic()==null) setGraphic(r);
                    // when rating is 1 (100%) cells wont get updated
                    // really bad workaround but the only that works for now
                    runLater(() -> runLater(() -> r.setRatingP(item)));
                    // the normal approach
                    // r.setRatingP(item);
                }
            }
        };
    }

}
