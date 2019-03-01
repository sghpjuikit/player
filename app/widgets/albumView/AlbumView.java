package albumView;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import kotlin.Unit;
import sp.it.pl.audio.playlist.PlaylistManager;
import sp.it.pl.audio.tagging.Metadata;
import sp.it.pl.audio.tagging.MetadataGroup;
import sp.it.pl.gui.itemnode.FieldedPredicateItemNode.PredicateData;
import sp.it.pl.gui.objects.grid.GridCell;
import sp.it.pl.gui.objects.grid.GridView;
import sp.it.pl.gui.objects.grid.GridView.CellSize;
import sp.it.pl.gui.objects.image.Thumbnail;
import sp.it.pl.gui.objects.image.cover.Cover;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.controller.LegacyController;
import sp.it.pl.layout.widget.controller.SimpleController;
import sp.it.pl.layout.widget.controller.io.Input;
import sp.it.pl.layout.widget.controller.io.Output;
import sp.it.pl.util.SwitchException;
import sp.it.pl.util.access.V;
import sp.it.pl.util.access.fieldvalue.ObjectField;
import sp.it.pl.util.animation.Anim;
import sp.it.pl.util.async.executor.EventReducer;
import sp.it.pl.util.conf.EditMode;
import sp.it.pl.util.conf.IsConfig;
import sp.it.pl.util.functional.TriConsumer;
import sp.it.pl.util.functional.Util;
import sp.it.pl.util.graphics.Resolution;
import sp.it.pl.util.graphics.drag.DragUtil;
import sp.it.pl.util.graphics.image.Image2PassLoader;
import sp.it.pl.util.graphics.image.ImageSize;
import static albumView.AlbumView.AnimateOn.IMAGE_CHANGE_1ST_TIME;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.util.Duration.millis;
import static sp.it.pl.audio.tagging.Metadata.Field.ALBUM;
import static sp.it.pl.audio.tagging.MetadataGroup.Field.VALUE;
import static sp.it.pl.gui.objects.grid.GridView.CellSize.NORMAL;
import static sp.it.pl.main.AppExtensionsKt.scaleEM;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.pl.util.async.AsyncKt.oneThreadExecutor;
import static sp.it.pl.util.async.AsyncKt.runFX;
import static sp.it.pl.util.async.AsyncKt.runLater;
import static sp.it.pl.util.async.AsyncKt.sleep;
import static sp.it.pl.util.async.future.Fut.fut;
import static sp.it.pl.util.dev.FailKt.failIfNotFxThread;
import static sp.it.pl.util.functional.Util.by;
import static sp.it.pl.util.functional.Util.forEachWithI;
import static sp.it.pl.util.functional.Util.listRO;
import static sp.it.pl.util.functional.Util.map;
import static sp.it.pl.util.functional.Util.stream;
import static sp.it.pl.util.functional.UtilKt.consumer;
import static sp.it.pl.util.reactive.UtilKt.attach1IfNonNull;
import static sp.it.pl.util.reactive.UtilKt.sync1If;

@SuppressWarnings("WeakerAccess")
@Widget.Info(
		author = "Martin Polakovic",
		name = "AlbumView",
		description = "Displays console output by listening to System.out, which contains all of the "
				+ "application logging.",
//		howto = "",
//		notes = "",
		version = "1",
		year = "2015",
		group = Widget.Group.DEVELOPMENT
)
@LegacyController
public class AlbumView extends SimpleController {

	static final double CELL_TEXT_HEIGHT = 40;

	@IsConfig(name = "Thumbnail size", info = "Size of the thumbnail.")
	final V<CellSize> cellSize = new V<>(NORMAL);
	@IsConfig(name = "Thumbnail size ratio", info = "Size ratio of the thumbnail.")
	final V<Resolution> cellSizeRatio = new V<>(Resolution.R_1x1);
	@IsConfig(name = "Animate thumbs on", info = "Determines when the thumbnail image transition is played.")
	final V<AnimateOn> animateThumbOn = new V<>(IMAGE_CHANGE_1ST_TIME);

	Output<MetadataGroup> out_sel;
	Output<List<Metadata>> out_sel_met;
	Input<List<Metadata>> in_items;
	final GridView<Album,MetadataGroup> view = new GridView<>(MetadataGroup.class, a -> a.items, cellSize.get().width, cellSize.get().width*cellSizeRatio.get().ratio +CELL_TEXT_HEIGHT, 5, 5);
	final ExecutorService executorThumbs = oneThreadExecutor();
	final ExecutorService executorImage = oneThreadExecutor(); // 2 threads perform better, but cause bugs

