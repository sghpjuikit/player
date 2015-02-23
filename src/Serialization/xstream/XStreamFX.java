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

    /** Registers javaFX property converters to provided xstream */
    public static void registerPropertyConverters(XStream x) {
        x.registerConverter(new StringPropertyConverter(x.getMapper()));
        x.registerConverter(new BooleanPropertyConverter(x.getMapper()));
        x.registerConverter(new ObjectPropertyConverter(x.getMapper()));
        x.registerConverter(new DoublePropertyConverter(x.getMapper()));
        x.registerConverter(new LongPropertyConverter(x.getMapper()));
        x.registerConverter(new IntegerPropertyConverter(x.getMapper()));
        x.registerConverter(new ObservableListConverter(x.getMapper()));
    }
}
