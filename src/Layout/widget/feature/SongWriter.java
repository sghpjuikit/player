
package Layout.widget.feature;

/**
 * Capable of writing data to song tags.
 * 
 * @author Plutonium_
 */
@Feature(
  name = "Song metadata writer", 
  description = "Capable of writing data to song tags", 
  type = SongWriter.class
)
public interface SongWriter extends SongReader {}
