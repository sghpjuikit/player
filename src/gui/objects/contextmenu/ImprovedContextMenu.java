package gui.objects.contextmenu;

import audio.Item;
import audio.playlist.PlaylistManager;
import audio.tagging.Metadata.Field;
import audio.tagging.MetadataGroup;
import audio.tagging.PlaylistItemGroup;
import gui.objects.image.Thumbnail;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.MouseEvent;
import javafx.stage.Screen;
import layout.widget.WidgetFactory;
import layout.widget.feature.FileExplorerFeature;
import layout.widget.feature.SongReader;
import layout.widget.feature.SongWriter;
import main.App;
import util.access.AccessibleValue;
import util.collections.map.ClassListMap;
import util.file.ImageFileFormat;
import util.file.Util;
import util.functional.Functors.Ƒ2;
import util.parsing.Parser;
import util.system.Environment;
import web.SearchUriBuilder;
import static java.util.stream.Collectors.toList;
import static layout.widget.WidgetManager.WidgetSource.NO_LAYOUT;
import static main.App.APP;
import static util.dev.Util.log;
import static util.dev.Util.noØ;
import static util.file.Util.recycleFile;
import static util.functional.Util.map;
import static util.graphics.Util.menuItem;
import static util.graphics.Util.menuItems;
import static util.graphics.UtilKt.getScreen;
import static util.system.Environment.copyToSysClipboard;

/**
 * Context menu which contains an object.
 * Usually, this object is set before showing the menu and allows menu items
 * to use this value for action.
 */
public class ImprovedContextMenu<E> extends ContextMenu implements AccessibleValue<E> {

	protected E v;

	public ImprovedContextMenu() {
		setConsumeAutoHidingEvents(false);
	}

	public ImprovedContextMenu(MenuItem... items) {
		this();
		getItems().addAll(items);
	}

	@Override
	public E getValue() {
		return v;
	}

	@Override
	public void setValue(E val) {
		this.v = val;
	}

	public void setValueAndItems(E value) {
		this.v = value;
		setItemsForValue();
	}

	@Override
	public void show(Node n, double screenX, double screenY) {
		super.show(n.getScene().getWindow(), screenX, screenY);
	}

	/**
	 * Shows the context menu for node at proper coordinates derived from mouse
	 * event.
	 * <p/>
	 * Prefer this method to show context menu. Use in MouseClick handler.
	 * <p/>
	 * Reason:
	 * When showing ContextMenu, there is a big difference between show(Window,x,y)
	 * and (Node,x,y). The former will not hide the menu when next click happens
	 * within the node itself! This method avoids that.
	 */
	public void show(Node n, MouseEvent e) {
		super.show(n.getScene().getWindow(), e.getScreenX(), e.getScreenY());
	}

	public void show(Node n, ContextMenuEvent e) {
		super.show(n.getScene().getWindow(), e.getScreenX(), e.getScreenY());
	}

	/**
	 * Adds menu item at the end with specified text and action.
	 *
	 * @param text non null text of the child item
	 * @param action non null onAction handler of the child item, taking the value as a parameter
	 */
	public void addItem(String text, Consumer<? super E> action) {
		noØ(text, action);
		MenuItem i = new MenuItem(text);
		i.setOnAction(e -> action.accept(getValue()));
		getItems().add(i);
	}

	public void addItemsForValue() {
		addItemsForValue(v);
	}

	public void addItemsForValue(Object value) {
		List<MenuItem> menuItems = CONTEXT_MENUS.get(this, value).collect(toList());
		getItems().addAll(menuItems);
	}

	public void setItemsForValue() {
		setItemsForValue(v);
	}

	public void setItemsForValue(Object value) {
		getItems().clear();
		addItemsForValue(value);
	}

/* ------------------------------------------------------------------------------------------------------------------ */

	public static final ContextMenuItemSuppliers CONTEXT_MENUS = new ContextMenuItemSuppliers();

