/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package AudioPlayer.tagging;

import AudioPlayer.playlist.Item;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import org.jaudiotagger.audio.AudioFile;
import utilities.Log;
import utilities.functional.functor.OnEnd;

/**
 * This class plays the role of static factory for Metadata. It can read files
 * and construct Metadata items. The class makes use of concurrency.
 * 
 * ------------- I/O OPERATION ---------------
 * Everything is read once at object creation time. There is no additional READ
 * operation after Metadata has been created ( with the exception of some of
 * the non-tag (possibly external) information - cover, etc)
 * 
 * @author Plutonium_
 */
public class MetadataReader {

    /**
     * Creates list of Metadata for provided items. Use to read multiple files at
     * once. The work runs on different thread to minimize I/O performance impact.
     *
     * To use this method, object implementing TaskRunner interface must be used.
     * Using different threads builds up unnecessary code and TaskRunner makes it
     * easier. Task Runner object implements two methods that will be automatically
     * called when the reading metadata on the other thread is done - successfully
     * respectively unsuccessfully. On success List<Metadata> will be returned.
     *
     * This method returns Task doing the work, thus allowing for binding to its
     * properties like progressProperty() and giving access to everything Task related.
     *
     * Calling this method will immediately start the reading process (on another
     * thread).
     *
     * @param items - List of PlaylistItem objects to read.
     * @param runner - takes care of executing code when reading finishes
     * @return Task<List<Metadata>> - Task to provide access to the work.
     */
    public static Task readMetadata(List<? extends Item> items, OnEnd<List<Metadata>> runner) {
        items.removeIf(Item::isCorrupt);
        final Task<List<Metadata>> task = new Task<List<Metadata>>() {
            @Override
            protected List<Metadata> call() throws Exception {
                List<Metadata> metadatas = new ArrayList<>();
                for (Item item : items) {
                    if (isCancelled()) {
                        updateMessage("Cancelled");
                        return new ArrayList<>();
                    }
                    AudioFile afile = MetaItem.readAudioFile(item.getFile());
                    if (afile != null) {
                        metadatas.add(new Metadata(afile));
                    }
                }
                return metadatas;
            }
        };
        task.setOnFailed((WorkerStateEvent e) -> {
            runner.failure();
            Log.err("Reading metadata failed for items.");
        });
        task.setOnSucceeded((WorkerStateEvent e) -> {
            try {
                runner.success(task.get());
            } catch (InterruptedException | ExecutionException ex) {            // ex.printStackTrace();
                runner.failure();
                Log.err("Reading metadata failed for items due to interrupted execution.");
            }
        });
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        return task;
    }

    /**
     * Reads metadata for specified item. 
     * Warning: runs on application thread!. Avoid using this method in loops in
     * chains.
     * @param item
     * @return metadata for specified item
     */
    public static Metadata create(Item item) {
        if (item.isCorrupt()) {
            return null;
        }
        AudioFile afile = MetaItem.readAudioFile(item.getFile());
        return (afile == null) ? null : new Metadata(afile);
    }

    /**
     * Reads Metadata for specified item. Runs on bgr thread. Calling this method
     * will immediately start the work.
     * @param item
     * @param runner - takes care of executing code when reading finishes
     * @return task reading the metadata returning the metadata on success or
     * null on error
     */
    public static Task create(Item item, OnEnd<Metadata> runner) {
        Task<Metadata> task = new Task<Metadata>() {
            @Override
            protected Metadata call() throws Exception {
                return create(item);
            }
        };
        task.setOnFailed((WorkerStateEvent e) -> {
            Log.err("Reading metadata failed for file " + item.getPath() + ".");
            runner.failure();
        });
        task.setOnSucceeded((WorkerStateEvent e) -> {
            try {
                runner.success(task.get());
            } catch (InterruptedException | ExecutionException ex) {
                Log.err("Reading metadata failed for file " + item.getPath() + ".");
                runner.failure();
            }
        });
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        return task;
    }
    
}
