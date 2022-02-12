package converter;

import de.jensd.fx.glyphs.octicons.OctIcon;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import kotlin.Unit;
import kotlin.reflect.KClass;
import kotlin.sequences.SequencesKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sp.it.pl.audio.Song;
import sp.it.pl.audio.tagging.Metadata;
import sp.it.pl.audio.tagging.SongReadingKt;
import sp.it.pl.layout.Widget;
import sp.it.pl.layout.controller.LegacyController;
import sp.it.pl.layout.controller.SimpleController;
import sp.it.pl.layout.controller.io.Input;
import sp.it.pl.layout.controller.io.Output;
import sp.it.pl.layout.feature.Opener;
import sp.it.pl.layout.feature.SongWriter;
import sp.it.pl.main.AppTexts;
import sp.it.pl.main.Css;
import sp.it.pl.main.Widgets;
import sp.it.pl.ui.itemnode.ChainValueNode.ListChainValueNode;
import sp.it.pl.ui.itemnode.ConfigEditor;
import sp.it.pl.ui.itemnode.ListAreaNode;
import sp.it.pl.ui.itemnode.ValueNode;
import sp.it.pl.ui.objects.SpitComboBox;
import sp.it.pl.ui.objects.icon.Icon;
import sp.it.pl.ui.pane.ConfigPane.Layout;
import sp.it.util.access.V;
import sp.it.util.collections.map.KClassListMap;
import sp.it.util.conf.Config;
import sp.it.util.conf.Constraint.PreserveOrder;
import sp.it.util.conf.Constraint.ValueSealedSet;
import sp.it.util.file.Util;
import sp.it.util.text.StringSplitParser.SplitData;
import sp.it.util.type.VType;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.LIST_ALT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.MINUS;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PLAY_CIRCLE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PLUS;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static javafx.collections.FXCollections.observableArrayList;
import static javafx.css.PseudoClass.getPseudoClass;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.geometry.Pos.TOP_LEFT;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.layout.Priority.ALWAYS;
import static kotlin.jvm.JvmClassMappingKt.getKotlinClass;
import static sp.it.pl.audio.tagging.SongWritingKt.writeNoRefresh;
import static sp.it.pl.main.AppDragKt.getAny;
import static sp.it.pl.main.AppDragKt.installDrag;
import static sp.it.pl.main.AppExtensionsKt.getEmScaled;
import static sp.it.pl.main.AppExtensionsKt.toUi;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.pl.main.AppProgressKt.withAppProgress;
import static sp.it.pl.main.WidgetTags.UTILITY;
import static sp.it.util.JavaLegacyKt.typeListOfAny;
import static sp.it.util.Util.filenamizeString;
import static sp.it.util.async.AsyncKt.IO;
import static sp.it.util.async.AsyncKt.runIO;
import static sp.it.util.async.future.Fut.fut;
import static sp.it.util.collections.UtilKt.materialize;
import static sp.it.util.dev.DebugKt.logger;
import static sp.it.util.dev.FailKt.failIf;
import static sp.it.util.file.UtilKt.writeTextTry;
import static sp.it.util.functional.Util.equalBy;
import static sp.it.util.functional.Util.filter;
import static sp.it.util.functional.Util.findFirstInt;
import static sp.it.util.functional.Util.forEachWithI;
import static sp.it.util.functional.Util.list;
import static sp.it.util.functional.Util.listRO;
import static sp.it.util.functional.Util.map;
import static sp.it.util.functional.Util.mapSlice;
import static sp.it.util.functional.Util.split;
import static sp.it.util.functional.Util.stream;
import static sp.it.util.functional.Util.streamBi;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.functional.UtilKt.runnable;
import static sp.it.util.reactive.UtilKt.attach;
import static sp.it.util.reactive.UtilKt.onChangeAndNow;
import static sp.it.util.reactive.UtilKt.sync;
import static sp.it.util.text.UtilKt.capitalLower;
import static sp.it.util.type.TypesKt.typeNothingNonNull;
import static sp.it.util.type.UtilKt.estimateRuntimeType;
import static sp.it.util.ui.Util.layHorizontally;
import static sp.it.util.ui.Util.layStack;
import static sp.it.util.ui.Util.layVertically;
import static sp.it.util.ui.UtilKt.label;
import static sp.it.util.ui.UtilKt.menuItem;
import static sp.it.util.ui.UtilKt.text;
import static sp.it.util.ui.UtilKt.textFlow;

