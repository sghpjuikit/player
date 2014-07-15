<Layout.Layout>
  <id>ded5b706-5df4-416f-a842-edf0c580ab34</id>
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
          <double>0.5520361990950227</double>
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
          <id>6341d694-ff8f-4141-a68e-91da8fbfe938</id>
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