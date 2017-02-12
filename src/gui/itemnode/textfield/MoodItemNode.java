package gui.itemnode.textfield;

import audio.tagging.Metadata;
import gui.objects.picker.MoodPicker;
import gui.objects.popover.PopOver;
import services.database.Db;
import util.parsing.Parser;

import static gui.objects.textfield.autocomplete.AutoCompletion.autoComplete;
import static util.functional.Util.filter;

/**
 * Text field intended for mood tagging specifically. It provides two additional
 * functionalities - auto-completion from list moods registered in application
 * and button to open mood picker popup. The position of the picker popup can be
 * customized.
 *
 * @author Martin Polakovic
 */
public class MoodItemNode extends TextFieldItemNode<String> {

    private PopOver.NodePos pos = PopOver.NodePos.RightCenter;

    public MoodItemNode() {
        super(Parser.DEFAULT.toConverterOf(String.class));
        setEditable(true);
	    autoComplete(this, p -> filter(Db.string_pool.getStrings(Metadata.Field.MOOD.name()), t -> Db.autocompletionFilter.apply(t,p.getUserText())));
    }

    /** @return the position for the picker to show on */
    public PopOver.NodePos getPos() {
        return pos;
    }

    /** @param pos the position for the picker to show on */
    public void setPos(PopOver.NodePos pos) {
        this.pos = pos;
    }

    @Override
    void onDialogAction() {
        MoodPicker picker = new MoodPicker();
        PopOver p = new PopOver(picker.getNode());
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
        picker.getNode().setPrefSize(800,600);
        p.show(this, pos);
    }

}