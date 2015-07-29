
/* 
 * Copyright 2014 Jens Deters.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package util;

import javafx.event.EventHandler;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyEvent;

/**
 *
 * @author Jens Deters
 */
public class InputConstraints {

    private InputConstraints() {
        // not needed here
    }

    public static void noLeadingAndTrailingBlanks(final TextInputControl textField) {
        textField.addEventFilter(KeyEvent.KEY_TYPED, createNoLeadingBlanksInputHandler());
        textField.focusedProperty().addListener(o -> {
            textField.setText(textField.getText().trim());
        });
    }

    public static void noLeadingBlanks(final TextInputControl textField) {
        textField.addEventFilter(KeyEvent.KEY_TYPED, createNoLeadingBlanksInputHandler());
    }

    public static void noBlanks(final TextInputControl textField) {
        textField.addEventFilter(KeyEvent.KEY_TYPED, createNoBlanksInputHandler());
        textField.focusedProperty().addListener(o -> {
            textField.setText(textField.getText().trim());
        });
    }
    
    public static void numbersOnly(final TextInputControl textField, boolean negative, boolean floating) {
        numbersOnly(textField, Integer.MAX_VALUE, negative, floating);
    }

    public static void numbersOnly(final TextInputControl textField, int maxLenth, boolean negative, boolean floating) {
        textField.addEventFilter(KeyEvent.KEY_TYPED, createNumbersOnlyInputHandler(negative ? 1+maxLenth : maxLenth, negative, floating));
        textField.focusedProperty().addListener(o -> {
            textField.setText(textField.getText().trim());
        });
    }

    public static void lettersOnly(final TextInputControl textField) {
        lettersOnly(textField, Integer.MAX_VALUE);
    }

    public static void lettersOnly(final TextInputControl textField, int maxLenth) {
        textField.addEventFilter(KeyEvent.KEY_TYPED, createLettersOnlyInputHandler(maxLenth));
        textField.focusedProperty().addListener(o -> {
            textField.setText(textField.getText().trim());
        });
    }

    public static EventHandler<KeyEvent> createNoLeadingBlanksInputHandler() {
        return (KeyEvent event) -> {
            if (event.getSource() instanceof TextInputControl) {
                TextInputControl textField = (TextInputControl) event.getSource();
                if (" ".equals(event.getCharacter()) && textField.getCaretPosition() == 0) {
                    event.consume();
                }
            }
        };
    }

    public static EventHandler<KeyEvent> createNumbersOnlyInputHandler(int maxLength, boolean negative, boolean floating) {
        String n = negative ? "//-" : "";
        String f = floating ? "." : "";
        String pattern = "[0-9" + f + n + "]";
        return createPatternInputHandler(maxLength, pattern);
    }
    public static EventHandler<KeyEvent> createNumbersOnlyInputHandler(int maxLength) {
        return createPatternInputHandler(maxLength, "[0-9.//-]");
    }

    public static EventHandler<KeyEvent> createLettersOnlyInputHandler(int maxLength) {
        return createPatternInputHandler(maxLength, "[A-Za-z]");
    }

    public static EventHandler<KeyEvent> createNoBlanksInputHandler() {
        return (KeyEvent event) -> {
            if (event.getSource() instanceof TextInputControl) {
                if (" ".equals(event.getCharacter())) {
                    event.consume();
                }
            }
        };
    }

    public static EventHandler<KeyEvent> createPatternInputHandler(int maxLength, String pattern) {
        return e -> {
            if (e.getSource() instanceof TextInputControl) {
                TextInputControl textField = (TextInputControl) e.getSource();
                if (textField.getText().length() >= maxLength || !e.getCharacter().matches(pattern)) {
                    e.consume();
                }
            }
        };
    }

}