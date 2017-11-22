package sp.it.pl.gui.itemnode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import sp.it.pl.gui.itemnode.ItemNode.ValueNode;
import sp.it.pl.gui.objects.combobox.ImprovedComboBox;
import sp.it.pl.util.access.V;
import sp.it.pl.util.collections.list.PrefList;
import sp.it.pl.util.conf.Config;
import sp.it.pl.util.conf.Config.AccessorConfig;
import sp.it.pl.util.functional.Functors.PƑ;
import sp.it.pl.util.functional.Functors.Ƒ1;
import static javafx.scene.layout.Priority.ALWAYS;
import static sp.it.pl.util.functional.Util.byNC;

/**
 * Value node containing function of one parameter {@link Ƒ1}.
 *
 * @param <I> type of function input
 * @param <O> type of function output
 */
public class FItemNode<I, O> extends ValueNode<Ƒ1<I,O>> {
	private final HBox root = new HBox(5);
	private final HBox paramB = new HBox(5);
	private final List<ConfigField> configs = new ArrayList<>();
	private final ComboBox<PƑ<I,O>> fCB;
	private boolean inconsistentState = false;

	public FItemNode(Supplier<PrefList<PƑ<I,O>>> functionPool) {
		fCB = new ImprovedComboBox<>(f -> f.name);
		fCB.getItems().setAll(functionPool.get());
		fCB.getItems().sort(byNC(f -> f.name));
		fCB.valueProperty().addListener((o, ov, nv) -> {
			configs.clear();
			paramB.getChildren().clear();
			nv.getParameters().forEach(p -> {
				V a = new V<>(p.defaultValue, v -> generateValue());
				Config cg = new AccessorConfig(p.type, p.name, p.description, a::setNapplyValue, a::getValue);
				ConfigField cf = ConfigField.create(cg);
				configs.add(cf);
				paramB.getChildren().add(cf.getNode());
			});
			if (!configs.isEmpty()) HBox.setHgrow(configs.get(configs.size() - 1).getNode(), ALWAYS);
			generateValue();
		});
		fCB.setValue(functionPool.get().getPreferredOrFirst()); // generate

		root.getChildren().addAll(fCB, paramB);
	}

	@Override
	public HBox getNode() {
		return root;
	}

	/**
	 * Focuses the first parameter's config field if any.
	 * <p/>
	 * {@inheritDoc }
	 */
	@Override
	public void focus() {
		if (!configs.isEmpty())
			configs.get(0).focus();
	}

	private void generateValue() {
		if (inconsistentState) return;
		PƑ<I,O> f = fCB.getValue();
		changeValue(f.toƑ1(configs.stream().map(ConfigField::getValue).toArray()));
	}

	public Class getTypeIn() {
		PƑ<I,O> f = fCB.getValue();
		return f==null ? Void.class : f.in;
	}

	public Class getTypeOut() {
		PƑ<I,O> f = fCB.getValue();
		return f==null ? Void.class : f.out;
	}

	public void clear() {
		inconsistentState = true;
		configs.forEach(ConfigField::setNapplyDefault);
		inconsistentState = false;
		generateValue();
	}

}