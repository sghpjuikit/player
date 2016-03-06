/**
 * Impl based on ControlsF:
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

package gui.objects.textfield.autocomplete;

import java.util.Arrays;
import java.util.Collection;

import javafx.beans.value.ChangeListener;
import javafx.scene.control.TextField;
import javafx.util.Callback;
import javafx.util.StringConverter;


/**
 * Represents a binding between a text field and a auto-completion popup
 *
 * @param <T> type of suggested objects
 */
public class AutoCompletion<T> extends AutoCompletionBinding<T> {

    private static <T> StringConverter<T> defaultStringConverter() {
        return new StringConverter<T>() {
            @Override public String toString(T t) {
                return t == null ? null : t.toString();
            }
            @SuppressWarnings("unchecked")
            @Override public T fromString(String string) {
                return (T) string;
            }
        };
    }


    /** String converter to be used to convert suggestions to strings. */
    StringConverter<T> converter;
    private final ChangeListener<String> textChangeListener = (o,ov,nv) -> {
        if (getCompletionTarget().isFocused())
            setUserInput(nv);
    };
    private final ChangeListener<Boolean> focusChangedListener = (o,ov,nv) -> {
        if(nv == false)
            hidePopup();
    };


    /**
     * Creates a new auto-completion binding between the given textField
     * and the given suggestion provider.
     *
     * @param textField
     * @param suggestionProvider
     */
    public AutoCompletion(TextField textField, Callback<ISuggestionRequest, Collection<T>> suggestionProvider, StringConverter<T> converter) {
        super(textField, suggestionProvider, converter);
        this.converter = converter;

        getCompletionTarget().textProperty().addListener(textChangeListener);
        getCompletionTarget().focusedProperty().addListener(focusChangedListener);
    }

    /**
     * Creates a new auto-completion binding between the given textField
     * and the given suggestion provider.
     *
     * @param textField
     * @param suggestionProvider
     */
    public AutoCompletion(final TextField textField, Callback<ISuggestionRequest, Collection<T>> suggestionProvider) {
        this(textField, suggestionProvider, defaultStringConverter());
    }

    public AutoCompletion(TextField textField, Collection<T> possibleSuggestions) {
        this(textField, SuggestionProvider.create(possibleSuggestions));
    }

    public AutoCompletion(TextField textField, T... possibleSuggestions) {
        this(textField, Arrays.asList(possibleSuggestions));
    }


    @Override
    public TextField getCompletionTarget(){
        return (TextField)super.getCompletionTarget();
    }

    @Override
    public void dispose(){
        getCompletionTarget().textProperty().removeListener(textChangeListener);
        getCompletionTarget().focusedProperty().removeListener(focusChangedListener);
    }

    @Override
    protected void acceptSuggestion(T completion){
        String newText = converter.toString(completion);
        getCompletionTarget().setText(newText);
        getCompletionTarget().positionCaret(newText.length());
    }

}