@SuppressWarnings({"WeakerAccess", "MismatchedQueryAndUpdateOfCollection", "FieldCanBeLocal", "unused"})
@Widget.Info(
    author = "Martin Polakovic",
    name = Widgets.CONVERTER_NAME,
    description = "Transformation utility. Capable of transforming objects "
        + "and chaining the transforming functions. Capable of text manipulation, file renaming and "
        + "audio tagging.",
    howto = ""
        + "\tUser can put text in an edit area and apply transformations on it "
        + "using available functions. The transformations are applied on each "
        + "line separately. It is possible to manually edit the text to fine-tune "
        + "the result.\n"
        + "\tThis is useful to edit multiple texts in the same way, e.g., to "
        + "edit filenames or song names. This is done using an 'applier' that "
        + "uses the 'output' and applies it no an input.\n"
        + "\tInput is a list of objects and can be set by drag&drop. It can "
        + "then be transformed to text (object per line) or other objects "
        + "using available functions, e.g., song to artist or file to filename.\n"
        + "\tOutput is the final contents of the text area exactly as "
        + "visible. Some transformations can produce multiple outputs "
        + "(again, per line), which produces separate text area for each output. "
        + "To discern different outputs, text areas have a name in their header.\n"
        + "\tAction is determined by the type of input. User can select which "
        + "output he wants to use. The action then applies the text to the "
        + "input, each line to its respective object (determined by order). "
        + "Number of objects and lines must match.\n"
        + "\n"
        + "Available appliers:\n"
        + "\tSave text to file - produce a text file with specified content\n"
        + "\tRename files - renames input files\n"
        + "\tTag songs - edits tag of song individually\n"
        + "\n"
        + "Available actions:\n"
        + "\tDrag&drop files : Sets files as input\n"
        + "\tDrag&drop songs : Sets songs as input\n"
        + "\tDrag&drop text : Sets text as input\n",
    version = "0.8.0",
    year = "2015",
    tags = UTILITY
)
@LegacyController
public class Converter extends SimpleController implements Opener, SongWriter {

    public final Input<Object> inputValue;

    private final ObservableList<Object> source = observableArrayList();
    private final EditArea ta_in = new EditArea("In", true);
    private final ObservableList<EditArea> tas = observableArrayList(ta_in);
    private final KClassListMap<Act<?>> acts = new KClassListMap<>(act -> act.type);
    private final HBox outTFBox = new HBox(getEmScaled(10));
    private final Applier applier = new Applier();
    private final HBox layout = new HBox(getEmScaled(10), outTFBox, applier.root);

