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

package sp.it.pl.gui.objects.autocomplete

import javafx.scene.control.TextField
import javafx.util.StringConverter
import sp.it.pl.util.reactive.Disposer
import sp.it.pl.util.reactive.Subscription
import sp.it.pl.util.reactive.attach

/**
 * Represents a binding between a text field and a auto-completion popup
 *
 * @param <T> type of suggested objects
 */
open class AutoCompletion<T>: AutoCompletionBinding<T> {

    /** String converter to be used to convert suggestions to strings. */
    protected val converter: StringConverter<T>
    /** [completionTarget] of more precise type. */
    protected val completionTargetTyped: TextField
    /** Disposer called on [dispose]. */
    protected val disposer = Disposer()

    /** Creates an auto-completion binding between the specified textField and suggestions. */
    internal constructor(textField: TextField, allSuggestions: (String) -> Collection<T>, converter: StringConverter<T>): super(textField, allSuggestions, converter) {
        this.completionTargetTyped = textField
        this.converter = converter

        textField.textProperty().attach(disposer) {
            if (it!=null && completionTarget.isFocused)
                updateSuggestions(it)
        }
        textField.focusedProperty().attach(disposer) {
            if (!it)
                hideAutoCompletePopup()
        }
    }

    override fun dispose() = disposer()

    override fun acceptSuggestion(suggestion: T) {
        val newText = converter.toString(suggestion)
        completionTargetTyped.text = newText
        completionTargetTyped.positionCaret(newText.length)
    }

    companion object {

        @Suppress("UNCHECKED_CAST")
        fun <T> defaultStringConverter() = object: StringConverter<T>() {
            override fun toString(t: T?) = t?.toString()
            override fun fromString(string: String) = string as T
        }

        fun <T> autoComplete(textField: TextField, allSuggestions: (String) -> Collection<T>, converter: StringConverter<T>): Subscription {
            val a = AutoCompletion(textField, allSuggestions, converter)
            return Subscription { a.dispose() }
        }

        fun <T> autoComplete(textField: TextField, allSuggestions: (String) -> Collection<T>) = autoComplete(textField, allSuggestions, defaultStringConverter())

        fun <T> autoComplete(textField: TextField, allSuggestions: Collection<T>) = autoComplete(textField, { allSuggestions })

        @SafeVarargs
        fun <T> autoComplete(textField: TextField, vararg allSuggestions: T) = autoComplete(textField, listOf(allSuggestions))

    }

}