package gui.objects.tablecell;

import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import audio.tagging.Metadata;
import util.parsing.ParsesFromString;
import util.parsing.StringParseStrategy;
import util.parsing.StringParseStrategy.From;
import util.plugin.IsPlugin;
import static util.parsing.StringParseStrategy.To.CONSTANT;

/** Cell for rating displaying the value as number. */
@IsPlugin
@StringParseStrategy( from = From.ANNOTATED_METHOD, to = CONSTANT, constant = "Number" )
public class NumberRatingCellFactory implements RatingCellFactory {

	@ParsesFromString
	public NumberRatingCellFactory() {}

	@Override
	public TableCell<Metadata, Double> apply(TableColumn<Metadata, Double> param) {
		return new TableCell<>(){
			{
				setAlignment(Pos.CENTER_RIGHT);
			}

			@Override
			protected void updateItem(Double item, boolean empty) {
				super.updateItem(item, empty);
				setText(empty ? null : String.format("%.2f", item));
			}
		};
	}

}