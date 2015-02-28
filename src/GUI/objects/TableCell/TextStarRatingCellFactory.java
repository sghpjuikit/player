/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.TableCell;

import AudioPlayer.tagging.Metadata;
import static java.lang.Math.round;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import util.parsing.StringParseStrategy;
import static util.parsing.StringParseStrategy.From.CONSTRUCTOR;
import static util.parsing.StringParseStrategy.To.CONSTANT;

/** Cell for rating displaying the value as text from '' to '*****'. */
@StringParseStrategy( from = CONSTRUCTOR, to = CONSTANT, constant = "Text star" )
public class TextStarRatingCellFactory implements RatingCellFactory {
    private static final String s0 = "";
    private static final String s1 = "*";
    private static final String s2 = "**";
    private static final String s3 = "***";
    private static final String s4 = "****";
    private static final String s5 = "*****";

    @Override
    public TableCell<Metadata, Double> call(TableColumn<Metadata, Double> param) {
        return new TableCell<Metadata,Double>(){
            {
                setAlignment(Pos.CENTER);
            }
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if(empty) {
                    setText(null);
                } else {
                    int r = (int)round(item/0.2);
                    String s;
                    if(r==0) s = s0;
                    else if(r==1) s = s1;
                    else if(r==2) s = s2;
                    else if(r==3) s = s3;
                    else if(r==4) s = s4;
                    else if(r==5) s = s5;
                    else s = null;
                    setText(s);
                }
            }
        };
    }
}
