package sp.it.pl.main

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.geometry.Pos
import javafx.scene.control.Label
import sp.it.pl.audio.SimpleItem
import sp.it.pl.audio.tagging.MetadataReader
import sp.it.pl.gui.infonode.ConvertTaskInfo
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.pane.ActionPane
import sp.it.pl.gui.pane.ActionPane.ComplexActionData
import sp.it.pl.gui.pane.ActionPane.collectionWrap
import sp.it.pl.gui.pane.ConfigPane
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.WidgetManager
import sp.it.pl.layout.widget.feature.PlaylistFeature
import sp.it.pl.layout.widget.feature.SongReader
import sp.it.pl.main.App.APP
import sp.it.pl.util.access.V
import sp.it.pl.util.access.ref.SingleR
import sp.it.pl.util.async.FX
import sp.it.pl.util.async.future.Fut.fut
import sp.it.pl.util.conf.Config
import sp.it.pl.util.file.AudioFileFormat
import sp.it.pl.util.file.Util.getFilesAudio
import sp.it.pl.util.functional.orNull
import sp.it.pl.util.graphics.Util.layHorizontally
import sp.it.pl.util.graphics.Util.layVertically
import java.io.File
import java.util.function.Consumer
import kotlin.streams.toList

fun addToLibraryConsumer(actionPane: ActionPane): ComplexActionData<Collection<File>, List<File>> = ComplexActionData(
        {
            val makeWritable = V(true)
            val editInTagger = V(true)  // TODO: enable only if Tagger/SongReader is available and avoid casts
            val editOnlyAdded = V(false)
            val enqueue = V(false)
            val task = MetadataReader.buildAddItemsToLibTask()
            val info = ConvertTaskInfo(
                    title = null,
                    message = Label(),
                    skipped = Label(),
                    state = Label(),
                    pi = appProgressIndicator()
            )
            val tagger = SingleR<Widget<*>, Void> { APP.widgetManager.factories.find { it.name()=="Tagger" }.orEmpty().create() }   // TODO: no empty widget...

            info.bind(task)
            layHorizontally(50.0, Pos.CENTER,
                    layVertically(50.0, Pos.CENTER,
                            ConfigPane(
                                    Config.forProperty(Boolean::class.java, "Make writable if read-only", makeWritable),
                                    Config.forProperty(Boolean::class.java, "Edit in Tagger", editInTagger),
                                    Config.forProperty(Boolean::class.java, "Edit only added files", editOnlyAdded),
                                    Config.forProperty(Boolean::class.java, "Enqueue in playlist", enqueue)
                            ),
                            layVertically(10.0, Pos.CENTER_LEFT,
                                    info.state,
                                    layHorizontally(10.0, Pos.CENTER_LEFT,
                                            info.message,
                                            info.progress
                                    ),
                                    info.skipped
                            ),
                            Icon(FontAwesomeIcon.CHECK, 25.0).onClick { e ->
                                (e.source as Icon).isDisable = true
                                fut<List<File>>(collectionWrap(actionPane.data) as List<File>)  // TODO: make automatic
                                        .use { files -> if (makeWritable.get()) files.forEach { it.setWritable(true) } }
                                        .map { files -> files.map { SimpleItem(it) } }
                                        .map(task)
                                        .showProgress(actionPane.actionProgress)
                                        .use(FX, Consumer { result ->
                                            if (editInTagger.get()) {
                                                val items = if (editOnlyAdded.get()) result.converted else result.all
                                                (tagger.get().controller as SongReader).read(items)
                                            }
                                            if (enqueue.get() && !result.all.isEmpty()) {
                                                APP.widgetManager.find(PlaylistFeature::class.java, WidgetManager.WidgetSource.ANY)
                                                        .orNull()
                                                        ?.playlist
                                                        ?.addItems(result.all)
                                            }
                                        })
                            }
                                    .withText("Execute")
                    ),
                    tagger.get().load()
            )
        },
        { files -> fut(files).map { getFilesAudio(it, AudioFileFormat.Use.APP, Integer.MAX_VALUE).toList() } }
)