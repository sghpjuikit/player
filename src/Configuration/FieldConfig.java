/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Configuration;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.util.Objects;

import Configuration.Config.ConfigBase;

/**
 * Class level {@link Config}.
 * <p>
 * Wraps static {@link Field}.
 * <p>
 * Use for class level configurations.
 *
 * @author Plutonium_
 */
public final class FieldConfig<T> extends ConfigBase<T> {

    private final Object instance;
    MethodHandle getter;
    MethodHandle setter;
    MethodHandle applier;

    /**
     * @param _name
     * @param c
     * @param category
     * @param instance owner of the field or null if static
     */
    FieldConfig(String _name, IsConfig c, Object instance, String category, MethodHandle getter, MethodHandle setter) {
        super(_name, c, getValueFromMethodHelper(getter, instance), category);
        this.getter = getter;
        this.setter = setter;
        this.instance = instance;
    }

    /** {@inheritDoc} */
    @Override
    public T getValue() {
        return getValueFromMethodHelper(getter, instance);
    }

    /** {@inheritDoc} */
    @Override
    public void setValue(T val) {
        try {
            if(instance==null) setter.invokeWithArguments(val);
            else setter.invokeWithArguments(instance,val);
        } catch (Throwable e) {
            throw new RuntimeException("Error setting config field " + getName(),e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void applyValue(T val) {
        if(applier != null) {
            try {
                int i = applier.type().parameterCount();

                if(i==1) applier.invokeWithArguments(val);
                else applier.invoke();
            } catch (Throwable e) {
                throw new RuntimeException("Error applying config field " + getName(),e);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public Class getType() {
        return getValue().getClass();
    }

    /**
     * Equals if and only if non null, is Config type and source field is equal.
     */
    @Override
    public boolean equals(Object o) {
        if(this==o) return true;

        if (o == null || !(o instanceof FieldConfig)) return false;

        FieldConfig c = (FieldConfig)o;
        return setter.equals(c.setter) && getter.equals(c.getter) &&
               applier.equals(c.applier);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 23 * hash + Objects.hashCode(this.applier);
        hash = 23 * hash + Objects.hashCode(this.getter);
        hash = 23 * hash + Objects.hashCode(this.setter);
        return hash;
    }

/******************************************************************************/

    private static<T> T getValueFromMethodHelper(MethodHandle mh, Object instance) {
        try {
            if(instance==null) return (T) mh.invoke();
            else return (T) mh.invokeWithArguments(instance);
        } catch (Throwable e) {
            throw new RuntimeException("Error during getting value from a config field. ",e);
        }
    }
}