    public Converter(Widget widget) {
	    super(widget);
        root.setPrefSize(getEmScaled(800), getEmScaled(500));

        inputValue = io.i.create("Value", new VType<>(Object.class, true), null, consumer(v -> source.setAll(unpackData(v))));

        // layout
        HBox ll = new HBox(getEmScaled(10), ta_in.getNode(),layout);
        HBox.setHgrow(ta_in.getNode(), ALWAYS);
        root.getChildren().add(ll);

        // drag&drop
        installDrag(
            root, LIST_ALT, () -> "Set data as input",
            e -> true,
            e -> false,
            consumer(e -> inputValue.setValue(getAny(e.getDragboard()))),
            e -> ta_in.getNode().getLayoutBounds()
        );

        tas.addListener((Change<? extends EditArea> change) ->
            outTFBox.getChildren().setAll(map(tas,EditArea::getNode))
        );

        // on source change run transformation
        source.addListener((Change<?> change) -> ta_in.setInput(source));

        ta_in.onItemChange = lines -> {
            List<EditArea> l;
            List<EditArea> custom_tas = filter(tas,ta -> ta.name.get().contains("Custom"));
            if (!ta_in.output.isEmpty() && ta_in.output.get(0) instanceof SplitData) {
                List<SplitData> s = ta_in.output.stream().filter(SplitData.class::isInstance).map(SplitData.class::cast).toList();
                List<String> names = s.get(0).stream().map(split -> split.parse_key()).toList();

                List<List<String>> outs = list(names.size(), ArrayList::new);
                s.forEach(splits -> forEachWithI(map(splits,split -> split.split()), (i,line) -> outs.get(i).add(line)));

                List<EditArea> li = list(outs.size(), () -> new EditArea(""));
                forEachWithI(outs, (i, ls) -> li.get(i).setData(names.get(i),  ls));

                l = li;
                l.add(0, ta_in);
                l.addAll(custom_tas);
                tas.setAll(l);
            } else {
                l = custom_tas;
                l.add(0,ta_in);
                tas.setAll(l);
            }
        };

        // add actions
        // Note that the actions are looked up (per class) in the order they are inserted. Per each
        // class, the first added action will become 'default' and show up as selected when data is
        // set.
        // Therefore, the order below matters.
        acts.accumulate(
            new Act<>("Rename files", getKotlinClass(File.class), 1, list("Filename"), (file, data) -> {
                String name = data.get("Filename");
                Util.renameFileNoSuffix(file, name);
            })
        );
        acts.accumulate(
            new Act<>("Rename files (and extension)", getKotlinClass(File.class), 1, list("Filename"), (file, data) -> {
                String name = data.get("Filename");
                Util.renameFile(file, name);
            })
        );
        acts.accumulate(
            new Act<>("Edit song tags", getKotlinClass(Song.class), 100, map(Metadata.Field.Companion.getAll(), f -> f.name()), data -> {
                List<Song> songs = source.stream().filter(Song.class::isInstance).map(Song.class::cast).toList();
                if (songs.isEmpty()) return;
                failIf(data.values().stream().anyMatch(it -> it.size()!=songs.size()), () -> "Data size mismatch");

                withAppProgress(
                    runIO(() -> {
                        for (int i=0; i<songs.size(); i++) {
                            int j = i;
                            writeNoRefresh(
                                songs.get(i),
                                consumer(w ->
                                    data.forEach((field, values) ->
                                        w.setFieldS(Metadata.Field.Companion.valueOf(field), values.get(j))
                                    )
                                )
                            );
                        }

                        APP.audio.refreshSongsWith(stream(songs).map(SongReadingKt::read).filter(m -> !m.isEmpty()).toList());
                        return null;
                    }),
                    widget.getCustomName().getValue() + "Editing song tags"
                );
            }) {{
                isInputsFixedLength = false;
            }}
        );
        acts.accumulate(new WriteFileAct());
        acts.accumulate(new ActCreateDirs());

        Output<String> outputAsText = io.o.create("Output (as text)", new VType<>(String.class, true), "");
        Output<List<Object>> output = io.o.create("Output", typeListOfAny(), List.of());
        ta_in.outputText.addListener((o, ov, nv) -> outputAsText.setValue(nv));
        ta_in.output.addListener((Change<?> o) -> output.setValue(materialize(ta_in.output)));

        // set empty content
        applier.fillActs(typeNothingNonNull());
    }

    @Override
    public void read(@Nullable Song song) {
        SongWriter.super.read(song);
    }

    @Override
    public void read(@NotNull List<? extends Song> songs) {
        inputValue.setValue(map(songs, Song::toMeta));
    }

    @Override
    public void open(Object data) {
        inputValue.setValue(data);
    }

    private static List<?> unpackData(Object o) {
        if (o instanceof String os)
            return split(os, "\n", x->x);
        else if (o instanceof Collection<?> oc)
            return list(oc);
        else return listRO(o);
    }

/******************************* helper classes *******************************/

    /* Generates unique name in format 'CustomN', where N is integer. */
    String taName() {
        return "Custom" + findFirstInt(1, i -> tas.stream().noneMatch(ta -> ta.name.get().equals("Custom"+i)));
    }

