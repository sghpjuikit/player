from threading import Thread
from queue import Queue
from collections.abc import Iterator
from time import sleep
from speech_recognition import Recognizer, Microphone, WaitTimeoutError # https://github.com/Uberi/speech_recognition
from util_tty_engines import Tty
from util_write_engine import Writer
from itertools import chain
import audioop
import os
import io
import numpy as np
import soundfile as sf
import torch
import warnings
import traceback
import whisper # https://github.com/openai/whisper
import pyaudio

def get_microphone_index_by_name(name):
    p = pyaudio.PyAudio()
    device_index = None

    for i in range(p.get_device_count()):
        device_info = p.get_device_info_by_index(i)
        if name == device_info['name']:
            device_index = i
            break

    p.terminate()
    return device_index

class Mic:
    def __init__(self, micName: str | None, micOn: bool, whisper: Queue, speak: Tty, write: Writer, micEnergy: int, micEnergyDebug: bool):
        self.listening = None
        self.whisper = whisper
        self.speak = speak
        self.write = write
        self.micName = micName
        self.micEnergy = 120
        self.micEnergyDebug = micEnergyDebug
        self.micOn = micOn
        self._stop = False

    def start(self):
        Thread(name='Mic', target=self._loop, daemon=True).start()

    def _loop(self):
        r = Recognizer()
        r.pause_threshold = 0.8
        r.phrase_threshold = 0.3
        r.energy_threshold = self.micEnergy
        r.dynamic_energy_threshold = False # does not work that well, instead we provide self.micEnergyDebug

        while not self._stop:

            # reconnect microphone
            source = None
            sourceI = 0
            while not self._stop and source is None:
                sourceI = sourceI+1

                # wait till mic is on
                if not self.micOn:
                    sleep(1)
                    continue

                try:
                    if self.micName is None:
                        source = Microphone(sample_rate=whisper.audio.SAMPLE_RATE)
                        # this helps retain mic after reconnecting
                        audio = source.pyaudio_module.PyAudio()
                        self.micName = audio.get_default_input_device_info()['name']
                        self.write(f"RAW: Using microphone: {self.micName}")
                        audio.terminate()
                    else:
                        i = get_microphone_index_by_name(self.micName)
                        if i is not None:
                            source = Microphone(device_index=i, sample_rate=whisper.audio.SAMPLE_RATE)
                            self.write(f"RAW: Using microphone: {self.micName}")
                        else:
                            if sourceI==1:
                                self.write(chain([f'ERR: no microphone {self.micName} found. Use one of:'], map(lambda name: '\n\t' + name, Microphone.list_microphone_names())))
                except Exception as e:
                    if sourceI==1:
                        self.speak("Failed to use microphone. See log for details")
                        traceback.print_exc()

                # mic connected
                if source is not None:
                    if sourceI>1: self.speak("Microphone back online.")
                    break

                # keep reconnecting microphone
                else:
                    sleep(1)
                    continue

            try:
                with source:
                    while not self._stop:

                        # wait till mic is on
                        while not self._stop and not self.micOn:
                            sleep(0.1)
                            continue

                        # sensitivity debug
                        while not self._stop and self.micEnergyDebug:
                            buffer = source.stream.read(source.CHUNK)
                            if len(buffer) == 0: break
                            energy = audioop.rms(buffer, source.SAMPLE_WIDTH)
                            self.write(f"Mic energy_treshold={r.energy_threshold} energy_current={energy}")

                        # listen to mic
                        try:
                            audio_data = r.listen(source, timeout=1)

                            # speech recognition
                            if not self._stop and self.micOn:
                                self.whisper.put(audio_data)

                        # ignore silence
                        except WaitTimeoutError:
                            pass

            # go reconnect mic
            except OSError as e:
                source = None
                if e.errno == -9988:
                    self.speak("Microphone offline. Check your microphone connection please.")
                else:
                    self.write("ERR: Other OSError occurred:" + str(e))
                    traceback.print_exc()
                pass

            # go reconnect mic
            except Exception as e:
                self.write("ERR: Error occurred:" + str(e))
                traceback.print_exc()
                pass

    def stop(self):
        self.micEnergyDebug = False
        self._stop = True


# home https://github.com/openai/whisper
class Whisper:
    def __init__(self, target, whisperOn: bool, model: str):
        self.queue = Queue()
        self._target = target
        self._stop = False
        self.model=model
        self.whisperOn=whisperOn

    def start(self):
        Thread(name='Whisper', target=self._loop, daemon=True).start()

    def _loop(self):
        # wait till whisper is on
        while not self.whisperOn and self._target is None:
            if self._stop: return
            sleep(0.1)
            continue

        modelDir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "models-whisper")
        if not os.path.exists(modelDir):
            os.makedirs(modelDir)
        model = whisper.load_model(self.model, download_root=modelDir, in_memory=True)
        warnings.filterwarnings("ignore", category=UserWarning, module='whisper.transcribe', lineno=114)

        while not self._stop:
            try:
                audio_data = self.queue.get()
                wav_bytes = audio_data.get_wav_data() # must be 16kHz
                wav_stream = io.BytesIO(wav_bytes)
                audio_array, sampling_rate = sf.read(wav_stream)
                audio_array = audio_array.astype(np.float32)
                if not self._stop and self.whisperOn:
                    text = model.transcribe(audio_array, language=None, task=None, fp16=torch.cuda.is_available())['text']
                    if not self._stop and self.whisperOn:
                        self._target(text)
            except Exception as e:
                self.write("ERR: Error occurred:" + str(e))
                traceback.print_exc()

    def stop(self):
        self._stop = True
