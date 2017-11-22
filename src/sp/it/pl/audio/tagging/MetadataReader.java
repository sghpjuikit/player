package sp.it.pl.audio.tagging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import javafx.concurrent.Task;
import javafx.scene.media.Media;
import sp.it.pl.audio.Item;
import sp.it.pl.audio.playlist.PlaylistItem;
import sp.it.pl.util.async.future.ConvertListTask;
import sp.it.pl.util.file.AudioFileFormat.Use;
import static java.util.stream.Collectors.toList;
import static sp.it.pl.audio.tagging.ExtKt.readAudioFile;
import static sp.it.pl.main.App.APP;
import static sp.it.pl.util.async.AsyncKt.runFX;
import static sp.it.pl.util.dev.Util.log;
import static sp.it.pl.util.dev.Util.noØ;
import static sp.it.pl.util.dev.Util.throwIfFxThread;
import static sp.it.pl.util.functional.Util.stream;

public class MetadataReader {

	// TODO: return Try

	/**
	 * Reads metadata for specified item.
	 * <p/>
	 * Involves I/O operation and blocks thread. Throws exception if executed on fx application thread.
	 *
	 * @param item item to read metadata for
	 * @return metadata for specified item or {@link Metadata#EMPTY} if any error. Never null.
	 */
	public static Metadata readMetadata(Item item) {
		throwIfFxThread();

		if (item.isCorrupt(Use.APP)) {
			return Metadata.EMPTY;
		}

		if (item.isFileBased()) {
			return readAudioFile(item.getFile())
				.map(Metadata::new)
				.getOr(Metadata.EMPTY);
		} else {
			// TODO: implement properly
			try {
				Media m = new Media(item.getUri().toString());
				//            m.getMetadata().forEach((String s, Object o) -> {
				//                System.out.println(s + " " + o);
				//            });

				// make a playlistItem and covert to metadata //why? // not 100%sure...
				// because PlaylistItem has advanced update() method? // probably
				return new PlaylistItem(item.getUri(), "", "", m.getDuration().toMillis()).toMeta();
			} catch (IllegalArgumentException|UnsupportedOperationException e) {
				log(MetadataReader.class).error("Error creating metadata for non file based item: {}", item);
				return item.toMeta();
			}
		}
	}

	/**
	 * Creates task that reads metadata for specified items.
	 *
	 * @param items list of items to read metadata for
	 * @param onEnd procedure to execute upon finishing this task providing the result and success flag. Must not be
	 * null.
	 * @return the task reading the files returning all successfully read metadata
	 * @throws NullPointerException if any parameter null
	 */
	public static Task<List<Metadata>> buildReadMetadataTask(Collection<? extends Item> items, BiConsumer<Boolean,List<Metadata>> onEnd) {
		noØ(items);
		noØ(onEnd);
		return new SuccessTask<List<Metadata>,SuccessTask>("Reading metadata", onEnd) {
			private final int all = items.size();
			private int completed = 0;
			private int skipped = 0;

			@Override
			protected List<Metadata> call() throws Exception {
				updateTitle("Reading metadata for items.");
				List<Metadata> metadatas = new ArrayList<>();

				for (Item item : items) {

					if (isCancelled()) {
						return metadatas;
					}

					Metadata m = readMetadata(item);
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

				// TODO: encapsulate & make threadsafe
				APP.db.getEm().getTransaction().begin();

				for (Item item : input) {
					if (isCancelled()) {
						log(MetadataReader.class).info("Metadata reading was canceled.");
						break;
					}

					Metadata m;
					try {
						m = APP.db.getEm().find(Metadata.class, Metadata.metadataID(item.getUri()));
						if (m==null) {
							MetadataWriter.useNoRefresh(item, MetadataWriter::setLibraryAddedNowIfEmpty);
							m = readMetadata(item);

							if (m.isEmpty()) {
								skipped.add(item);
							} else {
								APP.db.getEm().persist(m);
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

				APP.db.getEm().getTransaction().commit();
				runFX(APP.db::updateInMemoryDbFromPersisted);

				// update progress
				updateMessage(all.size(), processed.size());
				updateProgress(processed.size(), all.size());
				updateSkipped(skipped.size());

				return new Result<>(all, processed, converted, skipped);
			}
		};
	}

	/**
	 * @return a task that removes from library all items, which refer to non-existent files
	 */
	public static Task<Void> buildRemoveMissingFromLibTask() {
		return new Task<>() {
			private final StringBuffer sb = new StringBuffer(40);
			private int all = 0;
			private int completed = 0;
			private int removed = 0;

			{
				updateTitle("Removing missing items from library");
			}

			@Override
			protected Void call() throws Exception {
				List<Metadata> libraryItems = stream(APP.db.getAllItems()).filter(m -> !m.isEmpty() && m.isFileBased()).collect(toList());
				all = libraryItems.size();

				// TODO: encapsulate & make threadsafe
				APP.db.getEm().getTransaction().begin();

				for (Metadata m : libraryItems) {
					if (isCancelled()) break;

					completed++;

					if (m.isFileBased() && !m.getFile().exists()) {
						APP.db.getEm().remove(m);
						removed++;
					}

					// update state
					updateMessage(all, completed, removed);
					updateProgress(completed, all);
				}

				APP.db.getEm().getTransaction().commit();
				runFX(APP.db::updateInMemoryDbFromPersisted);

				// update state
				updateMessage(all, completed, removed);
				updateProgress(completed, all);

				return null;
			}

			private void updateMessage(int all, int done, int removed) {
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
	}
}