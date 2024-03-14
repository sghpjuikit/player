import numpy as np
import sounddevice as sd
import soundfile as sf
from util_actor import Actor, Event
from util_wrt import Writer


class SdEvent(Event):
    def __init__(self, type: str, text: str, audio: None | str | np.ndarray, skippable: bool):
        self.type = type
        self.text = text
        self.audio = audio
        self.skippable = skippable

    def str(self): return self.text


class SdActor(Actor):
    def __init__(self, write: Writer):
        super().__init__('play', 'SdActor', True)
        self._skip = False
        self.stream = None
        self.sentence_break = 0.3
        self.sample_rate = 24000
        self.write = write

    def skip(self):
        self._skip = True

    def stop(self):
        super().stop()
        if self.stream is not None: self.stream.stop()

    def _loop(self):
        self.stream = sd.OutputStream(channels=1, samplerate=24000)
        self.stream.start()

        def process(event):
            # skip
            if self._skip and event.skippable:
                return

            # skip stop at boundary
            if event.type=='boundary':
                self._skip = False
                return

            # play pause
            def playPause():
                samples_count = int(1 * self.sample_rate)
                samples = np.zeros(samples_count)
                self.stream.write(np.zeros(samples_count, dtype=np.float32))

            # play file
            if event.type=='f':
                audio_data, fs = sf.read(event.audio, dtype='float32')
                if fs!=self.sample_rate: return
                chunk_size = 1024
                audio_length = len(audio_data)
                start_pos = 0
                while start_pos < audio_length:
                    if (self._skip and event.skippable) or self._stop: break
                    end_pos = min(start_pos + chunk_size, audio_length)
                    chunk = audio_data[start_pos:end_pos]
                    self.stream.write(chunk)
                    start_pos = end_pos
                playPause()

            # play wav chunk
            if event.type=='b':
                for wav_chunk in event.audio:
                    if self._skip and event.skippable: break
                    self.stream.write(wav_chunk)
                playPause()

        # loop
        self._loaded = True
        while not self._stop: self._loopProcessEvent(process)
        self._clear_queue()

    def boundary(self):
        self.queue.put(SdEvent('boundary', '', None, False))

    def playFile(self, text: str, audio: str, skippable: bool):
        self.queue.put(SdEvent('f', text, audio, skippable))

    def playWavChunk(self, text: str, audio: np.ndarray, skippable: bool):
        self.queue.put(SdEvent('b', text, audio, skippable))