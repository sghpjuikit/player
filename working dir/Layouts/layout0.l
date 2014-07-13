<Layout.Layout>
  <id>0404f501-4f12-42e1-aee8-84141c011aef</id>
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
  <b>true</b>
  <child class="Layout.BiContainerPure">
    <id>0a03f269-b8bd-443b-8e99-a7ceece79106</id>
    <properties>
      <map>
        <entry>
          <string>col</string>
          <int>0</int>
        </entry>
        <entry>
          <string>orient</string>
          <javafx.geometry.Orientation>VERTICAL</javafx.geometry.Orientation>
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
          <id>a9f99eca-ab48-4e45-bb57-a49ee3f5fd6b</id>
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
            <id>6d18fb70-3cf8-492b-a236-576e4f66a4b9</id>
            <properties>
              <map>
                <entry>
                  <string>col</string>
                  <int>0</int>
                </entry>
                <entry>
                  <string>orient</string>
                  <javafx.geometry.Orientation>VERTICAL</javafx.geometry.Orientation>
                </entry>
                <entry>
                  <string>pos</string>
                  <double>0.2678132678132678</double>
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
                  <id>0dc82cb9-bb6b-4527-b3ae-27f52d7604a1</id>
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
                    <id>0164584e-ec55-44ea-b01f-dd7bf8b7d462</id>
                    <name>PlayerControls</name>
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
                        <gui__name>Show chapters</gui__name>
                        <name>showChapters</name>
                        <group>PlayerControlsController</group>
                        <info>Display chapter marks on seeker.</info>
                        <editable>true</editable>
                        <visible>true</visible>
                        <min>NaN</min>
                        <max>NaN</max>
                        <sourceField>
                          <name>showChapters</name>
                          <clazz>PlayerControls.PlayerControlsController</clazz>
                        </sourceField>
                        <defaultValue class="boolean">true</defaultValue>
                        <value class="boolean">true</value>
                        <tmp class="boolean">true</tmp>
                      </Configuration.ObjectConfig>
                      <Configuration.ObjectConfig>
                        <gui__name>Show info for chapters</gui__name>
                        <name>popupChapters</name>
                        <group>PlayerControlsController</group>
                        <info>Display pop up information for chapter marks on seeker.</info>
                        <editable>true</editable>
                        <visible>true</visible>
                        <min>NaN</min>
                        <max>NaN</max>
                        <sourceField>
                          <name>popupChapters</name>
                          <clazz>PlayerControls.PlayerControlsController</clazz>
                        </sourceField>
                        <defaultValue class="boolean">true</defaultValue>
                        <value class="boolean">true</value>
                        <tmp class="boolean">true</tmp>
                      </Configuration.ObjectConfig>
                      <Configuration.ObjectConfig>
                        <gui__name>Show elapsed time</gui__name>
                        <name>elapsedTime</name>
                        <group>PlayerControlsController</group>
                        <info>Show elapsed time instead of remaining.</info>
                        <editable>true</editable>
                        <visible>true</visible>
                        <min>NaN</min>
                        <max>NaN</max>
                        <sourceField>
                          <name>elapsedTime</name>
                          <clazz>PlayerControls.PlayerControlsController</clazz>
                        </sourceField>
                        <defaultValue class="boolean">true</defaultValue>
                        <value class="boolean">true</value>
                        <tmp class="boolean">true</tmp>
                      </Configuration.ObjectConfig>
                      <Configuration.ObjectConfig>
                        <gui__name>Play files on drop</gui__name>
                        <name>playDropped</name>
                        <group>PlayerControlsController</group>
                        <info>Plays the drag and dropped files instead of enqueuing them in playlist.</info>
                        <editable>true</editable>
                        <visible>true</visible>
                        <min>NaN</min>
                        <max>NaN</max>
                        <sourceField>
                          <name>playDropped</name>
                          <clazz>PlayerControls.PlayerControlsController</clazz>
                        </sourceField>
                        <defaultValue class="boolean">false</defaultValue>
                        <value class="boolean">false</value>
                        <tmp class="boolean">false</tmp>
                      </Configuration.ObjectConfig>
                    </configs>
                  </child>
                </Layout.UniContainer>
              </entry>
              <entry>
                <int>2</int>
                <Layout.UniContainer>
                  <id>bb2ee1b0-c6c2-41ea-a1cf-fadda97720e8</id>
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
                    <id>20fbe39f-a0b3-44fa-a723-34ac455f7e16</id>
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
                          <id>e21b352b-c650-4ecd-a7ac-4554f2b6844e</id>
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
                          <child class="Layout.Widgets.EmptyWidget">
                            <id>ddd1ae33-f0f8-499d-8b9b-e0ebf39e808c</id>
                            <name>Empty</name>
                            <configs class="empty-list"/>
                          </child>
                        </Layout.UniContainer>
                      </entry>
                      <entry>
                        <int>2</int>
                        <Layout.UniContainer>
                          <id>cb9b3703-681f-4316-955c-cb28fafc997d</id>
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
                            <id>9cd85d94-f041-40dc-9ab9-6f5ccbd4f9d2</id>
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
                                  <id>5134bd5b-4a0c-45be-b1f1-b835ac33dd55</id>
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
                                  <child class="Layout.Widgets.EmptyWidget">
                                    <id>2d94ac1d-0209-4669-a098-f3f0fbb2f7dd</id>
                                    <name>Empty</name>
                                    <configs class="empty-list"/>
                                  </child>
                                </Layout.UniContainer>
                              </entry>
                              <entry>
                                <int>2</int>
                                <Layout.UniContainer>
                                  <id>e40bfaef-9ca7-408d-9140-bc91d53db7d7</id>
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
                                  <child class="Layout.Widgets.EmptyWidget">
                                    <id>4692164d-794c-46f9-be9e-4be0fc3a9188</id>
                                    <name>Empty</name>
                                    <configs class="empty-list"/>
                                  </child>
                                </Layout.UniContainer>
                              </entry>
                            </children>
                          </child>
                        </Layout.UniContainer>
                      </entry>
                    </children>
                  </child>
                </Layout.UniContainer>
              </entry>
            </children>
          </child>
        </Layout.UniContainer>
      </entry>
      <entry>
        <int>2</int>
        <Layout.UniContainer>
          <id>f5c726cf-ff1e-4205-8ae5-ffdaa92efad6</id>
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
            <id>e3592e05-e887-426e-b915-50a7264a3fd5</id>
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
                  <id>790f4686-a4ee-4242-ad0f-c3c46930f6dd</id>
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
                  <child class="Layout.Widgets.ClassWidget">
                    <id>03bbed7a-1da5-4406-b3fe-caf6490204b8</id>
                    <name>Settings</name>
                    <configs>
                      <Configuration.ObjectConfig>
                        <gui__name>Is preferred</gui__name>
                        <name>preferred</name>
                        <group>ClassWidget</group>
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
                        <gui__name>Show non editable fields</gui__name>
                        <name>show_noneditable</name>
                        <group>Configurator</group>
                        <info>Include non read-only fields.</info>
                        <editable>true</editable>
                        <visible>true</visible>
                        <min>NaN</min>
                        <max>NaN</max>
                        <sourceField>
                          <name>show_noneditable</name>
                          <clazz>Layout.WidgetImpl.Configurator</clazz>
                        </sourceField>
                        <defaultValue class="boolean">false</defaultValue>
                        <value class="boolean">false</value>
                        <tmp class="boolean">false</tmp>
                      </Configuration.ObjectConfig>
                      <Configuration.ObjectConfig>
                        <gui__name>Field names alignment</gui__name>
                        <name>alignemnt</name>
                        <group>Configurator</group>
                        <info>Alignment of field names.</info>
                        <editable>true</editable>
                        <visible>true</visible>
                        <min>NaN</min>
                        <max>NaN</max>
                        <sourceField>
                          <name>alignemnt</name>
                          <clazz>Layout.WidgetImpl.Configurator</clazz>
                        </sourceField>
                        <defaultValue class="javafx.geometry.HPos">RIGHT</defaultValue>
                        <value class="javafx.geometry.HPos">RIGHT</value>
                        <tmp class="javafx.geometry.HPos">RIGHT</tmp>
                      </Configuration.ObjectConfig>
                      <Configuration.ObjectConfig>
                        <gui__name>Group titles alignment</gui__name>
                        <name>title_align</name>
                        <group>Configurator</group>
                        <info>Alignment of group names.</info>
                        <editable>true</editable>
                        <visible>true</visible>
                        <min>NaN</min>
                        <max>NaN</max>
                        <sourceField>
                          <name>title_align</name>
                          <clazz>Layout.WidgetImpl.Configurator</clazz>
                        </sourceField>
                        <defaultValue class="javafx.geometry.Pos">CENTER</defaultValue>
                        <value class="javafx.geometry.Pos">CENTER</value>
                        <tmp class="javafx.geometry.Pos">CENTER</tmp>
                      </Configuration.ObjectConfig>
                      <Configuration.ObjectConfig>
                        <gui__name>expanded</gui__name>
                        <name>expanded</name>
                        <group>Configurator</group>
                        <info></info>
                        <editable>true</editable>
                        <visible>false</visible>
                        <min>NaN</min>
                        <max>NaN</max>
                        <sourceField>
                          <name>expanded</name>
                          <clazz>Layout.WidgetImpl.Configurator</clazz>
                        </sourceField>
                        <defaultValue class="string"></defaultValue>
                        <value class="string"></value>
                        <tmp class="string"></tmp>
                      </Configuration.ObjectConfig>
                    </configs>
                    <class__type>Layout.WidgetImpl.Configurator</class__type>
                  </child>
                </Layout.UniContainer>
              </entry>
              <entry>
                <int>2</int>
                <Layout.UniContainer>
                  <id>5659d035-8a80-44a3-ab6a-ccedf0cf3ba9</id>
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
                    <id>8439acf5-3709-4494-9a56-7b51b0f8adcc</id>
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
        </Layout.UniContainer>
      </entry>
    </children>
  </child>
</Layout.Layout>