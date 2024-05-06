from imports import *
from util_mic import OnSpeechStart, Speech, OnSpeechEnd
from os.path import dirname, abspath, exists, join
from speech_recognition.audio import AudioData
from os import makedirs, remove
from datetime import datetime
from util_actor import Actor
from util_wrt import Writer
from time import sleep
from io import BytesIO
import whisper.audio
import soundfile as sf
import numpy as np
import torch
import uuid


@dataclass
class SpeechText:
    start: datetime
    audio: AudioData
    stop: datetime
    text: str


@dataclass
class EventStt:
    start: datetime
    audio: AudioData
    stop: datetime
    future: Future[SpeechText]


class Stt(Actor):
    def __init__(self, name: str, deviceName: str | None, write: Writer, enabled: bool, sample_rate: int):
        super().__init__("stt", name, deviceName, write, enabled)
        self.sample_rate: int = sample_rate
        self.onDone: Callable[SpeechText, None] = None

    def __call__(self, e: Speech, auto_handle: bool = True) -> Future[str]:
        ef = EventStt(e.start, e.audio, e.stop, Future())
        self.queue.put(ef)

        if auto_handle:
            def on_done(future):
                try: st = future.result()
                except Exception: st = None
                if st is not None and self.onDone is not None: self.onDone(st)
            ef.future.add_done_callback(on_done)

        return ef.future

    def _get_event_text(self, e: EventStt) -> str | None:
        return f"AudioData({e.start}, {e.stop})"

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
                with self._loopProcessEvent() as a:
                    try:
                        # prepare data
                        wav_bytes = a.audio.get_wav_data()  # must be 16kHz
                        wav_stream = BytesIO(wav_bytes)
                        audio_array, sampling_rate = sf.read(wav_stream)
                        audio_array = audio_array.astype(np.float32)
                        # sst
                        text = model.transcribe(audio_array, language=None, task=None, fp16=torch.cuda.is_available())['text']
                        # complete
                        if not self._stop and self.enabled: a.future.set_result(SpeechText(a.start, a.audio, a.stop, text))
                        else: a.future.set_exception(Exception("Stopped or disabled"))
                    except Exception as e:
                        a.future.set_exception(e)
                        raise e


# home https://github.com/NVIDIA/NeMo
# comparisons https://huggingface.co/spaces/hf-audio/open_asr_leaderboard
# https://huggingface.co/nvidia/parakeet-tdt-1.1b
# https://huggingface.co/nvidia/parakeet-ctc-1.1b
# https://huggingface.co/nvidia/parakeet-ctc-0.6b
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
        if self.model=="nvidia/parakeet-ctc-0.6b": model = nemo_asr.models.EncDecCTCModelBPE.from_pretrained(model_name="nvidia/parakeet-ctc-0.6b")

        model.to(torch.device(self.device))
        # loop
        with (self._looping()):
            while not self._stop:
                with self._loopProcessEvent() as a:
                    try:
                        # gather data
                        wav_bytes = a.audio.get_wav_data()  # must be 16kHz
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
                        if self.model=="nvidia/parakeet-ctc-0.6b": text = hypothese1 if hypothese1 else None
                        # complete
                        if not self._stop and self.enabled: a.future.set_result(SpeechText(a.start, a.audio, a.stop, text if text is not None else ''))
                        else: a.future.set_exception(Exception("Stopped or disabled"))
                    except Exception as e:
                        a.future.set_exception(e)
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
                with self._loopProcessEvent() as a:
                    try:
                        # gather data
                        audio_data = a.audio.frame_data
                        audio_sample_rate = a.audio.sample_rate.to_bytes(4, 'little')
                        audio_sample_width = a.audio.sample_width.to_bytes(2, 'little')
                        byte_array = audio_sample_rate + audio_sample_width + audio_data
                        # send request
                        conn = http.client.HTTPConnection(self.url, self.port, timeout=5)
                        conn.set_debuglevel(0)
                        conn.request('POST', '/stt', byte_array, {})
                        # read response
                        text = conn.getresponse().read().decode('utf-8')
                        # complete
                        if not self._stop and self.enabled: a.future.set_result(SpeechText(a.start, a.audio, a.stop, text))
                        else: a.future.set_exception(Exception("Stopped or disabled"))
                    except Exception as e:
                        a.future.set_exception(e)
                        raise e