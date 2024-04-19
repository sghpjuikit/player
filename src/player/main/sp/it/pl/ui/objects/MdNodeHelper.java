
/*
   Copied and adapted from `com.sandec.mdfx.impl.MDFXNodeHelper` from JPro-one/markdown-javafx-renderer (https://github.com/JPro-one/markdown-javafx-renderer)

   Copyright 2018 JPro-one (https://www.jpro.one) SANDEC GmbH
   Copyright 2021 spit

	   Licensed under the Apache License, Version 2.0 (the "License");
	   you may not use this file except in compliance with the License.
	   You may obtain a copy of the License at

	   http://www.apache.org/licenses/LICENSE-2.0

	   Unless required by applicable law or agreed to in writing, software
	   distributed under the License is distributed on an "AS IS" BASIS,
	   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	   See the License for the specific language governing permissions and
	   limitations under the License.
*/

package sp.it.pl.ui.objects;

import com.vladsch.flexmark.ast.AutoLink;
import com.vladsch.flexmark.ast.BlockQuote;
import com.vladsch.flexmark.ast.BulletList;
import com.vladsch.flexmark.ast.BulletListItem;
import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.Emphasis;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.HardLineBreak;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ast.LinkNodeBase;
import com.vladsch.flexmark.ast.ListItem;
import com.vladsch.flexmark.ast.OrderedList;
import com.vladsch.flexmark.ast.OrderedListItem;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.SoftLineBreak;
import com.vladsch.flexmark.ast.StrongEmphasis;
import com.vladsch.flexmark.ast.TextBase;
import com.vladsch.flexmark.ext.attributes.AttributeNode;
import com.vladsch.flexmark.ext.attributes.AttributesExtension;
import com.vladsch.flexmark.ext.attributes.AttributesNode;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.Strikethrough;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListItem;
import com.vladsch.flexmark.ext.tables.TableBody;
import com.vladsch.flexmark.ext.tables.TableCell;
import com.vladsch.flexmark.ext.tables.TableHead;
import com.vladsch.flexmark.ext.tables.TableRow;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Block;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;
import com.vladsch.flexmark.util.misc.Extension;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Pair;
import static javafx.scene.input.DataFormat.PLAIN_TEXT;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static sp.it.pl.ui.objects.MdNodeHelperKt.toNode;
import static sp.it.util.async.AsyncKt.FX;
import static sp.it.util.async.AsyncKt.runIO;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.reactive.EventsKt.onEventDown;

public class MdNodeHelper extends VBox {

	final static String ITALICS_CLASS_NAME = "markdown-italic";
	final static String BOLD_CLASS_NAME = "markdown-bold";
	final static String STRIKETHROUGH_CLASS_NAME = "markdown-strikethrough";

	MdNodeContent parent;

	List<String> elemStyleClass = new LinkedList<>();

	List<Consumer<Pair<Node,String>>> elemFunctions = new LinkedList<>();

	Boolean nodePerWord = false;

	List<String> styles = new LinkedList<>();

	VBox root = new VBox();

	GridPane grid = null;
	int gridX = 0;
	int gridY = 0;
	TextFlow flow = null;

	boolean isListOrdered = false;
	int orderedListCounter = 0;

	int[] currentChapter = new int[6];

	public boolean shouldShowContent() {
		return parent.showChapter(currentChapter);
	}

	public void newParagraph() {
		TextFlow newFlow = new TextFlow();
		newFlow.getStyleClass().add("markdown-normal-flow");
		root.getChildren().add(newFlow);
		flow = newFlow;
	}
	private ThreadLocal<Parser> parser = ThreadLocal.withInitial(() -> {
		LinkedList<Extension> extensions = new LinkedList<>();
		extensions.add(AutolinkExtension.create());
		extensions.add(TablesExtension.create());
		extensions.add(AttributesExtension.create());
		extensions.add(StrikethroughExtension.create());
		extensions.add(TaskListExtension.create());
		return Parser.builder().extensions(extensions).build();
	});
	public MdNodeHelper(MdNodeContent parent, String mdString) {
		this.parent = parent;

		root.getStyleClass().add("markdown-paragraph-list");
		root.setFillWidth(true);

		runIO(() -> parser.get().parse(mdString)).then(FX, node -> {
			new MdNodeHelper.MDParser(node).visitor.visitChildren(node);
			this.getChildren().add(root);
			return null;
		});
	}


	class MDParser {

		Document document;

		MDParser(Document document) {
			this.document = document;
		}

