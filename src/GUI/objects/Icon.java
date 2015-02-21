/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects;

import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import java.util.List;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableValue;
import javafx.css.*;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;

/**
 <p>
 @author Plutonium_
 */
public class Icon extends Label {
    
    private static final StyleablePropertyFactory<Icon> FACTORY = new StyleablePropertyFactory(Label.getClassCssMetaData());
    private static final CssMetaData<Icon, AwesomeIcon> ICON_CMD = FACTORY.createEnumCssMetaData(AwesomeIcon.class, "icon", i -> i.icon);
    
    
    
    public final StyleableProperty<AwesomeIcon> icon = new SimpleStyleableObjectProperty<AwesomeIcon>(ICON_CMD, this, "icon") {
        @Override public void set(AwesomeIcon v) {
            super.set(v);
            AwesomeDude.setIcon(Icon.this, v, String.valueOf(icon_size.get()));
        }
    };
    
     public ObservableValue<AwesomeIcon> iconProperty() { return ( ObservableValue<AwesomeIcon>)icon; }
     public final AwesomeIcon getIcon() { return icon.getValue(); }
     public final void setIcon(AwesomeIcon isSelected) { icon.setValue(isSelected); }
    
    
    public final IntegerProperty icon_size = new SimpleIntegerProperty(11);

    public Icon() {
        getStyleClass().add("icon");
    }
    public Icon(AwesomeIcon i) {
        icon.setValue(i);
        getStyleClass().add("icon");
    }
    public Icon(AwesomeIcon i, int size, String tooltip) {
        this(i, size, tooltip, (EventHandler)null);
    }
    public Icon(AwesomeIcon ico, int size, String tooltip, EventHandler<MouseEvent> onClick) {
        icon_size.set(size);
        if(ico!=null) icon.setValue(ico);
        if(tooltip!=null && !tooltip.isEmpty()) setTooltip(new Tooltip(tooltip));
        if(onClick!=null) setOnMouseClicked(onClick);
        getStyleClass().add("icon");
    }
    public Icon(AwesomeIcon ico, int size, String tooltip, Runnable onClick) {
        this(ico, size, tooltip, onClick==null ? null : e -> onClick.run());
    }
    
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return FACTORY.getCssMetaData();
    }

     @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return FACTORY.getCssMetaData();
    }
     
}

// private DoubleProperty gapProperty = new StyleableDoubleProperty(0) {
//      @Override 
//      public CssMetaData <MyWidget,Number > getCssMetaData() {
//          return GAP_META_DATA;
//      }
//
//       @Override
//      public Object getBean() {
//          return MyWidget.this;
//      }
//
//       @Override
//      public String getName() {
//          return "gap";
//      }
// };
// 
// private static final CssMetaData GAP_META_DATA = 
//     new CssMetaData <MyWidget,Number >("-my-gap", StyleConverter.getSizeConverter(), 0d) {
// 
//         @Override
//        public boolean isSettable(MyWidget node) {
//            return node.gapProperty == null || !node.gapProperty.isBound();
//        }
//    
//         @Override
//        public StyleableProperty <Number > getStyleableProperty(MyWidget node) {
//            return (StyleableProperty <Number >)node.gapProperty;
//        }
// };
// 
// private static final List <CssMetaData <? extends Node, ? > > cssMetaDataList; 
// static {
//     List <CssMetaData <? extends Node, ? > > temp =
//         new ArrayList <CssMetaData <? extends Node, ? > >(Control.getClassCssMetaData());
//     temp.add(GAP_META_DATA);
//     cssMetaDataList = Collections.unmodifiableList(temp);
// }
// 
// public static List <CssMetaData <? extends Node, ? > > getClassCssMetaData() {
//     return cssMetaDataList;
// }
// 
//  @Override
// public List <CssMetaData <? extends Node, ? > > getCssMetaData() {
//     return getClassCssMetaData();
// }

