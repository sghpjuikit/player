package sp.it.demo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.imageio.ImageIO;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Control;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.Position;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import static javax.swing.SwingUtilities.invokeLater;

/**
 * @see <a href=https://stackoverflow.com/questions/7585699/list-of-useful-environment-settings-in-java/7616206#7616206>https://stackoverflow.com/questions/7585699/list-of-useful-environment-settings-in-java/7616206#7616206</a>
 */
class MediaTypesDemo {

	public static void main(String[] args) {
		invokeLater(() -> {
			MediaTypes mediaTypes = new MediaTypes();
			JPanel p = new JPanel();
			mediaTypes.createGui(p);
			JOptionPane.showMessageDialog(null,p);
		});
	}

	private static class MediaTypes extends JFrame {

		JTable table;
		boolean sortable = false;
		JTree tree;

		public Object[][] mergeArrays(String name1, Object[] data1, String name2, Object[] data2) {
			Object[][] data = new Object[data1.length+data2.length][2];
			for (int ii=0; ii<data1.length; ii++) {
				data[ii][0] = name1;
				data[ii][1] = data1[ii];
			}
			int offset = data1.length;
			for (int ii=offset; ii<data.length; ii++) {
				data[ii][0] = name2;
				data[ii][1] = data2[ii-offset];
			}
			return data;
		}

		public void createGui(JPanel panel) {
			createGui(panel, "");
		}

		public String getShortLineName(String name) {
			String[] lineTypes = {
				"Clip",
				"SourceDataLine",
				"TargetDataLine",
				"Speaker",
				"Microphone",
				"Master Volume",
				"Line In"
			};
			for (String shortName : lineTypes) {
				if ( name.toLowerCase().replaceAll("_", " ").contains(shortName.toLowerCase() )) {
					return shortName;
				}
			}
			return name;
		}

		public void createGui(JPanel panel, String path) {

			//DefaultMutableTreeNode selected = null;

			panel.setLayout( new BorderLayout(5,5) );
			final JLabel output = new JLabel("Select a tree leaf to see the details.");
			panel.add(output, BorderLayout.SOUTH);

			table = new JTable();
			try {
				table.setAutoCreateRowSorter(true);
				sortable = true;
			} catch (Throwable ignore) {
				// 1.6+ functionality - not vital
			}
			JScrollPane tableScroll = new JScrollPane(table);
			Dimension d = tableScroll.getPreferredSize();
			d = new Dimension(450,d.height);
			tableScroll.setPreferredSize(d);
			panel.add( tableScroll, BorderLayout.CENTER );

			DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Media");
			DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);

			DefaultMutableTreeNode imageNode = new DefaultMutableTreeNode("Image");
			rootNode.add(imageNode);

			Object[][] data;
			int offset;
			String[] columnNames;

			data = mergeArrays(
				"Reader",
				ImageIO.getReaderFileSuffixes(),
				"Writer",
				ImageIO.getWriterFileSuffixes() );
			columnNames = new String[]{"Input/Output", "Image File Suffixes"};
			MediaData md = new MediaData( "Suffixes", columnNames, data);
			imageNode.add(new DefaultMutableTreeNode(md));

			data = mergeArrays(
				"Reader",
				ImageIO.getReaderMIMETypes(),
				"Writer",
				ImageIO.getWriterMIMETypes() );
			columnNames = new String[]{"Input/Output", "Image MIME Types"};
			md = new MediaData( "MIME", columnNames, data);
			imageNode.add(new DefaultMutableTreeNode(md));

			DefaultMutableTreeNode soundNode = new DefaultMutableTreeNode("Sound");
			rootNode.add(soundNode);

			DefaultMutableTreeNode soundSampledNode = new DefaultMutableTreeNode("Sampled");
			soundNode.add(soundSampledNode);

