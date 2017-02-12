package AlbumView;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import audio.playlist.PlaylistManager;
import audio.tagging.Metadata;
import audio.tagging.MetadataGroup;
import gui.objects.grid.GridCell;
import gui.objects.grid.GridView;
import gui.objects.image.Thumbnail;
import gui.objects.image.cover.Cover;
import layout.widget.Widget;
import layout.widget.controller.ClassController;
import layout.widget.controller.io.Input;
import layout.widget.controller.io.Output;
import services.database.Db;
import unused.TriConsumer;
import util.SwitchException;
import util.access.V;
import util.animation.Anim;
import util.async.executor.EventReducer;
import util.conf.IsConfig;
import util.conf.IsConfig.EditMode;
import util.functional.Util;
import util.graphics.drag.DragUtil;

import static AlbumView.AlbumView.AnimateOn.IMAGE_CHANGE_1ST_TIME;
import static AlbumView.AlbumView.CellSize.NORMAL;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static util.Util.loadImageFull;
import static util.Util.loadImageThumb;
import static util.async.Async.*;
import static util.async.future.Fut.fut;
import static util.dev.Util.throwIfNotFxThread;
import static util.functional.Util.*;
import static util.graphics.Util.bgr;
import static util.graphics.Util.setAnchor;

/**
 * Logger widget controller.
 *
 * @author Martin Polakovic
 */
@Widget.Info(
		author = "Martin Polakovic",
		name = "AlbumView",
		description = "Displays console output by listening to System.out, which contains all of the "
				+ "application logging.",
		howto = "",
		notes = "",
		version = "1",
		year = "2015",
		group = Widget.Group.DEVELOPMENT
)
public class AlbumView extends ClassController {

	static final double CELL_TEXT_HEIGHT = 40;

	Output<MetadataGroup> out_sel;
	Output<List<Metadata>> out_sel_met;
	Input<List<Metadata>> in_items;
	final GridView<Album,MetadataGroup> view = new GridView<>(MetadataGroup.class, a -> a.items, NORMAL.width, NORMAL.height, 5, 5);
	final ExecutorService executorThumbs = newSingleDaemonThreadExecutor();
	final ExecutorService executorImage = newSingleDaemonThreadExecutor(); // 2 threads perform better, but cause bugs

	@IsConfig(name = "Thumbnail size", info = "Size of the thumbnail.")
	final V<CellSize> cellSize = new V<>(NORMAL, s -> s.apply(view));
	@IsConfig(name = "Animate thumbs on", info = "Determines when the thumbnail image transition is played.")
	final V<AnimateOn> animateThumbOn = new V<>(IMAGE_CHANGE_1ST_TIME);

	public AlbumView() {
		view.primaryFilterField = MetadataGroup.Field.VALUE;
		view.setCellFactory(grid -> new AlbumCell());
		view.selectedItem.onChange(item -> {
			out_sel.setValue(item== null ? null : item.items);
			out_sel_met.setValue(item== null ? Util.listRO() : item.items.getGrouped());
		});
		setAnchor(this, view, 0d);

		view.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.ENTER)
				playSelected();
		});
	}

	@Override
	public void init() {
		in_items = getInputs().create("To display", List.class, listRO(), this::setItems);
		out_sel = outputs.create(widget.id,"Selected Album", MetadataGroup.class, null);
		out_sel_met = outputs.create(widget.id,"Selected", List.class, listRO());
	}

	/** Populates metadata groups to table from metadata list. */
	private void setItems(List<Metadata> list) {
		if (list==null) return;
		fut(Metadata.Field.ALBUM)
			.use(f -> {
				List<MetadataGroup> mgs = MetadataGroup.groupsOf(f,list).collect(toList());
				List<Metadata> fl = filterList(list,true);
				runLater(() -> {
					if (!mgs.isEmpty()) {
						selectionStore();

						Map<String,Album> m = stream(view.getItemsRaw()).toMap(a -> a.name, a -> a);
						List<Album> as = stream(mgs).map(Album::new)
								.peek(a -> {
									Album tmp = m.get(a.name);
									if (tmp!=null) {
										a.cover_file = tmp.cover_file;
										a.cover = tmp.cover;
										a.cover_loadedFull = tmp.cover_loadedFull;
										a.coverFile_loaded = tmp.coverFile_loaded;
										a.cover_loadedThumb = tmp.cover_loadedThumb;
									}
								})
								.sorted(by(a -> a.name))
								.toList();

						view.getItemsRaw().setAll(as);
						selectionReStore();
						out_sel_met.setValue(fl);
					}
				});
			})
			.run();
	}

	private List<Metadata> filterList(List<Metadata> list, boolean orAll) {
		if (list==null || list.isEmpty()) return listRO();
		return view.getSelectedOrAllItems(orAll)
				   .flatMap(mg -> mg.items.getGrouped().stream())
				   .collect(toList());
	}

	/**
	 *  Get all items in grouped in the selected groups, sorts using library sort order.
	 */
	private List<Metadata> filerListToSelectedNsort() {
		List<Metadata> l = filterList(in_items.getValue(),false);
		l.sort(Db.library_sorter.get());
		return l;
	}

	private void playSelected() {
		play(getSelected());
	}

	private List<Metadata> getSelected() {
		return filterList(in_items.getValue(),false);
	}

	private void play(List<Metadata> items) {
		if (items.isEmpty()) return;
		PlaylistManager.use(p -> p.setNplay(items.stream().sorted(Db.library_sorter.get())));
	}