	// TODO: move to app initialization
	static {
		CONTEXT_MENUS.add(
				File.class,
				(contextMenu, file) -> Stream.of(
						menuItem("Browse location", () -> Environment.browse(file)),
						menuItem("Open (in associated program)", () -> Environment.open(file)),
						menuItem("Edit (in associated editor)", () -> Environment.edit(file)),
						menuItem("Delete from disc", () -> recycleFile(file)),
						menuItem("Copy as ...", () ->
								Environment.saveFile(
										"Copy as...",
										App.APP.DIR_APP,
										file.getName(),
										contextMenu.getOwnerWindow(),
										ImageFileFormat.filter()
								)
										.ifOk(nf -> {
											try {
												Files.copy(file.toPath(), nf.toPath(), StandardCopyOption.REPLACE_EXISTING);
											} catch (IOException ex) {
												log(ImprovedContextMenu.class).error("File cpy failed.", ex);
											}
										})
						)
				)
		);
		CONTEXT_MENUS.add(
				MetadataGroup.class,
				(contextMenu, mg) -> Stream.of(
						menuItem("Play items", () -> PlaylistManager.use(p -> p.setNplay(mg.getGrouped().stream().sorted(APP.db.getLibraryComparator().get())))),
						menuItem("Enqueue items", () -> PlaylistManager.use(p -> p.addItems(mg.getGrouped()))),
						menuItem("Update items from file", () -> APP.actions.refreshItemsFromFileJob(mg.getGrouped())),
						menuItem("Remove items from library", () -> APP.db.removeItems(mg.getGrouped())),
						new Menu("Show in", null,
								menuItems(
										App.APP.widgetManager.getFactories().filter(f -> f.hasFeature(SongReader.class)),
										WidgetFactory::nameGui,
										f -> App.APP.widgetManager.use(f.nameGui(), NO_LAYOUT, c -> ((SongReader) c.getController()).read(mg.getGrouped()))
								)
						),
						new Menu("Edit tags in", null,
								menuItems(
										App.APP.widgetManager.getFactories().filter(f -> f.hasFeature(SongWriter.class)),
										WidgetFactory::nameGui,
										f -> App.APP.widgetManager.use(f.nameGui(), NO_LAYOUT, c -> ((SongWriter) c.getController()).read(mg.getGrouped()))
								)
						),
						menuItem("Explore items's directory", () -> Environment.browse(mg.getGrouped().stream().filter(Item::isFileBased).map(Item::getFile))),
						new Menu("Explore items' directory in", null,
								menuItems(
										App.APP.widgetManager.getFactories().filter(f -> f.hasFeature(FileExplorerFeature.class)),
										WidgetFactory::nameGui,
										f -> App.APP.widgetManager.use(f.nameGui(), NO_LAYOUT, c -> ((FileExplorerFeature) c.getController()).exploreFile(mg.getGrouped().get(0).getFile()))
								)
						),
						mg.getField()!=Field.ALBUM ? null : new Menu("Search cover in", null,
								menuItems(
										App.APP.instances.getInstances(SearchUriBuilder.class).stream(),
										q -> "in " + Parser.DEFAULT.toS(q),
										q -> Environment.browse(q.apply(mg.getValueS("<none>")))
								)
						)
				).filter(Objects::nonNull)
		);
		CONTEXT_MENUS.add(
				PlaylistItemGroup.class,
				(contextMenu, pig) -> Stream.of(
						menuItem("Play items", () -> PlaylistManager.use(p -> p.playItem(pig.items.get(0)))),
						menuItem("Remove items", () -> PlaylistManager.use(p -> p.removeAll(pig.items))),
						new Menu("Show in", null,
								menuItems(
										App.APP.widgetManager.getFactories().filter(f -> f.hasFeature(SongReader.class)),
										WidgetFactory::nameGui,
										f -> App.APP.widgetManager.use(f.nameGui(), NO_LAYOUT, c -> ((SongReader) c.getController()).read(pig.items))
								)
						),
						new Menu("Edit tags in", null,
								menuItems(
										App.APP.widgetManager.getFactories().filter(f -> f.hasFeature(SongWriter.class)),
										WidgetFactory::nameGui,
										f -> App.APP.widgetManager.use(f.nameGui(), NO_LAYOUT, c -> ((SongWriter) c.getController()).read(pig.items))
								)
						),
						menuItem("Crop items", () -> PlaylistManager.use(p -> p.retainAll(pig.items))),
						menuItem("Duplicate items as group", () -> PlaylistManager.use(p -> p.duplicateItemsAsGroup(pig.items))),
						menuItem("Duplicate items individually", () ->
								PlaylistManager.use(p -> p.duplicateItemsByOne(pig.items))
						),
						menuItem("Explore items's directory", () ->
								Environment.browse(pig.items.stream().filter(Item::isFileBased).map(Item::getFile))
						),
						menuItem("Add items to library", () ->
								APP.db.addItems(map(pig.items, Item::toMeta))
						),
						new Menu("Search album cover", null,
								menuItems(
										App.APP.instances.getInstances(SearchUriBuilder.class).stream(),
										q -> "in " + Parser.DEFAULT.toS(q),
										q -> APP.actions.itemToMeta(pig.items.get(0), i -> Environment.browse(q.apply(i.getAlbumOrEmpty())))
								)
						)
				)
		);
		CONTEXT_MENUS.add(
				Thumbnail.ContextMenuData.class,
				(contextMenu, cmd) -> Stream.<MenuItem>of(
						cmd.image==null ? null : new Menu("Image", null,
								menuItem("Save the image as ...", () ->
										Environment.saveFile(
												"Save image as...",
												App.APP.DIR_APP,
												cmd.iFile==null ? "new_image" : cmd.iFile.getName(),
												contextMenu.getOwnerWindow(),
												ImageFileFormat.filter()
										)
												.ifOk(file -> Util.writeImage(cmd.image, file))
								),
								menuItem("Copy to clipboard", () -> copyToSysClipboard(DataFormat.IMAGE, cmd.image))
						),
						cmd.fsDisabled ? null : new Menu("Image file", null,
								menuItem("Browse location", () -> Environment.browse(cmd.fsImageFile)),
								menuItem("Open (in associated program)", () -> Environment.open(cmd.fsImageFile)),
								menuItem("Edit (in associated editor)", () -> Environment.edit(cmd.fsImageFile)),
								menuItem("Delete from disc", () -> recycleFile(cmd.fsImageFile)),
								menuItem("Fullscreen", () -> {
									File f = cmd.fsImageFile;
									if (ImageFileFormat.isSupported(f)) {
										Screen screen = getScreen(contextMenu.getX(), contextMenu.getY());
										APP.actions.openImageFullscreen(f, screen);
									}
								})
						),
						cmd.representant==null ? null : menuOfItemsFor(contextMenu, cmd.representant)
				).filter(Objects::nonNull)
		);
	}

