package sp.it.pl.core

import java.util.UUID
import javafx.scene.Node
import javafx.scene.control.Skin
import kotlin.reflect.KClass
import sp.it.pl.ui.nodeinfo.MouseInfo
import sp.it.pl.ui.nodeinfo.SongAlbumInfo
import sp.it.pl.ui.nodeinfo.SongInfo
import sp.it.pl.ui.objects.GpuNvidiaInfo
import sp.it.pl.ui.objects.MdNode
import sp.it.pl.ui.objects.PcControls
import sp.it.pl.ui.objects.picker.DateClockDigitalIos
import sp.it.pl.ui.objects.picker.DatePickerContent
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
import sp.it.pl.web.BingSearchQBuilder
import sp.it.pl.web.DuckDuckGoImageQBuilder
import sp.it.pl.web.DuckDuckGoQBuilder
import sp.it.pl.web.GoogleImageQBuilder
import sp.it.pl.web.GoogleQBuilder
import sp.it.pl.web.SearchUriBuilder
import sp.it.pl.web.WikipediaQBuilder
import sp.it.util.collections.observableSet
import sp.it.util.type.InstanceMap
import sp.it.util.units.uuid

object CoreInstances: InstanceMap(), Core {

   /** [Node] classes recommended to user for use as widgets. */
   val recommendedNodeClassesAsWidgets = observableSet<RecommendedWidgetClass<*>>()

   @Suppress("RemoveExplicitTypeArguments")
   override fun init() {
      recommendedNodeClassesAsWidgets += listOf(
         RecommendedWidgetClass(uuid("07855729-8263-44d7-b69f-6d5ce20348ad"), MdNode::class, "MdNode") { MdNode() },
         RecommendedWidgetClass(uuid("238844e4-35ef-40f7-a357-035fcd39591a"), GpuNvidiaInfo::class, "GpuNvidiaInfo") { GpuNvidiaInfo() },
         RecommendedWidgetClass(uuid("d42f049a-7e01-421c-a2e5-16524fd21cbb"), PcControls::class, "PcControls") { PcControls() },
         RecommendedWidgetClass(uuid("0e6e89b3-237f-4072-a89c-14492afa1bd7"), DateClockDigitalIos::class, "DateClockDigitalIos") { DateClockDigitalIos() },
         RecommendedWidgetClass(uuid("539ffb5c-96d2-47f5-a6f8-cf398dc0f6b8"), DatePickerContent::class, "DatePickerContent") { DatePickerContent() },
         RecommendedWidgetClass(uuid("12f4b8c2-1162-4189-b056-986b551be943"), DateTimeClockWin::class, "DateTimeClockWin") { DateTimeClockWin() },
         RecommendedWidgetClass(uuid("15d41fa3-7801-4276-a484-50e4b4619580"), DateTimePickerContent::class, "DateTimePickerContent") { DateTimePickerContent() },
         RecommendedWidgetClass(uuid("1806a117-8636-47e2-a14b-f185fd0ca937"), FontPickerContent::class, "FontPickerContent") { FontPickerContent() },
         RecommendedWidgetClass(uuid("6ac8bd91-9432-4070-87ab-fb7c1f41b2a0"), TimeClockAnalog::class, "TimeClockAnalog") { TimeClockAnalog() },
         RecommendedWidgetClass(uuid("8bd6ca12-caa4-4f42-985a-6d4b9749cd32"), TimeClockDigital::class, "TimeClockDigital") { TimeClockDigital() },
         RecommendedWidgetClass(uuid("20a08e65-f3e8-4129-9c32-6d72cf07360a"), TimeClockDigitalIos::class, "TimeClockDigitalIos") { TimeClockDigitalIos() },
         RecommendedWidgetClass(uuid("6914b36e-b771-4e32-8339-07cbdb99c9f6"), TimePickerContent::class, "TimePickerContent") { TimePickerContent() },
         RecommendedWidgetClass(uuid("800c980b-c135-40e4-8026-1f0e68d3ca33"), MouseInfo::class, "Mouse Info") { MouseInfo() },
         RecommendedWidgetClass(uuid("d603d122-4fe6-4e93-be72-cfe94c49b05b"), SongInfo::class, "Song Info (small)") { SongInfo(false) },
         RecommendedWidgetClass(uuid("96c8ca9d-f959-4a48-ad20-b8938196fdc8"), SongAlbumInfo::class, "Album Info (small)") { SongAlbumInfo(false) },
      )

      addInstances<SearchUriBuilder>(
         DuckDuckGoQBuilder,
         DuckDuckGoImageQBuilder,
         WikipediaQBuilder,
         BingSearchQBuilder,
         BingImageSearchQBuilder,
         GoogleQBuilder,
         GoogleImageQBuilder,
      )
      addInstances<KClass<out Skin<Rating>>>(
         RatingSkinStar::class,
         RatingSkinNumber1::class,
         RatingSkinNumber100::class,
         RatingSkinNumberPercent::class,
      )
   }

   data class RecommendedWidgetClass<T: Node>(val id: UUID, val type: KClass<out Node>, val nameUi: String, val constructor: () -> T) {
      override fun hashCode() = type.hashCode()
      override fun equals(other: Any?) = other is RecommendedWidgetClass<*> && other.type==type
   }
}