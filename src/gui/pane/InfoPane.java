package gui.pane;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javafx.event.Event;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import gui.objects.icon.Icon;
import one.util.streamex.EntryStream;
import util.R;
import util.Util;
import util.conf.IsConfigurable;
import util.units.Dur;

import static gui.objects.icon.Icon.createInfoIcon;
import static java.util.stream.Collectors.*;
import static javafx.geometry.Pos.CENTER;
import static javafx.geometry.Pos.CENTER_RIGHT;
import static javafx.scene.layout.Priority.ALWAYS;
import static javafx.scene.layout.Priority.NEVER;
import static util.file.Environment.copyToSysClipboard;
import static util.functional.Util.by;
import static util.functional.Util.byNC;
import static util.graphics.Util.*;

@IsConfigurable("Shortcut Viewer")
public class InfoPane extends OverlayPane<Void> {

	private static final String STYLECLASS = "info-pane";
	private static final String STYLECLASS_GROUP = "info-pane-group-label";
	private static String fixASCII(String s) {
		if (s.length()==1) {
			char c = s.charAt(0);
			if (c=='\n') return "\n";
			if (c=='\r') return "\r";
		} else if (s.length()==2) {
			if (s.charAt(0)=='\n' && s.charAt(1)=='\r') return "\\n\\r";
			if (s.charAt(0)=='\r' && s.charAt(1)=='\n') return "\\r\\n";
		}
		return s;
	}
	private static String getGroup(String property_key) {
		int i = property_key.indexOf('.');
		return i<0 ? property_key : property_key.substring(0,i);
	}


	private final GridPane g = new GridPane();

	public InfoPane() {
		getStyleClass().add(STYLECLASS);

		Icon helpI = createInfoIcon("System information viewer\n\n" +
									"Displays available system properties. Click on the property to copy the value.");
		ScrollPane sp = new ScrollPane();
				   sp.setOnScroll(Event::consume);
				   sp.setContent(layStack(g, CENTER));
				   sp.setFitToWidth(true);
				   sp.setFitToHeight(false);
				   sp.setHbarPolicy(ScrollBarPolicy.NEVER);
				   sp.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
		VBox l = layHeaderTop(5, CENTER,
			layHorizontally(5,CENTER_RIGHT, helpI),
			layStack(sp, CENTER)
		);
		l.setMaxWidth(800);
		l.maxHeightProperty().bind(heightProperty().subtract(100));
		setContent(l);
	}

	@Override
	public void show(Void noValue) {
		super.show();

		// clear content
		g.getChildren().clear();
		g.getRowConstraints().clear();
		g.getColumnConstraints().clear();

		// build columns
		g.getColumnConstraints().add(new ColumnConstraints(550,550,550, NEVER, HPos.RIGHT, false));
		g.getColumnConstraints().add(new ColumnConstraints(10));
		g.getColumnConstraints().add(new ColumnConstraints(-1,-1,-1, ALWAYS, HPos.LEFT, false));

		@SuppressWarnings("unchecked")
		Map<String,List<Entry<String,String>>> props = (Map) System.getProperties()
				.entrySet().stream()
				.filter(e -> e.getKey() instanceof String && e.getValue() instanceof String)
				.collect(groupingBy(e -> getGroup((String)e.getKey())));

		ProcessHandle p = ProcessHandle.current();
		ProcessHandle.Info pInfo = p.info();
		EntryStream.of(
			"pid",          String.valueOf(p.pid()),
			"arguments",    pInfo.arguments().map(args -> String.join(", ", args)).orElse(""),
			"command",      pInfo.command().orElse(""),
			"commandline",  pInfo.commandLine().orElse(""),
			"start time",   pInfo.startInstant().map(i -> Util.localDateTimeFromMillis(i).toString()).orElse(""),
			"running time", pInfo.totalCpuDuration().map(d -> new Dur(d.toMillis()).toString()).orElse(""),
			"user",         pInfo.user().orElse("")
			)
			.collect(collectingAndThen(toList(), list -> props.put("process", list)));

		// build rows
		R<Integer> i = new R<>(-1);
		props.entrySet().stream()
			 .sorted(by(Entry::getKey))
			 .peek(e -> e.getValue().sort(byNC(Entry::getKey)))
			 .forEach(e -> {
					// group title row
					i.setOf(v -> v+1);
					Label group = new Label(e.getKey());
						  group.getStyleClass().add(STYLECLASS_GROUP);
					g.add(layVertically(0,Pos.CENTER, new Label(), group), 2,i.get());
					GridPane.setValignment(group.getParent(), VPos.CENTER);
					GridPane.setHalignment(group.getParent(), HPos.LEFT);

					// property rows
					for (Entry<String,String> a : e.getValue()) {
						i.setOf(v -> v+1);
						String name = a.getKey().startsWith(e.getKey()) ? a.getKey().substring(e.getKey().length()+1) : a.getKey();
						String val = fixASCII(a.getValue());

						Label nameL = new Label(name);
						Label valL = new Label(val);
							  valL.setCursor(Cursor.HAND);
							  valL.setOnMouseClicked(ev -> copyToSysClipboard(val));
						g.add(valL, 0,i.get());
						g.add(nameL, 2,i.get());
					}
			});
	}

}