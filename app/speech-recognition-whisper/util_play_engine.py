import os
import numpy as np
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

    def stop(self):
        self._stop = True
        if self.stream is not None:
            self.stream.stop()

    def _loop(self):
        # initialize sounddevice, soundfile, numpy
        try:
            import sounddevice as sd
            import soundfile as sf
        except ImportError:
            self.write("ERR: Sounddevice or soundfile or python module failed to load")
            return

        self.stream = sd.OutputStream(channels=1, samplerate=24000)
        Thread(name='SdActorPlayback-stream', target=self.stream.start, daemon=True).start()

        while not self._stop:
            try:
                type, audio, skippable = self.queue.get(timeout=1)

                # skip
                while self._skip and skippable:
                    type, audio, skippable = self.queue.get(timeout=1)
                self._skip = False

                # play file
                if type=='w':
                    samples_count = int(1 * self.sample_rate)
                    samples = np.zeros(samples_count)
                    self.stream.write(np.zeros(samples_count, dtype=np.float32))

                # play file
                if type=='f':
                    audio_data, fs = sf.read(audio, dtype='float32')
                    if fs!=self.sample_rate:
                        continue
                    self.stream.write(audio_data)

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

    def playFile(self, audio: str, skippable: bool):
        self.queue.put(('f', audio, skippable))

    def playWavChunk(self, audio: np.ndarray, skippable: bool):
        self.queue.put(('b', audio, skippable))

    def _loop(self):
        # loop
        while not self._stop:
            type, audio, skippable = self.queue.get()

            while self._skip and skippable:
                continue
            self._skip = False

            # play file
            if type=='f':
                self.play.queue.put((type, audio, skippable))
                self.play.queue.put(('w', None, True))

            # play iterator of wav chunks
            if type=='b':
                # must be consumed as soon as possible
                for wav_chunk in audio:
                    if not self._skip or not skippable:
                        self.play.queue.put((type, wav_chunk, skippable))
                if not self._skip or not skippable:
                    self.play.queue.put(('w', None, True))

    def skip(self):
        self._skip = True
        self.play.skip()

    def stop(self):
        self._stop = True
        self.play.stop()

class VlcActor:

    def __init__(self, vlc_path: str, write: Writer):
        self.vlc_path = vlc_path
        self.write = write
        self.skip_ = False
        self.queue = Queue()

    def start(self):
        Thread(name='VlcActor', target=self._loop, daemon=True).start()

    def _loop(self):
        # initialize vlc
        if len(self.vlc_path)>0 and os.path.exists(self.vlc_path):
            os.environ['PYTHON_VLC_MODULE_PATH'] = self.vlc_path
            os.environ['PYTHON_VLC_LIB_PATH'] = os.path.join(self.vlc_path, "libvlc.dll")
        try:
            import vlc
            import ctypes
        except ImportError as e:
            self.write("Vlc player or vlc python module failed to load")
            return

        # loop
        vlcInstance = None
        vlcPlayer = None
        while not self._stop:
            type, audio, skippable = self.queue.get()

            if self.skip_ and skippable:
                if vlcPlayer is not None:
                    player.stop()
                continue
            else:
                self.skip_ = False

            # initialize vlc once lazily
            if vlcInstance is None:
                vlcInstance = vlc.Instance()
                vlcPlayer = vlcInstance.media_player_new()

            # play audio file
            if type == 'f':
                media = vlcInstance.media_new(audio)
            # play audio data
            if type == 'b':
                write("ERR: wav chunk playback unsupported")

            vlcPlayer.set_media(media)
            vlcPlayer.play()

            # Wait to finish
            while vlcPlayer.get_state() != vlc.State.Ended:
                pass

        # dispose
        if vlcPlayer is not None:
            player.stop()
            player.release()
        if vlcInstance is not None:
            vlcInstance.release()

    def skip(self):
        self.skip_ = True

    def stop(self):
        self._stop = True
