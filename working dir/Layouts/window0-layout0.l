<Layout.Layout>
  <id>ce6f13a2-2c5c-4d74-9e8f-1b573733cf05</id>
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
    <id>122c6064-3a58-4848-bb12-16853354d422</id>
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
          <double>0.5028340080971659</double>
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
          <id>211a04f1-2b38-43b7-94c3-77ad0213d14c</id>
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
            <id>a76d717d-1b38-4fff-8e82-d92fd53b10c1</id>
            <name>Tagger</name>
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
                <gui__name>Read Mode</gui__name>
                <name>readMode</name>
                <group>TaggerController</group>
                <info>Source of data for the widget.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <defaultValue class="PseudoObjects.ReadMode">CUSTOM</defaultValue>
                <value class="PseudoObjects.ReadMode">CUSTOM</value>
              </Configuration.InstanceFieldConfig>
              <Configuration.InstanceFieldConfig>
                <gui__name>Read mode change on drag</gui__name>
                <name>changeReadModeOnTransfer</name>
                <group>TaggerController</group>
                <info>Change read mode to CUSTOM when data are arbitrary added to widget.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <defaultValue class="boolean">false</defaultValue>
                <value class="boolean">false</value>
              </Configuration.InstanceFieldConfig>
              <Configuration.InstanceFieldConfig>
                <gui__name>No value text</gui__name>
                <name>TAG_NO_VALUE</name>
                <group>TaggerController</group>
                <info>Field text in case no tag value for field.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <defaultValue class="string">-- no assigned value --</defaultValue>
                <value class="string">-- no assigned value --</value>
              </Configuration.InstanceFieldConfig>
              <Configuration.InstanceFieldConfig>
                <gui__name>Multiple value text</gui__name>
                <name>TAG_MULTIPLE_VALUE</name>
                <group>TaggerController</group>
                <info>Field text in case multiple tag values per field.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <defaultValue class="string">-- multiple values --</defaultValue>
                <value class="string">-- multiple values --</value>
              </Configuration.InstanceFieldConfig>
              <Configuration.InstanceFieldConfig>
                <gui__name>Allow change of playcount</gui__name>
                <name>allow_playcount_change</name>
                <group>TaggerController</group>
                <info>Change editability of playcount field. Generally to prevent change to non customary values.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <defaultValue class="boolean">false</defaultValue>
                <value class="boolean">false</value>
              </Configuration.InstanceFieldConfig>
              <Configuration.InstanceFieldConfig>
                <gui__name>Field text alignement</gui__name>
                <name>field_text_alignment</name>
                <group>TaggerController</group>
                <info>Alignment of the text in fields.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <defaultValue class="javafx.geometry.Pos">CENTER_LEFT</defaultValue>
                <value class="javafx.geometry.Pos">CENTER_LEFT</value>
              </Configuration.InstanceFieldConfig>
              <Configuration.InstanceFieldConfig>
                <gui__name>Mood picker popup position</gui__name>
                <name>popupPos</name>
                <group>TaggerController</group>
                <info>Position of the mood picker pop up relative to the mood text field.</info>
                <editable>true</editable>
                <visible>true</visible>
                <min>NaN</min>
                <max>NaN</max>
                <defaultValue class="GUI.objects.PopOver.PopOver$NodeCentricPos">DownCenter</defaultValue>
                <value class="GUI.objects.PopOver.PopOver$NodeCentricPos">DownCenter</value>
              </Configuration.InstanceFieldConfig>
            </configs>
          </child>
        </Layout.UniContainer>
      </entry>
      <entry>
        <int>2</int>
        <Layout.UniContainer>
          <id>964c0c47-7a70-4f26-b62b-fa6ab2174fa9</id>
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
            <id>8d8b6c8c-05d2-4684-a373-0192722d4670</id>
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
                  <id>e7557c5c-024e-4395-b590-e47850854c8c</id>
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
                    <id>20bf067a-0cb0-45a3-9bc9-f8ab09115b54</id>
                    <name>PlayerControls</name>
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
                        <gui__name>Show chapters</gui__name>
                        <name>showChapters</name>
                        <group>PlayerControlsController</group>
                        <info>Display chapter marks on seeker.</info>
                        <editable>true</editable>
                        <visible>true</visible>
                        <min>NaN</min>
                        <max>NaN</max>
                        <defaultValue class="boolean">true</defaultValue>
                        <value class="boolean">true</value>
                      </Configuration.InstanceFieldConfig>
                      <Configuration.InstanceFieldConfig>
                        <gui__name>Show info for chapters</gui__name>
                        <name>popupChapters</name>
                        <group>PlayerControlsController</group>
                        <info>Display pop up information for chapter marks on seeker.</info>
                        <editable>true</editable>
                        <visible>true</visible>
                        <min>NaN</min>
                        <max>NaN</max>
                        <defaultValue class="boolean">true</defaultValue>
                        <value class="boolean">true</value>
                      </Configuration.InstanceFieldConfig>
                      <Configuration.InstanceFieldConfig>
                        <gui__name>Show elapsed time</gui__name>
                        <name>elapsedTime</name>
                        <group>PlayerControlsController</group>
                        <info>Show elapsed time instead of remaining.</info>
                        <editable>true</editable>
                        <visible>true</visible>
                        <min>NaN</min>
                        <max>NaN</max>
                        <defaultValue class="boolean">true</defaultValue>
                        <value class="boolean">true</value>
                      </Configuration.InstanceFieldConfig>
                      <Configuration.InstanceFieldConfig>
                        <gui__name>Play files on drop</gui__name>
                        <name>playDropped</name>
                        <group>PlayerControlsController</group>
                        <info>Plays the drag and dropped files instead of enqueuing them in playlist.</info>
                        <editable>true</editable>
                        <visible>true</visible>
                        <min>NaN</min>
                        <max>NaN</max>
                        <defaultValue class="boolean">false</defaultValue>
                        <value class="boolean">false</value>
                      </Configuration.InstanceFieldConfig>
                    </configs>
                  </child>
                </Layout.UniContainer>
              </entry>
              <entry>
                <int>2</int>
                <Layout.UniContainer>
                  <id>65628086-13c3-4353-966e-c28fd920397f</id>
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
                    <id>7eae9240-6650-47ce-9dbe-82c1792083af</id>
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
</Layout.Layout>