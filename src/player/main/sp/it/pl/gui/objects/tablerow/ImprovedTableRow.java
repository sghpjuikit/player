package sp.it.pl.gui.objects.tablerow;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import javafx.css.PseudoClass;
import javafx.scene.control.TableRow;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import sp.it.pl.util.reactive.Subscription;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;

/**
 * {@link TableRow} with additional methods.
 * <p/>
 * Fluent API for frequently used mouse handlers.<br/>
 * For example: {@code new ImprovedRow().onLeftSingleClick((row,event) -> ...);}
 * <p/>
 * It is possible to apply various pseudoclasses using predicates on the item T
 * of the row. See {@link #styleRuleAdd(javafx.css.PseudoClass, java.util.function.Predicate)}.<br/>
 * Note that if the nature of the item's property predicating rows style is not
 * observable (POJO) or is external, all the styles must be applied on change
 * manually. This can be done simply by calling table.refresh();
 */
public class ImprovedTableRow<T> extends TableRow<T> {

	public ImprovedTableRow() {
		super();

		setOnMouseClicked(e -> {
			if (!isEmpty()) {
				MouseButton b = e.getButton();
				int clicks = e.getClickCount();
				if (b==PRIMARY && clicks==1 && onL1Click!=null) onL1Click.accept(this, e);
				if (b==PRIMARY && clicks==2 && onL2Click!=null) onL2Click.accept(this, e);
				if (b==SECONDARY && clicks==1 && onR1Click!=null) onR1Click.accept(this, e);
				if (b==SECONDARY && clicks==2 && onR2Click!=null) onR2Click.accept(this, e);
			}
		});
	}

/* --------------------- MOUSE HANDLER FLUENT API ------------------------------------------------------------------- */

	private BiConsumer<ImprovedTableRow<T>,MouseEvent> onL1Click = null;
	private BiConsumer<ImprovedTableRow<T>,MouseEvent> onL2Click = null;
	private BiConsumer<ImprovedTableRow<T>,MouseEvent> onR1Click = null;
	private BiConsumer<ImprovedTableRow<T>,MouseEvent> onR2Click = null;

	/**
	 * Registers handler for single left click. Does nothing if row empty.
	 *
	 * @param handler, which takes this row as additional parameter.
	 * @return this
	 */
	public ImprovedTableRow<T> onLeftSingleClick(BiConsumer<ImprovedTableRow<T>,MouseEvent> handler) {
		onL1Click = handler;
		return this;
	}

	public ImprovedTableRow<T> onLeftDoubleClick(BiConsumer<ImprovedTableRow<T>,MouseEvent> handler) {
		onL2Click = handler;
		return this;
	}

	public ImprovedTableRow<T> onRightSingleClick(BiConsumer<ImprovedTableRow<T>,MouseEvent> handler) {
		onR1Click = handler;
		return this;
	}

	public ImprovedTableRow<T> onRightDoubleClick(BiConsumer<ImprovedTableRow<T>,MouseEvent> handler) {
		onR2Click = handler;
		return this;
	}

/* --------------------- CSS ---------------------------------------------------------------------------------------- */

	private final List<Rule<T>> styleRules = new ArrayList<>();

	/**
	 * Adds styling rule that updates row's pseudoclass based on condition.
	 *
	 * @param pseudoclass name of the pseudoclass as defined in css
	 * @param condition pseudoclass will be used when condition tests true
	 */
	public Subscription styleRuleAdd(PseudoClass pseudoclass, Predicate<T> condition) {
		Rule<T> rule = new Rule<>(pseudoclass, condition);
		styleRules.add(rule);
		return () -> styleRules.remove(rule);
	}

	@Override
	protected void updateItem(T item, boolean empty) {
		super.updateItem(item, empty);
		updateStyleRules();
	}

	/**
	 * Updates pseudoclasses based on the rules. Use to manually refresh the
	 * styles. This is necessary if the condition of the rule stops applying.
	 * In other words, every dynamic rule needs to observe some observable and
	 * call this method to refresh the style.
	 */
	public void updateStyleRules() {
		if (isEmpty()) return;
		styleRules.forEach(rule -> {
			boolean v = rule.condition.test(getItem());
			pseudoClassStateChanged(rule.pseudoclass, v);
		});
	}

	private static class Rule<T> {
		public final PseudoClass pseudoclass;
		public final Predicate<T> condition;

		private Rule(PseudoClass pseudoclass, Predicate<T> condition) {
			this.pseudoclass = pseudoclass;
			this.condition = condition;
		}
	}
}
