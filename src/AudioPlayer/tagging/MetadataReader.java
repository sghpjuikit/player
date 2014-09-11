
package AudioPlayer.tagging;

import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.services.Database.DB;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.media.Media;
import javax.persistence.EntityManager;
import org.jaudiotagger.audio.AudioFile;
import utilities.Log;
import utilities.functional.functor.BiProcedure;

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
public class MetadataReader{

    /**
     * Creates list of Metadata for provided items. Use to read multiple files
     * at once. The work runs on background thread. The procedures executed on
     * task completion will be automatically executed from FXApplication thread.
     * <p>
     * This method returns {@link Task} doing the work, which allows binding to
     * its properties (for example progress) and more.
     * <p>
     * When any error occurs during the reading process, the reading will stop
     * and return all obtained metadata.
     * <p>
     * The result of the task is list of metadatas (The list will not be null 
     * nor contain null values) if task finshes sccessfully or null otherwise.
     * <p>
     * Calling this method will immediately start the reading process (on
     * another thread).
     * <p>
     * @param items List of items to read.
     * @param onEnd procedure to execute upon finishing this task providig
     * the result and success flag.
     * Must not be null.
     * @return task reading the files returning item's metadatas on successful
     * completion or all successfully obtained metadata when any error occurs.
     * @throws NullPointerException if any parameter null
     */
    public static Task<List<Metadata>> readMetadata(List<? extends Item> items, BiProcedure<Boolean, List<Metadata>> onEnd){
        // perform check
        Objects.requireNonNull(items);
        Objects.requireNonNull(onEnd);
                
        // create task
        final Task<List<Metadata>> task = new SuccessTask<List<Metadata>>("Reading metadata", onEnd){
            private final int all = items.size();
            private int completed = 0;
            private int skipped = 0;
            
            @Override 
            protected List<Metadata> call() throws Exception {
                updateTitle("Reading metadata for items.");
                List<Metadata> metadatas = new ArrayList();
                
                for (Item item: items){
                    
                    if (isCancelled()){
                        return metadatas;
                    }
                    
                    // create metadata
                    Metadata m = create(item);
                    // on fail
                    if (m.isEmpty()) skipped++;
                    // on success
                    else metadatas.add(m);
                    
                    // update state
                    completed++;
                    updateMessage("Completed " + completed + " out of " + all + ". " + skipped + " skipped.");
                    updateProgress(completed, all);
                }
                
                return metadatas;
            }
        };
        
        return executeTask(task);
    }

    /**
     * Reads {@link Metadata} for specified item.
     * When error occurs during reading {@link Metadata#EMPTY()} will be
     * returned.
     * <p>
     * Runs on main application thread. Avoid using this method in loops or in
     * chains.
     *
     * @param item
     * @return metadata for specified item or {@link Metadata#EMPTY()} if error.
     * Never null.
     */
    public static Metadata create(Item item){
//        // handle corrupt item
        if (item.isCorrupt()){
            return Metadata.EMPTY;        // is this good way to handle corrupt item? //no.
        }
        // handle items with no file representation
        if (!item.isFileBased()){
            return createNonFileBased(item);
        }
        // handle normal item
        else {
            afile = MetaItem.readAudioFile(item.getFile());
            return (afile == null) ? item.toMetadata() : new Metadata(afile);
        }
    }
    
    private static AudioFile afile;

    /**
     * Reads {@link Metadata} for specified item. Runs on background thread.
     * Calling this method will immediately start the execution. The procedures
     * executed on task completion will always be executed from FXApplication
     * thread.
     * <p>
     * This method returns {@link Task} doing the work, which allows binding to
     * its properties (for example progress) and more.
     * <p>
     * The result of the task is nonempty Metadata if task finshes successfully
     * or null otherwise.
     * 
     * @param item item to read metadata for. Must not be null.
     * @param onFinish procedure to execute upon finishing this task providig
     * the result and success flag.
     * Must not be null.
     * @return task reading the file returning its metadata on successful task
     * completion or nothing when any error occurs. Never null.
     * @throws NullPointerException if any parameter null
     */
    public static Task<Metadata> create(Item item, BiProcedure<Boolean, Metadata> onFinish){
        Objects.requireNonNull(item);
        Objects.requireNonNull(onFinish);

        Task<Metadata> task = new Task(){
            @Override protected Metadata call() throws Exception {
                return create(item);
            }
        };
        task.setOnSucceeded( e -> {
            Platform.runLater(() -> {
                try {
                    onFinish.accept(true, task.get());
                } catch (InterruptedException | ExecutionException ex){
                    Log.err("Reading metadata failed for : " + item.getURI() + ".");
                    onFinish.accept(false, null);
                }
            });
        });
        task.setOnFailed( e -> {
            Platform.runLater(() -> {
                Log.err("Reading metadata failed for : " + item.getURI() + ".");
                onFinish.accept(false, null);
            });
        });

        return executeTask(task);
    }

