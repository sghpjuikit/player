
package layout.widget.feature;

/**
 * Capable of writing data to song tags.
 * 
 * @author Martin Polakovic
 */
@Feature(
  name = "Song metadata writer", 
  description = "Capable of writing data to song tags", 
  type = SongWriter.class
)
public interface SongWriter extends SongReader {}
