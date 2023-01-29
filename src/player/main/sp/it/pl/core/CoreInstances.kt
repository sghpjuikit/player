package sp.it.pl.core

import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.Skin
import javafx.scene.text.Text
import kotlin.reflect.KClass
import sp.it.pl.layout.NodeFactory
import sp.it.pl.main.Widgets.ICON_BROWSER_NAME
import sp.it.pl.ui.nodeinfo.IconPickerContent
import sp.it.pl.ui.nodeinfo.MouseInfo
import sp.it.pl.ui.nodeinfo.SongAlbumInfo
import sp.it.pl.ui.nodeinfo.SongInfo
import sp.it.pl.ui.nodeinfo.WeatherInfo
import sp.it.pl.ui.objects.ColorInterpolationNode
import sp.it.pl.ui.objects.GpuNvidiaInfo
import sp.it.pl.ui.objects.MdNode
import sp.it.pl.ui.objects.PcControls
import sp.it.pl.ui.objects.UuidGeneratorNode
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
   val recommendedNodeClassesAsWidgets = observableSet<NodeFactory<*>>()

   @Suppress("RemoveExplicitTypeArguments")
   override fun init() {
      recommendedNodeClassesAsWidgets += listOf(
         NodeFactory(uuid("399dfa7b-dda1-4698-acaf-77c2297114d2"), UuidGeneratorNode::class, "UUID Generator") { UuidGeneratorNode() },
         NodeFactory(uuid("13636a4e-7959-443d-b318-55124459a8ae"), ColorInterpolationNode::class, "Color Interpolation") { ColorInterpolationNode() },
         NodeFactory(uuid("07855729-8263-44d7-b69f-6d5ce20348ad"), MdNode::class, "MdNode") { MdNode() },
         NodeFactory(uuid("238844e4-35ef-40f7-a357-035fcd39591a"), GpuNvidiaInfo::class, "GpuNvidiaInfo") { GpuNvidiaInfo() },
         NodeFactory(uuid("d42f049a-7e01-421c-a2e5-16524fd21cbb"), PcControls::class, "PcControls") { PcControls() },
         NodeFactory(uuid("0e6e89b3-237f-4072-a89c-14492afa1bd7"), DateClockDigitalIos::class, "DateClockDigitalIos") { DateClockDigitalIos() },
         NodeFactory(uuid("539ffb5c-96d2-47f5-a6f8-cf398dc0f6b8"), DatePickerContent::class, "DatePickerContent") { DatePickerContent() },
         NodeFactory(uuid("12f4b8c2-1162-4189-b056-986b551be943"), DateTimeClockWin::class, "DateTimeClockWin") { DateTimeClockWin() },
         NodeFactory(uuid("15d41fa3-7801-4276-a484-50e4b4619580"), DateTimePickerContent::class, "DateTimePickerContent") { DateTimePickerContent() },
         NodeFactory(uuid("1806a117-8636-47e2-a14b-f185fd0ca937"), FontPickerContent::class, "Font Browser") { FontPickerContent() },
         NodeFactory(uuid("6ac8bd91-9432-4070-87ab-fb7c1f41b2a0"), TimeClockAnalog::class, "TimeClockAnalog") { TimeClockAnalog() },
         NodeFactory(uuid("8bd6ca12-caa4-4f42-985a-6d4b9749cd32"), TimeClockDigital::class, "TimeClockDigital") { TimeClockDigital() },
         NodeFactory(uuid("20a08e65-f3e8-4129-9c32-6d72cf07360a"), TimeClockDigitalIos::class, "TimeClockDigitalIos") { TimeClockDigitalIos() },
         NodeFactory(uuid("6914b36e-b771-4e32-8339-07cbdb99c9f6"), TimePickerContent::class, "TimePickerContent") { TimePickerContent() },
         NodeFactory(uuid("800c980b-c135-40e4-8026-1f0e68d3ca33"), MouseInfo::class, "Mouse Info") { MouseInfo() },
         NodeFactory(uuid("d603d122-4fe6-4e93-be72-cfe94c49b05b"), SongInfo::class, "Song Info (small)") { SongInfo(false) },
         NodeFactory(uuid("96c8ca9d-f959-4a48-ad20-b8938196fdc8"), SongAlbumInfo::class, "Album Info (small)") { SongAlbumInfo(false) },
         NodeFactory(uuid("4f0a352d-8c80-46f5-a98f-46c354579c9f"), WeatherInfo::class, "Weather Info") { WeatherInfo() },
         NodeFactory(uuid("ae85ff79-d119-4677-a108-ca8f07bd4626"), javafx.scene.control.Label::class, "JavaFX Label") { Label("Label") },
         NodeFactory(uuid("5528126f-c9d5-40a7-ad72-a50f3bd9ab14"), javafx.scene.text.Text::class, "JavaFX Text") { Text("Text") },
         NodeFactory(uuid("109cbb7f-958b-47f7-8c89-072736a1b4c7"), IconPickerContent::class, ICON_BROWSER_NAME) { IconPickerContent() },
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

}