    static private Metadata createNonFileBased(Item item){
        try {
            Media m = new Media(item.getURI().toString());
//            m.getMetadata().forEach((String s, Object o) -> {
//                System.out.println(s + " " + o);
//            });
            
//            String name = m.getSource();  // this simply returns the URI which we already have
            String name = item.getInitialName();    // for now initial name is the best we can do
            double time = m.getDuration().toMillis();   //System.out.println("time "+time);
            // make a playlistItem and covert to metadata
            return new PlaylistItem(item.getURI(), name, time).toMetadata();
        } catch (IllegalArgumentException | UnsupportedOperationException e){
            Log.err("Error during creating metadata for non file based item: " + item);
            return item.toMetadata();
        }
    }

    
    /**
     * Reads metadata from files of the items and adds items to library. If item
     * already exists, it will not be overwritten or changed.
     * <p>
     * The task returns list of all provided items that are in the database after
     * the task succeeds.
     * 
     * @param items
     * @param onEnd
     * @return 
     */
    public static Task<List<Metadata>> readAaddMetadata(List<? extends Item> items, BiConsumer<Boolean,List<Metadata>> onEnd){
        // perform check
        Objects.requireNonNull(items);
                
        // create task
        final Task<List<Metadata>> task = new SuccessTask("Adding items to library.", onEnd){
            private final int all = items.size();
            private int completed = 0;
            private int skipped = 0;
            
            @Override 
            protected List<Metadata> call() throws Exception {
                List<Metadata> out = new ArrayList();
                Metadata m;
                
                EntityManager em = DB.em;
                              em.getTransaction().begin();
                try {
                    for (Item i : items){
                        completed++;
                        if (isCancelled()) break;

                        // create metadata
                        m = create(i);
                        // on fail
                        if (m.isEmpty()) {
                            skipped++;
                        // on success
                        } else {
                            Metadata l = em.find(Metadata.class, m.getId());
                            if(l == null) em.persist(m);
                            else m = l;
                            
                            out.add(m);
                        }
                        
                        // update
                        updateMessage(all,completed,skipped);
                        updateProgress(completed, all);
                    }
                    em.getTransaction().commit();
                    // emit library change to signal refresh
                    // we need to run the event on apFX thread
                    Platform.runLater(() -> DB.librarychange.push(null));
                } catch (Exception e ) {
                    e.printStackTrace();
                }
                        
                // update state
                updateMessage(all,completed,skipped);
                updateProgress(completed, all);
                return out;
            }
        };
        
        return executeTask(task);
    }
    
    public static Task<Void> removeMissingFromLibrary(BiConsumer<Boolean,Void> onEnd){                
        // create task
        final Task<Void> task = new SuccessTask("Removing missing items from library",onEnd){
            private int all = 0;
            private int completed = 0;
            private int removed = 0;
            
            @Override 
            protected Void call() throws Exception {                    //long timeStart = System.currentTimeMillis();
                EntityManager em = DB.em;
                              em.getTransaction().begin();
                List<Metadata> library_items = DB.getAllItems();
                all = library_items.size();

                for (Metadata m : library_items){
                    completed++;
                    if (isCancelled()) break;

                    if(!m.getFile().exists()) {
                        em.remove(m);
                        removed++;
                    }
                    updateMessage(all,completed,removed);
                    updateProgress(completed, all);
                }
                
                em.getTransaction().commit();
                
                Platform.runLater(() -> DB.librarychange.push(null));
                        
                // update state
                updateMessage(all,completed,removed);
                updateProgress(completed, all);                     //System.out.println((System.currentTimeMillis()-timeStart));
                
                return null;
            }
            
            @Override
            protected void updateMessage(int all, int done, int removed) {
                sb.setLength(0);
                sb.append("Completed ");
                sb.append(all);
                sb.append(" / ");
                sb.append(done);
                sb.append(". ");
                sb.append(removed);
                sb.append(" removed.");
                updateMessage(sb.toString());
            }
            
        };
        
        return executeTask(task);
    }
    
    private static<T> Task<T> executeTask(Task<T> task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        return task;
    }
    
}
