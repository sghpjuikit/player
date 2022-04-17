/*
 * Implementation based on ControlsF:
 *
 * Copyright (c) 2014, 2015, ControlsFX
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of ControlsFX, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL CONTROLSFX BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package sp.it.pl.ui.objects.autocomplete

import javafx.scene.Node
import javafx.util.StringConverter
import sp.it.util.access.v
import sp.it.util.async.executor.EventReducer
import sp.it.util.async.executor.EventReducer.toLast
import sp.it.util.async.runIO
import sp.it.util.async.runLater
import sp.it.util.collections.setTo
import sp.it.util.dev.fail
import sp.it.util.functional.ifIs
import sp.it.util.functional.orNull
import sp.it.util.functional.toUnit
import sp.it.util.reactive.Handler1
import sp.it.util.ui.minPrefMaxWidth

/**
 * Base class for auto-completion bindings.
 *
 * To use the auto-completion, refer to the [sp.it.pl.ui.objects.autocomplete.AutoCompletion].
 *
 * @param <T> type of suggestions
 * @see sp.it.pl.ui.objects.autocomplete.AutoCompletion
 */
abstract class AutoCompletionBinding<T> {

   /** Auto-completion popup. Lazy. Override [buildPopup] to change popup and [initPopup] to change its behavior. */
   protected val popup = lazy { buildPopup().apply { initPopup(this) } }
   /** AutoEvent reducer for [updateSuggestions] to reduce the number of suggestions evaluation calls. */
   protected val suggestionProviderEventReducer: EventReducer<String>
   /** If true, all user input changes are ignored. Primary used to avoid self triggering while auto-completing. */
   protected var ignoreInputChanges = false
   /** Auto-completion handlers */
   val onAutoCompleted = Handler1<T>()
   /** Whether the auto-completion popup hides when suggestion is accepted. */
   val hideOnSuggestion = v(true)
   /** Target node for auto completion */
   val completionTarget: Node
   /** Popup converter */
   private val converter: (T) -> String

   /**
    * Creates a new AutoCompletionBinding
    *
    * @param completionTarget The target node to which auto-completion shall be added
    * @param suggestionProvider The strategy to retrieve suggestions
    * @param converter The converter to be used to convert suggestions to strings
    */
   protected constructor(completionTarget: Node, suggestionProvider: (String) -> Collection<T>, converter: (T) -> String) {
      this.converter = converter
      this.completionTarget = completionTarget
      this.suggestionProviderEventReducer = toLast(250.0) { text ->
         runIO {
            suggestionProvider(text)
         } ui { suggestions ->
            if (!suggestions.isEmpty()) {
               popup.value.suggestions setTo suggestions
               showAutoCompletePopup()
            } else {
               hideAutoCompletePopup()
            }
         }
      }
   }

   protected open fun buildPopup() = AutoCompletePopup<T>()

   private fun initPopup(p: AutoCompletePopup<T>) {
      p.converter = object: StringConverter<T>() {
         override fun toString(o: T) = converter(o)
         override fun fromString(string: String?) = fail { "" }
      }
      p.onSuggestion += {
         try {
            ignoreInputChanges = true
            runLater { acceptSuggestion(it) }
            if (hideOnSuggestion.value) hideAutoCompletePopup()
            onAutoCompleted(it)
         } finally {
            ignoreInputChanges = false
         }
      }
   }

   abstract fun dispose()

   /** Set the current text the user has entered */
   fun updateSuggestions(userText: String) {
      if (!ignoreInputChanges)
         suggestionProviderEventReducer.push(userText)
   }

   /** Consumes user selected suggestion. Normally when user clicks or presses ENTER key on given suggestion. */
   protected abstract fun acceptSuggestion(suggestion: T)

   protected fun showAutoCompletePopup() {
      popup.value.show(completionTarget)
      popup.value.skin.node.minPrefMaxWidth = completionTarget.layoutBounds.width
      popup.value.selectFirstSuggestion()
   }

   /** Hide the auto-completion targets */
   protected fun hideAutoCompletePopup(): Unit = popup.orNull()?.hide().toUnit()

   private fun AutoCompletePopup<*>.selectFirstSuggestion() {
      skin.ifIs<AutoCompletePopupSkin<*>> {
         val list = it.node
         if (list.items!=null && list.items.isNotEmpty())
            list.selectionModel.select(0)
      }
   }

}