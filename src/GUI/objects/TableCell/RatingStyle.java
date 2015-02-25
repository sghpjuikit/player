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
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import main.App;

/**
 <p>
 @author Plutonium_
 */
public enum RatingStyle implements RatingCellFactory {
    STARS,
    BAR,
    NUMBER;

    @Override
    public TableCell<Metadata, Double> call(TableColumn<Metadata, Double> param) {
        switch(this) {
            case STARS:
                return new TableCell<Metadata,Double>(){
                    Rating r = new Rating(App.maxRating.get(), 0);
                    {
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
            case BAR:
                return new TableCell<Metadata,Double>(){
                    ProgressBar p = new ProgressBar();
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
            case NUMBER:
                return new TableCell<Metadata,Double>(){
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
            default: throw new AssertionError("illegal switch satement: " + this);
        }
    }
}
