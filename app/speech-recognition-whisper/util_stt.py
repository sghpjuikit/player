import uuid
from time import sleep
from typing import Callable

import whisper.audio
from speech_recognition import AudioData
from util_actor import Actor
from util_wrt import Writer
from os import makedirs, remove
from os.path import dirname, abspath, exists, join
from io import BytesIO
from warnings import filterwarnings
from traceback import print_exc
import soundfile as sf
import numpy as np
import torch


class Stt(Actor):
    def __init__(self, name: str, deviceName: str | None, write: Writer, enabled: bool, sample_rate: int, target: Callable[str, None] | None):
        super().__init__("stt", name, deviceName, write, enabled)
        self.sample_rate: int = sample_rate
        self.target = target

    def _loopWaitTillReady(self):
        while not self.enabled and self.target is None:
            self._clear_queue()
            if self._stop: return
            sleep(0.1)
            continue


class SttNone(Stt):
    def __init__(self, write: Writer, enabled: bool):
        super().__init__('SttNone', "cpu", write, enabled, 16000, lambda: None)

    def _loop(self):
        self._loopLoadAndIgnoreEvents()


# home https://github.com/openai/whisper
class SttWhisper(Stt):
    def __init__(self, target: Callable[str, None] | None, enabled: bool, device: str, model: str, write: Writer):
        super().__init__('SttWhisper', device, write, enabled, 16000, target)
        self.model = model
        self.device = device

    def _loop(self):
        self._loopWaitTillReady()

        # initialize whisper
        try:
            import whisper
        except ImportError:
            self.write("ERR: whisper python module failed to load")
            return
        # init model dir
        if self.sample_rate != whisper.audio.SAMPLE_RATE: raise Exception("Whisper must be 16000Hz sample rate")
        # init model dir
        modelDir = "models-whisper"
        if not exists(modelDir): makedirs(modelDir)
        # load model
        model = whisper.load_model(self.model, download_root=modelDir, device=torch.device(self.device), in_memory=True)
        # disable logging
        filterwarnings("ignore", category=UserWarning, module='whisper.transcribe', lineno=114)
        # loop
        with self._looping():
            while not self._stop:
                with self._loopProcessEvent() as audio:
                    wav_bytes = audio.get_wav_data()  # must be 16kHz
                    wav_stream = BytesIO(wav_bytes)
                    audio_array, sampling_rate = sf.read(wav_stream)
                    audio_array = audio_array.astype(np.float32)
                    text = model.transcribe(audio_array, language=None, task=None, fp16=torch.cuda.is_available())['text']
                    if not self._stop and self.enabled: self.target(text)


# home https://github.com/NVIDIA/NeMo
class SttNemo(Stt):
    def __init__(self, target: Callable[str, None] | None, enabled: bool, device: str, model: str, write: Writer):
        super().__init__('SttNemo', device, write, enabled, 16000, target)
        self.target = target
        self.model = model
        self.device = device

    def _loop(self):
        self._loopWaitTillReady()

        # disable logging
        import logging
        import nemo.utils as nemo_utils
        nemo_utils.logging.setLevel(logging.ERROR)
        # init cache dir
        cacheDir = join('cache', 'nemo')
        if not exists(cacheDir): makedirs(cacheDir)
        # load model
        import nemo.collections.asr as nemo_asr
        model = nemo_asr.models.EncDecRNNTBPEModel.from_pretrained(model_name=self.model)
        model.to(torch.device(self.device))
        # loop
        with self._looping():
            while not self._stop:
                with self._loopProcessEvent() as audio:
                    wav_bytes = audio.get_wav_data()  # must be 16kHz
                    wav_stream = BytesIO(wav_bytes)
                    audio_array, sampling_rate = sf.read(wav_stream)
                    audio_array = audio_array.astype(np.float32)

                    f = join('cache', 'nemo', str(uuid.uuid4())) + '.wav'
                    sf.write(f, audio_array, sampling_rate)

                    try:
                        hypotheses = model.transcribe([f])
                        hypothese1 = hypotheses[0] if hypotheses else None
                        text = hypothese1[0] if hypothese1 else None
                        if text is not None and not self._stop and self.enabled: self.target(text)
                    finally:
                        remove(f)
