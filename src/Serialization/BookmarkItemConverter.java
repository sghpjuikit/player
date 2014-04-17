/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Serialization;

import Library.BookmarkItem;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.net.URI;

/**
 * @author uranium
 * 
 * Converter class used for de/serializing PlaylistItem class with XStream.
 */
public class BookmarkItemConverter implements Converter {
    @Override
    public boolean canConvert(Class type) {
        return type.equals(BookmarkItem.class);
    }

    @Override
    public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
        BookmarkItem item = (BookmarkItem) value;
        writer.startNode("uri");
        writer.setValue(item.getURI().toString());
        writer.endNode();
        writer.startNode("name");
        writer.setValue(item.getName());
        writer.endNode();
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        reader.moveDown();
        URI uri = URI.create(reader.getValue());
        reader.moveUp();
        reader.moveDown();
        String name = reader.getValue();
        reader.moveUp();
        return new BookmarkItem(name, uri);
    }
}
