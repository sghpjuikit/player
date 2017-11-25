package sp.it.pl.core

import sp.it.pl.gui.objects.tablecell.BarRatingCellFactory
import sp.it.pl.gui.objects.tablecell.HyphenRatingCellFactory
import sp.it.pl.gui.objects.tablecell.NumberRatingCellFactory
import sp.it.pl.gui.objects.tablecell.RatingCellFactory
import sp.it.pl.gui.objects.tablecell.RatingRatingCellFactory
import sp.it.pl.gui.objects.tablecell.TextStarRatingCellFactory
import sp.it.pl.util.type.InstanceMap
import sp.it.pl.web.BingImageSearchQBuilder
import sp.it.pl.web.DuckDuckGoImageQBuilder
import sp.it.pl.web.DuckDuckGoQBuilder
import sp.it.pl.web.GoogleImageQBuilder
import sp.it.pl.web.SearchUriBuilder
import sp.it.pl.web.WikipediaQBuilder

object CoreInstances: InstanceMap(), Core {

    @Suppress("RemoveExplicitTypeArguments")
    override fun init() {
        addInstance<SearchUriBuilder>(
                DuckDuckGoQBuilder,
                DuckDuckGoImageQBuilder,
                WikipediaQBuilder,
                BingImageSearchQBuilder,
                GoogleImageQBuilder
        )
        addInstance<RatingCellFactory>(
                BarRatingCellFactory,
                HyphenRatingCellFactory,
                TextStarRatingCellFactory,
                NumberRatingCellFactory,
                RatingRatingCellFactory
        )
    }

}