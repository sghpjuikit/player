from threading import Thread
from queue import Queue
from typing import Callable
from speech_recognition import AudioData
from util_write_engine import Writer
from os import makedirs
from os.path import dirname, abspath, exists, join
from io import BytesIO
from warnings import filterwarnings
from traceback import print_exc
from whisper.audio import SAMPLE_RATE as whisper_sample_rate  # https://github.com/openai/whisper
from whisper import load_model as whisper_load  # https://github.com/openai/whisper
import soundfile as sf
import numpy as np
import torch

# home https://github.com/openai/whisper
class Whisper:
    def __init__(self, target: Callable[str, None] | None, whisperOn: bool, device: str | None, model: str, write: Writer):
        self.queue = Queue()
        self.write: Writer = write
        self._target = target
        self._stop = False
        self.model = model
        self.whisperOn: bool = whisperOn
        self.device = device
        self.sample_rate: int = whisper_sample_rate

    def start(self):
        Thread(name='Whisper', target=self._loop, daemon=True).start()

    def _loop(self):
        # wait till whisper is on
        while not self.whisperOn and self._target is None:
            if self._stop: return
            sleep(0.1)
            continue

        modelDir = join(dirname(abspath(__file__)), "models-whisper")
        if not exists(modelDir): makedirs(modelDir)

        device = None if self.device is None or len(self.device)==0 else torch.device(self.device)
        model = whisper_load(self.model, download_root=modelDir, device=device, in_memory=True)
        filterwarnings("ignore", category=UserWarning, module='whisper.transcribe', lineno=114)

        while not self._stop:
            try:
                audio_data = self.queue.get()
                wav_bytes = audio_data.get_wav_data() # must be 16kHz
                wav_stream = BytesIO(wav_bytes)
                audio_array, sampling_rate = sf.read(wav_stream)
                audio_array = audio_array.astype(np.float32)
                if not self._stop and self.whisperOn:
                    text = model.transcribe(audio_array, language=None, task=None, fp16=torch.cuda.is_available())['text']
                    if not self._stop and self.whisperOn: self._target(text)
            except Exception as e:
                self.write("ERR: Error occurred:" + str(e))
                print_exc()

    def stop(self):
        self._stop = True
