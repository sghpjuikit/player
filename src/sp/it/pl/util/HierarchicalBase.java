package sp.it.pl.util;

public abstract class HierarchicalBase<T, H extends Hierarchical<H>> implements Hierarchical<H> {

	public final T val;
	public final H parent;

	public HierarchicalBase(T value, H parent) {
		this.val = value;
		this.parent = parent;
	}

	@Override
	public H getHParent() {
		return parent;
	}

}