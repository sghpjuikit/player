package sp.it.pl.audio

import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Line
import javax.sound.sampled.TargetDataLine
import sp.it.util.functional.net

/** @return available microphone names */
fun microphoneNames(): List<String> =
   AudioSystem.getMixerInfo()
      .filter { AudioSystem.getMixer(it).net { m -> m.targetLineInfo.isNotEmpty() && m.isLineSupported(Line.Info(TargetDataLine::class.java)) } }
      .map { it.name }