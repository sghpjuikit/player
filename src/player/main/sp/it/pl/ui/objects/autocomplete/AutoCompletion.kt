/*
 * Implementation based on ControlsFX
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

import java.util.Objects
import javafx.scene.control.TextInputControl
import javafx.scene.input.KeyCode.SPACE
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_PRESSED
import sp.it.util.functional.asIf
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp

/**
 * Represents a binding between a text field and an auto-completion popup
 *
 * @param <T> type of suggested objects
 */
open class AutoCompletion<T>: AutoCompletionBinding<T> {

   /** String converter to be used to convert suggestions to strings. */
   protected val converter: (T) -> String
   /** [completionTarget] of more precise type. */
   protected val completionTargetTyped: TextInputControl
   /** Disposer called on [dispose]. */
   protected val disposer = Disposer()

   /** Creates an auto-completion binding between the specified textField and suggestions. */
   internal constructor(textField: TextInputControl, suggestionProvider: (String) -> Collection<T>, converter: (T) -> String): super(textField, suggestionProvider, converter) {
      this.completionTargetTyped = textField
      this.converter = converter

      // show suggestions on text change
      textField.textProperty().attach(disposer) {
         if (it!=null && completionTarget.isFocused)
            updateSuggestions(it)
      }
      // hide suggestions on focus lost
      textField.focusedProperty().attach(disposer) {
         if (!it)
            hideAutoCompletePopup()
      }
      // toggle suggestions on CTRL+SPACE
      textField.onEventDown(KEY_PRESSED, SPACE, false) {
         if (it.isShortcutDown) {
            updateSuggestions("")
            it.consume()
         }
      } on disposer
      // toggle suggestions on MOUSE_CLICK
      var wasShowing = false
      textField.onEventUp(MOUSE_PRESSED, PRIMARY, consume = false) {
         wasShowing = isAutoCompletePopupShowingLater
      } on disposer
      textField.onEventDown(MOUSE_CLICKED, PRIMARY, consume = false) {
         if (it.isStillSincePress)
            if (isAutoCompletePopupShowingLater || wasShowing) hideAutoCompletePopup() else updateSuggestions("")
      } on disposer
   }

   override fun dispose() = disposer()

   override fun Ctx.acceptSuggestion(suggestion: T) {
      if (completionTargetTyped.isEditable) {
         val newText = converter(suggestion)
         completionTargetTyped.userData = suggestion // set userData first so text listeners see new value
         completionTargetTyped.text = newText
         completionTargetTyped.positionCaret(newText.length)
      }
   }

   companion object {

      fun <T> defaultStringConverter(): (T) -> String = Objects::toString

      fun <T> autoComplete(textField: TextInputControl, suggestionProvider: (String) -> Collection<T>, converter: (T) -> String): Subscription {
         val a = AutoCompletion(textField, suggestionProvider, converter)
         textField.properties["autocomplete"] = a
         return Subscription {
            textField.properties - "autocomplete"
            a.dispose()
         }
      }

      fun <T> autoComplete(textField: TextInputControl, suggestionProvider: (String) -> Collection<T>) = autoComplete(textField, suggestionProvider, defaultStringConverter())

      fun <T> autoComplete(textField: TextInputControl, suggestionProvider: Collection<T>) = autoComplete(textField, { suggestionProvider })

      @SafeVarargs
      fun <T> autoComplete(textField: TextInputControl, vararg suggestionProvider: T) = autoComplete(textField, listOf(suggestionProvider))

      inline fun <reified T> of(textField: TextInputControl): AutoCompletion<T>? = textField.properties["autocomplete"].asIf()

   }

}