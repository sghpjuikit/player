<Layout.Layout>
  <id>cff0c086-22b7-4cbc-b417-b3d85a8005a5</id>
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
  <child class="Layout.BiContainerPure">
    <id>99dc5a7d-eab1-4cd8-8b9c-f885ecdc9f6a</id>
    <properties>
      <map>
        <entry>
          <string>orient</string>
          <javafx.geometry.Orientation>HORIZONTAL</javafx.geometry.Orientation>
        </entry>
        <entry>
          <string>pos</string>
          <double>0.4673304293714997</double>
        </entry>
        <entry>
          <string>collap</string>
          <double>0.0</double>
        </entry>
        <entry>
          <string>lock</string>
          <boolean>false</boolean>
        </entry>
      </map>
    </properties>
    <children>
      <entry>
        <int>1</int>
        <Layout.UniContainer>
          <id>3f3238a6-affa-4b28-8d73-cfe0159eb1bc</id>
          <properties>
            <map>
              <entry>
                <string>padding</string>
                <double>0.0</double>
              </entry>
            </map>
          </properties>
          <child class="Layout.BiContainerPure">
            <id>541e055b-e37e-4d13-b25e-9e2a2ee1b06b</id>
            <properties>
              <map>
                <entry>
                  <string>orient</string>
                  <javafx.geometry.Orientation>VERTICAL</javafx.geometry.Orientation>
                </entry>
                <entry>
                  <string>pos</string>
                  <double>0.20249653259361997</double>
                </entry>
                <entry>
                  <string>collap</string>
                  <double>0.0</double>
                </entry>
                <entry>
                  <string>lock</string>
                  <boolean>false</boolean>
                </entry>
              </map>
            </properties>
            <children>
              <entry>
                <int>1</int>
                <Layout.UniContainer>
                  <id>e5573f37-5c29-42fb-8c67-064f016005d9</id>
                  <properties>
                    <map>
                      <entry>
                        <string>padding</string>
                        <double>0.0</double>
                      </entry>
                    </map>
                  </properties>
                  <child class="Layout.Widgets.FXMLWidget">
                    <id>fedf35a1-ead7-48de-9630-0b5b539ac4c4</id>
                    <name>PlayerControls</name>
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
                        <gui__name>Show chapters</gui__name>
                        <name>showChapters</name>
                        <value class="boolean">true</value>
                        <type>java.lang.Boolean</type>
                        <group>PlayerControlsController</group>
                        <info>Display chapter marks on seeker.</info>
                        <editable>true</editable>
                        <visible>true</visible>
                        <min>NaN</min>
                        <max>NaN</max>
                      </Configuration.Config>
                      <Configuration.Config>
                        <gui__name>Show info for chapters</gui__name>
                        <name>popupChapters</name>
                        <value class="boolean">true</value>
                        <type>java.lang.Boolean</type>
                        <group>PlayerControlsController</group>
                        <info>Display pop up information for chapter marks on seeker.</info>
                        <editable>true</editable>
                        <visible>true</visible>
                        <min>NaN</min>
                        <max>NaN</max>
                      </Configuration.Config>
                      <Configuration.Config>
                        <gui__name>Show elapsed time</gui__name>
                        <name>elapsedTime</name>
                        <value class="boolean">true</value>
                        <type>java.lang.Boolean</type>
                        <group>PlayerControlsController</group>
                        <info>Show elapsed time instead of remaining.</info>
                        <editable>true</editable>
                        <visible>true</visible>
                        <min>NaN</min>
                        <max>NaN</max>
                      </Configuration.Config>
                    </configs>
                    <url>file:/C:/Users/Plutonium_/Documents/NetBeansProjects/Player/external/Widgets/PlayerControls/PlayerControls.fxml</url>
                  </child>
                </Layout.UniContainer>
              </entry>
              <entry>
                <int>2</int>
                <Layout.UniContainer>
                  <id>23ced142-acd5-41cb-af8a-8addae885eb1</id>
                  <properties>
                    <map>
                      <entry>
                        <string>padding</string>
                        <double>0.0</double>
                      </entry>
                    </map>
                  </properties>
                  <child class="Layout.Widgets.FXMLWidget">
                    <id>3290520a-c7b6-4644-b6cf-1695d0b4c8f0</id>
                    <name>Tagger</name>
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
                        <gui__name>Read Mode</gui__name>
                        <name>readMode</name>
                        <value class="PseudoObjects.ReadMode">CUSTOM</value>
                        <type>PseudoObjects.ReadMode</type>
                        <group>TaggerController</group>
                        <info>Source of data for the widget.</info>
                        <editable>true</editable>
                        <visible>true</visible>
                        <min>NaN</min>
                        <max>NaN</max>
                      </Configuration.Config>
                      <Configuration.Config>
                        <gui__name>Read mode change on drag</gui__name>
                        <name>changeReadModeOnTransfer</name>
                        <value class="boolean">false</value>
                        <type>java.lang.Boolean</type>
                        <group>TaggerController</group>
                        <info>Change read mode to CUSTOM when data are arbitrary added to widget.</info>
                        <editable>true</editable>
                        <visible>true</visible>
                        <min>NaN</min>
                        <max>NaN</max>
                      </Configuration.Config>
                      <Configuration.Config>
                        <gui__name>No value text</gui__name>
                        <name>TAG_NO_VALUE</name>
                        <value class="string">-- no assigned value --</value>
                        <type>java.lang.String</type>
                        <group>TaggerController</group>
                        <info>Field text in case no tag value for field.</info>
                        <editable>true</editable>
                        <visible>true</visible>
                        <min>NaN</min>
                        <max>NaN</max>
                      </Configuration.Config>
                      <Configuration.Config>
                        <gui__name>Multiple value text</gui__name>
                        <name>TAG_MULTIPLE_VALUE</name>
                        <value class="string">-- multiple values --</value>
                        <type>java.lang.String</type>
                        <group>TaggerController</group>
                        <info>Field text in case multiple tag values per field.</info>
                        <editable>true</editable>
                        <visible>true</visible>
                        <min>NaN</min>
                        <max>NaN</max>
                      </Configuration.Config>
                      <Configuration.Config>
                        <gui__name>Allow change of playcount</gui__name>
                        <name>allow_playcount_change</name>
                        <value class="boolean">false</value>
                        <type>java.lang.Boolean</type>
                        <group>TaggerController</group>
                        <info>Change editability of playcount field. Generally to prevent change to non customary values.</info>
                        <editable>true</editable>
                        <visible>true</visible>
                        <min>NaN</min>
                        <max>NaN</max>
                      </Configuration.Config>
                      <Configuration.Config>
                        <gui__name>Field text alignement</gui__name>
                        <name>field_text_alignment</name>
                        <value class="javafx.geometry.Pos">CENTER_LEFT</value>
                        <type>javafx.geometry.Pos</type>
                        <group>TaggerController</group>
                        <info>Alignment of the text in fields.</info>
                        <editable>true</editable>
                        <visible>true</visible>
                        <min>NaN</min>
                        <max>NaN</max>
                      </Configuration.Config>
                      <Configuration.Config>
                        <gui__name>Mood picker popup position</gui__name>
                        <name>popupPos</name>
                        <value class="GUI.objects.PopOver.PopOver$NodeCentricPos">DownCenter</value>
                        <type>GUI.objects.PopOver.PopOver$NodeCentricPos</type>
                        <group>TaggerController</group>
                        <info>Position of the mood picker pop up relative to the mood text field.</info>
                        <editable>true</editable>
                        <visible>true</visible>
                        <min>NaN</min>
                        <max>NaN</max>
                      </Configuration.Config>
                    </configs>
                    <url>file:/C:/Users/Plutonium_/Documents/NetBeansProjects/Player/external/Widgets/Tagger/Tagger.fxml</url>
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
          <id>d2069c02-ed5f-456c-9c4e-b68612d98287</id>
          <properties>
            <map>
              <entry>
                <string>padding</string>
                <double>0.0</double>
              </entry>
            </map>
          </properties>
          <child class="Layout.Widgets.FXMLWidget">
            <id>00e65cd3-1ab6-43c5-a611-273fe41ebb4c</id>
            <name>Playlist</name>
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
                <gui__name>Table orientation</gui__name>
                <name>table_orient</name>
                <value class="javafx.geometry.NodeOrientation">INHERIT</value>
                <type>javafx.geometry.NodeOrientation</type>
                <group>PlaylistController</group>
                <info>Orientation of table.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
              </Configuration.Config>
              <Configuration.Config>
                <gui__name>Table text orientation</gui__name>
                <name>cell_align</name>
                <value class="javafx.geometry.Pos">CENTER_LEFT</value>
                <type>javafx.geometry.Pos</type>
                <group>PlaylistController</group>
                <info>Orientation of text within table cells.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
              </Configuration.Config>
              <Configuration.Config>
                <gui__name>Zeropad numbers</gui__name>
                <name>zeropad</name>
                <value class="boolean">false</value>
                <type>java.lang.Boolean</type>
                <group>PlaylistController</group>
                <info>Adds 0 to uphold number length consistency.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
              </Configuration.Config>
              <Configuration.Config>
                <gui__name>Show table menu button</gui__name>
                <name>show_menu_button</name>
                <value class="boolean">true</value>
                <type>java.lang.Boolean</type>
                <group>PlaylistController</group>
                <info>Show table menu button for controlling columns.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
              </Configuration.Config>
              <Configuration.Config>
                <gui__name>Show table header</gui__name>
                <name>show_header</name>
                <value class="boolean">true</value>
                <type>java.lang.Boolean</type>
                <group>PlaylistController</group>
                <info>Show table header with columns.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
              </Configuration.Config>
              <Configuration.Config>
                <gui__name>Default open location</gui__name>
                <name>browse</name>
                <value class="string">C:\Users\Plutonium_\Documents\NetBeansProjects\test_files</value>
                <type>java.lang.String</type>
                <group>PlaylistController</group>
                <info>Opens this location for file dialogs.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
              </Configuration.Config>
              <Configuration.Config>
                <gui__name>File search depth</gui__name>
                <name>folder_depth</name>
                <value class="int">1</value>
                <type>java.lang.Integer</type>
                <group>PlaylistController</group>
                <info>Depth for recursive file search for folders.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
              </Configuration.Config>
            </configs>
            <url>file:/C:/Users/Plutonium_/Documents/NetBeansProjects/Player/external/Widgets/Playlist/Playlist.fxml</url>
          </child>
        </Layout.UniContainer>
      </entry>
    </children>
  </child>
</Layout.Layout>