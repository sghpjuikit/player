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
import static sp.it.pl.main.AppKt.APP;
import static sp.it.pl.util.dev.DebugKt.logger;
import static sp.it.pl.util.dev.FailKt.noNull;
import static sp.it.pl.util.dev.FailKt.failIfFxThread;

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
		failIfFxThread();

		if (item.isCorrupt(Use.APP)) {
			return Metadata.EMPTY;
		}

		if (item.isFileBased()) {
			return readAudioFile(noNull(item.getFile()))
				.map(Metadata::new)
				.getOr(Metadata.EMPTY);
		} else {
			// TODO: implement properly
			try {
				Media m = new Media(item.getUri().toString());

				// make a playlistItem and covert to metadata //why? // not 100%sure...
				// because PlaylistItem has advanced update() method? // probably
				return new PlaylistItem(item.getUri(), "", "", m.getDuration().toMillis()).toMeta();
			} catch (IllegalArgumentException|UnsupportedOperationException e) {
				logger(MetadataReader.class).error("Error creating metadata for non file based item: {}", item);
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
		noNull(items);
		noNull(onEnd);
		return new SuccessTask<List<Metadata>,SuccessTask>("Reading metadata", onEnd) {
			private final int all = items.size();
			private int completed = 0;
			private int skipped = 0;

			@Override
			protected List<Metadata> call() {
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

				for (Item item : input) {
					if (isCancelled()) {
						logger(MetadataReader.class).info("Metadata reading was canceled.");
						break;
					}

					Metadata m;
					try {
						m = APP.db.getItem(item);
						if (m==null) {
							MetadataWriter.useNoRefresh(item, MetadataWriter::setLibraryAddedNowIfEmpty);
							m = readMetadata(item);

							if (m.isEmpty()) {
								skipped.add(item);
							} else {
								converted.add(m);
							}
						} else {
							skipped.add(item);
						}
					} catch (Exception e) {
						logger(MetadataReader.class).warn("Problem during reading tag of {}", item, e);
					}

					processed.add(item);

					// update progress
					updateMessage(all.size(), processed.size());
					updateProgress(processed.size(), all.size());
					updateSkipped(skipped.size());
				}

				APP.db.addItems(converted);

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
			protected Void call() {
				List<Metadata> allItems = APP.db.getItemsById().streamV().collect(toList());
				List<Metadata> removedItems = new ArrayList<>();
				all = allItems.size();

				for (Metadata m : allItems) {
					if (isCancelled()) break;

					completed++;

					if (m.isFileBased() && !noNull(m.getFile()).exists()) {
						removed++;
						removedItems.add(m);
					}

					// update state
					updateMessage(all, completed, removed);
					updateProgress(completed, all);
				}

				APP.db.removeItems(removedItems);

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