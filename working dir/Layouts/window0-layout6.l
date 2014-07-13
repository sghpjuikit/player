<Layout.Layout>
  <id>4e22463b-c9a6-4230-bba4-09dc14044474</id>
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
    <id>c21f743f-7273-4a4e-9d6e-fd48344cb16d</id>
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
</Layout.Layout>