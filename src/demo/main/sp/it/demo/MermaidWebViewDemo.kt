package sp.it.demo

import javafx.application.Application
import javafx.concurrent.Worker
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.web.WebView
import javafx.stage.Stage
import sp.it.util.async.runFX
import sp.it.util.reactive.sync
import sp.it.util.ui.lay
import sp.it.util.ui.vBox
import sp.it.util.units.seconds

fun main(args: Array<String>) {
   Application.launch(MermaidDemoWebView::class.java, *args)
}

/** JavaFX mermaid diagram rendering using WebView and attempting to snapshot content to image. The image has issues if webview has scrollbars. */
class MermaidDemoWebView: Application() {
   private val MERMAID_CODE = "graph TD; A-->B; A-->C; B-->D; C-->D;"

   override fun start(primaryStage: Stage) {
      val webView = WebView().apply {
         minWidth = 200.0
         minHeight = 200.0
      }
      val imageView = ImageView().apply {
         fitWidth = 800.0
         fitHeight = 600.0
         isPreserveRatio = true
      }
      val root = vBox {
         lay += Label("WebView")
         lay += webView
         lay += Label("ImageView")
         lay += imageView
      }

      primaryStage.title = "Mermaid Diagram"
      primaryStage.scene = Scene(root, 800.0, 600.0)
      primaryStage.show()


      // Set up WebView
      val webEngine = webView.engine
      val html =
         """
            |<html>
            | <head><script src="https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js"></script></head>
            | <body><div class="mermaid">$MERMAID_CODE</div><script>mermaid.initialize({startOnLoad:true});</script></body>
            |</html>
            """.trimMargin()
      webEngine.loadContent(html)

      // Wait for the WebView to finish loading
      webEngine.loadWorker.stateProperty().sync { state ->
         println(state)
         if (state==Worker.State.SUCCEEDED) {
            runFX(1.seconds) {
               val webViewImage: Image = webView.snapshot(null, null)
               imageView.image = webViewImage
            }
         }
      }

   }

}