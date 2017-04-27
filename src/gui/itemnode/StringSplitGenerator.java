package gui.itemnode;

import gui.itemnode.ItemNode.ValueNode;
import javafx.scene.control.TextField;

public class StringSplitGenerator extends ValueNode<StringSplitParser> {
	private final TextField root = new TextField();

	public StringSplitGenerator() {
		root.setPromptText("expression");
		root.textProperty().addListener((o, ov, nv) -> generateValue(nv));
		generateValue("%Out%");
	}

	@Override
	public TextField getNode() {
		return root;
	}

	private void generateValue(String s) {
		try {
			root.setText(s);
			changeValue(new StringSplitParser(s));
		} catch (IllegalArgumentException e) {}    // ignore invalid values
	}

}