//methods greatly reduce the amount of boiler-plate code needed to implement the 
//StyleableProperty and CssMetaData. These methods take a Function<? extends Styleable
//, StyleableProperty<?>> which returns a reference to the property itself. See the 
//example below. Note that for efficient use of memory and for better CSS performance,
//creating the StyleablePropertyFactory as a static member, as shown below, is recommended. 
// public final class MyButton extends Button {
//
//     private static final StyleablePropertyFactory<MyButton> FACTORY = new StyleablePropertyFactory<>(Button.getClassCssMetaData());
//
//     MyButton(String labelText) {
//         super(labelText);
//         getStyleClass().add("my-button");
//     }
//
//     // Typical JavaFX property implementation
//     public ObservableValue<Boolean> selectedProperty() { return ( ObservableValue<Boolean>)icon; }
//     public final boolean isSelected() { return icon.getValue(); }
//     public final void setSelected(boolean isSelected) { icon.setValue(isSelected); }
//
//     // StyleableProperty implementation reduced to one line
//     private final StyleableProperty<Boolean> icon =
//         FACTORY.createStyleableBooleanProperty(this, "icon", "-my-icon", s -> s.icon);
//
//     @Override
//     public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
//         return FACTORY.getCssMetaData();
//     }
//
// }
// 
//The example above is the simplest use of StyleablePropertyFactory. But, this use 
//does not provide the static CssMetaData that is useful for getClassCssMetaData(), 
//which is described in the javadoc for CssMetaData). Static CssMetaData can, however
//, be created via StyleablePropertyFactory methods and will be returned by the methods
//which create StyleableProperty instances, as the example below illustrates. Note that 
//the static method getClassCssMetaData() is a convention used throughout the JavaFX code
//base but the getClassCssMetaData() method itself is not used at runtime.
// public final class MyButton extends Button {
//
//     private static final StyleablePropertyFactory<MyButton> FACTORY =
//         new StyleablePropertyFactory<>(Button.getClassCssMetaData());
//
//     private static final CssMetaData<MyButton, Boolean> SELECTED =
//         FACTORY.createBooleanCssMetaData("-my-icon", s -> s.icon, false, false);
//
//     MyButton(String labelText) {
//         super(labelText);
//         getStyleClass().add("my-button");
//     }
//
//     // Typical JavaFX property implementation
//     public ObservableValue<Boolean> selectedProperty() { return ( ObservableValue<Boolean>)icon; }
//     public final boolean isSelected() { return icon.getValue(); }
//     public final void setSelected(boolean isSelected) { icon.setValue(isSelected); }
//
//     // StyleableProperty implementation reduced to one line
//     private final StyleableProperty<Boolean> icon =
//         new SimpleStyleableBooleanProperty(SELECTED, this, "icon");
//
//     public static  List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
//         return FACTORY.getCssMetaData();
//     }
//
//      @Override
//     public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
//         return FACTORY.getCssMetaData();
//     }
//
// }
// 
//The same can be accomplished with an inner-class. The previous example called new
//    SimpleStyleableBooleanProperty to create the icon property. This example 
//uses the factory to access the CssMetaData that was created along with the anonymous
//inner-class. For all intents and purposes, the two examples are the same.
// public final class MyButton extends Button {
//
//     private static final StyleablePropertyFactory<MyButton> FACTORY =
//         new StyleablePropertyFactory<>(Button.getClassCssMetaData()) {
//         {
//             createBooleanCssMetaData("-my-icon", s -> s.icon, false, false);
//         }
//     }
//
//
//     MyButton(String labelText) {
//         super(labelText);
//         getStyleClass().add("my-button");
//     }
//
//     // Typical JavaFX property implementation
//     public ObservableValue<Boolean> selectedProperty() { return ( ObservableValue<Boolean>)icon; }
//     public final boolean isSelected() { return icon.getValue(); }
//     public final void setSelected(boolean isSelected) { icon.setValue(isSelected); }
//
//     // StyleableProperty implementation reduced to one line
//     private final StyleableProperty<Boolean> icon =
//         new SimpleStyleableBooleanProperty(this, "icon", "my-icon");
//
//     public static  List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
//         return FACTORY.getCssMetaData();
//     }
//
//      @Override
//     public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
//         return FACTORY.getCssMetaData();
//     }
//
// }
// 
//Caveats:
//The only option for creating a StyleableProperty with a number value is to create
//a StyleableProperty<Number> . The return value from the getValue() method of the 
//StyleableProperty is a Number. Therefore, the get method of the JavaFX property
//needs to call the correct value method for the return type. For example, 
//     public ObservableValue  offsetProperty() { return (ObservableValue )offset; }
//     public Double getOffset() { return offset.getValue().doubleValue(); }
//     public void setOffset(Double value) { offset.setValue(value); }
//     private final StyleableProperty  offset = FACTORY.createStyleableNumberProperty(this, "offset", "-my-offset", s -> ((MyButton)s).offset);
 