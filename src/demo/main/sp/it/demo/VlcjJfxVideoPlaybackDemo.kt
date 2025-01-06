package sp.it.demo

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import javafx.scene.input.TransferMode.COPY
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import kotlin.text.replaceFirst
import sp.it.util.ui.size
import sp.it.util.ui.x
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.javafx.videosurface.ImageViewVideoSurface

/** Demo showing how to play video in JavaFX ImageView efficiently using vlcj */
class VlcjJfxVideoPlaybackDemo : Application() {
   val factory = MediaPlayerFactory("--demux=avformat");
   val mediaPlayer = factory.mediaPlayers().newEmbeddedMediaPlayer();

   override fun start(primaryStage: Stage) {
      val imageView = ImageView().apply {
         fitWidthProperty().bind(primaryStage.widthProperty()) // Bind width to stage width
         fitHeightProperty().bind(primaryStage.heightProperty()) //
         isPreserveRatio = true
      }

      mediaPlayer.videoSurface().set(ImageViewVideoSurface(imageView));
      mediaPlayer.controls().setRepeat(true)

      primaryStage.title = "Vlcj Video Player"
      primaryStage.size = 500 x 500
      primaryStage.scene = Scene(StackPane(imageView, Label("Drag & drop file to play")))
      primaryStage.show()

      // Handle drag-and-drop events
      primaryStage.scene.setOnDragOver {
         if (it.dragboard.hasFiles()) it.acceptTransferModes(COPY)
         it.consume()
      }
      primaryStage.scene.setOnDragDropped {
         if (it.dragboard.hasFiles()) {
            val file = it.dragboard.files[0]
            mediaPlayer.media().play(file.absoluteFile.toURI().toString().replaceFirst("file:/", "file:///"))
         }
         it.consume()
      }
   }

   override fun stop() {
      mediaPlayer.controls().stop()
      mediaPlayer.release()
   }

}

fun main() {
   Application.launch(VlcjJfxVideoPlaybackDemo::class.java)
}