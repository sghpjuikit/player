from imports import *
from util_mic import OnSpeechStart, Speech, OnSpeechEnd, AudioData
from os.path import dirname, abspath, exists, join
from os import makedirs, remove
from datetime import datetime
from util_actor import Actor
from util_wrt import Writer
from util_fut import *
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
    user: str
    text: str


@dataclass
class EventStt:
    start: datetime
    audio: AudioData
    stop: datetime
    user: str
    future: Future[SpeechText]


class Stt(Actor):
    def __init__(self, name: str, deviceName: str | None, write: Writer, enabled: bool, sample_rate: int):
        super().__init__("stt", name, deviceName, write, enabled)
        self.sample_rate: int = sample_rate
        self.onDone: Callable[SpeechText, None] = None

    def __call__(self, e: Speech, auto_handle: bool = True) -> Future[SpeechText]:
        ef = EventStt(e.start, e.audio, e.stop, e.user, Future())
        self.queue.put(ef)

        if auto_handle:
            def on_done(future):
                try: st = future.result()
                except Exception: st = None
                if st is not None and self.onDone is not None: self.onDone(st)
            futureOnDone(ef.future, on_done)

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
                        text = model.transcribe(audio_array, language='en', task='transcribe', fp16=torch.cuda.is_available())['text']
                        # complete
                        if not self._stop and self.enabled: a.future.set_result(SpeechText(a.start, a.audio, a.stop, a.user, text))
                        else: a.future.set_exception(Exception("Stopped or disabled"))
                    except Exception as e:
                        a.future.set_exception(e)
                        raise e

# home https://github.com/shashikg/WhisperS2T
class SttWhisperS2T(Stt):
    def __init__(self, enabled: bool, device: str, model: str, write: Writer):
        super().__init__('SttWhisperS2T', device, write, enabled, 16000)
        self.model = model
        self.device = device

    def _loop(self):
        self._loopWaitTillReady()

        # initialize
        import whisper_s2t
        from whisper_s2t.backends.ctranslate2.model import BEST_ASR_CONFIG
        # init model dir
        if self.sample_rate != whisper.audio.SAMPLE_RATE: raise Exception("Whisper must be 16000Hz sample rate")
        # init model dir
        modelDir = "models-whispers2t"
        if not exists(modelDir): makedirs(modelDir)
        # load model
        device = self.device.split(':')[0] if ':' in self.device else self.device
        device_index = int(self.device.split(':')[1]) if ':' in self.device else 0
        model_kwargs = {
            'compute_type': 'int8', # Note int8 is only supported for CTranslate2 backend, for others only float16 is supported for lower precision.
            'asr_options': BEST_ASR_CONFIG
        }
        model = whisper_s2t.load_model(model_identifier=self.model, backend='CTranslate2', device=device, device_index=device_index, **model_kwargs)
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
                        # gather data as file
                        file = join('cache', 'nemo', str(uuid.uuid4())) + '.wav'
                        sf.write(file, audio_array, sampling_rate)
                        # sst
                        files = [file]
                        lang_codes = ['en']
                        tasks = ['transcribe']
                        initial_prompts = [None]
                        text = model.transcribe_with_vad(files, lang_codes=lang_codes, tasks=tasks, initial_prompts=initial_prompts, batch_size=32)
                        # complete
                        if not self._stop and self.enabled: a.future.set_result(SpeechText(a.start, a.audio, a.stop, a.user, text[0][0]['text']))
                        else: a.future.set_exception(Exception("Stopped or disabled"))
                    except Exception as e:
                        a.future.set_exception(e)
                        raise e
                    finally:
                        remove(file)


# home https://github.com/SYSTRAN/faster-whisper
class SttFasterWhisper(Stt):
    def __init__(self, enabled: bool, device: str, model: str, write: Writer):
        super().__init__('SttFasterWhisper', device, write, enabled, 16000)
        self.model = model
        self.device = device

    def _loop(self):
        self._loopWaitTillReady()

        # initialize
        from faster_whisper import WhisperModel
        # init model dir
        if self.sample_rate != whisper.audio.SAMPLE_RATE: raise Exception("Whisper must be 16000Hz sample rate")
        # init model dir
        modelDir = "models-fasterwhisper"
        if not exists(modelDir): makedirs(modelDir)
        # load model
        device = self.device.split(':')[0] if ':' in self.device else self.device
        device_index = int(self.device.split(':')[1]) if ':' in self.device else 0
        compute_type = "int8_float16" if self.device.startswith("cuda") else "int8"
        model = WhisperModel(model_size_or_path=self.model, download_root=modelDir, device=device, device_index=device_index, compute_type=compute_type)
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
                        segments, info = model.transcribe(audio=audio_array, language='en', task='transcribe', beam_size=5)
                        text = ''.join(segment.text for segment in segments)
                        # complete
                        if not self._stop and self.enabled: a.future.set_result(SpeechText(a.start, a.audio, a.stop, a.user, text))
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
                        if not self._stop and self.enabled: a.future.set_result(SpeechText(a.start, a.audio, a.stop, a.user, text if text is not None else ''))
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
                        res = conn.getresponse()
                        text = res.read().decode('utf-8')
                        if res.status != 200: raise Exception(text)
                        # complete
                        if not self._stop and self.enabled: a.future.set_result(SpeechText(a.start, a.audio, a.stop, a.user, text))
                        else: a.future.set_exception(Exception("Stopped or disabled"))
                    except Exception as e:
                        a.future.set_exception(e)
                        raise e