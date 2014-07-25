/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.tagging.Chapters;

import java.util.Objects;

/**
 *
 * @author uranium
 */
public final class CommentExtended implements Comparable<CommentExtended> {
    private String key;
    private String value;
    
    public CommentExtended(String _key, String val) {
        key = _key;
        value = val;
    }

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @param key the key to set
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }
    
    /**
     * Compares Comments.
     * @param o Comparing Object.
     * @return True only if their time is the same.
     */
    @Override
    public boolean equals(Object o) {
        if(this==o) return true; // this line can make a difference
        
        if (o == null || !(o instanceof CommentExtended)) { return false; }
        return getKey().equals(((CommentExtended) o).getKey()) && getValue().equals(((CommentExtended) o).getValue());
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 11 * hash + Objects.hashCode(this.key);
        hash = 11 * hash + Objects.hashCode(this.value);
        return hash;
    }
    
    /**
     * Compares object by natural order - alphabetically by key.
     */
    @Override
    public int compareTo(CommentExtended o) {
        return key.toLowerCase().compareTo(o.getKey().toLowerCase());
    }
    
    @Override
    public String toString() {
        return key + "\n" + value;
    }
}
