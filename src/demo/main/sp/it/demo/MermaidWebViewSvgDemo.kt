package sp.it.demo

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.StringReader
import java.util.Base64
import java.util.concurrent.CountDownLatch
import javafx.application.Application
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.embed.swing.SwingFXUtils
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.VBox
import javafx.scene.web.WebView
import javafx.stage.Stage
import javafx.stage.StageStyle
import javax.imageio.ImageIO
import org.apache.batik.anim.dom.SAXSVGDocumentFactory
import org.apache.batik.transcoder.TranscoderException
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.PNGTranscoder
import org.w3c.dom.svg.SVGDocument
import sp.it.util.dev.fail

const val MERMAID_CODE = """
graph LR
    A[Client] --> B{Load Balancer}
    B --> C[Server 1]
    B --> D[Server 2]
    style B fill:#f9f,stroke:#333,stroke-width:2px
"""

fun main() {
   Application.launch(MermaidWebViewSvgDemo::class.java)
}

/** JavaFX mermaid diagram rendering using webview an rendering to svg using imageio+batic. Has issues with styling parsing. */
class MermaidWebViewSvgDemo : Application() {

   override fun start(primaryStage: Stage) {
      val imageView = ImageView().apply {
         fitWidth = 800.0
         fitHeight = 600.0
      }

      val root = VBox(Label("ImageView"), imageView)

      primaryStage.title = "Mermaid Diagram"
      primaryStage.scene = Scene(root, 800.0, 600.0)
      primaryStage.show()

      renderMermaidToImage { image ->
         Platform.runLater { // Update UI on FX thread
            imageView.image = image
         }
      }
   }

   private fun renderMermaidToImage(callback: (Image?) -> Unit) {
      val latch = CountDownLatch(1)
      var image: Image? = null

      Platform.runLater {
         // Create an invisible stage
         val invisibleStage = Stage(StageStyle.TRANSPARENT) // Crucial: TRANSPARENT style
         val webView = WebView()
         val webEngine = webView.engine

         //set size to webview
         webView.prefWidth = 800.0
         webView.prefHeight = 600.0

         val scene = Scene(webView, 800.0, 600.0) // Set size
         scene.fill = null // Ensure transparency
         invisibleStage.scene = scene
         invisibleStage.width = 800.0 //Crucial: set the width of the stage or it wont render
         invisibleStage.height = 600.0 //Crucial: set the height of the stage or it wont render

         invisibleStage.show() // Add this line


         val html = """
                <html>
                <head>
                    <script src="https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js"></script>
                </head>
                <body>
                    <div id="mermaidDiagram" class="mermaid">
                        $MERMAID_CODE
                    </div>
                    <script>
                        mermaid.initialize({ startOnLoad: true });
                        mermaid.render('theGraph', '$MERMAID_CODE', function(svgCode){
                            document.getElementById('mermaidDiagram').innerHTML = svgCode;
                            window.done = true;
                        });
                    </script>
                </body>
                </html>
                """

         webEngine.loadContent(html)

         webEngine.loadWorker.stateProperty().addListener { _, _, newState ->
            if (newState == Worker.State.SUCCEEDED) {
               try {
                  val svgContent = webEngine.executeScript("document.getElementById('mermaidDiagram').innerHTML") as String

                  if (svgContent.isNullOrEmpty()) {
                     println("SVG content is null or empty")
                  } else {
                     image = svgToImage(svgContent)
                  }
               } catch (e: Exception) {
                  e.printStackTrace()
                  println("Error extracting SVG: ${e.message}")
               } finally {
                  invisibleStage.close() // Close the invisible stage after rendering
                  latch.countDown()
               }

            } else if (newState == Worker.State.FAILED) {
               println("WebView failed to load.")
               invisibleStage.close()
               latch.countDown()
            }
         }
      }

      Thread {
         latch.await()
         Platform.runLater {
            callback(image)
         }
      }.start()
   }


   private fun svgToImage(svgContent: String): Image? {
      try {
         val svgData = svgContent.replace("\n", "").trim()
         val base64Svg = Base64.getEncoder().encodeToString(svgData.toByteArray(Charsets.UTF_8))
         val imgData = "data:image/svg+xml;base64,$base64Svg"
         val bais = ByteArrayInputStream(Base64.getDecoder().decode(imgData.substring(imgData.indexOf(",") + 1)))
//         val bais = ByteArrayInputStream(svgData.toByteArray(Charsets.UTF_8))
         println(imgData)
         val bufferedImage = ImageIO.read(bais) ?: fail { "Failed to read svg, gives null" }

//         val svgString = String(svgData.toByteArray(Charsets.UTF_8), Charsets.UTF_8).removeSvgStyles()
//         val bufferedImage = readSVGFromString(svgString) ?: fail { "Failed to read svg, gives null" }

         return SwingFXUtils.toFXImage(bufferedImage, null)

      } catch (e: Exception) {
         e.printStackTrace()
         println("Error converting SVG to image: ${e.message}")
         return null
      }
   }
}


fun String.removeSvgStyles(): String {
   val styleRegex = "<style[^>]*>.*?</style>".toRegex(RegexOption.DOT_MATCHES_ALL)
   return this.replace(styleRegex, "")
}

fun readSVGFromString(svgString: String): BufferedImage? {
   try {
      // Create a Batik SVG document factory
      val parser = SAXSVGDocumentFactory(null) // null URI Resolver uses default

      // Parse the SVG string into a DOM tree
      val document: SVGDocument = parser.createSVGDocument(null, StringReader(svgString))

      // Create a transcoder (PNGTranscoder for image output)
      val transcoder = PNGTranscoder()
      transcoder.addTranscodingHint(org.apache.batik.transcoder.SVGAbstractTranscoder.KEY_DOCUMENT_ELEMENT_NAMESPACE_URI, "http://www.w3.org/2000/svg")
      transcoder.addTranscodingHint(org.apache.batik.transcoder.SVGAbstractTranscoder.KEY_DOCUMENT_ELEMENT, "svg")

      // Set the document as the transcoder input
      val input = TranscoderInput(document)

      // Create an output stream to hold the PNG data
      val outputStream = ByteArrayOutputStream()
      val output = TranscoderOutput(outputStream)

      // Transcode the SVG to PNG
      transcoder.transcode(input, output)

      // Create a BufferedImage from the PNG data
      val pngData = outputStream.toByteArray()
      ByteArrayInputStream(pngData).use { bis ->
         return ImageIO.read(bis)
      }

   } catch (e: IOException) {
      println("IOException: ${e.message}")
      e.printStackTrace()
      return null
   } catch (e: TranscoderException) {
      println("TranscoderException: ${e.message}")
      e.printStackTrace()
      return null
   } catch (e: Exception) {
      println("Generic Exception: ${e.message}")
      e.printStackTrace()
      return null
   }
}