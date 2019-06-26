package sp.it.pl.core

import javafx.scene.control.Skin
import sp.it.pl.gui.objects.rating.Rating
import sp.it.pl.gui.objects.rating.RatingSkinNumber
import sp.it.pl.gui.objects.rating.RatingSkinStar
import sp.it.pl.web.BingImageSearchQBuilder
import sp.it.pl.web.DuckDuckGoImageQBuilder
import sp.it.pl.web.DuckDuckGoQBuilder
import sp.it.pl.web.GoogleImageQBuilder
import sp.it.pl.web.SearchUriBuilder
import sp.it.pl.web.WikipediaQBuilder
import sp.it.util.type.InstanceMap
import kotlin.reflect.KClass

object CoreInstances: InstanceMap(), Core {

    @Suppress("RemoveExplicitTypeArguments")
    override fun init() {
        addInstances<SearchUriBuilder>(
            DuckDuckGoQBuilder,
            DuckDuckGoImageQBuilder,
            WikipediaQBuilder,
            BingImageSearchQBuilder,
            GoogleImageQBuilder
        )
        addInstances<KClass<out Skin<Rating>>>(
            RatingSkinStar::class,
            RatingSkinNumber::class
        )
    }

}