    private class EditArea extends ListAreaNode {
        public final StringProperty name;
        public final boolean isMain;

        EditArea() {
            this(taName());
        }

        EditArea(String name) {
            this(name, false);
        }

        EditArea(String name, boolean isMain) {
            super();
            this.isMain = isMain;
            this.transforms.setHeaderVisible(true);
            textArea.setPrefColumnCount(80);

            // graphics
            var nameL = new Label(name);

            var typeL = new Label("");
            var typeLUpdater = runnable(() -> {
                var text = input.size() + "x " + toUi(estimateRuntimeType(input)) + " → " + output.size() + "x " + toUi(estimateRuntimeType(output));
                typeL.setText(text);
            });
            onChangeAndNow(input, typeLUpdater);
            onChangeAndNow(output, typeLUpdater);

            var caretL = new Label("Caret: 0:0:0");
            sync(textArea.focusedProperty(), consumer(caretL::setVisible));
            attach(textArea.caretPositionProperty(), consumer(i -> {
                var lastCharAt = new AtomicInteger(0);
                var lastNewlineAt = new AtomicInteger(0);
                var xy = i.longValue() + 1;
                var y = 1 + textArea.getText().codePoints()
                    .limit(i.longValue())
                    .peek(c -> lastCharAt.incrementAndGet())
                    .filter(c -> c == '\n' || c == '\r')
                    .peek(c -> lastNewlineAt.set(lastCharAt.get()))
                    .count();
                var x = xy - lastNewlineAt.get();
                caretL.setText("Pos: " + xy + ":" + x + ":" + y);
            }));

            var applyI = new Icon(OctIcon.DATABASE)
                    .tooltip(
                        "Set input\n\nSet input for this area. The actual input, its transformation and the output will be discarded."
                        + (isMain ? "\n\nThis edit area is main, so the new input data will update the available actions." : "")
                    )
                    .action(THIS ->
                        new ContextMenu(
                            menuItem("Set input data to empty" , null, consumer(it -> setInput(list(Unit.INSTANCE)))),
                            menuItem("Set input data to output data", null, consumer(it -> setInput(output))),
                            menuItem("Set input data to lines of visible text", null, consumer(it -> setInput(Arrays.asList(getValAsText().split("\\n"))))),
                            menuItem("Set input data to system clipboard", null, consumer(it -> setInput(list(getAny(Clipboard.getSystemClipboard())))))
                        ).show(THIS, Side.BOTTOM, 0, 0)
                    );
            var remI = new Icon(MINUS)
                    .tooltip("Remove\n\nRemove this edit area.")
                    .action(() -> tas.remove(this));
            var addI = new Icon(PLUS)
                    .tooltip("Add\n\nCreate new edit area")
                    .action(THIS ->
                        new ContextMenu(
                            menuItem("With no data", null, consumer(it -> createNewAreaWithNoData())),
                            menuItem("With input of this area", null, consumer(it -> createNewAreaWithInputData())),
                            menuItem("With output of this area", null, consumer(it -> createNewAreaWithOutputNoData()))
                        ).show(THIS, Side.BOTTOM, 0, 0)
                    );

            // layout
            getNode().getChildren().add(0,
                layVertically(5,Pos.CENTER,
                    layStack(
                        nameL,Pos.CENTER,
                        layHorizontally(5,Pos.CENTER_RIGHT, applyI,new Label(),remI,addI),Pos.CENTER_RIGHT
                    ),
                    layStack(
                        typeL, CENTER_LEFT,
                        caretL,Pos.CENTER_RIGHT
                    )
                )
            );

            this.name = nameL.textProperty();
            remI.setDisable(isMain);   // disallow removing main edit area

            // drag & drop (main area is handled globally)
            if (!isMain) {
                installDrag(
                    getNode(), OctIcon.DATABASE, () -> "Set data to " + this.name.get() + " edit area",
                    e -> true,
                    consumer(e -> setInput(unpackData(getAny(e.getDragboard()))))
                );
            }

            textArea.addEventFilter(KEY_PRESSED, e -> {
                if (e.getCode()==KeyCode.V && e.isShortcutDown()) {
                    var pasted_text = Clipboard.getSystemClipboard().getString();
                    if (pasted_text!=null) {
                        var areaLines = textArea.getText().split("\\n");
                        var pastedLines = pasted_text.split("\\n");
                        if (areaLines.length==pastedLines.length) {
                            var text = streamBi(areaLines, pastedLines, (a,p) -> a+p).collect(joining("\n"));
                            textArea.setText(text);
                            e.consume();
                        } else {
                            if (textArea.getText().isEmpty() && transforms.length()<=1) {
                                setInput(unpackData(pasted_text));
                            }
                        }
                    }
                }
            });

            setData(name, list(Unit.INSTANCE));
        }

