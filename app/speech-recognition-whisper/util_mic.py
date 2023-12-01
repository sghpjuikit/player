from threading import Thread
from queue import Queue
from collections.abc import Iterator
from time import sleep
from speech_recognition import Recognizer, Microphone # https://github.com/Uberi/speech_recognition
from typing import cast
import os
import io
import numpy as np
import soundfile as sf
import torch
import warnings
import whisper # https://github.com/openai/whisper


class Mic:
    def __init__(self, micOn: bool, whisper: Queue):
        self.listening = None
        self.whisper = whisper
        self.micOn = micOn

    def start(self):
        r = Recognizer()
        r.pause_threshold = 0.8
        r.phrase_threshold = 0.3
        r.energy_threshold = 120
        r.dynamic_energy_threshold = False

        source = Microphone(sample_rate=whisper.audio.SAMPLE_RATE)
        source.SAMPLE_RATE
        # with source:
        #     r.adjust_for_ambient_noise(source, duration=3)

        def callback(recognizer, audio_data):
            if (self.micOn):
                self.whisper.put(audio_data)

        self.listening = r.listen_in_background(source, callback)

    def stop(self):
        if self.listening is not None:
            cast(callable, self.listening)(False)


class Whisper:
    def __init__(self, target, model: str):
        self.queue = Queue()
        self._target = target
        self._stop = False
        self.model=model

    def start(self):
        Thread(name='Whisper', target=self._loop, daemon=True).start()

    def _loop(self):
        modelDir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "models-whisper")
        if not os.path.exists(modelDir):
            os.makedirs(modelDir)
        model = whisper.load_model(self.model, download_root=modelDir, in_memory=True)
        warnings.filterwarnings("ignore", category=UserWarning, module='whisper.transcribe', lineno=114)

        while not self._stop and self._target is None:
            sleep(0.1)

        while not self._stop:
            audio_data = self.queue.get()
            wav_bytes = audio_data.get_wav_data() # must be 16kHz
            wav_stream = io.BytesIO(wav_bytes)
            audio_array, sampling_rate = sf.read(wav_stream)
            audio_array = audio_array.astype(np.float32)
            text = model.transcribe(audio_array, language=None, task=None, fp16=torch.cuda.is_available())['text']

            if not self._stop:
                self._target(text)

    def stop(self):
        self._stop = True
