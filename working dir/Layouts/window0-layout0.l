<Layout.Layout>
  <id>afca3073-53e6-42f4-8c55-7ba6c587bd77</id>
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
          <double>0.5077574047954866</double>
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
          <id>b9943b5e-5c1c-428e-9d7a-49fb4705fb30</id>
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
                  <double>0.10732323232323232</double>
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
                  <id>28caea7b-2373-4752-a3a6-800b3df54206</id>
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
                    <id>0c97b17e-4239-48ae-91d4-cb5333a79dc8</id>
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
                  <id>5f37068c-f281-47d8-a469-f0d147b69c62</id>
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
                    <id>fa25c957-9aba-452f-9aad-01e477c4956d</id>
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