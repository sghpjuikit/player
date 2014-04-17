
package Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Plutonium_
 */
public final class SkinEnum extends StringEnum {
    private static final List<String> list = new ArrayList<>();
    private String s;
    
    public SkinEnum() {
    }
    public SkinEnum(String skin) {
        add(skin);
        s = skin;
    }
    
    @Override
    public String get() { 
        return s;
    }
    
    @Override
    public List<String> values() {
        return list;
    }
    
//    public List<SkinEnum> valuesOrig() {
//        return list.stream().map(SkinEnum::new).collect(Collectors.toList());
//    }
    
    @Override
    public SkinEnum valueOf(String str) {
        if (list.contains(str)) return new SkinEnum(str);
        else return new SkinEnum();
    }
    
    
    
}
