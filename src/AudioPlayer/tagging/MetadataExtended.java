/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.tagging;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.util.Duration;
import utilities.FileUtil;
import utilities.Log;

/**
 * @author uranium
 */
public final class MetadataExtended {
    private final Metadata metadata;
    private final ArrayList<CommentExtended> comments;
    private final ArrayList<Chapter> chapters;
    
    /**
     * Constructor.
     * Initializes fields. Use getters/setters to work with this object.
     * @param meta metadata
     */
    public MetadataExtended(Metadata meta) {
        metadata = meta;
        comments = new ArrayList<>();
        chapters = new ArrayList<>();
    }
    /**
     * @return the comments
     */
    public List<CommentExtended> getComments() {
        return Collections.unmodifiableList(comments);
    }
    /**
     * @return the chapters
     */
    public List<Chapter> getChapters() {
        return Collections.unmodifiableList(chapters);
    }
    
    public void addChapter(Duration time, String info) {
        chapters.add(new Chapter(time, info));
        Collections.sort(chapters);
//        save();
    }   
    public void addComment( String key, String comment) {
        comments.add(new CommentExtended(key, comment));
        Collections.sort(comments);
//        save();
    }
    public void removeChapters(List<Chapter> to_remove) {
        chapters.removeAll(to_remove);
//        save();
    }
    public void removeComments(List<CommentExtended> to_remove) {
        comments.removeAll(to_remove);
//        save();
    }
    public void editChapter(Chapter chapter, Duration time, String text) {
        int index = getChapters().indexOf(chapter);
        Chapter ch = getChapters().get(index);
                ch.setTime(time);
                ch.setInfo(text);
//        save();
    }
    public void editComment(CommentExtended comment, String key, String text) {
        int index = getComments().indexOf(comment);
        CommentExtended ch = getComments().get(index);
                ch.setKey(key);
                ch.setValue(text);
//        save();
    }
    
    /**
     * Saves chapters.
     * @note: This involves persisting of data and as such IO operations are
     * involved.
     */
    private void save() {
        String content = "";
            content += "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                       "<Chapterlist version=\"1.0\">\n";
        for (CommentExtended c: comments) {
            content += "        <Info name=\"" + c.getKey() + "\" value=\"" + c.getValue() + "\"/>\n";
        }
        for (Chapter c: chapters) {
            content += "        <Chapter pos=\"" + String.valueOf(c.getTime().toMillis()) + "\" name=\"" + c.getInfo() + "\"/>\n";
        }
            content += "</Chapterlist>";

        FileUtil.writeFile(metadata.getFile().toString()+".xml", content);
        
        MetadataWriter writer = MetadataWriter.create(metadata);
        writer.setChapters(getChapters());
        writer.write();
    }
    
    /**
    * loads chapters.
     * @param meta
     * @return 
    */
    public static MetadataExtended readFromFile(Metadata meta) {
        // check validity and open file
        File f = new File(meta.getFile().toString()+".xml");
        MetadataExtended m = new MetadataExtended(meta);
        
        if (!f.exists()) { Log.mess("File " + f.toString() + " doesnt exist"); return m; }
        if (!f.canRead()) { Log.mess("File " + f.toString() + " cannot be read"); return m; }
        if (!f.canWrite()) { Log.mess("File " + f.toString() + " cannot be wrote into"); return m; }
        
        Xml xml = new Xml(f.toString(), "Chapterlist");
        for (Xml x: xml.children("Chapter")) { 
            m.chapters.add(new Chapter(Duration.millis(x.optDouble("pos")), x.optString("name")));
        }
        for (Xml x: xml.children("Info")) {
            m.comments.add(new CommentExtended(x.optString("name"), x.optString("value")));
        }
        Collections.sort(m.chapters);
        Collections.sort(m.comments);
            
        return m;
    }
}
