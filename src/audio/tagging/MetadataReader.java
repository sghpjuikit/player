package audio.tagging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.media.Media;

import org.jaudiotagger.audio.AudioFile;

import audio.Item;
import audio.playlist.PlaylistItem;
import services.database.Db;
import util.async.future.ConvertListTask;
import util.file.AudioFileFormat.Use;

import static services.database.Db.em;
import static util.async.Async.runFX;
import static util.async.Async.runNew;
import static util.dev.Util.log;
import static util.dev.Util.noØ;

/**
 * This class plays the role of static factory for Metadata. It can read files
 * and construct Metadata items. The class makes use of concurrency.
 *
 * ------------- I/O OPERATION ---------------
 * Everything is read once at object creation time. There is no additional READ
 * operation after Metadata has been created ( with the exception of some of
 * the non-tag (possibly external) information - cover, etc)
 *
 * @author Martin Polakovic
 */
public class MetadataReader{

    private static Task<List<Metadata>> buildReadMetadata(Collection<? extends Item> items, BiConsumer<Boolean, List<Metadata>> onEnd){
	    noØ(items);
	    noØ(onEnd);
        return new SuccessTask<List<Metadata>,SuccessTask>("Reading metadata", onEnd){
            private final int all = items.size();
            private int completed = 0;
            private int skipped = 0;

            @Override
            protected List<Metadata> call() throws Exception {
                updateTitle("Reading metadata for items.");
                List<Metadata> metadatas = new ArrayList<>();

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
    }

    /**
     * Creates list of Metadata for provided items. Use to read multiple files
     * at once. The work runs on background thread. The procedures executed on
     * task completion will be automatically executed from FXApplication thread.
     * <p/>
     * This method returns {@link Task} doing the work, which allows binding to
     * its properties (for example progress) and more.
     * <p/>
     * When any error occurs during the reading process, the reading will stop
     * and return all obtained metadata.
     * <p/>
     * The result of the task is list of metadatas (The list will not be null
     * nor contain null values) if task finshes sccessfully or null otherwise.
     * <p/>
     * Calling this method will immediately start the reading process (on
     * another thread).
     *
     * @param items List of items to read.
     * @param onEnd procedure to execute upon finishing this task providig
     * the result and success flag.
     * Must not be null.
     * @return task reading the files returning item's metadatas on successful
     * completion or all successfully obtained metadata when any error occurs.
     *
     * @throws NullPointerException if any parameter null
     */
    public static Task<List<Metadata>> readMetadata(Collection<? extends Item> items, BiConsumer<Boolean, List<Metadata>> onEnd){
        // create task
        final Task task = buildReadMetadata(items, onEnd);

        // run immediately and return task
        runNew(task);
        return task;
    }

    /**
     * Transforms items into their metadatas by reading the files.
     * For asynchronouse use only.
     * Items for which reading fails are ignored.
     */
    public static List<Metadata> readMetadata(Collection<? extends Item> items){
        List<Metadata> metadatas = new ArrayList<>();

        for (Item item: items){
            // create metadata
            Metadata m = create(item);
            if (!m.isEmpty()) metadatas.add(m);
        }

        return metadatas;
    }

    /**
     * Reads {@link Metadata} for specified item.
     * When error occurs during reading {@link Metadata#EMPTY} will be
     * returned.
     * <p/>
     * Incurs costly I/O.
     * Avoid using this method in loops or in chains on main application thread.
     *
     * @param item
     * @return metadata for specified item or {@link Metadata#EMPTY} if error.
     * Never null.
     */
    public static Metadata create(Item item){
//        // handle corrupt item
        if (item.isCorrupt(Use.APP)){
            return Metadata.EMPTY;        // is this good way to handle corrupt item? //no.
        }
        // handle items with no file representation
        if (!item.isFileBased()){
            return createNonFileBased(item);
        }
        // handle normal item
        else {
            afile = MetaItem.readAudioFile(item.getFile());
            return (afile == null) ? item.toMeta() : new Metadata(afile);
        }
    }

    private static AudioFile afile;

    /**
     * Reads {@link Metadata} for specified item. Runs on background thread.
     * Calling this method will immediately start the execution. The procedures
     * executed on task completion will always be executed from FXApplication
     * thread.
     * <p/>
     * This method returns {@link Task} doing the work, which allows binding to
     * its properties (for example progress) and more.
     * <p/>
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
    public static Task<Metadata> create(Item item, BiConsumer<Boolean, Metadata> onFinish){
	    noØ(item);
	    noØ(onFinish);

        Task<Metadata> task = new Task<>(){
            @Override protected Metadata call() throws Exception {
                return create(item);
            }
        };
        task.setOnSucceeded( e ->
            Platform.runLater(() -> {
                try {
                    onFinish.accept(true, task.get());
                } catch (InterruptedException | ExecutionException ex){
                    log(MetadataReader.class).error("Reading metadata failed for: {}",item);
                    onFinish.accept(false, null);
                }
            })
        );
        task.setOnFailed( e ->
            Platform.runLater(() -> {
                log(MetadataReader.class).error("Reading metadata failed for: {}", item);
                onFinish.accept(false, null);
            })
        );

        // run immediately and return task
        runNew(task);
        return task;
    }

    static private Metadata createNonFileBased(Item item){
        try {
            Media m = new Media(item.getURI().toString());
//            m.getMetadata().forEach((String s, Object o) -> {
//                System.out.println(s + " " + o);
//            });

            // make a playlistItem and covert to metadata //why? // not 100%sure...
            // because PlaylistItem has advanced update() method? // probably
            return new PlaylistItem(item.getURI(), "", "", m.getDuration().toMillis()).toMeta();
        } catch (IllegalArgumentException | UnsupportedOperationException e){
            log(MetadataReader.class).error("Error creating metadata for non file based item: {}",item);
            return item.toMeta();
        }
    }


    /**
     * Creates a task that:
     * <ul>
     * <li> Reads metadata from files of the items.
     * <li> Adds items to library. If library already contains the item, it will not be added.
     * <li> Returns detailed information about the end result
     * </ul>
     *
     * @return the task
     */
    public static ConvertListTask<Item,Metadata> buildAddItemsToLibTask() {
	    return new ConvertListTask<>("Adding items to library") {
            @Override
            protected Result<Item,Metadata> compute(Collection<? extends Item> input) {
	            List<Item> all = new ArrayList<>(input);
                List<Item> processed = new ArrayList<>(all.size());
                List<Metadata> converted = new ArrayList<>(all.size());
                List<Item> skipped = new ArrayList<>(0);

	            em.getTransaction().begin();

                for (Item item : input) {
                    if (isCancelled()) {
                        log(MetadataReader.class).info("Metadata reading was canceled.");
                        break;
                    }

	                Metadata m;
                    try {
                        m = em.find(Metadata.class, Metadata.metadataID(item.getURI()));
                        if (m == null) {
                            MetadataWriter.useNoRefresh(item, MetadataWriter::setLibraryAddedNowIfEmpty);
                            m = create(item);

                            if (m.isEmpty()) {
	                            skipped.add(item);
                            } else {
                                em.persist(m);
                                converted.add(m);
                            }
                        } else {
		                    skipped.add(item);
                        }
                    } catch (Exception e) {
                        log(MetadataReader.class).warn("Problem during reading tag of {}", item);
                    }

                    processed.add(item);

                    // update progress
                    updateMessage(all.size(), processed.size());
                    updateProgress(processed.size(), all.size());
	                updateSkipped(skipped.size());
                }

                em.getTransaction().commit();

	            // update library model
                runFX(Db::updateInMemoryDBfromPersisted);

                // update progress
	            updateMessage(all.size(), processed.size());
	            updateProgress(processed.size(), all.size());
	            updateSkipped(skipped.size());

                return new Result<>(all, processed, converted, skipped);
            }
        };
    }

    public static Task<Void> removeMissingFromLibrary(BiConsumer<Boolean,Void> onEnd){
        // create task
        final Task<Void> task = new SuccessTask<>("Removing missing items from library",onEnd){
            private int all = 0;
            private int completed = 0;
            private int removed = 0;

            @Override
            protected Void call() throws Exception {                    //long timeStart = System.currentTimeMillis();
                List<Metadata> library_items = Db.getAllItems();
                all = library_items.size();
                em.getTransaction().begin();

                for (Metadata m : library_items){
                    completed++;
                    if (isCancelled()) break;

                    if (!m.getFile().exists()) {
                        em.remove(m);
                        removed++;
                    }
                    updateMessage(all,completed,removed);
                    updateProgress(completed, all);
                }

                em.getTransaction().commit();
                // update library model
                runFX(Db::updateInMemoryDBfromPersisted);

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

        // run immediately and return task
        runNew(task);
        return task;
    }
}