<Layout.Layout>
  <id>686e7f4e-6d25-425c-afeb-4b345a7ebea1</id>
  <properties>
    <map>
      <entry>
        <string>padding</string>
        <double>20.0</double>
      </entry>
      <entry>
        <string>locked</string>
        <boolean>false</boolean>
      </entry>
    </map>
  </properties>
  <child class="Layout.Widgets.FXMLWidget">
    <id>dec9fee4-241c-4523-8f38-133ca1eef2f7</id>
    <name>FileInfo</name>
    <configs>
      <Configuration.Config>
        <gui__name>Is preferred</gui__name>
        <name>preferred</name>
        <value class="boolean">false</value>
        <type>java.lang.Boolean</type>
        <group>FXMLWidget</group>
        <info>Preffered widget for its widget type.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Read mode</gui__name>
        <name>readMode</name>
        <value class="PseudoObjects.ReadMode">PLAYING</value>
        <type>PseudoObjects.ReadMode</type>
        <group>FileInfoController</group>
        <info>Source of data for the widget.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Column min width</gui__name>
        <name>minColumnWidth</name>
        <value class="double">150.0</value>
        <type>java.lang.Double</type>
        <group>FileInfoController</group>
        <info>Minimal width for field columns. Use -1 for automatic width</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Cover source</gui__name>
        <name>cover_source</name>
        <value class="AudioPlayer.tagging.Metadata$CoverSource">ANY</value>
        <type>AudioPlayer.tagging.Metadata$CoverSource</type>
        <group>FileInfoController</group>
        <info>Source for cover image.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Read mode change on drag</gui__name>
        <name>changeReadModeOnTransfer</name>
        <value class="boolean">true</value>
        <type>java.lang.Boolean</type>
        <group>FileInfoController</group>
        <info>Change read mode to CUSTOM when data are arbitrary added to widget.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Rating editable</gui__name>
        <name>editableRating</name>
        <value class="boolean">true</value>
        <type>java.lang.Boolean</type>
        <group>FileInfoController</group>
        <info>Allow change of rating.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Rating stars</gui__name>
        <name>maxRating</name>
        <value class="int">5</value>
        <type>java.lang.Integer</type>
        <group>FileInfoController</group>
        <info>Number of stars for rating. Rating value is recalculated accordingly.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Rating allow partial</gui__name>
        <name>partialRating</name>
        <value class="boolean">true</value>
        <type>java.lang.Boolean</type>
        <group>FileInfoController</group>
        <info>Allow partial values for rating.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Rating react on hover</gui__name>
        <name>hoverRating</name>
        <value class="boolean">true</value>
        <type>java.lang.Boolean</type>
        <group>FileInfoController</group>
        <info>Move rating according to mouse when hovering.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Rating zoom on hover</gui__name>
        <name>rating_zoom</name>
        <value class="boolean">true</value>
        <type>java.lang.Boolean</type>
        <group>FileInfoController</group>
        <info>Rating zoom on hover.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Rating skin</gui__name>
        <name>rating_skin</name>
        <value class="string">file:/C:/Users/Plutonium_/Documents/NetBeansProjects/Player/external/controls/Rating/blue.css</value>
        <type>java.lang.String</type>
        <group>FileInfoController</group>
        <info>Rating skin.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Overrun style</gui__name>
        <name>overrun_style</name>
        <value class="javafx.scene.control.OverrunStyle">ELLIPSIS</value>
        <type>javafx.scene.control.OverrunStyle</type>
        <group>FileInfoController</group>
        <info>Style of clipping fields&apos; text when outside of the area.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Display cover</gui__name>
        <name>showCover</name>
        <value class="boolean">true</value>
        <type>java.lang.Boolean</type>
        <group>FileInfoController</group>
        <info>Show cover.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Display fields</gui__name>
        <name>showFields</name>
        <value class="boolean">true</value>
        <type>java.lang.Boolean</type>
        <group>FileInfoController</group>
        <info>Show fields.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Display empty fields</gui__name>
        <name>showEmptyFields</name>
        <value class="boolean">true</value>
        <type>java.lang.Boolean</type>
        <group>FileInfoController</group>
        <info>Show empty fields.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Group fields</gui__name>
        <name>groupFields</name>
        <value class="boolean">true</value>
        <type>java.lang.Boolean</type>
        <group>FileInfoController</group>
        <info>Separate fields by gap and group them..</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Show title</gui__name>
        <name>showTitle</name>
        <value class="boolean">true</value>
        <type>java.lang.Boolean</type>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Show track</gui__name>
        <name>showtrack</name>
        <value class="boolean">true</value>
        <type>java.lang.Boolean</type>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Show disc</gui__name>
        <name>showdisc</name>
        <value class="boolean">true</value>
        <type>java.lang.Boolean</type>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Show artist</gui__name>
        <name>showartist</name>
        <value class="boolean">true</value>
        <type>java.lang.Boolean</type>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Show album artist</gui__name>
        <name>showalbum_artist</name>
        <value class="boolean">true</value>
        <type>java.lang.Boolean</type>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Show album</gui__name>
        <name>showalbum</name>
        <value class="boolean">true</value>
        <type>java.lang.Boolean</type>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Show year</gui__name>
        <name>showyear</name>
        <value class="boolean">true</value>
        <type>java.lang.Boolean</type>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Show genre</gui__name>
        <name>showgenre</name>
        <value class="boolean">true</value>
        <type>java.lang.Boolean</type>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Show composer</gui__name>
        <name>showcomposer</name>
        <value class="boolean">true</value>
        <type>java.lang.Boolean</type>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Show publisher</gui__name>
        <name>showpublisher</name>
        <value class="boolean">true</value>
        <type>java.lang.Boolean</type>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Show rating</gui__name>
        <name>showrating</name>
        <value class="boolean">true</value>
        <type>java.lang.Boolean</type>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Show playcount</gui__name>
        <name>showplaycount</name>
        <value class="boolean">true</value>
        <type>java.lang.Boolean</type>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Show comment</gui__name>
        <name>showcomment</name>
        <value class="boolean">true</value>
        <type>java.lang.Boolean</type>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Show category</gui__name>
        <name>showcategory</name>
        <value class="boolean">true</value>
        <type>java.lang.Boolean</type>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Show filesize</gui__name>
        <name>showfilesize</name>
        <value class="boolean">true</value>
        <type>java.lang.Boolean</type>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Show filename</gui__name>
        <name>showfilename</name>
        <value class="boolean">true</value>
        <type>java.lang.Boolean</type>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Show format</gui__name>
        <name>showformat</name>
        <value class="boolean">true</value>
        <type>java.lang.Boolean</type>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Show bitrate</gui__name>
        <name>showbitrate</name>
        <value class="boolean">true</value>
        <type>java.lang.Boolean</type>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Show encoding</gui__name>
        <name>showencoding</name>
        <value class="boolean">true</value>
        <type>java.lang.Boolean</type>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
      <Configuration.Config>
        <gui__name>Show location</gui__name>
        <name>showlocation</name>
        <value class="boolean">true</value>
        <type>java.lang.Boolean</type>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
      </Configuration.Config>
    </configs>
    <url>file:/C:/Users/Plutonium_/Documents/NetBeansProjects/Player/external/Widgets/FileInfo/FileInfo.fxml</url>
  </child>
</Layout.Layout>