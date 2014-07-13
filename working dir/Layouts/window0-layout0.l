<Layout.Layout>
  <id>8cef6795-aba5-43e4-92dd-5b4f9f96ecbb</id>
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
    <id>e7e8312f-7257-4432-8115-f9c7e91aeeb5</id>
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
</Layout.Layout>