			md = new MediaData("Suffixes", "Sound File Suffixes", AudioSystem.getAudioFileTypes());
			soundSampledNode.add(new DefaultMutableTreeNode(md));

			Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
			String[][] mixerData = new String[mixerInfo.length][4];
			for (int ii=0; ii<mixerData.length; ii++) {
				mixerData[ii][0] = mixerInfo[ii].getName();
				mixerData[ii][1] = mixerInfo[ii].getVendor();
				mixerData[ii][2] = mixerInfo[ii].getVersion();
				mixerData[ii][3] = mixerInfo[ii].getDescription();
			}
			columnNames = new String[]{"Name", "Vendor", "Version", "Description"};
			md = new MediaData("Mixers", columnNames, mixerData);
			DefaultMutableTreeNode soundSampledMixersNode = new DefaultMutableTreeNode(md);
			soundSampledNode.add(soundSampledMixersNode);

			for (Mixer.Info mixerInfo1 : mixerInfo) {
				Mixer mixer = AudioSystem.getMixer(mixerInfo1);
				data = mergeArrays(
					"Source",
					mixer.getSourceLineInfo(),
					"Target",
					mixer.getTargetLineInfo() );
				columnNames = new String[]{ "Input/Output", "Line Info" };
				md = new MediaData(mixerInfo1.getName(), columnNames, data);
				DefaultMutableTreeNode soundSampledMixerNode = new DefaultMutableTreeNode(md);
				soundSampledMixersNode.add( soundSampledMixerNode );
				Line.Info[] source = mixer.getSourceLineInfo();
				Line.Info[] target = mixer.getTargetLineInfo();
				Line[] all = new Line[source.length + target.length];
				try {
					for (int jj=0; jj<source.length; jj++) {
						all[jj] = AudioSystem.getLine(source[jj]);
					}
					for (int jj=source.length; jj<all.length; jj++) {
						all[jj] = AudioSystem.getLine(target[jj-source.length]);
					}
					columnNames = new String[]{"Attribute", "Value"};
					for (Line line : all) {
						Control[] controls = line.getControls();
						if (line instanceof DataLine dataLine) {
							AudioFormat audioFormat = dataLine.getFormat();
							data = new Object[7+controls.length][2];

							data[0][0] = "Channels";
							data[0][1] = audioFormat.getChannels();

							data[1][0] = "Encoding";
							data[1][1] = audioFormat.getEncoding();

							data[2][0] = "Frame Rate";
							data[2][1] = audioFormat.getFrameRate();

							data[3][0] = "Sample Rate";
							data[3][1] = audioFormat.getSampleRate();

							data[4][0] = "Sample Size (bits)";
							data[4][1] = audioFormat.getSampleSizeInBits();

							data[5][0] = "Big Endian";
							data[5][1] = audioFormat.isBigEndian();

							data[6][0] = "Level";
							data[6][1] = dataLine.getLevel();

						} else if (line instanceof Port port) {
							Port.Info portInfo = (Port.Info) port.getLineInfo();
							data = new Object[2+controls.length][2];

							data[0][0] = "Name";
							data[0][1] = portInfo.getName();

							data[1][0] = "Source";
							data[1][1] = portInfo.isSource();
						} else {
							System.out.println( "?? " + line );
						}
						int start = data.length-controls.length;
						for (int kk=start; kk<data.length; kk++) {
							data[kk][0] = "Control";
							int index = kk-start;
							data[kk][1] = controls[index];
						}
						md = new MediaData(getShortLineName(line.getLineInfo().toString()), columnNames, data);
						soundSampledMixerNode.add(new DefaultMutableTreeNode(md));
					}
				} catch(Exception e) {
					e.printStackTrace();
				}
			}

			int[] midiTypes = MidiSystem.getMidiFileTypes();
			data = new Object[midiTypes.length][2];
			for (int ii=0; ii<midiTypes.length; ii++) {
				data[ii][0] = midiTypes[ii];
				data[ii][1] = switch (midiTypes[ii]) {
					case 0 -> "Single Track";
					case 1 -> "Multi Track";
					case 2 -> "Multi Song";
					default -> "Unknown";
				};
			}
			columnNames = new String[]{"Type", "Description"};
			md = new MediaData("MIDI", columnNames, data);
			DefaultMutableTreeNode soundMIDINode = new DefaultMutableTreeNode(md);
			soundNode.add(soundMIDINode);

