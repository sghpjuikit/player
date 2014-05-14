/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package AudioPlayer.tagging;

import AudioPlayer.playlist.Item;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import org.jaudiotagger.audio.AudioFile;
import utilities.Log;
import utilities.functional.functor.Procedure;
import utilities.functional.functor.UnProcedure;

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
     * once. The work runs on background thread. The procedures executed on task
     * completion will always be executed from FXApplication 
     * thread.
     * <p>
     * This method returns {@link Task} doing the work, which allows binding to
     * its properties (for example progress) and more.
     * <p>
     * Calling this method will immediately start the reading process (on another
     * thread).
     * <p>
     * @param items List of items to read.
     * @param onSuccess procedure to execute when task finishes successfully. 
     * Must not be null.
     * @param onError procedure to execute when task finishes successfully. 
     * Must not be null.
     * @return task reading the files returning item's metadatas on successful
     * completion or nothing when any error occurs.
     * @throws NullPointerException if any parameter null
     */
    public static Task<List<Metadata>> readMetadata(List<? extends Item> items, 
            UnProcedure<List<Metadata>> onSuccess, Procedure onError) {
        Objects.requireNonNull(items);
        Objects.requireNonNull(onSuccess);
        Objects.requireNonNull(onError);
        
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
            Platform.runLater( () -> {
                onError.run();
                Log.err("Reading metadata failed for items.");
            });
        });
        task.setOnSucceeded((WorkerStateEvent e) -> {
            Platform.runLater( () -> {
                try {
                    onSuccess.accept(task.get());
                } catch (InterruptedException | ExecutionException ex) {
                    onError.run();
                    Log.err("Reading metadata failed for items due to interrupted execution.");
                }
            });
        });
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        return task;
    }

    /**
     * Reads {@link Metadata} for specified item.
     * When error occurs during reading empty metadata will be returned.
     * <p>
     * Runs on main application thread!. Avoid using this method in loops or in
     * chains.
     * @param item
     * @return metadata for specified item
     */
    public static Metadata create(Item item) {
        if (item.isCorrupt()) 
            return Metadata.EMPTY();
        
        AudioFile afile = MetaItem.readAudioFile(item.getFile());
        return (afile == null) ? null : new Metadata(afile);
    }

    /**
     * Reads {@link Metadata} for specified item. Runs on background thread. 
     * Calling this method will immediately start the execution. The procedures
     * executed on task completion will always be executed from FXApplication 
     * thread.
     * <p>
     * This method returns {@link Task} doing the work, which allows binding to
     * its properties (for example progress) and more.
     * @param item item to read metadata for. Must not be null.
     * @param onSuccess procedure to execute when task finishes successfully. 
     * Must not be null.
     * @param onError procedure to execute when task finishes successfully. 
     * Must not be null.
     * @return task reading the file returning its metadata on successful task
     * completion or nothing when any error occurs.
     * @throws NullPointerException if any parameter null
     */
    public static Task<Metadata> create(Item item, UnProcedure<Metadata> onSuccess, Procedure onError) {
        Objects.requireNonNull(item);
        Objects.requireNonNull(onSuccess);
        Objects.requireNonNull(onError);
        
        Task<Metadata> task = new Task<Metadata>() {
            @Override protected Metadata call() throws Exception {
                if(item.isCorrupt()) 
                    throw new RuntimeException("Metadata failed. Item is corrupt.");
                return create(item);
            }
        };
        task.setOnSucceeded((WorkerStateEvent e) -> {
            Platform.runLater(() -> {
                try {
                    onSuccess.accept(task.get());
                } catch (InterruptedException | ExecutionException ex) {
                    Log.err("Reading metadata failed for file " + item.getPath() + ".");
                    onError.run();
                }
            });
        });
        task.setOnFailed((WorkerStateEvent e) -> {
            Platform.runLater(() -> {
                Log.err("Reading metadata failed for file " + item.getPath() + ".");
                onError.run();
            });
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        return task;
    }
    
}
