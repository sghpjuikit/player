package sp.it.pl.audio.tagging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import javafx.concurrent.Task;
import javafx.scene.media.Media;
import sp.it.pl.audio.Song;
import sp.it.pl.audio.playlist.PlaylistSong;
import sp.it.pl.util.async.future.ConvertListTask;
import sp.it.pl.util.file.AudioFileFormat.Use;
import static java.util.stream.Collectors.toList;
import static sp.it.pl.audio.tagging.ExtKt.readAudioFile;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.pl.util.dev.DebugKt.logger;
import static sp.it.pl.util.dev.FailKt.failIfFxThread;
import static sp.it.pl.util.dev.FailKt.noNull;

public class MetadataReader {

	// TODO: return Try

	/**
	 * Reads metadata for specified song.
	 * <p/>
	 * Involves I/O operation and blocks thread. Throws exception if executed on fx application thread.
	 *
	 * @param song song to read metadata for
	 * @return metadata for specified song or {@link Metadata#EMPTY} if any error. Never null.
	 */
	public static Metadata readMetadata(Song song) {
		failIfFxThread();

		if (song.isCorrupt(Use.APP)) {
			return Metadata.EMPTY;
		}

		if (song.isFileBased()) {
			return readAudioFile(noNull(song.getFile()))
				.map(Metadata::new)
				.getOr(Metadata.EMPTY);
		} else {
			// TODO: implement properly
			try {
				Media m = new Media(song.getUri().toString());

				// make a playlistItem and covert to metadata //why? // not 100%sure...
				// because PlaylistSong has advanced update() method? // probably
				return new PlaylistSong(song.getUri(), "", "", m.getDuration().toMillis()).toMeta();
			} catch (IllegalArgumentException|UnsupportedOperationException e) {
				logger(MetadataReader.class).error("Error creating metadata for non file based song: {}", song);
				return song.toMeta();
			}
		}
	}

	/**
	 * Creates task that reads metadata for specified songs.
	 *
	 * @param songs list of songs to read metadata for
	 * @return the task reading the files returning all successfully read metadata
	 * @throws NullPointerException if any parameter null
	 */
	public static Task<List<Metadata>> readMetadataTask(Collection<? extends Song> songs) {
		noNull(songs);
		return new Task<>() {
			private final StringBuffer sb = new StringBuffer(40);
			private final int all = songs.size();
			private int completed = 0;
			private int failed = 0;

			{
				updateTitle("Reading metadata");
			}

			@Override
			protected List<Metadata> call() {
				List<Metadata> metadatas = new ArrayList<>();

				for (Song song : songs) {
					if (isCancelled()) return metadatas;

					completed++;

					Metadata m = readMetadata(song);
					if (m.isEmpty()) failed++; // on fail
					else metadatas.add(m);	// on success

					updateMessage(all, completed, failed);
					updateProgress(completed, all);
				}

				if (!isCancelled()) {
					updateMessage(all, completed, failed);
					updateProgress(completed, all);
				}

				return metadatas;
			}

			private void updateMessage(int all, int done, int failed) {
				sb.setLength(0);
				sb.append("Read: ");
				sb.append(done);
				sb.append("/");
				sb.append(all);
				sb.append(" ");
				sb.append(" Failed: ");
				sb.append(failed);
				updateMessage(sb.toString());
			}
		};
	}

	/**
	 * Creates a task that:
	 * <ul>
	 * <li> Reads metadata from files of the songs.
	 * <li> Adds songs to library. If library already contains the song, it will not be added.
	 * <li> Returns detailed information about the end result
	 * </ul>
	 *
	 * @return the task
	 */
	public static ConvertListTask<Song,Metadata> addSongsToLibTask() {
		return new ConvertListTask<>("Adding songs to library") {
			@Override
			protected Result<Song,Metadata> compute(Collection<? extends Song> input) {
				List<Song> all = new ArrayList<>(input);
				List<Song> processed = new ArrayList<>(all.size());
				List<Metadata> converted = new ArrayList<>(all.size());
				List<Song> skipped = new ArrayList<>(0);

				for (Song song : input) {
					if (isCancelled()) break;

					Metadata m;
					try {
						m = APP.db.getSong(song);
						if (m==null) {
							MetadataWriter.useNoRefresh(song, MetadataWriter::setLibraryAddedNowIfEmpty);
							m = readMetadata(song);

							if (m.isEmpty()) {
								skipped.add(song);
							} else {
								converted.add(m);
							}
						} else {
							skipped.add(song);
						}
					} catch (Exception e) {
						logger(MetadataReader.class).warn("Problem during reading tag of {}", song, e);
					}

					processed.add(song);

					// update progress
					updateMessage(all.size(), processed.size());
					updateProgress(processed.size(), all.size());
					updateSkipped(skipped.size());
				}

				if (!isCancelled()) {
					APP.db.addSongs(converted);

					// update progress
					updateMessage(all.size(), processed.size());
					updateProgress(processed.size(), all.size());
					updateSkipped(skipped.size());
				}

				return new Result<>(all, processed, converted, skipped);
			}
		};
	}

	/**
	 * @return a task that removes from library all songs, which refer to non-existent files
	 */
	public static Task<Void> removeMissingSongsFromLibTask() {
		return new Task<>() {
			private final StringBuffer sb = new StringBuffer(40);
			private int all = 0;
			private int completed = 0;

			{
				updateTitle("Removing missing songs from library");
			}

			@Override
			protected Void call() {
				List<Metadata> allItems = APP.db.getSongsById().streamV().collect(toList());
				List<Metadata> removedItems = new ArrayList<>();
				all = allItems.size();

				for (Metadata m : allItems) {
					if (isCancelled()) break;

					completed++;

					if (m.isFileBased() && !noNull(m.getFile()).exists()) {
						removedItems.add(m);
					}

					updateMessage(all, completed, 0);
					updateProgress(completed, all);
				}

				if (!isCancelled()) {
					APP.db.removeSongs(removedItems);

					updateMessage(all, completed, removedItems.size());
					updateProgress(completed, all);
				}

				return null;
			}

			private void updateMessage(int all, int done, int removed) {
				sb.setLength(0);
				sb.append("Checked: ");
				sb.append(done);
				sb.append("/");
				sb.append(all);
				sb.append(" ");
				sb.append(" Removed: ");
				sb.append(removed);
				updateMessage(sb.toString());
			}
		};
	}

	@SuppressWarnings("DuplicateExpressions")
	public static <T> void setOnDone(Task<T> task, BiConsumer<Boolean, T> onEnd) {
		task.setOnSucceeded(e -> onEnd.accept(true, task.getValue()));
		task.setOnFailed(e -> onEnd.accept(false, task.getValue()));
		task.setOnCancelled(e -> onEnd.accept(false, task.getValue()));
	}
}