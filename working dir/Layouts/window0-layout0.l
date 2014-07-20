<Layout.Layout>
  <id>1becfc7f-4e98-483c-a0a8-c76442e1aeee</id>
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
          <double>0.5028949545078577</double>
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
          <id>d6c815b3-17df-4d6b-90dc-406b1db146a2</id>
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
                  <double>0.14342105263157895</double>
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
                  <id>30ed8414-a53c-44f8-9eed-5e9b70f2651d</id>
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
                    <id>2c87ed05-3e4a-4c3e-980c-d768c9cee2f5</id>
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
                  <id>95ba76d2-24e0-4452-9088-6ac9b36fb9ef</id>
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
                    <id>e5a42825-c755-4db2-8336-074eac557b7b</id>
                    <name>Settings</name>
                    <configs>
                      <Configuration.InstanceFieldConfig>
                        <gui__name>Is preferred</gui__name>
                        <name>preferred</name>
                        <group>ClassWidget</group>
                        <info>Preferred widget for its widget type.</info>
                        <editable>true</editable>
                        <visible>true</visible>
                        <min>NaN</min>
                        <max>NaN</max>
                        <defaultValue class="boolean">false</defaultValue>
                        <value class="boolean">false</value>
                      </Configuration.InstanceFieldConfig>
                      <Configuration.InstanceFieldConfig>
                        <gui__name>Show non editable fields</gui__name>
                        <name>show_noneditable</name>
                        <group>Configurator</group>
                        <info>Include non read-only fields.</info>
                        <editable>true</editable>
                        <visible>true</visible>
                        <min>NaN</min>
                        <max>NaN</max>
                        <defaultValue class="boolean">false</defaultValue>
                        <value class="boolean">false</value>
                      </Configuration.InstanceFieldConfig>
                      <Configuration.InstanceFieldConfig>
                        <gui__name>Field names alignment</gui__name>
                        <name>alignemnt</name>
                        <group>Configurator</group>
                        <info>Alignment of field names.</info>
                        <editable>true</editable>
                        <visible>true</visible>
                        <min>NaN</min>
                        <max>NaN</max>
                        <defaultValue class="javafx.geometry.HPos">RIGHT</defaultValue>
                        <value class="javafx.geometry.HPos">RIGHT</value>
                      </Configuration.InstanceFieldConfig>
                      <Configuration.InstanceFieldConfig>
                        <gui__name>Group titles alignment</gui__name>
                        <name>title_align</name>
                        <group>Configurator</group>
                        <info>Alignment of group names.</info>
                        <editable>true</editable>
                        <visible>true</visible>
                        <min>NaN</min>
                        <max>NaN</max>
                        <defaultValue class="javafx.geometry.Pos">CENTER</defaultValue>
                        <value class="javafx.geometry.Pos">CENTER</value>
                      </Configuration.InstanceFieldConfig>
                      <Configuration.InstanceFieldConfig>
                        <gui__name>expanded</gui__name>
                        <name>expanded</name>
                        <group>Configurator</group>
                        <info></info>
                        <editable>true</editable>
                        <visible>false</visible>
                        <min>NaN</min>
                        <max>NaN</max>
                        <defaultValue class="string">GUI</defaultValue>
                        <value class="string">GUI</value>
                      </Configuration.InstanceFieldConfig>
                    </configs>
                    <class__type>Layout.WidgetImpl.Configurator</class__type>
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