
package AudioPlayer.tagging;

import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.services.Database.DB;
import static AudioPlayer.services.Database.DB.emf;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
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
     * @param onFinish procedure to execute upon finishing this task providig
     * the result and success flag.
     * Must not be null.
     * @return task reading the files returning item's metadatas on successful
     * completion or all successfully obtained metadata when any error occurs.
     * @throws NullPointerException if any parameter null
     */
    public static Task<List<Metadata>> readMetadata(List<? extends Item> items, BiProcedure<Boolean, List<Metadata>> onFinish){
        // perform check
        Objects.requireNonNull(items);
        Objects.requireNonNull(onFinish);
                
        // create task
        final Task<List<Metadata>> task = new Task<List<Metadata>>(){
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
            @Override protected void succeeded() {
                super.succeeded();
                updateMessage("Reading metadata finished succeeded!");
                onFinish.accept(true, getValue());
            }

            @Override protected void cancelled() {
                super.cancelled();
                updateMessage("Reading metadata cancelled!");
                onFinish.accept(false, null);
            }

            @Override protected void failed() {
                super.failed();
                updateMessage("Reading metadata failed!");
                onFinish.accept(false, null);
            }

            @Override
            protected void updateMessage(String message) {
                super.updateMessage(message);
                System.out.println(message);
            }

            @Override
            protected void updateProgress(long workDone, long max) {
                super.updateProgress(workDone, max);
                System.out.println("Completed " + workDone + "/" + max + ".");
            }
            
        };
        // execute
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        return task;
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
            return (afile == null) ? Metadata.EMPTY : new Metadata(afile);
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

        Task<Metadata> task = new Task<Metadata>(){
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

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        return task;
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

    
    
    public static Task<Void> readAaddMetadata(List<? extends Item> items){
        // perform check
        Objects.requireNonNull(items);
                
        // create task
        final Task<Void> task = new Task<Void>(){
            private final int all = items.size();
            private int completed = 0;
            private int skipped = 0;
            @Override 
            protected Void call() throws Exception {
//                updateTitle("Reading metadata and adding items to library.");
//                
//                Metadata m;
//                for (int i=0; i<items.size(); i++){
//                    
//                    if (isCancelled()) break;
//                    
//                    // create metadata
//                    m = create(items.get(i));
//                    // on fail
//                    if (m.isEmpty()) skipped++;
//                    // on success
//                    else {
////                        DB.addItems(Collections.singletonList(m));
//                        System.out.println("Completed " + completed + " out of " + all + ". " + skipped + " skipped.");
//                    }
//                    // update state
//                    completed++;
//                    updateMessage("Completed " + completed + " out of " + all + ". " + skipped + " skipped.");
//                    updateProgress(completed, all);
//                }
//                return null;
                
                
                updateTitle("Reading metadata and adding items to library.");
                Metadata m;
                
                EntityManager em = emf.createEntityManager();
                              em.getTransaction().begin();
                try {
                    for (Item i : items){
                        completed++;
                        if (isCancelled()) break;

                        // create metadata
                        m = create(i);
                        // on fail
                        if (m.isEmpty()) skipped++;
                        // on success
                        else {
                            if(em.find(Metadata.class, m.getId()) == null)
                                em.persist(m);
                            updateMessage("Completed " + completed + " out of " + all + ". " + skipped + " skipped.");
                            updateProgress(completed, all);
                        }
                    }
                    em.getTransaction().commit();
                    Platform.runLater(() -> DB.librarychange.push(null));
                } finally {
                    em.close();
                }
                        
                // update state
                updateMessage("Completed " + completed + " out of " + all + ". " + skipped + " skipped.");
                updateProgress(completed, all);
                
//                DB.addItems(Arrays.asList(ms));
                
//                updateTitle("Reading metadata and adding items to library.");
//                List<Metadata> metadatas = new ArrayList(100);
//                Metadata[] ms = new Metadata[2];
//                
//                for (int i=0; i<items.size(); i++){
//                    int j= i%2;
//                    if (isCancelled()) break;
//                    
//                    // create metadata
//                    Metadata m = create(items.get(i));
//                    // on fail
//                    if (m.isEmpty()) skipped++;
//                    // on success
//                    else ms[j] = m;
//                    
//                    // batch
//                    if(j==1) {
//                        DB.addItems(Arrays.asList(ms));
////                        metadatas.clear();
//                        System.out.println("Completed " + completed + " out of " + all + ". " + skipped + " skipped.");
//                    }
//                    
//                    // update state
//                    completed++;
//                    updateMessage("Completed " + completed + " out of " + all + ". " + skipped + " skipped.");
//                    updateProgress(completed, all);
//                }
//                DB.addItems(Arrays.asList(ms));
                return null;
            }
            @Override protected void succeeded() {
                super.succeeded();
                updateMessage("Reading metadata finished succeeded!");
            }

            @Override protected void cancelled() {
                super.cancelled();
                updateMessage("Reading metadata cancelled!");
            }

            @Override protected void failed() {
                super.failed();
                updateMessage("Reading metadata failed!");
            }

            @Override
            protected void updateMessage(String message) {
                super.updateMessage(message);
                System.out.println(message);
            }

            @Override
            protected void updateProgress(long workDone, long max) {
                super.updateProgress(workDone, max);
                System.out.println("Completed " + workDone + "/" + max + ".");
            }
            
        };
        // execute
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        return task;
    }
    
}