		NodeVisitor visitor = new NodeVisitor(
			//new VisitHandler<>(com.vladsch.flexmark.ast.Node.class, this::visit),
			new VisitHandler<>(Code.class, this::visit),
			new VisitHandler<>(BlockQuote.class, this::visit),
			//new VisitHandler<>(Quotes.class, this::visit),
			new VisitHandler<>(Block.class, this::visit),
			new VisitHandler<>(Document.class, this::visit),
			new VisitHandler<>(Emphasis.class, this::visit),
			new VisitHandler<>(StrongEmphasis.class, this::visit),
			new VisitHandler<>(FencedCodeBlock.class, this::visit),
			new VisitHandler<>(SoftLineBreak.class, this::visit),
			new VisitHandler<>(HardLineBreak.class, this::visit),
			new VisitHandler<>(Heading.class, this::visit),
			new VisitHandler<>(ListItem.class, this::visit),
			new VisitHandler<>(BulletListItem.class, this::visit),
			new VisitHandler<>(OrderedListItem.class, this::visit),
			new VisitHandler<>(TaskListItem.class, this::visit),
			new VisitHandler<>(BulletList.class, this::visit),
			new VisitHandler<>(OrderedList.class, this::visit),
			new VisitHandler<>(Paragraph.class, this::visit),
			new VisitHandler<>(com.vladsch.flexmark.ast.Image.class, this::visit),
			new VisitHandler<>(Link.class, this::visit),
			new VisitHandler<>(AutoLink.class, this::visit),
			new VisitHandler<>(LinkNodeBase.class, this::visit),
			new VisitHandler<>(TextBase.class, this::visit),
			new VisitHandler<>(com.vladsch.flexmark.ast.Text.class, this::visit),
			new VisitHandler<>(com.vladsch.flexmark.ext.gfm.strikethrough.Strikethrough.class, this::visit),
			new VisitHandler<>(TableHead.class, this::visit),
			new VisitHandler<>(TableBody.class, this::visit),
			new VisitHandler<>(TableRow.class, this::visit),
			new VisitHandler<>(TableCell.class, this::visit)
		);

		public void visit(Code code) {

			var text = code.getText().normalizeEOL();
			Label label = new Label(text);
			label.getStyleClass().add("markdown-code");

			Region bgr1 = new Region();
			bgr1.setManaged(false);
			bgr1.getStyleClass().add("markdown-code-background");
			label.boundsInParentProperty().addListener((p, oldV, newV) -> {
				bgr1.setTranslateX(newV.getMinX() + 2);
				bgr1.setTranslateY(newV.getMinY() - 2);
				bgr1.resize(newV.getWidth() - 4, newV.getHeight() + 4);
			});
			onEventDown(label, MOUSE_CLICKED, PRIMARY, true, consumer(it -> Clipboard.getSystemClipboard().setContent(Map.of(PLAIN_TEXT, text))));

			flow.getChildren().add(bgr1);
			flow.getChildren().add(label);

//			visitor.visitChildren(code);
		}

		public void visit(BlockQuote customBlock) {
			VBox oldRoot = root;
			root = new VBox();
			root.getStyleClass().add("markdown-normal-block-quote");
			oldRoot.getChildren().add(root);

			visitor.visitChildren(customBlock);

			root = oldRoot;
			newParagraph();
		}

		public void visit(Block customBlock) {
			flow.getChildren().add(new Text("\n\n"));
			visitor.visitChildren(customBlock);
		}


		public void visit(Document document) {
			visitor.visitChildren(document);
		}

		public void visit(Emphasis emphasis) {
			elemStyleClass.add(ITALICS_CLASS_NAME);
			visitor.visitChildren(emphasis);
			elemStyleClass.remove(ITALICS_CLASS_NAME);
		}

		public void visit(StrongEmphasis strongEmphasis) {
			elemStyleClass.add(BOLD_CLASS_NAME);
			visitor.visitChildren(strongEmphasis);
			elemStyleClass.remove(BOLD_CLASS_NAME);
		}

		public void visit(Strikethrough strikethrough) {
			elemStyleClass.add(STRIKETHROUGH_CLASS_NAME);
			visitor.visitChildren(strikethrough);
			elemStyleClass.remove(STRIKETHROUGH_CLASS_NAME);
		}


		public void visit(FencedCodeBlock fencedCodeBlock) {
			if(!shouldShowContent()) return;

			root.getChildren().add(toNode(fencedCodeBlock));
		}

		public void visit(SoftLineBreak softLineBreak) {
			//flow <++ new Text("\n")
			addText(" ", "");
			visitor.visitChildren(softLineBreak);
		}

		public void visit(HardLineBreak hardLineBreak) {
			flow.getChildren().add(new Text("\n"));
			visitor.visitChildren(hardLineBreak);
		}


