/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package AudioPlayer.tagging;

import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.PlaylistItem;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.scene.media.Media;
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
     * completion will be automatically executed from FXApplication thread.
     * <p>
     * This method returns {@link Task} doing the work, which allows binding to
     * its properties (for example progress) and more.
     * <p>
     * When any error occurs during the reading process, the reading will stop
     * and return all obtained metadata.
     * <p>
     * The result of the task is list of metadatas. The list will not be null
     * nor contain null.
     * <p>
     * Calling this method will immediately start the reading process (on another
     * thread).
     * <p>
     * @param items List of items to read.
     * @param onSuccess procedure to execute when task finishes successfully. 
     * Must not be null.
     * @param onError procedure to execute when task finishes with any error. 
     * Must not be null.
     * @return task reading the files returning item's metadatas on successful
     * completion or all successfully obtained metadata when any error occurs.
     * @throws NullPointerException if any parameter null
     */
    public static Task<List<Metadata>> readMetadata(List<? extends Item> items, 
                    UnProcedure<List<Metadata>> onSuccess, Procedure onError) {
        // perform check
        Objects.requireNonNull(items);
        Objects.requireNonNull(onSuccess);
        Objects.requireNonNull(onError);
        // filter out unreadable items
        items.removeIf(Item::isCorrupt);
        // create task
        final Task<List<Metadata>> task = new Task<List<Metadata>>() {
            @Override
            protected List<Metadata> call() throws Exception {
                List<Metadata> metadatas = new ArrayList();
                for (Item item : items) {
                    if (isCancelled()) {
                        updateMessage("Cancelled");
                        return metadatas;
                    }
                    
                    Metadata m = create(item);
                    if (!m.isEmpty()) metadatas.add(create(item));
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
        // execute
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        return task;
    }

    /**
     * Reads {@link Metadata} for specified item.
     * When error occurs during reading {@link Metadata#EMPTY()} will be returned.
     * <p>
     * Runs on main application thread. Avoid using this method in loops or in
     * chains.
     * @param item
     * @return metadata for specified item or {@link Metadata#EMPTY()} if error.
     * Never null.
     */
    public static Metadata create(Item item) {
        // handle corrupt item
        if (item.isCorrupt()) return Metadata.EMPTY();
        // handle items with no file representation
        if(!item.isFileBased()) return createNonFileBased(item.getURI());
        // handle normal item
        else {
            AudioFile afile = MetaItem.readAudioFile(item.getFile());
            return (afile == null) ? Metadata.EMPTY() : new Metadata(afile);
        }
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
     * completion or nothing when any error occurs. Never null.
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
                    Log.err("Reading metadata failed for : " + item.getURI() + ".");
                    onError.run();
                }
            });
        });
        task.setOnFailed((WorkerStateEvent e) -> {
            Platform.runLater(() -> {
                Log.err("Reading metadata failed for : " + item.getURI() + ".");
                onError.run();
            });
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        return task;
    }
    
    
    static private Metadata createNonFileBased(URI uri) {
        try {
            Media m = new Media(uri.toString());                                System.out.println("DEBUG");m.getMetadata().forEach((String s, Object o) -> System.out.println(s + " " + o));
            String name = m.getSource();
            double time = m.getDuration().toMillis();
            return new PlaylistItem(uri, name, time).toMetadata();
        } catch (IllegalArgumentException | NullPointerException | UnsupportedOperationException e) {
                e.printStackTrace();
            return null;
        }
    }
    
}