	private static Menu menuOfItemsFor(ImprovedContextMenu<?> contextMenu, Object value) {
		String menuName = App.APP.className.get(value==null ? Void.class : value.getClass());
		return menuOfItemsFor(contextMenu, menuName, value);
	}

	private static Menu menuOfItemsFor(ImprovedContextMenu<?> contextMenu, String menuName, Object value) {
		MenuItem[] menuItems = CONTEXT_MENUS.get(contextMenu, value).toArray(MenuItem[]::new);
		return new Menu(menuName, null, menuItems);
	}

	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	public static class ContextMenuItemSuppliers {
		private final ClassListMap<Ƒ2<ImprovedContextMenu<?>,Object,Stream<MenuItem>>> m = new ClassListMap<>(o -> {
			throw new RuntimeException();
		});

		@SuppressWarnings("unchecked")
		public <T> void add(Class<T> type, Ƒ2<ImprovedContextMenu<?>,T,Stream<MenuItem>> items) {
			m.accumulate(type, (Ƒ2) items);
		}

		public Stream<MenuItem> get(ImprovedContextMenu<?> contextMenu, Object value) {
			return m.getElementsOfSuperV(value==null ? Void.class : value.getClass()).stream()
					.map(supplier -> supplier.apply(contextMenu, value))
					.flatMap(stream -> stream);
		}

	}
}