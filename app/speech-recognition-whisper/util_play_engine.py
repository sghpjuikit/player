import os
import numpy as np
import sounddevice as sd
import soundfile as sf
from threading import Thread
from util_write_engine import Writer
from queue import Queue
from queue import Empty

class SdActorPlayback:
    def __init__(self):
        self.queue = Queue()
        self._skip = False
        self._stop = False
        self.thread = None
        self.stream = None
        self.sentence_break = 0.3
        self.sample_rate = 24000

    def start(self):
        Thread(name='SdActorPlayback', target=self._loop, daemon=True).start()

    def skip(self):
        self._skip = True
        pass

    def stop(self):
        self._stop = True
        if self.stream is not None:
            self.stream.stop()

    def _loop(self):
        self.stream = sd.OutputStream(channels=1, samplerate=24000)
        Thread(name='SdActorPlayback-stream', target=self.stream.start, daemon=True).start()

        # loop
        while not self._stop:
            try:
                type, audio, skippable = self.queue.get()

                # skip
                if self._skip and skippable:
                    continue

                # skip stop at boundary
                if type=='boundary':
                    self._skip = False
                    continue

                # play pause
                if type=='w':
                    samples_count = int(1 * self.sample_rate)
                    samples = np.zeros(samples_count)
                    self.stream.write(np.zeros(samples_count, dtype=np.float32))

                # play file
                if type=='f':
                    audio_data, fs = sf.read(audio, dtype='float32')
                    if fs!=self.sample_rate: continue
                    chunk_size = 1024
                    audio_length = len(audio_data)
                    start_pos = 0
                    while start_pos < audio_length:
                        if (self._skip and skippable) or self._stop: break
                        end_pos = min(start_pos + chunk_size, audio_length)
                        chunk = audio_data[start_pos:end_pos]
                        self.stream.write(chunk)
                        start_pos = end_pos

                # play wav chunk
                if type=='b':
                    self.stream.write(audio)

            except Empty:
                continue


class SdActor:
    def __init__(self):
        self._skip = False
        self._stop = False
        self.queue = Queue()
        self.play = SdActorPlayback()

    def start(self):
        self.play.start()
        Thread(name='SdActor', target=self._loop, daemon=True).start()

    def boundary(self):
        self.queue.put(('boundary', None, False))

    def playFile(self, audio: str, skippable: bool):
        self.queue.put(('f', audio, skippable))

    def playWavChunk(self, audio: np.ndarray, skippable: bool):
        self.queue.put(('b', audio, skippable))

    def _loop(self):
        # loop
        while not self._stop:
            type, audio, skippable = self.queue.get()

            # skip
            if self._skip and skippable:
                continue

            # skip stop at boundary
            if type=='boundary':
                self._skip = False
                self.play.queue.put(('boundary', None, False))
                continue

            # play file
            if type=='f':
                self.play.queue.put((type, audio, skippable))
                self.play.queue.put(('w', None, True))

            # play iterator of wav chunks
            if type=='b':
                for wav_chunk in audio:
                    if self._skip and skippable: break
                    self.play.queue.put((type, wav_chunk, skippable))
                self.play.queue.put(('w', None, True))

    def skip(self):
        self._skip = True
        self.play.skip()

    def stop(self):
        self._stop = True
        self.play.stop()
