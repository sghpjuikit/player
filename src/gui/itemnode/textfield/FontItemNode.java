package gui.itemnode.textfield;

import javafx.scene.text.Font;

import gui.Gui;
import gui.objects.picker.FontSelectorDialog;
import util.parsing.Parser;

/**
 *
 * @author Martin Polakovic
 */
public class FontItemNode extends TextFieldItemNode<Font> {

    public FontItemNode() {
        super(Parser.DEFAULT.toConverterOf(Font.class));
    }

    @Override
    void onDialogAction() {
        new FontSelectorDialog(Gui.font.get())
	            .showAndWait()
                .ifPresent(this::setValue);
    }
}