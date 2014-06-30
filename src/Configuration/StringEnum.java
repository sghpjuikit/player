
package Configuration;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author Plutonium_
 */
public abstract class StringEnum {
    private String s;
    
    public StringEnum() {}
    public StringEnum(String skin) {
        add(skin);
        s = skin;
    }
    
    /**
     * Implementation should return its own static values() of values.
     * @return  value values()*/
    public abstract List<String> values();
    
    /** @return  value*/
    public String get() { 
        return s;
    }
    public void set(String item) {
        s = item;
    }

    public int ordinal() {
        return values().indexOf(s);
    }
    
    public String next() {
        if (isEmpty()) return null;
        if (ordinal()==values().size()-1) return values().get(0);
        else return values().get(ordinal()+1);
    }
    
    public String next(String skin) {
        if (isEmpty() || !contains(skin)) return null;
        if (values().indexOf(skin) == values().size()-1) return values().get(0);
        else return values().get(values().indexOf(skin)+1);
    }    
    
    public boolean isEmpty() {
        return values().isEmpty();
    }
    public final void add(String item) {
        Objects.requireNonNull(item);
        if (!contains(item)) values().add(item);
    }
    public void remove(String skin) {
        values().remove(skin);
    }
    public boolean contains(String item) {
        return values().contains(item);
    }    
    public void removeAll() {
        values().clear();
    }
    
    /** parses from string */
    public abstract StringEnum valueOf(String str);

    @Override
    public String toString() {
        return get();
    }
    
    public List<StringEnum> valuesOrig() {
        return values().stream().map(ss->{
            try {
                return (StringEnum)getClass().getConstructor(String.class).newInstance(ss);
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
                Logger.getLogger(StringEnum.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }
    
    @Override
    public boolean equals(Object o){
        if (o==this) return true;
        if (! (o instanceof StringEnum)) return false;
        if (s== null) s = "";
        return s.equals(((StringEnum)o).s);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 11 * hash + Objects.hashCode(this.s);
        return hash;
    }
    
}
