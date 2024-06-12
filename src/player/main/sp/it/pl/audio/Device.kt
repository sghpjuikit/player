package sp.it.pl.audio

import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Line
import javax.sound.sampled.SourceDataLine
import javax.sound.sampled.TargetDataLine

/** @return available audio input device names */
fun audioInputDeviceNames(): List<String> =
   AudioSystem.getMixerInfo()
      .filter { AudioSystem.getMixer(it).run { targetLineInfo.isNotEmpty() && isLineSupported(Line.Info(TargetDataLine::class.java)) } }
      .map { it.name }

/** @return available audio output device names */
fun audioOutputDeviceNames(): List<String> =
   AudioSystem.getMixerInfo()
      .filter { AudioSystem.getMixer(it).run { sourceLineInfo.isNotEmpty() && isLineSupported(Line.Info(SourceDataLine::class.java)) } }
      .map { it.name }