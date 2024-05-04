import uuid
from time import sleep
from typing import Callable

import whisper.audio
from speech_recognition.audio import AudioData
from util_actor import Actor
from util_wrt import Writer
from os import makedirs, remove
from os.path import dirname, abspath, exists, join
from dataclasses import dataclass
from concurrent.futures import Future
from io import BytesIO
import soundfile as sf
import numpy as np
import torch

@dataclass
class EventStt:
    event: AudioData
    future: Future[str]

    def __iter__(self):
        yield self.event
        yield self.future


class Stt(Actor):
    def __init__(self, name: str, deviceName: str | None, write: Writer, enabled: bool, sample_rate: int):
        super().__init__("stt", name, deviceName, write, enabled)
        self.sample_rate: int = sample_rate
        self.onDone: Callable[str, None] = None

    def __call__(self, e: AudioData, auto_handle: bool = True) -> Future[str]:
        ef = EventStt(e, Future())
        self.queue.put(ef)

        def on_done(future):
            try: text = future.result()
            except Exception: text = None
            if text is not None and self.onDone is not None: self.onDone(text)

        if auto_handle: ef.future.add_done_callback(on_done)
        return ef.future

    def _get_event_text(self, e: EventStt) -> str | None:
        return "AudioData"

    def _loopWaitTillReady(self):
        while not self.enabled:
            self._clear_queue()
            if self._stop: return
            sleep(0.1)


class SttNone(Stt):
    def __init__(self, write: Writer, enabled: bool):
        super().__init__('SttNone', "cpu", write, enabled, 16000)

    def _loop(self):
        self._loopLoadAndIgnoreEvents()


# home https://github.com/openai/whisper
class SttWhisper(Stt):
    def __init__(self, enabled: bool, device: str, model: str, write: Writer):
        super().__init__('SttWhisper', device, write, enabled, 16000)
        self.model = model
        self.device = device

    def _loop(self):
        self._loopWaitTillReady()

        # initialize
        import whisper
        # init model dir
        if self.sample_rate != whisper.audio.SAMPLE_RATE: raise Exception("Whisper must be 16000Hz sample rate")
        # init model dir
        modelDir = "models-whisper"
        if not exists(modelDir): makedirs(modelDir)
        # load model
        model = whisper.load_model(self.model, download_root=modelDir, device=torch.device(self.device), in_memory=True)
        # loop
        with self._looping():
            while not self._stop:
                with self._loopProcessEvent() as af:
                    try:
                        audio, f = af
                        # prepare data
                        wav_bytes = audio.get_wav_data()  # must be 16kHz
                        wav_stream = BytesIO(wav_bytes)
                        audio_array, sampling_rate = sf.read(wav_stream)
                        audio_array = audio_array.astype(np.float32)
                        # sst
                        text = model.transcribe(audio_array, language=None, task=None, fp16=torch.cuda.is_available())['text']
                        # complete
                        if not self._stop and self.enabled: f.set_result(text)
                        else: f.set_exception(Exception("Stopped or disabled"))
                    except Exception as e:
                        f.set_exception(e)
                        raise e


# home https://github.com/NVIDIA/NeMo
# comparisons https://huggingface.co/spaces/hf-audio/open_asr_leaderboard
# https://huggingface.co/nvidia/parakeet-tdt-1.1b
# https://huggingface.co/nvidia/parakeet-ctc-1.1b
class SttNemo(Stt):
    def __init__(self, enabled: bool, device: str, model: str, write: Writer):
        super().__init__('SttNemo', device, write, enabled, 16000)
        self.model = model
        self.device = device

    def _loop(self):
        self._loopWaitTillReady()

        # initialize
        import nemo.utils as nemo_utils
        from nemo.utils.nemo_logging import Logger
        import nemo.collections.asr as nemo_asr
        # disable logging
        import logging
        nemo_utils.logging.setLevel(logging.ERROR)
        logging.getLogger('nemo_logging').setLevel(logging.ERROR)
        # init cache dir
        cacheDir = join('cache', 'nemo')
        if not exists(cacheDir): makedirs(cacheDir)
        # load model
        if self.model=="nvidia/parakeet-tdt-1.1b": model = nemo_asr.models.EncDecRNNTBPEModel.from_pretrained(model_name="nvidia/parakeet-tdt-1.1b")
        if self.model=="nvidia/parakeet-ctc-1.1b": model = nemo_asr.models.EncDecCTCModelBPE.from_pretrained(model_name="nvidia/parakeet-ctc-1.1b")

        model.to(torch.device(self.device))
        # loop
        with (self._looping()):
            while not self._stop:
                with self._loopProcessEvent() as af:
                    try:
                        audio, f = af
                        # gather data
                        wav_bytes = audio.get_wav_data()  # must be 16kHz
                        wav_stream = BytesIO(wav_bytes)
                        audio_array, sampling_rate = sf.read(wav_stream)
                        audio_array = audio_array.astype(np.float32)
                        # gather data as file
                        file = join('cache', 'nemo', str(uuid.uuid4())) + '.wav'
                        sf.write(file, audio_array, sampling_rate)
                        # sst
                        hypotheses = model.transcribe([file], verbose=False)
                        hypothese1 = hypotheses[0] if hypotheses else None
                        if self.model=="nvidia/parakeet-tdt-1.1b": text = hypothese1[0] if hypothese1 else None
                        if self.model=="nvidia/parakeet-ctc-1.1b": text = hypothese1 if hypothese1 else None
                        # complete
                        if not self._stop and self.enabled: f.set_result(text if text is not None else '')
                        else: f.set_exception(Exception("Stopped or disabled"))
                    except Exception as e:
                        f.set_exception(e)
                        raise e
                    finally:
                        remove(file)


class SttHttp(Stt):
    def __init__(self, url: str, port: int, enabled: bool, device: str, model: str, write: Writer):
        super().__init__('SttHttp', 'http', write, enabled, 16000)
        self.url = url
        self.port = port

    def _loop(self):
        self._loopWaitTillReady()

        # initialize
        import io, http.client
        # loop
        with self._looping():
            while not self._stop:
                with self._loopProcessEvent() as af:
                    try:
                        # gather data
                        audio, f = af
                        audio_data = audio.frame_data
                        audio_sample_rate = audio.sample_rate.to_bytes(4, 'little')
                        audio_sample_width = audio.sample_width.to_bytes(2, 'little')
                        byte_array = audio_sample_rate + audio_sample_width + audio_data
                        # send request
                        conn = http.client.HTTPConnection(self.url, self.port, timeout=5)
                        conn.set_debuglevel(0)
                        conn.request('POST', '/stt', byte_array, {})
                        # read response
                        text = conn.getresponse().read().decode('utf-8')
                        # complete
                        if not self._stop and self.enabled: f.set_result(text)
                        else: f.set_exception(Exception("Stopped or disabled"))
                    except Exception as e:
                        f.set_exception(e)
                        raise e