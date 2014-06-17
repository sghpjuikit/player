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

    public Chapter(Duration _time) {
        time = _time;
        info = "";
    }
    public Chapter(Duration _time, String _info) {
        time = _time;
        info = _info;
    }
    
    /**
     * @return the time
     */
    public Duration getTime() {
        return time;
    }

    /**
     * @param time the time to set
     */
    public void setTime(Duration time) {
        this.time = time;
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
     * Compares chapters.
     * @param o Comparing Object.
     * @return True only if their time is the same.
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Chapter)) { return false; }
        return getTime().toMillis() == ((Chapter) o).getTime().toMillis();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 19 * hash + Objects.hashCode(this.time);
        return hash;
    }
    
    /**
     * Compares chapter by natural order - numerically by time.
     * @param o
     * @return 
     */
    @Override
    public int compareTo(Chapter o) {
        return this.time.greaterThan(o.time) ? 1 : (time.lessThan(o.time) ? -1 : 0);
    }
    
    @Override
    public String toString() {
        return time.toString() + "\n" + info;
    }
}
