/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Serialization.xstream;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;

/** XStream with registered converters for javaFX Properties. */
public final class XStreamFX extends XStream {

    public XStreamFX() {
        super();
        registerPropertyConverters(this);
    }

    public XStreamFX(HierarchicalStreamDriver hierarchicalStreamDriver) {
        super(hierarchicalStreamDriver);
        registerPropertyConverters(this);
    }

    /**
     * Utility to configure a xStream with JavaFX property converters.<br>
     * <br>
     * Created at 17/09/11 11:18.<br>
     *
     * @author Antoine Mischler <antoine@dooapp.com>
     */
    public static void registerPropertyConverters(XStream xStream) {
        xStream.registerConverter(new StringPropertyConverter(xStream.getMapper()));
        xStream.registerConverter(new BooleanPropertyConverter(xStream.getMapper()));
        xStream.registerConverter(new ObjectPropertyConverter(xStream.getMapper()));
        xStream.registerConverter(new DoublePropertyConverter(xStream.getMapper()));
        xStream.registerConverter(new LongPropertyConverter(xStream.getMapper()));
        xStream.registerConverter(new IntegerPropertyConverter(xStream.getMapper()));
        xStream.registerConverter(new com.dooapp.xstreamfx.ObservableListConverter(xStream.getMapper()));
    }
}
