/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.tagging;

import java.util.Objects;
import javafx.util.Duration;

/**
 *
 * @author uranium
 */
public final class Chapter implements Comparable<Chapter> {
    private Duration time;
    private String info;

    public Chapter(Duration time) {
        this(time, "");
    }
    
    public Chapter(Duration time, String _info) {
        setTime(time);
        info = _info;
    }
    
    public Chapter(String text) {
        int i = text.indexOf('-');
        if (i==-1) throw new IllegalArgumentException("Not parsable chapter string: " + text);
        String s = text.substring(0,i);
        try {
            setTimeInMillis(Double.parseDouble(s));
        } catch(NumberFormatException e) {
            throw new IllegalArgumentException("Not parsable chapter string: " + text);
        }
        info = text.substring(i+1, text.length());
    }
    
    /**
     * @return the time with granularity of 1ms
     */
    public Duration getTime() {
        return time;
    }

    /**
     * @param time the time to set
     */
    public void setTime(Duration time) {
        setTimeInMillis(time.toMillis());
    }
    
    /**
     * @param time the time to set
     */
    public void setTimeInMillis(double millis) {
        this.time = new Duration(Math.rint(millis));
    }
    
    /**
     * @return the info
     */
    public String getInfo() {
        return info;
    }

    /**
     * @param info the info to set
     */
    public void setInfo(String info) {
        this.info = info;
    }
    
    /**
     * @return true f and only if their time is the same.
     */
    @Override
    public boolean equals(Object o) {
        return (o == null || !(o instanceof Chapter)) 
                ? false
                : getTime().equals(((Chapter)o).getTime());
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 19 * hash + Objects.hashCode(this.time);
        return hash;
    }
    
    /**
     * Compares chapter by natural order - by time.
     * @param o
     * @return 
     */
    @Override
    public int compareTo(Chapter o) {
        return time.greaterThan(o.time) ? 1 : (time.lessThan(o.time) ? -1 : 0);
    }
    
    @Override
    public String toString() {
        return Math.rint(time.toMillis()) + "-" + info;
    }
}
