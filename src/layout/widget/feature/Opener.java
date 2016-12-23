package layout.widget.feature;

@Feature(
	name = "Opener",
	description = "Capable of opening any data ",
	type = Opener.class
)
public interface Opener {

	/**
	 *  Opens the data. This can be any object, including an array or collection.
	 *  <p/>
	 *  The way the data is handled is up to the implementation.
	 *
	 * @param data data to be opened
	 */
	void open(Object data);

}