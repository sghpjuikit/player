package sp.it.pl.gui.itemnode.textfield;

import javafx.scene.text.Font;
import sp.it.pl.gui.objects.picker.FontSelectorDialog;
import sp.it.pl.util.parsing.Parser;

public class FontItemNode extends TextFieldItemNode<Font> {

	public FontItemNode() {
		super(Parser.DEFAULT.toConverterOf(Font.class));
	}

	@Override
	void onDialogAction() {
		new FontSelectorDialog(getValue(), this::setValue).show(this);
	}
}