/* ---------- SELECTION RESTORE ------------------------------------------------------------------------------------- */

    // restoring selection if table items change, we want to preserve as many
    // selected items as possible - when selection changes, we select all items
    // (previously selected) that are still in the table
    private boolean sel_ignore = false;
    private boolean sel_ignore_canturnback = true;
    private Set sel_old;
    // restoring selection from previous session, we serialize string
    // representation and try to restre when application runs again
    // we restore only once
    @IsConfig(name = "Last selected", editable = EditMode.APP)
    private String sel_last = "null";
    private boolean sel_last_restored = false;

    private void selectionStore() {
        // remember selected
        sel_old = view.getSelectedItems().map(a -> a.items.getValue()).collect(toSet());
        sel_ignore = true;
        sel_ignore_canturnback = false;
    }

    private void selectionReStore() {
        if (view.getItemsRaw().isEmpty()) return;

        // restore last selected from previous session
        if (!sel_last_restored && !"null".equals(sel_last)) {
            forEachWithI(view.getItemsRaw(), (i,album) -> {
                if (album.name.equals(sel_last)) {
	                view.implGetSkin().select(album);
                    sel_last_restored = true; // restore only once
                    return;
                }
            });

        // update selected - restore every available old one
        } else {
            forEachWithI(view.getItemsRaw(), (i,album) -> {
                if (sel_old.contains(album.items.getValue())) {
                    view.implGetSkin().select(album);
                }
            });
        }

        sel_ignore = false;
        sel_ignore_canturnback = true;
    }

