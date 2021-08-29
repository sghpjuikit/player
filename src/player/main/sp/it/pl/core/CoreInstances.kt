package sp.it.pl.core

import com.sun.javafx.scene.control.DatePickerContent
import javafx.scene.Node
import javafx.scene.control.Skin
import kotlin.reflect.KClass
import sp.it.pl.ui.objects.GpuNvidiaInfo
import sp.it.pl.ui.objects.MdNode
import sp.it.pl.ui.objects.PcControls
import sp.it.pl.ui.objects.picker.DateClockDigitalIos
import sp.it.pl.ui.objects.picker.DateTimeClockWin
import sp.it.pl.ui.objects.picker.DateTimePickerContent
import sp.it.pl.ui.objects.picker.FontPickerContent
import sp.it.pl.ui.objects.picker.TimeClockAnalog
import sp.it.pl.ui.objects.picker.TimeClockDigital
import sp.it.pl.ui.objects.picker.TimeClockDigitalIos
import sp.it.pl.ui.objects.picker.TimePickerContent
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
import sp.it.util.collections.observableSet
import sp.it.util.type.InstanceMap

object CoreInstances: InstanceMap(), Core {

   /** [Node] classes recommended to user for use as widgets. */
   val recommendedNodeClassesAsWidgets = observableSet<KClass<out Node>>()

   @Suppress("RemoveExplicitTypeArguments")
   override fun init() {
      recommendedNodeClassesAsWidgets += listOf(
         MdNode::class,
         GpuNvidiaInfo::class,
         PcControls::class,
         DateClockDigitalIos::class,
         DatePickerContent::class,
         DateTimeClockWin::class,
         DateTimePickerContent::class,
         FontPickerContent::class,
         TimeClockAnalog::class,
         TimeClockDigital::class,
         TimeClockDigitalIos::class,
         TimePickerContent::class,
      )

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