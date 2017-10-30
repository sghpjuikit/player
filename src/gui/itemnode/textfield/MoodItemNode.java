package gui.itemnode.textfield;

import audio.tagging.Metadata;
import gui.objects.picker.MoodPicker;
import gui.objects.popover.PopOver;
import gui.objects.popover.NodePos;
import util.access.V;
import util.parsing.Parser;
import static gui.objects.textfield.autocomplete.AutoCompletion.autoComplete;
import static main.App.APP;
import static util.functional.Util.filter;

/**
 * {@link gui.itemnode.textfield.TextFieldItemNode} for audio mood tagging values.<br/>
 * It provides two additional functionalities
 * <ul>
 * <li> Auto-completion from set of moods application is aware of
 * <li> Mood picker popup. The position of the picker popup can be customized.
 * </ul>
 */
public class MoodItemNode extends TextFieldItemNode<String> {

	/** The position for the picker to show on. */
	public final V<NodePos> pos = new V<>(NodePos.RightCenter);

	public MoodItemNode() {
		super(Parser.DEFAULT.toConverterOf(String.class));
		setEditable(true);
		autoComplete(this, p -> filter(APP.db.getStringPool().getStrings(Metadata.Field.MOOD.name()), t -> APP.db.getAutocompletionFilter().apply(t, p.getUserText())));
	}

	@Override
	void onDialogAction() {
		MoodPicker picker = new MoodPicker();
		PopOver<?> p = new PopOver<>(picker.getNode());
		p.detachable.set(false);
		p.setArrowSize(0);
		p.setArrowIndent(0);
		p.setCornerRadius(0);
		p.setAutoHide(true);
		p.setAutoFix(true);
		picker.onCancel = p::hide;
		picker.onSelect = mood -> {
			setValue(mood);
			p.hide();
		};
		picker.getNode().setPrefSize(800, 600);
		p.show(this, pos.get());
	}

}