	@SuppressWarnings("unchecked")
	public AlbumView(Widget widget) {
		super(widget);
		root.setPrefSize(scaleEM(800), scaleEM(800));

		view.search.field = VALUE;
		view.primaryFilterField = VALUE;
		view.setCellFactory(grid -> new AlbumCell());
		view.selectedItem.onChange(item -> {
			out_sel.setValue(item== null ? null : item.items);
			out_sel_met.setValue(item== null ? Util.listRO() : item.items.getGrouped());
		});
		root.getChildren().add(view);

		view.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.ENTER)
				playSelected();
		});

		// update filters of VALUE type, we must wat until skin has been built
		sync1If(view.skinProperty(), v -> v!=null, skin -> {
			Metadata.Field<String> f = ALBUM;
			view.implGetSkin().filter.inconsistent_state = true;
			view.implGetSkin().filter.setPrefTypeSupplier(() -> PredicateData.ofField(VALUE));
			view.implGetSkin().filter.setData(map(MetadataGroup.Field.FIELDS, mgf -> new PredicateData<ObjectField<MetadataGroup,Object>>(mgf.toString(f), mgf.getType(f), (MetadataGroup.Field) mgf)));
			view.implGetSkin().filter.shrinkTo(0);
			view.implGetSkin().filter.growTo1();
			view.implGetSkin().filter.clear();
			return Unit.INSTANCE;
		});

		in_items = inputs.create("To display", (Class) List.class, listRO(), this::setItems);
		out_sel = outputs.create(widget.id,"Selected Album", MetadataGroup.class, null);
		out_sel_met = outputs.create(widget.id,"Selected", (Class<List<Metadata>>) (Object) List.class, listRO());

		cellSize.onChange(v -> applyCellSize());
		cellSizeRatio.onChange(v -> applyCellSize());
	}

	@Override
	public void focus() {
		attach1IfNonNull(view.skinProperty(), consumer(skin -> view.implGetSkin().requestFocus()));
	}

	void applyCellSize() {
		view.setCellWidth(cellSize.get().width);
		view.setCellHeight((cellSize.get().width/cellSizeRatio.get().ratio)+CELL_TEXT_HEIGHT);
	}

	/** Populates metadata groups to table from metadata list. */
	private void setItems(List<Metadata> list) {
		if (list==null) return;
		fut(ALBUM)
			.useBy(f -> {
				List<MetadataGroup> mgs = MetadataGroup.groupsOf(f,list).collect(toList());
				List<Metadata> fl = filterList(list,true);
				runLater(() -> {
					if (!mgs.isEmpty()) {
						selectionStore();

						Map<String,Album> m = stream(view.getItemsRaw()).collect(toMap(a -> a.name, a -> a));
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
								.collect(toList());

						view.getItemsRaw().setAll(as);
						selectionReStore();
						out_sel_met.setValue(fl);
					}
				});
			});
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
		l.sort(APP.db.getLibraryComparator().get());
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
		PlaylistManager.use(p -> p.setNplay(items.stream().sorted(APP.db.getLibraryComparator().get())));
	}

/* ---------- SELECTION RESTORE ------------------------------------------------------------------------------------- */

    // restoring selection if table items change, we want to preserve as many
    // selected items as possible - when selection changes, we select all items
    // (previously selected) that are still in the table
    private boolean sel_ignore = false;
    private boolean sel_ignore_canturnback = true;
    private Set<Object> sel_old;
    // restoring selection from previous session, we serialize string
    // representation and try to restore when application runs again
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

		public void loadCover(boolean full, ImageSize size, TriConsumer<Boolean,File,Image> action) {
			File file = getCoverFile();
			if (file!=null) {
				if (full) {
					// Normally, we would use: boolean was_loaded = cover_loadedFull;
					// but that would cause animation to be played again, which we do not want
					boolean was_loaded = cover_loadedThumb || cover_loadedFull;
					if (!cover_loadedFull) {
						Image img = Image2PassLoader.INSTANCE.getLq().invoke(file, size);
						if (img!=null) {
							cover = img;
							action.accept(was_loaded,file,cover);
						}
						cover_loadedFull = true;
					}
				} else {
					boolean was_loaded = cover_loadedThumb;
					if (!cover_loadedThumb) {
						Image imgc = Thumbnail.getCached(file, size);
						cover = imgc!=null ? imgc : Image2PassLoader.INSTANCE.getHq().invoke(file, size);
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
				public MetadataGroup getRepresentant() {
					return getItem() == null ? null : getItem().items;
				}
			};
			thumb.setBorderVisible(true);
			thumb.setDragEnabled(false);
			thumb.getPane().setOnDragDetected(e -> {
				if (e.getButton()==MouseButton.PRIMARY && view.selectedItem.get()!=null) {
					Dragboard db = thumb.getPane().startDragAndDrop(TransferMode.COPY);
					if (thumb.getImage()!=null) db.setDragView(thumb.getImage());
					DragUtil.setSongList(filerListToSelectedNsort(),db,true);
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
			root.setMinSize(-1, -1);
			root.setPrefSize(-1, -1);
			root.setMaxSize(-1, -1);
			root.hoverProperty().addListener((o, ov, nv) -> root.setEffect(nv ? new ColorAdjust(0, 0, 0.15, 0) : null));
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
			failIfNotFxThread();

	        if (item.cover_loadedFull) {
		        setCoverPost(item, true, item.cover_file, item.cover);
	        } else {
				ImageSize size = thumb.calculateImageLoadSize();
	            // load thumbnail
	            executorThumbs.execute(() ->
	                item.loadCover(false, size, (was_loaded, file, img) -> setCoverPost(item, was_loaded, file, img))
	            );
	            // load high quality thumbnail
	            executorImage.execute(() ->
	                item.loadCover(true, size, (was_loaded, file, img) -> setCoverPost(item, was_loaded, file, img))
	            );
	        }
		}

		private void setCoverLater(Album item) {
			failIfNotFxThread();

			thumb.loadImage((File) null); // prevent displaying old content before cover loads
			setCoverLater.push(item);
		}

		/** Finished loading of the cover. */
		private void setCoverPost(Album item, boolean imgAlreadyLoaded, File imgFile, Image img) {
            runFX(() -> {
                if (item == getItem()) { // prevents content inconsistency
                    boolean animate = animateThumbOn.get().needsAnimation(this, imgAlreadyLoaded, img);
                    thumb.loadImage(img, imgFile);
                    if (animate)
                        new Anim(thumb.getView()::setOpacity).dur(millis(400)).intpl(x -> x*x*x*x).play();
                }
            });
		}
	}

	enum AnimateOn {
		IMAGE_CHANGE, IMAGE_CHANGE_1ST_TIME, IMAGE_CHANGE_FROM_EMPTY;

		public boolean needsAnimation(AlbumCell cell, boolean imgAlreadyLoaded, Image img) {
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