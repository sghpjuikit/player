import numpy as np
import sounddevice as sd
import soundfile as sf
from util_actor import Actor
from util_wrt import Writer
from scipy import signal


class SdEvent:
    def __init__(self, type: str, text: str, audio: None | str | np.ndarray | int, skippable: bool):
        self.type = type
        self.text = text
        self.audio = audio
        self.skippable = skippable

class SdActor(Actor):
    def __init__(self, write: Writer):
        super().__init__('play', 'SdActor', "cpu", write, True)
        self._skip = False
        self.sentence_break = 1
        self.sample_rate = 24000

    def skip(self):
        self._skip = True

    def _get_event_text(self, e: SdEvent) -> str:
        return f'{e.type}:{e.text}'

    def _loop(self):
        stream = sd.OutputStream(channels=1, samplerate=24000)
        stream.start()

        # loop
        with self._looping():
            while not self._stop:
                with self._loopProcessEvent() as event:
                    try:
                        # skip
                        if self._skip and event.skippable:
                            continue

                        # skip stop at boundary
                        if event.type == 'boundary':
                            self._skip = False
                            continue

                        # play pause
                        def playPause():
                            samples_count = int(self.sentence_break * self.sample_rate)
                            samples = np.zeros(samples_count)
                            stream.write(np.zeros(samples_count, dtype=np.float32))

                        if event.type == 'p':
                            samples_count = int(int(event.audio)/1000.0 * self.sample_rate)
                            samples = np.zeros(samples_count)
                            stream.write(np.zeros(samples_count, dtype=np.float32))
                            
                        # play file
                        if event.type == 'f':
                            audio_data, fs = sf.read(event.audio, dtype='float32')
                            if fs != self.sample_rate: audio_data = signal.resample(audio_data, int(len(audio_data) * self.sample_rate / fs))
                            chunk_size = 1024
                            audio_length = len(audio_data)
                            start_pos = 0
                            while start_pos < audio_length:
                                if (self._skip and event.skippable) or self._stop: break
                                end_pos = min(start_pos + chunk_size, audio_length)
                                chunk = audio_data[start_pos:end_pos]
                                stream.write(chunk)
                                start_pos = end_pos
                            playPause()

                        # play wav chunk
                        if event.type == 'b':
                            for wav_chunk in event.audio:
                                if self._skip and event.skippable: break
                                stream.write(wav_chunk)
                            playPause()
                    except Exception as x:
                        if (self._stop): pass  # daemon thread can get interrupted and stream crash mid write
                        else: raise x
        stream.stop()

    def boundary(self):
        self.queue.put(SdEvent('boundary', '', None, False))

    def playPause(self, millis: int, skippable: bool):
        self.queue.put(SdEvent('p', '', millis, True))
        
    def playFile(self, text: str, audio: str, skippable: bool):
        self.queue.put(SdEvent('f', text, audio, skippable))

    def playWavChunk(self, text: str, audio: np.ndarray, skippable: bool):
        self.queue.put(SdEvent('b', text, audio, skippable))
