package sp.it.pl.ui.objects

import com.vladsch.flexmark.ast.FencedCodeBlock
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.geometry.Pos.BOTTOM_RIGHT
import javafx.scene.Node
import javafx.scene.input.Clipboard
import javafx.scene.input.DataFormat.PLAIN_TEXT
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.web.WebView
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.toNodeMermaid
import sp.it.util.reactive.Subscribed
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.attach
import sp.it.util.ui.drag.set
import sp.it.util.ui.lay
import sp.it.util.ui.maxSize
import sp.it.util.ui.prefSize
import sp.it.util.ui.stackPane
import sp.it.util.ui.text
import sp.it.util.ui.textFlow
import sp.it.util.ui.x2

fun FencedCodeBlock.toNode(): Node =
   when (info.toString().trim().lowercase()) {
      "mermaid" -> toNodeMermaid()
      else -> toNodeNormal()
   }

fun FencedCodeBlock.toNodeMermaid(): Node =
   stackPane {
      styleClass += "markdown-codeblock-box"
      styleClass += "markdown-codeblock-box-mermaid"
      val text = this@toNodeMermaid.getContentChars().normalizeEOL().trim()

      lay += WebView().apply {
         pageFill = Color.TRANSPARENT
         isContextMenuEnabled = false

         engine.loadContent(
            """
            <html>
            <head>
                <script src="https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js"></script>
            </head>
            <body style='background-color: transparent; color: white;'>
                <div id="mermaidDiagram" class="mermaid">
                    $text
                </div>
                <script>
                    mermaid.initialize({ startOnLoad: true });
                    mermaid.render('theGraph', '$text', function(svgCode){
                        document.getElementById('mermaidDiagram').innerHTML = svgCode;
                        window.done = true;
                    });
                </script>
            </body>
            </html>
            """
         )
      }
      initCodeBlockCopy(text)
   }

fun FencedCodeBlock.toNodeNormal(): Node =
   stackPane {
      val text = this@toNodeNormal.getContentChars().normalizeEOL().trim()
      styleClass += "markdown-codeblock-box"

      lay += textFlow {
         lay += text(text) {
            styleClass += "markdown-codeblock"
         }
      }
      initCodeBlockCopy(text)
   }

private fun StackPane.initCodeBlockCopy(text: String) {
   val s = Subscribed {
      val ip = object: StackPane() {
         init {
            isPickOnBounds = false
            styleClass += "markdown-codeblock-box-icon-box"
            lay(BOTTOM_RIGHT) += Icon(FontAwesomeIcon.COPY).onClickDo { Clipboard.getSystemClipboard()[PLAIN_TEXT] = text }
         }
         override fun computeMinHeight(width: Double) =  0.0
         override fun computeMinWidth(height: Double) = 0.0
         override fun computePrefHeight(width: Double) =  0.0
         override fun computePrefWidth(height: Double) = 0.0
      }
      lay += ip
      Subscription { lay -= ip }
   }
   hoverProperty() attach s::subscribe
}