/* ------------------------------------------------------------------------------------------------------------------ */

	class Album {
		final MetadataGroup items;
		final String name;
		public Image cover;
		public File cover_file;
		public boolean coverFile_loaded;
		public boolean cover_loadedThumb;
		public volatile boolean cover_loadedFull;

		public Album(MetadataGroup items) {
			this.items = items;
			this.name = items.getValueS("");
		}

		public void loadCover(boolean full, double width, double height, TriConsumer<Boolean,File,Image> action) {
			File file = getCoverFile();
			if (file!=null) {
				if (full) {
					// Normally, we would use: boolean was_loaded = cover_loadedFull;
					// but that would cause animation to be played again, which we do not want
					boolean was_loaded = cover_loadedThumb || cover_loadedFull;
					if (!cover_loadedFull) {
						Image img = loadImageFull(file, width, height);
						if (img!=null) {
							cover = img;
							action.accept(was_loaded,file,cover);
						}
						cover_loadedFull = true;
					}
				} else {
					boolean was_loaded = cover_loadedThumb;
					if (!cover_loadedThumb) {
						Image imgc = Thumbnail.getCached(file, width, height);
						cover = imgc!=null ? imgc : loadImageThumb(file, width, height);
						cover_loadedThumb = true;
					}
					action.accept(was_loaded,file,cover);
				}
			}
		}

		protected File getCoverFile() {
			if (coverFile_loaded) return cover_file;
			coverFile_loaded = true;
			cover_file = computeCoverFile();
			return cover_file;
		}

		protected File computeCoverFile() {
			return items.getGrouped().stream().findAny()
				       .map(m -> m.getCover(Cover.CoverSource.DIRECTORY))
				       .map(Cover::getFile).orElse(null);
		}
	}

	class AlbumCell extends GridCell<Album,MetadataGroup> {
		Pane root;
		Label name;
		Thumbnail thumb;
		EventReducer<Album> setCoverLater = EventReducer.toLast(100, item -> executorThumbs.execute(() -> {
			sleep(10); // gives FX thread some space to avoid lag under intense workload
			runFX(() -> {
				if (item == getItem())
					setCoverNow(item);
			});
		}));

		@Override
		protected void updateItem(Album item, boolean empty) {
			if (getItem() == item) return;
			super.updateItem(item, empty);

			if (item == null) {
				// empty cell has no graphics
				// we do not clear the content of the graphics however
				setGraphic(null);
			} else {
				if (root == null) {
					// we create graphics only once and only when first requested
					createGraphics();
					// we set graphics only once (when creating it)
					setGraphic(root);
				}
				// if cell was previously empty, we set graphics back
				// this improves performance compared to setting it every time
				if (getGraphic() != root) setGraphic(root);

				// set name
				name.setText(item.name);
				// set cover
				// The item caches the cover Image, so it is only loaded once. That is a heavy op.
				// This method can be called very frequently and:
				//     - block ui thread when scrolling
				//     - reduce ui performance when resizing
				// Solved by delaying the image loading & drawing, which reduces subsequent
				// invokes into single update (last).
				boolean loadLater = item.cover_loadedFull;
				if (loadLater) setCoverNow(item);
				else setCoverLater(item);
			}
		}

		@Override
		public void updateSelected(boolean selected) {
			super.updateSelected(selected);
			if (thumb != null && thumb.image.get() != null) thumb.animationPlayPause(selected);
		}

		private void createGraphics() {
			name = new Label();
			name.setAlignment(Pos.CENTER);
			thumb = new Thumbnail() {
				@Override
				protected Object getRepresentant() {
					return getItem();
				}
			};
			thumb.getPane().hoverProperty().addListener((o, ov, nv) -> thumb.getView().setEffect(nv ? new ColorAdjust(0, 0, 0.2, 0) : null));
			thumb.setDragEnabled(false);
			thumb.getPane().setOnDragDetected(e -> {
				if (e.getButton()==MouseButton.PRIMARY && view.selectedItem.get()!=null) {
					Dragboard db = thumb.getPane().startDragAndDrop(TransferMode.COPY);
					if (thumb.getImage()!=null) db.setDragView(thumb.getImage());
					DragUtil.setItemList(filerListToSelectedNsort(),db,true);
				}
				e.consume();
			});
			thumb.getPane().addEventHandler(MOUSE_CLICKED, e -> {
				if (e.getButton()==MouseButton.PRIMARY && e.getClickCount()==2)
					playSelected();
			});

			root = new Pane(thumb.getPane(), name) {
				// Cell layout should be fast - gets called multiple times on resize.
				// Why not use custom pane for more speed if we can.
				@Override
				protected void layoutChildren() {
					double w = getWidth();
					double h = getHeight();
					thumb.getPane().resizeRelocate(0, 0, w, h - CELL_TEXT_HEIGHT);
					name.resizeRelocate(0, h - CELL_TEXT_HEIGHT, w, CELL_TEXT_HEIGHT);
				}
			};
			root.setBackground(bgr(new Color(0,0,0,0.2)));
			root.setPadding(Insets.EMPTY);
			root.setMinSize(-1, -1);
			root.setPrefSize(-1, -1);
			root.setMaxSize(-1, -1);
		}

		/**
		 * Begins loading cover for the item. If item changes meanwhile, the result is stored
		 * (it will not need to load again) to the old item, but not showed.
		 * <p/>
		 * Thumbnail quality may be decreased to achieve good performance, while loading high
		 * quality thumbnail in the bgr. Each phase uses its own executor.
		 * <p/>
		 * Must be called on FX thread.
		 */
		private void setCoverNow(Album item) {
			throwIfNotFxThread();
	        if (item.cover_loadedFull) {
		        setCoverPost(item, true, item.cover_file, item.cover);
	        } else {
	            double width = cellSize.get().width,
	                   height = cellSize.get().height - CELL_TEXT_HEIGHT;
	            // load thumbnail
	            executorThumbs.execute(() ->
	                item.loadCover(false, width, height, (was_loaded, file, img) -> setCoverPost(item, was_loaded, file, img))
	            );
	            // load high quality thumbnail
	            executorImage.execute(() ->
	                item.loadCover(true, width, height, (was_loaded, file, img) -> setCoverPost(item, was_loaded, file, img))
	            );
	        }
		}

		private void setCoverLater(Album item) {
			throwIfNotFxThread();
			thumb.loadImage((File) null); // prevent displaying old content before cover loads
			setCoverLater.push(item);
		}

		/** Finished loading of the cover. */
		private void setCoverPost(Album item, boolean imgAlreadyLoaded, File imgFile, Image img) {
            runFX(() -> {
                if (item == getItem()) { // prevents content inconsistency
                    boolean animate = animateThumbOn.get().needsAnimation(this, imgAlreadyLoaded, imgFile, img);
                    thumb.loadImage(img, imgFile);
                    if (animate)
                        new Anim(thumb.getView()::setOpacity).dur(400).intpl(x -> x * x * x * x).play();
                }
            });
		}
	}

	enum CellSize {
		SMALL(100, 100 + CELL_TEXT_HEIGHT),
		NORMAL(200, 200 + CELL_TEXT_HEIGHT),
		LARGE(300, 300 + CELL_TEXT_HEIGHT),
		GIANT(450, 450 + CELL_TEXT_HEIGHT);

		final double width;
		final double height;

		CellSize(double width, double height) {
			this.width = width;
			this.height = height;
		}

		void apply(GridView<?, ?> grid) {
			grid.setCellWidth(width);
			grid.setCellHeight(height);
		}
	}

	enum AnimateOn {
		IMAGE_CHANGE, IMAGE_CHANGE_1ST_TIME, IMAGE_CHANGE_FROM_EMPTY;

		public boolean needsAnimation(AlbumCell cell, boolean imgAlreadyLoaded, File imgFile, Image img) {
			if (this == IMAGE_CHANGE)
				return cell.thumb.image.get() != img;
			else if (this == IMAGE_CHANGE_FROM_EMPTY)
				return cell.thumb.image.get() == null && img != null;
			else if (this == IMAGE_CHANGE_1ST_TIME)
				return !imgAlreadyLoaded && img != null;
			else
				throw new SwitchException(this);
		}
	}
}