        // Weird reasons for needing this method, just call it bad design. Not worth 'fixing'.
        public void setData(String name, List<?> input) {
            this.name.set(capitalLower(name));
            setInput(input);
        }

        @Override
        public void setInput(@NotNull List<?> input) {
            super.setInput(input);
            fillActionData();
        }

        public void fillActionData(){
            if (isMain && applier!=null) {
                applier.fillActs(transforms.getTypeIn());
            }
        }

        @Override
        public String toString() {
            return name.get();
        }

        public void createNewAreaWithNoData() {
            tas.add(tas.indexOf(this)+1, new EditArea());
        }

        public void createNewAreaWithInputData() {
            EditArea t = new EditArea();
            t.setInput(input);
            tas.add(tas.indexOf(this)+1,t);
        }

        public void createNewAreaWithOutputNoData() {
            EditArea t = new EditArea();
            t.setInput(output);
            tas.add(tas.indexOf(this)+1,t);
        }
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "ConstantConditions"})
    private class Applier {
        private final SpitComboBox<Act<?>> actCB = new SpitComboBox<>(act -> act.name, AppTexts.textNoVal);
        Ins ins;
        BiConsumer<File,String> applier = (f, s) -> {
            File rf = f.getParentFile().getAbsoluteFile();
            int dot = f.getPath().lastIndexOf('.');
            String p = f.getPath();
            String ext = p.substring(dot);
            f.renameTo(new File(rf, filenamizeString(s)+ext));
        };
        @SuppressWarnings("unchecked")
        private final Icon runB = new Icon(PLAY_CIRCLE, 20, "Apply", () -> {
            var action = (Act<Object>) actCB.getValue();
            if (action==null) return;

            if (action.isPartial()) {
                boolean empty = source.isEmpty() || ins.values().findAny().isEmpty();
                if (empty) return;

                boolean in_out_type_match = true;   //action.type.isInstance(source.get(0));    // TODO: implement properly
                boolean same_size = equalBy(tas, ta -> ta.getVal().size());
                if (!in_out_type_match || !same_size) return;

                Map<String,List<? extends String>> m = ins.values().collect(toMap(in -> in.name, in -> in.ta.getVal()));
                for (int i=0; i<source.size(); i++)
                    action.actionPartial.accept(source.get(i), mapSlice(m,i));
            } else {
                Map<String,List<? extends String>> m = ins.values().collect(toMap(in -> in.name, in -> in.ta.getVal()));
                action.actionImpartial.accept(m);
            }
        });
        VBox root = new VBox(getEmScaled(10),
            label("Action", consumer((it) ->
                it.getStyleClass().add("form-config-pane-config-name")
            )),
            textFlow(consumer(it -> {
                it.getStyleClass().add(Css.DESCRIPTION);
                it.getStyleClass().add("form-config-pane-config-description");
                it.getChildren().add(text("Action that uses the data in text area", consumer(itt -> {})));
            })),
            actCB,
            label("Data", consumer((it) ->
                it.getStyleClass().add("form-config-pane-config-name")
            )),
            textFlow(consumer(it -> {
                it.getStyleClass().add(Css.DESCRIPTION);
                it.getStyleClass().add("form-config-pane-config-description");
                it.getChildren().add(text("Map text areas to the action input.\nAction can have multiple inputs", consumer(itt -> {})));
            })),
            runB.withText(Side.RIGHT, "Apply")
        );

        public Applier() {
            root.getStyleClass().add("form-config-pane");
            root.pseudoClassStateChanged(getPseudoClass(Layout.EXTENSIVE.name().toLowerCase()), true);
            actCB.valueProperty().addListener((o,ov,nv) -> {
                if (nv==null) return;
                if (ins!=null) root.getChildren().remove(ins.node());
                if (root.getChildren().size()==7) root.getChildren().remove(5);
                ins = new Ins(nv, nv.isInputsFixedLength);
                root.getChildren().add(5,ins.node());
                Node n = nv.getNode();
                if (n!=null) root.getChildren().add(6,n);
            });
        }

        public void fillActs(VType<?> c) {
            List<Act<?>> l = SequencesKt.toList(acts.getElementsOfSuperV((KClass<?>) c.getType().getClassifier()));
            actCB.getItems().setAll(l);
            if (!l.isEmpty()) actCB.setValue(l.get(0));
        }
    }
    private static class Act<Y> {
        String name;
        int max;
        List<String> names;
        KClass<Y> type;
        boolean isInputsFixedLength;
        BiConsumer<Y,Map<String,? extends String>> actionPartial = null;
        Consumer<? super Map<String,List<? extends String>>> actionImpartial = null;

        private Act(String name, KClass<Y> type, int max, BiConsumer<Y,Map<String,? extends String>> action) {
            this.name = name;
            this.type = type;
            this.max = max;
            this.actionPartial = action;
            this.isInputsFixedLength = false;
        }
        private Act(String name, KClass<Y> type, int max, Consumer<Map<String,List<? extends String>>> action) {
            this.name = name;
            this.type = type;
            this.max = max;
            this.actionImpartial = action;
            this.isInputsFixedLength = false;
        }
        Act(String name, KClass<Y> type, int max, List<String> names, BiConsumer<Y,Map<String,? extends String>> action) {
            this(name, type, max, action);
            this.names = names;
            this.isInputsFixedLength = true;
        }
        Act(String name, KClass<Y> type, int max, List<String> names, Consumer<Map<String,List<? extends String>>> action) {
            this(name, type, max, action);
            this.names = names;
            this.isInputsFixedLength = true;
        }

        Node getNode() {
            return null;
        }

        public boolean isPartial() {
            return actionPartial!=null;
        }
    }
    private static class WriteFileAct extends Act<Void> {
        V<String> nam = new V<>("new_file.txt");
        V<File> loc = new V<>(APP.getLocation());

        public WriteFileAct() {
            super("Write file", getKotlinClass(Void.class), 1, list("Contents"), (Consumer<Map<String,List<? extends String>>>) null);
            actionImpartial = data -> {
                var file = new File(loc.get(), nam.get());
                var text = String.join("\n", data.get("Contents"));
                writeTextTry(file, text);
            };
        }

        @Override
        public Node getNode() {
            var node = layVertically(getEmScaled(10),TOP_LEFT,
                label("Output file name", consumer((it) ->
                    it.getStyleClass().add("form-config-pane-config-name")
                )),
                textFlow(consumer(it -> {
                    it.getStyleClass().add(Css.DESCRIPTION);
                    it.getStyleClass().add("form-config-pane-config-description");
                    it.getChildren().add(text("Output file name. Previous file will be overwritten.", consumer(itt -> {})));
                })),
                ConfigEditor.create(Config.forProperty(String.class, "File name", nam)).buildNode(),
                label("Output location", consumer((it) ->
                    it.getStyleClass().add("form-config-pane-config-name")
                )),
                textFlow(consumer(it -> {
                    it.getStyleClass().add(Css.DESCRIPTION);
                    it.getStyleClass().add("form-config-pane-config-description");
                    it.getChildren().add(text("Location the file will be saved to", consumer(itt -> {})));
                })),
                ConfigEditor.create(Config.forProperty(File.class, "Location", loc)).buildNode()
            );
            node.getStyleClass().add("form-config-pane");
            return node;
        }
    }
    private static class ActCreateDirs extends Act<Void> {
        V<Boolean> use_loc = new V<>(false);
        V<File> loc = new V<>(APP.getLocationHome());

        public ActCreateDirs() {
            super("Create directories", getKotlinClass(Void.class), 1, list("Names (Paths)"), (Consumer<Map<String,List<? extends String>>>) null);
            actionImpartial = data ->
                fut(data.get("Names (Paths)"))
                   .useBy(IO, names -> {
                       File dir = loc.get();
                       names.forEach((String name) -> {
                           try {
                               File newFile;
                               if (use_loc.get()) {
                                   name = name.startsWith(File.separator) ? name.substring(1) : name;
                                   newFile = new File(dir, filenamizeString(name));
                               } else {
                                   newFile = new File(name);
                               }
                               Files.createDirectories(newFile.toPath());
                           } catch (IOException e) {
                               logger(Converter.class).info("Couldn't create file/directory",e);
                           }
                       });
                   });
        }

        @Override
        public Node getNode() {
            Node n = ConfigEditor.create(Config.forProperty(File.class, "Location", loc)).buildNode();
            use_loc.syncC(v -> n.setDisable(!v));
            var node = layVertically(getEmScaled(10),TOP_LEFT,
                label("Relative", consumer((it) ->
                    it.getStyleClass().add("form-config-pane-config-name")
                )),
                text("If relative, the path will be resolved against the below directory", consumer(it -> {
                    it.getStyleClass().add(Css.DESCRIPTION);
                    it.getStyleClass().add("form-config-pane-config-description");
                })),
                ConfigEditor.create(Config.forProperty(Boolean.class, "In directory", use_loc)).buildNode(),
                label("Relative against", consumer((it) ->
                    it.getStyleClass().add("form-config-pane-config-name")
                )),
                text("Location the directories will be created in", consumer(it -> {
                    it.getStyleClass().add(Css.DESCRIPTION);
                    it.getStyleClass().add("form-config-pane-config-description");
                })),
                n
            );
            node.getStyleClass().add("form-config-pane");
            return node;
        }
    }

    private static class In {
        public String name;
        public EditArea ta;

        In(String name, EditArea ta) {
            this.name = name;
            this.ta = ta;
        }
    }
    private class InPane extends ValueNode<In> {
        final V<String> name;
        final V<EditArea> input;
        final ConfigEditor<String> configEditorA;
        final ConfigEditor<EditArea> configEditorB;
        final HBox root;

        @SuppressWarnings("unchecked")
        InPane(Collection<? extends String> actions) {
            super(null);
            name = new V<>(actions.stream().findFirst().orElse(null));
            input = new V<>(stream(tas).filter(ta -> ta.name.get().equalsIgnoreCase("out")).findAny().orElse(ta_in));
            configEditorA = ConfigEditor.create(Config.forProperty(String.class, "", name).addConstraints(new ValueSealedSet<>(() -> actions), PreserveOrder.INSTANCE));
            configEditorB = ConfigEditor.create(Config.forProperty(EditArea.class, "", input).addConstraints(new ValueSealedSet<>(() -> tas), PreserveOrder.INSTANCE));
            root = layHorizontally(getEmScaled(10), CENTER_LEFT, configEditorA.buildNode(), new Label("←"), configEditorB.buildNode());
        }

        @Override
        public In getVal() {
            return new In(configEditorA.getConfig().getValue(), configEditorB.getConfig().getValue());
        }

        @Override
        public @NotNull Node getNode() {
            return root;
        }
    }
    private class Ins {
        ListChainValueNode<In, InPane> ins;

        Ins(Act<?> a, boolean isFixedLength) {
            ins = new ListChainValueNode<>(i -> new InPane(a.names));
            ins.editable.setValue(!isFixedLength);
            ins.growTo1();
            ins.maxChainLength.set(a.max);
        }

        public VBox node() {
            return ins.getNode();
        }

        public Stream<In> values() {
            return ins.getValues();
        }

    }
}