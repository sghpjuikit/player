package sp.it.demo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import org.jetbrains.annotations.NotNull;

/**
 * A small GUId app. that shows many system and environment properties.
 *
 * @author Andrew Thompson
 * @version 2008-06-29
 * @see <a href=https://stackoverflow.com/questions/7585699/list-of-useful-environment-settings-in-java/7616206#7616206>https://stackoverflow.com/questions/7585699/list-of-useful-environment-settings-in-java/7616206#7616206</a>
 */
class SystemPropertiesDemo {

	private static final String sentence = "The quick brown fox jumped over the lazy dog.";
	private static final String sep = System.getProperty("line.separator");
	private static final String fontText = sentence + sep + sentence.toUpperCase() + sep + "0123456789 !@#$%^&*()_+ []\\;',./ {}|:\"<>?";

	private static String[] convertObjectToSortedStringArray(Object[] unsorted) {
		String[] sorted = new String[unsorted.length];
		for (int ii = 0; ii < sorted.length; ii++) {
			sorted[ii] = (String) unsorted[ii];
		}
		Arrays.sort(sorted);
		return sorted;
	}

	private static String dataPairToTableRow(String property, Object value) {
		String val = valueToString(property, value);
		return "<tr>" +
			"<th>" +
			"<code>" +
			property +
			"</code>" +
			"</th>" +
			"<td>" +
			val +
			"</td>" +
			"</tr>";
	}

	private static String valueToString(String property, Object value) {
		if (value instanceof Color) {
			Color color = (Color) value;
			return "<div style='width: 100%; height: 100%; " +
				"background-color: #" +
				Integer.toHexString(color.getRed()) +
				Integer.toHexString(color.getGreen()) +
				Integer.toHexString(color.getBlue()) +
				";'>" + value + "</div>";
		} else if (property.toLowerCase().endsWith("path") ||
			property.toLowerCase().endsWith("dirs")) {
			return delimitedToHtmlList(
				(String) value,
				System.getProperty("path.separator"));
		} else {
			return value.toString();
		}
	}

	private static String delimitedToHtmlList(String values, String delimiter) {
		var parts = values.split(delimiter);
		var sb = new StringBuilder();
		sb.append("<ol>");
		for (String part : parts) {
			sb.append("<li>");
			sb.append(part);
			sb.append("</li>");
		}
		return sb.toString();
	}

	private static Component getExampleOfFont(String fontFamily) {
		Font font = new Font(fontFamily, Font.PLAIN, 24);
		JTextArea ta = new JTextArea();
		ta.setFont(font);
		ta.setText(fontText);
		ta.setEditable(false);
		// don't allow these to get focus, as it
		// interferes with desired scroll behavior
		ta.setFocusable(false);
		return ta;
	}

	private static JScrollPane getOutputWidgetForContent(String content) {
		JEditorPane op = new JEditorPane();
		op.setContentType("text/html");
		op.setEditable(false);

		op.setText(content);

		return new JScrollPane(op);
	}