			columnNames = new String[]{
				"Attribute",
				"Value"};
			MidiDevice.Info[] midiDeviceInfo = MidiSystem.getMidiDeviceInfo() ;
			for (MidiDevice.Info midiDeviceInfo1 : midiDeviceInfo) {
				data = new Object[6][2];
				data[0][0] = "Name";
				data[0][1] = midiDeviceInfo1.getName();
				data[1][0] = "Vendor";
				data[1][1] = midiDeviceInfo1.getVendor();
				data[2][0] = "Version";
				String version = midiDeviceInfo1.getVersion();
				data[2][1] = version.replaceAll("Version ", "");
				data[3][0] = "Description";
				data[3][1] = midiDeviceInfo1.getDescription();
				data[4][0] = "Maximum Transmitters";
				data[5][0] = "Maximum Receivers";
				try {
					MidiDevice midiDevice = MidiSystem.getMidiDevice(midiDeviceInfo1);
					Object valueTransmitter;
					if (midiDevice.getMaxTransmitters()==AudioSystem.NOT_SPECIFIED) {
						valueTransmitter = "Not specified";
					} else {
						valueTransmitter = midiDevice.getMaxTransmitters();
					}
					Object valueReceiver;
					if (midiDevice.getMaxReceivers()==AudioSystem.NOT_SPECIFIED) {
						valueReceiver = "Not specified";
					} else {
						valueReceiver = midiDevice.getMaxReceivers();
					}
					data[4][1] = valueTransmitter;
					data[5][1] = valueReceiver;
				}catch(MidiUnavailableException mue) {
					data[4][1] = "Unknown";
					data[5][1] = "Unknown";
				}
				md = new MediaData(midiDeviceInfo1.getName(), columnNames, data);
				soundMIDINode.add( new DefaultMutableTreeNode(md) );
			}

			tree = new JTree(treeModel);
			tree.setRootVisible(false);
			tree.getSelectionModel().setSelectionMode
				(TreeSelectionModel.SINGLE_TREE_SELECTION);
			tree.addTreeSelectionListener((TreeSelectionEvent tse) -> {
				if (sortable) {
					output.setText("Click table column headers to sort.");
				}

				DefaultMutableTreeNode node = (DefaultMutableTreeNode)
					tree.getLastSelectedPathComponent();

				if (node == null) return;

				Object nodeInfo = node.getUserObject();
				if (nodeInfo instanceof MediaData mediaData) {
					table.setModel( new DefaultTableModel(
						mediaData.getData(),
						mediaData.getColumnNames()) );
				}
			});

			for (int ii=0; ii<tree.getRowCount(); ii++) {
				tree.expandRow(ii);
			}

			String[] paths = path.split("\\|");
			int row = 0;
			TreePath treePath = null;
			for (String prefix : paths) {
				treePath = tree.getNextMatch( prefix, row, Position.Bias.Forward );
				row = tree.getRowForPath(treePath);
			}

			panel.add(new JScrollPane(tree),BorderLayout.WEST);

			tree.setSelectionPath(treePath);
			tree.scrollRowToVisible(row);
		}
	}

	private static class MediaData {

		String name;
		String[] columnNames;
		Object[][] data;

		MediaData(String name, String columnName, Object[] data) {
			this.name = name;

			columnNames = new String[1];
			columnNames[0] = columnName;

			this.data = new Object[data.length][1];
			for (int ii=0; ii<data.length; ii++) {
				this.data[ii][0] = data[ii];
			}
		}

		MediaData(String name, String[] columnNames, Object[][] data) {
			this.name = name;
			this.columnNames = columnNames;
			this.data = data;
		}

		@Override
		public String toString() {
			return name;
		}

		public String[] getColumnNames() {
			return columnNames;
		}

		public Object[][] getData() {
			return data;
		}
	}

}