<Layout.Layout>
  <id>ebba37cb-9883-4d79-9445-cc0f19ef4e21</id>
  <properties>
    <map>
      <entry>
        <string>padding</string>
        <double>0.0</double>
      </entry>
      <entry>
        <string>locked</string>
        <boolean>false</boolean>
      </entry>
    </map>
  </properties>
  <b>false</b>
  <child class="Layout.Widgets.FXMLWidget">
    <id>49a6bd5b-7548-4198-9fa7-164d991c5041</id>
    <name>FileInfo</name>
    <configs>
      <Configuration.InstanceFieldConfig>
        <gui__name>Is preferred</gui__name>
        <name>preferred</name>
        <group>FXMLWidget</group>
        <info>Preferred widget for its widget type.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="boolean">false</defaultValue>
        <value class="boolean">false</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Read mode</gui__name>
        <name>readMode</name>
        <group>FileInfoController</group>
        <info>Source of data for the widget.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="PseudoObjects.ReadMode">PLAYING</defaultValue>
        <value class="PseudoObjects.ReadMode">PLAYING</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Column min width</gui__name>
        <name>minColumnWidth</name>
        <group>FileInfoController</group>
        <info>Minimal width for field columns. Use -1 for automatic width</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="double">150.0</defaultValue>
        <value class="double">150.0</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Cover source</gui__name>
        <name>cover_source</name>
        <group>FileInfoController</group>
        <info>Source for cover image.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="AudioPlayer.tagging.Cover.Cover$CoverSource">ANY</defaultValue>
        <value class="AudioPlayer.tagging.Cover.Cover$CoverSource">ANY</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Read mode change on drag</gui__name>
        <name>changeReadModeOnTransfer</name>
        <group>FileInfoController</group>
        <info>Change read mode to CUSTOM when data are arbitrary added to widget.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="boolean">true</defaultValue>
        <value class="boolean">true</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Rating editable</gui__name>
        <name>editableRating</name>
        <group>FileInfoController</group>
        <info>Allow change of rating.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="boolean">true</defaultValue>
        <value class="boolean">true</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Rating stars</gui__name>
        <name>maxRating</name>
        <group>FileInfoController</group>
        <info>Number of stars for rating. Rating value is recalculated accordingly.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="int">7</defaultValue>
        <value class="int">7</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Rating allow partial</gui__name>
        <name>partialRating</name>
        <group>FileInfoController</group>
        <info>Allow partial values for rating.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="boolean">true</defaultValue>
        <value class="boolean">true</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Rating react on hover</gui__name>
        <name>hoverRating</name>
        <group>FileInfoController</group>
        <info>Move rating according to mouse when hovering.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="boolean">true</defaultValue>
        <value class="boolean">true</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Rating zoom on hover</gui__name>
        <name>rating_zoom</name>
        <group>FileInfoController</group>
        <info>Rating zoom on hover.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="boolean">true</defaultValue>
        <value class="boolean">true</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Rating skin</gui__name>
        <name>rating_skin</name>
        <group>FileInfoController</group>
        <info>Rating skin.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="string">file:/C:/Users/Plutonium_/Documents/GitHub/player/working%20dir/controls/Rating/blue.css</defaultValue>
        <value class="string">file:/C:/Users/Plutonium_/Documents/GitHub/player/working%20dir/controls/Rating/blue.css</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Overrun style</gui__name>
        <name>overrun_style</name>
        <group>FileInfoController</group>
        <info>Style of clipping fields&apos; text when outside of the area.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="javafx.scene.control.OverrunStyle">ELLIPSIS</defaultValue>
        <value class="javafx.scene.control.OverrunStyle">ELLIPSIS</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Display cover</gui__name>
        <name>showCover</name>
        <group>FileInfoController</group>
        <info>Show cover.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="boolean">true</defaultValue>
        <value class="boolean">true</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Display fields</gui__name>
        <name>showFields</name>
        <group>FileInfoController</group>
        <info>Show fields.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="boolean">true</defaultValue>
        <value class="boolean">true</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Display empty fields</gui__name>
        <name>showEmptyFields</name>
        <group>FileInfoController</group>
        <info>Show empty fields.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="boolean">true</defaultValue>
        <value class="boolean">true</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Group fields</gui__name>
        <name>groupFields</name>
        <group>FileInfoController</group>
        <info>Separate fields by gap and group them..</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="boolean">true</defaultValue>
        <value class="boolean">true</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Show title</gui__name>
        <name>showTitle</name>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="boolean">true</defaultValue>
        <value class="boolean">true</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Show track</gui__name>
        <name>showtrack</name>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="boolean">true</defaultValue>
        <value class="boolean">true</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Show disc</gui__name>
        <name>showdisc</name>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="boolean">true</defaultValue>
        <value class="boolean">true</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Show artist</gui__name>
        <name>showartist</name>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="boolean">true</defaultValue>
        <value class="boolean">true</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Show album artist</gui__name>
        <name>showalbum_artist</name>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="boolean">true</defaultValue>
        <value class="boolean">true</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Show album</gui__name>
        <name>showalbum</name>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="boolean">true</defaultValue>
        <value class="boolean">true</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Show year</gui__name>
        <name>showyear</name>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="boolean">true</defaultValue>
        <value class="boolean">true</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Show genre</gui__name>
        <name>showgenre</name>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="boolean">true</defaultValue>
        <value class="boolean">true</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Show composer</gui__name>
        <name>showcomposer</name>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="boolean">true</defaultValue>
        <value class="boolean">true</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Show publisher</gui__name>
        <name>showpublisher</name>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="boolean">true</defaultValue>
        <value class="boolean">true</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Show rating</gui__name>
        <name>showrating</name>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="boolean">true</defaultValue>
        <value class="boolean">true</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Show playcount</gui__name>
        <name>showplaycount</name>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="boolean">true</defaultValue>
        <value class="boolean">true</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Show comment</gui__name>
        <name>showcomment</name>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="boolean">true</defaultValue>
        <value class="boolean">true</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Show category</gui__name>
        <name>showcategory</name>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="boolean">true</defaultValue>
        <value class="boolean">true</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Show filesize</gui__name>
        <name>showfilesize</name>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="boolean">true</defaultValue>
        <value class="boolean">true</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Show filename</gui__name>
        <name>showfilename</name>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="boolean">true</defaultValue>
        <value class="boolean">true</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Show format</gui__name>
        <name>showformat</name>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="boolean">true</defaultValue>
        <value class="boolean">true</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Show bitrate</gui__name>
        <name>showbitrate</name>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="boolean">true</defaultValue>
        <value class="boolean">true</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Show encoding</gui__name>
        <name>showencoding</name>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="boolean">true</defaultValue>
        <value class="boolean">true</value>
      </Configuration.InstanceFieldConfig>
      <Configuration.InstanceFieldConfig>
        <gui__name>Show location</gui__name>
        <name>showlocation</name>
        <group>FileInfoController</group>
        <info>Show this field.</info>
        <editable>true</editable>
        <visible>true</visible>
        <min>NaN</min>
        <max>NaN</max>
        <defaultValue class="boolean">true</defaultValue>
        <value class="boolean">true</value>
      </Configuration.InstanceFieldConfig>
    </configs>
  </child>
</Layout.Layout>