from util_actor import Actor
from util_wrt import Writer
from scipy import signal
from imports import *
import sounddevice as sd
import soundfile as sf
import numpy as np
import time
import os

class SdEvent:
    def __init__(self, type: str, text: str, audio: None | str | np.ndarray | int, skippable: bool):
        self.type = type
        self.text = text
        self.audio = audio
        self.skippable = skippable
        self.future = Future()

    @classmethod
    def boundary(cls):
        return cls('boundary', '', None, False)

    @classmethod
    def empty(cls):
        return cls('e', f'', None, True)
    
    @classmethod
    def pause(cls, millis: int, skippable: bool):
        return cls('p', f'{millis}ms', millis, True)

    @classmethod
    def file(cls, text: str, audio: str, skippable: bool):
        return cls('f', text, audio, skippable)

    @classmethod
    def wavChunks(cls, text: str, audio: np.ndarray, skippable: bool):
        return cls('b', text, audio, skippable)
    
class SdActor(Actor):
    def __init__(self, write: Writer):
        super().__init__('play', 'SdActor', "cpu", write, True)
        self._skip = False
        self.sentence_break = 0.8
        self.sample_rate = 24000
        self.volume_adjuster = VolumeAdjuster()

    def start(self):
        super().start()
        self.volume_adjuster.start()

    def stop(self):
        super().stop()
        self.volume_adjuster.stop()

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
                            event.future.set_result(None)
                            continue

                        # skip stop at boundary
                        if event.type == 'boundary':
                            self._skip = False
                            event.future.set_result(None)
                            continue

                        if event.type == 'e':
                            event.future.set_result(None)

                        if event.type == 'p':
                            for i in range(0, int(event.audio)//100):
                                if (self._skip and event.skippable) or self._stop:
                                    event.future.set_result(None)
                                    break
                                time.sleep(0.01)
                            event.future.set_result(None)

                        # play file
                        if event.type == 'f':
                            self.volume_adjuster.speechStarted()
                            audio_data, fs = sf.read(event.audio, dtype='float32')
                            if fs != self.sample_rate: audio_data = signal.resample(audio_data, int(len(audio_data) * self.sample_rate / fs))
                            chunk_size = 1024
                            audio_length = len(audio_data)
                            start_pos = 0
                            while start_pos < audio_length:
                                if (self._skip and event.skippable) or self._stop:
                                    event.future.set_result(None)
                                    break
                                end_pos = min(start_pos + chunk_size, audio_length)
                                chunk = audio_data[start_pos:end_pos]
                                stream.write(chunk)
                                start_pos = end_pos
                            self.volume_adjuster.speechEnded()
                            event.future.set_result(None)
                            time.sleep(self.sentence_break)

                        # play wav chunk
                        if event.type == 'b':
                            self.volume_adjuster.speechStarted()
                            for wav_chunk in event.audio:
                                if self._skip and event.skippable:
                                    event.future.set_result(None)
                                    break
                                stream.write(wav_chunk)
                            self.volume_adjuster.speechEnded()
                            event.future.set_result(None)
                            time.sleep(self.sentence_break)

                    except Exception as x:
                        event.future.set_exception(x)
                        if event.type == "b" or event.type == "f": self.volume_adjuster.speechEnded()
                        if (self._stop): pass  # daemon thread can get interrupted and stream crash mid write
                        else: raise x
        stream.stop()
        stream.close()

    def playEvent(self, e: SdEvent) -> Future[None]:
        self.queue.put(e)
        return e.future


class VolumeAdjuster:
    def __init__(self, decreasedVolume=0.3, delay=1.0, smoothDuration=0.25):
        self.decreasedVolume = decreasedVolume
        self.delay = delay
        self.smoothDuration = smoothDuration
        self.smoothSteps = 1
        self.exclude_pid = os.getpid()
        self.speech_ended_time = time.time()-delay
        self.volume_adjustment_thread: Thread = None
        self.state = "SPEECH_STOPPED"
        self.lowered_sessions = []
        self.lowered_original_volumes = []

    def lower_volume(self):
        # do nothing non windows
        import platform
        if platform.system() != 'Windows': return

        # requires windows
        from pycaw.pycaw import AudioUtilities, ISimpleAudioVolume

        # prevent restore_volume multiple times
        self.lowered_sessions = []
        self.lowered_original_volumes = []

        step = 1.0/self.smoothSteps
        for i in range(1, 1+self.smoothSteps):
            for session in AudioUtilities.GetAllSessions():
                volume = session._ctl.QueryInterface(ISimpleAudioVolume)
                if session.Process and session.Process.pid!=self.exclude_pid:
                    current_volume = volume.GetMasterVolume()
                    target_volume = current_volume*self.decreasedVolume
                    self.lowered_sessions.append(session)
                    self.lowered_original_volumes.append(current_volume)
                    volume.SetMasterVolume(current_volume-(current_volume-target_volume)*(i * step), None)
            if self.smoothSteps>1: time.sleep(self.smoothDuration / self.smoothSteps)

    def restore_volume(self):
        # do nothing non windows
        import platform
        if platform.system() != 'Windows': return

        # requires windows
        from pycaw.pycaw import AudioUtilities, ISimpleAudioVolume

        step = 1.0/self.smoothSteps
        for i in range(1, 1+self.smoothSteps):
            for session, original_volume in zip(self.lowered_sessions, self.lowered_original_volumes):
                volume = session._ctl.QueryInterface(ISimpleAudioVolume)
                current_volume = volume.GetMasterVolume()
                volume.SetMasterVolume(current_volume-(current_volume-original_volume)*(i * step), None)
            if self.smoothSteps>1: time.sleep(self.smoothDuration / self.smoothSteps)

        # prevent restore_volume multiple times
        self.lowered_sessions = []
        self.lowered_original_volumes = []

    def loop(self):
        try:
            while True:
                time.sleep(0.1)
                if self.state == "STOPPED": break
                if self.state == "SPEECH_STARTED":
                    self.state = "SPEECH"
                    self.lower_volume()
                if self.state == "SPEECH_STOPPING" and time.time() - self.speech_ended_time > self.delay:
                    self.state = "SPEECH_STOPPED"
                    self.restore_volume()
            self.restore_volume()
        except Exception:
            print_exc()

    def start(self) -> Thread:
        self.volume_adjustment_thread = Thread(name="Volume adjuster", target=self.loop, daemon=False).start()

    def stop(self):
        self.state = "STOPPED"

    def speechStarted(self):
        if self.state == "STOPPED": return
        if self.state=="SPEECH_STOPPED": self.state = "SPEECH_STARTED"
        else: self.state = "SPEECH"

    def speechEnded(self):
        if self.state == "STOPPED": return
        if self.state != "SPEECH_STOPPING" and self.state != "SPEECH_STOPPED":
            self.speech_ended_time = time.time()
            self.state = "SPEECH_STOPPING"