	public static void main(String[] args) {
		JTabbedPane tabPane = new JTabbedPane();
		StringBuilder sb;
		String header = "<html><body><table border=1 width=100%>";

		sb = new StringBuilder(header);
		Properties prop = System.getProperties();
		String[] propStrings = convertObjectToSortedStringArray(
			prop.stringPropertyNames().toArray());
		for (String propString : propStrings) {
			sb.append(dataPairToTableRow(propString, System.getProperty(propString)));
		}
		tabPane.addTab(
			"System",
			getOutputWidgetForContent(sb.toString()));

		sb = new StringBuilder(header);
		var environment = System.getenv();
		String[] envStrings = convertObjectToSortedStringArray(
			environment.keySet().toArray());
		for (String envString : envStrings) {
			sb.append(dataPairToTableRow(envString, environment.get(envString)));
		}
		tabPane.addTab(
			"Environment",
			getOutputWidgetForContent(sb.toString()));

		sb = new StringBuilder(header);
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gs = ge.getScreenDevices();
		for (int j = 0; j < gs.length; j++) {
			GraphicsDevice gd = gs[j];
			sb.append(
				dataPairToTableRow(
					"Device " + j,
					gd.toString() +
						"  " +
						gd.getIDstring()));
			GraphicsConfiguration[] gc =
				gd.getConfigurations();
			for (int i = 0; i < gc.length; i++) {
				sb.append(
					dataPairToTableRow(
						"Config " +
							i,
						(int) gc[i].getBounds().getWidth() +
							"x" +
							(int) gc[i].getBounds().getHeight() +
							" " +
							gc[i].getColorModel() +
							", " +
							"  Accelerated: " +
							gc[i].getImageCapabilities().isAccelerated() +
							"  True Volatile: " +
							gc[i].getImageCapabilities().isTrueVolatile()));
			}
		}
		tabPane.addTab(
			"Graphics Environment",
			getOutputWidgetForContent(sb.toString()));

		String[] fonts = ge.getAvailableFontFamilyNames();
		JPanel fontTable = new JPanel(new BorderLayout(3, 1));
		// to enable key based scrolling in the font panel
		fontTable.setFocusable(true);
		JPanel fontNameCol = new JPanel(new GridLayout(0, 1, 2, 2));
		JPanel fontExampleCol = new JPanel(new GridLayout(0, 1, 2, 2));
		fontTable.add(fontNameCol, BorderLayout.WEST);
		fontTable.add(fontExampleCol, BorderLayout.CENTER);
		for (String font : fonts) {
			fontNameCol.add(new JLabel(font));
			fontExampleCol.add(getExampleOfFont(font));
		}
		tabPane.add("Fonts", new JScrollPane(fontTable));

		sb = new StringBuilder(header);

		sb.append("<thead>");
		sb.append("<tr>");
		sb.append("<th>");
		sb.append("Code");
		sb.append("</th>");
		sb.append("<th>");
		sb.append("Language");
		sb.append("</th>");
		sb.append("<th>");
		sb.append("Country");
		sb.append("</th>");
		sb.append("<th>");
		sb.append("Variant");
		sb.append("</th>");
		sb.append("</tr>");
		sb.append("</thead>");

		Locale[] locales = Locale.getAvailableLocales();
		SortableLocale[] sortableLocale = new SortableLocale[locales.length];
		for (int ii = 0; ii < locales.length; ii++) {
			sortableLocale[ii] = new SortableLocale(locales[ii]);
		}
		Arrays.sort(sortableLocale);
		for (int ii = 0; ii < locales.length; ii++) {
			String prefix = "";
			String suffix = "";
			Locale locale = sortableLocale[ii].getLocale();
			if (locale.equals(Locale.getDefault())) {
				prefix = "<b>";
				suffix = "</b>";
			}
			sb.append(dataPairToTableRow(
				prefix +
					locale +
					suffix,
				prefix +
					locale.getDisplayLanguage() +
					suffix +
					"</td><td>" +
					prefix +
					locale.getDisplayCountry() +
					suffix +
					"</td><td>" +
					prefix +
					locale.getDisplayVariant() +
					suffix));
		}
		tabPane.add("Locales",
			getOutputWidgetForContent(sb.toString()));

		int border = 5;
		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(new EmptyBorder(border, border, border, border));
		p.add(tabPane, BorderLayout.CENTER);
		p.setPreferredSize(new Dimension(400, 400));
		JFrame f = new JFrame("Properties");
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		f.getContentPane().add(p, BorderLayout.CENTER);
		f.pack();
		f.setMinimumSize(f.getPreferredSize());
		f.setSize(600, 500);
		f.setLocationRelativeTo(null);
		f.setVisible(true);
	}

	private static class SortableLocale implements Comparable<SortableLocale> {

		Locale locale;

		SortableLocale(Locale locale) {
			this.locale = locale;
		}

		public String toString() {
			return locale.toString();
		}

		public Locale getLocale() {
			return locale;
		}

		public int compareTo(@NotNull SortableLocale l) {
			return locale.toString().compareTo(l.toString());
		}
	}

}