		public void visit(Heading heading) {

			if (heading.getLevel() == 1 || heading.getLevel() == 2) {
				currentChapter[heading.getLevel()] += 1;

				for(int i=heading.getLevel() + 1; i <= currentChapter.length - 1; i+=1) {
					currentChapter[i] = 0;
				}
			}

			if (shouldShowContent()) {
				newParagraph();

				flow.getStyleClass().add("markdown-heading");
				flow.getStyleClass().add("markdown-heading-" + heading.getLevel());
				flow.addEventHandler(MOUSE_CLICKED, e -> {
					if (e.getButton() == PRIMARY) {
						parent.scrollToAnchor(heading.getAnchorRefText().replace(" ", "-"));
						e.consume();
					}
				});

				visitor.visitChildren(heading);
			}
		}

		public void visitListItem(String text, com.vladsch.flexmark.util.ast.Node node) {
			VBox oldRoot = root;

			VBox newRoot = new VBox();
			newRoot.getStyleClass().add("markdown-paragraph-list");
			newRoot.setFillWidth(true);

			orderedListCounter += 1;

			Label dot = new Label(text);
			dot.getStyleClass().add("markdown-list-item-dot");
			dot.getStyleClass().add("markdown-text");

			HBox hbox = new HBox();
			hbox.getChildren().add(dot);
			hbox.setAlignment(Pos.TOP_LEFT);
			HBox.setHgrow(newRoot,Priority.ALWAYS);
			newRoot.setPrefWidth(1.0); // This way, it doesn't take space from the "dot" label
			hbox.getChildren().add(newRoot);

			oldRoot.getChildren().add(hbox);

			root = newRoot;

			visitor.visitChildren(node);
			root = oldRoot;
		}

		public void visit(TaskListItem listItem) {
			if(!shouldShowContent()) return;
			String text = listItem.isItemDoneMarker() ? "☑" : "☐";
			visitListItem(text,listItem);
		}

		public void visit(ListItem listItem) {
			if(!shouldShowContent()) return;

			String text = isListOrdered ? (" " + (orderedListCounter+1) + ". ") : " • ";

			visitListItem(text,listItem);
		}

		public void visit(BulletList bulletList) {

			if(!shouldShowContent()) return;
			isListOrdered = false;
			VBox oldRoot = root;
			root = new VBox();
			oldRoot.getChildren().add(root);
			newParagraph();
			flow.getStyleClass().add("markdown-normal-flow");
			visitor.visitChildren(bulletList);
			root = oldRoot;
		}

		public void visit(OrderedList orderedList) {
			int previousCounter = orderedListCounter;
			orderedListCounter = 0;
			isListOrdered = true;
			VBox oldRoot = root;
			root = new VBox();
			oldRoot.getChildren().add(root);
			newParagraph();
			flow.getStyleClass().add("markdown-normal-flow");
			visitor.visitChildren(orderedList);
			orderedListCounter = previousCounter;
			root = oldRoot;
		}

		public void visit(Paragraph paragraph) {
			if(!shouldShowContent()) return;

			List<AttributesNode> attrs = AttributesExtension.NODE_ATTRIBUTES.get(document).get(paragraph);
			newParagraph();
			flow.getStyleClass().add("markdown-normal-flow");
			setAttrs(attrs,true);
			visitor.visitChildren(paragraph);
			setAttrs(attrs,false);
		}

		public void visit(com.vladsch.flexmark.ast.Image image) {
			String url = image.getUrl().toString();
			//System.out.println("imgUrl: " + image.getUrl());
			//System.out.println("img.getUrlContent: " + image.getUrlContent());
			//System.out.println("img.nodeName: " + image.getNodeName());
			Node node = parent.generateImage(url);
			addFeatures(node,"");
			flow.getChildren().add(node);
			//visitor.visitChildren(image);
		}

		public void visit(LinkNodeBase link) {
			LinkedList<Node> nodes = new LinkedList<>();

			Consumer<Pair<Node, String>> addProp = (pair) -> {
				Node node = pair.getKey();
				String txt = pair.getValue();
				nodes.add(node);

				node.getStyleClass().add("markdown-link");
				parent.setLink(node, link.getUrl().normalizeEndWithEOL(), txt);
			};
			Platform.runLater(() -> {
				BooleanProperty lastValue = new SimpleBooleanProperty(false);
				Runnable updateState = () -> {
					boolean isHover = false;
					for(Node node: nodes) {
						if(node.isHover()) {
							isHover = true;
						}
					}
					if (isHover != lastValue.get()) {
						lastValue.set(isHover);
						for(Node node: nodes) {
							if (isHover) {
								node.getStyleClass().add("markdown-link-hover");
							} else {
								node.getStyleClass().remove("markdown-link-hover");
							}

						}
					}

				};

				for(Node node: nodes) {
					node.hoverProperty().addListener((p, o, n) -> updateState.run());
				}
				updateState.run();
			});

			boolean oldNodePerWord = nodePerWord;
			nodePerWord = true;
			elemFunctions.add(addProp);
			visitor.visitChildren(link);
			nodePerWord = oldNodePerWord;
			elemFunctions.remove(addProp);
		}

