import os.path
import uuid
from time import sleep
from typing import Callable
from speech_recognition import AudioData
from util_actor import Actor
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


class Stt(Actor):
    def __init__(self, name: str, enabled: bool, sample_rate: int):
        super().__init__("stt", name, enabled)
        self.sample_rate: int = sample_rate


class SttNone(Stt):
    def __init__(self, enabled: bool):
        super().__init__('SttNone', enabled, 16000)

    def _loop(self):
        self._loopLoadAndIgnoreEvents()


# home https://github.com/openai/whisper
class SttWhisper(Stt):
    def __init__(self, target: Callable[str, None] | None, enabled: bool, device: str | None, model: str, write: Writer):
        super().__init__('SttWhisper', enabled, whisper_sample_rate)
        self.write: Writer = write
        self._target = target
        self.model = model
        self.device = device

    def _loop(self):
        # wait till enabled
        while not self.enabled and self._target is None:
            self._clear_queue()
            if self._stop: return
            sleep(0.1)
            continue

        modelDir = join(dirname(abspath(__file__)), "models-whisper")
        if not exists(modelDir): makedirs(modelDir)

        # load model
        device = None if self.device is None or len(self.device)==0 else torch.device(self.device)
        model = whisper_load(self.model, download_root=modelDir, device=device, in_memory=True)
        # disable logging
        filterwarnings("ignore", category=UserWarning, module='whisper.transcribe', lineno=114)

        self._loaded = True
        while not self._stop:
            try:
                audio_data = self.queue.get()
                wav_bytes = audio_data.get_wav_data() # must be 16kHz
                wav_stream = BytesIO(wav_bytes)
                audio_array, sampling_rate = sf.read(wav_stream)
                audio_array = audio_array.astype(np.float32)
                if not self._stop and self.enabled:
                    text = model.transcribe(audio_array, language=None, task=None, fp16=torch.cuda.is_available())['text']
                    self.events_processed += 1
                    if not self._stop and self.enabled: self._target(text)
            except Exception as e:
                self.write("ERR: Error occurred:" + str(e))
                print_exc()

        self._clear_queue()


# home https://github.com/NVIDIA/NeMo
class SttNemo(Stt):
    def __init__(self, target: Callable[str, None] | None, enabled: bool, device: str | None, model: str, write: Writer):
        super().__init__('SttNemo', enabled, 16000)
        self.write: Writer = write
        self._target = target
        self.model = model
        self.device = device

    def _loop(self):
        # wait till enabled
        while not self.enabled and self._target is None:
            self._clear_queue()
            if self._stop: return
            sleep(0.1)
            continue

        # disable logging
        import logging
        import nemo.utils as nemo_utils
        nemo_utils.logging.setLevel(logging.ERROR)
        # load model
        import nemo.collections.asr as nemo_asr
        model = nemo_asr.models.EncDecRNNTBPEModel.from_pretrained(model_name=self.model)
        # set device
        device = None if self.device is None or len(self.device)==0 else torch.device(self.device)
        model.to(device)

        self._loaded = True
        while not self._stop:
            try:
                audio_data = self.queue.get()
                wav_bytes = audio_data.get_wav_data() # must be 16kHz
                wav_stream = BytesIO(wav_bytes)
                audio_array, sampling_rate = sf.read(wav_stream)
                audio_array = audio_array.astype(np.float32)

                f = os.path.join('cache', str(uuid.uuid4())) + '.wav'
                sf.write(f, audio_array, sampling_rate)

                if not self._stop and self.enabled:
                    hypotheses = model.transcribe([f])
                    hypothese1 = hypotheses[0] if hypotheses else None
                    text = hypothese1[0] if hypothese1 else None
                    self.events_processed += 1
                    if text is not None and not self._stop and self.enabled: self._target(text)

            except Exception as e:
                self.write("ERR: Error occurred:" + str(e))
                print_exc()

        self._clear_queue()