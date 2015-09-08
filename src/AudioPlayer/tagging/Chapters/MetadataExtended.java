/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.tagging.Chapters;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.util.Duration;

import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataWriter;
import AudioPlayer.tagging.Xml;
import util.File.FileUtil;

import static util.dev.Util.log;

/**
 * @author uranium
 */
public final class MetadataExtended {
    private final Metadata metadata;
    private final ArrayList<CommentExtended> comments = new ArrayList();
    private final ArrayList<Chapter> chapters = new ArrayList();


    public MetadataExtended(Metadata metadata) {
        this.metadata = metadata;
    }
    /**
     * @return the comments
     */
    public List<CommentExtended> getComments() {
        return new ArrayList(comments);
    }
    /**
     * @return the chapters
     */
    public List<Chapter> getChapters() {
        return new ArrayList(chapters);
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
                ch.setText(text);
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
            content += "        <Chapter pos=\"" + String.valueOf(c.getTime().toMillis()) + "\" name=\"" + c.getText() + "\"/>\n";
        }
            content += "</Chapterlist>";

        File f = new File(metadata.getLocation(),metadata.getFilenameFull() + ".xml");
        FileUtil.writeFile(f.getPath(), content);

        MetadataWriter.use(metadata, w->w.setChapters(getChapters()));
    }

    /**
     * Loads chapters from a file.
     * @param meta
     * @return
     */
    public void readFromFile() {
        // check validity and open file
        if(!metadata.isFileBased()) return;

        File f = new File(metadata.getLocation(),metadata.getFilenameFull() + ".xml");

        if (f.exists()) {
            if (!f.canRead()) {
                log(this).warn("File {} not be readable", f);
            } else {
                Xml xml = new Xml(f.toString(), "Chapterlist");
                for (Xml x: xml.children("Chapter")) {
                    this.chapters.add(new Chapter(Duration.millis(x.optDouble("pos")), x.optString("name")));
                }
                for (Xml x: xml.children("Info")) {
                    this.comments.add(new CommentExtended(x.optString("name"), x.optString("value")));
                }
                Collections.sort(this.chapters);
                Collections.sort(this.comments);
            }
        }
    }
}
