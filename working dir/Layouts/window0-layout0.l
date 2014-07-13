<Layout.Layout>
  <id>9b0afec6-9a1e-4c44-b1b3-19525b13f9e0</id>
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
  <child class="Layout.BiContainerPure">
    <id>1a445f91-3fb4-4be6-aef4-a668543006ad</id>
    <properties>
      <map>
        <entry>
          <string>col</string>
          <int>0</int>
        </entry>
        <entry>
          <string>orient</string>
          <javafx.geometry.Orientation>HORIZONTAL</javafx.geometry.Orientation>
        </entry>
        <entry>
          <string>pos</string>
          <double>0.5</double>
        </entry>
        <entry>
          <string>collap</string>
          <double>0.0</double>
        </entry>
        <entry>
          <string>abs_size</string>
          <boolean>false</boolean>
        </entry>
        <entry>
          <string>locked</string>
          <boolean>false</boolean>
        </entry>
      </map>
    </properties>
    <b>false</b>
    <children>
      <entry>
        <int>1</int>
        <Layout.UniContainer>
          <id>852231ee-c7e7-4bc9-8a4c-b23a9656ddd8</id>
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
            <id>6e460555-aae5-4983-9fd5-478c6b3b0077</id>
            <name>FileInfo</name>
            <configs>
              <Configuration.ObjectConfig>
                <gui__name>Is preferred</gui__name>
                <name>preferred</name>
                <group>FXMLWidget</group>
                <info>Preferred widget for its widget type.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>preferred</name>
                  <clazz>Layout.Widgets.Widget</clazz>
                </sourceField>
                <defaultValue class="boolean">false</defaultValue>
                <value class="boolean">false</value>
                <tmp class="boolean">false</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Read mode</gui__name>
                <name>readMode</name>
                <group>FileInfoController</group>
                <info>Source of data for the widget.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>readMode</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="PseudoObjects.ReadMode">PLAYING</defaultValue>
                <value class="PseudoObjects.ReadMode">PLAYING</value>
                <tmp class="PseudoObjects.ReadMode">PLAYING</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Column min width</gui__name>
                <name>minColumnWidth</name>
                <group>FileInfoController</group>
                <info>Minimal width for field columns. Use -1 for automatic width</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>minColumnWidth</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="double">150.0</defaultValue>
                <value class="double">150.0</value>
                <tmp class="double">150.0</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Cover source</gui__name>
                <name>cover_source</name>
                <group>FileInfoController</group>
                <info>Source for cover image.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>cover_source</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="AudioPlayer.tagging.Cover.Cover$CoverSource">ANY</defaultValue>
                <value class="AudioPlayer.tagging.Cover.Cover$CoverSource">ANY</value>
                <tmp class="AudioPlayer.tagging.Cover.Cover$CoverSource">ANY</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Read mode change on drag</gui__name>
                <name>changeReadModeOnTransfer</name>
                <group>FileInfoController</group>
                <info>Change read mode to CUSTOM when data are arbitrary added to widget.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>changeReadModeOnTransfer</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Rating editable</gui__name>
                <name>editableRating</name>
                <group>FileInfoController</group>
                <info>Allow change of rating.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>editableRating</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Rating stars</gui__name>
                <name>maxRating</name>
                <group>FileInfoController</group>
                <info>Number of stars for rating. Rating value is recalculated accordingly.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>maxRating</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="int">7</defaultValue>
                <value class="int">7</value>
                <tmp class="int">7</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Rating allow partial</gui__name>
                <name>partialRating</name>
                <group>FileInfoController</group>
                <info>Allow partial values for rating.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>partialRating</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Rating react on hover</gui__name>
                <name>hoverRating</name>
                <group>FileInfoController</group>
                <info>Move rating according to mouse when hovering.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>hoverRating</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Rating zoom on hover</gui__name>
                <name>rating_zoom</name>
                <group>FileInfoController</group>
                <info>Rating zoom on hover.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>rating_zoom</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Rating skin</gui__name>
                <name>rating_skin</name>
                <group>FileInfoController</group>
                <info>Rating skin.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>rating_skin</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="string">file:/C:/Users/Plutonium_/Documents/GitHub/player/working%20dir/controls/Rating/blue.css</defaultValue>
                <value class="string">file:/C:/Users/Plutonium_/Documents/GitHub/player/working%20dir/controls/Rating/blue.css</value>
                <tmp class="string">file:/C:/Users/Plutonium_/Documents/GitHub/player/working%20dir/controls/Rating/blue.css</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Overrun style</gui__name>
                <name>overrun_style</name>
                <group>FileInfoController</group>
                <info>Style of clipping fields&apos; text when outside of the area.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>overrun_style</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="javafx.scene.control.OverrunStyle">ELLIPSIS</defaultValue>
                <value class="javafx.scene.control.OverrunStyle">ELLIPSIS</value>
                <tmp class="javafx.scene.control.OverrunStyle">ELLIPSIS</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Display cover</gui__name>
                <name>showCover</name>
                <group>FileInfoController</group>
                <info>Show cover.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>showCover</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Display fields</gui__name>
                <name>showFields</name>
                <group>FileInfoController</group>
                <info>Show fields.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>showFields</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Display empty fields</gui__name>
                <name>showEmptyFields</name>
                <group>FileInfoController</group>
                <info>Show empty fields.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>showEmptyFields</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Group fields</gui__name>
                <name>groupFields</name>
                <group>FileInfoController</group>
                <info>Separate fields by gap and group them..</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>groupFields</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Show title</gui__name>
                <name>showTitle</name>
                <group>FileInfoController</group>
                <info>Show this field.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>showTitle</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Show track</gui__name>
                <name>showtrack</name>
                <group>FileInfoController</group>
                <info>Show this field.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>showtrack</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Show disc</gui__name>
                <name>showdisc</name>
                <group>FileInfoController</group>
                <info>Show this field.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>showdisc</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Show artist</gui__name>
                <name>showartist</name>
                <group>FileInfoController</group>
                <info>Show this field.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>showartist</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Show album artist</gui__name>
                <name>showalbum_artist</name>
                <group>FileInfoController</group>
                <info>Show this field.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>showalbum_artist</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Show album</gui__name>
                <name>showalbum</name>
                <group>FileInfoController</group>
                <info>Show this field.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>showalbum</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Show year</gui__name>
                <name>showyear</name>
                <group>FileInfoController</group>
                <info>Show this field.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>showyear</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Show genre</gui__name>
                <name>showgenre</name>
                <group>FileInfoController</group>
                <info>Show this field.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>showgenre</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Show composer</gui__name>
                <name>showcomposer</name>
                <group>FileInfoController</group>
                <info>Show this field.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>showcomposer</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Show publisher</gui__name>
                <name>showpublisher</name>
                <group>FileInfoController</group>
                <info>Show this field.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>showpublisher</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Show rating</gui__name>
                <name>showrating</name>
                <group>FileInfoController</group>
                <info>Show this field.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>showrating</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Show playcount</gui__name>
                <name>showplaycount</name>
                <group>FileInfoController</group>
                <info>Show this field.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>showplaycount</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Show comment</gui__name>
                <name>showcomment</name>
                <group>FileInfoController</group>
                <info>Show this field.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>showcomment</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Show category</gui__name>
                <name>showcategory</name>
                <group>FileInfoController</group>
                <info>Show this field.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>showcategory</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Show filesize</gui__name>
                <name>showfilesize</name>
                <group>FileInfoController</group>
                <info>Show this field.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>showfilesize</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Show filename</gui__name>
                <name>showfilename</name>
                <group>FileInfoController</group>
                <info>Show this field.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>showfilename</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Show format</gui__name>
                <name>showformat</name>
                <group>FileInfoController</group>
                <info>Show this field.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>showformat</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Show bitrate</gui__name>
                <name>showbitrate</name>
                <group>FileInfoController</group>
                <info>Show this field.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>showbitrate</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Show encoding</gui__name>
                <name>showencoding</name>
                <group>FileInfoController</group>
                <info>Show this field.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>showencoding</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Show location</gui__name>
                <name>showlocation</name>
                <group>FileInfoController</group>
                <info>Show this field.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>showlocation</name>
                  <clazz>FileInfo.FileInfoController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
            </configs>
          </child>
        </Layout.UniContainer>
      </entry>
      <entry>
        <int>2</int>
        <Layout.UniContainer>
          <id>9679c038-94b1-4b74-8091-ae4b636d0bf9</id>
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
            <id>9f774261-3549-46ae-9756-c9f411561f1c</id>
            <name>Playlist</name>
            <configs>
              <Configuration.ObjectConfig>
                <gui__name>Is preferred</gui__name>
                <name>preferred</name>
                <group>FXMLWidget</group>
                <info>Preferred widget for its widget type.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>preferred</name>
                  <clazz>Layout.Widgets.Widget</clazz>
                </sourceField>
                <defaultValue class="boolean">false</defaultValue>
                <value class="boolean">false</value>
                <tmp class="boolean">false</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Table orientation</gui__name>
                <name>table_orient</name>
                <group>PlaylistController</group>
                <info>Orientation of table.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>table_orient</name>
                  <clazz>Playlist.PlaylistController</clazz>
                </sourceField>
                <defaultValue class="javafx.geometry.NodeOrientation">INHERIT</defaultValue>
                <value class="javafx.geometry.NodeOrientation">INHERIT</value>
                <tmp class="javafx.geometry.NodeOrientation">INHERIT</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Table text orientation</gui__name>
                <name>cell_align</name>
                <group>PlaylistController</group>
                <info>Orientation of text within table cells.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>cell_align</name>
                  <clazz>Playlist.PlaylistController</clazz>
                </sourceField>
                <defaultValue class="javafx.geometry.Pos">CENTER_LEFT</defaultValue>
                <value class="javafx.geometry.Pos">CENTER_LEFT</value>
                <tmp class="javafx.geometry.Pos">CENTER_LEFT</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Zeropad numbers</gui__name>
                <name>zeropad</name>
                <group>PlaylistController</group>
                <info>Adds 0 to uphold number length consistency.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>zeropad</name>
                  <clazz>Playlist.PlaylistController</clazz>
                </sourceField>
                <defaultValue class="boolean">false</defaultValue>
                <value class="boolean">false</value>
                <tmp class="boolean">false</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Show table menu button</gui__name>
                <name>show_menu_button</name>
                <group>PlaylistController</group>
                <info>Show table menu button for controlling columns.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>show_menu_button</name>
                  <clazz>Playlist.PlaylistController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Show table header</gui__name>
                <name>show_header</name>
                <group>PlaylistController</group>
                <info>Show table header with columns.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>show_header</name>
                  <clazz>Playlist.PlaylistController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Search show always</gui__name>
                <name>always_show_search</name>
                <group>PlaylistController</group>
                <info>Forbid hiding of the search paneat all times. It will always be displayed.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>always_show_search</name>
                  <clazz>Playlist.PlaylistController</clazz>
                </sourceField>
                <defaultValue class="boolean">false</defaultValue>
                <value class="boolean">false</value>
                <tmp class="boolean">false</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Search hide always</gui__name>
                <name>always_hide_search</name>
                <group>PlaylistController</group>
                <info>Allows hiding search pane even if in effect.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>always_hide_search</name>
                  <clazz>Playlist.PlaylistController</clazz>
                </sourceField>
                <defaultValue class="boolean">false</defaultValue>
                <value class="boolean">false</value>
                <tmp class="boolean">false</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Search ignore case</gui__name>
                <name>ignoreCase</name>
                <group>PlaylistController</group>
                <info>Ignore case when comparing for search results.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>ignoreCase</name>
                  <clazz>Playlist.PlaylistController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
              <Configuration.ObjectConfig>
                <gui__name>Search show original index</gui__name>
                <name>orig_index</name>
                <group>PlaylistController</group>
                <info>Show index of the itme as it was in the unfiltered playlisteven when filter applied.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <sourceField>
                  <name>orig_index</name>
                  <clazz>Playlist.PlaylistController</clazz>
                </sourceField>
                <defaultValue class="boolean">true</defaultValue>
                <value class="boolean">true</value>
                <tmp class="boolean">true</tmp>
              </Configuration.ObjectConfig>
            </configs>
          </child>
        </Layout.UniContainer>
      </entry>
    </children>
  </child>
</Layout.Layout>