package gui.objects.tablecell;

import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import audio.tagging.Metadata;
import audio.tagging.MetadataWriter;
import gui.objects.rating.Rating;
import util.parsing.ParsesFromString;
import util.parsing.StringParseStrategy;
import util.parsing.StringParseStrategy.From;
import util.plugin.IsPlugin;
import static javafx.scene.control.ContentDisplay.GRAPHIC_ONLY;
import static main.App.APP;
import static util.parsing.StringParseStrategy.To.CONSTANT;

/** Cell for rating displaying the value as rating control. */
@IsPlugin
@StringParseStrategy( from = From.ANNOTATED_METHOD, to = CONSTANT, constant = "Stars" )
public class RatingRatingCellFactory implements RatingCellFactory {

	@ParsesFromString
	public RatingRatingCellFactory() {}

	@Override
	public TableCell<Metadata, Double> apply(TableColumn<Metadata, Double> c) {
		return new TableCell<>(){
			Rating r = new Rating(APP.maxRating.get(), 0);

			{
				setContentDisplay(GRAPHIC_ONLY);
				setAlignment(Pos.CENTER);
				r.icons.bind(APP.maxRating);
				r.partialRating.bind(APP.partialRating);
				r.updateOnHover.bind(APP.hoverRating);
				r.editable.bind(APP.allowRatingChange);
				if (c.getUserData().equals(Metadata.Field.RATING)) {
					r.setOnRatingChanged(rv -> MetadataWriter.useToRate(c.getTableView().getItems().get(getIndex()), rv));
				}
			}

			@Override
			protected void updateItem(Double item, boolean empty) {
				super.updateItem(item, empty);
				if (empty) {
					setGraphic(null);
				} else {
					r.rating.set(item);
					if (getGraphic()==null) setGraphic(r);
				}
			}
		};
	}

}