package sp.it.pl.core

import javafx.scene.control.Skin
import kotlin.reflect.KClass
import sp.it.pl.ui.objects.rating.Rating
import sp.it.pl.ui.objects.rating.RatingSkinNumber1
import sp.it.pl.ui.objects.rating.RatingSkinNumber100
import sp.it.pl.ui.objects.rating.RatingSkinNumberPercent
import sp.it.pl.ui.objects.rating.RatingSkinStar
import sp.it.pl.web.BingImageSearchQBuilder
import sp.it.pl.web.DuckDuckGoImageQBuilder
import sp.it.pl.web.DuckDuckGoQBuilder
import sp.it.pl.web.GoogleImageQBuilder
import sp.it.pl.web.SearchUriBuilder
import sp.it.pl.web.WikipediaQBuilder
import sp.it.util.type.InstanceMap

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
         RatingSkinNumber1::class,
         RatingSkinNumber100::class,
         RatingSkinNumberPercent::class
      )
   }

}