		public void visit(TextBase text) {
			List<AttributesNode> attrs = AttributesExtension.NODE_ATTRIBUTES.get(document).get(text);
			setAttrs(attrs,true);
			visitor.visitChildren(text);
			setAttrs(attrs,false);
		}

		public void visit(com.vladsch.flexmark.ast.Text text) {
			visitor.visitChildren(text);

			String wholeText = text.getChars().normalizeEOL();

			String[] textsSplit;
			if (nodePerWord) {
				textsSplit = text.getChars().normalizeEOL().split(" ");
			} else {
				textsSplit = new String[1];
				textsSplit[0] = text.getChars().normalizeEOL();
			}
			final String[] textsSplitFinal = textsSplit;

			for(int i = 0; i <= textsSplit.length - 1; i+=1) {
				if (i == 0) {
					addText(textsSplitFinal[i], wholeText);
				} else {
					addText(" " + textsSplitFinal[i], wholeText);
				}
			}
		}

		public void visit(TableHead customNode) {

			if(!shouldShowContent()) return;

			TextFlow oldFlow = flow;
			grid = new GridPane();
			grid.getStyleClass().add("markdown-table-table");
			gridX = 0;
			gridY = -1;
			root.getChildren().add(grid);

			visitor.visitChildren(customNode);

			for(int i = 1; i <=gridX; i+=1) {
				ColumnConstraints constraint = new ColumnConstraints();
				if (i ==gridX) {
					constraint.setPercentWidth(100.0 * (2.0 / (gridX + 1.0)));
				}
				grid.getColumnConstraints().add(constraint);
			}

			flow = oldFlow;
			newParagraph();
			//flow.styleClass ::= "markdown-normal-flow"
		}

		public void visit(TableBody customNode) {
			if(!shouldShowContent()) return;
			visitor.visitChildren(customNode);
			//} else if(customNode instanceof TableBlock) {
			//  super.visit(customNode);
		}

		public void visit(TableRow customNode) {
			if(customNode.getRowNumber() != 0) {
				gridX = 0;
				gridY += 1;
				visitor.visitChildren(customNode);
			}
		}

		public void visit(TableCell customNode) {
			TextFlow oldFlow = flow;
			flow = new TextFlow();
			flow.getStyleClass().add("markdown-normal-flow");
			TextFlow container = flow;
			flow.setPrefWidth(9999);
			flow.getStyleClass().add("markdown-table-cell");
			if (gridY== 0) {
				flow.getStyleClass().add("markdown-table-cell-top");
			}
			if (gridY% 2 == 0) {
				flow.getStyleClass().add("markdown-table-odd");
			} else {
				flow.getStyleClass().add("markdown-table-even");
			}
			grid.add(container, gridX, gridY);
			gridX += 1;
			visitor.visitChildren(customNode);
		}

		public void setAttrs(List<AttributesNode> attrs, boolean add) {
			if(attrs == null) return;

			List<com.vladsch.flexmark.util.ast.Node> attrs2 = new LinkedList<>();
			for(AttributesNode att: attrs) {
				for(com.vladsch.flexmark.util.ast.Node attChild: att.getChildren()) {
					attrs2.add(attChild);
				}
			}

			attrs2.stream().filter(AttributeNode.class::isInstance).map(AttributeNode.class::cast).forEach(att -> {
				if(att.getName().toString().equalsIgnoreCase("style")) {
					if(add) styles.add(att.getValue().toString());
					else styles.remove(att.getValue().toString());
				}
				if(att.isClass()) {
					if(add) elemStyleClass.add(att.getValue().toString());
					else elemStyleClass.remove(att.getValue().toString());
				}
			});
		}

	}

	public void addText(String text, String wholeText) {
		if(!text.isEmpty()) {

			Text toAdd = new Text(text);
			toAdd.getStyleClass().add("markdown-text");

			addFeatures(toAdd,wholeText);

			flow.getChildren().add(toAdd);
		}
	}

	public void addFeatures(Node toAdd, String wholeText) {
		for(String elemStyleClass: elemStyleClass) {
			toAdd.getStyleClass().add(elemStyleClass);
		}
		for(Consumer<Pair<Node,String>> f: elemFunctions) {
			f.accept(new Pair<>(toAdd,wholeText));
		}
		if(!styles.isEmpty()) {
			var tmp = new StringBuilder();
			for(String style: styles) {
				tmp.append(style).append(";");
			}
			toAdd.setStyle(toAdd.getStyle() + tmp);
		}
	}
}
