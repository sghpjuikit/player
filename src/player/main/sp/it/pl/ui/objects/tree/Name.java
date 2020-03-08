package sp.it.pl.ui.objects.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import sp.it.util.HierarchicalBase;
import static sp.it.util.dev.FailKt.noNull;
import static sp.it.util.functional.Util.by;
import static sp.it.util.functional.Util.stream;

public class Name extends HierarchicalBase<String,Name> {
	private static final char DELIMITER = '.';

	public static Name treeOfPaths(String rootName, Collection<String> paths) {
		Name root = new Name(rootName, "", null);
		paths.stream().distinct().forEach(root::addPath);
		root.sort();
		return root;
	}

	public final String pathUp;
	private List<Name> children;

	private Name(String value, Name parent) {
		this(value, parent==null || parent.pathUp.isEmpty() ? value : parent.pathUp + DELIMITER + value, parent);
	}

	private Name(String value, String pathUp, Name parent) {
		super(value, parent);
		this.pathUp = pathUp;
	}

	public void addPath(String path) {
		if (noNull(path).isEmpty()) return;

		int i = path.indexOf(DELIMITER);
		boolean isLeaf = i<0;

		if (isLeaf) {
			stream(getHChildren()).filter(name -> path.equals(name.value)).findFirst().ifPresentOrElse(
				name -> {},
				() -> getHChildren().add(new Name(path, this))
			);
		} else {
			String prefix = path.substring(0, i);
			String suffix = path.substring(i + 1);

			stream(getHChildren()).filter(name -> prefix.equals(name.value)).findFirst()
				.ifPresentOrElse(
					name -> name.addPath(suffix),
					() -> {
						Name name = new Name(prefix, this);
						getHChildren().add(name);
						name.addPath(suffix);
					}
				);
		}
	}

	public void sort() {
		if (children!=null) {
			children.sort(by(name -> name.value));
			children.forEach(Name::sort);
		}
	}

	@Override
	public List<Name> getHChildren() {
		if (children==null) children = new ArrayList<>();
		return children;
	}
}